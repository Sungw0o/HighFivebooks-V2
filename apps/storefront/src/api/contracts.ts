/**
 * 백엔드 실제 DTO/스웨거 기준 계약 타입.
 * 근거: docs/STOREFRONT_API_CONTRACT.md (services/* controller/dto/swagger 직접 분석)
 */

// ---------- 공통 페이지 응답 ----------

/** Spring Data Page 원형 (book/member/coupon-server) */
export interface SpringPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

/** order-server CommonPageResponse */
export interface CommonPageResponse<T> {
  data: T[]
  totalElements: number
  totalPages: number
  pageNumber: number
  pageSize: number
  isLast: boolean
}

// ---------- 공통 enum (백엔드 enum 원형) ----------

/** book-server BookSortType */
export type BookSortType = 'POPULAR' | 'NEW' | 'LOW_PRICE' | 'HIGH_PRICE' | 'RATING' | 'REVIEW'

/** member-server Gender */
export type Gender = 'MALE' | 'FEMALE' | 'UNKNOWN'

/** member-server EmailType */
export type EmailType = 'SIGNUP' | 'RESET_PASSWORD' | 'FIND_ID' | 'ACTIVATE'

/** order-server ReturnReason */
export type ReturnReason = 'SIMPLE_CHANGE' | 'PRODUCT_DEFECT' | 'DELIVERY_DELAY' | 'WRONG_DELIVERY'

/** order-server DeliveryStatus (OrderResponse.status) */
export type DeliveryStatus =
  | 'PAYMENT_WAITING'
  | 'PREPARING'
  | 'DELIVERING'
  | 'DELIVERY_COMPLETED'
  | 'PURCHASE_CONFIRMED'
  | 'CANCELED'
  | 'RETURN_REQUESTED'
  | 'RETURN_COMPLETED'

// ---------- book-server ----------

export interface CategoryResponse {
  categoryId: number
  categoryName: string
}

export interface TagResponse {
  tagId: number
  tagName: string
}

export interface BookResponse {
  /** @JsonProperty("id") */
  id: number
  title: string
  author: string
  isbn: string
  price: number
  imageUrl: string | null
  categories: CategoryResponse[] | null
  tags: TagResponse[] | null
  content: string | null
  publisher: string | null
  pubDate: string | null
  avgRating: number | null
  reviewCount: number | null
  aiSummary: string | null
  aiReviewSummary: string | null
  categoryId: number | null
  parentId: number | null
}

export interface BookReviewResponse {
  reviewId: number
  memberId: number
  loginId: string
  content: string
  rating: number
  createdAt: string
  reviewImages: { imageUrl: string }[] | null
  likeCount: number | null
  isLiked: boolean | null
}

/** POST /api/books/{book-id}/reviews — multipart part "request" */
export interface ReviewCreateRequest {
  rating: number
  /** 10~1000자 */
  content: string
}

export interface ReviewCreateResponse {
  reviewId: number
  rating: number
  content: string
}

export interface MyPageReviewResponse {
  reviewId: number
  bookId: number
  bookTitle: string
  createdAt: string
}

// ---------- member-server: auth/account ----------

export interface LoginRequest {
  loginId: string
  password: string
}

/** boolean getter isProfileComplete → JSON 키는 profileComplete */
export interface TokenDto {
  accessToken: string
  refreshToken: string
  profileComplete: boolean
}

export interface MemberCreateRequest {
  loginId: string
  /** 8~20자 영문+숫자+특수문자 */
  password: string
  name: string
  /** 010-0000-0000 */
  phone: string
  email: string
  gender: Gender
  /** ISO date */
  birthDate: string
}

export interface EmailVerifyRequest {
  email: string
  code: string
  type: EmailType
}

export interface MemberResponse {
  name: string
  email: string
  birthDate: string
  phone: string
  status: string
  gradeName: string
}

export interface MemberUpdateRequest {
  name?: string
  email?: string
  phone?: string
  gender?: Gender
  birthDate?: string
}

/** POST /api/accounts/find/password */
export interface PasswordResetRequest {
  loginId: string
  email: string
  authCode: string
  /** 8~20자, 영문+숫자 포함 */
  newPassword: string
}

// ---------- member-server: cart / point / address ----------

export interface CartDetailResponse {
  bookId: number
  title: string
  price: number
  quantity: number
  totalPrice: number
  image: string | null
}

export interface CartListResponse {
  items: CartDetailResponse[]
  totalCartPrice: number
  hasGuestCart: boolean
}

export interface PointBalanceResponse {
  memberId: number
  currentPoint: number
  totalEarnedPoint: number
}

export interface PointHistoryResponse {
  id: number
  amount: number
  description: string
  currentPoint: number
  transactionDate: string
  orderId: number | null
}

