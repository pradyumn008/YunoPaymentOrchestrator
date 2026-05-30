package com.yuno.payment.orchestrator.routing;

import com.yuno.payment.orchestrator.domain.PaymentMethod;
import com.yuno.payment.orchestrator.domain.ProviderCode;
import org.springframework.stereotype.Component;

@Component
public class RoutingEngine {

	public RoutingDecision route(PaymentMethod paymentMethod) {
		return switch (paymentMethod) {
			case CARD -> new RoutingDecision(ProviderCode.A, ProviderCode.B);
			case UPI -> new RoutingDecision(ProviderCode.B, ProviderCode.A);
		};
	}
}
