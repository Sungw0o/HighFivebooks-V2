# 주문 도메인 Cache·Redis 개선 후보 발굴 지시서

## 새 세션에 전달할 요청

HighFiveBooks V2의 `order-server`를 전체 점검해서 **Caffeine 로컬 캐시 또는 Redis를 적용해
실제로 개선할 가치가 있는 후보를 다시 찾아줘.**

현재 적용한 회원 등급 Caffeine 사례만 재검토하는 작업이 아니다. 주문 생성·조회·결제 후처리·취소·
스케줄러까지 전부 읽고, 반복 조회·외부 네트워크 왕복·중복 처리·다중 인스턴스 정합성 문제를 찾아라.

기술을 먼저 정하지 말고 다음 순서로 진행한다.

1. 병목 또는 정합성 문제가 있는 코드 경로 발견
2. 현재 호출 횟수와 지연시간 측정
3. 로컬 캐시·Redis·DB 원자 연산·이벤트 처리 중 적절한 수단 비교
4. 기대 효과와 stale·무효화·장애 위험 제시
5. 가치가 가장 큰 후보만 구현 계획으로 제안

## 먼저 읽을 파일

- `AGENTS.md`
- `docs/order-flow-boundary-map.md`
- `docs/MEMBER_GRADE_CACHE_PERFORMANCE.md`
- `docs/ORDER_CREATE_SEGMENT_PERFORMANCE.md`
- `services/order-server/src/main/java`
- `services/order-server/src/main/resources`
- order-server가 호출하는 book/member/coupon/payment Feign endpoint와 실제 구현
- `docker-compose.yml`
- `perf/k6`

작업 트리가 이미 수정돼 있으므로 먼저 `git status --short`와 관련 diff를 확인하고 기존 변경을
되돌리거나 덮어쓰지 않는다.

## 전체 점검 범위

### 1. 반복 조회와 외부 호출

다음을 `rg`로 모두 찾아 호출 위치·빈도·데이터 변경 주기를 표로 정리한다.

- `@Cacheable`, `@CacheEvict`, `CacheManager`
- 모든 OpenFeign client 호출
- 주문 한 건에서 반복되는 repository 조회
- 주문 목록·상세에서 발생하는 N+1 또는 같은 데이터 재조회
- 관리자 정책·포장지·배송 정책·회원 등급처럼 변경이 드문 데이터
- 주문 상태 polling처럼 같은 키를 짧은 시간 반복 조회하는 API

각 후보마다 아래를 기록한다.

| 항목 | 내용 |
|---|---|
| 호출 경로 | 어떤 API에서 몇 번 호출되는지 |
| 데이터 소유자 | order 또는 외부 서비스 |
| 변경 빈도 | 요청마다·수분·일·월 단위 |
| stale 허용 | 허용 시간과 영향 |
| 무효화 주체 | 어느 서비스가 변경을 아는지 |
| 장애 영향 | 캐시 장애·원본 장애 시 동작 |
| 추천 수단 | Caffeine·Redis·캐시 금지·다른 방법 |

### 2. Caffeine 후보

다음 조건을 만족하는 데이터를 우선 찾는다.

- 같은 order-server 인스턴스에서 반복 사용
- 변경이 드물고 짧은 stale을 허용
- 캐시 목적이 외부 네트워크 왕복 자체 제거
- 인스턴스마다 잠시 다른 값을 봐도 금액·재고·상태 정합성이 깨지지 않음
- 키 종류와 최대 크기를 제한할 수 있음

후보 예시를 그대로 채택하지 말고 코드로 검증한다.

- 포장 정책 또는 포장지 메타데이터
- 회원 등급 외의 읽기 전용 회원 메타데이터
- 짧은 시간 반복 조회되는 주문 표시용 정적 정보

가격·재고·쿠폰 사용 여부·결제 상태처럼 즉시 정합성이 필요한 값은 로컬 캐시에서 우선 제외한다.

### 3. Redis 후보

Redis는 단순히 빠른 저장소로 쓰지 말고 다음 문제가 있을 때만 후보로 둔다.

