# 주문 생성 구간별 성능 측정

## 결론

`POST /api/orders`의 총 응답시간만 비교하지 않고 주문 생성 경로를 Micrometer Timer로 분해했다.
32 VU에서 가장 먼저 드러난 병목은 회원 등급 조회가 아니라 배송비 계산에 붙어 있던 불필요한
읽기 전용 트랜잭션이었다. 이를 제거하고, 실패해도 주문 생성에 영향을 주지 않는 장바구니 정리를
주문 DB 트랜잭션 커밋 이후로 이동했다.

warm Caffeine 조건의 주문 생성 p95 중앙값은 253.44ms에서 214.12ms로 39.32ms(15.5%) 감소했다.
전후 모든 실행은 주문 생성 성공률 100%, 실패 0건이었다.

## 측정 조건

- 날짜: 2026-08-13
- 환경: Windows Docker Desktop, 단일 order-server 인스턴스
- 대상: `POST /api/orders`
- 부하: 32 VU, 실행당 320건, 사전 warm-up 1건
- 데이터: 회원 ID 1, 도서 ID 1, 쿠폰·포장·포인트 사용 없음
- 회원 등급 캐시: Caffeine warm hit
- DB 커넥션 풀: Apache Commons DBCP2, 최대 10개
- 성공 조건: HTTP 201이며 응답에 `orderId` 존재
- 구간 p95: Prometheus histogram에서 p95가 포함된 최소 상한 버킷

## 최초 구간 분해

계측 직후 321건(사전 warm-up 1건 포함)의 결과다.

| 구간 | 평균 | p95 | 설명 |
|---|---:|---:|---|
| 전체 | 160.35ms | 246.07ms | order-server 내부 전체 |
| 배송비 계산 | 69.50ms | **156.59ms** | 배송 정책 Redis cache hit 포함 |
| 본 트랜잭션 | 41.73ms | 67.11ms | 주문·배송 저장, 장바구니 Feign 포함 |
| 재고 선점 | 33.14ms | 50.33ms | book-server Feign |
| 도서 정보 | 14.72ms | 27.96ms | book-server Feign |
| 장바구니 정리 | 11.65ms | 22.37ms | member-server Feign, 당시 트랜잭션 내부 |
| 주문 저장 | 8.21ms | 16.78ms | order DB |
| 배송 저장 | 1.12ms | 3.84ms | order DB |
| 회원 등급 | 0.71ms | 1.00ms | Caffeine warm hit |

`DeliveryServiceImpl`은 DB 작업이 없는 계산 서비스인데 클래스에 `@Transactional(readOnly = true)`가
붙어 있었다. 따라서 Redis cache hit 경로에서도 바깥 트랜잭션 프록시가 DBCP2 커넥션을 획득했다.
32개 요청이 최대 10개 커넥션을 경쟁하면서 배송비 계산 구간에 대기가 포함됐다.

## 변경

1. DB 작업이 없는 `DeliveryServiceImpl`에서 읽기 전용 트랜잭션을 제거했다.
2. `tryClearCart`를 `createOrderInTransaction`에서 제거했다.
3. 트랜잭션 프록시가 정상 반환해 커밋이 끝난 뒤 `clearCartAfterCommit`을 호출하도록 이동했다.
4. 장바구니 정리 실패를 경고 로그만 남기고 주문은 유지하는 기존 계약은 보존했다.

장바구니 호출은 비동기로 바꾸지 않았다. 이번 변경의 목적은 API 응답에서 호출 자체를 숨기는 것이
아니라 외부 네트워크 I/O 동안 DB 커넥션을 점유하지 않도록 트랜잭션 경계를 바로잡는 것이다.

## 최종 결과

### API p95 반복 측정

| 상태 | 1차 | 2차 | 3차 | 중앙값 |
|---|---:|---:|---:|---:|
| 변경 전 | 303.28ms | 208.09ms | 253.44ms | **253.44ms** |
| 변경 후 | 219.73ms | 214.12ms | 202.60ms | **214.12ms** |

단일 로컬 Docker 환경의 실행 간 편차가 있어 한 번의 최솟값이 아니라 3회 p95 중앙값을 비교했다.

### 변경 후 누적 구간

최종 세 실행 963건의 누적 histogram 결과다.

| 구간 | 평균 | p95 |
|---|---:|---:|
| 전체 | 132.18ms | 201.33ms |
| 본 트랜잭션 | 59.10ms | 111.85ms |
| 재고 선점 | 38.04ms | 67.11ms |
| 도서 정보 | 15.67ms | 33.55ms |
| 장바구니 정리(커밋 후) | 12.61ms | 27.96ms |
| 주문 저장 | 6.63ms | 22.37ms |
| 배송비 계산 | 5.82ms | **16.78ms** |
| 배송 저장 | 0.70ms | 2.45ms |
| 회원 등급 | 0.41ms | 1.00ms |

## 해석과 다음 우선순위

- 회원 등급 Caffeine은 warm hit에서 p95 약 1ms이므로 현재 임계 경로가 아니다.
- 배송 정책은 모든 인스턴스가 같은 값을 봐야 하므로 Redis 캐시를 유지했다. 문제는 Redis 자체가
  아니라 캐시 조회를 감싼 불필요한 DB 트랜잭션이었다.
- 장바구니 Feign을 커밋 이후로 옮겨 외부 I/O 중 DB 커넥션 점유를 제거했다.
- 남은 큰 외부 구간은 재고 선점과 도서 정보 조회다. 둘을 병렬화하면 평균상 절약 가능한 상한은
  더 짧은 도서 조회 약 16ms 수준인 반면, 도서 검증 실패 시 이미 선점된 재고를 해제해야 해 보상
  범위가 넓어진다. 현재 측정만으로는 복잡도 대비 우선순위가 낮다.

## 재현

```powershell
cd services/order-server
.\mvnw.cmd test
.\mvnw.cmd package -DskipTests

cd ../..
docker compose --profile apps -f docker-compose.yml -f perf/docker-compose.order-profile.yml build order-server
docker compose --profile apps -f docker-compose.yml -f perf/docker-compose.order-profile.yml up -d --no-deps --force-recreate order-server

docker run --rm `
  -v "${PWD}/perf/k6:/scripts" `
  -v "${PWD}/perf/results:/results" `
  -e BASE_URL=http://host.docker.internal:9006 `
  -e VUS=32 -e ITERATIONS=320 -e WARM_CACHE=true `
  grafana/k6 run `
  --summary-export=/results/order-create-cart-after-commit.json `
  /scripts/member-grade-cache-benchmark.js
```

원본 k6 summary는 `perf/results/order-create-cart-after-commit*.json`에 저장했다.
