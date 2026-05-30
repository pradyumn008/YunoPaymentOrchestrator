# Test Case Documentation — Yuno Payment Orchestrator

## Scope

The tests cover a simplified backend payment orchestration flow:

- Payment creation (CARD and UPI)
- Payment fetch by ID
- Routing by payment method (CARD → Provider A, UPI → Provider B)
- Primary provider retry (up to 2 attempts)
- Secondary provider failover (1 attempt)
- Duplicate Request-Id detection
- Payment status tracking
- Input validation and error responses

---

## Test Classification

### Integration Tests — `PaymentApiIntegrationTests`

Full Spring context with H2 in-memory database (MySQL-compatible mode).

| ID | Scenario | Type | Classification | Expected Result |
| --- | --- | --- | --- | --- |
| TC-001 | Create CARD payment with valid request | Positive | Sanity | HTTP 201, `status=SUCCEEDED`, `provider=A`, 1 attempt |
| TC-002 | Create UPI payment and fetch by ID | Positive | Sanity | HTTP 201 on create; HTTP 200 on fetch with `provider=B` |
| TC-003 | Duplicate `Request-Id` is rejected | Negative | Regression | HTTP 409, message: "same requestId" |
| TC-004 | Invalid create payment request | Negative | Regression | HTTP 400, violations for `amount`, `currency`, `merchantReferenceId` |
| TC-005 | Fetch unknown payment ID (invalid UUID) | Negative | Sanity | HTTP 404, message: "Payment not found: missing-payment" |

---

### Unit Tests — `RoutingEngineTest`

No Spring context. Tests the routing decision logic in isolation.

| ID | Scenario | Type | Classification | Expected Result |
| --- | --- | --- | --- | --- |
| TC-R01 | CARD payment routing | Positive | Sanity | Primary = A, Failover = B |
| TC-R02 | UPI payment routing | Positive | Sanity | Primary = B, Failover = A |
| TC-R03 | Primary ≠ Failover for all payment methods | Positive | Regression | Primary and failover are always distinct providers |

---

### Unit Tests — `ProviderConnectorTest`

No Spring context. Tests `ProviderAConnector` and `ProviderBConnector` in isolation.

| ID | Scenario | Type | Classification | Expected Result |
| --- | --- | --- | --- | --- |
| TC-P01 | Provider A reports code A | Positive | Sanity | `providerCode() = A` |
| TC-P02 | Provider A always returns a successful response | Positive | Sanity | `successful=true`, reference starts with "A-", `failureReason=null` |
| TC-P03 | Provider A generates unique reference each call | Positive | Regression | Two calls produce different references |
| TC-P04 | Provider B reports code B | Positive | Sanity | `providerCode() = B` |
| TC-P05 | Provider B always returns a successful response | Positive | Sanity | `successful=true`, reference starts with "B-", `failureReason=null` |
| TC-P06 | Provider B generates unique reference each call | Positive | Regression | Two calls produce different references |
| TC-P07 | `PaymentProviderResponse.success` factory | Positive | Sanity | `successful=true`, reference set, `failureReason=null` |
| TC-P08 | `PaymentProviderResponse.failure` factory | Negative | Sanity | `successful=false`, `providerReference=null`, reason set |

---

### Unit Tests — `PaymentOrchestrationServiceTest`

Mockito mocks for repositories, routing engine, providers, and mapper.

| ID | Scenario | Type | Classification | Expected Result |
| --- | --- | --- | --- | --- |
| TC-S01 | New CARD payment succeeds via Provider A | Positive | Sanity | `status=SUCCEEDED`, `provider=A`; A×1, B×0 |
| TC-S02 | New UPI payment succeeds via Provider B | Positive | Sanity | `status=SUCCEEDED`, `provider=B`; B×1, A×0 |
| TC-S03 | Primary A fails twice, failover B succeeds | Positive | Integration | `status=SUCCEEDED`, `provider=B`; A×2, B×1 |
| TC-S04 | Both providers fail | Negative | Integration | `status=FAILED`; A×2, B×1 |
| TC-S05 | Duplicate `Request-Id` throws before any DB write | Negative | Regression | `DuplicateRequestIdException`; no save, no provider call |
| TC-S06 | Correct `Request-Id` passed to idempotency service | Positive | Regression | `idempotencyService.checkAndStore` receives the exact request ID |
| TC-S07 | `getPayment` for existing payment | Positive | Sanity | Returns persisted payment with attempt history |
| TC-S08 | `getPayment` for unknown ID | Negative | Sanity | `PaymentNotFoundException` thrown |

