# HighFiveBooks Refactoring Roadmap

작성일: 2026-07-06

## 0. 현재 판단

`C:\Users\성우\Desktop\Highfivebooks`는 비어 있던 작업 폴더였고, Git 저장소가 아니었다.

GitHub 확인 결과 `nhnacademy-be12-high-five` 조직에 원본 레포들이 있으며, 개인 계정에는 `Sungw0o/order_server` fork가 이미 존재한다. 따라서 작업 방식은 다음이 가장 좋다.

- 메인 작업 레포: `Sungw0o/order_server`
- 기준 원본: `nhnacademy-be12-high-five/order_server`
- 보조 분석 레포: `nhnacademy-be12-high-five/coupon_server`
- 이 폴더에 클론된 경로:
  - `C:\Users\성우\Desktop\Highfivebooks\order_server`
  - `C:\Users\성우\Desktop\Highfivebooks\coupon_server`
  - `C:\Users\성우\Desktop\Highfivebooks\book_server`
  - `C:\Users\성우\Desktop\Highfivebooks\member_server`
  - `C:\Users\성우\Desktop\Highfivebooks\payment_server`
  - `C:\Users\성우\Desktop\Highfivebooks\front_server_original`
  - `C:\Users\성우\Desktop\Highfivebooks\highfivebooks-console` (초기 실험 산출물, V2 기준에서는 storefront로 대체)
  - `C:\Users\성우\Desktop\Highfivebooks\k8s-manifests`

결론: MSA는 유지한다. 모놀리스로 합치지 않고 Kubernetes-native 구조로 전환한다. 본인 담당 도메인인 `order_server`를 깊게 리팩토링하고, `coupon_server`는 메시징/정합성 패턴을 배우고 order 연동 안정성을 보강하는 학습 리팩토링 대상으로 둔다. 기존 Thymeleaf `front_server`는 직접 고치지 않고, React/Vite 기반 `apps/storefront`로 실제 사용자 쇼핑몰 프론트를 새로 구현한다.

## 1. 리팩토링 범위

### 반드시 한다

1. `order_server` 테스트 실행 환경 안정화
2. `OrderServiceImpl` 클래스 레벨 `@Transactional` 제거 및 트랜잭션 범위 재설계
3. 결제 성공 RabbitMQ 소비 흐름에 Retry/DLQ 정책 추가
4. 주문 자동 취소/자동 구매확정 스케줄러 중복 실행 방지
5. Feign timeout 및 장애 격리 정책 추가
6. React storefront에서 도서 탐색, 장바구니, 주문, 결제, 마이페이지 플로우 구현
7. README/포트폴리오 문서화

### 하면 좋다

1. `coupon_server`의 DLQ/Retry 패턴을 order에 이식한 근거 문서화
2. `coupon_server`의 `useCoupon` 멱등성 보강
3. Redis Lua 발급 후 DB 저장 실패 시 보상 로직 검증
4. k6 또는 nGrinder로 전후 수치 만들기

### 하지 않는다

1. `front_server` 리팩토링
2. `book_server`, `member_server`의 대규모 코드 수정
3. Eureka/Config/Gateway를 유지한 채 포트폴리오 주제로 삼기
4. 상시 클라우드 운영
5. MSA를 모놀리스로 합치기

## 2. 확인된 베이스라인

### GitHub/클론

- 조직: `nhnacademy-be12-high-five`
- 원본 레포: `order_server`, `coupon_server`, `book_server`, `member_server`, `payment_server`, `eureka_server`, `config_server`, `gateway`, `front_server`
- 개인 fork: `Sungw0o/order_server`
- `order_server` remote:
  - `origin`: `https://github.com/Sungw0o/order_server.git`
  - `upstream`: `https://github.com/nhnacademy-be12-high-five/order_server.git`

### 코드에서 확인한 핵심 문제

- `OrderServiceImpl`에 클래스 레벨 `@Transactional`이 걸려 있어 주문 생성 중 Feign 호출이 DB 트랜잭션 안에서 수행된다.
- `processPaymentSuccessMessage`도 같은 클래스 트랜잭션의 영향을 받고, 쿠폰 확정 Feign 호출이 트랜잭션 내부에서 실행된다.
- `RabbitMqConfig`는 JSON converter와 listener factory만 있고 DLX/DLQ/Retry 설정이 없다.
- `PaymentMessageListener`는 예외 발생 시 그대로 다시 던지므로 poison message 격리 정책이 없다.
- `OrderCancelScheduler`는 `@Scheduled`만 있고 멀티 인스턴스 중복 실행 방지가 없다.
- `pom.xml`에는 Resilience4j/ShedLock 의존성이 없다.
- `application.yml`에는 Feign timeout/retry/circuit breaker 정책이 보이지 않는다.

