# Order Server Flow and Boundary Map

작성일: 2026-07-07
최종 갱신: 2026-07-08

## 1. 목적

이 문서는 `services/order-server`가 주문 흐름에서 어떤 외부 도메인 서버와 통신하는지 정리한다. 목적은 세 가지다.

- Feign boundary test의 기준을 고정한다.
- 트랜잭션 경계 리팩토링의 위험 지점을 식별한다.
- React storefront가 주문/결제 화면을 구현할 때 참고할 API 후보를 제공한다.

## 2. 외부 Client 목록

### BookClient

대상 서비스: `TEAM5-BOOK-SERVER`

| 메서드 | HTTP | 목적 | 주요 흐름 |
|---|---|---|---|
| `getBooksBulk` | `POST /api/books/bulk` | 주문 상품 도서 정보 일괄 조회 | 주문 생성 |
| `holdStockBatch` | `POST /api/books/stock/hold/batch?orderKey=` | 재고 선점 | 주문 생성 |
| `confirmStockDeduction` | `POST /api/books/stock/confirm-deduction?orderKey=` | 결제 성공 후 재고 차감 확정 | 결제 성공 메시지 |
| `restoreStock` | `POST /api/books/stock/restore` + `Idempotency-Key` | 취소/반품 후 재고 복구 | 주문 취소, 반품 승인 |
| `releaseHeldStock` | `POST /api/books/release-stock?orderKey=` | 결제 전 선점 재고 해제 | 주문 생성 실패 보상, 결제 대기 취소 |

### MemberClient

대상 서비스: `TEAM5-MEMBER-SERVER`

| 메서드 | HTTP | 목적 | 주요 흐름 |
|---|---|---|---|
| `getMemberGrade` | `GET /api/members/{userId}/grade` | 회원 등급별 적립률 조회 | 주문 생성 |
| `getPointBalance` | `GET /internal/point-transactions/{userId}` | 포인트 잔액 조회 | storefront 후보 |
| `reservePoint` | `POST /internal/point-transactions/tcc/reserve` | 포인트 사용 예약 | 주문 생성 |
| `confirmPoint` | `POST /internal/point-transactions/tcc/confirm` | 포인트 사용 확정 | 결제 성공 메시지 |
| `cancelPoint` | `POST /internal/point-transactions/tcc/cancel` | 포인트 예약 취소 | 주문 생성 실패 보상, 주문 취소 |
| `createTransaction` | `POST /internal/point-transactions` | 포인트 적립/환불/회수 통합 처리 | 구매 확정, 반품 |

### CouponClient

대상 서비스: `TEAM5-COUPON-SERVER`, base path `/api/coupons`

| 메서드 | HTTP | 목적 | 주요 흐름 |
|---|---|---|---|
| `calculateCoupon` | `POST /api/coupons/calculate` + `X-USER-ID` | 쿠폰 할인 금액 계산 | 주문 생성 |
| `useCoupon` | `POST /api/coupons/use` + `X-USER-ID` | 쿠폰 사용 확정 | 결제 성공 메시지 |
| `cancelCouponUsage` | `POST /api/coupons/cancel` + `X-USER-ID` | 쿠폰 사용 취소 | 주문 취소, 반품 승인 |

### PaymentClient

대상 서비스: `TEAM5-PAYMENT-SERVER`

| 메서드 | HTTP | 목적 | 주요 흐름 |
|---|---|---|---|
| `confirmPayment` | `POST /api/payments/confirm` | 결제 승인 요청 | 결제 플로우 후보 |
| `cancelPayment` | `POST /api/payments/{paymentKey}/cancel` | 결제 취소 | 배송 준비 중 주문 취소 |

### CartClient

대상 서비스: `TEAM5-MEMBER-SERVER`, context id `cartClient`

| 메서드 | HTTP | 목적 | 주요 흐름 |
|---|---|---|---|
| `clearCart` | `DELETE /api/cart/items` + `X-USER-ID` | 주문 생성 후 장바구니 비우기 | 주문 생성 |
| `clearCartForOrder` | `DELETE /items/immediately` | 즉시 주문 장바구니 정리 후보 | 현재 주문 흐름에서는 직접 사용하지 않음 |

## 3. 주문 생성 흐름

