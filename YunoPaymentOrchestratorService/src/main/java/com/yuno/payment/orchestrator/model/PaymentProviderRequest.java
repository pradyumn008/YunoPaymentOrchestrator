package com.yuno.payment.orchestrator.model;

import com.yuno.payment.orchestrator.domain.PaymentMethod;

import java.math.BigDecimal;

public record PaymentProviderRequest(
		String paymentId,
		BigDecimal amount,
		String currency,
		PaymentMethod paymentMethod,
		String merchantReferenceId
) {
}
