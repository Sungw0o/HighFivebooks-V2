# HighFiveBooks 이력서 프로젝트 문단

갱신일: 2026-08-12

## 프로젝트 요약

도서 커머스 MSA의 주문 흐름을 중심으로 JPA 조회 병목, TCC 멱등성과 실패 보상, RabbitMQ DLQ를 검증하고 Docker Compose로 로컬 재현 환경을 정리했습니다.

## 이력서 bullet

- **검증 완료** — 도서 157,118건에서 JPA collection `EntityGraph`와 `Pageable` 조합의 메모리 pagination 병목을 재현하고, 조회별 fetch graph 분리로 목록 p95를 9,039.51ms에서 51.97ms로 개선했습니다(1회 동일 시나리오 기준 약 174배).
- **부분 검증** — 주문 Confirm/Cancel 중복과 포인트·결제·재고 보상 실패/timeout 테스트를 추가해 재고·포인트의 1회 반영과 현재 순차 보상의 중단 지점을 명시했습니다.
- **부분 검증** — RabbitMQ retry/DLQ 구성을 적용하고 poison message 격리 기록과 조건부 실브로커 통합 테스트를 마련했습니다.

## 문제 해결 서술

### JPA 목록 조회

- **문제**: 작은 페이지인데도 목록 p95가 9초를 넘었다.
- **선택**: 변경 폭이 작은 fetch graph 분리를 택했다.
- **해결**: 목록은 `publisher`, 상세는 `bookAuthors.author`를 로딩하도록 분리했다.
- **검증**: 저장된 k6 JSON에서 p95 9,039.51ms → 51.97ms를 확인했다.
- **한계**: 짧은 전후 1회 측정이므로 반복 실험 중앙값은 아니다.

### 주문 TCC와 DLQ

- **문제**: 중복 이벤트와 timeout이 이중 반영 또는 부분 보상을 만들 수 있었다.
- **선택**: 상태 gate·idempotency key·RabbitMQ DLQ로 재처리 경계를 나눴다.
- **해결**: 중복 Confirm/Cancel과 보상 실패 테스트, 실브로커 DLQ 통합 테스트를 추가했다.
- **검증**: order 37건과 stock 11건의 대상 테스트가 통과했다. DLQ 실브로커 테스트는 이번 세션에서 skip됐다.
- **한계**: 보상 단계 영속화와 durable retry가 없어 완전한 TCC coordinator는 아니다.

## 핵심 기술

Java 21, Spring Boot 3.5.7, Spring Data JPA, OpenFeign/Resilience4j, RabbitMQ, MySQL, Redis, Elasticsearch, Docker Compose, JUnit 5·Mockito·k6.

숫자와 기술명 대조 기준은 `PORTFOLIO_RESUME_CROSSCHECK.md`를 따른다.
