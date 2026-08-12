# HighFiveBooks TCC failure matrix

작성일: 2026-08-12

## 상태 기준

- **검증 완료**: 자동 테스트 또는 저장된 원시 결과로 재현했다.
- **부분 검증**: 단위 테스트·구성·과거 실행 기록은 있으나 실서비스 간 통합 또는 장애 복구까지 확인하지 못했다.
- **미검증**: 설계나 예상 동작만 있다.

## 주문 생성(Try)과 보상

| 실패 지점 | 이미 반영된 상태 | 현재 동작 | 기대 보상 | 검증 | 남은 위험 |
|---|---|---|---|---|---|
| 도서 정보 조회/재고 선점 전 | 없음 | 주문 생성 중단 | 없음 | 부분 검증 | Feign 실 timeout 미주입 |
| 재고 선점 후 주문 DB 저장 실패 | `StockHeld` | 포인트 Cancel 시도 후 재고 release | 포인트 예약 취소, 선점 해제 | 검증 완료 | 포인트 Cancel에 `orderId=0`을 사용해 추적성이 낮음 |
| 포인트 Cancel 실패 | 재고 선점, 포인트 예약 가능 | 실패를 기록하고 재고 release 계속 시도 | 실패 작업 재시도/운영 큐 | 검증 완료 | 실패 보상 영속화·재시도 없음 |
| 재고 release timeout | 재고 선점 유지 가능 | timeout을 기록하고 원본 예외 반환 | 재고 release 재시도 | 검증 완료 | 수동 복구 전까지 hold 잔존 가능 |
| 쿠폰 할인 계산 실패 | 재고 선점 | `COUPON_SERVICE_ERROR` 반환 | 재고 release | 부분 검증 | 호출 순서에 따라 불필요한 hold 시간 발생 |

## 결제 성공(Confirm)

| 실패/중복 지점 | 현재 동작 | 멱등 기준 | 검증 | 남은 위험 |
|---|---|---|---|---|
| 동일 결제 성공 메시지 재수신 | 주문이 `PAYMENT_WAITING`이 아니면 종료 | 주문 상태 | 검증 완료 | 두 consumer가 동시에 WAITING을 읽는 경쟁 조건은 미검증 |
| 쿠폰 Confirm 후 재고 Confirm 실패 | listener 예외 → retry/DLQ | 쿠폰 서비스 자체 멱등성 필요 | 부분 검증 | 도메인별 확정 완료 상태를 주문 서버가 기록하지 않음 |
| 재고 Confirm 중복 | `CONFIRM-{orderKey}` 존재 시 종료 | `StockIdempotencyRecord` unique key | 검증 완료 | 실제 DB 동시 삽입 경쟁은 미검증 |
| 포인트 Confirm 중복 | 이미 확정된 거래면 성공 처리 | 주문별 포인트 거래 상태 | 검증 완료 | 서비스 간 동시 호출 통합 테스트 미실행 |
| 상태 저장 직전 장애 | 외부 Confirm은 끝났지만 주문은 WAITING일 수 있음 | 외부 서비스 멱등성에 의존 | 부분 검증 | 단계별 완료 로그/outbox 없음 |
| poison message | listener retry 후 reject | RabbitMQ DLX/DLQ | 부분 검증 | 자동 실브로커 테스트는 Docker 중단으로 이번 세션 skip |

## 주문 취소(Cancel)

| 실패/중복 지점 | 현재 동작 | 검증 | 남은 위험 |
|---|---|---|---|
| 동일 Cancel 재호출 | 주문이 `CANCELED`이면 외부 호출 없이 종료 | 검증 완료 | 동시 Cancel 경쟁은 미검증 |
| 포인트 Cancel 실패 | 즉시 예외, 쿠폰·재고·상태 변경 중단 | 검증 완료 | 부분 보상 상태를 저장하거나 재시도하지 않음 |
| 쿠폰 Cancel 실패/timeout | 즉시 예외, 결제·재고·상태 변경 중단 | 부분 검증 | 재호출 시 앞선 성공 보상을 다시 호출할 수 있음 |
| 결제 Cancel timeout | 재고 복구와 상태 변경 중단 | 검증 완료 | Toss가 실제 취소했지만 응답만 유실된 경우 판별 필요 |
| 재고 Restore 중복 | 동일 idempotency key면 종료 | 검증 완료 | 실제 DB 동시성은 미검증 |
| 상태 변경 실패 | 외부 보상 완료 후 주문 상태가 남을 수 있음 | 미검증 | 보상 완료 체크포인트/재처리 작업 필요 |

## 테스트 근거

- `OrderServiceImplTest`: 중복 Confirm, 주문 생성 실패 보상, 포인트/재고 보상 실패·timeout
- `OrderCancelServiceTest`: 중복 Cancel, 포인트 Cancel 실패, 결제 Cancel timeout
- `StockServiceImplTest`: 중복 재고 Confirm/Restore 시 수량 1회 변경
- `PointServiceImplTest`: 포인트 Confirm/Cancel 멱등 분기
- `RabbitMqDlqIntegrationTest`: 실제 브로커 reject → DLQ 이동(환경변수 기반 조건부 실행)

## 결론

현재 구조는 단일 요청의 순차 실패와 동일 요청 재호출에는 방어 근거가 있다. 그러나 단계별 보상 완료 상태를 영속화하지 않아 동시 요청, 응답 유실, 보상 중 프로세스 종료까지 포함한 완전한 TCC coordinator로 보기는 어렵다. 포트폴리오에는 **“서비스별 멱등 키와 실패 재현 테스트를 적용했고, durable compensation retry는 후속 과제”**로 표현한다.
