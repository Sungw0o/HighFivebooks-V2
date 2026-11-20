# HighFiveBooks V2

NHN Academy 팀 프로젝트(FE 2, BE 5) HighFiveBooks를 팀 프로젝트가 끝난 뒤 개인 저장소에서 다시 정리한 버전입니다. 팀 원본은 [nhnacademy-be12-high-five](https://github.com/orgs/nhnacademy-be12-high-five)에 있습니다.

> 주문 정합성, 메시지 장애 격리, 대용량 조회 성능을 개선한 온라인 서점 MSA입니다.

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.7-6DB33F?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-MSA-6DB33F?logo=spring&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Retry%2FDLQ-FF6600?logo=rabbitmq&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.2-DC382D?logo=redis&logoColor=white)

## 한눈에 보기

| 항목 | 내용 |
| --- | --- |
| 서비스 | 도서 검색부터 주문·결제·배송까지 처리하는 온라인 서점 MSA입니다. |
| 기간 | 팀 프로젝트 2025.11–2026.01, V2 개인 정리 2026.07–08 |
| 인원 | 팀 7명(FE 2, BE 5), V2는 1명 |
| 내 역할 | 팀에서는 백엔드 주문 도메인과 배포 인프라를 맡았고, V2에서는 주문 실패 경로를 보강했습니다. |
| 기술 스택 | Java 21, Spring Boot 3.5.7, Spring Cloud(Eureka·Config·OpenFeign), MySQL, Redis, RabbitMQ, Elasticsearch, Docker Compose, GitHub Actions |

## 내가 맡은 일

**팀 프로젝트 (2025.11–2026.01)**

