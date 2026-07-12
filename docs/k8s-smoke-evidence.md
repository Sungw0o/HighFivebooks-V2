# K8s Smoke Evidence

검증일: 2026-07-12

대상: `k8s/base`

결과: **PASS**

## 목적

HighFiveBooks V2의 기본 Kubernetes 리소스가 단순 렌더링을 넘어 실제 로컬 클러스터에서 기동되고, 서비스 연결과 readiness까지 정상인지 확인합니다.

## 실행 환경

```text
OS: Windows
Docker Server: 27.1.1
Kubernetes context: kind-highfivebooks
kind cluster: highfivebooks
Node status: Ready
```

이번 검증 대상은 `k8s/base`의 일반 Deployment입니다. `k8s/rollouts`의 Argo Rollout이나 Argo CD Application을 실제 동기화한 결과로 해석하지 않습니다.

## 실행 명령

```powershell
kubectl config current-context
kind get clusters
kubectl kustomize k8s/base
powershell -ExecutionPolicy Bypass -File .\scripts\k8s-smoke.ps1
```

## 스모크 결과

스크립트가 다음 항목을 순서대로 확인했고 마지막에 `K8s smoke check passed.`를 반환했습니다.

| 검증 항목 | 결과 |
|---|---|
| `highfivebooks` namespace | Active |
| MySQL StatefulSet | rollout 완료 |
| Redis StatefulSet | rollout 완료 |
| RabbitMQ StatefulSet | rollout 완료 |
| Elasticsearch StatefulSet | rollout 완료 |
| MinIO StatefulSet | rollout 완료 |
| book-server Deployment | rollout 완료 |
| member-server Deployment | rollout 완료 |
| coupon-server Deployment | rollout 완료 |
| payment-server Deployment | rollout 완료 |
| order-server Deployment | rollout 완료 |
| 5개 백엔드 EndpointSlice | address 존재 |
| order-server `BOOK_SERVICE_URL` | `http://book-server:8080` |
| 서비스 DDL 설정 | `update` 확인 |
| order-server 의존성 연결 | MySQL·RabbitMQ 연결 로그 확인 |
| Elasticsearch Nori plugin | `analysis-nori` 확인 |
| 한국어 analyzer | `java`, 동의어 `자바` 토큰 확인 |
| 5개 서비스 readiness | 모두 HTTP 200 |

## 확인된 범위

- Kustomize base 리소스가 kind에 적용 가능한 상태입니다.
- 5개 상태 저장 인프라와 5개 백엔드 서비스가 rollout을 완료합니다.
- order-server가 Kubernetes Service DNS 설정으로 book-server를 참조합니다.
- EndpointSlice가 생성되어 Service 뒤에 실제 Pod 주소가 연결됩니다.
- order-server가 MySQL과 RabbitMQ에 연결됩니다.
- book-server 검색에 필요한 Elasticsearch Nori 분석기가 동작합니다.
- 모든 백엔드 readiness endpoint가 트래픽 수신 가능 상태를 반환합니다.

## 이 결과로 주장하지 않는 범위

- Jenkins 빌드가 GHCR 이미지를 push한 실제 실행 결과
- Argo CD가 Git 변경을 감지해 클러스터를 동기화한 실제 실행 결과
- Argo Rollouts가 canary를 20%·50%·100%로 승격한 실제 실행 결과
- Prometheus 지표에 따라 canary 성공·실패를 자동 판정한 결과
- 운영 클라우드의 고가용성·장애 복구 수준

현재 `k8s/rollouts/order-server-rollout.yaml`은 20% → 60초 대기 → 50% → 60초 대기 → 100%로 진행하는 시간 기반 canary입니다. `AnalysisTemplate`이 없으므로 메트릭 기반 자동 canary라고 표현하지 않습니다.

## 재현 방법

```powershell
kubectl apply -k k8s/base
kubectl -n highfivebooks get pods
powershell -ExecutionPolicy Bypass -File .\scripts\k8s-smoke.ps1
```

재실행 시 날짜, Git commit SHA, Docker·kind·Kubernetes 버전과 전체 터미널 출력을 함께 보관하면 포트폴리오 증거로 더 강하게 사용할 수 있습니다.
