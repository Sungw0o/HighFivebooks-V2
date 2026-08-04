# HighFiveBooks V2

> 온라인 서점 도메인을 5개 Spring Boot 서비스와 React 스토어프론트로 구성하고, Kubernetes 표준 리소스와 GitOps 배포 흐름으로 재정비한 MSA 프로젝트입니다.

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.7-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19.2-61DAFB?logo=react&logoColor=black)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Kustomize-326CE5?logo=kubernetes&logoColor=white)
![Jenkins](https://img.shields.io/badge/CI-Jenkins-D24939?logo=jenkins&logoColor=white)
![Argo CD](https://img.shields.io/badge/CD-Argo_CD-EF7B4D?logo=argo&logoColor=white)

## 💻 Tech Stacks

| 구분 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.7, Spring Cloud 2025.0.0, Spring Data JPA, OpenFeign, Resilience4j |
| Frontend | React 19.2, TypeScript 5.7, Vite 6.4, React Router 7, Axios, Tailwind CSS 4, Framer Motion |
| Data | MySQL 8.4, Redis 7.2, Elasticsearch 8.18 + Nori, MinIO |
| Messaging | RabbitMQ, Retry/DLQ, ShedLock |
| Infrastructure | Docker Compose, Kubernetes, Kustomize, AWS EKS/ALB/ECR overlay |
| CI/CD | Jenkins, Kaniko, Amazon ECR, Argo CD, Argo Rollouts |
| Test | JUnit 5, Mockito, Spring Boot Test, Testcontainers, k6 |

## 🏗️ System Architecture

<p align="center">
  <img src="docs/highfivebooks-kubernetes-architecture.svg" alt="HighFiveBooks V2 Kubernetes 시스템 아키텍처" width="100%" />
</p>

### V1에서 V2로

| V1 | V2 | 목적 |
| --- | --- | --- |
| Spring Cloud Gateway | Kubernetes Ingress | 진입점과 라우팅을 표준 리소스로 통합 |
| Eureka | Kubernetes Service DNS | 별도 서비스 레지스트리 제거 |
| Config Server | ConfigMap / Secret | 설정 배포 단위를 Kubernetes로 일원화 |
| 서비스별 수동 배포 | Kustomize base/overlay | 로컬과 AWS 환경 차이를 선언적으로 관리 |
| 이미지 태그 수동 변경 | Git SHA 기반 불변 태그 | 배포 버전 추적성과 롤백 근거 확보 |

`k8s/base`에는 로컬 공통 리소스를, `k8s/overlays/aws`에는 ALB Ingress, ECR 이미지, `gp3` 스토리지, 리소스 제한 및 topology spread 설정을 분리했습니다. MSA 경계는 유지하면서 플랫폼 책임만 Kubernetes로 옮겼습니다.

## 🚀 CI/CD Pipeline

<p align="center">
  <img src="docs/highfivebooks-gitops-pipeline.svg" alt="HighFiveBooks V2 CI/CD 파이프라인" width="100%" />
</p>

- Jenkins는 `services/**` 변경 범위를 계산해 필요한 서비스만 빌드하고 테스트합니다.
- Kaniko가 Docker daemon 없이 이미지를 빌드하여 Amazon ECR에 Git SHA 태그로 푸시합니다.
- Jenkins는 클러스터에 직접 배포하지 않고 `k8s/overlays/aws`의 이미지 태그만 갱신합니다.
- Argo CD가 Git을 단일 진실 공급원으로 감시하며 `prune`과 `selfHeal` 정책으로 상태를 동기화합니다.
- 주문 서비스는 Argo Rollouts 매니페스트로 카나리 배포 단계를 선언했습니다.

## ✨ 주요 기능 (Key Features)

| 영역 | 주요 기능 |
| --- | --- |
| 도서 | 도서 목록/상세, 카테고리 검색, Elasticsearch Nori 검색, 리뷰와 좋아요, Aladin 도서 수집 |
| 회원 | 회원가입과 로그인, 소셜 로그인, 배송지, 장바구니, 포인트와 회원 등급 |
| 쿠폰 | 쿠폰 발급/계산/사용/취소, 이벤트 소비 재시도와 DLQ 처리 |
| 주문 | 주문 생성과 조회, 포장/배송 정책, 주문 취소와 반품, 최근 주문 조회 |
| 결제 | Toss Payments 승인/취소, 결제 이벤트 발행과 실패 복구 |
| 분산 정합성 | 재고 `hold/confirm/restore`, 포인트 `reserve/confirm/cancel`의 TCC 보상 흐름 |
| 장애 격리 | Feign timeout, `Retryer.NEVER_RETRY`, Circuit Breaker, 메시지 멱등성 경계 |

## 🧪 Validation & Evidence

- 주문 서비스 회귀 테스트: TCC 보상, Feign 재시도 금지, 결제 이벤트 멱등성 및 DLQ 경계를 포함합니다.
- Kubernetes 스모크 검증: `scripts/k8s-smoke.ps1`에서 배포 상태와 주요 의존성을 확인합니다.
- 도서 조회 k6 비교 기록: p95 `9,039.51 ms → 51.97 ms` 결과를 `perf/results`에 보관합니다.
- CI/CD 구성은 [Jenkinsfile](Jenkinsfile), [Argo CD Application](k8s/gitops/argocd-application.yaml), [Order Rollout](k8s/rollouts/order-server-rollout.yaml)에서 확인할 수 있습니다.

> 저장소의 AWS 매니페스트와 파이프라인은 재현 가능한 배포 구성이며, 항상 실행 중인 운영 환경을 의미하지는 않습니다.

## 🛠️ Local Development

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

스토어프론트만 실행하려면 다음 명령을 사용합니다.

```powershell
Set-Location apps/storefront
npm ci
npm run dev
```

환경 변수의 실제 값은 `.env`에만 두고 저장소에는 커밋하지 않습니다.

## 📁 Repository Structure

```text
HighFiveBooks-V2/
├─ apps/storefront/          # React 고객/관리자 화면
├─ services/                 # order, book, member, coupon, payment
├─ k8s/
│  ├─ base/                  # 공통 Kubernetes 리소스
│  ├─ overlays/aws/          # EKS/ALB/ECR 환경 차이
│  ├─ gitops/                # Argo CD Application
│  └─ rollouts/              # 카나리 배포 정의
├─ perf/                     # k6 시나리오와 측정 결과
├─ scripts/                  # 로컬/클러스터 검증 자동화
├─ docs/                     # 설계, 운영, 트러블슈팅 근거
├─ docker-compose.yml
└─ Jenkinsfile
```

## 📚 Documentation

- [Storefront API Contract](docs/STOREFRONT_API_CONTRACT.md)
- [Performance Report](docs/PERFORMANCE_REPORT.md)
- [Order Flow Boundary Map](docs/order-flow-boundary-map.md)
- [AWS EKS Operation Plan](docs/AWS_EKS_ONE_WEEK_OPERATION_PLAN.md)
- [Argo CD Rollouts Runbook](docs/ARGOCD_ROLLOUTS_RUNBOOK.md)
