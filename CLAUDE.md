# CLAUDE.md - HighFiveBooks V2 Claude Harness

Claude, 이 저장소에서 작업하기 전에 반드시 루트의 `AGENTS.md`를 먼저 끝까지 읽어라.

이 프로젝트는 단순 React 프론트 프로젝트가 아니다. `HighFivebooks-V2`는 monorepo이지만, 구조는 MSA다.

```text
apps/console              React/Vite demo console
services/order-server     main backend refactoring target
services/book-server      runtime dependency
services/member-server    runtime dependency
services/coupon-server    messaging/idempotency reference
services/payment-server   payment event dependency
k8s                       Kubernetes manifests
docs                      planning notes
```

## Your Primary Role

주 담당은 `apps/console` 프론트엔드다.

하지만 이 프론트는 쇼핑몰 전체 화면이 아니다. 주문 생성, 결제 성공 이벤트, 쿠폰/재고/포인트 보상, RabbitMQ 장애, Kubernetes 전환을 보여주는 운영 데모 콘솔이다.

## You May Read Backend Code

필요하면 백엔드 코드를 읽어라.

특히 아래는 프론트 API 계약을 잡기 위해 확인할 수 있다.

```text
services/order-server/src/main/java
services/order-server/src/test/java
services/payment-server/src/main/java
services/coupon-server/src/main/java
```

백엔드도 수정할 수는 있다. 단, 다음 변경은 먼저 분석 보고와 계획을 제출한 뒤 진행한다.

- `OrderServiceImpl` 트랜잭션 경계 변경
- Feign client 계약 변경
- RabbitMQ queue/exchange/retry/DLQ 변경
- scheduler/ShedLock 변경
- application.yml, pom.xml, workflow 변경
- DB schema/entity 구조 변경

## Frontend Rules

- 작업 위치는 `apps/console`.
- API base URL은 환경변수로 둔다.
- mock과 real API adapter를 분리한다.
- 실제 백엔드 endpoint를 확인하지 않고 임의 API를 확정하지 않는다.
- `any` 타입을 남발하지 않는다.
- landing page를 만들지 않는다.
- 첫 화면은 실제 시연 콘솔이어야 한다.
- `node_modules`, `dist`, `.env`는 커밋하지 않는다.

검증:

```powershell
cd apps/console
npm run build
```

## Backend Context You Must Preserve

- MSA 유지.
- monolith로 합치지 않음.
- Eureka는 Kubernetes Service DNS로 대체.
- Config Server는 ConfigMap/Secret으로 대체.
- Gateway는 Ingress로 대체.
- `order-server`가 핵심 리팩토링 대상.
- React 콘솔은 백엔드/인프라 개선을 보여주는 시연 도구.

## Stop And Ask Before

- Secret, `.env`, API key, DB password 수정
- 실제 배포 실행
- GitHub Actions secret/variable 변경
- 원격 DB 또는 외부 결제 API 사용
- 대규모 백엔드 구조 변경
- 여러 서비스에 걸친 breaking API 변경

## Minimum Completion Report

작업이 끝나면 반드시 아래를 보고한다.

```text
변경 파일:
검증 명령:
검증 결과:
백엔드 API 계약 영향:
남은 위험:
다음 작업:
```

