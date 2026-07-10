# HighFiveBooks Ephemeral EKS

Terraform skeleton for short-lived ECR/EKS evidence.

Use this only for portfolio proof:

1. Create ECR repositories.
2. Create a small EKS cluster.
3. Deploy `k8s/base`.
4. Capture smoke evidence.
5. Destroy the cluster.

Never store AWS credentials, kubeconfig, `.tfstate`, or `.tfvars` with real account values in Git.

## Commands

```powershell
terraform init
terraform fmt -check
terraform validate
terraform plan -var-file=example.tfvars
```

Apply only after reviewing the plan:

```powershell
terraform apply -var-file=example.tfvars
```

Destroy immediately after evidence:

```powershell
terraform destroy -var-file=example.tfvars
```
