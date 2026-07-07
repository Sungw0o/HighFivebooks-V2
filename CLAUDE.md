# CLAUDE.md - HighFiveBooks V2 Claude Harness

Claude, 작업 전에 반드시 루트 `AGENTS.md`를 먼저 끝까지 읽어라.

이 프로젝트는 단순 프론트 프로젝트도 아니고 monolith 프로젝트도 아니다. `HighFivebooks-V2`는 monorepo 안에 여러 MSA 서비스를 담은 구조다.

```text
apps/storefront           React/Vite user storefront
services/order-server     main backend refactoring target
services/book-server      book/search/stock service
services/member-server    member/point/cart service
services/coupon-server    coupon service
services/payment-server   payment service
k8s                       Kubernetes manifests
docs                      planning notes
```

## Your Primary Role

주 담당은 `apps/storefront` 프론트엔드다.

목표는 기존 Thymeleaf `front_server`를 React/Vite로 대체하는 실제 사용자 쇼핑몰 프론트를 구현하는 것이다.

구현 범위:

- 홈
- 도서 목록/검색/카테고리
- 도서 상세/리뷰
- 장바구니
- 주문서
- 배송지
- 쿠폰/포인트 적용
- 결제
- 주문 완료/실패
- 마이페이지
- 주문 내역/주문 상세/취소/반품

별도 운영 콘솔은 만들지 않는다. 필요한 관리자 기능은 실제 서비스 요구에 맞는 화면으로만 추가한다.

## Backend Reading Is Allowed

필요하면 백엔드 코드를 읽어라.

```text
services/order-server/src/main/java
services/book-server/src/main/java
services/member-server/src/main/java
services/coupon-server/src/main/java
services/payment-server/src/main/java
```

API 계약이 불명확하면 임의로 확정하지 말고 다음을 정리한다.

```text
필요 화면:
필요 API:
예상 request:
예상 response:
현재 백엔드 존재 여부:
백엔드 변경 필요 여부:
```

## Backend Modification Rules

백엔드도 수정할 수는 있다. 단, 아래 변경은 먼저 분석 보고와 계획을 제출한 뒤 진행한다.

- `OrderServiceImpl` 트랜잭션 경계 변경
- Feign client 계약 변경
- RabbitMQ queue/exchange/retry/DLQ 변경
- scheduler/ShedLock 변경
- `application.yml`
- `pom.xml`
- GitHub Actions workflow
- DB schema/entity 구조 변경

## Frontend Rules

- 작업 위치는 `apps/storefront`.
- API base URL은 환경변수로 둔다.
- mock과 real API adapter를 분리한다.
- 실제 백엔드 endpoint를 확인하지 않고 임의 API를 확정하지 않는다.
- `any` 타입을 남발하지 않는다.
- 첫 화면은 실제 쇼핑몰 홈 또는 도서 탐색 화면이어야 한다.
- `node_modules`, `dist`, `.env`는 커밋하지 않는다.

검증:

```powershell
cd apps/storefront
npm run build
```

## Commit Message Rules

커밋 메시지는 깃모지와 한글 요약을 사용한다.

형식:

```text
<gitmoji> <type>: <한글 요약>
```

예시:

```text
✨ feat: React 스토어프론트 홈 화면 추가
🐛 fix: 장바구니 수량 변경 오류 수정
📝 docs: 스토어프론트 API 계약 문서 갱신
💄 style: 홈 화면 섹션 간격 조정
```

규칙:

- 요약은 한글로 쓴다.
- 마침표로 끝내지 않는다.
- 한 커밋은 하나의 의도만 담는다.
- `node_modules`, `dist`, `.env`, Secret/API key는 커밋하지 않는다.
- 작업 후 가능한 경우 `npm run build`를 통과시킨다.

## Backend Context To Preserve

- MSA 유지.
- monolith로 합치지 않음.
- Eureka는 Kubernetes Service DNS로 대체.
- Config Server는 ConfigMap/Secret으로 대체.
- Gateway는 Ingress로 대체.
- `order-server`가 핵심 리팩토링 대상.
- React storefront는 기존 Thymeleaf 프론트를 대체하는 실제 사용자 프론트.

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
