# HighFiveBooks Performance Test Progress - 2026-07-09

이 문서는 2026-07-09 기준 성능·부하 테스트 진행 상황을 로컬 레포에 남기기 위한 기록이다. 수치가 있는 항목은 측정 결과로 사용하고, 수치가 없는 항목은 실행 대기 상태로 분리한다.

## 1. 주문 생성 API 부하 테스트

### 대상

```text
POST /api/orders
서비스: order-server
스크립트: perf/k6/order-create-baseline.js
```

### 실행 예시

```powershell
docker run --rm -i `
  -e BASE_URL=http://host.docker.internal:9006 `
  -e BOOK_ID=1 `
  -e VUS=50 `
  -e DURATION=2m `
  -v "${PWD}\perf:/perf" `
  grafana/k6:0.53.0 run `
  --summary-export "/perf/results/order-create-50vu.json" `
  "/perf/k6/order-create-baseline.js"
```

### 확보된 결과

| 시나리오 | 요청 수 | 처리량 | 실패율 | 평균 | p95 | 최대 |
|---|---:|---:|---:|---:|---:|---:|
| 1 VU / 30초 | 27 | 0.89 req/s | 0.00% | 121.37ms | 152.33ms | 170.84ms |
| 5 VU / 1분 | 270 | 4.46 req/s | 0.00% | 118.79ms | 155.37ms | 202.86ms |
| 10 VU / 1분 | 541 | 8.85 req/s | 0.00% | 113.43ms | 154.50ms | 175.34ms |
| 20 VU / 2분 | 2160 | 17.96 req/s | 0.00% | 110.96ms | 144.19ms | 281.67ms |
| 50 VU / 2분 | 5435 | 44.89 req/s | 0.00% | 107.95ms | 133.29ms | 684.93ms |

### 해석

주문 생성 API는 Docker Compose 로컬 MSA 환경에서 50 VU, 2분 동안 실패율 0%와 p95 133.29ms를 기록했다. 현재 스크립트는 `sleep(1)`을 포함하므로 최대 한계 부하가 아니라 동시 사용자 안정성 테스트로 해석한다.

## 2. 결제 성공 RabbitMQ 처리 검증

### 대상

```text
Queue: payment-success-queue
DLQ: high-five-order-payment-dead-letter-queue
Scripts:
  scripts/publish-payment-success.ps1
  scripts/show-rabbit-queues.ps1
```

### 정상 메시지 실행 예시

```powershell
.\scripts\publish-payment-success.ps1 `
  -OrderId 1 `
  -TotalAmount 18000 `
  -PaymentKey "manual-success-1"

.\scripts\show-rabbit-queues.ps1
```

### Poison message 실행 예시

```powershell
.\scripts\publish-payment-success.ps1 `
  -OrderId 2 `
  -TotalAmount 1 `
  -PaymentKey "manual-poison-1"

.\scripts\show-rabbit-queues.ps1
```

### 확보된 결과

| 케이스 | 입력 | 기대 결과 | 확인 결과 |
|---|---|---|---|
| 정상 결제 성공 | `orderId=1`, `totalAmount=18000` | 주문 상태 `PAYMENT_WAITING` -> `PREPARING` | 정상 처리 확인 |
| 금액 불일치 | `orderId=2`, `totalAmount=1` | retry 후 DLQ 격리 | DLQ 1건 이동 확인 |

### 해석

정상 메시지는 주문 후처리 consumer가 처리하고, 금액 불일치 poison message는 retry 이후 DLQ로 격리된다. 따라서 결제 성공 이벤트 처리에서 장애 메시지가 무한 재소비되어 정상 주문 처리까지 막는 문제를 방어할 수 있다.

## 3. 주문 목록 DB 인덱스 테스트 준비

### 대상

```text
GET /api/admin/orders?page=0&size=20
GET /api/admin/orders?page=0&size=20&status=PAYMENT_WAITING
GET /api/orders?page=0&size=20 + X-USER-ID
스크립트: perf/k6/order-list-baseline.js
```

### 실행 예시

```powershell
docker run --rm -i `
  -e BASE_URL=http://host.docker.internal:9006 `
  -e VUS=20 `
  -e DURATION=1m `
  -e STATUS=PAYMENT_WAITING `
  -v "${PWD}\perf:/perf" `
  grafana/k6:0.53.0 run `
  --summary-export "/perf/results/order-list-payment-waiting.json" `
  "/perf/k6/order-list-baseline.js"
