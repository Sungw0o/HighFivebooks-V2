# AWS EKS Troubleshooting Evidence

Measured on 2026-07-27 in `ap-northeast-2`.

## 1. Free-tier worker node launch failure

### Problem

The first managed node group used `t3.medium` and failed with
`AsgInstanceLaunchFailures`. AWS rejected the launch because the instance type
was not eligible for the account's Free Tier.

### Decision

The available Free Tier types included `t3.micro` and `t3.small`. `t3.small`
was selected because it keeps the existing x86 image compatibility while
providing 2 vCPU and 2 GiB memory per node. The worker group was renamed from
`highfivebooks-ephemeral-default` to `highfivebooks-ephemeral-workers` to avoid
the deleting failed group.

### Result

- Worker group creation: about 99 seconds
  - Created: `2026-07-27 01:34:23 KST`
  - Active: `2026-07-27 01:36:02 KST`
- First worker: 1 Ready node
- Scale-out update: desired nodes `1 -> 2` in 24 seconds
- After scale-out: 2 Ready nodes
- Allocatable CPU: `1930m -> 3860m`
- Allocatable memory: `1,467,760Ki -> 2,935,520Ki`

## 2. EBS CSI Pod Identity dependency failure

### Problem

The first Terraform dependency graph waited for the EBS CSI add-on to become
active before creating its Pod Identity association. The EBS controller needed
that association to obtain AWS credentials, so both controller pods remained
at `1/6` containers ready and `CrashLoopBackOff`.

Observed controller error:

```text
failed to refresh cached credentials, no EC2 IMDS role found
```

Before the fix:

- EKS managed add-ons: 0
- StorageClass: legacy `gp2` only
- EBS controller replicas: 2
- Ready containers: `1/6` per controller pod
- Aggregated restarts: 22 per controller pod at capture time
- EBS node DaemonSet: `3/3 Running` on both workers

### Decision

The IAM scope was kept on the EBS controller service account instead of adding
EBS permissions to the EC2 node role:

1. Create the EKS Pod Identity Agent.
2. Create the EBS CSI IAM role and attach `AmazonEBSCSIDriverPolicy`.
3. Associate the role with `kube-system/ebs-csi-controller-sa`.
4. Restart only the EBS CSI controller deployment so new pods receive the
   identity.

The EBS add-on that AWS had created during the interrupted apply was imported
into Terraform state before applying the corrected graph.

### Result

- Pod Identity association creation: 1 second
- Controller recovery after rollout restart: 20.57 seconds
- EBS controller: `6/6 Running`, restart count 0 on both replicas
- EBS node pods: `3/3 Running` on both workers
- EBS CSI add-on: `ACTIVE`, health issues 0
- Managed add-ons: `0 -> 2`
  - `eks-pod-identity-agent`
  - `aws-ebs-csi-driver`

## 3. gp3 dynamic provisioning smoke test

### Test

A temporary 1 GiB `gp3` PVC and BusyBox pod were created. The pod wrote
`highfivebooks-ebs-ok` to the mounted path and read it back.

### Result

- StorageClass: `gp3`, default, `ebs.csi.aws.com`
- PVC and mount ready: 12.33 seconds
- PVC status: `Bound`
- Provisioned volume: `vol-0a6ef22b452a62fd3`
- Read/write verification: success
- PVC reclaim policy: `Delete`
- EBS volume deletion confirmed after the smoke resources were removed

## 4. Infrastructure consistency

After the recovery and state import:

```text
Terraform: No changes. Your infrastructure matches the configuration.
```

Terraform manages the EKS cluster, worker group, ECR repositories, IAM roles,
Pod Identity association, EBS CSI add-on, VPC, subnets, and route resources.

## 5. Portfolio-ready summary

While moving HighFiveBooks to EKS, a Free Tier restriction blocked the original
`t3.medium` managed node group. After comparing eligible instance types,
`t3.small` was selected to preserve x86 compatibility, and two workers were
made Ready with 3860m allocatable CPU and 2,935,520Ki memory. During EBS
integration, an incorrect Terraform dependency caused both EBS controller pods
to remain at `1/6 CrashLoopBackOff`. The dependency was corrected so Pod
Identity and least-privilege IAM were available first, then only the controller
was restarted. It recovered to `6/6 Running` in 20.57 seconds, and a 1 GiB gp3
PVC was dynamically provisioned and mounted in 12.33 seconds. A final Terraform
plan reported zero drift.

## Next measurements

- Build and push the six application/infrastructure images to ECR.
- Deploy the AWS Kustomize overlay and measure pod startup/readiness.
- Compare local and EKS k6 p95, failure rate, and throughput.
- Install metrics-server and verify HPA scale-out time.
- Add ALB Ingress and measure external request latency.
- Capture CloudWatch control-plane and workload evidence.
