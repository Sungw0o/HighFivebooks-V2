# Order Server Resilience Evidence

이 문서는 order-server 리팩토링 중 스케줄러 중복 실행 방지, Feign 장애 격리, K8s 전환 검증 근거를 한 곳에 모은다.

## 1. Scheduler Lock

### 적용 대상

order-server에는 두 개의 반복 작업이 있다.

- `runOrderAutoCancel`: 10분마다 결제 대기 만료 주문을 취소한다.
- `runDailyOrderStatusUpdate`: 매일 03시에 배송 완료와 구매 확정을 자동 처리한다.

K8s에서 `order-server` replica를 2개 이상으로 늘리면 각 Pod의 `@Scheduled` 메서드가 동시에 실행될 수 있다. 같은 주문을 여러 Pod가 동시에 취소하거나 구매 확정하면 외부 보상 호출과 포인트 메시지가 중복될 위험이 있다.

### 선택

Redis 기반 ShedLock을 사용한다.

- 이미 order-server가 Redis에 연결하므로 별도 DB 테이블 없이 적용할 수 있다.
- 애플리케이션 내부 스케줄러 구조를 유지하면서 멀티 인스턴스 중복 실행만 막을 수 있다.
- lock namespace는 `highfivebooks:order`로 분리해 다른 서비스와 키 충돌을 피한다.

### 설정 근거

- 기본 lock timeout: `PT10M`
- 결제 대기 만료 정리: `lockAtMostFor=PT9M`, `lockAtLeastFor=PT30S`
- 일일 주문 상태 변경: `lockAtMostFor=PT30M`, `lockAtLeastFor=PT1M`

`lockAtMostFor`는 Pod 장애나 작업 중단 시 락이 영구 점유되지 않게 하는 안전장치다. `lockAtLeastFor`는 작업이 매우 빨리 끝나더라도 같은 스케줄 구간에서 다른 Pod가 즉시 이어 실행되는 것을 줄인다.

### CronJob과 비교

K8s CronJob은 스케줄 실행 주체를 클러스터로 옮길 수 있어 운영 표준화에는 좋다. 다만 현재 order-server의 자동 취소와 자동 상태 변경은 서비스 내부 도메인 로직을 직접 호출하고 있고, 로컬/테스트 환경에서도 같은 코드를 검증해야 한다.

이번 리팩토링에서는 애플리케이션 스케줄러를 유지하고 ShedLock으로 중복 실행만 방어했다. 추후 운영 전용 배포에서는 CronJob으로 분리할 수 있지만, 그 경우 스케줄러 전용 command 또는 내부 API 계약을 별도로 만들어야 한다.

### 검증

- `SchedulerLockConfigTest`: `@EnableScheduling`, `@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")` 검증
- `OrderCancelSchedulerLockTest`: 두 스케줄러 메서드의 `@SchedulerLock` 이름과 timeout 검증

## 2. Feign 장애 격리

### 목표

외부 서비스 지연이나 장애가 order-server의 DB 트랜잭션 점유와 요청 지연으로 번지지 않게 한다.

### 적용 내용

- `application-local.yml`, `application-prod.yml`에 Feign connect/read timeout 명시
- Spring Cloud OpenFeign circuit breaker 활성화
- Resilience4j 기본 circuit breaker 설정을 환경변수로 조정 가능하게 구성
- Feign 기본 `Retryer`는 `Retryer.NEVER_RETRY`로 비활성화

### retry 판단

주문 흐름의 핵심 외부 호출은 재고 선점, 포인트 예약/확정, 쿠폰 사용 확정처럼 상태 변경을 포함한다. 이런 호출에 Feign 레벨 자동 retry를 걸면 외부 서비스가 요청을 처리했지만 응답만 실패한 경우 중복 차감이나 중복 확정 위험이 생긴다.

따라서 기본 정책은 다음과 같다.

- Feign 클라이언트의 암묵적 retry는 비활성화한다.
- timeout과 circuit breaker로 빠르게 실패시킨다.
- 보상이 필요한 흐름은 order 도메인 서비스에서 명시적으로 처리한다.
- 결제 성공 메시지처럼 재처리 가능한 비동기 흐름은 RabbitMQ listener retry/DLQ 정책으로 격리한다.

### 검증

- `FeignResilienceConfigTest`: Feign retry 비활성화, local/prod timeout과 circuit breaker 설정 검증
- `FeignClientBoundaryTest`: 실제 Feign contract의 path/header/body와 비재시도 경계를 검증

## 3. K8s 전환 검증 연결

K8s 전환 검증은 `docs/k8s-transition-runbook.md`의 smoke check와 수동 확인 항목으로 관리한다.

검증 포인트:

- Eureka 대신 `http://*-server:8080` Service DNS 사용
- Config Server 대신 ConfigMap/Secret 환경변수 사용
- RabbitMQ `payment-success-queue` DLQ arguments 생성
- Rabbit listener retry backoff 값이 ConfigMap으로 주입
- order-server replica 2개 이상에서도 Redis ShedLock으로 스케줄러 중복 실행 방지
- liveness/readiness probe로 5개 서비스 기동 상태 확인

## 면접 설명 문장

주문 서버는 결제, 재고, 포인트, 쿠폰처럼 외부 상태 변경을 많이 호출하기 때문에 단순 retry보다 경계 분리가 중요했습니다. Feign 기본 retry는 끄고 timeout/circuit breaker로 빠르게 실패시키며, 메시지 기반 재처리가 가능한 결제 성공 이벤트는 RabbitMQ retry/DLQ로 격리했습니다. 또한 K8s에서 order-server를 여러 Pod로 늘렸을 때 스케줄러가 중복 실행될 수 있어 Redis 기반 ShedLock을 적용했습니다.
