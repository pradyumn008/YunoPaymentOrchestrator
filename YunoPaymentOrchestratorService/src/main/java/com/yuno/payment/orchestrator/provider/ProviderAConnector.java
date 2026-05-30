package com.yuno.payment.orchestrator.provider;

import com.yuno.payment.orchestrator.domain.ProviderCode;
import java.util.UUID;

import com.yuno.payment.orchestrator.model.PaymentProviderRequest;
import com.yuno.payment.orchestrator.model.PaymentProviderResponse;
import org.springframework.stereotype.Component;

@Component
public class ProviderAConnector implements ProviderConnector {

	@Override
	public ProviderCode providerCode() {
		return ProviderCode.A;
	}

	@Override
	public PaymentProviderResponse authorize(PaymentProviderRequest request) {
		return PaymentProviderResponse.success("A-" + UUID.randomUUID());
	}
}
