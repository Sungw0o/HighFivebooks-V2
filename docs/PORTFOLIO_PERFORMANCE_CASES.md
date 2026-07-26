# HighFiveBooks Performance Portfolio Cases

작성일: 2026-07-09

이 문서는 HighFiveBooks V2를 포트폴리오와 면접에서 설명하기 위한 성능·부하·운영 사례 카드다. 수치가 확인된 항목은 그대로 사용하고, 아직 실행이 필요한 항목은 `해야됨`으로 표시한다.

## 사용 기준

- `완료`: 코드, 문서, 테스트 또는 측정 수치가 로컬 레포에 남아 있다.
- `보강 필요`: 코드/하네스는 있으나 최종 수치 캡처가 부족하다.
- `해야됨`: 설계 방향만 있고 실제 측정이나 구현 결정이 필요하다.

## 백엔드 5개

### 1. 대용량 도서 목록 조회 JPA pagination 병목 개선

상태: 완료

하다 보니 AWS RDS에서 확보한 도서 데이터를 로컬 MSA 환경에 복제한 뒤, 157,118건 데이터 기준 도서 목록 API가 p95 9039.51ms까지 느려지는 문제가 생겼습니다.

처음에는 DB 인덱스 문제를 의심했지만, Hibernate SQL과 repository 설정을 확인해 보니 `BookRepository.findAll(Pageable)`에 collection `EntityGraph(bookAuthors, bookAuthors.author)`가 적용되어 DB pagination이 아니라 메모리 pagination이 발생하는 구조였습니다.

DTO projection, native query, fetch graph 분리 중에서 고민했고, 기존 서비스 코드 변경 폭이 가장 작은 fetch graph 분리가 더 낫다고 판단했습니다. 목록 조회에서는 `publisher`만 즉시 로딩하고, 저자 정보는 상세 조회 `findById`에서만 로딩하도록 분리했습니다.

그 결과 같은 k6 시나리오에서 목록 조회 p95가 9039.51ms에서 51.97ms로 낮아졌고, p95 기준 약 173.94배 개선됐습니다.

근거:

- `docs/PERFORMANCE_REPORT.md`
- `perf/results/book-read-baseline-20260708-234828.json`
- `perf/results/book-read-optimized-20260708-235147.json`

### 2. 생일 쿠폰 발급 배치 처리 병목 개선

상태: 보강 필요

하다 보니 2,400만 명 회원 중 약 200만 명 대상 생일 쿠폰 발급 배치가 4시간 이상 소요되는 문제가 생겼습니다.

MySQL 실행 계획에서는 `MONTH(birth_date)` 조건 때문에 생일 회원 조회가 풀 테이블 스캔으로 동작했고, 작은 chunk와 잦은 commit, JPA 기반 쓰기 비용도 함께 병목이 됐습니다. 추가로 코드를 확인해 보니 coupon-server Reader는 `page`, `size`를 넘기고 있었지만 member-server API가 이를 받지 않아 chunk 단위 조회 의도가 실제 pagination으로 이어지지 않았습니다.

query function index, generated column, 애플리케이션 관리 컬럼 중에서 고민했고, MySQL에서 안정적으로 인덱스를 태울 수 있는 `birth_month` 컬럼과 `(birth_month, id)` 복합 인덱스가 더 낫다고 판단했습니다. 우선 member-server 생일자 조회 API에 `page`, `size`를 반영해 Reader chunk와 API pagination을 맞췄고, Writer는 JDBC batch insert 경로를 확인했습니다.

아직 최종 수치는 `birth_month` 적용 전후 `EXPLAIN`과 배치 실행 시간 측정이 필요합니다.

근거:

- `docs/DB_BATCH_TUNING.md`
- `services/member-server/src/main/java/com/nhnacademy/member_server/controller/MemberController.java`
- `services/member-server/src/main/java/com/nhnacademy/member_server/repository/MemberRepository.java`

해야됨:

- `MONTH(birth_date)` baseline `EXPLAIN` 캡처
- `birth_month` 적용 후 `EXPLAIN` 캡처
- chunk size별 처리량과 전체 배치 시간 측정

### 3. 주문 생성 API 동시 사용자 안정성 검증

상태: 완료

하다 보니 주문 서버가 book, member, coupon, payment 도메인과 모두 통신해서 단위 테스트만으로는 실제 주문 생성 안정성을 설명하기 어려운 문제가 생겼습니다.

Controller 단위 테스트, 서비스 mock 테스트, Docker Compose 통합 부하 테스트 중에서 고민했고, 포트폴리오 근거로는 로컬 MSA를 모두 띄운 상태에서 k6로 주문 API를 직접 호출하는 방식이 더 낫다고 판단했습니다.

비회원 주문 생성 시나리오를 기준으로 1, 5, 10, 20, 50 VU 단계 테스트를 실행했고, 50 VU / 2분 기준 5435건 요청, 실패율 0%, p95 133.29ms를 확인했습니다.

