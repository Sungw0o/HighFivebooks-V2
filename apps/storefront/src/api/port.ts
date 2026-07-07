import type {
  AddressListResponse,
  AddressRequest,
  AddressResponse,
  BookInfoDto,
  BookResponse,
  BookReviewResponse,
  BookSortType,
  CartListResponse,
  CategoryResponse,
  CommonPageResponse,
  CouponCalculationResponseDto,
  CouponResponseDto,
  DailySalesResponse,
  DeliveryPolicyResponse,
  EmailVerifyRequest,
  GuestOrderDetailResponse,
  MemberCouponResponseDto,
  MemberCreateRequest,
  MemberResponse,
  MemberUpdateRequest,
  MyPageReviewResponse,
  OrderCreateRequest,
  OrderCreateResponse,
  OrderReturnCheckResponse,
  OrderReturnRequest,
  OrderResponse,
  OrderStatusUpdateRequest,
  OrderValidationInfoResponse,
  PasswordResetRequest,
  PaymentConfirmRequest,
  PaymentConfirmResponse,
  PaymentMethodResponse,
  PaymentStatsResponse,
  PointBalanceResponse,
  PointHistoryResponse,
  ReviewCreateRequest,
  ReviewCreateResponse,
  SpringPage,
  TokenDto,
  WrapperResponse,
} from './contracts'

/**
 * 프론트가 사용하는 API 포트. mock/real adapter가 동일 시그니처로 구현한다.
 * endpoint 매핑·헤더 계약은 docs/STOREFRONT_API_CONTRACT.md 참조.
 */

export interface BooksApi {
  getBestSellers(size: number): Promise<BookResponse[]>
  getNewBooks(size: number): Promise<BookResponse[]>
  getPopularBooks(size: number): Promise<BookResponse[]>
  getParentCategories(): Promise<CategoryResponse[]>
  getChildCategories(parentId: number): Promise<CategoryResponse[]>
  getBooks(page: number, size: number): Promise<SpringPage<BookResponse>>
  getBooksByCategory(categoryId: number, page: number, size: number): Promise<SpringPage<BookResponse>>
  search(keyword: string, sort: BookSortType, page: number, size: number): Promise<SpringPage<BookResponse>>
  getBook(bookId: number): Promise<BookResponse>
  /** true=찜됨, false=해제됨 */
  toggleLike(bookId: number): Promise<boolean>
  getLikeStatus(bookId: number): Promise<boolean>
  getMyLikedBooks(): Promise<BookResponse[]>
}

export interface ReviewsApi {
  getReviews(bookId: number, page: number, size: number): Promise<SpringPage<BookReviewResponse>>
  /** multipart: request(JSON) + images */
  createReview(bookId: number, request: ReviewCreateRequest, images?: File[]): Promise<ReviewCreateResponse>
  getMyReviews(page: number, size: number): Promise<SpringPage<MyPageReviewResponse>>
}

export interface AuthApi {
  login(loginId: string, password: string): Promise<TokenDto>
  logout(): Promise<void>
  signup(request: MemberCreateRequest): Promise<void>
  checkLoginId(loginId: string): Promise<boolean>
  sendSignupEmailCode(email: string): Promise<void>
  verifyEmailCode(request: EmailVerifyRequest): Promise<string>
  sendFindIdCode(email: string): Promise<void>
  /** 인증 성공 시 loginId 반환 */
  findLoginId(request: EmailVerifyRequest): Promise<string>
  sendPasswordResetCode(email: string): Promise<void>
  resetPassword(request: PasswordResetRequest): Promise<void>
}

export interface MembersApi {
  getMe(): Promise<MemberResponse>
  updateMe(request: MemberUpdateRequest): Promise<MemberResponse>
  getAddresses(): Promise<AddressListResponse>
  getDefaultAddress(): Promise<AddressResponse | null>
  createAddress(request: AddressRequest): Promise<AddressResponse>
  updateAddress(addressId: number, request: AddressRequest): Promise<AddressResponse>
  deleteAddress(addressId: number): Promise<void>
  setDefaultAddress(addressId: number): Promise<AddressResponse>
  getPointBalance(): Promise<PointBalanceResponse>
  getPointHistory(page: number, size: number): Promise<SpringPage<PointHistoryResponse>>
}

export interface CartApi {
  getCart(): Promise<CartListResponse>
  addItem(bookId: number, quantity: number): Promise<void>
  updateItem(bookId: number, quantity: number): Promise<void>
  removeItem(bookId: number): Promise<void>
  clear(): Promise<void>
  mergeGuestCart(): Promise<void>
}

export interface OrdersApi {
  create(request: OrderCreateRequest): Promise<OrderCreateResponse>
  getMyOrders(page: number, size: number): Promise<CommonPageResponse<OrderResponse>>
  getOrder(orderId: number): Promise<OrderResponse>
  cancel(orderId: number): Promise<void>
  confirm(orderId: number): Promise<void>
  getWrappers(): Promise<WrapperResponse[]>
  getDeliveryPolicy(): Promise<DeliveryPolicyResponse>
  getPaymentInfo(orderKey: string): Promise<OrderValidationInfoResponse>
  checkPurchase(memberId: number, bookId: number): Promise<boolean>
  checkReturnEligibility(orderId: number): Promise<OrderReturnCheckResponse>
  requestReturn(orderId: number, request: OrderReturnRequest): Promise<void>
  getGuestOrder(orderId: number, password: string): Promise<GuestOrderDetailResponse>
}

export interface CouponsApi {
  getBookCoupons(bookId: number): Promise<CouponResponseDto[]>
  getIssuableCoupons(page: number, size: number): Promise<SpringPage<CouponResponseDto>>
  issueCoupon(couponId: number): Promise<void>
  getMyCoupons(page: number, size: number): Promise<SpringPage<MemberCouponResponseDto>>
  getUsableCoupons(bookIds?: number[], categoryIds?: number[]): Promise<MemberCouponResponseDto[]>
  calculate(couponId: number, totalOrderPrice: number): Promise<CouponCalculationResponseDto>
}

export interface PaymentsApi {
  getMethods(): Promise<PaymentMethodResponse[]>
  confirm(request: PaymentConfirmRequest): Promise<PaymentConfirmResponse>
}

export interface AdminApi {
  getStatsSummary(): Promise<PaymentStatsResponse>
  getDailySales(startDate?: string, endDate?: string): Promise<DailySalesResponse[]>
  getBooks(page: number, size: number): Promise<SpringPage<BookResponse>>
  createBook(dto: BookInfoDto): Promise<BookInfoDto>
  updateBook(bookId: number, dto: BookInfoDto): Promise<BookResponse>
  deleteBook(bookId: number): Promise<void>
  /** 알라딘 ISBN 조회로 도서 정보 자동 채움 */
  searchBookByIsbn(isbn: string): Promise<BookInfoDto>
  getOrders(page: number, size: number, status?: string): Promise<CommonPageResponse<OrderResponse>>
  updateOrderStatus(orderId: number, request: OrderStatusUpdateRequest): Promise<void>
}

export interface StorefrontApi {
  books: BooksApi
  reviews: ReviewsApi
  auth: AuthApi
  members: MembersApi
  cart: CartApi
  orders: OrdersApi
  coupons: CouponsApi
  payments: PaymentsApi
  admin: AdminApi
}
