# EKS GitOps 배포 안정화 근거

## 문제 정의

Jenkins 빌드 에이전트와 애플리케이션을 같은 `t3.small` 노드 그룹에서 실행한 뒤
Elasticsearch 롤링 재배포가 `Pending` 상태에 머물렀다.

- 스케줄러 이벤트: `0/8 nodes are available`
- 제외 사유: CI 전용 taint 1대, 메모리 부족 7대
- Elasticsearch 이전 Pod 요청량: `768Mi`
- Elasticsearch 실제 사용량: `817476Ki`
- 같은 노드의 order-server 실제 사용량: `383748Ki`
- eviction 직전 노드 가용 메모리: `63628Ki`
- 결과: Elasticsearch와 order-server가 `memory pressure`로 eviction

메모리 요청량만 더 낮추면 스케줄러가 실제 사용량보다 낙관적으로 배치하므로,
같은 장애가 반복될 수 있다고 판단했다.

## 해결 과정

1. Elasticsearch 요청량을 `768Mi`에서 `640Mi`로 조정하고 JVM Heap을 `384Mi`로 제한했다.
2. StatefulSet의 이전 Pod가 `768Mi` 요청을 유지하는 것을 확인하고 PVC를 보존한 채 Pod만 재생성했다.
3. Elasticsearch가 다른 Java 서비스와 다시 배치되면서 메모리 압박이 재발하는 것을 Kubernetes Event로 확인했다.
4. Terraform에 `ap-northeast-2b` 고정 검색 전용 노드 그룹을 추가했다.
5. `workload=search` label과 `dedicated=search:NoSchedule` taint를 적용했다.
6. Elasticsearch에 nodeSelector와 toleration을 추가해 전용 노드로 격리했다.
7. 일반 앱 노드를 7대에서 6대로 줄여 총 노드 수를 8대로 유지했다.

최종 노드 구성은 다음과 같다.

| 용도 | 수량 | 인스턴스 | 배치 |
|---|---:|---|---|
| MSA 애플리케이션 | 6 | `t3.small` | Multi-AZ |
| Elasticsearch | 1 | `t3.small` | `ap-northeast-2b` |
| Jenkins 빌드 에이전트 | 1 | `t3.small` | CI taint 격리 |

## 결과

- Terraform: `No changes`
- Argo CD: `Synced / Healthy`
- Elasticsearch: `1/1 Running`, restart `0`
- 백엔드 Deployment: 서비스별 `2/2 Available`
- order-server Rollout: `2/2 Available`
- ALB 도서 목록 API 연속 호출: `30/30 HTTP 200`
- 응답 시간: 평균 `137.8ms`, p95 `485ms`, 최대 `2100ms`
- ECR 이미지: Git SHA immutable tag 사용
- Jenkins 전체 파이프라인: Maven 테스트, Kaniko 빌드, ECR push, manifest tag commit 완료

## 재현 및 확인 명령

```powershell
kubectl get nodes -L topology.kubernetes.io/zone,workload
kubectl get pods -n highfivebooks -o wide
kubectl get events -n highfivebooks --sort-by=.metadata.creationTimestamp
kubectl describe pod elasticsearch-0 -n highfivebooks
kubectl get applications -n argocd

cd infra/aws/ephemeral-eks
terraform validate
terraform plan
```

## 회고

리소스 요청량은 스케줄링 기준일 뿐 실제 사용량을 제한하지 않는다. 작은 노드에서
메모리를 많이 사용하는 Elasticsearch를 Java MSA와 함께 배치하면 요청량 합계가
허용 범위여도 kubelet eviction이 발생할 수 있었다. 단순 request 하향보다 워크로드
성격에 따른 노드 격리와 taint/toleration 적용이 장애 재발 방지에 더 적합했다.
