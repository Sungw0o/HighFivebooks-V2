# HighFiveBooks Checklist Gap Audit

작성일: 2026-07-09

이 문서는 취업 체크리스트 관점에서 HighFiveBooks V2 코드와 문서를 실제로 대조한 결과다. 핵심은 이미 적용된 항목을 놓치지 않고, 아직 포트폴리오 근거가 약한 항목을 다음 작업으로 연결하는 것이다.

## 결론

붙여넣은 외부 평가에서 HighFiveBooks는 로컬 코드 없이 Notion 문서 기준으로만 판단되어 일부 항목이 과소평가됐다. 실제 코드 기준으로는 Redis 캐시, Redis Lua 원자 처리, Feign timeout/CircuitBreaker, RabbitMQ Retry/DLQ, k6 스크립트, Docker Compose MSA smoke는 이미 존재한다.

다만 체크리스트가 강조하는 "성능 기본기 쇼케이스" 관점에서는 `EXPLAIN` 캡처, Hikari connection pool 수치화, Tomcat thread 수치화, JVM 옵션, tcpdump 장애 분석 근거가 아직 약하다.

## 1. 적당한 기본기

| 항목 | 실제 상태 | 판단 | 근거 |
|---|---|---|---|
| MySQL EXPLAIN 쿼리 개선 | 도서 목록은 k6와 JPA fetch graph 병목 개선 완료. 생일 쿠폰 배치는 EXPLAIN 계획 문서화, 전후 캡처는 아직 없음 | 보강 필요 | `docs/PERFORMANCE_REPORT.md`, `docs/DB_BATCH_TUNING.md` |
| 로컬 캐시 성능 개선 | 로컬 Caffeine은 없음. 대신 Redis CacheManager와 `@Cacheable` 적용 | 일부 완료 | `bookDetail`, `newBooks`, `bookReviews`, `activeDeliveryPolicy` |
| Redis 성능 개선 및 데이터 정합성 | 쿠폰 선착순 발급 Lua, 장바구니 Lua, 조회수/랭킹 ZSet, Redis 기반 ShedLock 존재 | 완료에 가까움 | coupon/member/book/order 서버 Redis 코드 |
| 비동기/동기 외부 호출 개선 | order-server Feign timeout, CircuitBreaker, NEVER_RETRY, 트랜잭션 경계 분리 완료 | 완료 | `docs/order-resilience-evidence.md` |
| 프로젝트 근거 뒷받침 | README, 성능 리포트, 배치 튜닝, 포트폴리오 사례 카드, Notion 페이지 존재 | 완료 | `README.md`, `docs/PORTFOLIO_PERFORMANCE_CASES.md` |

## 2. 조금 더 역량 있는 기본기

| 항목 | 실제 상태 | 판단 | 다음 작업 |
|---|---|---|---|
| tcpdump 패킷 분석 장애 해결 | 없음 | 해야됨 | RabbitMQ 또는 MySQL 연결 실패 상황을 하나 잡아 `tcpdump`/`docker exec` 기반 증거 작성 |
| connection pool properties 설정 | 명시적 Hikari 수치가 거의 없음 | 해야됨 | local/prod에 `maximum-pool-size`, `minimum-idle`, `connection-timeout`, `max-lifetime` 추가 |
| k6 성능 테스트 | 도서 조회 개선 수치, 주문 목록 smoke, 주문 생성 하네스 존재 | 일부 완료 | 주문 생성 50 VU JSON 저장, RabbitMQ 1000건 처리량 저장 |
| thread 수치 / Tomcat vs Netty | 서버는 Spring MVC/Tomcat. thread 수치 명시는 없음. Netty 전환은 필수 아님 | 보강 필요 | Tomcat thread/connection 설정 수치화. Netty 전환은 하지 않는 이유 문서화 |

## 3. AI 트렌드

