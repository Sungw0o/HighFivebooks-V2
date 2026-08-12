# HighFiveBooks 문제 해결 포트폴리오

작성일: 2026-08-12

모든 사례는 `문제 → 선택 → 해결 → 검증 → 한계`로 정리한다. 상태는 자동 테스트·원시 측정 결과가 있으면 **검증 완료**, 코드나 과거 기록만 있으면 **부분 검증**, 설계만 있으면 **미검증**으로 표시한다.

## 포트폴리오 사용 가이드

숫자마다 검증 강도가 다르다. k6 JSON이 남은 도서 조회는 직접 측정값이지만, 주문 부하는 요약 문서만 있어 “측정 기록 기준”이라고 한정한다. 생일 쿠폰 배치 수치는 원시 로그가 없어 외부 문서에 싣지 않는다.

면접에서는 사례를 모두 나열하지 않고 JPA pagination, TCC 멱등/보상, RabbitMQ DLQ 세 가지를 우선한다. Docker Compose와 RDS 복제는 이 실험을 가능하게 한 재현 기반으로 짧게 연결한다.

각 사례의 상태는 구현량이 아니라 증거 수준을 뜻한다. “부분 검증”은 실패가 아니라 확인 범위가 단위 테스트·과거 실행 기록·구성 코드 중 일부에 머문다는 표시다. “미검증” 항목은 선택 이유와 후속 실험 계획까지만 설명하고 성과 수치로 사용하지 않는다.

## 백엔드

### 1. JPA pagination 병목 개선

상태: **검증 완료**

- **문제**: 도서 157,118건에서 collection `EntityGraph`와 `Pageable`을 함께 사용한 목록 조회 p95가 9,039.51ms까지 증가했다.
- **선택**: DTO projection·native query보다 기존 코드 영향이 작은 조회 목적별 fetch graph 분리를 택했다.
- **해결**: 목록 조회는 `publisher`만 즉시 로딩하고, `bookAuthors.author`는 상세 조회에서만 로딩하도록 분리했다.
- **검증**: 동일 k6 스크립트의 저장 결과에서 목록 p95 9,039.51ms → 51.97ms, 평균 6,907.78ms → 44.17ms, 실패율 0%를 확인했다. 1회 전후 측정 기준 약 174배다.
- **한계**: 전후 요청 수가 160건과 295건으로 다르고 30초 측정 1회이므로, 반복 실험의 중앙값은 아니다.

근거: `PERFORMANCE_REPORT.md`, `JPA_PERFORMANCE_REPRODUCTION.md`, `perf/results/book-read-*.json`

### 2. 생일 쿠폰 배치 조회 구조 개선

상태: **미검증**

- **문제**: 생일자 조회의 `MONTH(birth_date)`가 인덱스를 활용하기 어렵고, coupon Reader가 전달한 `page/size`를 member API가 반영하지 않아 chunk 의도와 실제 조회가 달랐다.
- **선택**: MySQL에서 예측 가능한 `birth_month` 컬럼과 `(birth_month, id)` 복합 인덱스, API pagination 정합화를 설계했다.
- **해결**: member 생일자 API에 `page/size`를 연결하고 coupon Writer의 JDBC batch 경로를 확인했다.
- **검증**: 코드 경로는 확인했지만 `EXPLAIN` 전후와 전체 배치 시간은 측정하지 못했다.
- **한계**: 기존 문서의 회원 2,400만 명·대상 200만 명·4시간 이상 수치는 원시 실행 로그가 없어 이력서에서 제외한다.

근거: `DB_BATCH_TUNING.md`, member/coupon server 코드

### 3. 주문 생성 API 동시 사용자 안정성

상태: **부분 검증**

- **문제**: 주문 서버는 도서·회원·쿠폰 도메인을 호출하므로 mock 테스트만으로 통합 안정성을 설명하기 어려웠다.
- **선택**: Docker Compose로 MSA를 기동하고 k6로 실제 주문 API를 호출했다.
- **해결**: 비회원 주문을 1·5·10·20·50 VU 단계로 실행했다.
- **검증**: 50 VU/2분에서 5,435건, 44.89 req/s, 실패율 0%, p95 133.29ms를 기록했다.
- **한계**: `sleep(1)`이 있어 한계 부하가 아닌 동시 사용자 안정성 결과이며, 50 VU 원시 summary JSON이 저장소에 없다.

근거: `PERFORMANCE_TEST_PROGRESS_2026-07-09.md`, `perf/k6/order-create-baseline.js`

### 4. 결제 성공 이벤트 Retry/DLQ

상태: **부분 검증**

