package com.yuno.payment.orchestrator.model;

public record PaymentProviderResponse(
		boolean successful,
		String providerReference,
		String failureReason
) {

	public static PaymentProviderResponse success(String providerReference) {
		return new PaymentProviderResponse(true, providerReference, null);
	}

	public static PaymentProviderResponse failure(String failureReason) {
		return new PaymentProviderResponse(false, null, failureReason);
	}
}
