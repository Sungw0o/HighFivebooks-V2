# Runtime Configuration

HighFiveBooks V2 separates local development settings from production
Kubernetes settings with Spring profiles.

## Profiles

```text
local  Local Docker Compose infrastructure
prod   Kubernetes runtime with ConfigMap and Secret values
```

Each backend service keeps a small common `application.yml` and profile-specific
runtime files:

```text
application.yml
application-local.yml
application-prod.yml
```

`local` is the default profile for developer machines. Kubernetes manifests must
set:

```env
SPRING_PROFILES_ACTIVE=prod
```

## Local Ports

```text
book-server     localhost:9002
member-server   localhost:9001
coupon-server   localhost:9004
payment-server  localhost:9005
order-server    localhost:9006
```

Local infrastructure:

```text
MySQL      localhost:3307
RabbitMQ   localhost:5672
Rabbit UI  http://localhost:15672
Redis      localhost:6380
```

## Local Database Split

The local compose stack uses one MySQL container and separate databases per
service.

```text
highfive_order    order_user
highfive_book     book_user
highfive_member   member_user
highfive_coupon   coupon_user
highfive_payment  payment_user
```

## Service Discovery

Eureka and Config Server are disabled in both `local` and `prod`.

Local Feign clients use explicit localhost URLs:

```env
BOOK_SERVICE_URL=http://localhost:9002
MEMBER_SERVICE_URL=http://localhost:9001
COUPON_SERVICE_URL=http://localhost:9004
PAYMENT_SERVICE_URL=http://localhost:9005
ORDER_SERVICE_URL=http://localhost:9006
```

Production Feign clients should use Kubernetes Service DNS:

```env
BOOK_SERVICE_URL=http://book-server:8080
MEMBER_SERVICE_URL=http://member-server:8080
COUPON_SERVICE_URL=http://coupon-server:8080
PAYMENT_SERVICE_URL=http://payment-server:8080
ORDER_SERVICE_URL=http://order-server:8080
```

## Feign Resilience

`order-server` disables implicit Feign retry and relies on explicit timeout,
circuit breaker, and domain compensation paths.

```env
FEIGN_CONNECT_TIMEOUT_MS=1000
FEIGN_READ_TIMEOUT_MS=3000
CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE=20
CIRCUIT_BREAKER_MINIMUM_CALLS=5
CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD=50
CIRCUIT_BREAKER_WAIT_DURATION=10s
CIRCUIT_BREAKER_HALF_OPEN_CALLS=3
```

The retry decision is documented in
[`order-resilience-evidence.md`](order-resilience-evidence.md). The short
version is that stock, coupon, and point state-changing calls should not be
retried implicitly at the Feign layer.

## RabbitMQ Listener Retry

Payment success messages are retried by the Rabbit listener container and then
dead-lettered when retries are exhausted.

```env
RABBIT_LISTENER_RETRY_MAX_ATTEMPTS=3
RABBIT_LISTENER_RETRY_INITIAL_INTERVAL_MS=1000
RABBIT_LISTENER_RETRY_MULTIPLIER=2.0
RABBIT_LISTENER_RETRY_MAX_INTERVAL_MS=10000
```

The payment success queue is configured with a dead-letter exchange and routing
key in `services/order-server`.

## Scheduler Lock

`order-server` uses Redis-backed ShedLock for scheduled jobs. This protects
multi-replica Kubernetes deployments from running the same order cleanup or
status update job on multiple Pods at the same time.

Redis runtime values:

```env
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_DATABASE=0
```

## Local Startup

From the repository root:

```powershell
copy .env.example .env
docker compose up -d mysql rabbitmq redis
```

Then run a backend service with the default `local` profile:

```powershell
cd services/order-server
.\mvnw.cmd spring-boot:run
```

or explicitly:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

## Production Rule

Do not ship `.env` values to production. Kubernetes should inject non-secret
settings through ConfigMaps and sensitive values through Secrets.
