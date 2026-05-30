package com.yuno.payment.orchestrator.service;

import com.yuno.payment.orchestrator.api.model.PaymentAttemptResponse;
import com.yuno.payment.orchestrator.api.model.PaymentMethod;
import com.yuno.payment.orchestrator.api.model.PaymentResponse;
import com.yuno.payment.orchestrator.api.model.PaymentStatus;
import com.yuno.payment.orchestrator.api.model.ProviderCode;
import com.yuno.payment.orchestrator.repository.entity.PaymentAttemptEntity;
import com.yuno.payment.orchestrator.repository.entity.PaymentEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

	public PaymentResponse toResponse(PaymentEntity payment, List<PaymentAttemptEntity> attempts) {
		return new PaymentResponse(
				UUID.fromString(payment.getId()),
				payment.getAmount(),
				payment.getCurrency(),
				toApi(payment.getPaymentMethod()),
				toApi(payment.getStatus()),
				payment.getMerchantReferenceId(),
				toOffsetDateTime(payment.getCreatedAt()),
				toOffsetDateTime(payment.getUpdatedAt()),
				attempts.stream().map(this::toAttemptResponse).toList()
		)
				.provider(toApi(payment.getProvider()))
				.providerPaymentId(payment.getProviderPaymentId())
				.failureReason(payment.getFailureReason());
	}

	private PaymentAttemptResponse toAttemptResponse(PaymentAttemptEntity attempt) {
		return new PaymentAttemptResponse(
				attempt.getAttemptNumber(),
				toApi(attempt.getProvider()),
				toApi(attempt.getStatus()),
				toOffsetDateTime(attempt.getAttemptedAt())
		)
				.providerReference(attempt.getProviderReference())
				.failureReason(attempt.getFailureReason());
	}

	private PaymentMethod toApi(com.yuno.payment.orchestrator.domain.PaymentMethod paymentMethod) {
		return PaymentMethod.valueOf(paymentMethod.name());
	}

	private PaymentStatus toApi(com.yuno.payment.orchestrator.domain.PaymentStatus status) {
		return PaymentStatus.valueOf(status.name());
	}

	private ProviderCode toApi(com.yuno.payment.orchestrator.domain.ProviderCode provider) {
		return provider == null ? null : ProviderCode.valueOf(provider.name());
	}

	private OffsetDateTime toOffsetDateTime(Instant instant) {
		return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}
