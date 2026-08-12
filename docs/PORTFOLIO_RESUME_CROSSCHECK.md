# 이력서·포트폴리오 숫자·기술명 전수 대조

대조일: 2026-08-12

## 숫자 대조

| 주장 | 기준값 | 근거 | 판정 | 표기 규칙 |
|---|---:|---|---|---|
| 도서 데이터 | 157,118건 | 성능 보고서, k6 결과, 이력서 초안 | 검증 완료 | `157,118건`으로 통일 |
| 도서 목록 p95 전 | 9,039.51ms | `book-read-baseline-20260708-234828.json` | 검증 완료 | 초 단위는 `약 9.04초` 병기 가능 |
| 도서 목록 p95 후 | 51.97ms | `book-read-optimized-20260708-235147.json` | 검증 완료 | 문서의 `51.96ms`를 `51.97ms`로 수정 |
| 개선 배수 | 173.94배 | 9,039.51 / 51.97 | 검증 완료 | 본문은 `약 174배` |
| 주문 생성 50 VU | 5,435건, 2분, p95 133.29ms, 실패율 0% | 진행 문서 표 | 부분 검증 | 원시 summary JSON 부재를 명시 |
| 주문 서버 테스트 | 132건, 실패 0, 오류 0, skip 1 | 2026-08-12 `mvnw test` | 검증 완료 | skip 1은 실브로커 DLQ 조건부 테스트 |
| 생일 배치 | 회원 2,400만/대상 200만/4시간+ | 분석 문서 | 미검증 | 원시 실행 로그 확보 전 이력서 수치 제외 |

## 기술명 대조

| 범주 | 표준 표기 | 확인 위치 | 판정/주의 |
|---|---|---|---|
| Runtime | Java 21, Spring Boot 3.5.7, Spring Cloud 2025.0.0 | `pom.xml` | 검증 완료 |
| ORM/API | Spring Data JPA, OpenFeign, Resilience4j | order/book code | 검증 완료 |
| Data | MySQL 8.4, Redis 7.2, Elasticsearch 8.18 + Nori, MinIO | Compose/README | 구현 확인; 버전은 환경별 차이 가능 |
| Messaging | RabbitMQ Retry/DLQ | config/tests | 부분 검증; 실브로커 자동 테스트 미실행 |
| Scheduler | ShedLock 7.7.0 + Redis provider | order `pom.xml`/tests | 검증 완료 |
| Platform | Docker Compose | Compose/실행 스크립트 | 검증 완료 |
| AWS | EC2, RDS | 기존 배포 구성/데이터 재현 문서 | 부분 검증; 현재 상시 운영 의미 아님 |
| CI/CD | GitHub Actions, CodeRabbit, SonarQube, health check | 기존 CI/CD 구성도 | 부분 검증; 저장소 내 실행 로그는 별도 확인 필요 |
| Test | JUnit 5, Mockito, Spring Boot Test, k6 | tests/perf | 검증 완료 |
| Testcontainers | README에만 표기 | 의존성 검색 결과 없음 | 삭제 필요 |
| Cloudflare/S3 | 설계 문서 | 실제 배포 근거 없음 | 미검증, 핵심 기술 목록에서 제외 |

## 수정 반영

- 성능 개선 후 p95를 모든 핵심 문서에서 `51.97ms`로 통일한다.
- `Testcontainers`는 실제 의존성이 없어 기술 목록에서 제거한다.
- 숫자가 없는 기능 나열보다 JPA p95, TCC 멱등성, DLQ처럼 근거가 있는 사례를 우선한다.
- 별도 이력서 파일이 작업공간에 없어 이번 대조는 `PORTFOLIO_RESUME_UPDATE_2026-07-08.md`를 이력서 원본으로 간주했다.
- `AGENTS.md`의 `98 tests` 기대값은 현재 132건과 달라 과거 기준으로 분류한다.
