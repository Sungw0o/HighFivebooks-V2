# Order Server Flow and Boundary Map

작성일: 2026-07-07

## 0. 목적

이 문서는 `services/order-server`의 1순위 리팩토링 준비 산출물이다.

목표:

- 주문 흐름에서 어떤 외부 도메인 서버와 통신하는지 고정한다.
- 현재 테스트가 mock 기반인지, 실제 HTTP 계약을 검증하는지 분류한다.
- 다음 작업인 Feign boundary test와 트랜잭션 경계 리팩토링의 우선순위를 정한다.
- Claude가 구현할 `apps/storefront`가 주문/결제 화면을 만들 때 참고할 API 후보를 제공한다.

---

## 1. 외부 Client 목록

### BookClient

대상 서비스: `TEAM5-BOOK-SERVER`

| 메서드 | HTTP | 목적 | 주요 흐름 |
|---|---|---|---|
| `getBooksBulk` | `POST /api/books/bulk` | 주문 상품 책 정보 일괄 조회 | 주문 생성 |
| `holdStockBatch` | `POST /api/books/stock/hold/batch?orderKey=` | 재고 선점 | 주문 생성 |
| `confirmStockDeduction` | `POST /api/books/stock/confirm-deduction?orderKey=` | 결제 성공 후 재고 차감 확정 | 결제 성공 메시지 |
| `restoreStock` | `POST /api/books/stock/restore` + `Idempotency-Key` | 취소/반품 시 재고 복구 | 주문 취소, 반품 승인 |
| `releaseHeldStock` | `POST /api/books/release-stock?orderKey=` | 결제 전 선점 재고 해제 | 주문 생성 실패 보상, 결제대기 취소 |

### MemberClient

대상 서비스: `TEAM5-MEMBER-SERVER`

| 메서드 | HTTP | 목적 | 주요 흐름 |
|---|---|---|---|
| `getMemberGrade` | `GET /api/members/{userId}/grade` | 등급별 적립률 조회 | 주문 생성 |
| `getPointBalance` | `GET /internal/point-transactions/{userId}` | 포인트 잔액 조회 | storefront 후보, 현재 주문 생성에서는 직접 사용 안 함 |
| `createTransaction` | `POST /internal/point-transactions` | 포인트 적립/환불/회수 통합 처리 | 반품 승인 |
| `reservePoint` | `POST /internal/point-transactions/tcc/reserve` | 포인트 사용 예약 | 주문 생성 DB 저장 후 |
| `confirmPoint` | `POST /internal/point-transactions/tcc/confirm` | 포인트 사용 확정 | 결제 성공 메시지 |
| `cancelPoint` | `POST /internal/point-transactions/tcc/cancel` | 포인트 사용 예약 취소 | 주문 생성 실패 보상, 주문 취소 |

### CouponClient

대상 서비스: `TEAM5-COUPON-SERVER`, base path `/api/coupons`

| 메서드 | HTTP | 목적 | 주요 흐름 |
|---|---|---|---|
| `calculateCoupon` | `POST /api/coupons/calculate` + `X-USER-ID` | 쿠폰 할인 금액 계산 | 주문 생성 |
| `useCoupon` | `POST /api/coupons/use` + `X-USER-ID` | 결제 성공 후 쿠폰 사용 확정 | 결제 성공 메시지 |
| `cancelCouponUsage` | `POST /api/coupons/cancel` + `X-USER-ID` | 주문 취소/반품 시 쿠폰 사용 취소 | 주문 취소, 반품 승인 |

### PaymentClient

대상 서비스: `TEAM5-PAYMENT-SERVER`

| 메서드 | HTTP | 목적 | 주요 흐름 |
|---|---|---|---|
| `confirmPayment` | `POST /api/payments/confirm` | 결제 승인 요청 | 현재 order 내부에서는 직접 호출 지점 없음 |
| `cancelPayment` | `POST /api/payments/{paymentKey}/cancel` | 결제 취소 | 배송 준비 중 주문 취소 |

### CartClient

대상 서비스: `TEAM5-MEMBER-SERVER`, context id `cartClient`

