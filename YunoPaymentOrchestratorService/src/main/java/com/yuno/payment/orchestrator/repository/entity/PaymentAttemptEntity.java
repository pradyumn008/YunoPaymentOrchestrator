package com.yuno.payment.orchestrator.repository.entity;

import com.yuno.payment.orchestrator.domain.PaymentStatus;
import com.yuno.payment.orchestrator.domain.ProviderCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttemptEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** FK → {@code transactions.id} */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transaction_id", nullable = false)
	private PaymentEntity payment;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProviderCode provider;

	@Column(name = "attempt_number", nullable = false)
	private int attemptNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "provider_reference", length = 100)
	private String providerReference;

	@Column(name = "failure_reason", length = 255)
	private String failureReason;

	@Column(name = "attempted_at", nullable = false)
	private Instant attemptedAt;

	public PaymentAttemptEntity() {
	}

	public PaymentAttemptEntity(PaymentEntity payment, ProviderCode provider, int attemptNumber, PaymentStatus status,
			String providerReference, String failureReason) {
		this.payment = payment;
		this.provider = provider;
		this.attemptNumber = attemptNumber;
		this.status = status;
		this.providerReference = providerReference;
		this.failureReason = failureReason;
	}

	@PrePersist
	void prePersist() {
		this.attemptedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public PaymentEntity getPayment() {
		return payment;
	}

	public ProviderCode getProvider() {
		return provider;
	}

	public int getAttemptNumber() {
		return attemptNumber;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public String getProviderReference() {
		return providerReference;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public Instant getAttemptedAt() {
		return attemptedAt;
	}
}
