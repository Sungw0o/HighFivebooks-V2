# 회원 등급 적립률 캐시 성능 측정

## 결론

주문 생성마다 발생하던 member-server 등급 조회를 Caffeine 로컬 캐시로 전환했다.
동일한 회원 주문 부하에서 warm p95 중앙값은 캐시 없음 366.39ms, Redis 290.01ms,
Caffeine 253.44ms였다. Caffeine은 캐시 없음보다 30.8%, Redis보다 12.6% 낮았다.

성능 수치보다 중요한 선택 근거는 캐시 범위다. 배송 정책은 모든 order-server 인스턴스가
동일한 값을 공유해야 하므로 기존 Redis 캐시를 유지하고, 회원 적립률은 짧은 stale을
허용하면서 Feign 네트워크 홉을 제거하는 것이 목적이므로 Caffeine을 사용했다.

## 측정 전에 발견한 계약 오류

order-server의 `MemberClient`는 `GET /api/members/{userId}/grade`를 호출했지만
member-server에 해당 API가 없었다. 예외가 주문 서비스의 `0.0` 폴백에 가려져 회원 주문은
성공하면서도 적립 예정 포인트가 항상 0점으로 저장됐다.

API 계약을 복구한 뒤 GENERAL 등급 회원의 15,000원 주문에서 적립률 1%와 적립 예정 포인트
150점이 저장되는 것을 확인했다.

## 환경과 시나리오

- 날짜: 2026-08-13
- 환경: Windows Docker Desktop, 단일 order-server 인스턴스
- 부하 도구: k6 0.53.0
- 대상: `POST /api/orders`
- 데이터: 동일 회원 ID 1, 동일 도서 ID 1
- 동시성: 32 VU
- warm 측정: 워밍업 1건 후 320건, 모드별 3회
- cold 동시성 측정: order-server 재시작 후 워밍업 없이 32건
- 성공 조건: HTTP 201 및 응답의 `orderId` 존재
- member-server 호출 수: Actuator `http.server.requests`의 등급 API COUNT 증분

실행 스크립트는 `perf/k6/member-grade-cache-benchmark.js`다. 주문 전체 API를 측정하므로
수치에는 도서 조회, 재고 선점, 배송비 계산, 주문·배송 DB 저장, 장바구니 삭제 비용도 포함된다.

## Warm 결과

| 방식 | 1차 p95 | 2차 p95 | 3차 p95 | p95 중앙값 | 성공률 | 등급 API 호출 |
|---|---:|---:|---:|---:|---:|---:|
| 캐시 없음 | 526.85ms | 242.15ms | 366.39ms | **366.39ms** | 100% | 요청마다 1회 |
| Redis | 476.78ms | 254.11ms | 290.01ms | **290.01ms** | 100% | 0회 |
| Caffeine | 303.28ms | 208.09ms | 253.44ms | **253.44ms** | 100% | 0회 |

단일 로컬 환경의 짧은 반복 측정이라 실행 간 편차가 크다. 따라서 최저값이나 단일 실행값이
아닌 세 번의 p95 중앙값을 사용했다.

## Cold 동시 요청과 stampede

| 방식 | 동시 주문 | member-server 등급 API 호출 |
|---|---:|---:|
| 캐시 없음 | 32건 | 32회 |
| Redis | 32건 | 32회 |
| Caffeine (`sync=true`) | 32건 | **1회** |

Caffeine은 같은 JVM 안에서 동일 키의 concurrent miss를 하나의 loader 호출로 합쳤다.
Redis 방식은 분산 저장소에 값을 공유하지만 이 구성만으로는 cold miss의 loader 실행을
인스턴스·스레드 사이에서 합치지 못했다. Redis의 cold p95가 높게 나온 원인이기도 하다.

cold 지연시간은 모드별 JVM 재시작 직후 한 번만 측정해 JIT와 커넥션 풀 워밍업의 영향을
분리할 수 없으므로 개선 수치로 사용하지 않는다. 호출 수 결과만 stampede 검증 근거로 쓴다.

## 구현 정책

- 기본 전략: Caffeine
- TTL: 5분
- 최대 엔트리: 10,000
- 동일 키 동시 로딩: `@Cacheable(sync = true)`
- 캐시 키: 회원 ID
- null과 장애 폴백 값: 캐시하지 않음
- 장애 처리: 캐시 loader는 예외를 전달하고 주문 계층에서 기존 0% 폴백 적용
- 실험 전환: `MEMBER_GRADE_CACHE_TYPE=none|redis|caffeine`

회원 등급은 매월 1일 스케줄러로 재산정되므로 최대 5분의 stale 적립률을 허용했다. 다중
order-server에서는 각 인스턴스가 별도 로컬 캐시를 가지므로 최초 요청은 인스턴스마다 한 번씩
member-server를 호출한다.

## 원시 결과

`perf/results/member-grade-{none|redis|caffeine}-{cold|warm...}.json`에 k6 summary를 저장했다.
모든 측정에서 주문 실패율은 0%였다.