| 메서드 | HTTP | 목적 | 주요 흐름 |
|---|---|---|---|
| `clearCart` | `DELETE /api/cart/items` + `X-USER-ID` | 주문 생성 후 장바구니 비우기 | 주문 생성 |
| `clearCartForOrder` | `DELETE /items/immediately` | 즉시 주문 장바구니 정리 후보 | 현재 order 흐름에서는 사용처 없음 |

---

## 2. 주문 생성 흐름

진입점:

```text
POST /api/orders
OrderController.createOrder
  -> OrderServiceImpl.createOrder
```

현재 흐름:

```text
1. orderKey 생성
2. member-server: 회원 등급 조회
   - 실패 시 적립률 0.0으로 진행
3. book-server: 책 정보 일괄 조회
4. order DB: 포장지 조회
5. book-server: 재고 선점
6. order 내부: 배송비 계산
7. coupon-server: 쿠폰 할인 계산
   - 실패 시 OrderException(COUPON_SERVICE_ERROR)
8. OrderCreateService.createOrderInTransaction 호출
   8-1. 비회원 비밀번호 검증/암호화
   8-2. order DB 저장
   8-3. member-server: 포인트 사용 예약
   8-4. order DB 배송 정보 저장
   8-5. member-server/cart: 장바구니 비우기
9. DB 저장 또는 내부 생성 실패 시 보상
   - member-server: 포인트 예약 취소
   - book-server: 재고 선점 해제
```

관찰:

- `OrderServiceImpl`에 클래스 레벨 `@Transactional`이 남아 있다.
- 그래서 `createOrder` 전체가 트랜잭션에 묶이고, 책 조회/재고 선점/쿠폰 계산 같은 외부 I/O가 트랜잭션 안에서 실행될 가능성이 있다.
- `OrderCreateService.createOrderInTransaction`은 DB 저장을 별도 트랜잭션으로 분리하려는 의도지만, 호출자에 클래스 레벨 트랜잭션이 있으면 실제 경계가 흐려진다.
- `OrderCreateService.createOrderInTransaction` 내부에도 `memberClient.reservePoint`, `cartClient.clearCart` 외부 호출이 있다.

다음 리팩토링 후보:

- `OrderServiceImpl` 클래스 레벨 `@Transactional` 제거
- 조회 메서드는 `readOnly = true` 유지
- `createOrder` 외부 I/O는 트랜잭션 밖에서 수행
- DB 저장만 명확한 트랜잭션으로 제한
- `reservePoint`와 `clearCart`를 DB 트랜잭션 밖으로 뺄 수 있는지 검토

---

## 3. 결제 성공 메시지 흐름

진입점:

```text
RabbitMQ payment-success-queue
  -> PaymentMessageListener.handlePaymentSuccess
  -> OrderServiceImpl.processPaymentSuccessMessage
```

현재 흐름:

```text
1. order DB: 주문 조회
2. 상태가 PAYMENT_WAITING이 아니면 무시
3. 결제 금액 검증
4. order 상태 PREPARING 변경
5. paymentKey 저장
6. coupon-server: 쿠폰 사용 확정
7. book-server: 재고 차감 확정
8. member-server: 포인트 사용 확정
```

관찰:

- 금액 비교가 `order.getPaymentAmount() != message.getTotalAmount().intValue()` 형태다.
- `OrderServiceImpl` 클래스 레벨 `@Transactional` 영향으로 쿠폰/재고/포인트 확정 호출이 DB 트랜잭션 안에서 실행될 수 있다.
- `PaymentMessageListener`는 예외 발생 시 그대로 다시 던진다.
- 현재 `RabbitMqConfig`에는 DLQ/Retry 정책이 없다.
- poison message가 무한 재소비될 위험이 있다.
- 같은 메시지 중복 수신 시 멱등성 보장이 명확하지 않다. 단, 상태가 이미 `PREPARING`이면 early return 되므로 일부 중복은 무시된다.

다음 리팩토링 후보:

- 영구 오류와 일시 오류 분리
- 금액 불일치는 제한 재시도 없이 DLQ 격리 후보
- coupon/book/member 장애는 제한 재시도 후 DLQ 후보
- 메시지 idempotency key 또는 order 상태 기반 멱등성 명시
- 결제 성공 후처리의 DB 상태 변경과 외부 확정 호출 순서 재검토