```text
POST /api/orders
  -> OrderController.createOrder
  -> OrderServiceImpl.createOrder
     1. orderKey 생성
     2. member-server: 회원 등급 조회
     3. book-server: 도서 정보 일괄 조회
     4. book-server: 재고 선점
     5. order-server: 배송비 계산
     6. coupon-server: 쿠폰 할인 계산
     7. OrderCreateService.createOrderInTransaction
        - 주문 DB 저장
        - 포인트 사용 예약
        - 배송 정보 저장
        - 장바구니 비우기
     8. 실패 시 보상
        - 포인트 예약 취소
        - 재고 선점 해제
```

리팩토링 결과:

- 주문 오케스트레이션 전체에 클래스 레벨 트랜잭션을 걸지 않는다.
- DB 저장은 `OrderCreateService`에서 명시적 트랜잭션으로 제한한다.
- 외부 Feign 호출은 가능한 한 넓은 DB 트랜잭션 밖에서 수행한다.

## 4. 결제 성공 메시지 흐름

```text
RabbitMQ payment-success-queue
  -> PaymentMessageListener.handlePaymentSuccess
  -> OrderServiceImpl.processPaymentSuccessMessage
     1. 주문과 주문 상품 조회
     2. 주문 상태가 PAYMENT_WAITING이 아니면 무시
     3. 결제 금액 검증
     4. coupon-server: 쿠폰 사용 확정
     5. book-server: 재고 차감 확정
     6. member-server: 포인트 사용 확정
     7. OrderStatusMutationService.markPaymentSuccess
```

리팩토링 결과:

- 외부 확정 호출 후 DB 상태 변경 전담 서비스가 주문 상태를 변경한다.
- 리스너 예외는 삼키지 않고 Rabbit retry/DLQ 정책으로 전파한다.
- 금액 불일치 같은 poison message는 제한 재시도 후 DLQ로 격리된다.

## 5. 주문 취소 흐름

```text
POST /api/orders/{orderId}/cancel
  -> OrderCancelService.cancelOrderTransactional
     1. 주문과 주문 상품 조회
     2. 상태별 외부 보상 호출
        - point cancel/refund
        - coupon cancel
        - payment cancel
        - stock restore/release
     3. OrderStatusMutationService.markCanceled
```

리팩토링 결과:

- 외부 보상 호출을 긴 DB 트랜잭션 안에 묶지 않는다.
- 상태 변경은 전담 mutation service에서 처리한다.

## 6. 자동 배치 흐름

### 결제 대기 만료 취소

```text
OrderCancelScheduler.runOrderAutoCancel
  -> OrderServiceImpl.cancelExpiredOrders
  -> book-server: 선점 재고 해제
  -> member-server: 포인트 예약 취소
  -> OrderStatusMutationService.markCanceled
```

### 일일 주문 상태 변경

```text
OrderCancelScheduler.runDailyOrderStatusUpdate
  -> OrderServiceImpl.autoCompleteDelivery
  -> OrderServiceImpl.autoConfirmPurchase
  -> RabbitMQ point-queue: 포인트 적립 메시지 발행
```

리팩토링 결과:

- 두 스케줄러 메서드 모두 Redis ShedLock을 사용한다.
- K8s에서 order-server replica가 2개 이상이어도 같은 작업이 중복 실행되지 않도록 방어한다.

## 7. 테스트 분류

현재 order-server 전체 테스트:

```text
126 tests, 0 failures, 0 errors, 0 skipped
```

주요 테스트:

| 테스트 | 보호하는 경계 |
|---|---|
| `FeignClientBoundaryTest` | 실제 Feign path/header/body, 비재시도 경계 |
| `OrderTransactionBoundaryTest` | 오케스트레이션과 DB mutation 트랜잭션 분리 |
| `RabbitMqConfigTest` | payment-success queue DLQ와 retry 설정 |
| `PaymentMessageListenerTest` | 리스너 예외 전파와 retry/DLQ 진입 |
| `SchedulerLockConfigTest` | ShedLock 활성화 |
| `OrderCancelSchedulerLockTest` | 스케줄러별 lock 이름과 timeout |

## 8. Storefront 주문 API 후보

React storefront가 주문 화면에서 우선 확인할 API:

```text
POST   /api/orders
GET    /api/orders/{orderId}
GET    /api/orders/recent
POST   /api/orders/{orderId}/cancel
POST   /api/orders/guests/search
POST   /api/payments/confirm
GET    /api/members/me/addresses
GET    /api/members/me/coupons
GET    /api/cart
DELETE /api/cart/items
```

주의:

- 위 목록은 storefront 구현 후보이며, 최종 확정 전 각 서비스 controller/dto를 다시 확인한다.
- book/member/coupon/payment 쪽 storefront API는 각 서비스 문서를 별도로 읽고 확정한다.
