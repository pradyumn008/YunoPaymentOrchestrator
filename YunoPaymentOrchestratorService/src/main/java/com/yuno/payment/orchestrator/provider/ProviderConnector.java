package com.yuno.payment.orchestrator.provider;

import com.yuno.payment.orchestrator.domain.ProviderCode;
import com.yuno.payment.orchestrator.model.PaymentProviderRequest;
import com.yuno.payment.orchestrator.model.PaymentProviderResponse;

public interface ProviderConnector {

	ProviderCode providerCode();

	PaymentProviderResponse authorize(PaymentProviderRequest request);
}