| 항목 | 실제 상태 | 판단 | 다음 작업 |
|---|---|---|---|
| Claude token/context 절약 | `AGENTS.md` 하네스가 있으나 token/context 절약 규칙은 문서화 부족 | 보강 필요 | Claude 지침에 "먼저 docs/contract 읽기, 전체 파일 덤프 금지, rg 우선" 추가 |
| MCP 서버 프로젝트 적용 | 프로젝트 런타임 코드에 MCP 적용 없음 | 선택 | 포트폴리오 앱 자체에 MCP를 넣을 필요는 낮음. 개발 워크플로 MCP 활용으로 설명 가능 |
| skills 나만의 하네스 | `AGENTS.md`는 있음. Claude Skills 형식은 아님 | 일부 완료 | `docs/AI_AGENT_HARNESS.md`로 역할/권한/커밋 규칙 정리 |
| hooks 권한 가드 | 없음 | 해야됨 | Claude hooks 또는 git pre-commit으로 `.env`, pem, secret, node_modules 방지 |
| 전방위 테스트 자동화 | order 테스트, Feign 경계, RabbitMQ DLQ, member 타깃 테스트, k6 하네스 존재 | 일부 완료 | GitHub Actions에서 백엔드 단위 테스트 + k6 smoke 분리 |

## 4. 기본기지만 강점이 될 수 있는 항목

| 항목 | 실제 상태 | 판단 | 다음 작업 |
|---|---|---|---|
| thread / Netty | Tomcat 기반. Netty 전환 근거 없음 | 보강 필요 | Tomcat thread 수치화로 충분. WebFlux/Netty 전환은 오버엔지니어링 가능 |
| JVM GC / Ratio 튜닝 | Dockerfile은 기본 `java -jar`만 사용. `MaxRAMPercentage`, GC 옵션 없음 | 해야됨 | `JAVA_TOOL_OPTIONS`로 container-aware JVM 옵션 추가 |
| tcpdump | 없음 | 해야됨 | RabbitMQ 연결 또는 MySQL handshake 장애 분석 예시 하나 만들기 |

## 이미 강점으로 써야 하는 것

### 도서 조회 성능 개선

157,118건 도서 데이터에서 JPA collection `EntityGraph`와 `Pageable` 조합으로 목록 조회 p95가 9039.51ms까지 느려졌고, 목록 조회 fetch graph를 `publisher` 중심으로 축소해 p95를 51.97ms로 낮췄다.

### Redis Cache

book-server는 Redis CacheManager를 사용하고, `bookDetail`, `newBooks`, `bookReviews`에 TTL을 다르게 둔다. order-server도 배송 정책 조회에 `@Cacheable`을 적용한다. 따라서 "캐시 없음"이 아니라 "캐시 성능 before/after 수치가 부족함"이 정확하다.

### Redis Lua 정합성

coupon-server는 선착순 쿠폰 발급에서 Redis Lua script로 잔여 수량과 사용자 중복 발급을 원자적으로 처리한다. member-server는 장바구니 upsert/merge에 Lua를 사용한다. 따라서 "Redis 정합성 약함"이 아니라 "동시성 부하 결과 저장이 필요함"이 정확하다.

### 외부 호출 장애 격리

order-server는 Feign timeout, CircuitBreaker, NEVER_RETRY, RabbitMQ Retry/DLQ, 트랜잭션 경계 분리를 이미 갖고 있다. 이 항목은 포트폴리오 핵심 강점으로 유지한다.

## 다음에 바로 하면 좋은 작업 순서

1. 주문 생성 50 VU k6 결과 JSON 저장
2. RabbitMQ 정상/poison message 실행 결과 JSON 또는 로그 캡처 저장
3. Hikari connection pool과 Tomcat thread 설정을 local/prod에 명시
4. `JAVA_TOOL_OPTIONS` 기반 JVM container 옵션 추가
5. 생일 쿠폰 `birth_month` 인덱스 전후 EXPLAIN 캡처
6. 선착순 쿠폰 Lua 동시 발급 k6 시나리오 작성
7. `.env`, `.pem`, `node_modules`, `dist` 방지 hook 추가
8. tcpdump는 마지막에 하나만, RabbitMQ/MySQL 연결 장애 분석 예시로 선택

## 면접용 정리

HighFiveBooks는 체크리스트의 모든 항목을 억지로 넣는 프로젝트가 아니라, 주문·결제·쿠폰·재고가 얽힌 MSA에서 실제로 의미 있는 운영 문제를 골라 개선한 프로젝트다. 이미 강한 축은 도서 조회 성능 개선, Redis Lua 정합성, RabbitMQ DLQ, Feign 장애 격리, Docker/K8s 전환이다. 남은 보강은 Hikari/Tomcat/JVM/tcpdump처럼 운영 파라미터를 수치로 설명하는 쪽이다.