export interface AddressResponse {
  addressId: number
  alias: string
  recipient: string
  phone: string
  zipCode: string
  roadAddress: string
  detailAddress: string
  isDefault: boolean
}

export interface AddressListResponse {
  addressList: AddressResponse[]
}

export interface AddressRequest {
  alias: string
  recipient: string
  phone: string
  zipCode: string
  roadAddress: string
  detailAddress: string
  defaultAddress: boolean
}

// ---------- order-server ----------

export interface OrderItemRequest {
  bookId: number
  quantity: number
  wrapperId?: number | null
}

export interface OrderCreateRequest {
  userId?: number | null
  orderPassword?: string | null
  receiverName: string
  receiverAddress: string
  requestDeliveryDate?: string | null
  couponId?: number | null
  usedPoint?: number | null
  orderItems: OrderItemRequest[]
}

export interface OrderCreateResponse {
  orderId: number
  orderKey: string
  orderName: string
  totalAmount: number
}

export interface OrderItemResponse {
  bookTitle: string
  quantity: number
  price: number
}

export interface OrderResponse {
  id: number
  userId: number | null
  orderName: string
  orderDate: string
  status: string
  totalPrice: number
  trackingNumber: string
  items: OrderItemResponse[]
}

export interface OrderValidationInfoResponse {
  orderId: number
  paymentAmount: number
  orderKey: string
  userId: number | null
  usedPoint: number
}

export interface GuestOrderItemResponse {
  title: string
  quantity: number
  price: number
  totalPrice: number
  wrapperName: string | null
}

export interface GuestOrderDetailResponse {
  orderId: number
  orderNumber: string
  orderDate: string
  statusName: string
  receiverName: string
  receiverPhone: string
  address: string
  addressDetail: string
  deliveryRequest: string | null
  wrappingFee: number
  totalAmount: number
  deliveryFee: number
  couponDiscount: number
  pointDiscount: number
  paymentAmount: number
  orderItems: GuestOrderItemResponse[]
}

export interface OrderReturnCheckResponse {
  isEligible: boolean
  estimatedReturnFee: number | null
  estimatedRefundAmount: number | null
  message: string | null
}

export interface OrderReturnRequest {
  returnReason: ReturnReason
  description: string
}

export interface DeliveryPolicyResponse {
  id: number
  standardShippingFee: number
  minOrderAmount: number
  isActive: boolean
  effectiveDate: string
  remoteAreaSurcharge: number
}

export interface WrapperResponse {
  id: number
  name: string
  price: number
}

// ---------- coupon-server ----------

export interface CouponResponseDto {
  id: number
  couponPolicyId: number
  couponName: string
  description: string | null
  issueCount: number | null
  issueStartAt: string | null
  issueEndAt: string | null
  validPeriodDate: number | null
  validEndAt: string | null
  remainingCount: number | null
  status: string
  couponType: string
  policyStatus: string
}

export interface MemberCouponResponseDto {
  id: number
  userId: number
  couponId: number
  couponName: string
  status: string
  issuedAt: string
  usedAt: string | null
  expiredAt: string | null
  orderId: number | null
  discountValue: number
  discountType: string
  condition: string
  daysRemaining: number
}

export interface CouponCalculationResponseDto {
  discountAmount: number
  finalPrice: number
}

// ---------- payment-server ----------

export interface PaymentMethodResponse {
  id: number
  name: string
  alias: string
  isActive: boolean
}

export interface PaymentConfirmRequest {
  paymentKey: string
  orderKey: string
  amount: number
  paymentMethod: string
}

export interface PaymentConfirmResponse {
  paymentId: number
  status: string
  amount: number
  orderId: number
}

// ---------- 관리자 ----------

/** payment-server GET /api/payments/admin/stats/summary */
export interface PaymentStatsResponse {
  totalSalesAmount: number
  totalCancelAmount: number
  netSalesAmount: number
  totalTransactionCount: number
  successCount: number
  cancelCount: number
}

/** payment-server GET /api/payments/admin/stats/daily */
export interface DailySalesResponse {
  date: string
  dailyTotalAmount: number
  dailyCount: number
}

/** book-server 관리자 도서 등록/수정 DTO. 할인율·재고·판매상태 필드 없음(추가 필요, 계약 문서 참조) */
export interface BookInfoDto {
  isbn: string
  title: string
  authors: string[]
  publisher: string
  publishedDate: string
  price: number
  image: string | null
  description: string | null
  categoryId: number | null
}

/** order-server PUT /api/admin/orders/{orderId}/status */
export interface OrderStatusUpdateRequest {
  status: DeliveryStatus
  trackingNumber?: string | null
}
