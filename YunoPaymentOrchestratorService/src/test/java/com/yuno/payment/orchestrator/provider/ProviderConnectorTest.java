package com.yuno.payment.orchestrator.provider;

import com.yuno.payment.orchestrator.domain.PaymentMethod;
import com.yuno.payment.orchestrator.domain.ProviderCode;
import com.yuno.payment.orchestrator.model.PaymentProviderRequest;
import com.yuno.payment.orchestrator.model.PaymentProviderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Provider connector unit tests")
class ProviderConnectorTest {

    private static final PaymentProviderRequest CARD_REQUEST = new PaymentProviderRequest(
            "pay-001", new BigDecimal("100.00"), "USD", PaymentMethod.CARD, "ORDER-001");

    private static final PaymentProviderRequest UPI_REQUEST = new PaymentProviderRequest(
            "pay-002", new BigDecimal("50.00"), "INR", PaymentMethod.UPI, "ORDER-002");

    // ── Provider A ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-P01: Provider A reports code A")
    void providerA_providerCode_isA() {
        assertThat(new ProviderAConnector().providerCode()).isEqualTo(ProviderCode.A);
    }

    @Test
    @DisplayName("TC-P02: Provider A always returns a successful response with 'A-' reference")
    void providerA_authorize_succeeds() {
        PaymentProviderResponse response = new ProviderAConnector().authorize(CARD_REQUEST);

        assertThat(response.successful()).isTrue();
        assertThat(response.providerReference()).startsWith("A-");
        assertThat(response.failureReason()).isNull();
    }

    @Test
    @DisplayName("TC-P03: Provider A generates a unique reference on each call")
    void providerA_authorize_generatesUniqueReference() {
        String ref1 = new ProviderAConnector().authorize(CARD_REQUEST).providerReference();
        String ref2 = new ProviderAConnector().authorize(CARD_REQUEST).providerReference();
        assertThat(ref1).isNotEqualTo(ref2);
    }

    // ── Provider B ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-P04: Provider B reports code B")
    void providerB_providerCode_isB() {
        assertThat(new ProviderBConnector().providerCode()).isEqualTo(ProviderCode.B);
    }

    @Test
    @DisplayName("TC-P05: Provider B always returns a successful response with 'B-' reference")
    void providerB_authorize_succeeds() {
        PaymentProviderResponse response = new ProviderBConnector().authorize(UPI_REQUEST);

        assertThat(response.successful()).isTrue();
        assertThat(response.providerReference()).startsWith("B-");
        assertThat(response.failureReason()).isNull();
    }

    @Test
    @DisplayName("TC-P06: Provider B generates a unique reference on each call")
    void providerB_authorize_generatesUniqueReference() {
        String ref1 = new ProviderBConnector().authorize(UPI_REQUEST).providerReference();
        String ref2 = new ProviderBConnector().authorize(UPI_REQUEST).providerReference();
        assertThat(ref1).isNotEqualTo(ref2);
    }

    // ── PaymentProviderResponse factories ─────────────────────────────────────

    @Test
    @DisplayName("TC-P07: PaymentProviderResponse.success sets correct fields")
    void paymentProviderResponse_success() {
        PaymentProviderResponse r = PaymentProviderResponse.success("ref-123");
        assertThat(r.successful()).isTrue();
        assertThat(r.providerReference()).isEqualTo("ref-123");
        assertThat(r.failureReason()).isNull();
    }

    @Test
    @DisplayName("TC-P08: PaymentProviderResponse.failure sets correct fields")
    void paymentProviderResponse_failure() {
        PaymentProviderResponse r = PaymentProviderResponse.failure("timeout");
        assertThat(r.successful()).isFalse();
        assertThat(r.providerReference()).isNull();
        assertThat(r.failureReason()).isEqualTo("timeout");
    }
}
