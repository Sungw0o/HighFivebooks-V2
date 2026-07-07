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