- **문제**: 금액 불일치나 후처리 실패 메시지가 무한 재소비되면 정상 주문 처리까지 방해할 수 있었다.
- **선택**: 동기 Feign 재호출이나 예외 무시 대신 RabbitMQ listener retry 후 DLQ 격리를 택했다.
- **해결**: `payment-success-queue`에 DLX/routing key를 연결하고 `RejectAndDontRequeueRecoverer`를 적용했다.
- **검증**: 구성·listener 단위 테스트와 수동 poison message의 DLQ 1건 기록이 있다. 실제 브로커 reject→DLQ 자동 통합 테스트도 추가했다.
- **한계**: 현재 Docker 엔진이 꺼져 자동 통합 테스트는 조건부 skip됐고, 1,000건 처리량과 retry 로그는 미측정이다.

근거: `RabbitMqConfigTest`, `PaymentMessageListenerTest`, `RabbitMqDlqIntegrationTest`

### 5. TCC Confirm/Cancel 멱등성과 실패 보상

상태: **부분 검증**

- **문제**: 결제 이벤트·취소 요청 중복이나 timeout은 재고·포인트를 이중 반영하거나 부분 보상 상태를 만들 수 있다.
- **선택**: 주문 상태 gate와 도메인별 idempotency key를 유지하고 실패 지점을 테스트로 고정했다.
- **해결**: 재고 Confirm은 `CONFIRM-{orderKey}`, Restore는 요청 키, 포인트는 주문 거래 상태로 중복을 차단한다. 주문 Cancel은 이미 `CANCELED`면 종료한다.
- **검증**: 중복 Confirm/Cancel, 포인트 Cancel 실패, 결제 Cancel timeout, 재고 Confirm/Restore 1회 변경 테스트가 통과했다.
- **한계**: 외부 보상 완료 단계를 영속화하지 않아 동시 요청·응답 유실·프로세스 종료에 대한 durable retry는 없다.

근거: `TCC_FAILURE_MATRIX.md`, order/book/member 테스트

## 클라우드·플랫폼

### 6. RDS 데이터의 로컬 재현

상태: **검증 완료**

- **문제**: 작은 seed로는 ORM pagination과 검색 I/O 병목이 드러나지 않았다.
- **선택**: 전체 서비스를 상시 클라우드에 두지 않고 RDS 데이터를 dump로 로컬에 복제했다.
- **해결**: `mysqldump` 압축 덤프와 PowerShell import 흐름을 구성했다.
- **검증**: 로컬 MySQL의 도서 157,118건으로 k6 전후 결과를 저장해 JPA 병목을 재현했다.
- **한계**: dump import 시간과 테이블별 row count 캡처는 남아 있지 않다.

근거: `PERFORMANCE_REPORT.md`, `scripts/import-book-dump.ps1`

### 7. Docker Compose MSA 실행 안정화

상태: **부분 검증**

- **문제**: 서비스별 수동 실행은 주문·결제·재고 흐름 재현이 느리고 환경 차이에 취약했다.
- **선택**: 빠른 반복 실험과 서비스 간 연동 재현을 위해 Docker Compose를 사용했다.
- **해결**: 5개 백엔드와 MySQL·Redis·RabbitMQ·Elasticsearch·MinIO를 묶고 Nori 이미지, mail health, payment `curl` 문제를 정리했다.
- **검증**: 과거 전체 healthy와 책 API 조회 기록이 있다.
- **한계**: 이번 세션에는 Docker 엔진이 없어 새 PC 기준 재현을 다시 실행하지 못했다.

근거: `LOCAL_REPRODUCIBILITY.md`, Compose와 실행 스크립트

## 핵심 기술

Java 21, Spring Boot 3.5.7, Spring Data JPA, OpenFeign/Resilience4j, RabbitMQ, MySQL, Redis, Elasticsearch, Docker Compose, JUnit 5·Mockito·k6.

기술은 전체 나열보다 사례별 선택 이유와 검증 근거를 우선하며, 실제 실행 근거가 부족한 기술은 핵심 목록에서 제외한다.

## 검증 요약

| 상태 | 사례 | 외부 표현 |
|---|---|---|
| 검증 완료 | JPA pagination, RDS 데이터 재현 | 저장된 수치와 확인 범위를 그대로 사용 |
| 부분 검증 | 주문 부하, RabbitMQ DLQ, TCC, Compose | “구성/단위 테스트/당시 실행 기록 기준”을 함께 표기 |
| 미검증 | 생일 쿠폰 배치 최종 성능 | 문제와 설계만 설명하고 개선 수치 제외 |

최종 한 줄은 다음 범위가 안전하다. “실제 규모 데이터로 JPA pagination 병목을 재현·개선하고, 주문 TCC 멱등성과 RabbitMQ DLQ를 테스트했습니다.” 이 문장은 자동 보상 완성이나 운영 SLO까지 주장하지 않는다.
