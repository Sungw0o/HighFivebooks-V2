# HighFiveBooks AWS EKS 1주 운영 전환 계획

작성일: 2026-07-26

## 목표

HighFiveBooks V2를 로컬 Docker Compose 재현 환경에서 끝내지 않고, AWS EKS 기반 MSA 운영 환경으로 1주일 배포해 클라우드/DevOps 포트폴리오 증거를 확보한다.

이번 전환의 목적은 상용 수준의 장기 운영이 아니라, 다음 AWS 구성 요소를 실제로 연결하고 운영 흐름을 검증하는 것이다.

- IAM Role / Policy / IRSA
- ECR
- EKS Managed Node Group
- AWS Load Balancer Controller / ALB
- EBS CSI Driver / PVC
- RDS MySQL
- S3 + CloudFront
- CloudWatch
- Jenkins CI
- Argo CD GitOps

## 최종 목표 아키텍처

```text
User
  |
Route53 or Cloudflare DNS
  |
CloudFront
  |-- static --> S3 React storefront
  |
  |-- /api/* --> ALB
                 |
              EKS Ingress
                 |
    ---------------------------------
    |       |       |       |       |
  book   member  coupon  payment  order(replicas=2)
                                   |
                              RabbitMQ event consume

EKS internal infra:
  - RabbitMQ + EBS PVC
  - Redis
  - Elasticsearch Nori
  - MinIO + EBS PVC

Managed data:
  - RDS MySQL

Delivery:
  GitHub -> Jenkins -> ECR -> kustomization tag commit -> Argo CD -> EKS

Operation:
  IAM / IRSA / CloudWatch / kubectl smoke / k6 smoke
```

Draw.io 구조도:

```text
docs/assets/highfivebooks-aws-eks-architecture.drawio
```

## 범위

### 포함

- React storefront를 S3 + CloudFront로 정적 배포
- 5개 백엔드 서비스를 ECR 이미지로 빌드/push
- EKS에 `book`, `member`, `coupon`, `payment`, `order` 배포
- `order-server`는 replicas=2로 실행
- RDS MySQL을 공통 DB로 연결
- RabbitMQ와 MinIO는 EBS PVC로 상태 저장 검증
- ALB Ingress로 외부 API 접근
- Jenkins가 변경 서비스 build/test/image push/tag commit 담당
- Argo CD가 Git 상태 기준으로 EKS sync
- CloudWatch에서 ALB, EKS, RDS 지표와 로그 일부 확인
- 1주일 운영 후 증거 캡처 및 비용 회고

### 제외

- NAT Gateway 기반 private node 정석 구성
- OpenSearch, ElastiCache, Amazon MQ 같은 관리형 대체 서비스
- 멀티 AZ 고가용성 운영
- HPA 기반 장기 오토스케일링
- 상시 운영용 TLS/WAF/보안 정책 완성

## 비용 가드레일

비용을 줄이기 위해 다음 원칙을 지킨다.

- NAT Gateway는 만들지 않는다.
- EKS node는 public subnet에 배치한다.
- Managed Node Group desired size는 1로 시작한다.
- 필요할 때만 max size를 2로 올린다.
- RDS는 free tier 또는 가장 작은 MySQL 인스턴스로 시작한다.
- ALB는 1개만 생성한다.
- EBS PVC 용량은 RabbitMQ, MinIO 각각 최소 크기로 시작한다.
- EKS는 1주 운영 후 반드시 삭제한다.
- 생성 전 AWS Budget과 Billing Alert를 켠다.

## 0단계. 사전 준비

### 로컬 도구

- AWS CLI
- Terraform
- kubectl
- eksctl 또는 AWS CLI EKS 명령
- Docker Desktop
- Kustomize
- Helm
- Jenkins

### AWS 확인

```powershell
aws sts get-caller-identity
aws configure get region
```

결정값:

```text
region: ap-northeast-2
cluster: highfivebooks-week
namespace: highfivebooks
image tag: git short SHA
```

### 해야됨

- [ ] AWS Budget 생성
- [ ] 결제 알림 이메일 확인
- [ ] 사용할 region 확정
- [ ] 삭제 예정일 캘린더 등록
- [ ] `.env` / secret 값 정리

## 1단계. ECR 구성

목표:

- 서비스별 ECR repository 생성
- 이미지 tag 규칙을 short SHA로 통일

대상 repository:

```text
highfivebooks-v2/order-server
highfivebooks-v2/book-server
highfivebooks-v2/member-server
highfivebooks-v2/coupon-server
highfivebooks-v2/payment-server
highfivebooks-v2/elasticsearch-nori
```

검증:

```powershell
aws ecr describe-repositories
aws ecr list-images --repository-name highfivebooks-v2/order-server
```

