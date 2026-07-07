# HighFiveBooks V2 - Root Agent Harness

> 이 파일은 Claude Code, Codex, 기타 AI 에이전트가 HighFiveBooks V2에서 작업하기 전에 반드시 읽어야 하는 최상위 하네스다.
> 프론트엔드 작업을 맡더라도 이 프로젝트의 핵심은 MSA 백엔드 리팩토링과 Kubernetes 전환이다.
> 저장소는 monorepo지만 서비스 구조는 monolith가 아니다. 각 Spring Boot 서버는 독립 실행/독립 배포되는 MSA로 유지한다.

---

## 0. 프로젝트 한 줄 정의

HighFiveBooks V2는 기존 팀 프로젝트의 MSA 구조를 유지하면서, 주문 도메인을 중심으로 트랜잭션 경계, 메시징 안정성, 멀티 인스턴스 운영 문제를 리팩토링하고 Kubernetes-native 런타임으로 전환하는 포트폴리오 프로젝트다.

핵심 문장:

> 리팩토링 관리를 위해 monorepo로 통합했지만, 각 도메인 서버는 독립 실행/독립 배포 가능한 MSA로 유지한다. Kubernetes에서는 각 서비스를 별도 Deployment/Service로 배포한다.

---

## 1. 현재 저장소 구조

```text
HighFivebooks-V2/
  apps/
    console/              React/Vite 데모 콘솔

  services/
    order-server/         메인 리팩토링 대상
    coupon-server/        메시징/멱등성 참고 및 일부 개선 대상
    book-server/          런타임 의존성: 책 정보, 재고
    member-server/        런타임 의존성: 회원, 등급, 포인트, 장바구니
    payment-server/       런타임 의존성: 결제 확인, 결제 성공 이벤트

  k8s/                    Kubernetes 매니페스트
  docs/                   계획서, 로드맵, 분석 문서
```

---

## 2. 역할 분담

### Claude Code

Claude는 프론트엔드를 주로 맡을 수 있다. 단, 단순 화면 작업자가 아니라 이 프로젝트의 백엔드/MSA 맥락까지 이해하는 풀스택 협업 에이전트로 행동한다.

주 담당:

- `apps/console` React/Vite 콘솔 구현
- API 연동 구조 설계
- 주문/결제/쿠폰/재고/포인트 상태를 보여주는 시연 UI 구현
- 백엔드 API 계약을 읽고 프론트 타입/상태 모델 정리

보조 가능:

- `services/order-server` 흐름 분석
- Feign boundary/API contract 분석
- 주문 생성/결제 성공/취소 보상 흐름 문서화
- K8s 시연 흐름을 프론트 UI에 녹이는 작업

주의:

- Claude가 백엔드를 수정할 수는 있지만, `order-server`의 트랜잭션/메시징/스케줄러 리팩토링은 큰 변경이므로 먼저 분석 보고 후 진행한다.
- 프론트 구현 중 백엔드 API가 필요하면 mock으로 대충 숨기지 말고, 필요한 endpoint와 DTO를 명확히 적는다.

### Codex

주 담당:

- `services/order-server` 리팩토링
- 테스트 베이스라인 유지
- Feign boundary test, transaction boundary, RabbitMQ DLQ/Retry, scheduler lock
- 문서/Notion 진행 로그 갱신
- 최종 검증, commit, push

### Human

주 담당:

- 방향 결정
- 포트폴리오 스토리 승인
- Secret, 배포, 외부 서비스 계정, GitHub 설정 승인

---

## 3. 절대 원칙

1. 이 프로젝트는 MSA 유지가 원칙이다. 서비스를 하나의 Spring Boot 앱으로 합치지 않는다.
2. monorepo는 저장소 관리 방식일 뿐이다. 배포 단위는 여전히 여러 서비스다.
3. `front_server`는 V2의 주 작업 대상이 아니다. 프론트는 `apps/console`에서 React로 구현한다.
4. `eureka_server`, `config_server`, `gateway`는 V2에서 1급 서비스로 유지하지 않는다.
5. Eureka는 Kubernetes Service DNS로 대체한다.
6. Config Server는 ConfigMap/Secret으로 대체한다.
7. Gateway는 Ingress로 대체한다.
8. 백엔드 안정화 전에는 프론트에 과도한 시간을 쓰지 않는다. 프론트는 백엔드 개선을 시연하는 콘솔이다.
9. Secret, API key, DB password, JWT secret을 코드에 직접 쓰지 않는다.
10. 테스트가 깨진 상태로 리팩토링을 크게 밀지 않는다.