- 여러 order-server 인스턴스가 같은 값을 봐야 함
- 중복 요청·중복 메시지를 인스턴스 간 차단해야 함
- 짧은 TTL의 idempotency key 또는 상태 공유가 필요함
- DB까지 갈 필요 없는 임시 상태·rate limit·dedup 상태
- 원자적 비교·증가·교체가 필요해 Lua 또는 Redis 원자 명령이 적합함

반드시 현재 코드에 이미 존재하는 다음 항목과 중복되지 않는지 확인한다.

- 배송 정책 Redis 캐시와 `@CacheEvict`
- Redis 기반 ShedLock
- 결제 메시지 멱등성 또는 주문 상태 gate
- 서비스별 idempotency key

Redis 적용이 DB를 source of truth로 유지하는지, Redis 장애 시 주문을 막을지 우회할지도 명시한다.

### 4. 캐시를 쓰면 안 되는 후보

아래 값은 성능만 보고 캐시하지 않는다.

- 실시간 재고와 재고 선점 결과
- 도서 가격 또는 주문 금액 계산의 기준값
- 쿠폰 사용·취소 상태
- 포인트 잔액과 예약 상태
- 결제·주문 상태

캐시하려면 변경 이벤트, 버전, 짧은 TTL, 최종 DB 검증 등 정합성 장치를 먼저 제시한다.

## 측정 방법

후보를 찾은 뒤 구현 전에 baseline부터 확보한다.

- 전체 API p50/p95/p99
- 후보 구간 자체의 p50/p95/p99
- 원본 repository 또는 Feign 호출 수
- cache hit/miss와 hit ratio
- cold 동시 miss 시 원본 호출 수
- Redis 명령 수와 왕복시간
- HTTP 실패율과 결과 정합성
- 가능하면 조건별 10회 이상 반복하고 중앙값·범위를 기록

전체 응답시간만 보고 캐시 효과로 결론 내리지 않는다. Micrometer Timer 또는 기존
`OrderCreateMetrics`를 확장해 대상 구간을 분리한다.

## Docker 실행

```powershell
cd C:\Users\SSAFY\Desktop\HighFivebooks-V2
git status --short
docker compose --profile apps ps
```

필요하면 전체 환경을 기동한다.

```powershell
docker compose --profile apps up -d
```

기존 주문 프로파일링 override:

```powershell
docker compose --profile apps `
  -f docker-compose.yml `
  -f perf/docker-compose.order-profile.yml `
  up -d --no-deps --force-recreate order-server
```

기본 검증:

```powershell
cd services\order-server
.\mvnw.cmd test
```

## 후보 평가 기준

각 후보를 아래 기준으로 5점 만점 평가한다.

| 기준 | 질문 |
|---|---|
| 효과 | 실제 호출 수나 p95가 충분히 줄어드는가? |
| 정합성 | stale·중복·유실 위험을 통제할 수 있는가? |
| 단순성 | 운영 구성과 코드 복잡도가 효과에 비해 적절한가? |
| 재현성 | before/after를 같은 조건으로 반복 측정할 수 있는가? |
| 포트폴리오 가치 | 기술 선택 이유와 트레이드오프를 설명할 수 있는가? |

합계가 높아도 정합성 위험이 큰 후보는 제외한다.

## 최종 보고 형식

구현 전에 다음 형식으로 먼저 보고한다.

### 후보 전체 표

| 순위 | 코드 경로 | 문제 | 추천 수단 | 기대 효과 | 정합성 위험 | 측정 방법 |
|---:|---|---|---|---|---|---|

### 최종 추천

- **Caffeine 1순위 후보**: 선택 이유, 무효화·TTL 정책
- **Redis 1순위 후보**: 선택 이유, DB와의 책임 경계
- **적용하지 않을 후보**: 캐시가 부적절한 이유
- **가장 먼저 측정할 API**: 요청 조건과 k6 시나리오

### 구현 계획

- 변경 파일
- 추가 테스트
- baseline과 after 측정 순서
- 실패 시 롤백 기준

후보를 억지로 만들지 않는다. 코드와 측정상 가치 있는 후보가 없으면 “추가 캐시보다 트랜잭션·쿼리·
외부 호출 구조 개선이 우선”이라고 결론 내려도 된다. 사용자의 확인을 받기 전에는 새 캐시나 Redis
구조를 구현하지 말고, Notion 이력서·포트폴리오도 수정하지 않는다.
