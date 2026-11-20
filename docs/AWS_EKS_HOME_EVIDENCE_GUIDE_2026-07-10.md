# AWS ECR/EKS 증거 확보 집 작업 지시서

작성일: 2026-07-10

목표: HighFiveBooks V2의 ECR + 임시 EKS 배포 증거를 짧게 확보하고, 바로 삭제한다.

중요 원칙:

- `terraform apply` 이후부터 비용이 발생한다.
- 증거 캡처가 끝나면 같은 자리에서 바로 `terraform destroy`까지 실행한다.
- 현재 AWS CLI가 root access key로 연결되어 있다면, 작업 후 root access key를 반드시 삭제한다.
- Secret, access key, `.tfstate`, kubeconfig는 GitHub/Notion/채팅에 올리지 않는다.

---

## 0. 집에서 시작하기 전 체크

AWS 콘솔에서 먼저 확인한다.

- Region: `ap-northeast-2` 서울
- Budget: `highfivebooks-eks-safety-budget`, 월 `5 USD`
- 알림 이메일 등록 완료

PowerShell에서 확인한다.

```powershell
aws sts get-caller-identity
aws configure get region
```

기대값:

```text
Account: <ACCOUNT_ID>
Region: ap-northeast-2
```

`Arn`이 `root`로 나오면 실습 후 access key 삭제를 잊지 않는다.

---

## 1. 저장소 최신화

원본 작업폴더에서 시작한다.

```powershell
cd C:\Users\SSAFY\desktop\HighFivebooks-V2
git status -sb
```

로컬 변경이 많아서 pull이 불안하면 새 폴더에 깨끗하게 받는다.

```powershell
cd C:\tmp
git clone https://github.com/Sungw0o/HighFivebooks-V2.git hfb-aws-evidence
cd C:\tmp\hfb-aws-evidence
```

이미 폴더가 있으면:

```powershell
cd C:\tmp\hfb-aws-evidence
git pull --ff-only origin main
```

---

## 2. Terraform 준비

```powershell
cd infra\aws\ephemeral-eks
terraform init
terraform fmt -check -recursive
terraform validate
```

전부 성공해야 다음으로 간다.

---

## 3. 비용 없는 plan 확인

```powershell
terraform plan -var-file example.tfvars
```

정상 계획:

```text
Plan: 27 to add, 0 to change, 0 to destroy.
```

생성 예정 리소스:

- ECR repository 6개
- EKS cluster `highfivebooks-ephemeral`
- node group `t3.medium` 1대
- VPC, public subnet 2개, route table, internet gateway
- EKS IAM role, node IAM role

캡처 파일명 추천:

```text
#4-terraform-plan-27-to-add.png
```

---

## 4. 실제 생성 전 마지막 확인

아래 5개가 모두 맞으면 진행한다.

- Budget 알림 생성 완료
- Region `ap-northeast-2`
- Docker Desktop 실행 중
- 1시간 안에 끝낼 시간 확보
- 바로 destroy할 준비 완료

진행하기로 마음먹었으면 다음 명령을 실행한다.

```powershell
terraform apply -var-file example.tfvars
```

중간에 묻는 질문:

```text
Do you want to perform these actions?
```

입력:

```text
yes
```

EKS 생성은 10~20분 걸릴 수 있다.

캡처 파일명 추천:

```text
#4-terraform-apply-complete.png
```

---

## 5. ECR 증거 캡처

```powershell
aws ecr describe-repositories --region ap-northeast-2
```

봐야 할 repository:

- `highfivebooks-v2/order-server`
- `highfivebooks-v2/book-server`
- `highfivebooks-v2/member-server`
- `highfivebooks-v2/coupon-server`
- `highfivebooks-v2/payment-server`
- `highfivebooks-v2/elasticsearch-nori`

캡처 파일명 추천:

```text
#4-ecr-repositories-created.png
```

---

## 6. kubeconfig 연결

```powershell
aws eks update-kubeconfig --region ap-northeast-2 --name highfivebooks-ephemeral
kubectl get nodes
```

기대값:

```text
STATUS
Ready
```

캡처 파일명 추천:

```text
#4-eks-node-ready.png
```

---

## 7. Argo Rollouts 설치

```powershell
kubectl create namespace argo-rollouts
kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml
kubectl get pods -n argo-rollouts
```

기대값:

```text
argo-rollouts-...  1/1  Running
```

캡처 파일명 추천:

```text
#4-argo-rollouts-controller-running.png
```

---

## 8. HighFiveBooks 매니페스트 적용

저장소 루트로 이동한다.

```powershell
cd C:\tmp\hfb-aws-evidence
kubectl apply -k k8s/base
```

상태 확인:

```powershell
kubectl -n highfivebooks get pods
kubectl -n highfivebooks get svc
kubectl -n highfivebooks get ingress
kubectl -n highfivebooks get rollout
kubectl argo rollouts get rollout order-server -n highfivebooks
```

캡처 파일명 추천:

```text
#4-eks-highfivebooks-pods-svc-ingress.png
#4-eks-order-rollout-healthy.png
```

주의:

- 이미지가 아직 ECR로 push되지 않았다면 app pod가 `ImagePullBackOff`가 날 수 있다.
- 그래도 EKS cluster, ECR, K8s manifest 적용 구조 증거는 확보 가능하다.
- 완전한 smoke까지 하려면 서비스 이미지를 ECR에 push하고 `kustomization.yaml` image registry를 ECR로 바꿔야 한다.

---

## 9. 이미지 push까지 할 경우

ECR login:

```powershell
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com
```

예시: order-server build/push

```powershell
$tag = git rev-parse --short HEAD
cd C:\tmp\hfb-aws-evidence
docker build -t <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/highfivebooks-v2/order-server:$tag services/order-server
docker push <ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com/highfivebooks-v2/order-server:$tag
```

다른 서비스도 같은 방식:

- `book-server`
- `member-server`
- `coupon-server`
- `payment-server`

이 단계는 시간이 오래 걸릴 수 있으므로, 집에서 시간이 넉넉할 때만 진행한다.

---

## 10. 반드시 삭제

증거 캡처가 끝나면 바로 삭제한다.

```powershell
cd C:\tmp\hfb-aws-evidence\infra\aws\ephemeral-eks
terraform destroy -var-file example.tfvars
```

질문이 나오면:

```text
yes
```

캡처 파일명 추천:

```text
#4-terraform-destroy-complete.png
```

삭제 확인:

```powershell
aws eks describe-cluster --region ap-northeast-2 --name highfivebooks-ephemeral
```

정상적으로 삭제되면 cluster not found 계열 오류가 나온다.

---

## 11. root access key 삭제

AWS 콘솔에서:

1. 오른쪽 위 계정 이름 클릭
2. Security credentials
3. Access keys
4. 이번에 만든 root access key 비활성화 또는 삭제

이 캡처도 남기면 좋다.

```text
#4-root-access-key-deleted.png
```

---

## 12. Notion에 붙일 최종 요약

```text
ECR 6개 repository와 ephemeral EKS cluster를 Terraform으로 생성하고, kubeconfig 연결 후 Kubernetes 리소스 적용과 Rollout 확인까지 수행했다. 실증 후 즉시 terraform destroy로 EKS/VPC/NodeGroup/IAM 리소스를 제거해 비용을 통제했다.
```

---

## 문제 발생 시 즉시 확인

현재 계정/리전:

```powershell
aws sts get-caller-identity
aws configure get region
```

현재 EKS:

```powershell
aws eks list-clusters --region ap-northeast-2
kubectl config current-context
kubectl get nodes
```

비용 리소스 빠른 확인:

```powershell
aws eks list-clusters --region ap-northeast-2
aws ec2 describe-instances --region ap-northeast-2 --query "Reservations[].Instances[].{Id:InstanceId,State:State.Name,Type:InstanceType}"
aws ecr describe-repositories --region ap-northeast-2 --query "repositories[].repositoryName"
```

삭제가 막히면 `terraform destroy -var-file example.tfvars`를 다시 실행한다.