---

## 4. 프론트엔드 작업 지침

작업 위치:

```text
apps/console
```

기술:

- React
- TypeScript
- Vite
- CSS 또는 프로젝트에 도입된 스타일링 방식

프론트의 목적:

- 쇼핑몰 전체 UI 재구현이 아니다.
- 백엔드 리팩토링과 K8s 전환을 보여주는 데모/운영 콘솔이다.

1차 화면 목표:

```text
HighFiveBooks Ops Console

[Service Status]
order | coupon | book | member | payment | rabbitmq | redis | mysql

[Order Scenario]
userId
bookId / quantity
couponId
usedPoint
[Create Order]

[Payment Event]
orderId
paymentKey
totalAmount
[Publish Success Event]
[Publish Invalid Amount Event]

[Compensation / Result]
order status
stock status
coupon status
point status
message status

[Kubernetes]
ingress endpoint
order replicas
rollout status
last deploy version
```

프론트 구현 규칙:

- API base URL은 환경변수로 둔다.
- 실제 API가 아직 없으면 mock adapter를 분리한다.
- mock 데이터와 실제 API 호출 코드를 같은 함수 안에 뒤섞지 않는다.
- 백엔드 계약이 불명확하면 필요한 endpoint, request, response를 문서화한다.
- `any`를 남발하지 않는다.
- UI는 시연자가 한눈에 주문/결제/보상/K8s 상태를 설명할 수 있게 구성한다.
- marketing landing page를 만들지 않는다.
- 첫 화면은 실제 콘솔이어야 한다.

프론트가 백엔드에 요청해야 할 API 후보:

```text
GET  /api/health/services
POST /api/orders
GET  /api/orders/{orderId}
POST /api/payments/success-events
POST /api/payments/invalid-amount-events
POST /api/orders/{orderId}/cancel
GET  /api/scenarios/{scenarioId}/result
```

위 경로는 확정 명세가 아니다. 구현 전 `services/order-server`의 실제 controller와 adapter 구조를 확인한다.

---

## 5. 백엔드 작업 지침

메인 작업 위치:

```text
services/order-server
```

보조 분석 위치:

```text
services/coupon-server
services/book-server
services/member-server
services/payment-server
```

백엔드 우선순위:

1. 주문 흐름 지도와 테스트 분류
2. Feign 경계 테스트
3. 트랜잭션 경계 리팩토링
4. RabbitMQ DLQ/Retry
5. 멀티 인스턴스 스케줄러 방어
6. Feign timeout/CircuitBreaker
7. 통합 환경
8. K8s 전환
9. React 콘솔 연동

`order-server` 현재 기준:

- 로컬 테스트는 통과해야 한다.
- 기본 `mvn test`는 JaCoCo agent를 붙이지 않는다.
- coverage는 Maven profile로 분리한다.

검증 명령:

```powershell
cd services/order-server
.\mvnw.cmd test
```

기대 결과:

```text
98 tests, 0 failures, 0 errors, 0 skipped
```

백엔드 리팩토링 금지 행동:

- 테스트 없이 `OrderServiceImpl`의 큰 흐름을 갈아엎지 않는다.
- 외부 Feign 호출을 DB 트랜잭션 안에 새로 추가하지 않는다.
- 재고/쿠폰/포인트처럼 중복 호출 위험이 있는 API에 무작정 retry를 걸지 않는다.
- 메시지 처리 실패를 무한 requeue 상태로 방치하지 않는다.
- K8s 도입을 이유로 서비스 경계를 없애지 않는다.

---

## 6. 주문 흐름 지도

### 주문 생성

```text
React Console
  -> order-server
    -> member-server: 회원 등급 조회
    -> member-server: 포인트 예약
    -> book-server: 책 정보 조회
    -> book-server: 재고 선점
    -> coupon-server: 쿠폰 할인 계산
    -> order DB 저장
```

### 결제 성공 후처리

```text
payment-server 또는 RabbitMQ
  -> order-server: payment-success message
    -> order 상태/금액 검증
    -> coupon-server: 쿠폰 사용 확정
    -> book-server: 재고 차감 확정
    -> member-server: 포인트 확정
    -> order 상태 변경
```