근거:

- `docs/PERFORMANCE_TEST_PROGRESS_2026-07-09.md`
- `perf/k6/order-create-baseline.js`

보강 필요:

- 회원 주문, 쿠폰, 포인트 사용을 포함한 확장 시나리오
- summary export JSON 파일 저장

### 4. 결제 성공 이벤트 Retry/DLQ 격리

상태: 완료

하다 보니 결제 성공 이후 금액 불일치나 후처리 실패 메시지가 들어오면 같은 메시지를 계속 재소비하거나 주문 상태 변경 흐름에 장애가 전파될 수 있는 문제가 생겼습니다.

Feign 동기 호출로 결제 성공을 처리하는 방식, listener에서 예외를 삼키는 방식, RabbitMQ retry/DLQ로 격리하는 방식 중에서 고민했고, 결제 성공 이벤트는 재처리 가능한 비동기 흐름이므로 RabbitMQ retry/DLQ가 더 낫다고 판단했습니다.

`payment-success-queue`에 dead-letter exchange와 routing key를 붙이고, listener retry 이후 `RejectAndDontRequeueRecoverer`로 DLQ에 격리되도록 구성했습니다. 정상 메시지는 주문 상태를 `PAYMENT_WAITING`에서 `PREPARING`으로 변경했고, 금액 불일치 메시지는 DLQ 1건 이동을 확인했습니다.

근거:

- `docs/PERFORMANCE_TEST_PROGRESS_2026-07-09.md`
- `docs/order-resilience-evidence.md`
- `services/order-server/src/main/java/com/nhnacademy/order_server/config/RabbitMqConfig.java`
- `scripts/publish-payment-success.ps1`
- `scripts/show-rabbit-queues.ps1`

보강 필요:

- 1000건 이상 메시지 처리량 측정
- retry 횟수와 DLQ 이동 로그 캡처

### 5. 재고 선점·확정·복구 멱등성 검증

상태: 보강 필요

하다 보니 주문 생성과 결제 성공 사이에 재고를 언제 차감할지 명확하지 않으면, 결제 실패나 중복 이벤트에서 재고가 두 번 차감되거나 복구되지 않는 문제가 생겼습니다.

주문 생성 시 즉시 차감, 결제 성공 후 차감, 선점 후 확정/복구 방식 중에서 고민했고, 결제 전 이탈과 실패 보상을 고려하면 선점 후 확정/복구 방식이 더 낫다고 판단했습니다.

현재 book-server에는 `StockHeld`와 `StockIdempotencyRecord`가 있어 선점, 확정, 복구의 멱등성 근거를 남길 수 있습니다. 다만 실제 동시성 수치로 말하려면 주문 생성 실패 보상, 결제 성공 중복 메시지, 취소/반품 복구를 각각 k6 또는 통합 테스트로 측정해야 합니다.

근거:

- `docs/order-flow-boundary-map.md`
- `services/book-server/src/main/java/com/nhnacademy/book_server/entity/StockHeld.java`
- `services/book-server/src/main/java/com/nhnacademy/book_server/entity/StockIdempotencyRecord.java`

해야됨:

- 중복 결제 성공 메시지에서 재고 차감 1회 검증
- 주문 취소/반품 복구 중복 요청에서 재고 복구 1회 검증
- 최종 재고 수량 검증 SQL 캡처

## 클라우드 / 인프라 5개

### 1. RDS 대용량 도서 데이터 확보 및 로컬 복제

상태: 완료

하다 보니 작은 seed 데이터만으로는 JPA fetch, pagination, 검색 인덱스, DB I/O 병목이 드러나지 않는 문제가 생겼습니다.

전체 서비스를 클라우드에 계속 띄우는 방식, 로컬 seed만 쓰는 방식, RDS에서 필요한 도서 데이터만 확보해 로컬로 복제하는 방식 중에서 고민했고, 프리티어 비용과 반복 실험 속도를 고려하면 RDS 데이터 확보 후 로컬 복제가 더 낫다고 판단했습니다.

그 결과 로컬 MySQL에서 157,118건 도서 데이터를 기준으로 k6 성능 테스트를 수행할 수 있었고, 실제로 도서 목록 p95 9.03초 병목을 찾아냈습니다.

근거:

- `docs/PERFORMANCE_REPORT.md`
- `scripts/import-book-dump.ps1`

보강 필요:

- dump 파일명, import 시간, 테이블별 row count 캡처

### 2. Docker Compose 기반 MSA 통합 실행 안정화

상태: 완료

하다 보니 서버를 각각 직접 실행하면 주문, 결제, 재고, 쿠폰, 포인트 흐름을 한 번에 재현하기 어렵고, 환경 차이 때문에 다른 컴퓨터에서 같은 테스트를 반복하기 어려운 문제가 생겼습니다.

