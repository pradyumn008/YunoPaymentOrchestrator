# Payment Orchestrator

Simplified backend payment orchestration system inspired by Yuno-style routing. The service exposes APIs to create and fetch payments, routes CARD payments to Provider A and UPI payments to Provider B, retries the primary provider, fails over to the secondary provider, tracks status, and enforces idempotency via Redis-backed `Request-Id` deduplication.

Public GitHub repository link: https://github.com/pradyumn008/YunoPaymentOrchestrator

## Technology Stack

- Java 21
- Spring Boot 4
- Spring WebMVC
- Spring Data JPA
- MySQL
- Redis (idempotency)
- Flyway
- H2 for automated tests
- Gradle

## High-Level Architecture

Client requests enter through `PaymentsController`, which validates API input and delegates to `PaymentOrchestrationService`. The service checks the Redis-backed idempotency store (using the `Request-Id` header), creates a payment record, asks `RoutingEngine` for the primary and failover providers, calls provider connector stubs, records every provider attempt, and persists final payment status.

Flow:

```text
Client
  -> Controller Layer (PaymentsController)
  -> Service Layer (PaymentOrchestrationService)
  -> Routing Engine
  -> Provider Connectors A/B
  -> Persistence Layer (MySQL via JPA)
  -> Idempotency Store (Redis)
```

## Functional Requirements

- Create Payment API accepts amount, currency, payment method, and merchant reference.
- Fetch Payment API returns current payment status and provider attempts.
- Routing maps `CARD -> Provider A` and `UPI -> Provider B`.
- Retry policy tries the primary provider twice.
- Failover policy tries the secondary provider once after primary retry failure.
- Idempotency is enforced with the `Request-Id` request header via Redis (24-hour TTL). Duplicate `Request-Id` values are rejected with HTTP 409.
- Payment status is persisted as `INITIATED`, `PROCESSING`, `SUCCEEDED`, or `FAILED`.

## Non-Functional Requirements

- Idempotency prevents duplicate payment creation for retried client requests.
- Flyway owns schema changes so environments receive deterministic migrations.
- JPA runs in `validate` mode to detect entity/schema drift at startup.
- Provider attempts are recorded for auditability and troubleshooting.
- Input validation rejects invalid amount, currency, method, reference, and missing `Request-Id` header.
- Health and metrics endpoints are exposed through Spring Actuator.

## Integration Points

- MySQL stores payments and provider attempts.
- Redis stores idempotency keys with a configurable TTL (default 24 hours).
- Flyway applies SQL migrations from `src/main/resources/db/migration`.
- Provider A and Provider B are implemented as local connector stubs that always succeed. They can be replaced with HTTP clients for real provider integrations.

## Installation

Prerequisites:

- JDK 21
- MySQL 8 or compatible
- Redis 6 or compatible

Create the database:

```sql
CREATE DATABASE payment_orchestrator;
```

Configure credentials with environment variables or edit `YunoPaymentOrchestratorService/src/main/resources/application.properties`:

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/payment_orchestrator?createDatabaseIfNotExist=true'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='password'
export SPRING_REDIS_HOST='localhost'
export SPRING_REDIS_PORT='6379'
```

Install dependencies and run tests:

```bash
./gradlew test
```

## Execution

Start the service:

```bash
./gradlew bootRun
```

The API runs on:

```text
http://localhost:9091
```

Swagger UI is available when the app is running:

```text
http://localhost:9091/swagger-ui/index.html
```

## API Guide

Create payment:

```bash
curl -X POST http://localhost:9091/api/v1/payments \
  -H 'Content-Type: application/json' \
  -H 'Request-Id: demo-req-001' \
  -d '{
    "amount": 1200.50,
    "currency": "USD",
    "paymentMethod": "CARD",
    "merchantReferenceId": "ORDER-1001"
  }'
```

Fetch payment:

```bash
curl http://localhost:9091/api/v1/payments/{paymentId}
```

## Input Parameters

Create Payment:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `Request-Id` | Header | Yes | Client-supplied unique request identifier. Max 128 characters. Each value may only be used once. |
| `amount` | Decimal | Yes | Must be at least `0.01`, max 2 fraction digits. |
| `currency` | String | Yes | Three-letter uppercase ISO-4217 code. |
| `paymentMethod` | Enum | Yes | `CARD` or `UPI`. |
| `merchantReferenceId` | String | Yes | Merchant order/reference identifier. Max 100 characters. |

Output:

| Field | Description |
| --- | --- |
| `paymentId` | System-generated UUID payment identifier. |
| `amount` | Payment amount. |
| `currency` | ISO-4217 currency code. |
| `paymentMethod` | `CARD` or `UPI`. |
| `status` | Current payment status (`INITIATED`, `PROCESSING`, `SUCCEEDED`, `FAILED`). |
| `merchantReferenceId` | Merchant reference from the request. |
| `provider` | Provider that finally processed the payment (`A` or `B`), if any. |
| `providerPaymentId` | Provider reference, if successful. |
| `failureReason` | Failure reason when payment fails. |
| `createdAt` | Payment creation timestamp. |
| `updatedAt` | Last update timestamp. |
| `attempts` | Ordered provider attempt history. |

## Performance Considerations

- Idempotency lookup is a Redis `SETNX` operation (O(1)).
- Fetch payment is a primary-key read plus indexed attempt lookup on `payment_attempts.payment_id`.
- `merchant_reference_id` and `status` are indexed for operational search and dashboards.
- Provider calls are currently local stubs that always succeed. Real HTTP connectors should use timeouts, circuit breakers, connection pooling, and bounded retries.
- Metrics can be captured through `/actuator/metrics`, including JVM, HTTP server, and datasource metrics.

## Documentation

- Test case documentation: `docs/TEST_CASES.md`
- Development prompts: `docs/PROMPTS.md`