### 테스트 상태

`.\mvnw.cmd test` 실행 결과, 테스트가 0개 실행된 채 Surefire fork가 실패했다.

원인은 코드 테스트 실패가 아니라 Windows 한글 사용자 경로가 Surefire/Jacoco fork 경로에서 깨지는 문제로 보인다.

따라서 첫 작업은 기능 리팩토링이 아니라 테스트 실행 안정화다.

### React storefront 방향

초기에는 `highfivebooks-console`을 React/Vite/TypeScript로 생성했지만, 최종 방향은 운영 콘솔이 아니라 기존 Thymeleaf 프론트를 대체하는 사용자용 `apps/storefront`다.

현재 상태:

- Claude가 `docs/STOREFRONT_API_CONTRACT.md`에 화면별 API 계약을 정리하기 시작했다.
- storefront 구현 전 실제 backend controller/dto 기준으로 계약을 먼저 맞춘다.
- mock adapter는 실제 API 계약과 동일한 타입으로만 만든다.

주의:

- 임의 endpoint를 확정하지 않는다.
- 기존 `front_server`는 참고만 하고 직접 리팩토링하지 않는다.

## 3. 브랜치/PR 전략

`dev`에서 바로 작업하지 말고 작은 브랜치로 나눈다.

1. `refactor/test-baseline`
   - Maven Surefire/Jacoco 실행 환경 정리
   - 테스트가 로컬에서 일관되게 도는 상태 확보
   - 실패 테스트가 있다면 기능 변경 없이 원인만 분류

2. `refactor/order-transaction-boundary`
   - 클래스 레벨 `@Transactional` 제거
   - 조회 메서드에는 `readOnly = true`
   - DB 상태 변경 메서드는 명시적 트랜잭션
   - 외부 Feign/Rabbit 호출은 가능한 한 트랜잭션 밖으로 이동
   - 주문 생성, 결제 후처리, 취소 보상 테스트 보강

3. `refactor/payment-message-dlq`
   - payment success queue에 DLX/DLQ 선언
   - 영구 오류와 일시 오류 분기
   - Retry max attempts/backoff 설정
   - 금액 불일치 메시지가 DLQ로 격리되는 테스트 추가

4. `refactor/scheduler-lock`
   - ShedLock JDBC 또는 Redis 도입
   - `cancelExpiredOrders`, `autoConfirmPurchase` 중복 실행 방지
   - 멀티 인스턴스 시나리오를 문서화

5. `refactor/feign-resilience`
   - Feign connect/read timeout 명시
   - 핵심 외부 호출에 Retry/CircuitBreaker 적용
   - TCC Try 실패는 빠르게 실패하고 보상 흐름으로 빠지게 정리

6. `docs/portfolio-evidence`
   - README 개선
   - 전후 구조도
   - 실험 시나리오
   - 면접 답변 초안

## 4. 구현 순서

### Phase 0. 작업장 정리

- `order_server`를 기준 작업 레포로 확정
- `coupon_server`는 비교 분석용으로 유지
- `book_server`, `member_server`, `payment_server`는 런타임 의존성으로 클론 완료
- `front_server_original`은 Thymeleaf 원본 참고용으로만 보존
- React 프론트는 `apps/storefront`로 구현
- Kubernetes 설정은 각 서비스 레포 안에 흩뿌리지 않고 `k8s-manifests`에 모은다.
- 원본 팀 프로젝트와 개인 리팩토링 버전의 관계를 README에 명확히 적는다.

완료 조건:

- 작업 브랜치 전략 확정
- 로컬 테스트 커맨드 확정
- 현재 실패 원인 기록

### Phase 1. 테스트 베이스라인

목표: 리팩토링 전에 안전망을 켠다.

작업:

- [x] Surefire/Jacoco fork 실패 재현 및 해결
- [x] JaCoCo를 기본 빌드에서 `coverage` Maven profile로 분리
- [x] CI에서 실행할 테스트 명령과 로컬 Windows 명령을 분리
- [x] 테스트 전용 `application.yml`로 Spring Cloud Config/Eureka 비활성화
- [x] `TESTING.md`에 로컬/커버리지 테스트 방법 문서화
- 핵심 테스트 목록 확인
  - 주문 생성
  - 결제 성공 메시지 후처리
  - 주문 취소/보상
  - 스케줄러 위임
  - Rabbit listener

