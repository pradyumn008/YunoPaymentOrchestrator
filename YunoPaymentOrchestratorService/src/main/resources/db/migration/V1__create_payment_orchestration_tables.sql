-- ============================================================
-- V1: Initial schema
-- Tables: transactions, payment_attempts
-- Idempotency is handled by Redis (no DB table required)
-- ============================================================

CREATE TABLE transactions (
    id VARCHAR(36) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    merchant_reference_id VARCHAR(100) NOT NULL,
    provider VARCHAR(20),
    provider_payment_id VARCHAR(100),
    failure_reason VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_transactions_merchant_reference_id ON transactions (merchant_reference_id);
CREATE INDEX idx_transactions_status ON transactions (status);

CREATE TABLE payment_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id VARCHAR(36) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    attempt_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_reference VARCHAR(100),
    failure_reason VARCHAR(255),
    attempted_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payment_attempts_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id)
);

CREATE INDEX idx_payment_attempts_transaction_id ON payment_attempts (transaction_id);
