# AWS EKS 배포 재개 지시서

## 현재 결론

- `t3.small`은 HighFiveBooks 전체 MSA를 실행하기에 부족하다.
- `t3.small` 2대에서는 `Too many pods`, `Insufficient memory`로 Pod가 Pending 상태에 머물렀다.
- `t3.small` 3대로 확장한 뒤에도 한 노드의 메모리가 고갈됐다.
- EC2 콘솔 로그에서 Java 프로세스가 약 484~505MiB RSS를 사용하다 반복 OOM-kill된 사실을 확인했다.
- 해당 노드는 `Kubelet stopped posting node status`와 함께 `NotReady`가 됐다.
- AWS EC2 시스템 및 인스턴스 상태 검사는 모두 `ok`였으므로 EC2 하드웨어 장애가 아니라 워커 메모리 부족이 원인이다.
- 전체 MSA 검증 기준은 `t3.small` 6대로 잡는다.
- gp3 볼륨은 AZ 고정이므로 두 가용 영역에 각각 3대가 배치되도록 짝수 단위로 확장한다.
- Java 서비스의 memory request를 512Mi로 현실화하고 topology spread를 적용해 특정 노드에 몰리지 않게 한다.
- Free Tier 오버레이에서는 단일 replica 롤링 업데이트의 순간 자원 2배 사용을 피하기 위해 Deployment를 Recreate로, Argo Rollout을 maxSurge 0/maxUnavailable 1로 설정한다.
- EBS 재마운트 후 Elasticsearch 데이터 경로 권한을 보장하도록 fsGroup 1000을 적용하고 heap을 384Mi로 제한한다.
- Elasticsearch는 768Mi 컨테이너 제한에서 OOMKilled됐으므로 request 768Mi, limit 1Gi로 격리한다.
- 작은 노드에서 Spring Boot 초기화가 60초를 넘으므로 5분 startupProbe를 두고 조기 liveness 재시작을 방지한다.
- 이메일 인증을 보류한 검증 환경에서는 mail health indicator만 비활성화해 전체 health가 503으로 오판되지 않게 한다.
- 검증 종료 후 비용 절감을 위해 desired size를 1로 낮출 수 있다.

## 현재 완료 상태

- EKS 클러스터: `highfivebooks-ephemeral`
- 리전: `ap-northeast-2`
- 노드 그룹: `highfivebooks-ephemeral-workers`
- 워커: `t3.small` 6대, `ap-northeast-2a/2b`에 각각 3대
- 노드: 6/6 Ready
- PVC: 5/5 Bound
- namespace 워크로드: 10/10 Ready
- 백엔드 `/actuator/health`: 5/5 HTTP 200
- 현재 Pod 재시작 합계: 0
- Terraform state에 노드 그룹 import 및 교체 결과가 반영됐다.
- AWS Load Balancer Controller: Helm chart `1.14.0`, 1/1 Ready
- ALB: internet-facing, active
- ALB Target Group: 5/5 healthy
- 외부 `GET /api/books`: HTTP 200

## 재개 전 확인

```powershell
cd C:\Users\성우\Desktop\HighFivebooks-V2

Get-Process terraform -ErrorAction SilentlyContinue

aws eks describe-nodegroup `
  --region ap-northeast-2 `
  --cluster-name highfivebooks-ephemeral `
  --nodegroup-name highfivebooks-ephemeral-workers `
  --query "nodegroup.{status:status,instanceTypes:instanceTypes,desired:scalingConfig.desiredSize,health:health.issues}" `
  --output json

kubectl get nodes -o wide
```

Terraform 프로세스가 끝났고 노드 그룹이 `ACTIVE`라면 다음 검증으로 진행한다. 노드 그룹이 `CREATE_FAILED`라면 `health.issues`와 Auto Scaling Group 활동을 먼저 확인한다.

## Terraform 상태 확인

Terraform 프로세스가 종료된 뒤에만 실행한다.

```powershell
cd C:\Users\성우\Desktop\HighFivebooks-V2\infra\aws\ephemeral-eks

