package com.yuno.payment.orchestrator.api;

import com.yuno.payment.orchestrator.api.model.CreatePaymentRequest;
import com.yuno.payment.orchestrator.api.model.PaymentResponse;
import com.yuno.payment.orchestrator.service.PaymentOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PaymentsController implements PaymentsApi {

    private final PaymentOrchestrationService paymentOrchestrationService;

    public PaymentsController(PaymentOrchestrationService paymentOrchestrationService) {
        this.paymentOrchestrationService = paymentOrchestrationService;
    }

    @Override
    public ResponseEntity<PaymentResponse> createPayment(String requestId, CreatePaymentRequest createPaymentRequest) {
        PaymentResponse response = paymentOrchestrationService.createPayment(requestId, createPaymentRequest);
        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<PaymentResponse> getPayment(UUID paymentId) {
        return ResponseEntity.ok(paymentOrchestrationService.getPayment(paymentId.toString()));
    }
}