### 주문 취소/보상

```text
order-server
  -> book-server: 재고 복구
  -> member-server: 포인트 예약 취소/환불
  -> coupon-server: 쿠폰 사용 취소
  -> payment-server: 결제 취소
```

---

## 7. Kubernetes 방향

V2 최종 런타임은 Kubernetes-native 구성을 목표로 한다.

배포 단위:

```text
console Deployment
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

- Eureka 제거: Service DNS 사용
- Config Server 제거: ConfigMap/Secret 사용
- Gateway 제거: Ingress 사용
- order-server는 replica 2개 이상 시나리오를 고려한다.
- scheduler 중복 실행 방지를 반드시 고려한다.

---

## 8. 브랜치와 커밋

현재 기준 브랜치:

```text
main
```

권장 브랜치:

```text
frontend/console-scenarios
refactor/order-flow-map
refactor/order-transaction-boundary
refactor/payment-message-dlq
refactor/scheduler-lock
infra/k8s-baseline
docs/portfolio-evidence
```

커밋 예시:

```text
docs: add agent harness for Claude frontend work
feat: add order scenario console shell
refactor: split order transaction boundary
test: add order client boundary tests
infra: add order server k8s manifests
```

작업 원칙:

- 한 브랜치는 하나의 목적만 가진다.
- 큰 변경은 분석 -> 계획 -> 구현 -> 검증 순서로 진행한다.
- 커밋 전 `git status`로 불필요한 파일을 확인한다.
- `node_modules`, `dist`, `target`, `.env`는 커밋하지 않는다.

---

## 9. 작업 전 체크리스트

```text
[ ] 현재 위치가 HighFivebooks-V2 루트인가?
[ ] 작업 대상이 apps/console인지 services/order-server인지 명확한가?
[ ] 이 변경이 MSA 유지 원칙과 충돌하지 않는가?
[ ] 실제 API 계약을 확인했는가?
[ ] mock이면 mock이라고 분리해 표시했는가?
[ ] Secret 또는 로컬 환경 파일을 건드리지 않는가?
[ ] 변경 후 실행할 검증 명령을 정했는가?
```

---

## 10. 작업 후 체크리스트

프론트:

```powershell
cd apps/console
npm run build
```

백엔드:

```powershell
cd services/order-server
.\mvnw.cmd test
```

공통:

```text
[ ] 빌드/테스트가 통과했는가?
[ ] 불필요한 파일이 수정되지 않았는가?
[ ] API 계약 변경이 문서화되었는가?
[ ] Secret이 노출되지 않았는가?
[ ] 다음 사람이 이어받을 수 있게 README/docs/Notion 갱신이 필요한가?
```

---

## 11. Claude에게 맡길 때 사용할 요청 템플릿

```text
HighFiveBooks V2 프론트 작업을 맡아줘.
먼저 루트 AGENTS.md와 CLAUDE.md를 읽고, 이 프로젝트가 monolith가 아니라 monorepo 안의 MSA라는 점을 기준으로 잡아줘.

주 작업 위치는 apps/console이야.
다만 프론트는 쇼핑몰 전체 UI가 아니라 order/payment/coupon/stock/point/K8s 전환을 시연하는 운영 콘솔이야.

백엔드도 읽을 수 있어.
필요하면 services/order-server의 controller/dto/client를 분석해서 필요한 API 계약을 정리해줘.
하지만 백엔드 코드를 수정하기 전에는 먼저 변경 계획과 영향 범위를 보고해줘.

금지:
- monolith로 합치기
- front_server를 되살리기
- .env 또는 Secret 수정
- node_modules/dist/target 커밋
- 실제 API 계약을 확인하지 않고 임의 endpoint 확정

작업 후에는 apps/console에서 npm run build를 통과시켜줘.
```

---

## 12. Codex에게 맡길 때 사용할 요청 템플릿

```text
HighFiveBooks V2 백엔드 리팩토링을 진행해줘.
루트 AGENTS.md와 docs/HIGHFIVEBOOKS_REFACTORING_ROADMAP.md를 기준으로,
services/order-server의 1순위인 주문 흐름 지도와 Feign boundary 정리부터 시작해줘.

작업 후 services/order-server에서 .\mvnw.cmd test를 통과시켜줘.
필요하면 Notion 리팩토링 계획서 ver2에도 진행 로그를 추가해줘.
```

