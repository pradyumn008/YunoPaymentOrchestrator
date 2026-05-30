package com.yuno.payment.orchestrator.provider;

import com.yuno.payment.orchestrator.domain.ProviderCode;
import java.util.UUID;

import com.yuno.payment.orchestrator.model.PaymentProviderRequest;
import com.yuno.payment.orchestrator.model.PaymentProviderResponse;
import org.springframework.stereotype.Component;

@Component
public class ProviderBConnector implements ProviderConnector {

	@Override
	public ProviderCode providerCode() {
		return ProviderCode.B;
	}

	@Override
	public PaymentProviderResponse authorize(PaymentProviderRequest request) {
		return PaymentProviderResponse.success("B-" + UUID.randomUUID());
	}
}