완료 조건:

- `.\mvnw.cmd test` 통과: 98 tests, 0 failures, 0 errors, 0 skipped
- 실패가 남는 경우 기능 실패와 환경 실패가 분리되어 문서화

### Phase 1.5. 주문 흐름 지도와 테스트 분류

목표: order-server가 어떤 외부 도메인과 어떤 계약으로 통신하는지 고정한다.

작업:

- [x] `BookClient`, `MemberClient`, `CouponClient`, `PaymentClient`, `CartClient` 호출 목록 정리
- [x] 주문 생성 / 결제 성공 / 주문 취소 / 자동 스케줄러 흐름별 외부 통신 지도 작성
- [x] 현재 테스트를 Unit / Slice / Context / 미보유 Boundary test로 분류
- [x] storefront에서 우선 참고할 order API 후보 정리

산출물:

- `docs/order-flow-boundary-map.md`

확인된 핵심 리스크:

- `OrderServiceImpl` 클래스 레벨 `@Transactional` 때문에 외부 Feign I/O가 트랜잭션 안에서 실행될 가능성이 있다.
- `OrderCreateService.createOrderInTransaction` 내부에도 포인트 예약과 장바구니 삭제 외부 호출이 있다.
- `OrderCancelService.cancelOrderTransactional`은 `REQUIRES_NEW` 트랜잭션 안에서 payment/member/coupon/book 외부 호출을 수행한다.
- Feign 실제 HTTP 계약을 검증하는 boundary test는 아직 없다.
- RabbitMQ `payment-success-queue`에 Retry/DLQ 정책과 테스트가 아직 없다.

### Phase 2. 트랜잭션 경계 리팩토링

목표: 외부 I/O 때문에 DB 커넥션을 오래 물고 있는 구조를 끊는다.

작업:

- `OrderServiceImpl` 클래스 레벨 `@Transactional` 제거
- `createOrder`에서 회원 등급 조회, 재고 선점, 배송비 계산은 트랜잭션 밖에서 수행
- DB 저장은 `OrderCreateService.createOrderInTransaction`에 한정
- 결제 성공 후처리도 쿠폰/포인트/외부 호출 위치를 재검토
- 금액 타입 비교를 `int/Long` 혼용에서 명확한 타입 비교로 정리
- "테스트 통과용" 같은 임시 주석 제거

완료 조건:

- 주문 생성 성공/실패/보상 테스트 통과
- 외부 호출 실패 시 보상 호출이 유지됨
- 포트폴리오에 "트랜잭션 범위 축소"로 설명 가능한 전후 차이가 생김

### Phase 3. 메시징 안정화

목표: 결제 성공 poison message가 무한 재소비되지 않게 한다.

작업:

- `payment-success-queue`에 DLX/DLQ 설정
- Retry max attempts와 backoff 설정
- 영구 오류는 즉시 DLQ 또는 제한 재시도 후 DLQ
- 일시 오류는 제한 재시도
- `coupon_server`의 RabbitMQ 설정을 참고해 order 표준으로 이식

완료 조건:

- 금액 불일치 메시지가 무한 requeue되지 않음
- 정상 메시지 처리량이 poison message에 막히지 않음
- 테스트 또는 로컬 RabbitMQ 재현 로그 확보

### Phase 4. 스케줄러 멀티 인스턴스 방어

목표: 주문 서버 2개 이상 실행 시 자동 취소/구매확정이 중복 실행되지 않게 한다.

작업:

- ShedLock 도입 방식 결정
  - 이미 Redis를 쓰므로 Redis lock이 간단하다.
  - 감사 추적과 DB 일관성을 강조하려면 JDBC도 가능하다.
- `OrderCancelScheduler`에 락 적용
- 대안으로 K8s CronJob 분리도 문서에 비교만 남긴다.

완료 조건:

- 동일 시각 2 인스턴스 실행 시 한 인스턴스만 작업 수행
- "ShedLock vs CronJob" 면접 답변 정리

### Phase 5. Feign 장애 격리

목표: 외부 서비스 지연/장애가 주문 서버 전체 장애로 번지지 않게 한다.

작업:

