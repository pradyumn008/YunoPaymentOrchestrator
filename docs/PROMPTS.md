# Development Prompts

This file documents the prompts used during development of the Payment Orchestrator service.


## Prompt 1 — 

```text
Set up the Spring Boot project structure for a payment orchestration system with separate modules for API and Service layers.
```

## Prompt 2 — 

```text
Implement the Create Payment and Fetch Payment REST APIs with proper request validation, error handling, and response structure.
```

## Prompt 3 — 

```text
Implement the routing engine that maps CARD payments to Provider A and UPI payments to Provider B, with retry on the primary provider and failover to the secondary provider.
```

## Prompt 4 — 

```text
Add idempotency handling using the Idempotency-Key request header to prevent duplicate payment creation on client retries.
```

## Prompt 5 —

```text
Create Flyway SQL migrations to define the payments, payment_attempts, and idempotency_records tables with proper indexes and constraints.
```

## Prompt 6 —

```text
Add a simulation mechanism so that provider failures can be triggered on demand during local testing and integration tests without modifying production code.
```

## Prompt 7 — 

```text
Implement a global exception handler that returns structured error responses for validation failures, duplicate idempotency keys, missing payments, and unexpected server errors.
```

## Prompt 8 — 

```text
Write unit tests for the routing engine, provider connectors, and orchestration service. Also write integration tests for the full API flow.
```

## Prompt 9 — 

```text
Define the OpenAPI specification for the payment APIs and configure Gradle to generate server stubs from the contract.
```