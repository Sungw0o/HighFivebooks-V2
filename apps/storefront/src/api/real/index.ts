import { http } from '../http'
import { tokenStore } from '../tokenStore'
import type {
  AddressListResponse,
  AddressResponse,
  BookInfoDto,
  BookResponse,
  BookReviewResponse,
  CartListResponse,
  CategoryResponse,
  CommonPageResponse,
  CouponCalculationResponseDto,
  CouponResponseDto,
  DailySalesResponse,
  DeliveryPolicyResponse,
  GuestOrderDetailResponse,
  MemberCouponResponseDto,
  MemberResponse,
  MyPageReviewResponse,
  OrderCreateResponse,
  OrderReturnCheckResponse,
  OrderResponse,
  OrderValidationInfoResponse,
  PaymentConfirmResponse,
  PaymentMethodResponse,
  PaymentStatsResponse,
  PointBalanceResponse,
  PointHistoryResponse,
  ReviewCreateResponse,
  SpringPage,
  TokenDto,
  WrapperResponse,
} from '../contracts'
import type { StorefrontApi } from '../port'

/**
 * 실제 백엔드 계약 매핑 (docs/STOREFRONT_API_CONTRACT.md).
 * Authorization/X-USER-ID/guestCookie는 http.ts 인터셉터가 처리한다.
 */
