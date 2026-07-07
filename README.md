# HighFiveBooks V2

Personal refactoring and Kubernetes migration workspace for the original
HighFiveBooks team project.

## Goal

This repository keeps the original MSA shape and turns the Spring Cloud based
runtime into a Kubernetes-native runtime.

The project does not merge the services into a monolith. The main portfolio
story is:

- refactor `order-server` deeply
- use `coupon-server` as a reference for messaging reliability patterns
- keep `book-server`, `member-server`, and `payment-server` as runtime
  dependencies
- replace Eureka with Kubernetes Service DNS
- replace Config Server with ConfigMap and Secret
- replace Gateway with Ingress
- replace the Thymeleaf frontend with a small React operations console

## Repository Layout

```text
apps/
  console/              React/Vite demo console

services/
  order-server/         Main refactoring target
  coupon-server/        Messaging/idempotency reference and light refactor target
  book-server/          Runtime dependency for stock and book data
  member-server/        Runtime dependency for grade, points, and cart
  payment-server/       Runtime dependency for payment confirmation events

k8s/                    Kubernetes manifests
docs/                   Planning and implementation notes
```

## Excluded Original Services

The original Spring Cloud operational services are intentionally not carried
forward as first-class V2 services:

- `front_server`: replaced by `apps/console`
- `eureka_server`: replaced by Kubernetes Service DNS
- `config_server`: replaced by ConfigMap and Secret
- `gateway`: replaced by Ingress

## Current Baseline

`services/order-server` has the first refactoring baseline applied:

- default `mvn test` no longer attaches the JaCoCo Java agent
- JaCoCo is available through the `coverage` Maven profile
- tests disable Spring Cloud Config and Eureka through `src/test/resources`
- local order service tests pass

```powershell
cd services/order-server
.\mvnw.cmd test
```

Expected result:

```text
98 tests, 0 failures, 0 errors, 0 skipped
```

## Next Priorities

1. Map the order flow and every Feign boundary.
2. Add boundary tests for book/member/coupon/payment calls.
3. Refactor the order transaction boundary.
4. Add RabbitMQ retry and DLQ handling for payment success messages.
5. Add scheduler locking for multi-instance order deployments.
6. Build the local integration environment.
7. Move the runtime to Kubernetes manifests.
8. Connect the React console to real API scenarios.

