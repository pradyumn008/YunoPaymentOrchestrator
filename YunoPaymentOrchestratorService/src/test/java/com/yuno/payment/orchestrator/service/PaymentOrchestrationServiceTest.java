package com.yuno.payment.orchestrator.service;

import com.yuno.payment.orchestrator.api.model.CreatePaymentRequest;
import com.yuno.payment.orchestrator.api.model.PaymentMethod;
import com.yuno.payment.orchestrator.api.model.PaymentResponse;
import com.yuno.payment.orchestrator.api.model.PaymentStatus;
import com.yuno.payment.orchestrator.api.model.ProviderCode;
import com.yuno.payment.orchestrator.exception.PaymentNotFoundException;
import com.yuno.payment.orchestrator.idempotency.DuplicateRequestIdException;
import com.yuno.payment.orchestrator.idempotency.IdempotencyService;
import com.yuno.payment.orchestrator.repository.PaymentAttemptRepository;
import com.yuno.payment.orchestrator.repository.entity.PaymentEntity;
import com.yuno.payment.orchestrator.repository.PaymentRepository;
import com.yuno.payment.orchestrator.model.PaymentProviderResponse;
import com.yuno.payment.orchestrator.provider.ProviderAConnector;
import com.yuno.payment.orchestrator.provider.ProviderBConnector;
import com.yuno.payment.orchestrator.routing.RoutingDecision;
import com.yuno.payment.orchestrator.routing.RoutingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentOrchestrationService unit tests")
class PaymentOrchestrationServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentAttemptRepository attemptRepository;
    @Mock private IdempotencyService idempotencyService;
    @Mock private RoutingEngine routingEngine;
    @Mock private PaymentMapper paymentMapper;
    @Mock private ProviderAConnector providerAConnector;
    @Mock private ProviderBConnector providerBConnector;

    private PaymentOrchestrationService service;

    @BeforeEach
    void setUp() {
        when(providerAConnector.providerCode()).thenReturn(com.yuno.payment.orchestrator.domain.ProviderCode.A);
        when(providerBConnector.providerCode()).thenReturn(com.yuno.payment.orchestrator.domain.ProviderCode.B);

        service = new PaymentOrchestrationService(
                paymentRepository, attemptRepository, idempotencyService,
                routingEngine, paymentMapper, List.of(providerAConnector, providerBConnector));
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-S01: CARD payment processed through Provider A")
    void createPayment_card_succeeds_viaProviderA() {
        when(routingEngine.route(com.yuno.payment.orchestrator.domain.PaymentMethod.CARD))
                .thenReturn(new RoutingDecision(
                        com.yuno.payment.orchestrator.domain.ProviderCode.A,
                        com.yuno.payment.orchestrator.domain.ProviderCode.B));
        when(providerAConnector.authorize(any())).thenReturn(PaymentProviderResponse.success("A-ref-001"));
        when(paymentMapper.toResponse(any(), any())).thenReturn(successResponse(ProviderCode.A));

        PaymentResponse response = service.createPayment("req-001", cardRequest());

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(response.getProvider()).isEqualTo(ProviderCode.A);
        verify(idempotencyService).checkAndStore(eq("req-001"), anyString());
        verify(providerAConnector, times(1)).authorize(any());
        verify(providerBConnector, never()).authorize(any());
    }

    @Test
    @DisplayName("TC-S02: UPI payment processed through Provider B")
    void createPayment_upi_succeeds_viaProviderB() {
        when(routingEngine.route(com.yuno.payment.orchestrator.domain.PaymentMethod.UPI))
                .thenReturn(new RoutingDecision(
                        com.yuno.payment.orchestrator.domain.ProviderCode.B,
                        com.yuno.payment.orchestrator.domain.ProviderCode.A));
        when(providerBConnector.authorize(any())).thenReturn(PaymentProviderResponse.success("B-ref-001"));
        when(paymentMapper.toResponse(any(), any())).thenReturn(successResponse(ProviderCode.B));

        service.createPayment("req-002", upiRequest());

        verify(providerBConnector, times(1)).authorize(any());
        verify(providerAConnector, never()).authorize(any());
    }

    // ── Retry & Failover (unit-level — mock providers to fail) ───────────────

    @Test
    @DisplayName("TC-S03: Primary A fails twice; failover to B succeeds")
    void createPayment_primaryFails_failoverSucceeds() {
        when(routingEngine.route(any())).thenReturn(new RoutingDecision(
                com.yuno.payment.orchestrator.domain.ProviderCode.A,
                com.yuno.payment.orchestrator.domain.ProviderCode.B));
        when(providerAConnector.authorize(any())).thenReturn(PaymentProviderResponse.failure("A failure"));
        when(providerBConnector.authorize(any())).thenReturn(PaymentProviderResponse.success("B-ref-002"));
        when(paymentMapper.toResponse(any(), any())).thenReturn(successResponse(ProviderCode.B));

        service.createPayment("req-003", cardRequest());

        verify(providerAConnector, times(2)).authorize(any());
        verify(providerBConnector, times(1)).authorize(any());
    }

    @Test
    @DisplayName("TC-S04: Both providers fail — payment marked FAILED")
    void createPayment_allProvidersFail() {
        when(routingEngine.route(any())).thenReturn(new RoutingDecision(
                com.yuno.payment.orchestrator.domain.ProviderCode.A,
                com.yuno.payment.orchestrator.domain.ProviderCode.B));
        when(providerAConnector.authorize(any())).thenReturn(PaymentProviderResponse.failure("A failure"));
        when(providerBConnector.authorize(any())).thenReturn(PaymentProviderResponse.failure("B failure"));
        when(paymentMapper.toResponse(any(), any())).thenReturn(failedResponse());

        PaymentResponse response = service.createPayment("req-004", cardRequest());

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(providerAConnector, times(2)).authorize(any());
        verify(providerBConnector, times(1)).authorize(any());
    }

    // ── Idempotency ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-S05: Duplicate Request-Id throws before any DB write")
    void createPayment_duplicateRequestId_throwsBeforeDbWrite() {
        doThrow(new DuplicateRequestIdException("same requestId"))
                .when(idempotencyService).checkAndStore(anyString(), anyString());

        assertThatThrownBy(() -> service.createPayment("dup-req", cardRequest()))
                .isInstanceOf(DuplicateRequestIdException.class)
                .hasMessage("same requestId");

        verify(paymentRepository, never()).save(any());
        verify(providerAConnector, never()).authorize(any());
    }

    @Test
    @DisplayName("TC-S06: IdempotencyService receives the correct requestId")
    void createPayment_correctRequestIdPassedToIdempotencyService() {
        when(routingEngine.route(any())).thenReturn(new RoutingDecision(
                com.yuno.payment.orchestrator.domain.ProviderCode.A,
                com.yuno.payment.orchestrator.domain.ProviderCode.B));
        when(providerAConnector.authorize(any())).thenReturn(PaymentProviderResponse.success("A-ref"));
        when(paymentMapper.toResponse(any(), any())).thenReturn(successResponse(ProviderCode.A));

        service.createPayment("req-006", cardRequest());

        verify(idempotencyService).checkAndStore(eq("req-006"), anyString());
    }

    // ── getPayment ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-S07: getPayment returns the persisted payment")
    void getPayment_existingPayment() {
        String paymentId = UUID.randomUUID().toString();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(minimalPayment()));
        when(attemptRepository.findByPaymentIdOrderByAttemptNumberAsc(anyString())).thenReturn(List.of());
        when(paymentMapper.toResponse(any(), any())).thenReturn(successResponse(ProviderCode.A));

        PaymentResponse response = service.getPayment(paymentId);

        assertThat(response).isNotNull();
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    @DisplayName("TC-S08: getPayment throws for unknown ID")
    void getPayment_unknownId_throwsPaymentNotFoundException() {
        when(paymentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPayment("missing"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found: missing");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreatePaymentRequest cardRequest() {
        return new CreatePaymentRequest(new BigDecimal("1200.50"), "USD", PaymentMethod.CARD, "ORDER-001");
    }

    private CreatePaymentRequest upiRequest() {
        return new CreatePaymentRequest(new BigDecimal("50.00"), "INR", PaymentMethod.UPI, "ORDER-002");
    }

    private PaymentResponse successResponse(ProviderCode provider) {
        return new PaymentResponse(
                UUID.randomUUID(), new BigDecimal("1200.50"), "USD",
                PaymentMethod.CARD, PaymentStatus.SUCCEEDED, "ORDER-001",
                OffsetDateTime.now(), OffsetDateTime.now(), List.of()
        ).provider(provider);
    }

    private PaymentResponse failedResponse() {
        return new PaymentResponse(
                UUID.randomUUID(), new BigDecimal("1200.50"), "USD",
                PaymentMethod.CARD, PaymentStatus.FAILED, "ORDER-001",
                OffsetDateTime.now(), OffsetDateTime.now(), List.of()
        ).failureReason("B failure");
    }

    private PaymentEntity minimalPayment() {
        return new PaymentEntity(new BigDecimal("1200.50"), "USD",
                com.yuno.payment.orchestrator.domain.PaymentMethod.CARD, "ORDER-001");
    }
}
