package com.yuno.payment.orchestrator.routing;

import com.yuno.payment.orchestrator.domain.PaymentMethod;
import com.yuno.payment.orchestrator.domain.ProviderCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RoutingEngine}.
 *
 * <p>Classification: <b>Sanity / Regression</b>
 *
 * <p>Verifies that the routing engine maps each payment method to the correct
 * primary and failover provider without any Spring context overhead.
 */
@DisplayName("RoutingEngine unit tests")
class RoutingEngineTest {

    private final RoutingEngine routingEngine = new RoutingEngine();

    // -------------------------------------------------------------------------
    // CARD routing
    // -------------------------------------------------------------------------

    /**
     * TC-R01 | CARD → primary = A, failover = B (Sanity)
     */
    @Test
    @DisplayName("TC-R01: CARD payment routes primary to Provider A and failover to Provider B")
    void route_cardPayment_primaryIsProviderA_failoverIsProviderB() {
        RoutingDecision decision = routingEngine.route(PaymentMethod.CARD);

        assertThat(decision.primaryProvider())
                .as("CARD primary provider")
                .isEqualTo(ProviderCode.A);
        assertThat(decision.failoverProvider())
                .as("CARD failover provider")
                .isEqualTo(ProviderCode.B);
    }

    // -------------------------------------------------------------------------
    // UPI routing
    // -------------------------------------------------------------------------

    /**
     * TC-R02 | UPI → primary = B, failover = A (Sanity)
     */
    @Test
    @DisplayName("TC-R02: UPI payment routes primary to Provider B and failover to Provider A")
    void route_upiPayment_primaryIsProviderB_failoverIsProviderA() {
        RoutingDecision decision = routingEngine.route(PaymentMethod.UPI);

        assertThat(decision.primaryProvider())
                .as("UPI primary provider")
                .isEqualTo(ProviderCode.B);
        assertThat(decision.failoverProvider())
                .as("UPI failover provider")
                .isEqualTo(ProviderCode.A);
    }

    // -------------------------------------------------------------------------
    // Structural invariants
    // -------------------------------------------------------------------------

    /**
     * TC-R03 | Primary and failover providers are always distinct (Regression)
     */
    @Test
    @DisplayName("TC-R03: Primary and failover providers are never the same")
    void route_primaryAndFailoverAreAlwaysDistinct() {
        for (PaymentMethod method : PaymentMethod.values()) {
            RoutingDecision decision = routingEngine.route(method);
            assertThat(decision.primaryProvider())
                    .as("Primary and failover must differ for method %s", method)
                    .isNotEqualTo(decision.failoverProvider());
        }
    }
}

