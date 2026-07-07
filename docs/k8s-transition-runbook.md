# HighFiveBooks K8s Transition Runbook

목표는 상시 운영이 아니라 Spring Cloud 기반 MSA를 Kubernetes 기본 구성으로 전환할 수 있음을 검증하는 것이다.

## 전환 원칙

- Eureka는 Kubernetes Service DNS로 대체한다.
- Config Server는 ConfigMap과 Secret으로 대체한다.
- Gateway는 Ingress로 대체한다.
- 로컬 `.env` 값은 Kubernetes에 직접 넣지 않는다.
- `k8s/base/secret.example.yaml`은 예시 파일이다. 실제 배포 전 값은 별도 Secret으로 교체한다.

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

## 이미지 태그 교체

기본 매니페스트는 아래 이미지 이름을 사용한다.

```text
ghcr.io/sungw0o/highfivebooks-v2/book-server:latest
ghcr.io/sungw0o/highfivebooks-v2/member-server:latest
ghcr.io/sungw0o/highfivebooks-v2/coupon-server:latest
ghcr.io/sungw0o/highfivebooks-v2/payment-server:latest
ghcr.io/sungw0o/highfivebooks-v2/order-server:latest
```

로컬 kind에서 직접 빌드한다면 이미지 이름을 맞춰 빌드한 뒤 kind에 로드한다.

```powershell
docker build -t ghcr.io/sungw0o/highfivebooks-v2/order-server:latest services/order-server
kind load docker-image ghcr.io/sungw0o/highfivebooks-v2/order-server:latest
```

## 적용 순서

```powershell
kubectl apply -k k8s/base
kubectl -n highfivebooks get pods
kubectl -n highfivebooks get svc
kubectl -n highfivebooks get ingress
```

Secret 예시 값은 반드시 실제 값으로 교체한다.

```powershell
kubectl -n highfivebooks edit secret highfivebooks-secret
```

또는 별도 Secret manifest를 만들어 `secret.example.yaml` 대신 사용한다.

## 로컬 Ingress

Ingress host는 `highfivebooks.local`이다. 로컬 테스트 시 hosts 파일에 클러스터 ingress IP를 연결한다.

```text
127.0.0.1 highfivebooks.local
```

kind에서 nginx ingress를 쓰는 경우 ingress controller 설치가 먼저 필요하다.

## 검증 포인트

```powershell
kubectl -n highfivebooks rollout status deploy/order-server
kubectl -n highfivebooks logs deploy/order-server
kubectl -n highfivebooks exec deploy/order-server -- printenv SPRING_PROFILES_ACTIVE
```

확인할 것:

- `SPRING_PROFILES_ACTIVE=prod`
- 로컬 전환 검증에서는 빈 MySQL 부팅을 위해 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`를 ConfigMap으로 오버라이드
- Feign URL이 `http://*-server:8080` Service DNS를 사용
- RabbitMQ `payment-success-queue`가 DLQ arguments와 함께 생성
- order-server replicas가 2여도 ShedLock으로 스케줄러 중복 실행 방지

## 다음 실험

- ArgoCD Application manifest 추가
- Argo Rollouts canary 또는 blue-green manifest 추가
- 전환 전후 구성도와 rollout 캡처를 포트폴리오 문서에 첨부

## ArgoCD

ArgoCD가 설치되어 있다면 아래 Application으로 `k8s/base`를 sync한다.

```powershell
kubectl apply -f k8s/gitops/argocd-application.yaml
argocd app sync highfivebooks
argocd app wait highfivebooks
```

## Argo Rollouts

`k8s/base/apps.yaml`의 `order-server` Deployment와 `k8s/rollouts/order-server-rollout.yaml`은 같은 이름을 사용한다. Rollout 실험 시에는 Deployment를 제거하거나 scale down한 뒤 Rollout을 적용한다.

```powershell
kubectl -n highfivebooks scale deploy/order-server --replicas=0
kubectl apply -f k8s/rollouts/order-server-rollout.yaml
kubectl argo rollouts get rollout order-server -n highfivebooks --watch
```

면접에서는 이렇게 설명한다.

```text
상시 운영보다 전환 검증이 목적이라, 먼저 Deployment/Service/ConfigMap/Secret로 Eureka/Config/Gateway 의존을 걷어냈습니다.
그 뒤 order-server만 canary Rollout 대상으로 두어 주문 도메인 변경을 점진적으로 배포하는 실험을 분리했습니다.
```