캡처:

- ECR repository 목록
- 서비스 이미지 tag 목록

## 2단계. IAM 구성

목표:

- Jenkins, EKS node, ALB Controller, EBS CSI Driver 권한을 분리한다.
- 가능하면 IRSA로 Kubernetes ServiceAccount와 IAM Role을 연결한다.

필요 권한:

```text
EKS Cluster Role
EKS Node Role
ECR push/pull 권한
AWS Load Balancer Controller Role
EBS CSI Driver Role
Argo CD read 권한
Jenkins Git/ECR push 권한
```

해야됨:

- [ ] Jenkins에서 사용할 AWS credential 또는 role 결정
- [ ] AWS Load Balancer Controller IAM policy 생성
- [ ] EBS CSI Driver IAM policy 생성
- [ ] IRSA 활성화 여부 확인

캡처:

- IAM Role 목록
- ALB Controller policy
- EBS CSI Driver policy
- ServiceAccount annotation

## 3단계. EKS 클러스터 생성

현재 레포에는 비용 절감형 EKS Terraform 골격이 있다.

```text
infra/aws/ephemeral-eks
```

현재 특징:

- VPC 생성
- public subnet 2개
- Internet Gateway
- NAT Gateway 없음
- EKS Cluster
- Managed Node Group
- node desired size 1

실행:

```powershell
cd infra/aws/ephemeral-eks
terraform init
terraform plan -var-file=example.tfvars
terraform apply -var-file=example.tfvars
```

검증:

```powershell
aws eks update-kubeconfig --region ap-northeast-2 --name highfivebooks-week
kubectl get nodes
kubectl get ns
```

캡처:

- EKS cluster AWS Console
- `kubectl get nodes`
- EC2 node group

## 4단계. RDS MySQL 연결

목표:

- MySQL은 Pod 내부가 아니라 RDS로 분리한다.
- EKS node security group에서만 접근하게 제한한다.
- DB 접속 정보는 Kubernetes Secret으로 주입한다.

구성:

```text
RDS MySQL
  database: highfivebooks
  user: highfive
  security group: EKS node SG inbound 3306 허용
```

해야됨:

- [ ] RDS 생성
- [ ] 보안 그룹 연결
- [ ] schema/init SQL 반영
- [ ] 도서 데이터 dump import 여부 결정
- [ ] Kubernetes Secret 갱신

검증:

```powershell
kubectl -n highfivebooks run mysql-client --rm -it --image=mysql:8.4 -- mysql -h <rds-endpoint> -u <user> -p
```

캡처:

- RDS instance 상태
- 보안 그룹 inbound
- `SELECT COUNT(*) FROM book`

## 5단계. EBS CSI Driver와 PVC

목표:

- EBS를 직접 써본 근거를 만든다.
- RabbitMQ, MinIO 중 최소 하나는 PVC로 데이터 유지 검증을 한다.

설치:

```powershell
aws eks describe-addon-versions --addon-name aws-ebs-csi-driver --region ap-northeast-2
aws eks create-addon --cluster-name highfivebooks-week --addon-name aws-ebs-csi-driver --region ap-northeast-2
```

또는 Helm/IRSA 방식으로 설치한다.

검증:

```powershell
kubectl get storageclass
kubectl -n highfivebooks get pvc,pv
kubectl -n highfivebooks delete pod <rabbitmq-pod>
kubectl -n highfivebooks get pod
```

확인할 것:

- PVC Bound
- Pod 재생성 후 데이터 유지

캡처:

- StorageClass
- PVC/PV Bound
- EBS Volume AWS Console

## 6단계. AWS Load Balancer Controller와 ALB

목표:

- Kubernetes Ingress로 ALB를 자동 생성한다.
- `/api/*` 요청을 EKS 서비스로 라우팅한다.

해야됨:

- [ ] OIDC provider 확인
- [ ] ALB Controller IAM Role 생성
- [ ] Helm으로 AWS Load Balancer Controller 설치
- [ ] Ingress annotation을 AWS ALB용으로 분리
- [ ] ALB DNS 접근 확인

검증:

```powershell
kubectl -n kube-system get deploy aws-load-balancer-controller
kubectl -n highfivebooks get ingress
kubectl -n highfivebooks describe ingress
```

캡처:

- AWS ALB 화면
- Target group healthy
- Ingress ADDRESS
- API health 응답

## 7단계. K8s AWS overlay 분리

현재 `k8s/base`는 local/kind와 EKS 전환 공통 기준이다. AWS 전환에서는 별도 overlay를 둔다.

추가 후보:

```text
k8s/overlays/aws
  kustomization.yaml
  ingress-alb-patch.yaml
  rds-config-patch.yaml
  storageclass.yaml
  rabbitmq-pvc-patch.yaml
  minio-pvc-patch.yaml
```

변경 방향:

- MySQL manifest는 RDS 사용 시 제외 또는 비활성화
- Ingress는 ALB annotation 적용
- Secret은 실제 값 대신 `secret.example.yaml` 유지
- RabbitMQ/MinIO는 PVC 적용
- image registry는 ECR로 교체

검증:

```powershell
kubectl kustomize k8s/overlays/aws
```

캡처:

- overlay 렌더링 결과
- base와 aws overlay 차이

## 8단계. 서비스 이미지 빌드와 배포

로컬 또는 Jenkins에서 ECR로 push한다.

```powershell
$tag = git rev-parse --short HEAD
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <account>.dkr.ecr.ap-northeast-2.amazonaws.com

docker build -t <account>.dkr.ecr.ap-northeast-2.amazonaws.com/highfivebooks-v2/order-server:$tag services/order-server
docker push <account>.dkr.ecr.ap-northeast-2.amazonaws.com/highfivebooks-v2/order-server:$tag
```

반복 대상:

```text
book-server
member-server
coupon-server
payment-server
order-server
```

검증:

```powershell
kubectl apply -k k8s/overlays/aws
kubectl -n highfivebooks get pods,svc,ingress,pvc
kubectl -n highfivebooks logs deploy/order-server
```

캡처:

- ECR image tag
- Pod Running
- Service/Ingress
- App health

## 9단계. Jenkins + Argo CD GitOps

목표:

- Jenkins는 CI와 tag commit만 수행한다.
- Argo CD가 배포 권한을 가진다.

현재 Jenkinsfile 방향:

```text
changed service detect
-> Maven build/test
-> Docker image build/push
-> k8s/base kustomization image tag update
-> Git push
-> Argo CD sync
```

AWS 전환 시 보강:

- GHCR 대신 ECR registry 사용 여부 결정
- `k8s/base` 대신 `k8s/overlays/aws` tag 갱신 여부 결정
- Jenkins AWS credential 등록
- Argo CD Application path를 `k8s/overlays/aws`로 변경

검증:

```powershell
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl apply -f k8s/gitops/argocd-application.yaml
```

캡처:

- Jenkins build success
- changed service detection
- ECR push log
- Git tag update commit
- Argo CD Synced / Healthy
- rollout history

## 10단계. S3 + CloudFront storefront

목표:

- React storefront 정적 파일을 S3에 배포한다.
- CloudFront로 제공한다.
- `/api/*`는 ALB로 보낸다.

해야됨:

- [ ] storefront production build
- [ ] S3 bucket 생성
- [ ] CloudFront distribution 생성
- [ ] origin 1: S3
- [ ] origin 2: ALB
- [ ] behavior `/api/*` -> ALB
- [ ] default behavior -> S3

검증:

```powershell
cd apps/storefront
npm ci
npm run build
aws s3 sync dist s3://<bucket-name> --delete
```

캡처:

- S3 bucket
- CloudFront behaviors
- storefront 접속 화면
- API 호출 성공

## 11단계. 운영 검증

1주일 동안 매일 짧게 확인한다.

### 매일 확인

```powershell
kubectl -n highfivebooks get pods,svc,ingress,pvc
kubectl -n highfivebooks top pods
kubectl -n highfivebooks logs deploy/order-server --tail=100
aws cloudwatch get-metric-statistics ...
```

### k6 smoke

```powershell
docker run --rm -i `
  -e BASE_URL=http://<alb-dns-or-domain> `
  -e VUS=1 `
  -e DURATION=10s `
  grafana/k6:0.53.0 run - < perf/k6/order-list-baseline.js
