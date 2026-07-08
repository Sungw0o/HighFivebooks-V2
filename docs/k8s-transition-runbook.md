# HighFiveBooks K8s Transition Runbook

이 문서는 HighFiveBooks V2를 로컬 Kubernetes 환경에서 검증하기 위한 실행 절차다. 목표는 상시 운영이 아니라 Spring Cloud 기반 MSA를 Kubernetes-native 구성으로 전환할 수 있음을 확인하는 것이다.

## 전환 원칙

- Eureka는 Kubernetes Service DNS로 대체한다.
- Config Server는 ConfigMap과 Secret으로 대체한다.
- Gateway는 Ingress로 대체한다.
- `.env` 값은 클러스터에 직접 넣지 않고 ConfigMap/Secret으로 분리한다.
- `k8s/base/secret.example.yaml`은 예시 파일이다. 실제 배포 전에는 값을 반드시 교체한다.

## 구성

```text
k8s/base
  namespace.yaml
  configmap.yaml
  secret.example.yaml
  mysql.yaml
  redis.yaml
  rabbitmq.yaml
  elasticsearch.yaml
  minio.yaml
  apps.yaml
  ingress.yaml
```

애플리케이션 Deployment:

```text
book-server
member-server
coupon-server
payment-server
order-server
```

인프라 StatefulSet:

```text
mysql
redis
rabbitmq
elasticsearch
minio
```

## 이미지 준비

기본 매니페스트는 아래 이미지 이름을 사용한다.

```text
ghcr.io/sungw0o/highfivebooks-v2/book-server:latest
ghcr.io/sungw0o/highfivebooks-v2/member-server:latest
ghcr.io/sungw0o/highfivebooks-v2/coupon-server:latest
ghcr.io/sungw0o/highfivebooks-v2/payment-server:latest
ghcr.io/sungw0o/highfivebooks-v2/order-server:latest
```

로컬 kind에서 직접 빌드한다면 같은 태그로 빌드하고 kind에 로드한다.

```powershell
docker build -t ghcr.io/sungw0o/highfivebooks-v2/order-server:latest services/order-server
kind load docker-image ghcr.io/sungw0o/highfivebooks-v2/order-server:latest
```

Elasticsearch는 한글 검색 분석을 위해 `analysis-nori` 플러그인이 포함된 커스텀 이미지를 사용한다.

```powershell
docker build -t ghcr.io/sungw0o/highfivebooks-v2/elasticsearch-nori:8.18.8 infra/elasticsearch
kind load docker-image ghcr.io/sungw0o/highfivebooks-v2/elasticsearch-nori:8.18.8 --name highfivebooks
```

## 적용

```powershell
kubectl apply -k k8s/base
kubectl -n highfivebooks get pods
kubectl -n highfivebooks get svc
kubectl -n highfivebooks get ingress
```

Secret 예시 값은 실제 값으로 교체한다.

```powershell
kubectl -n highfivebooks edit secret highfivebooks-secret
```

또는 `secret.example.yaml`을 복사해 별도 Secret manifest로 관리한다.

## ConfigMap 확인

`k8s/base/configmap.yaml`은 런타임 전환의 핵심 증거다.

- `SPRING_PROFILES_ACTIVE=prod`
- `BOOK_SERVICE_URL=http://book-server:8080`
- `MEMBER_SERVICE_URL=http://member-server:8080`
- `COUPON_SERVICE_URL=http://coupon-server:8080`
- `PAYMENT_SERVICE_URL=http://payment-server:8080`
- `ORDER_SERVICE_URL=http://order-server:8080`
- `FEIGN_CONNECT_TIMEOUT_MS=1000`
- `FEIGN_READ_TIMEOUT_MS=3000`
- `RABBIT_LISTENER_RETRY_MAX_ATTEMPTS=3`
- `RABBIT_LISTENER_RETRY_INITIAL_INTERVAL_MS=1000`
- `RABBIT_LISTENER_RETRY_MULTIPLIER=2.0`
- `RABBIT_LISTENER_RETRY_MAX_INTERVAL_MS=10000`