- 주문 엔티티와 API를 설계하고, 재고·포인트·쿠폰을 Feign으로 묶는 주문 생성을 구현했습니다. ([order_server 커밋](https://github.com/nhnacademy-be12-high-five/order_server/commits?author=Sungw0o))
- 주문 조회, 취소·반품, 관리자 주문 관리와 RabbitMQ 결제 성공 후처리를 구현했습니다.
- [Eureka Server](https://github.com/nhnacademy-be12-high-five/eureka_server)·[Config Server](https://github.com/nhnacademy-be12-high-five/config_server)와 서비스 공용 GitHub Actions 배포 워크플로우를 만들었습니다.

**V2 개인 정리 (2026.07–08)**

- 주문 실패 경로를 보강했습니다. 트랜잭션 경계 분리, 상태 변경 Feign 재시도 해제, Retry/DLQ, ShedLock을 적용했습니다. ([84a398a](https://github.com/Sungw0o/HighFivebooks-V2/commit/84a398a38878ec44b5644e0273e5f707076cdc95), [b4aebb7](https://github.com/Sungw0o/HighFivebooks-V2/commit/b4aebb7f97ac9ef9b4dd7b12ea305e04fc1ef81f), [fa1a794](https://github.com/Sungw0o/HighFivebooks-V2/commit/fa1a79430a12b70aa7204adc72daccbf94f3db1d), [ab8fd14](https://github.com/Sungw0o/HighFivebooks-V2/commit/ab8fd145f18187054341c170314fe35fb5810f9e))
- Feign 계약 테스트와 중복·실패 경로 테스트를 추가했습니다. ([c160a01](https://github.com/Sungw0o/HighFivebooks-V2/commit/c160a019b363a493ed08a8e8390cb2348c24c89b), [실패 지점 표](docs/TCC_FAILURE_MATRIX.md))

## 문제를 고친 기록

### 1. 결제 성공 메시지의 반복 실패와 중복 확정

Feign 재시도와 RabbitMQ 재처리가 겹쳐 같은 확정이 반복될 수 있던 문제입니다. 내용은 [Retry/DLQ 격리](#반복-실패-메시지-retrydlq-격리)와 [TCC 멱등성](#tcc-기반-주문-정합성과-멱등성)에, 처리 순서는 [핵심 흐름](#핵심-흐름-결제-성공-메시지-처리)에 정리했습니다.

### 2. 서버 두 대에서 주문 스케줄러 중복 실행

- **문제**: 미결제 주문 취소와 구매 확정 일괄 처리가 서버마다 따로 돌 수 있었습니다.
- **원인**: 배포 설정이 주문 서버를 두 대로 띄웠고([maven.yml#L40](services/order-server/.github/workflows/maven.yml#L40)), 스케줄러에는 `@Scheduled`만 있었습니다. 같은 주문에 보상 호출과 포인트 적립 메시지가 두 번 나갈 수 있었습니다.
- **해결**: 주문 서버가 이미 쓰던 Redis로 ShedLock을 적용했습니다. ([SchedulerLockConfig](services/order-server/src/main/java/com/nhnacademy/order_server/config/SchedulerLockConfig.java), [OrderCancelScheduler](services/order-server/src/main/java/com/nhnacademy/order_server/scheduler/OrderCancelScheduler.java))
- **결과**: 락 설정을 테스트 2개로 검증했고, 선택 근거를 [문서](docs/order-resilience-evidence.md)에 남겼습니다. ([84a398a](https://github.com/Sungw0o/HighFivebooks-V2/commit/84a398a38878ec44b5644e0273e5f707076cdc95))

### 3. 외부 호출이 DB 트랜잭션 안에서 실행되던 구조

- **문제**: 외부 서비스가 느려지면 주문 DB 트랜잭션도 그만큼 길어졌습니다.
- **원인**: `OrderServiceImpl` 전체에 `@Transactional`이 걸려 있었습니다. 결제 성공 처리에서 쿠폰·재고·포인트 Feign 호출이 같은 트랜잭션 안에 있었습니다.
- **해결**: 외부 호출은 트랜잭션 밖에서 하고, 상태 변경만 [OrderStatusMutationService](services/order-server/src/main/java/com/nhnacademy/order_server/service/impl/OrderStatusMutationService.java)의 짧은 트랜잭션으로 옮겼습니다.
- **결과**: 조율 메서드에 트랜잭션을 다시 씌우면 [OrderTransactionBoundaryTest](services/order-server/src/test/java/com/nhnacademy/order_server/service/impl/OrderTransactionBoundaryTest.java)가 실패합니다. ([fa1a794](https://github.com/Sungw0o/HighFivebooks-V2/commit/fa1a79430a12b70aa7204adc72daccbf94f3db1d))

## 💻 Tech Stacks

| 구분 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.7, Spring Data JPA, Spring Cloud Gateway, Eureka, Config Server, OpenFeign, Resilience4j |
| Frontend | Thymeleaf, React, TypeScript, Vite |
| Data | MySQL 8.4, Redis 7.2, Elasticsearch 8.18 + Nori, MinIO |
| Messaging | RabbitMQ, Retry/DLQ, idempotency key |
| Infrastructure | Ubuntu, Docker, Docker Compose, Nginx, HAProxy, Cloudflare |
| CI/CD | GitHub Actions, CodeRabbit, SonarQube, Docker health check |
| Test | JUnit 5, Mockito, Spring Boot Test, k6 |

## 🏗️ System Architecture

![HighFiveBooks Spring Cloud MSA 시스템 아키텍처](docs/assets/highfivebooks-system-architecture.png)

Cloudflare와 Nginx·HAProxy가 외부 트래픽을 전달하고, Gateway가 인증·인가와 도메인 라우팅을 담당합니다. Eureka는 서비스 위치를 제공하고 Config Server는 공통 설정을 관리합니다. V2에서도 주문·도서·회원·쿠폰·결제 서비스의 독립 경계는 유지했습니다.

### 시스템 구성도 (docker-compose 기준)

[docker-compose.yml](docker-compose.yml)과 서비스별 `application-prod.yml`에 정의된 구성입니다.

```mermaid
flowchart LR
    User["사용자 / API 클라이언트"]

    subgraph Apps["앱 서비스 (compose profile: apps)"]
        Member["member-server :9001"]
        Book["book-server :9002"]
        Coupon["coupon-server :9004"]
        Payment["payment-server :9005"]
        Order["order-server :9006"]
    end

    subgraph Infra["인프라 컨테이너"]
        MySQL[("MySQL 8.4")]
        Redis[("Redis 7.2")]
        MQ[["RabbitMQ 3.13"]]
        ES[("Elasticsearch 8.18 + Nori")]
        MinIO[("MinIO")]
    end

    Toss["Toss Payments API"]
    Aladin["Aladin API"]

    User --> Apps
    Order -->|"OpenFeign"| Book
    Order -->|"OpenFeign"| Member
    Order -->|"OpenFeign"| Coupon
    Order -->|"OpenFeign"| Payment
    Payment -->|"결제 성공 이벤트"| MQ
    MQ -->|"payment-success-queue"| Order
    Apps --> MySQL
    Apps --> Redis
    Apps --> MQ
    Book --> ES
    Book --> MinIO
    Payment --> Toss
    Book --> Aladin
```

### 주문/TCC 처리 흐름

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Order as Order Service
    participant Book as Book Service
    participant Member as Member Service
    participant Payment as Payment Service
    participant MQ as RabbitMQ<br/>payment-success-queue
    participant DB as Order DB

    Note over User,DB: Try 단계
    User->>Order: 주문 생성 요청
    Order->>Book: 재고 선점 Try
    Book-->>Order: 선점 성공
    Order->>Member: 포인트 가승인 Try
    Member-->>Order: 가승인 성공
    Order->>DB: PAYMENT_WAITING 주문 저장
    Order-->>User: 결제 대기 주문 반환

    Note over User,DB: 결제 성공 / Confirm 단계
    User->>Payment: 결제 요청
    Payment->>MQ: 결제 성공 이벤트 발행
    MQ->>Order: payment success consume
    Order->>Book: 재고 차감 Confirm
    Book-->>Order: 확정 성공
    Order->>Member: 포인트 사용 Confirm
    Member-->>Order: 확정 성공
    Order->>DB: PREPARING 상태 변경

    Note over User,DB: 실패 / Cancel 단계
    alt 결제 실패 또는 주문 취소
        Order->>Book: 재고 선점 해제 Cancel
        Order->>Member: 포인트 가승인 해제 Cancel
        Order->>DB: CANCELED 상태 변경
    end
```

이 흐름은 포트폴리오의 **HighFiveBooks 주문 도메인/TCC 다이어그램**과 동일한 참여자와 단계로 정리했습니다. 쿠폰 계산·사용은 실제 주문 기능에 포함되지만, 다이어그램에서는 재고·포인트 TCC 경계를 명확히 보여주기 위해 생략했습니다.

## 🚀 CI/CD Pipeline

![HighFiveBooks GitHub Actions CI/CD 파이프라인](docs/assets/highfivebooks-cicd-legacy.png)

- Push와 Pull Request를 기준으로 자동 검증을 시작합니다.
- CodeRabbit 리뷰와 SonarQube 정적 분석 결과를 확인한 뒤 배포합니다.
- 배포 후 서비스 health check를 수행하고, 실패하면 수정 후 파이프라인을 다시 실행합니다.

팀 프로젝트 기간에 구성한 파이프라인입니다. 각 서비스의 `maven.yml`이 PR에서는 `ci-check`, push에서는 `cd-deploy`로 팀 저장소의 공용 워크플로우를 호출합니다([order-server maven.yml#L10-L45](services/order-server/.github/workflows/maven.yml#L10-L45)). 단계별 정의 위치는 다음과 같습니다.

| 단계 | 정의 위치 |
| --- | --- |
| 테스트(`mvn clean verify`) | [service-workflow.yml#L113-L130](https://github.com/nhnacademy-be12-high-five/.github/blob/dc4ca623c0430e0e08d9a11958335e5e68855261/.github/workflows/service-workflow.yml#L113-L130) |
| SonarQube 정적 분석(main·dev 브랜치) | [service-workflow.yml#L95-L121](https://github.com/nhnacademy-be12-high-five/.github/blob/dc4ca623c0430e0e08d9a11958335e5e68855261/.github/workflows/service-workflow.yml#L95-L121) |
| Docker 이미지 빌드·푸시 | [service-workflow.yml#L133-L149](https://github.com/nhnacademy-be12-high-five/.github/blob/dc4ca623c0430e0e08d9a11958335e5e68855261/.github/workflows/service-workflow.yml#L133-L149) |
| SSH 배포(인스턴스 -1, -2 순차) | [service-workflow.yml#L151-L225](https://github.com/nhnacademy-be12-high-five/.github/blob/dc4ca623c0430e0e08d9a11958335e5e68855261/.github/workflows/service-workflow.yml#L151-L225) |
| 헬스체크(`/actuator/health` UP 확인) | [service-workflow.yml#L193-L208](https://github.com/nhnacademy-be12-high-five/.github/blob/dc4ca623c0430e0e08d9a11958335e5e68855261/.github/workflows/service-workflow.yml#L193-L208) |

## 핵심 흐름: 결제 성공 메시지 처리

[문제를 고친 기록](#문제를-고친-기록) 1·3번이 만나는 경로입니다. 외부 호출은 트랜잭션 밖에서 하고, 실패한 메시지는 재시도 뒤 DLQ로 보냅니다.

```mermaid
sequenceDiagram
    participant Pay as payment-server
    participant MQ as RabbitMQ
    participant L as PaymentMessageListener
    participant S as OrderServiceImpl
    participant Ext as 쿠폰·도서·회원 서버
    participant M as OrderStatusMutationService
    participant DB as Order DB
    participant DLQ as Order Payment DLQ

    Pay->>MQ: 결제 성공 이벤트 발행
    MQ->>L: payment-success-queue 소비
    L->>S: processPaymentSuccessMessage (트랜잭션 없음)
    S->>DB: 주문 조회
    alt 주문이 PAYMENT_WAITING이 아님
        S-->>L: 처리 없이 종료 (중복 메시지)
    else 결제 대기 주문
        S->>Ext: 쿠폰 사용, 재고 확정, 포인트 확정 (Feign, 자동 재시도 없음)
        S->>M: markPaymentSuccess
        M->>DB: PREPARING으로 변경 (짧은 트랜잭션)
    end
    opt 처리 중 예외 (금액 불일치, 외부 호출 실패)
        L->>L: 예외 전파 후 최대 3회 시도 (1초부터 2배 backoff)
        L->>DLQ: RejectAndDontRequeueRecoverer로 Dead Letter Exchange 이동
    end
```

근거: [PaymentMessageListener](services/order-server/src/main/java/com/nhnacademy/order_server/listener/PaymentMessageListener.java), [OrderServiceImpl](services/order-server/src/main/java/com/nhnacademy/order_server/service/impl/OrderServiceImpl.java), [RabbitMqConfig](services/order-server/src/main/java/com/nhnacademy/order_server/config/RabbitMqConfig.java), [application-prod.yml#L58-L64](services/order-server/src/main/resources/application-prod.yml#L58-L64), [FeignResilienceConfig](services/order-server/src/main/java/com/nhnacademy/order_server/config/FeignResilienceConfig.java)

## ERD

order-server 엔티티(`services/order-server/.../entity`) 기준 주문 DB의 핵심 테이블입니다.

```mermaid
erDiagram
    ORDERS ||--o{ ORDER_ITEM : "주문 상품"
    ORDERS ||--o| DELIVERY : "배송"
    ORDERS ||--o| ORDER_RETURN : "반품"
    WRAPPER |o--o{ ORDER_ITEM : "포장지"

    ORDERS {
        bigint order_id PK
        varchar order_key UK
        bigint user_id
        varchar dlv_stat
        int pay_amt
        int prod_amt
        int dlv_fee
        int cpn_disc_amt
        int pnt_use_amt
        varchar payment_key
        bigint coupon_id
    }
    ORDER_ITEM {
        bigint order_item_id PK
        bigint order_id FK
        bigint wrp_id FK
        bigint book_id
        int qty
        int unit_prc
    }
    DELIVERY {
        bigint order_id PK "orders.order_id 참조"
        date req_dlv_dt
        datetime act_ship_dt
        datetime act_comp_dt
        varchar tracking_no
    }
    ORDER_RETURN {
        bigint order_id PK "orders.order_id 참조"
        varchar ret_rsn
        int refund_amt
        datetime ret_dt
    }
    WRAPPER {
        bigint wrapper_id PK
        varchar wrp_nm
        int wrp_price
    }
    DELIVER_POLICY {
        bigint deliver_policy_id PK
        int min_order_amount
        int standard_shipping_fee
        int remote_area_surcharge
    }
```

`book_id`, `user_id`, `coupon_id`는 다른 서비스의 DB를 가리키는 ID이며 외래 키로 묶지 않습니다. `DELIVER_POLICY`는 배송비 계산에만 쓰는 독립 테이블입니다.

## ✨ 주요 기능 (Key Features)

| 영역 | 주요 기능 |
| --- | --- |
| 도서 탐색 | 목록·상세 조회, 카테고리 탐색, Elasticsearch Nori 검색, 리뷰·좋아요, Aladin 도서 수집 |
| 회원 | 회원가입·로그인, 소셜 로그인, 배송지, 장바구니, 포인트, 회원 등급 |
| 쿠폰 | 쿠폰 발급·계산·사용·취소 |
| 주문 | 주문 생성·조회, 포장·배송 정책, 주문 취소·반품, 최근 주문 조회 |
| 결제 | Toss Payments 승인·취소, 결제 성공 이벤트 발행 |
| 분산 정합성 | 재고 `hold/confirm/release`, 포인트 `reserve/confirm/cancel`, 중복 요청 방지 |
| 장애 격리 | RabbitMQ Retry/DLQ, Feign timeout·CircuitBreaker, 상태 변경 호출의 자동 retry 차단 |

## 🔎 Design Highlights

### TCC 기반 주문 정합성과 멱등성

- **문제 상황**: 서비스별 DB를 사용해 재고·포인트·결제를 하나의 로컬 트랜잭션으로 묶을 수 없었고, 실패나 중복 요청이 부분 반영과 이중 처리를 만들 수 있었습니다.
- **의사 결정**: 이벤트 순서와 보상 이벤트까지 관리해야 하는 Saga보다, 선점 가능한 재고·포인트의 Try·Confirm·Cancel을 코드에서 명확히 확인할 수 있는 TCC를 선택했습니다.
- **해결**: 재고 선점과 포인트 가승인 후 결제 결과에 따라 확정 또는 반환하고, 주문 상태와 도메인별 idempotency key로 중복 Confirm·Cancel을 차단했습니다.
- **검증 결과**: 중복 Confirm·Cancel, 포인트 반환 실패, 재고 반환 지연, 결제 취소 지연 경로를 테스트했습니다.
- **한계**: 보상 완료 단계를 별도로 영속화하지 않아 프로세스 종료 이후의 durable retry는 지원하지 않습니다.

### 반복 실패 메시지 Retry/DLQ 격리

- **문제 상황**: 금액 불일치나 주문 후처리 실패 메시지가 반복 소비되면 정상 메시지 처리까지 방해할 수 있었습니다.
- **의사 결정**: 상태 변경 Feign 호출의 자동 retry는 끄고, 다시 처리할 수 있는 결제 성공 메시지에만 제한된 retry와 DLQ를 적용했습니다.
- **해결**: Listener 예외를 컨테이너로 전달하고 최대 3회 재시도한 뒤 `RejectAndDontRequeueRecoverer`와 Dead Letter Exchange로 격리했습니다.
- **검증 결과**: 큐·Exchange·DLQ·Binding과 retry/backoff 설정을 테스트하고, 로컬 RabbitMQ에서 payload와 `x-death` 헤더가 유지된 DLQ 이동을 확인했습니다.
- **한계**: 실브로커 통합 테스트는 RabbitMQ가 제공되는 환경에서만 조건부로 실행됩니다.

### JPA Collection Fetch와 Pageable 병목 개선

- **문제 상황**: 도서 157,118건에서 collection `EntityGraph`와 `Pageable`을 함께 사용하자 Hibernate가 연관 결과를 메모리에 올린 뒤 pagination을 수행해 목록 p95가 9,039.51ms까지 증가했습니다.
- **의사 결정**: API 응답을 바꾸지 않으면서 목록과 상세의 조회 목적에 따라 Fetch Graph를 분리했습니다.
- **해결**: 목록에서는 `publisher`만 즉시 로딩하고, `bookAuthors.author`는 상세 조회에서만 불러오도록 변경했습니다.
- **검증 결과**: 동일 k6 시나리오의 저장 결과에서 목록 p95가 9,039.51ms에서 51.97ms로 개선됐습니다.
- **한계**: 30초 전후 1회 측정이며 요청 수가 달라 반복 실험 중앙값으로 표현하지 않습니다.

## 🧪 Validation Status

| 항목 | 상태 |
| --- | --- |
| order-server | 132 tests, 0 failures, 0 errors, 1 skipped |
| book-server | 346 tests, 0 failures, 0 errors, 0 skipped |
| TCC 멱등·보상 | 중복 Confirm/Cancel과 실패·timeout 단위 테스트 통과 |
| RabbitMQ Retry/DLQ | 설정·Listener 테스트 통과, 실브로커 통합 테스트는 환경에 따라 조건부 실행 |
| JPA 목록 조회 | p95 9,039.51ms → 51.97ms, HTTP 실패율 0% |

Windows의 비 ASCII 사용자 경로에서 JaCoCo agent가 JVM 시작에 실패할 수 있어 book-server 전체 테스트는 `-Djacoco.skip=true` 조건으로 확인했습니다.

## 🛠️ Local Development

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

기본 포트는 다음과 같습니다.

| 서비스 | 포트 |
| --- | ---: |
| member-server | 9001 |
| book-server | 9002 |
| coupon-server | 9004 |
| payment-server | 9005 |
| order-server | 9006 |
| RabbitMQ Management | 15672 |

실제 DB 비밀번호, JWT Secret, 외부 API 자격증명은 `.env` 또는 배포 환경에만 설정합니다.

## 📁 Repository Structure

```text
HighFivebooks-V2/
├─ apps/storefront/          # React 사용자 쇼핑몰 프론트
├─ services/
│  ├─ order-server/         # 주문 조율, TCC, 결제 이벤트 처리
│  ├─ book-server/          # 도서, 검색, 재고
│  ├─ member-server/        # 회원, 포인트, 장바구니
│  ├─ coupon-server/        # 쿠폰 계산, 사용, 취소
│  └─ payment-server/       # 결제 승인, 취소, 이벤트 발행
├─ perf/                    # k6 시나리오와 결과
├─ scripts/                 # 실행·검증 스크립트
├─ docs/                    # 설계와 트러블슈팅 근거
└─ docker-compose.yml
```