개별 IDE 실행, Kubernetes 선전환, Docker Compose 통합 실행 중에서 고민했고, 로컬에서 빠르게 반복 측정하려면 Compose가 더 낫다고 판단했습니다.

MySQL, Redis, RabbitMQ, Elasticsearch, MinIO와 5개 백엔드 서비스를 Compose로 묶고, `.env.example`, `local-up.ps1`, `build-services.ps1`, `LOCAL_REPRODUCIBILITY.md`를 정리했습니다. payment-server healthcheck용 `curl` 누락, mail health indicator, Elasticsearch Nori 이미지 문제도 실행 과정에서 정리했습니다.

근거:

- `docker-compose.yml`
- `docs/LOCAL_REPRODUCIBILITY.md`
- `scripts/local-up.ps1`
- `scripts/build-services.ps1`

보강 필요:

- 새 PC에서 `git clone -> .env -> docker compose up` 검증 캡처

### 3. Elasticsearch Nori 검색 인프라 환경 일관성 확보

상태: 보강 필요

하다 보니 기본 Elasticsearch 이미지만 사용하면 한글 검색 분석기가 환경마다 다르게 동작하거나 Nori plugin 누락으로 검색 품질과 인덱스 생성이 흔들리는 문제가 생겼습니다.

컨테이너 시작 시 plugin 설치, 별도 커스텀 이미지, 검색 기능을 로컬에서 제외하는 방식 중에서 고민했고, Compose와 K8s에서 같은 이미지를 쓰는 커스텀 이미지 방식이 더 낫다고 판단했습니다.

현재 Nori plugin 포함 이미지를 기준으로 환경을 맞추는 방향은 정리됐지만, 검색 API 자체의 before/after p95와 analyzer 결과 캡처는 아직 필요합니다.

근거:

- `docker-compose.yml`
- `services/book-server/src/main/resources/Elastic/analysis/high-five.json`
- `services/book-server/src/main/resources/Elastic/analysis/synonyms.txt`

해야됨:

- Nori plugin 설치 확인 API 캡처
- 검색 API k6 측정
- analyzer 결과 샘플 캡처

### 4. Kubernetes 전환 smoke, readiness, ingress 검증

상태: 보강 필요

하다 보니 Pod가 `Running`이어도 DB, RabbitMQ, Elasticsearch 같은 의존성이 준비되지 않아 실제 API 요청을 받을 수 없는 문제가 생겼습니다.

단순 `kubectl get pods`, 수동 curl, smoke script 방식 중에서 고민했고, 전환 근거로는 rollout, readiness, Service DNS, Ingress 접근을 한 번에 확인하는 smoke script가 더 낫다고 판단했습니다.

현재 `k8s/base`, `k8s/rollouts`, `scripts/k8s-smoke.ps1`, `docs/k8s-transition-runbook.md`는 준비되어 있습니다. 다만 현재 세션에서는 K8s endpoint가 떠 있지 않아 최신 smoke 결과 캡처는 아직 필요합니다.

근거:

- `docs/k8s-transition-runbook.md`
- `docs/k8s-smoke-evidence.md`
- `scripts/k8s-smoke.ps1`
- `k8s/base`

해야됨:

- `kubectl kustomize k8s/base` 결과 캡처
- `scripts/k8s-smoke.ps1` 최신 실행 결과 캡처
- Ingress 기준 API curl 캡처

### 5. Cloudflare + S3 + EC2 Ingress + RDS 배포 구조 분리

상태: 해야됨

하다 보니 React 정적 파일과 API 트래픽을 같은 EC2 경로로 처리하면 프리티어 EC2가 정적 리소스까지 떠안고, TLS, CDN, WAF, DNS 책임도 한 서버에 몰리는 문제가 생깁니다.

EC2 단일 nginx, S3 정적 호스팅 + EC2 API, Cloudflare 앞단 분리 중에서 고민했고, 비용과 운영 책임 분리를 고려하면 `Cloudflare /* -> S3`, `Cloudflare /api/* -> EC2 public -> nginx Ingress -> k8s service -> RDS` 구조가 더 낫다고 판단했습니다.

현재 구조도와 문서 설계는 끝났고, 실제 배포는 비용과 도메인 설정 확인 후 진행해야 합니다.

근거:

- `docs/assets/highfivebooks-architecture.svg`
- `README.md`

해야됨:

- S3 정적 배포
- Cloudflare DNS, TLS, cache rule 설정
- `/api/*` origin routing 확인
- EC2 Ingress 접근 로그 캡처

## 한 줄 요약

현재 바로 강하게 말할 수 있는 항목은 도서 조회 성능 개선, 주문 생성 API 부하 테스트, RabbitMQ DLQ 격리, RDS 데이터 복제, Docker Compose 재현 환경이다. 생일 쿠폰 배치, 재고 멱등성, Elasticsearch 검색, K8s smoke, Cloudflare 배포는 코드와 계획은 있으나 최종 수치 또는 캡처가 더 필요하다.
