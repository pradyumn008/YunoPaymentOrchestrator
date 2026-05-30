package com.yuno.payment.orchestrator;

import com.yuno.payment.orchestrator.idempotency.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Spring context smoke test.
 *
 * <p>Verifies that the application context starts without errors in the test profile.
 * The {@link IdempotencyService} is mocked because the test profile excludes Redis
 * autoconfiguration (so no {@code StringRedisTemplate} / {@code RedisIdempotencyService}
 * is created).
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

	@MockitoBean
	IdempotencyService idempotencyService;

	@Test
	void contextLoads() {
	}

}