```

### 장애 연습 후보

- order-server Pod 1개 삭제 후 복구 확인
- RabbitMQ Pod 삭제 후 PVC 데이터 유지 확인
- RDS connection 실패 시 로그 확인
- 잘못된 image tag 배포 후 Argo CD/rollout 상태 확인

## 12단계. 증거 캡처 체크리스트

### AWS

- [ ] EKS cluster
- [ ] Managed Node Group
- [ ] ECR repositories
- [ ] ECR image tags
- [ ] RDS instance
- [ ] RDS security group
- [ ] ALB
- [ ] Target group healthy
- [ ] EBS volumes
- [ ] IAM roles
- [ ] CloudWatch metrics
- [ ] S3 bucket
- [ ] CloudFront distribution behaviors

### Kubernetes

- [ ] `kubectl get nodes`
- [ ] `kubectl -n highfivebooks get pods`
- [ ] `kubectl -n highfivebooks get svc`
- [ ] `kubectl -n highfivebooks get ingress`
- [ ] `kubectl -n highfivebooks get pvc,pv`
- [ ] `kubectl describe ingress`
- [ ] `kubectl logs deploy/order-server`
- [ ] `kubectl rollout status`

### CI/CD

- [ ] Jenkins build success
- [ ] Maven test log
- [ ] Docker build/push log
- [ ] kustomization image tag commit
- [ ] Argo CD Synced / Healthy

### 성능/운영

- [ ] k6 smoke result
- [ ] API health response
- [ ] Pod restart recovery
- [ ] RabbitMQ PVC recovery
- [ ] 비용 사용량 캡처

## 13단계. 삭제 계획

삭제는 생성만큼 중요하다.

순서:

```powershell
kubectl delete -k k8s/overlays/aws
kubectl delete namespace argocd
terraform destroy -var-file=example.tfvars
aws rds delete-db-instance ...
aws cloudfront update-distribution ...
aws s3 rm s3://<bucket> --recursive
aws s3 rb s3://<bucket>
```

확인:

- [ ] EKS 삭제
- [ ] EC2 node 삭제
- [ ] ALB 삭제
- [ ] EBS volume 삭제
- [ ] RDS 삭제 또는 중지
- [ ] NAT Gateway 없음 확인
- [ ] Elastic IP 미사용 확인
- [ ] CloudFront/S3 정리
- [ ] Billing Dashboard 확인

## 7일 운영 일정

| 일차 | 목표 | 산출물 |
|---|---|---|
| Day 1 | AWS 계정/예산/IAM/ECR 정리 | Budget, IAM Role, ECR 캡처 |
| Day 2 | EKS 생성, kubectl 연결 | EKS cluster, node 캡처 |
| Day 3 | RDS, EBS CSI, ALB Controller 구성 | RDS, PVC, ALB Controller 캡처 |
| Day 4 | 서비스 이미지 push, K8s 배포 | Pod/Service/Ingress Running 캡처 |
| Day 5 | Jenkins + Argo CD GitOps 연결 | Jenkins/Argo CD sync 캡처 |
| Day 6 | S3 + CloudFront storefront 연결, k6 smoke | CloudFront, API, k6 캡처 |
| Day 7 | 장애 복구 테스트, 비용 회고, 삭제 | 복구 로그, 비용 캡처, destroy 캡처 |

## 포트폴리오 문장 초안

HighFiveBooks V2에서는 로컬 Docker Compose 기반 MSA 재현 환경을 AWS EKS로 확장해 1주일간 실제 클라우드 환경에서 운영 검증을 수행했습니다. React storefront는 S3/CloudFront로 분리하고, 백엔드 5개 서비스는 ECR 이미지를 기반으로 EKS에 배포했습니다. 외부 API 트래픽은 AWS Load Balancer Controller가 생성한 ALB Ingress로 라우팅했고, MySQL은 RDS로 분리했습니다. RabbitMQ와 MinIO는 EBS CSI Driver 기반 PVC를 연결해 Pod 재생성 이후에도 데이터가 유지되는지 검증했습니다. Jenkins는 변경된 서비스만 build/test/image push 후 Kustomize image tag를 갱신하고, Argo CD가 Git 상태를 기준으로 EKS에 동기화하는 GitOps 흐름을 구성했습니다.

## 면접 답변 초안

비용 제약이 있는 개인 프로젝트였지만, 단순 EC2 배포로 끝내면 Kubernetes와 AWS 인프라 운영 경험을 충분히 보여주기 어렵다고 판단했습니다. 그래서 NAT Gateway와 관리형 OpenSearch/ElastiCache처럼 비용이 큰 구성은 제외하되, EKS, ECR, RDS, EBS, ALB, IAM, CloudWatch처럼 클라우드 인프라 엔지니어 직무에서 기본이 되는 요소는 직접 연결해 1주일 동안 운영했습니다. Jenkins는 배포 권한을 직접 갖지 않고 이미지 빌드와 GitOps 태그 갱신만 담당하게 했고, Argo CD가 Git을 단일 소스로 삼아 클러스터에 동기화하도록 역할을 분리했습니다.

## 해야됨

- [ ] `k8s/overlays/aws` 작성
- [ ] Terraform에 RDS 선택 적용 여부 결정
- [ ] EBS CSI Driver 설치 방식 결정
- [ ] ALB Controller 설치 runbook 작성
- [ ] Jenkins registry를 GHCR에서 ECR로 전환할지 결정
- [ ] Argo CD Application path를 aws overlay로 전환
- [ ] CloudFront `/api/*` behavior 설계
- [ ] 운영 캡처 저장 위치 정리
- [ ] 일주일 운영 후 비용 회고 작성