```

### 현재 확인값

```text
관리자 주문 전체 첫 페이지: 약 337ms
관리자 PAYMENT_WAITING 첫 페이지: 약 306ms
```

### 2026-07-09 Docker 재기동 후 smoke 결과

Docker Desktop 재기동 후 Compose 전체 서비스가 `healthy` 상태로 올라온 것을 확인하고, 주문 목록 API에 대해 1 VU / 5초 smoke 테스트를 실행했다.

```text
결과 파일: perf/results/order-list-smoke-20260709-220928.json
요청 수: 10
checks: 100%
실패율: 0.00%
평균 응답 시간: 11.01ms
p95: 15.28ms
```

이 결과는 인덱스 개선 수치가 아니라, Docker Compose 로컬 MSA와 k6 하네스가 정상 연결되는지 확인한 smoke evidence로 사용한다.

### 남은 작업

- 회원 주문 seed를 추가해 `GET /api/orders + X-USER-ID` 인덱스 효과를 확인한다.
- `order_date`, `delivery_status`, `user_id` 조건의 `EXPLAIN` 전후를 비교한다.
- k6 결과 파일을 `perf/results`에 남긴다.

## 4. 도서 조회 성능 테스트 연결

도서 조회 성능 테스트의 상세 결과는 `docs/PERFORMANCE_REPORT.md`에 기록했다.

```text
도서 데이터: 157,118건
목록 조회 p95: 9039.51ms -> 51.97ms
개선 폭: 약 173.94배
```

## 5. 생일 쿠폰 배치 튜닝 연결

생일 쿠폰 배치 튜닝의 상세 분석은 `docs/DB_BATCH_TUNING.md`에 기록했다.

현재 반영한 개선:

- member-server 생일자 조회 API에 `page`, `size` 반영
- coupon-server Reader의 chunk 단위 조회 의도가 실제 pagination으로 이어지도록 수정
- coupon-server Writer의 JDBC batch insert 경로 확인

남은 핵심 측정:

- `MONTH(birth_date)` 조회의 `EXPLAIN`
- `birth_month` generated/direct column 적용 후 `EXPLAIN`
- chunk size별 처리량과 전체 배치 시간

## 6. 다음 우선순위

1. RabbitMQ payment-success 1000건 처리량 테스트
2. 회원 주문 seed 생성 후 주문 목록 DB 인덱스 + EXPLAIN 전후 비교
3. 회원/쿠폰/포인트 포함 주문 생성 시나리오 확장
4. book/coupon/member 서버 장애 주입 후 Feign timeout / CircuitBreaker 검증
5. K8s smoke 재실행 및 Ingress 기준 주문 생성 부하 테스트

## 포트폴리오 문장

HighFiveBooks V2에서는 Docker Compose 기반 로컬 MSA 환경에서 주문 생성 API에 k6 부하 테스트를 적용해 동시 사용자 50명, 2분 동안 5435건의 주문 요청을 처리하면서 실패율 0%, p95 133.29ms를 확인했습니다. 또한 RabbitMQ 결제 성공 메시지 흐름에서 정상 메시지는 주문 상태를 `PREPARING`으로 전이시키고, 금액 불일치 poison message는 DLQ로 격리되는 것을 검증해 주문·결제 이벤트 정합성과 장애 격리 근거를 확보했습니다.
