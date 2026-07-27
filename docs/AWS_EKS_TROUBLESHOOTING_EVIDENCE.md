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

## 5. Worker memory and EBS availability-zone scheduling

### Problem

The full MSA did not fit on the initial small worker configurations:

- 2 workers: `Too many pods` and `Insufficient memory`
- 3 workers: a worker became `NotReady`
- EC2 console output showed Java processes using about 484-505 MiB RSS before
  repeated OOM kills
- The node reported `Kubelet stopped posting node status`
- EC2 system and instance status checks remained `ok`

Increasing the group to 4 workers provided enough aggregate memory, but the
Elasticsearch PVC was fixed to `ap-northeast-2b`. Both workers in that
availability zone were already at 78-80% requested memory, so Elasticsearch
remained Pending with `0/4 nodes are available: 4 Insufficient memory`.

### Decision

- Keep the Free Tier eligible `t3.small` type.
- Set backend memory requests to 512 MiB and limits to 640 MiB based on the
  observed Java RSS.
- Spread backend pods by hostname.
- Use six workers so the two availability zones each receive three nodes.
- Use `Recreate` for single-replica Deployments and
  `maxSurge: 0`/`maxUnavailable: 1` for the order Rollout to avoid temporary
  double allocation during updates.

### Result

- Failed `t3.medium` group imported into Terraform state before replacement
- `t3.small` 4-worker group replacement: 1 minute 49 seconds
- Scale from 4 to 6 workers:
  - Terraform apply: 10.00 seconds
  - Six Ready nodes: 34.20 seconds after apply
- Final zone distribution:
  - `ap-northeast-2a`: 3 nodes
  - `ap-northeast-2b`: 3 nodes
- Final node health: 6/6 Ready, 0 NotReady

## 6. Elasticsearch EBS permission and memory recovery

### Problem

After EBS reattachment, Elasticsearch failed to create
`/usr/share/elasticsearch/data/node.lock` with `AccessDeniedException`.
After fixing ownership, the 768 MiB container limit was still insufficient and
the pod was OOMKilled during startup.

### Decision

- Apply pod `fsGroup: 1000` with `OnRootMismatch`.
- Reduce Elasticsearch heap from 512 MiB to 384 MiB.
- Reserve 768 MiB and set the container limit to 1 GiB.

### Result

- EBS data path became writable without recreating the PVC.
- Elasticsearch became Ready in 149.05 seconds.
- Final restart count: 0.

## 7. Secret precedence and application startup

### Problem

The Kubernetes Secret had been created from the entire local `.env`. Because
the Secret was loaded after the ConfigMap, local Docker values overrode
Kubernetes service discovery values. All five services timed out while
connecting to MySQL. The member health endpoint also returned 503 because the
deferred Gmail credentials made `MailHealthIndicator` report DOWN.

### Decision

- Rebuild the cluster Secret in memory with only the 30 keys defined by
  `secret.example.yaml`.
- Keep hosts, ports, database names, and service URLs only in the ConfigMap.
- Add a five-minute startup probe for slow Spring Boot cold starts.
- Disable only the mail health indicator while email authentication is
  intentionally deferred.

### Result

- Secret/ConfigMap overlapping keys: multiple local overrides -> 0
- Backend cold-start times:
  - payment-server: 65.06 seconds
  - coupon-server: 71.60 seconds
  - member-server: 78.69 seconds
  - book-server: 102.03 seconds
  - order-server: 96.29 seconds; Ready in 110.97 seconds
- Final backend restarts: 0
- Internal `/actuator/health`: 5/5 HTTP 200
- Final namespace state: 10/10 application and infrastructure pods Ready

## 8. Portfolio-ready summary

While moving HighFiveBooks to EKS, a Free Tier restriction blocked the original
`t3.medium` managed node group, so Free Tier eligible `t3.small` workers were
used. Two and three workers failed from pod density and memory pressure, while
four workers still could not place an EBS-bound Elasticsearch pod in its
availability zone. Memory requests were corrected from observed 484-505 MiB
Java RSS, pods were spread across hosts, and the group was scaled to six nodes
across two zones. All nodes became Ready in 34.20 seconds. EBS CSI Pod Identity
recovered its controller to `6/6 Running` in 20.57 seconds, and a 1 GiB gp3 PVC
was provisioned and mounted in 12.33 seconds. Elasticsearch EBS permissions and
memory were then tuned, and local `.env` values were removed from the
Kubernetes Secret so ConfigMap service discovery could take effect. The final
result was 10/10 Ready pods, zero restarts, and HTTP 200 health responses from
all five backend services.

## 9. Internet-facing ALB Ingress

### Problem

The Kubernetes Ingress still referenced the local `nginx` class and
`highfivebooks.local`. No Ingress controller was installed, so the resource
had no external address and the EKS services could not be reached outside the
cluster. The local Ingress also exposed `/internal/point-transactions`, which
must not become a public ALB route.

### Decision

- Install AWS Load Balancer Controller with Helm chart `1.14.0`.
- Use a controller-only IAM policy and EKS Pod Identity instead of granting
  ELB permissions to every worker node.
- Reuse the two public subnets tagged with
  `kubernetes.io/role/elb=1`.
- Route directly to Pod IPs with `alb.ingress.kubernetes.io/target-type: ip`.
- Use `/actuator/health` for all target group health checks.
- Remove the local hostname and internal point transaction route from the AWS
  overlay.

### Result

- Terraform: 4 IAM and Pod Identity resources added, 0 changed, 0 destroyed
- Helm controller installation: approximately 23 seconds
- ALB state: `active`, `internet-facing`, `application`
- Target groups: 5/5 healthy
- External `GET /api/books`: HTTP 200, 229-byte response
- Target health and external request verification: approximately 148 seconds
- Internal point transaction route: not present on the public ALB

## Next measurements

- Compare local and EKS k6 p95, failure rate, and throughput.
- Install metrics-server and verify HPA scale-out time.
- Measure ALB request latency with the same k6 scenario used locally.
- Add ACM TLS and DNS after selecting the final public domain.
- Capture CloudWatch control-plane and workload evidence.
