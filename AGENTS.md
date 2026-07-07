# HighFiveBooks V2 - Root Agent Harness

> Claude Code, Codex, 기타 AI 에이전트는 작업 전에 이 파일을 먼저 읽는다.
> 이 저장소는 monorepo지만 monolith가 아니다. 서비스는 독립 실행/독립 배포되는 MSA로 유지한다.
> 프론트는 단순 데모가 아니라 기존 Thymeleaf `front_server`를 React로 대체하는 실제 사용자 쇼핑몰 프론트다.

---

## 0. 프로젝트 정의

HighFiveBooks V2는 기존 HighFiveBooks 팀 프로젝트를 개인 포트폴리오용으로 재정리하는 저장소다.

목표:

- MSA 유지
- `order-server` 중심 백엔드 리팩토링
- Spring Cloud 인프라 요소를 Kubernetes-native 구조로 전환
- 기존 Thymeleaf 프론트를 React/Vite storefront로 대체

핵심 문장:

> 리팩토링 관리를 위해 monorepo로 통합했지만, 각 도메인 서버는 독립 실행/독립 배포 가능한 MSA로 유지한다.

---

## 1. 저장소 구조

```text
HighFivebooks-V2/
  apps/
    storefront/           React/Vite 사용자 쇼핑몰 프론트

  services/
    order-server/         메인 리팩토링 대상
    book-server/          책 정보, 검색, 재고
    member-server/        회원, 등급, 포인트, 장바구니
    coupon-server/        쿠폰, 메시징/멱등성 참고
    payment-server/       결제 확인, 결제 성공 이벤트

  k8s/                    Kubernetes manifests
  docs/                   계획서, 로드맵, 분석 문서
```

`apps/storefront`가 Claude의 주 작업 위치다.

---

## 2. 에이전트 역할

### Claude Code

주 담당:

- `apps/storefront` React/Vite 프론트 구현
- 기존 Thymeleaf `front_server`의 사용자 플로우를 React로 대체
- 도서 탐색, 도서 상세, 장바구니, 주문서, 결제, 마이페이지 구현
- 백엔드 controller/dto/client를 읽고 API 계약 정리
- 필요한 API가 없거나 불명확하면 임의 구현하지 말고 계약 초안 작성

보조 가능:

- `services/order-server` controller/dto 흐름 분석
- order/book/member/coupon/payment API 연동 범위 정리
- 백엔드 수정 계획 작성

주의:

- Claude가 백엔드를 읽는 것은 허용한다.
- Claude가 백엔드를 수정할 수도 있지만, 큰 변경은 먼저 계획을 보고한다.
- 특히 트랜잭션, Feign, RabbitMQ, Scheduler, `pom.xml`, `application.yml`, workflow 변경은 사용자 확인 후 진행한다.

### Codex

주 담당:

- `services/order-server` 리팩토링
- 테스트 베이스라인 유지
- Feign boundary test
- transaction boundary
- RabbitMQ DLQ/Retry
- scheduler lock
- K8s manifests
- 문서/Notion 진행 로그

---

## 3. 절대 원칙

1. MSA를 monolith로 합치지 않는다.
2. monorepo는 저장소 관리 방식일 뿐이고, 배포 단위는 여러 서비스다.
3. 기존 `front_server`는 직접 고치지 않는다. React `apps/storefront`로 대체한다.
4. `storefront`는 실제 사용자 쇼핑몰 프론트다.
5. 별도 운영 콘솔은 만들지 않는다. 필요한 관리자 기능은 실제 서비스 요구에 맞는 화면으로만 추가한다.
6. Eureka는 Kubernetes Service DNS로 대체한다.
7. Config Server는 ConfigMap/Secret으로 대체한다.
8. Gateway는 Ingress로 대체한다.
9. Secret, API key, DB password, JWT secret을 코드에 직접 쓰지 않는다.
10. `node_modules`, `dist`, `target`, `.env`는 커밋하지 않는다.

---

## 4. Frontend Scope

작업 위치:

```text
apps/storefront
```

기술:

- React
- TypeScript
- Vite

구현 목표:

```text
Home
  - 추천/신간/베스트 도서

Book
  - 도서 목록
  - 검색
  - 카테고리
  - 도서 상세
  - 리뷰

Cart
  - 장바구니 조회
  - 수량 변경
  - 선택 삭제

Order
  - 주문서
  - 배송지
  - 포장
  - 쿠폰 적용
  - 포인트 사용
  - 최종 금액 계산

Payment
  - 결제 요청
  - 결제 성공/실패 처리
  - 주문 상태 확인

My Page
  - 회원 정보
  - 주소
  - 쿠폰
  - 포인트
  - 주문 내역
  - 주문 상세
  - 취소/반품
```

프론트 규칙:

- API base URL은 환경변수로 둔다.
- mock과 real API adapter를 분리한다.
- 실제 백엔드 endpoint를 확인하지 않고 임의 API를 확정하지 않는다.
- API가 없으면 필요한 endpoint/request/response를 문서화한다.
- `any`를 남발하지 않는다.
- marketing landing page를 만들지 않는다.
- 첫 화면은 실제 쇼핑몰 홈 또는 도서 탐색 화면이어야 한다.
- 기존 `front_server` 코드는 참고할 수 있지만 그대로 이식하지 않는다.

API 후보:

```text
GET    /api/books
GET    /api/books/{bookId}
GET    /api/books/search
GET    /api/categories
GET    /api/cart
POST   /api/cart/items
PATCH  /api/cart/items/{itemId}
DELETE /api/cart/items/{itemId}
GET    /api/members/me
GET    /api/members/me/addresses
GET    /api/members/me/coupons
GET    /api/members/me/points
POST   /api/orders
GET    /api/orders/{orderId}
GET    /api/orders/recent
POST   /api/orders/{orderId}/cancel
POST   /api/payments/confirm
```

위 경로는 후보일 뿐이다. 구현 전 실제 controller를 확인한다.

---

## 5. Backend Scope

메인 작업 위치:

```text
services/order-server
```

보조 분석 위치:

```text
services/book-server
services/member-server
services/coupon-server
services/payment-server
```

우선순위:

1. 주문 흐름 지도와 테스트 분류
2. Feign 경계 테스트
3. 트랜잭션 경계 리팩토링
4. RabbitMQ DLQ/Retry
5. 멀티 인스턴스 스케줄러 방어
6. Feign timeout/CircuitBreaker
7. 통합 환경
8. K8s 전환
9. storefront API 연동

검증:

```powershell
cd services/order-server
.\mvnw.cmd test
```

기대 결과:

```text
98 tests, 0 failures, 0 errors, 0 skipped
```

금지:

- 테스트 없이 주문 흐름을 크게 갈아엎기
- 외부 Feign I/O를 DB 트랜잭션 안에 새로 추가하기
- 재고/쿠폰/포인트 API에 무작정 retry 걸기
- poison message를 무한 requeue 상태로 방치하기

---

## 6. Order Flow

주문 생성:

```text
React Storefront
  -> order-server
    -> member-server: 회원 등급 조회
    -> member-server: 포인트 예약
    -> book-server: 책 정보 조회
    -> book-server: 재고 선점
    -> coupon-server: 쿠폰 할인 계산
    -> order DB 저장
```

결제 성공:

```text
payment-server 또는 RabbitMQ
  -> order-server
    -> 주문 상태/금액 검증
    -> coupon-server: 쿠폰 사용 확정
    -> book-server: 재고 차감 확정
    -> member-server: 포인트 확정
    -> order 상태 변경
```

취소/보상:

```text
order-server
  -> book-server: 재고 복구
  -> member-server: 포인트 예약 취소/환불
  -> coupon-server: 쿠폰 사용 취소
  -> payment-server: 결제 취소
```

---

## 7. Kubernetes Direction

최종 배포 단위:

```text
storefront Deployment
order-server Deployment
book-server Deployment
member-server Deployment
coupon-server Deployment
payment-server Deployment
MySQL
Redis
RabbitMQ
Ingress
```

전환 규칙:

- Eureka 제거: Service DNS
- Config Server 제거: ConfigMap/Secret
- Gateway 제거: Ingress
- order-server replica 2개 이상 고려
- scheduler 중복 실행 방지 고려

---

## 8. Branch Examples

```text
frontend/storefront
frontend/storefront-api-contract
refactor/order-flow-map
refactor/order-transaction-boundary
refactor/payment-message-dlq
refactor/scheduler-lock
infra/k8s-baseline
docs/portfolio-evidence
```

---

## 9. Validation

Frontend:

```powershell
cd apps/storefront
npm run build
```

Backend:

```powershell
cd services/order-server
.\mvnw.cmd test
```

---

## 10. Claude Prompt Template

```text
HighFiveBooks V2 프론트 작업을 맡아줘.
먼저 루트 AGENTS.md와 CLAUDE.md를 읽고 따라줘.

이 프로젝트는 monorepo지만 monolith가 아니라 MSA야.
주 작업 위치는 apps/storefront야.
기존 Thymeleaf front_server를 직접 고치지 말고, React/Vite로 실제 사용자 쇼핑몰 프론트를 새로 구현해줘.

목표는 실제 서비스 플로우야.
도서 탐색, 상세, 장바구니, 주문서, 쿠폰/포인트, 결제, 마이페이지까지 구현 범위를 잡아줘.

필요하면 services/order-server, book-server, member-server, coupon-server, payment-server를 읽어서 API 계약을 정리해도 돼.
하지만 백엔드 코드 수정, 의존성 추가, application.yml/pom.xml/workflow 수정, Secret/.env 수정, 배포 실행은 먼저 계획을 보고하고 확인받아줘.

작업 후 apps/storefront에서 npm run build를 통과시켜줘.
```
