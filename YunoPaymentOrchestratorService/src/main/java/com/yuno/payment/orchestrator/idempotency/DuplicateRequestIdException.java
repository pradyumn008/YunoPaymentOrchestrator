package com.yuno.payment.orchestrator.idempotency;

public class DuplicateRequestIdException extends RuntimeException {

    public DuplicateRequestIdException(String message) {
        super(message);
    }
}

