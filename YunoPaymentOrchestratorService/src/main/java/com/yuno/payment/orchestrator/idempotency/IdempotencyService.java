package com.yuno.payment.orchestrator.idempotency;


public interface IdempotencyService {

    void checkAndStore(String requestId, String transactionId);
}

