# ECR and Ephemeral EKS Runbook

## Goal

This runbook captures the HighFiveBooks public-cloud deployment evidence without leaving paid infrastructure running.

The target evidence is:

- ECR repositories exist for every deployable service image.
- Jenkins or a local operator can push short-SHA tagged images.
- An ephemeral EKS cluster can run the existing `k8s/base` manifests.
- Smoke output is captured.
- The cluster is destroyed immediately after evidence collection.

## Scope

Services:

- `order-server`
- `book-server`
- `member-server`
- `coupon-server`
- `payment-server`
- `elasticsearch-nori`

Default image namespace:

```text
<aws_account_id>.dkr.ecr.<region>.amazonaws.com/highfivebooks-v2/<service>
```

## Cost Guardrails

Do not create EKS until these are true:

- AWS CLI is logged in to the intended account.
- Region is confirmed.
- Terraform plan is reviewed.
- EKS Kubernetes version is left as AWS default unless a supported version is intentionally selected.
- Node group desired size is `1`.
- Destroy window is scheduled before creation.
- Screenshots/log capture checklist is ready.

Recommended local timer:

```powershell
Start-Process powershell -ArgumentList '-NoExit', '-Command', 'Start-Sleep -Seconds 3600; Write-Host "Destroy EKS now"'
```

## ECR Setup

Initialize Terraform:

```powershell
cd infra/aws/ephemeral-eks
terraform init
terraform plan -var-file=example.tfvars
```

Apply only when ready:

```powershell
terraform apply -var-file=example.tfvars
```

Capture:

```powershell
aws ecr describe-repositories --repository-names highfivebooks-v2/order-server highfivebooks-v2/book-server highfivebooks-v2/member-server highfivebooks-v2/coupon-server highfivebooks-v2/payment-server
```

## Image Push Pattern

Login:

```powershell
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <aws_account_id>.dkr.ecr.ap-northeast-2.amazonaws.com
```

Build and push one service:

```powershell
$tag = git rev-parse --short HEAD
docker build -t <aws_account_id>.dkr.ecr.ap-northeast-2.amazonaws.com/highfivebooks-v2/order-server:$tag services/order-server
docker push <aws_account_id>.dkr.ecr.ap-northeast-2.amazonaws.com/highfivebooks-v2/order-server:$tag
```

Repeat for changed services only.

## EKS Deployment

Update kubeconfig:

```powershell
aws eks update-kubeconfig --region ap-northeast-2 --name highfivebooks-ephemeral
kubectl get nodes
```

Install required controllers:

```powershell
kubectl create namespace argo-rollouts
kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml
kubectl get pods -n argo-rollouts
```

Apply the manifests:

```powershell
kubectl apply -k k8s/base
kubectl -n highfivebooks get pods
kubectl -n highfivebooks get svc
kubectl -n highfivebooks get ingress
kubectl -n highfivebooks get rollout
```

Run smoke:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/k8s-smoke.ps1
```

## Evidence Checklist

Capture these screens or logs:

- ECR repositories
- pushed image tags
- `kubectl get nodes`
- `kubectl -n highfivebooks get pods,svc,ingress`
- `kubectl argo rollouts get rollout order-server -n highfivebooks`
- smoke success output
- `terraform destroy` completion

## Destroy

Destroy immediately after evidence capture:

```powershell
cd infra/aws/ephemeral-eks
terraform destroy -var-file=example.tfvars
```

Confirm:

```powershell
aws eks describe-cluster --name highfivebooks-ephemeral --region ap-northeast-2
aws ecr describe-repositories --repository-names highfivebooks-v2/order-server
```

The EKS cluster lookup should fail after destroy. ECR repositories may remain if `force_delete_ecr` is `false`.

## Portfolio Summary

HighFiveBooks uses local kind for repeatable zero-cost validation, then uses ECR and an ephemeral EKS cluster only for short-lived public-cloud evidence. This keeps the portfolio proof real while reducing unnecessary cloud spend.