---

## Additional Manual / Future-Automation Scenarios

| ID | Scenario | Type | Classification | Expected Result |
| --- | --- | --- | --- | --- |
| TC-011 | Missing `Request-Id` header | Negative | Regression | HTTP 400, "Missing required header: Request-Id" |
| TC-012 | Amount is zero | Negative | Regression | HTTP 400, `violations.amount` |
| TC-013 | Amount has more than two decimal places | Negative | Regression | HTTP 400 |
| TC-014 | Currency is lowercase / not three letters | Negative | Regression | HTTP 400, `violations.currency` |
| TC-015 | Merchant reference is blank | Negative | Regression | HTTP 400, `violations.merchantReferenceId` |
| TC-016 | UPI primary B fails (mocked), failover A succeeds | Positive | Integration | B×2 FAILED, A×1 SUCCEEDED, `provider=A` |
| TC-017 | Provider attempt audit trail ordering | Positive | Regression | Attempts in ascending `attemptNumber` order |
| TC-018 | Flyway migration on empty database | Positive | Integration | All tables created; JPA schema validation passes |

---

## Automated Test Coverage Summary

```
src/test/java/
├── com/yuno/payment/orchestrator/
│   ├── ApplicationTests.java                         (Spring context smoke test)
│   ├── payment/
│   │   └── PaymentApiIntegrationTests.java           (5 integration tests)
│   ├── routing/
│   │   └── RoutingEngineTest.java                    (3 unit tests)
│   ├── provider/
│   │   └── ProviderConnectorTest.java                (8 unit tests)
│   └── service/
│       └── PaymentOrchestrationServiceTest.java      (8 unit tests)

Total: 25 automated tests — all passing
```

Run all tests:

```bash
./gradlew test
```

---

## Manual Test Examples

### TC-001 CARD Payment — Success

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H 'Content-Type: application/json' \
  -H 'Request-Id: tc-001' \
  -d '{
    "amount": 1200.50,
    "currency": "USD",
    "paymentMethod": "CARD",
    "merchantReferenceId": "ORDER-TC-001"
  }'
```

Expected: HTTP `201`, `status=SUCCEEDED`, `provider=A`, `attempts[0].status=SUCCEEDED`

---

### TC-003 Duplicate Request-Id

Send the exact same request twice with the same `Request-Id`.

- First call → HTTP `201`
- Second call → HTTP `409`, message: "same requestId"

---

### TC-004 Invalid Request

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H 'Content-Type: application/json' \
  -H 'Request-Id: tc-004' \
  -d '{
    "amount": 0,
    "currency": "usd",
    "paymentMethod": "CARD",
    "merchantReferenceId": ""
  }'
```

Expected: HTTP `400`, violations for `amount`, `currency`, `merchantReferenceId`

---

### TC-005 Unknown Payment ID

```bash
curl http://localhost:8080/api/v1/payments/missing-payment
```

Expected: HTTP `404`, message: "Payment not found: missing-payment"

---

## Performance Considerations

| Metric | Notes |
| --- | --- |
| DB writes per create (success, 1 attempt) | 3 (payment + attempt + idempotency record) |
| DB writes per create (2 retries + failover success) | 5 (payment + 3 attempts + idempotency record) |
| DB reads per create | 1 (idempotency lookup) |
| DB reads per fetch | 2 (payment + attempts) |
| Provider latency (stubs) | < 1 ms (in-memory); real providers add network I/O |
| Index coverage | `payments(status)`, `payments(merchant_reference_id)`, `payment_attempts(payment_id)` |
| Connection pooling | HikariCP (Spring Boot default); tune `spring.datasource.hikari.maximum-pool-size` for production load |
| Transaction scope | `createPayment` runs in a single `@Transactional` boundary; all writes commit atomically |
