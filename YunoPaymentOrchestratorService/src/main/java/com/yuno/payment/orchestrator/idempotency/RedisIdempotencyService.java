package com.yuno.payment.orchestrator.idempotency;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
//@ConditionalOnBean(StringRedisTemplate.class)
public class RedisIdempotencyService implements IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisIdempotencyService(
            StringRedisTemplate redisTemplate,
            @Value("${idempotency.redis.ttl-hours:24}") long ttlHours) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofHours(ttlHours);
    }

    @Override
    public void checkAndStore(String requestId, String transactionId) {
        Boolean stored = redisTemplate.opsForValue().setIfAbsent(requestId, transactionId, ttl);
        if (!Boolean.TRUE.equals(stored)) {
            throw new DuplicateRequestIdException("same requestId");
        }
    }
}