- Feign connect/read timeout 설정
- book/member/coupon/payment 호출 중요도별 Retry 여부 결정
- 재고 선점/쿠폰 확정처럼 중복 호출 위험이 있는 API는 무조건 재시도하지 않는다.
- Resilience4j CircuitBreaker로 빠른 실패와 fallback 정리

완료 조건:

- book 서버 지연 시 주문 API가 오래 매달리지 않음
- 실패 시 보상/사용자 응답이 명확함
- 설정값이 README에 설명됨

### Phase 6. 쿠폰 서버 학습 리팩토링

목표: 본인 담당이 아닌 코드를 "내가 다 만들었다"처럼 보이지 않게 하면서, 리뷰/개선 역량을 보여준다.

작업:

- 쿠폰 서버의 DLQ/Retry 구조를 분석해 order에 적용한 근거 작성
- `useCoupon`을 같은 `orderId` 재호출에는 성공 처리하도록 멱등화 검토
- Redis Lua 성공 후 DB 저장 실패 시 Redis 보상 필요 여부를 테스트로 재현

완료 조건:

- 포트폴리오 표현이 "팀 코드 리뷰 및 개선 기여"로 정직하게 정리됨
- order 재시도와 coupon 멱등성 충돌 시나리오가 설명 가능함

### Phase 7. K8s/GitOps 전환 검증

목표: 상시 운영이 아니라 전환 검증 실험으로 증빙을 만든다.

작업:

- kind 또는 k3s 로컬 클러스터
- order/coupon/book/member/payment 최소 Deployment/Service
- MySQL/Redis/RabbitMQ 매니페스트
- Eureka는 Service DNS로 대체
- Config Server는 ConfigMap/Secret으로 대체
- Gateway는 Ingress로 대체
- ArgoCD sync
- Argo Rollouts canary 또는 blue-green 1회 시연

완료 조건:

- 전환 전/후 구성도 2장
- Rollout 캡처 또는 GIF
- README로 재현 가능

### Phase 8. React storefront

목표: 기존 Thymeleaf `front_server`를 직접 고치지 않고, React/Vite 기반 사용자 쇼핑몰 프론트로 대체한다.

작업:

- 홈/도서 목록/검색/카테고리
- 도서 상세/리뷰
- 장바구니
- 주문서
- 배송지
- 쿠폰/포인트 적용
- 결제 성공/실패 처리
- 주문 완료
- 마이페이지, 주문 내역, 주문 상세
- 주문 취소/반품 신청

완료 조건:

- React 앱이 로컬에서 실행됨
- 백엔드 실제 API 계약과 맞는 adapter/type이 존재함
- mock adapter와 real adapter가 동일 타입을 사용함
- 면접에서 "기존 Thymeleaf를 React로 대체한 이유"를 사용자 경험/유지보수/프론트 분리 관점으로 설명 가능

## 5. 포트폴리오 스토리

핵심 문장:

> 팀 프로젝트의 전체 코드를 건드리기보다, 본인 담당 도메인인 주문 서버에서 트랜잭션 경계, 메시징 안정성, 멀티 인스턴스 스케줄링 문제를 선별해 리팩토링했습니다. 쿠폰 서버의 DLQ/Retry 패턴을 분석해 주문 서버에 이식했고, K8s 전환 실험으로 Eureka/Config/Gateway를 클라우드 네이티브 구성으로 대체했습니다.

이력서 bullet 후보:

- 주문 생성 흐름의 클래스 레벨 트랜잭션을 제거하고 외부 Feign I/O와 DB 트랜잭션을 분리해 커넥션 점유 시간을 줄이는 구조로 개선
- 결제 성공 RabbitMQ 이벤트에 Retry/DLQ 정책을 추가해 poison message 무한 재소비 위험을 격리
- 멀티 인스턴스 주문 서버의 스케줄러 중복 실행 가능성을 ShedLock으로 방지
- Spring Cloud 기반 MSA를 로컬 K8s 환경으로 전환해 Service DNS, ConfigMap, Ingress, Argo Rollouts 적용을 검증

## 6. 다음 액션

바로 시작한다면 순서는 이것이다.

1. `order_server`에서 `refactor/test-baseline` 브랜치 생성
2. Surefire/Jacoco fork 실패 해결
3. 현재 테스트 목록이 실제로 실행되는지 확인
4. `OrderServiceImpl` 트랜잭션 경계 테스트 추가
5. 클래스 레벨 `@Transactional` 제거