로컬 전환 검증에서는 빈 MySQL 초기화를 위해 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`를 사용한다. 운영에서는 migration 도구와 `validate` 조합으로 바꾸는 것이 안전하다.

## 로컬 Ingress

Ingress host는 `highfivebooks.local`이다. 로컬 테스트에서는 hosts 파일에 ingress IP를 연결한다.

```text
127.0.0.1 highfivebooks.local
```

kind에서 nginx ingress를 쓰는 경우 ingress controller 설치가 먼저 필요하다.

## Smoke Check

기본 배포 검증은 아래 스크립트로 실행한다.

```powershell
.\scripts\k8s-smoke.ps1
```

스크립트가 확인하는 것:

- namespace 존재
- MySQL, Redis, RabbitMQ, Elasticsearch, MinIO StatefulSet rollout 완료
- 5개 백엔드 Deployment rollout 완료
- 각 Service EndpointSlice 존재
- order-server의 `BOOK_SERVICE_URL`이 Kubernetes Service DNS를 사용
- order-server의 JPA ddl-auto 전환 검증값 확인
- order-server MySQL/RabbitMQ 연결 로그 존재
- Elasticsearch `analysis-nori` 플러그인과 한글 분석기 동작 확인
- 5개 백엔드 서비스의 내부 Service DNS readiness 응답 확인

일부 서비스의 `/actuator/health` 전체 상태는 외부 dependency health indicator 때문에 `DOWN`일 수 있다. 배포 생존성 검증에서는 readiness endpoint를 우선 확인한다.

## 수동 확인

```powershell
kubectl -n highfivebooks rollout status deploy/order-server
kubectl -n highfivebooks logs deploy/order-server
kubectl -n highfivebooks exec deploy/order-server -- printenv SPRING_PROFILES_ACTIVE
kubectl -n highfivebooks exec deploy/order-server -- printenv BOOK_SERVICE_URL
kubectl -n highfivebooks exec deploy/order-server -- printenv FEIGN_CONNECT_TIMEOUT_MS
kubectl -n highfivebooks exec deploy/order-server -- printenv RABBIT_LISTENER_RETRY_MAX_ATTEMPTS
```

확인해야 할 값:

- `SPRING_PROFILES_ACTIVE=prod`
- `BOOK_SERVICE_URL=http://book-server:8080`
- `FEIGN_CONNECT_TIMEOUT_MS=1000`
- `RABBIT_LISTENER_RETRY_MAX_ATTEMPTS=3`

## RabbitMQ DLQ 확인

order-server는 `payment-success-queue`에 dead-letter exchange와 routing key를 선언한다. 실패 메시지는 listener retry가 끝난 뒤 DLQ로 격리된다.

확인 항목:

- `payment-success-queue` arguments에 `x-dead-letter-exchange=high-five-order-dead-letter-exchange`
- `payment-success-queue` arguments에 `x-dead-letter-routing-key=high-five.order.payment.dead.letter`
- `high-five-order-payment-dead-letter-queue` 존재

RabbitMQ management UI를 열 수 있다면 queue arguments를 UI에서 확인한다. CLI로 확인할 때는 RabbitMQ Pod 안에서 `rabbitmqctl list_queues name arguments`를 사용한다.

```powershell
kubectl -n highfivebooks exec statefulset/rabbitmq -- rabbitmqctl list_queues name arguments
```

## Scheduler Lock 확인

order-server는 Redis 기반 ShedLock을 사용한다.

- lock namespace: `highfivebooks:order`
- 결제 대기 만료 주문 정리 lock: `order.cancelExpiredOrders`
- 일일 주문 상태 변경 lock: `order.dailyStatusUpdate`

replica를 2개 이상으로 늘려도 같은 시점의 스케줄러 작업은 하나의 Pod에서만 실행되어야 한다.

```powershell
kubectl -n highfivebooks scale deploy/order-server --replicas=2
kubectl -n highfivebooks rollout status deploy/order-server
kubectl -n highfivebooks logs deploy/order-server --since=15m
```

Redis 키는 환경과 ShedLock 버전에 따라 표현이 다를 수 있으므로, 운영 검증에서는 애플리케이션 로그와 중복 처리 결과가 없는지를 함께 확인한다.

## Feign 장애 격리 확인

order-server prod 프로필은 Feign timeout과 circuit breaker를 명시한다. Feign 기본 retry는 코드에서 비활성화되어 있다.

확인 항목:

- `FEIGN_CONNECT_TIMEOUT_MS=1000`
- `FEIGN_READ_TIMEOUT_MS=3000`
- `CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD=50`
- 주문 흐름의 재고/쿠폰/포인트 상태 변경 호출은 Feign 자동 retry 대상이 아님

관련 근거는 `docs/order-resilience-evidence.md`에 정리되어 있다.

## 다음 실험

- ArgoCD Application manifest로 `k8s/base` sync
- Argo Rollouts로 order-server canary 또는 blue-green 배포 실험
- smoke check 결과와 주요 로그를 포트폴리오 증거로 캡처

## ArgoCD

ArgoCD가 설치되어 있다면 아래 Application으로 `k8s/base`를 sync한다.

```powershell
kubectl apply -f k8s/gitops/argocd-application.yaml
argocd app sync highfivebooks
argocd app wait highfivebooks
```

## Argo Rollouts

`k8s/base/apps.yaml`의 `order-server` Deployment와 `k8s/rollouts/order-server-rollout.yaml`은 같은 workload 이름을 사용한다. Rollout 실험 시에는 Deployment를 scale down한 뒤 Rollout을 적용한다.

```powershell
kubectl -n highfivebooks scale deploy/order-server --replicas=0
kubectl apply -f k8s/rollouts/order-server-rollout.yaml
kubectl argo rollouts get rollout order-server -n highfivebooks --watch
```

면접에서는 이렇게 설명한다.

```text
상시 운영보다 전환 검증이 목적이라, 먼저 Deployment/Service/ConfigMap/Secret으로 Eureka, Config Server, Gateway 의존을 걷어냈습니다.
그 다음 order-server를 canary Rollout 대상으로 삼아 주문 도메인 변경을 점진적으로 배포하는 실험을 분리했습니다.
```