---

## 4. 주문 취소/보상 흐름

진입점:

```text
POST /api/orders/{orderId}/cancel
OrderController.cancelOrder
  -> OrderServiceImpl.cancelOrder
  -> OrderCancelService.cancelOrderTransactional
```

현재 흐름:

```text
1. order DB: 주문 조회
2. member-server: 사용 포인트 예약 취소
3. coupon-server: 쿠폰 사용 취소
4-A. PREPARING 상태
   - payment-server: 결제 취소
   - book-server: 재고 복구
4-B. 그 외 상태
   - book-server: 재고 선점 해제
5. order 상태 CANCELED 변경
```

관찰:

- `OrderCancelService.cancelOrderTransactional`은 `REQUIRES_NEW` 트랜잭션이다.
- 포인트/쿠폰/결제/재고 외부 호출이 트랜잭션 안에서 실행된다.
- `PREPARING` 외 상태는 모두 결제 대기 취소처럼 처리된다. 상태별 정책 재검토가 필요하다.
- 결제 취소 성공 후 재고 복구 실패 같은 부분 실패 처리 전략이 명확하지 않다.

다음 리팩토링 후보:

- 취소 가능 상태 명시
- 외부 호출 순서와 보상 전략 문서화
- 결제 취소/재고 복구/쿠폰 취소 실패 시 정책 분리
- `restoreStock` idempotency key 규칙 표준화

---

## 5. 자동 스케줄러 흐름

진입점:

```text
OrderCancelScheduler.runOrderAutoCancel
  -> OrderServiceImpl.cancelExpiredOrders

OrderCancelScheduler.runDailyOrderStatusUpdate
  -> OrderServiceImpl.autoCompleteDelivery
  -> OrderServiceImpl.autoConfirmPurchase
```

현재 흐름:

```text
cancelExpiredOrders
  - 24시간 지난 PAYMENT_WAITING 주문 조회
  - member-server: 포인트 예약 취소
  - book-server: 재고 선점 해제
  - order 상태 CANCELED

autoCompleteDelivery
  - 3일 지난 DELIVERING 주문 조회
  - DELIVERY_COMPLETED 변경

autoConfirmPurchase
  - 10일 지난 DELIVERY_COMPLETED 주문 조회
  - purchaseConfirm 호출
  - RabbitMQ point-queue로 포인트 적립 메시지 발행
```

관찰:

- `@Scheduled`만 있고 ShedLock 또는 CronJob 분리가 없다.
- K8s에서 `order-server` replica가 2개 이상이면 동일 작업이 중복 실행될 수 있다.
- `autoConfirmPurchase`는 `this.purchaseConfirm` 내부 호출이라 프록시 기반 트랜잭션 적용 관점도 확인이 필요하다.

다음 리팩토링 후보:

- ShedLock Redis/JDBC 중 하나 선택
- 자동 취소와 자동 구매확정에 lock 적용
- K8s CronJob으로 분리할지 비교 문서화

---

## 6. 현재 테스트 분류

현재 `mvnw.cmd test` 기준:

```text
98 tests, 0 failures, 0 errors, 0 skipped
```

### Unit Test

Mockito 기반으로 서비스 내부 분기와 외부 client 호출 여부를 검증한다.

| 테스트 | 보호하는 흐름 |
|---|---|
| `OrderServiceImplTest.CreateOrderTest` | 주문 생성, 책 조회/재고 선점/쿠폰 계산/보상 호출 |
| `OrderServiceImplTest.ProcessPaymentSuccessMessageTest` | 결제 성공 메시지, 금액 검증, 쿠폰 사용 호출 |
| `OrderServiceImplTest.BatchAndStatusTest` | 자동 배송완료, 자동 구매확정, 만료 주문 취소 |
| `OrderCreateServiceTest` | DB 저장, 포인트 예약, 장바구니 삭제 호출 |
| `OrderCancelServiceTest` | 결제대기/배송준비 취소 시 재고 해제/복구와 결제 취소 |
| `AdminOrderServiceImplTest` | 관리자 상태 변경, 반품 승인 시 포인트/쿠폰/재고 호출 |

### Slice Test

