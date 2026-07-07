# Storefront API Contract (apps/storefront)

> 2026-07-07 기준. services/* controller/dto를 직접 읽고 정리했다.
> 프론트는 이 문서의 타입을 기준으로 mock adapter를 임시 구현하고, real adapter는 동일 타입으로 연결한다.
> 인증: 백엔드는 `X-USER-ID` 헤더 기반(게이트웨이/Ingress가 JWT→헤더 변환 전제). 프론트는 `TokenDto.accessToken` 보관.

## 화면별 계약

### 홈 `/`
| 용도 | API | 상태 |
|---|---|---|
| 이달의 책(히어로) | 전용 API 없음 → `GET /api/books/best-seller?size=1` 대체 | 대체 |
| Marquee/추천 타일 | `GET /api/books/new?size=`, `GET /api/books/popular?size=` (book) | 있음 |
| 카테고리 5개 | `GET /api/categories/parent` → `CategoryResponse{categoryId, categoryName}` | 있음 |
| Picks 컬렉션 3종 | 컬렉션/큐레이션 API 없음 → 프론트 정적 큐레이션(태그 `GET /api/tags` 활용 가능) | 없음 |

### 책 목록/검색 `/books`
| 용도 | API | 상태 |
|---|---|---|
| 전체 목록(페이지) | `GET /api/books?page&size&sort` → `Page<BookResponse>` (Spring Page 원형) | 있음 |
| 검색 | `GET /api/search?keyword&sort=POPULAR|...&page&size` (BookSortType) | 있음 |
| 카테고리별 | `GET /api/categories/{categoryId}/books` → `Page<BookResponse>` | 있음 |
| 하위 카테고리 | `GET /api/categories/{parentId}/child` | 있음 |
| 가격대/출판사 필터 | 서버 파라미터 없음 | **추가 필요** (임시: 페이지 내 클라이언트 필터) |

`BookResponse`: `{ id, title, author, isbn, price, imageUrl, categories[], tags[], content, publisher, pubDate, avgRating, reviewCount, aiSummary, aiReviewSummary, categoryId, parentId }`
※ 정가/할인율 필드 없음(단일 `price`) → 디자인의 취소선 정가+할인% 표시는 **추가 필요**(`originalPrice`, `discountRate`). 임시: 할인 미표시 또는 mock 전용.

### 책 상세 `/books/:id`
| 용도 | API | 상태 |
|---|---|---|
| 상세 | `GET /api/books/{id}` | 있음 |
| 리뷰 목록 | `GET /api/books/{book-id}/reviews?page&size` → `Page<BookReviewResponse>` | 있음 |
| 찜 토글/상태 | `POST /api/books/{bookId}/likes`, `GET /api/books/{bookId}/likes` (X-USER-ID) | 있음 |
| 도서 쿠폰 | `GET /api/coupons/books/{book-id}` (coupon) | 있음 |

`BookReviewResponse`: `{ reviewId, memberId, loginId, content, rating, createdAt, reviewImages[], likeCount, isLiked }`

### 장바구니 `/cart` (member-server, 게스트 쿠키 지원)
| 용도 | API | 상태 |
|---|---|---|
| 조회 | `GET /api/cart` → `CartListResponse{ items: CartDetailResponse[], totalCartPrice, hasGuestCart }` | 있음 |
| 담기 | `POST /api/cart/items` `{bookId, quantity(1~100)}` | 있음 |
| 수량 변경 | `PUT /api/cart/items` `{bookId, quantity}` | 있음 |
| 항목/전체 삭제 | `DELETE /api/cart/items/{bookId}`, `DELETE /api/cart/items` | 있음 |
| 게스트 병합 | `POST /api/cart/merge`, `DELETE /api/cart/guest` | 있음 |

`CartDetailResponse`: `{ bookId, title, price, quantity, totalPrice, image }`

### 주문/결제 `/checkout`
| 용도 | API | 상태 |
|---|---|---|
| 주문 생성 | `POST /api/orders` `OrderCreateRequest` → `OrderCreateResponse{orderId, orderKey, orderName, totalAmount}` | 있음 |
| 배송 정책 | `GET /api/orders/policy/current` → `{standardShippingFee, minOrderAmount, remoteAreaSurcharge…}` | 있음 |
| 포장지 | `GET /api/orders/wrappers` | 있음 |
| 배송지 | `GET /api/address`, `GET /api/address/default`, `POST /api/address` (member) | 있음 |
| 사용 가능 쿠폰 | `GET /api/coupons/members/order` (X-USER-ID) | 있음 |
| 쿠폰 계산 | `POST /api/coupons/calculate` `{couponId, totalOrderPrice}` → `{discountAmount, finalPrice}` | 있음 |
| 포인트 잔액 | `GET /api/points/balance` → `{memberId, currentPoint, totalEarnedPoint}` | 있음 |
| 결제 수단 | `GET /api/payments/methods` → `{id, name, alias, isActive}[]` | 있음 |
| 결제 검증 정보 | `GET /api/orders/{orderKey}/payments` | 있음 |
| 결제 승인 | `POST /api/payments/confirm` `{paymentKey, orderKey, amount, paymentMethod}` → `{paymentId, status, amount, orderId}` | 있음 |

`OrderCreateRequest`: `{ userId?, orderPassword?(비회원), receiverName, receiverAddress, requestDeliveryDate?, couponId?, usedPoint?, orderItems: [{bookId, quantity, wrapperId?}] }`

### 주문 완료 `/order/complete`
`OrderCreateResponse` + `PaymentConfirmResponse` + `GET /api/orders/{orderId}` → `OrderResponse`. 있음.

### 마이페이지 `/my`
| 용도 | API | 상태 |
|---|---|---|
| 내 정보 | `GET /api/members/me` → `{name, email, birthDate, phone, status, gradeName}` | 있음 |
| 주문 내역 | `GET /api/orders` → `CommonPageResponse<OrderResponse>` `{data[], totalElements, totalPages, pageNumber, pageSize, isLast}` | 있음 |
| 주문 취소/확정/반품 | `POST /api/orders/{orderId}/cancel`, `POST /api/orders/{orderId}/confirm`, `GET .../returns/eligibility`, `POST .../returns` | 있음 |
| 찜한 책 | `GET /api/my-page/likes` (book) | 있음 |
| 내 리뷰 | `GET /api/books/members/me/reviews` | 있음 |
| 포인트 | `GET /api/points/balance`, `GET /api/points/history` | 있음 |
| 보유 쿠폰 | `GET /api/coupons/members` | 있음 |
| 통계 카드(진행중/찜/리뷰 수) | 집계 API 없음 → 목록 API totalElements로 프론트 계산 | 대체 |

`OrderResponse`: `{ id, userId, orderName, orderDate, status(DeliveryStatus), totalPrice, trackingNumber, items[] }`

### 리뷰 작성 `/review/new`
| 용도 | API | 상태 |
|---|---|---|
| 등록 | `POST /api/books/{book-id}/reviews` — **multipart**: `request`(JSON: rating, content) + `images[]` | 있음 |
| 구매 여부 확인 | `GET /api/orders/check-purchase?memberId&bookId` | 있음 |
| 수정 | `POST /api/books/{book-id}/reviews/{review-id}` | 있음 |

※ 리뷰 **제목 필드 없음**(rating+content만) → 디자인의 제목 입력은 **추가 필요** 또는 UI에서 제외.

### 로그인/회원가입 `/login`, `/signup`
| 용도 | API | 상태 |
|---|---|---|
| 로그인 | `POST /api/auth/login` `{loginId, password}` → `TokenDto{accessToken, refreshToken, isProfileComplete}` | 있음 (※ 이메일이 아닌 **loginId** 기반 — 디자인 카피 조정) |
| 소셜 | `POST /api/auth/login/{provider}` | 있음 |
| 토큰 재발급/로그아웃 | `POST /api/auth/reissue`, `POST /api/auth/logout` | 있음 |
| 가입 | `POST /api/accounts/signup` `{loginId, password(8~20 영문+숫자+특수), name, phone(010-xxxx-xxxx), email, gender, birthDate}` | 있음 |
| ID 중복 | `GET /api/accounts/check-id?loginId` | 있음 |
| 이메일 인증 | `POST /api/emails/signup`, `POST /api/emails/verify` | 있음 |
| **휴대폰 인증** | 없음(이메일 인증만) | **없음** → 디자인의 휴대폰 인증 버튼은 이메일 인증으로 대체 또는 추가 필요 |

### 관리자 `/admin/*`
| 용도 | API | 상태 |
|---|---|---|
| 매출 통계 | `GET /api/payments/admin/stats/summary`, `GET .../daily` | 있음 |
| 재고 부족 리스트 | 전용 API 없음 | **추가 필요** (임시: admin books 목록에서 파생) |
| 도서 CRUD | `GET/POST /api/admin/books`, `GET/PUT/DELETE /api/admin/books/{id}`, `GET /api/admin/books/search-api?isbn` | 있음 |
| 표지 업로드 | 파일 업로드 API 미확인(imageUrl 문자열만) | **확인 필요** |
| 주문 관리 | `GET /api/admin/orders`, `PUT /api/admin/orders/{orderId}/status` | 있음 |
| 쿠폰 관리 | `/api/coupons/admin/*` | 있음 |

## 요약: 없거나 추가가 필요한 것
1. 정가/할인율(`originalPrice`/`discountRate`) — BookResponse에 없음
2. 목록 가격대/출판사 서버 필터
3. 리뷰 제목 필드
4. 휴대폰 인증 API (이메일 인증만 존재)
5. 큐레이션(이달의 책/Picks) API
6. 마이페이지 통계 집계 API
7. 관리자 재고 부족 리스트 API
8. 도서 표지 파일 업로드 API (확인 필요)

## 스웨거 추가 확인 사항 (2026-07-08, axios adapter 반영 완료)
- **토큰 재발급**: `POST /api/auth/reissue` — body 없음, **`X-Refresh-Token` 헤더**로 전달
- **로그아웃**: `X-User-ID` + `Authorization: Bearer` 둘 다 필요
- **TokenDto JSON 키**: `profileComplete` (isProfileComplete 아님 — boolean getter 직렬화)
- **비회원 장바구니**: 서버가 `guestCookie` 쿠키를 Set-Cookie로 발급 → axios `withCredentials: true` 필수
- **X-USER-ID**: 각 서비스가 헤더를 그대로 신뢰(서비스 내 JWT 필터 없음, 구 gateway 계약).
  임시로 프론트 인터셉터가 JWT sub를 디코드해 직접 주입. **K8s 전환 시 Ingress/BFF에서 JWT 검증 후 주입하는 구조 확정 필요** (보안상 프론트 주입은 데모 한정)
- **BookSortType 실제 값**: `POPULAR | NEW | LOW_PRICE | HIGH_PRICE | RATING | REVIEW`
- **Gender**: `MALE | FEMALE | UNKNOWN`, **EmailType**: `SIGNUP | RESET_PASSWORD | FIND_ID | ACTIVATE`, **ReturnReason**: `SIMPLE_CHANGE | PRODUCT_DEFECT | DELIVERY_DELAY | WRONG_DELIVERY`
- **OrderItemResponse 실제 필드**: `{ bookTitle, quantity, price }`
- **사용 가능 쿠폰**: `GET /api/coupons/members/order?bookIds=&categoryIds=` (선택 파라미터)
- **리뷰 등록**: multipart `request` part는 `application/json` Blob, `content`는 10~1000자

## 프론트 전제
- Base URL: `VITE_API_BASE_URL` 환경변수 (Ingress 단일 진입 가정, 경로 프리픽스로 서비스 라우팅)
- 페이지 응답 2종 유의: Spring `Page<T>`(book/coupon/member) vs `CommonPageResponse<T>`(order)
- mock adapter는 위 타입과 100% 동일한 시그니처로 구현
