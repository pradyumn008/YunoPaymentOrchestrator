package com.yuno.payment.orchestrator.routing;

import com.yuno.payment.orchestrator.domain.ProviderCode;

public record RoutingDecision(
		ProviderCode primaryProvider,
		ProviderCode failoverProvider
) {
}
