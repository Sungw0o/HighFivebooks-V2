# HighFiveBooks V2 Refactoring Roadmap

작성일: 2026-07-06
최종 갱신: 2026-07-08

## 1. 목표

HighFiveBooks V2는 기존 팀 프로젝트를 개인 포트폴리오용으로 재정리한 저장소다. 핵심 방향은 MSA를 유지하면서 Spring Cloud 운영 구성 일부를 Kubernetes-native 구성으로 전환하고, 주문 도메인의 안정성을 깊게 보강하는 것이다.

핵심 문장:

```text
monorepo로 리팩토링 관리를 단순화했지만, 각 도메인 서버는 독립 실행/독립 배포 가능한 MSA로 유지한다.
```

## 2. 범위

### 완료한 핵심 작업

- order-server 테스트 베이스라인 확보
- 주문 흐름과 Feign 경계 지도 작성
- Feign boundary test 추가
- `OrderServiceImpl` 중심 트랜잭션 경계 분리
- 결제 성공 RabbitMQ 메시지 Retry/DLQ 정책 추가
- 주문 스케줄러 Redis ShedLock 적용
- Feign timeout, CircuitBreaker, retry 비활성화 정책 정리
- K8s liveness/readiness probe 추가
- K8s ConfigMap/Secret, Service DNS, Ingress 전환 문서화
- README와 포트폴리오 근거 문서 갱신

### 일부러 하지 않는 것

- MSA를 모놀리스로 합치기
- 기존 Thymeleaf `front_server` 직접 리팩토링
- Secret, API key, DB password를 코드에 커밋
- 재고/쿠폰/포인트 같은 상태 변경 Feign 호출에 무조건 자동 retry 적용
- 운영 클러스터 상시 운영 선언

## 3. 현재 검증 상태

order-server 전체 테스트:

```text
126 tests, 0 failures, 0 errors, 0 skipped
```

K8s manifest 렌더링:

```powershell
kubectl kustomize k8s/base
```

결과: 성공

K8s smoke check:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\k8s-smoke.ps1
```

현재 로컬 환경에는 `kubectl` current-context가 없고 kind도 설치되어 있지 않아 실제 클러스터 smoke는 실행하지 못했다. 자세한 기록은 `docs/k8s-smoke-evidence.md`에 남긴다.

## 4. 리팩토링 단계

### Phase 1. 테스트 베이스라인

목표:

- Windows 로컬에서 order-server 테스트를 안정적으로 실행
- JaCoCo는 기본 테스트에서 분리하고 coverage profile로 이동
- 테스트용 `application.yml`로 Config Server/Eureka 의존을 끊음

결과:

- `services/order-server/TESTING.md` 작성
- 현재 전체 테스트 126개 통과

### Phase 2. 주문 흐름과 Feign 경계 지도

목표:

- 주문 생성, 결제 성공, 취소, 자동 배치 흐름에서 어떤 외부 서비스를 호출하는지 고정
- mock 기반 서비스 테스트와 실제 HTTP 계약 테스트를 분리

결과:

- `docs/order-flow-boundary-map.md` 작성
- `FeignClientBoundaryTest`로 book/member/coupon/payment 주요 계약 검증

### Phase 3. 트랜잭션 경계 리팩토링

문제:

- 클래스 레벨 `@Transactional` 때문에 외부 Feign/Rabbit I/O가 DB 트랜잭션 안에서 실행될 수 있었다.
- 주문 취소 보상 호출도 긴 트랜잭션 안에 묶일 위험이 있었다.

결과:

- 주문 오케스트레이션은 트랜잭션 밖에서 시작
- DB 상태 변경은 전담 mutation service에서 명시적 트랜잭션으로 처리
- 결제 성공, 구매 확정, 취소, 만료 취소 흐름의 경계 테스트 보강

### Phase 4. RabbitMQ 메시징 안정화

문제:

- 결제 성공 메시지 처리 실패 시 poison message가 반복 소비될 수 있었다.

결과:

- `payment-success-queue`에 DLX/DLQ 설정
- listener retry max attempts/backoff 설정
- 리스너가 예외를 삼키지 않고 Rabbit retry/DLQ 정책으로 전파하는 테스트 추가

### Phase 5. 스케줄러 멀티 인스턴스 방어

문제:

- K8s에서 order-server replica가 2개 이상이면 `@Scheduled` 작업이 Pod마다 실행될 수 있었다.

결과:

- Redis 기반 ShedLock 도입
- 결제 대기 만료 주문 취소와 일일 상태 변경 작업에 `@SchedulerLock` 적용
- ShedLock vs CronJob 판단 근거 문서화

### Phase 6. Feign 장애 격리

문제:

- 상태 변경 Feign 호출에 자동 retry를 걸면 중복 차감, 중복 확정 위험이 있다.

결과:

- Feign 기본 retry를 `Retryer.NEVER_RETRY`로 비활성화
- local/prod 프로필에 timeout과 CircuitBreaker 설정 명시
- 비동기 재처리가 가능한 결제 성공 이벤트는 RabbitMQ retry/DLQ로 격리

### Phase 7. K8s 전환 검증

목표:

- Eureka를 Service DNS로 대체
- Config Server를 ConfigMap/Secret으로 대체
- Gateway를 Ingress로 대체
- 5개 백엔드 서비스의 readiness/liveness probe 구성

결과:

- `k8s/base` manifest 구성
- `kubectl kustomize k8s/base` 렌더링 성공
- 실제 클러스터 smoke는 현재 로컬 kube context 부재로 보류

### Phase 8. React Storefront

남은 큰 작업:

- `apps/storefront`를 실제 사용자 쇼핑몰 프론트로 완성
- 도서 탐색, 상세, 장바구니, 주문서, 결제, 마이페이지 흐름 구현
- 실제 backend controller/dto 기준 API 계약과 연결

## 5. 포트폴리오 설명 문장

```text
주문 서버는 결제, 재고, 포인트, 쿠폰처럼 외부 상태 변경을 조율하는 도메인이라 장애 전파와 중복 처리 위험이 컸습니다.
DB 트랜잭션과 외부 I/O 경계를 분리하고, Feign 자동 retry를 끈 대신 timeout/CircuitBreaker와 명시적 보상 흐름을 사용했습니다.
비동기 재처리가 가능한 결제 성공 이벤트는 RabbitMQ Retry/DLQ로 격리했고, K8s 멀티 replica 환경의 스케줄러 중복 실행은 Redis ShedLock으로 방어했습니다.
```

## 6. 남은 우선순위

1. 실제 kind 또는 Docker Desktop Kubernetes 환경에서 `scripts/k8s-smoke.ps1` 성공 로그 캡처
2. React storefront 실제 연동
3. ArgoCD/Argo Rollouts 실험 증거 추가
4. README에 아키텍처 다이어그램과 화면 캡처 추가