export const realApi: StorefrontApi = {
  books: {
    getBestSellers: async (size) =>
      (await http.get<BookResponse[]>('/api/books/best-seller', { params: { size } })).data,
    getNewBooks: async (size) => (await http.get<BookResponse[]>('/api/books/new', { params: { size } })).data,
    getPopularBooks: async (size) =>
      (await http.get<BookResponse[]>('/api/books/popular', { params: { size } })).data,
    getParentCategories: async () => (await http.get<CategoryResponse[]>('/api/categories/parent')).data,
    getChildCategories: async (parentId) =>
      (await http.get<CategoryResponse[]>(`/api/categories/${parentId}/child`)).data,
    getBooks: async (page, size) =>
      (await http.get<SpringPage<BookResponse>>('/api/books', { params: { page, size } })).data,
    getBooksByCategory: async (categoryId, page, size) =>
      (await http.get<SpringPage<BookResponse>>(`/api/categories/${categoryId}/books`, { params: { page, size } }))
        .data,
    search: async (keyword, sort, page, size) =>
      (await http.get<SpringPage<BookResponse>>('/api/search', { params: { keyword, sort, page, size } })).data,
    getBook: async (bookId) => (await http.get<BookResponse>(`/api/books/${bookId}`)).data,
    toggleLike: async (bookId) => (await http.post<boolean>(`/api/books/${bookId}/likes`)).data,
    getLikeStatus: async (bookId) => (await http.get<boolean>(`/api/books/${bookId}/likes`)).data,
    getMyLikedBooks: async () => (await http.get<BookResponse[]>('/api/my-page/likes')).data,
  },

  reviews: {
    getReviews: async (bookId, page, size) =>
      (await http.get<SpringPage<BookReviewResponse>>(`/api/books/${bookId}/reviews`, { params: { page, size } }))
        .data,
    createReview: async (bookId, request, images) => {
      const form = new FormData()
      form.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }))
      images?.forEach((file) => form.append('images', file))
      return (
        await http.post<ReviewCreateResponse>(`/api/books/${bookId}/reviews`, form, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
      ).data
    },
    getMyReviews: async (page, size) =>
      (await http.get<SpringPage<MyPageReviewResponse>>('/api/books/members/me/reviews', { params: { page, size } }))
        .data,
  },

  auth: {
    login: async (loginId, password) => {
      const dto = (await http.post<TokenDto>('/api/auth/login', { loginId, password })).data
      tokenStore.set({ accessToken: dto.accessToken, refreshToken: dto.refreshToken })
      return dto
    },
    logout: async () => {
      try {
        await http.post('/api/auth/logout')
      } finally {
        tokenStore.clear()
      }
    },
    signup: async (request) => {
      await http.post('/api/accounts/signup', request)
    },
    checkLoginId: async (loginId) =>
      (await http.get<boolean>('/api/accounts/check-id', { params: { loginId } })).data,
    sendSignupEmailCode: async (email) => {
      await http.post('/api/emails/signup', { email })
    },
    verifyEmailCode: async (request) => (await http.post<string>('/api/emails/verify', request)).data,
    sendFindIdCode: async (email) => {
      await http.post('/api/emails/find-id', { email })
    },
    findLoginId: async (request) => (await http.post<string>('/api/accounts/find/id/verify', request)).data,
    sendPasswordResetCode: async (email) => {
      await http.post('/api/emails/password-reset', { email })
    },
    resetPassword: async (request) => {
      await http.post('/api/accounts/find/password', request)
    },
  },

  members: {
    getMe: async () => (await http.get<MemberResponse>('/api/members/me')).data,
    updateMe: async (request) => (await http.put<MemberResponse>('/api/members/me', request)).data,
    getAddresses: async () => (await http.get<AddressListResponse>('/api/address')).data,
    getDefaultAddress: async () => {
      try {
        return (await http.get<AddressResponse>('/api/address/default')).data
      } catch {
        return null
      }
    },
    createAddress: async (request) => (await http.post<AddressResponse>('/api/address', request)).data,
    updateAddress: async (addressId, request) =>
      (await http.put<AddressResponse>(`/api/address/${addressId}`, request)).data,
    deleteAddress: async (addressId) => {
      await http.delete(`/api/address/${addressId}`)
    },
    getPointBalance: async () => (await http.get<PointBalanceResponse>('/api/points/balance')).data,
    getPointHistory: async (page, size) =>
      (await http.get<SpringPage<PointHistoryResponse>>('/api/points/history', { params: { page, size } })).data,
  },

  cart: {
    getCart: async () => (await http.get<CartListResponse>('/api/cart')).data,
    addItem: async (bookId, quantity) => {
      await http.post('/api/cart/items', { bookId, quantity })
    },
    updateItem: async (bookId, quantity) => {
      await http.put('/api/cart/items', { bookId, quantity })
    },
    removeItem: async (bookId) => {
      await http.delete(`/api/cart/items/${bookId}`)
    },
    clear: async () => {
      await http.delete('/api/cart/items')
    },
    mergeGuestCart: async () => {
      await http.post('/api/cart/merge')
    },
  },

  orders: {
    create: async (request) => (await http.post<OrderCreateResponse>('/api/orders', request)).data,
    getMyOrders: async (page, size) =>
      (await http.get<CommonPageResponse<OrderResponse>>('/api/orders', { params: { page, size } })).data,
    getOrder: async (orderId) => (await http.get<OrderResponse>(`/api/orders/${orderId}`)).data,
    cancel: async (orderId) => {
      await http.post(`/api/orders/${orderId}/cancel`)
    },
    confirm: async (orderId) => {
      await http.post(`/api/orders/${orderId}/confirm`)
    },
    getWrappers: async () => (await http.get<WrapperResponse[]>('/api/orders/wrappers')).data,
    getDeliveryPolicy: async () => (await http.get<DeliveryPolicyResponse>('/api/orders/policy/current')).data,
    getPaymentInfo: async (orderKey) =>
      (await http.get<OrderValidationInfoResponse>(`/api/orders/${orderKey}/payments`)).data,
    checkPurchase: async (memberId, bookId) =>
      (await http.get<boolean>('/api/orders/check-purchase', { params: { memberId, bookId } })).data,
    checkReturnEligibility: async (orderId) =>
      (await http.get<OrderReturnCheckResponse>(`/api/orders/${orderId}/returns/eligibility`)).data,
    requestReturn: async (orderId, request) => {
      await http.post(`/api/orders/${orderId}/returns`, request)
    },
    getGuestOrder: async (orderId, password) =>
      (await http.post<GuestOrderDetailResponse>('/api/orders/guests/search', { orderId, password })).data,
  },

  coupons: {
    getBookCoupons: async (bookId) => (await http.get<CouponResponseDto[]>(`/api/coupons/books/${bookId}`)).data,
    getIssuableCoupons: async (page, size) =>
      (await http.get<SpringPage<CouponResponseDto>>('/api/coupons/templates', { params: { page, size } })).data,
    issueCoupon: async (couponId) => {
      await http.post('/api/coupons/issue', { couponId })
    },
    getMyCoupons: async (page, size) =>
      (await http.get<SpringPage<MemberCouponResponseDto>>('/api/coupons/members', { params: { page, size } }))
        .data,
    getUsableCoupons: async (bookIds, categoryIds) =>
      (
        await http.get<MemberCouponResponseDto[]>('/api/coupons/members/order', {
          params: { bookIds: bookIds?.join(','), categoryIds: categoryIds?.join(',') },
        })
      ).data,
    calculate: async (couponId, totalOrderPrice) =>
      (await http.post<CouponCalculationResponseDto>('/api/coupons/calculate', { couponId, totalOrderPrice })).data,
  },

  payments: {
    getMethods: async () => (await http.get<PaymentMethodResponse[]>('/api/payments/methods')).data,
    confirm: async (request) => (await http.post<PaymentConfirmResponse>('/api/payments/confirm', request)).data,
  },

  admin: {
    getStatsSummary: async () => (await http.get<PaymentStatsResponse>('/api/payments/admin/stats/summary')).data,
    getDailySales: async (startDate, endDate) =>
      (await http.get<DailySalesResponse[]>('/api/payments/admin/stats/daily', { params: { startDate, endDate } }))
        .data,
    getBooks: async (page, size) =>
      (await http.get<SpringPage<BookResponse>>('/api/admin/books', { params: { page, size } })).data,
    createBook: async (dto) => (await http.post<BookInfoDto>('/api/admin/books', dto)).data,
    updateBook: async (bookId, dto) => (await http.put<BookResponse>(`/api/admin/books/${bookId}`, dto)).data,
    deleteBook: async (bookId) => {
      await http.delete(`/api/admin/books/${bookId}`)
    },
    searchBookByIsbn: async (isbn) =>
      (await http.get<BookInfoDto>('/api/admin/books/search-api', { params: { isbn } })).data,
    getOrders: async (page, size, status) =>
      (await http.get<CommonPageResponse<OrderResponse>>('/api/admin/orders', { params: { page, size, status } }))
        .data,
    updateOrderStatus: async (orderId, request) => {
      await http.put(`/api/admin/orders/${orderId}/status`, request)
    },
  },
}
