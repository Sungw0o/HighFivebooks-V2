# K8s Smoke Evidence

작성일: 2026-07-08

## 목적

`k8s/base` 매니페스트가 렌더링 가능한지 확인하고, 실제 Kubernetes smoke check를 실행할 때 필요한 조건과 현재 로컬 환경의 막힌 지점을 기록한다.

## 실행 환경 확인

현재 로컬 환경 상태:

```text
kubectl config current-context
-> error: current-context is not set

kind get clusters
-> kind 명령을 찾을 수 없음
```

따라서 현재 환경에서는 실제 클러스터 대상 smoke check를 완료할 수 없다.

## Manifest 렌더링 검증

실행:

```powershell
kubectl kustomize k8s/base
```

결과: 성공

렌더링 결과에서 확인한 핵심 항목:

- `Namespace/highfivebooks`
- `ConfigMap/highfivebooks-config`
- `Secret/highfivebooks-secret` 예시
- 5개 백엔드 Service: `book-server`, `member-server`, `coupon-server`, `payment-server`, `order-server`
- 5개 백엔드 Deployment
- MySQL, Redis, RabbitMQ, Elasticsearch, MinIO StatefulSet
- Ingress host: `highfivebooks.local`
- `order-server` replicas: `2`
- `BOOK_SERVICE_URL=http://book-server:8080`
- `FEIGN_CONNECT_TIMEOUT_MS=1000`
- `FEIGN_READ_TIMEOUT_MS=3000`
- `RABBIT_LISTENER_RETRY_MAX_ATTEMPTS=3`
- `RABBIT_LISTENER_RETRY_INITIAL_INTERVAL_MS=1000`
- `RABBIT_LISTENER_RETRY_MULTIPLIER=2.0`
- `RABBIT_LISTENER_RETRY_MAX_INTERVAL_MS=10000`

## Smoke 스크립트 실행 기록

처음 실행:

```powershell
.\scripts\k8s-smoke.ps1
```

결과:

```text
이 시스템에서 스크립트를 실행할 수 없으므로 scripts\k8s-smoke.ps1 파일을 로드할 수 없습니다.
```

실행 정책 우회 후 실행:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\k8s-smoke.ps1
```

스크립트 인자 전달 버그 수정 전 결과:

```text
error: unknown command "get namespace highfivebooks" for "kubectl"
```

조치:

- `scripts/k8s-smoke.ps1`의 `Invoke-Kubectl` 인자 이름을 `$Args`에서 `$KubectlArgs`로 변경
- `Invoke-Kubectl "get", "namespace", $Namespace` 형식 호출을 `Invoke-Kubectl get namespace $Namespace` 형식으로 수정

수정 후 재실행 결과:

```text
> kubectl get namespace highfivebooks
Unable to connect to the server: dial tcp [::1]:8080: connectex: No connection could be made because the target machine actively refused it.
kubectl command failed: kubectl get namespace highfivebooks
```

판단:

- smoke 스크립트는 정상적으로 `kubectl get namespace highfivebooks`까지 진입한다.
- 실패 원인은 스크립트가 아니라 현재 로컬에 연결된 Kubernetes cluster/context가 없기 때문이다.

## 실제 성공 캡처 절차

Kubernetes 환경이 준비된 뒤 아래 순서로 재실행한다.

```powershell
kubectl config current-context
kubectl apply -k k8s/base
kubectl -n highfivebooks get pods
powershell -ExecutionPolicy Bypass -File .\scripts\k8s-smoke.ps1
```

성공 시 캡처할 항목:

- `kubectl -n highfivebooks get pods`
- `kubectl -n highfivebooks get svc`
- `kubectl -n highfivebooks rollout status deploy/order-server`
- `kubectl -n highfivebooks logs deploy/order-server --tail=100`
- `powershell -ExecutionPolicy Bypass -File .\scripts\k8s-smoke.ps1` 성공 출력

## 포트폴리오 기록 문장

```text
현재 로컬 머신에는 Kubernetes context와 kind가 없어 실제 smoke 완료까지는 수행하지 못했지만,
매니페스트 렌더링은 성공했고 smoke 스크립트는 kubectl 실행 단계까지 검증했습니다.
이후 kind 또는 Docker Desktop Kubernetes 환경에서 같은 스크립트로 rollout, endpoint, Service DNS, RabbitMQ, Redis, readiness를 확인하도록 재현 절차를 남겼습니다.
```