terraform plan
terraform state show aws_eks_node_group.default
```

AWS에는 노드 그룹이 있는데 Terraform state에 없다면 임의로 재생성하지 말고 import한다.

```powershell
terraform import aws_eks_node_group.default highfivebooks-ephemeral:highfivebooks-ephemeral-workers
terraform plan
```

## 워크로드 복구 확인

```powershell
kubectl get nodes
kubectl get pods -n highfivebooks -o wide
kubectl get pvc -n highfivebooks
kubectl get events -n highfivebooks --sort-by=.lastTimestamp
```

완료 기준:

- `t3.small` 노드 6대가 모두 `Ready`
- MySQL, Redis, RabbitMQ, Elasticsearch, MinIO PVC가 모두 `Bound`
- 인프라 Pod와 5개 백엔드 서비스가 모두 `Running` 및 `Ready`
- `order-server` Rollout이 `Available`
- 노드 이벤트에 OOM-kill 또는 `MemoryPressure`가 없음

## 애플리케이션 재기동

인프라 Pod가 먼저 Ready가 된 뒤 백엔드를 재기동한다.

```powershell
kubectl rollout restart deployment/book-server -n highfivebooks
kubectl rollout restart deployment/member-server -n highfivebooks
kubectl rollout restart deployment/coupon-server -n highfivebooks
kubectl rollout restart deployment/payment-server -n highfivebooks

$orderPod = kubectl get pod -n highfivebooks `
  -l app=order-server `
  -o jsonpath='{.items[0].metadata.name}'
kubectl delete pod $orderPod -n highfivebooks
```

백엔드가 계속 실패하면 이전 로그를 확인한다.

```powershell
kubectl logs -n highfivebooks <pod-name> --previous --tail=100
```

초기 실패 때는 Secret에 포함된 로컬 Docker 포트와 호스트가 ConfigMap을 덮어써 MySQL 연결 타임아웃과 JDBC 메타데이터 조회 실패가 발생했다. Secret은 `secret.example.yaml`에 정의된 민감 키만 포함해야 한다.

## ALB Ingress

컨트롤러 IAM 권한과 Pod Identity는 Terraform이 관리한다.

```powershell
cd C:\Users\성우\Desktop\HighFivebooks-V2\infra\aws\ephemeral-eks
terraform apply

cd C:\Users\성우\Desktop\HighFivebooks-V2
.\scripts\install-aws-load-balancer-controller.ps1
kubectl apply -k k8s/overlays/aws
```

ALB 주소와 상태를 확인한다.

```powershell
kubectl get ingress highfivebooks-api -n highfivebooks -o wide

$albHost = kubectl get ingress highfivebooks-api `
  -n highfivebooks `
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'

Invoke-WebRequest "http://$albHost/api/books" -UseBasicParsing
```

AWS 오버레이는 로컬 전용 호스트를 제거하고 `alb` IngressClass, IP
target, `/actuator/health` 검사를 적용한다. 내부 API인
`/internal/point-transactions`는 외부 ALB 경로에서 제외한다.

## 비용 정리

- 전체 검증 중: `t3.small` 6대
- 유휴 상태: desired size 1
- ALB는 트래픽이 없어도 시간당 비용이 발생한다.
- Terraform destroy 전 `.\scripts\remove-aws-load-balancer-controller.ps1`을 실행해 ALB와 컨트롤러가 만든 보안 그룹을 먼저 삭제한다.
- 작업 종료: Terraform으로 전체 AWS 임시 환경 제거
- 예산 알림: `HighFiveBooks-EKS-Weekly-Guardrail`, 월 80 USD, 실제 지출 70% 알림

## 포트폴리오 근거

문제 상황:

> 비용을 줄이기 위해 EKS 워커를 `t3.small`로 구성했지만, 2대에서는 Pod 수와 메모리 부족으로 핵심 인프라가 Pending 상태에 머물렀다. 3대로 확장한 뒤에도 실제 Java 메모리 사용량이 Kubernetes requests보다 커 특정 노드에서 약 484~505MiB RSS의 Java 프로세스가 반복 OOM-kill됐고, kubelet heartbeat가 중단되며 노드가 NotReady가 됐다.

해결 방향:

> 인스턴스 메모리를 높이는 방법과 Free Tier 대상인 소형 노드를 수평 확장하는 방법을 비교했다. 학습 환경의 비용 조건을 우선해 `t3.small`을 선택하되, 실제 Java RSS를 기준으로 memory request를 512Mi로 보정하고 topology spread로 백엔드 Pod를 분산했다. 4대 구성에서는 EBS가 고정된 AZ에 512Mi 연속 공간이 없어 Elasticsearch가 Pending 상태에 머물러, AZ별 3대가 되도록 6대로 확장했다. 검증 종료 후에는 1대로 축소하도록 구성했다.

측정 결과:

- small 4대 노드 그룹 생성: 1분 49초
- small 4대에서 6대로 확장 후 Ready: 34.20초
- Elasticsearch 조정 후 Ready: 149.05초
- 백엔드 기동 시간: 65.06~102.03초
- 최종 결과: 노드 6/6 Ready, Pod 10/10 Ready, health 5/5 HTTP 200, 재시작 0회
- ALB 결과: 5/5 Target Group healthy, 외부 도서 API HTTP 200
