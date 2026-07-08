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
- replace the Thymeleaf frontend with a React storefront

## Repository Layout

```text
apps/
  storefront/           React/Vite user storefront

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

- `front_server`: replaced by `apps/storefront`
- `eureka_server`: replaced by Kubernetes Service DNS
- `config_server`: replaced by ConfigMap and Secret
- `gateway`: replaced by Ingress

## Current Refactoring Status

`services/order-server` is the main backend refactoring target. The current
state focuses on reducing distributed-system failure modes without collapsing
the MSA boundaries.

- the order flow and Feign boundaries are mapped
- Feign contract tests cover the main book/member/coupon/payment client calls
- order DB mutations are separated from external Feign/Rabbit orchestration
- payment success messages use RabbitMQ retry and DLQ isolation
- scheduled order jobs use Redis-backed ShedLock for multi-instance safety
- Feign default retry is disabled; timeout and circuit breaker settings are explicit
- K8s manifests replace Eureka/Config Server/Gateway with Service DNS, ConfigMap/Secret, and Ingress

More detail:

- [`docs/order-flow-boundary-map.md`](docs/order-flow-boundary-map.md)
- [`docs/order-resilience-evidence.md`](docs/order-resilience-evidence.md)
- [`docs/k8s-transition-runbook.md`](docs/k8s-transition-runbook.md)

## Verification

```powershell
cd services/order-server
.\mvnw.cmd test
```

Expected result:

```text
126 tests, 0 failures, 0 errors, 0 skipped
```

Render the base Kubernetes manifests:

```powershell
kubectl kustomize k8s/base
```

Run the local K8s smoke check after applying the manifests:

```powershell
.\scripts\k8s-smoke.ps1
```

## Local Infrastructure

Local development uses one MySQL container with separate databases per service,
plus shared RabbitMQ and Redis containers.

```powershell
copy .env.example .env
docker compose up -d mysql rabbitmq redis
```

Default local endpoints:

```text
MySQL:     localhost:3307
RabbitMQ:  localhost:5672
Rabbit UI: http://localhost:15672
Redis:     localhost:6380
```

The MySQL init script creates these service databases:

```text
highfive_order
highfive_book
highfive_member
highfive_coupon
highfive_payment
```

The default credentials in `.env.example` are local-only placeholders.
Kubernetes must use generated Secret values instead.

Runtime profile details are tracked in
[`docs/runtime-config.md`](docs/runtime-config.md).

## Portfolio Evidence

The main engineering story is:

1. Kept the original MSA shape instead of merging services into a monolith.
2. Reduced DB transaction scope in `order-server` so external Feign/Rabbit I/O is not hidden inside broad service-level transactions.
3. Added Feign boundary tests to make external API contracts visible and regression-resistant.
4. Isolated RabbitMQ poison messages with retry and DLQ policies for payment success events.
5. Protected scheduled order jobs from duplicate execution when `order-server` runs with multiple replicas.
6. Moved the runtime direction from Spring Cloud operational services to Kubernetes-native primitives.

Interview version:

```text
I focused the refactoring on the order domain because it coordinates payment,
stock, coupon, and point state changes. I separated DB mutations from external
I/O, disabled implicit Feign retries for non-idempotent calls, added RabbitMQ
retry/DLQ for asynchronous payment success events, and protected scheduled jobs
with Redis-backed ShedLock for multi-replica Kubernetes deployments.
```

## Remaining Priorities

1. Capture K8s smoke check output and screenshots/log snippets as final portfolio evidence.
2. Finish the React/Vite storefront integration against the real backend contracts.
3. Add README screenshots or diagrams for the order flow and K8s transition.
4. Optionally split ArgoCD/Argo Rollouts experiments into a separate deployment evidence section.