`@WebMvcTest`로 controller 요청/응답과 service 위임을 검증한다.

| 테스트 | 보호하는 API |
|---|---|
| `OrderControllerTest` | `/api/orders` 주문 생성/조회/취소/구매확정/포장지/배송정책 |
| `AdminOrderControllerTest` | 관리자 주문 목록/상태 변경/반품 처리 |
| `AdminDeliveryPolicyControllerTest` | 배송 정책 등록/조회/삭제 |
| `AdminWrapperControllerTest` | 포장지 등록/조회/수정/삭제 |
| `GlobalExceptionHandlerTest` | 예외 응답 포맷 |

### Context Test

| 테스트 | 의미 |
|---|---|
| `OrderServerApplicationTests` | Spring context 로딩, 외부 client는 `@MockitoBean`으로 대체 |

### 아직 없는 테스트

| 종류 | 현재 상태 | 필요 이유 |
|---|---|---|
| Feign boundary test | 없음 | 실제 request path/header/body/response contract 검증 필요 |
| Rabbit listener retry/DLQ test | 없음 | poison message 무한 재소비 방지 검증 필요 |
| 멱등성 test | 일부 상태 기반만 있음 | 결제 성공 메시지 중복 수신, 재고/쿠폰/포인트 중복 호출 방지 |
| 실제 MSA integration test | 없음 | order + book/member/coupon/payment 실제 연동 검증 |
| scheduler multi-instance test | 없음 | K8s replica 2개 이상에서 중복 실행 방지 검증 |

---

## 7. 다음 작업 순서

### 1. Feign boundary test 도입

우선 후보:

1. `BookClient`
   - `getBooksBulk`
   - `holdStockBatch`
   - `confirmStockDeduction`
   - `releaseHeldStock`
   - `restoreStock`
2. `MemberClient`
   - `reservePoint`
   - `confirmPoint`
   - `cancelPoint`
3. `CouponClient`
   - `calculateCoupon`
   - `useCoupon`
   - `cancelCouponUsage`
4. `PaymentClient`
   - `cancelPayment`

권장 도구:

- Spring Cloud OpenFeign + WireMock
- 또는 OkHttp MockWebServer

### 2. 트랜잭션 경계 리팩토링

우선 변경 후보:

- `OrderServiceImpl` 클래스 레벨 `@Transactional` 제거
- 메서드별 `@Transactional(readOnly = true)` 유지
- DB 상태 변경 메서드만 명시적 트랜잭션 부여
- `OrderCreateService.createOrderInTransaction` 내부 외부 호출 제거 검토
- `OrderCancelService.cancelOrderTransactional` 내부 외부 호출 분리 검토

### 3. RabbitMQ DLQ/Retry

우선 메시지:

- `payment-success-queue`

분리 기준:

- 금액 불일치: 영구 오류
- 주문 없음: 영구 오류 또는 수동 확인
- coupon/book/member 장애: 일시 오류 가능
- 동일 메시지 중복: 멱등 성공 처리

---

## 8. Storefront 참고 API

Claude가 `apps/storefront`를 만들 때 order 화면에서 우선 확인할 API:

| 화면 | API |
|---|---|
| 주문서 생성 | `POST /api/orders` |
| 결제 정보 조회 | `GET /api/orders/{orderKey}/payments` |
| 주문 상세 | `GET /api/orders/{orderId}` |
| 최근 주문 | `GET /api/orders/recent` + `X-USER-ID` |
| 내 주문 목록 | `GET /api/orders` + `X-USER-ID` |
| 비회원 주문 조회 | `POST /api/orders/guests/search` |
| 포장지 목록 | `GET /api/orders/wrappers` |
| 배송 정책 | `GET /api/orders/policy/current` |
| 주문 취소 | `POST /api/orders/{orderId}/cancel` |
| 구매 확정 | `POST /api/orders/{orderId}/confirm` |
| 반품 가능 여부 | `GET /api/orders/{orderId}/returns/eligibility` |
| 반품 신청 | `POST /api/orders/{orderId}/returns` |

주의:

- book/member/coupon/payment 쪽 storefront API는 각 서비스 controller를 별도로 읽고 확정해야 한다.
- 이 문서의 API 후보는 order-server 기준이다.

