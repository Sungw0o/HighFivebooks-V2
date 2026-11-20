# HighFive Cloud Roadmap Issue Drafts - 2026-07-10

Source: https://app.notion.com/p/399827b32761819983e6deb14e777802

These drafts split the Notion roadmap into GitHub issues for `Sungw0o/HighFivebooks-V2`.
GitHub issue creation was blocked by integration permissions, so these are ready-to-create issue bodies.

## 1. HighFive 실제 로컬 K8s 클러스터 구동 및 smoke 증거 확보

### Background
HighFive is positioned as the K8s / MSA / distributed service project.
The first priority is to prove that the manifests are executable, not just documented.

### Scope
- Run the five services in kind or minikube: order, book, member, coupon, payment.
- Run core infrastructure such as MySQL, Redis, and RabbitMQ.
- Verify Service DNS, Ingress, ConfigMap, and Secret flow.
- Add a reproducible smoke test script for representative APIs.
- Capture service-to-service calls and RabbitMQ integration logs.
- Record at least one failure or degraded scenario.

### Deliverables
- Local cluster run guide.
- Smoke test script.
- `kubectl get pods,svc,ingress` output or screenshots.
- Normal and failure scenario logs.

### Done
- Core services are Running/Ready in the local cluster.
- Smoke tests can be reproduced.
- Evidence is usable in the portfolio.

## 2. HighFive Jenkins CI에서 빌드/테스트/ECR 푸시/manifest tag update 구성

### Background
Jenkins should own CI, not deployment.
It builds, tests, pushes images, and updates manifest tags.
Argo CD owns deployment.

### Scope
- Define monorepo path trigger strategy.
- Draft Jenkinsfile for building only changed services.
- Run service tests in the pipeline.
- Build Docker images and define tag conventions.
- Push images to ECR.
- Commit manifest image tag updates.
- Document that Jenkins must not run `kubectl apply`.

### Deliverables
- Jenkinsfile.
- CI flow document.
- ECR push logs.
- Manifest tag update commit example or log.

### Done
- Jenkins builds/tests/pushes changed services.
- Manifest image tag changes are committed to Git.
- Deployment responsibility remains with Argo CD.

## 3. HighFive Argo CD GitOps sync 및 Argo Rollouts 점진 배포 구성

### Background
The deployment story for HighFive is GitOps.
Jenkins changes Git, Argo CD syncs, and Argo Rollouts handles canary or blue-green deployment.

### Scope
- Review Argo CD Application manifests.
- Align Kustomize paths and sync targets.
- Choose canary or blue-green strategy for Argo Rollouts.
- Document promote and abort flow.
- Connect readiness/smoke checks to deployment verification.
- Document the anti-pattern of direct Jenkins deployment.

### Deliverables
- Argo CD Application manifests.
- Rollout manifests.
- Sync logs or screenshots.
- Rollouts promote/abort logs.
- Deployment strategy document.

### Done
- Argo CD sync works from Git changes.
- Canary or blue-green deployment is reproducible.
- Promote/abort and smoke verification flow is documented.

## 4. HighFive ECR + ephemeral EKS 배포 증거 확보

### Background
ECR/EKS should be treated as ephemeral evidence, not always-on infrastructure, because of cost.

### Scope
- Define ECR repository structure and image tag rules.
- Draft Terraform for EKS, VPC, and node groups.
- Document temporary EKS creation and deployment steps.
- Run smoke tests on EKS.
- Add destroy procedure and cost prevention checklist.

### Deliverables
- ECR repository setup document.
- EKS Terraform code.
- EKS deployment smoke output or screenshots.
- Destroy log or cost prevention guide.

### Done
- Service images are pushed to ECR.
- EKS deployment and smoke evidence exists.
- Destroy and cost prevention steps are clear.

## 5. HighFive OpenStack 사설 IaaS 기반 K8s 이식성 카드 정리

### Background
OpenStack is a differentiation card, but it is time-expensive.
Start only after local cluster evidence, Jenkins, and Argo CD are done.

### Scope
- Define Nova-based K8s node VM direction.
- Define Neutron internal networking direction.
- Identify Cinder volume candidates such as MySQL, RabbitMQ, and MinIO.
- Explain portability between public EKS and private IaaS using the same Kustomize manifests.
- Separate actual build scope from documentation-only scope.

### Deliverables
- OpenStack-based HighFive deployment design document.
- EKS vs private IaaS portability comparison.
- Minimum execution procedure and evidence checklist.

### Done
- OpenStack is connected to the HighFive distributed/on-prem story.
- Scope, time cost, and risk are explicit before implementation.

## 6. HighFive MSA 관측성: tracing, RabbitMQ DLQ depth, 서비스 latency 중심 정리

### Background
HighFive observability should not copy BookTown's stack one-to-one.
It should focus on distributed service debugging.

### Scope
- Define service-to-service latency metrics.
- Define RabbitMQ DLQ depth metrics and alert candidates.
- Review tracing options such as OpenTelemetry, Tempo, or Jaeger.
- Map trace points for order creation, payment success, and compensation flows.
- Document observability points for failure scenarios.

### Deliverables
- HighFive MSA observability design document.
- Core metrics list.
- Trace target use cases.
- DLQ depth check and alert procedure.

### Done
- Observability is framed around MSA troubleshooting.
- The order/payment/compensation flow has clear metrics and trace targets.

## BookTown Items Not Opened Here

The Notion roadmap also contains BookTown issues, but they belong in the BookTown repository, not `HighFivebooks-V2`.

- BookTown Terraform for S3, CloudFront, EC2, IAM, Route53, and Security Group.
- BookTown single EC2 Nginx blue-green deployment.
- BookTown Lambda side pipeline.
- BookTown 3-tier + ASG Terraform reference.
