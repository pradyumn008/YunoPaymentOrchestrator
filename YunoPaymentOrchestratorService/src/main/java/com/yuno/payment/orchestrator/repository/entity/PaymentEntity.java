package com.yuno.payment.orchestrator.repository.entity;

import com.yuno.payment.orchestrator.domain.PaymentMethod;
import com.yuno.payment.orchestrator.domain.PaymentStatus;
import com.yuno.payment.orchestrator.domain.ProviderCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "transactions")
public class PaymentEntity implements Persistable<String> {

	@Id
	@Column(length = 36)
	private String id;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 20)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "merchant_reference_id", nullable = false, length = 100)
	private String merchantReferenceId;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private ProviderCode provider;

	@Column(name = "provider_payment_id", length = 100)
	private String providerPaymentId;

	@Column(name = "failure_reason", length = 255)
	private String failureReason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Transient
	private boolean isNew = true;

	public PaymentEntity() {
	}

	public PaymentEntity(BigDecimal amount, String currency, PaymentMethod paymentMethod,
			String merchantReferenceId) {
		this.id = UUID.randomUUID().toString();
		this.amount = amount;
		this.currency = currency;
		this.paymentMethod = paymentMethod;
		this.status = PaymentStatus.INITIATED;
		this.merchantReferenceId = merchantReferenceId;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = Instant.now();
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}

	@Override
	public String getId() { return id; }

	@Override
	public boolean isNew() { return isNew; }

	public BigDecimal getAmount() { return amount; }

	public String getCurrency() { return currency; }

	public PaymentMethod getPaymentMethod() { return paymentMethod; }

	public PaymentStatus getStatus() { return status; }

	public void setStatus(PaymentStatus status) { this.status = status; }

	public String getMerchantReferenceId() { return merchantReferenceId; }

	public ProviderCode getProvider() { return provider; }

	public void setProvider(ProviderCode provider) { this.provider = provider; }

	public String getProviderPaymentId() { return providerPaymentId; }

	public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

	public String getFailureReason() { return failureReason; }

	public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

	public Instant getCreatedAt() { return createdAt; }

	public Instant getUpdatedAt() { return updatedAt; }
}
