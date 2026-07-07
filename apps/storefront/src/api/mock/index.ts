import type {
  AddressResponse,
  BookInfoDto,
  BookResponse,
  BookReviewResponse,
  BookSortType,
  CartDetailResponse,
  CartListResponse,
  CommonPageResponse,
  CouponResponseDto,
  MemberCouponResponseDto,
  MemberResponse,
  OrderResponse,
  SpringPage,
  TokenDto,
} from '../contracts'
import type { StorefrontApi } from '../port'
import { MOCK_BOOKS, MOCK_CATEGORIES, MOCK_REVIEWS } from './books'

const LATENCY_MS = 250

function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), LATENCY_MS))
}

function toPage<T>(all: T[], page: number, size: number): SpringPage<T> {
  const start = page * size
  const content = all.slice(start, start + size)
  const totalPages = Math.max(1, Math.ceil(all.length / size))
  return {
    content,
    totalElements: all.length,
    totalPages,
    number: page,
    size,
    first: page === 0,
    last: page >= totalPages - 1,
  }
}

function toCommonPage<T>(all: T[], page: number, size: number): CommonPageResponse<T> {
  const p = toPage(all, page, size)
  return {
    data: p.content,
    totalElements: p.totalElements,
    totalPages: p.totalPages,
    pageNumber: p.number,
    pageSize: p.size,
    isLast: p.last,
  }
}

function sortBooks(books: BookResponse[], sort: BookSortType): BookResponse[] {
  const copy = [...books]
  switch (sort) {
    case 'NEW':
      return copy.sort((a, b) => (b.pubDate ?? '').localeCompare(a.pubDate ?? ''))
    case 'LOW_PRICE':
      return copy.sort((a, b) => a.price - b.price)
    case 'HIGH_PRICE':
      return copy.sort((a, b) => b.price - a.price)
    case 'RATING':
      return copy.sort((a, b) => (b.avgRating ?? 0) - (a.avgRating ?? 0))
    case 'REVIEW':
    case 'POPULAR':
    default:
      return copy.sort((a, b) => (b.reviewCount ?? 0) - (a.reviewCount ?? 0))
  }
}

// ---------- 세션 내 in-memory 상태 (real adapter는 실제 서버 사용) ----------

const cartItems = new Map<number, number>()
const likedBookIds = new Set<number>()
const myReviews: BookReviewResponse[] = []
let loggedIn = false
let orderSeq = 1000
const myOrders: OrderResponse[] = []

const MOCK_MEMBER: MemberResponse = {
  name: '봉식',
  email: 'sungwoo515@example.com',
  birthDate: '1995-05-15',
  phone: '010-1234-5678',
  status: 'ACTIVE',
  gradeName: 'GOLD',
}

const MOCK_ADDRESS: AddressResponse = {
  addressId: 1,
  alias: '집',
  recipient: '봉식',
  phone: '010-1234-5678',
  zipCode: '61186',
  roadAddress: '광주광역시 북구 어딘가로 123',
  detailAddress: '101동 202호',
  isDefault: true,
}

const MOCK_MY_COUPONS: MemberCouponResponseDto[] = [
  {
    id: 1,
    userId: 1,
    couponId: 10,
    couponName: '신규 가입 10% 할인',
    status: 'ACTIVE',
    issuedAt: '2026-06-01T09:00:00',
    usedAt: null,
    expiredAt: '2026-08-01T00:00:00',
    orderId: null,
    discountValue: 10,
    discountType: 'PERCENT',
    condition: '10,000원 이상 구매 시 사용 가능',
    daysRemaining: 25,
  },
]

const MOCK_ISSUABLE_COUPONS: CouponResponseDto[] = [
  {
    id: 101,
    couponPolicyId: 1,
    couponName: '여름 독서 3,000원 할인',
    description: '20,000원 이상 주문 시 사용 가능한 정액 할인 쿠폰',
    issueCount: 500,
    issueStartAt: '2026-06-01T00:00:00',
    issueEndAt: '2026-08-31T23:59:59',
    validPeriodDate: 30,
    validEndAt: null,
    remainingCount: 312,
    status: 'ACTIVE',
    couponType: 'AMOUNT',
    policyStatus: 'ACTIVE',
  },
  {
    id: 102,
    couponPolicyId: 2,
    couponName: '베스트셀러 15% 할인',
    description: '베스트셀러 카테고리 도서 구매 시 적용',
    issueCount: 200,
    issueStartAt: '2026-07-01T00:00:00',
    issueEndAt: '2026-07-31T23:59:59',
    validPeriodDate: 14,
    validEndAt: null,
    remainingCount: 88,
    status: 'ACTIVE',
    couponType: 'PERCENT',
    policyStatus: 'ACTIVE',
  },
  {
    id: 103,
    couponPolicyId: 3,
    couponName: '첫 구매 5,000원 할인',
    description: '가입 후 첫 주문에 사용 가능한 웰컴 쿠폰',
    issueCount: 1000,
    issueStartAt: '2026-01-01T00:00:00',
    issueEndAt: '2026-12-31T23:59:59',
    validPeriodDate: 60,
    validEndAt: null,
    remainingCount: 543,
    status: 'ACTIVE',
    couponType: 'AMOUNT',
    policyStatus: 'ACTIVE',
  },
]

const MOCK_TOKEN: TokenDto = {
  // header.payload(sub=1).signature 형태의 데모 토큰 (sub 디코드용)
  accessToken: `mock.${btoa(JSON.stringify({ sub: '1' }))}.mock`,
  refreshToken: 'mock-refresh-token',
  profileComplete: true,
}

function toCartList(): CartListResponse {
  const items: CartDetailResponse[] = [...cartItems.entries()].map(([bookId, quantity]) => {
    const found = MOCK_BOOKS.find((b) => b.id === bookId)
    const price = found?.price ?? 0
    return {
      bookId,
      title: found?.title ?? '알 수 없는 도서',
      price,
      quantity,
      totalPrice: price * quantity,
      image: found?.imageUrl ?? null,
    }
  })
  return {
    items,
    totalCartPrice: items.reduce((sum, i) => sum + i.totalPrice, 0),
    hasGuestCart: false,
  }
}

export const mockApi: StorefrontApi = {
  books: {
    getBestSellers: (size) => delay(sortBooks(MOCK_BOOKS, 'POPULAR').slice(0, size)),
    getNewBooks: (size) => delay(sortBooks(MOCK_BOOKS, 'NEW').slice(0, size)),
    getPopularBooks: (size) => delay(sortBooks(MOCK_BOOKS, 'RATING').slice(0, size)),
    getParentCategories: () => delay(MOCK_CATEGORIES),
    getChildCategories: () => delay([]),
    getBooks: (page, size) => delay(toPage(MOCK_BOOKS, page, size)),
    getBooksByCategory: (categoryId, page, size) =>
      delay(toPage(MOCK_BOOKS.filter((b) => b.categoryId === categoryId), page, size)),
    search: (keyword, sort, page, size) => {
      const lower = keyword.toLowerCase()
      const filtered = MOCK_BOOKS.filter(
        (b) => b.title.toLowerCase().includes(lower) || b.author.toLowerCase().includes(lower),
      )
      return delay(toPage(sortBooks(filtered, sort), page, size))
    },
    getBook: (bookId) => {
      const found = MOCK_BOOKS.find((b) => b.id === bookId)
      if (!found) return Promise.reject(new Error(`Book not found: ${bookId}`))
      return delay(found)
    },
    toggleLike: (bookId) => {
      const liked = likedBookIds.has(bookId)
      if (liked) likedBookIds.delete(bookId)
      else likedBookIds.add(bookId)
      return delay(!liked)
    },
    getLikeStatus: (bookId) => delay(likedBookIds.has(bookId)),
    getMyLikedBooks: () => delay(MOCK_BOOKS.filter((b) => likedBookIds.has(b.id))),
  },

  reviews: {
    getReviews: (_bookId, page, size) => delay(toPage([...myReviews, ...MOCK_REVIEWS], page, size)),
    createReview: (_bookId, request) => {
      const review: BookReviewResponse = {
        reviewId: 100 + myReviews.length,
        memberId: 1,
        loginId: 'me',
        content: request.content,
        rating: request.rating,
        createdAt: new Date().toISOString(),
        reviewImages: null,
        likeCount: 0,
        isLiked: false,
      }
      myReviews.unshift(review)
      return delay({ reviewId: review.reviewId, rating: review.rating, content: review.content })
    },
    getMyReviews: (page, size) =>
      delay(
        toPage(
          myReviews.map((r) => ({
            reviewId: r.reviewId,
            bookId: 1,
            bookTitle: MOCK_BOOKS[0].title,
            createdAt: r.createdAt,
          })),
          page,
          size,
        ),
      ),
  },

  auth: {
    login: (_loginId, _password) => {
      loggedIn = true
      return delay(MOCK_TOKEN)
    },
    logout: () => {
      loggedIn = false
      return delay(undefined)
    },
    signup: () => delay(undefined),
    checkLoginId: (loginId) => delay(loginId !== 'taken'),
    sendSignupEmailCode: () => delay(undefined),
    verifyEmailCode: () => delay('인증 성공'),
    sendFindIdCode: () => delay(undefined),
    findLoginId: () => delay('bongsik01'),
    sendPasswordResetCode: () => delay(undefined),
    resetPassword: () => delay(undefined),
  },

  members: {
    getMe: () => (loggedIn ? delay(MOCK_MEMBER) : Promise.reject(new Error('401 Unauthorized'))),
    updateMe: (request) => delay({ ...MOCK_MEMBER, ...request }),
    getAddresses: () => delay({ addressList: [MOCK_ADDRESS] }),
    getDefaultAddress: () => delay(MOCK_ADDRESS),
    createAddress: (request) =>
      delay({ ...MOCK_ADDRESS, addressId: 2, ...request, isDefault: request.defaultAddress }),
    updateAddress: (addressId, request) =>
      delay({ ...MOCK_ADDRESS, addressId, ...request, isDefault: request.defaultAddress }),
    deleteAddress: () => delay(undefined),
    setDefaultAddress: (addressId) => delay({ ...MOCK_ADDRESS, addressId, isDefault: true }),
    getPointBalance: () => delay({ memberId: 1, currentPoint: 3200, totalEarnedPoint: 158000 }),
    getPointHistory: (page, size) =>
      delay(
        toPage(
          [
            {
              id: 1,
              amount: 500,
              description: '상품 구매 적립',
              currentPoint: 3200,
              transactionDate: '2026-06-20T14:30:00',
              orderId: 1001,
            },
          ],
          page,
          size,
        ),
      ),
  },

  cart: {
    getCart: () => delay(toCartList()),
    addItem: (bookId, quantity) => {
      cartItems.set(bookId, (cartItems.get(bookId) ?? 0) + quantity)
      return delay(undefined)
    },
    updateItem: (bookId, quantity) => {
      cartItems.set(bookId, Math.max(1, quantity))
      return delay(undefined)
    },
    removeItem: (bookId) => {
      cartItems.delete(bookId)
      return delay(undefined)
    },
    clear: () => {
      cartItems.clear()
      return delay(undefined)
    },
    mergeGuestCart: () => delay(undefined),
  },

  orders: {
    create: (request) => {
      const orderId = ++orderSeq
      const items = request.orderItems.map((item) => {
        const found = MOCK_BOOKS.find((b) => b.id === item.bookId)
        return {
          bookTitle: found?.title ?? '알 수 없는 도서',
          quantity: item.quantity,
          price: found?.price ?? 0,
        }
      })
      const total = items.reduce((sum, i) => sum + i.price * i.quantity, 0) - (request.usedPoint ?? 0)
      const orderName = items.length > 1 ? `${items[0].bookTitle} 외 ${items.length - 1}건` : items[0]?.bookTitle ?? ''
      myOrders.unshift({
        id: orderId,
        userId: request.userId ?? null,
        orderName,
        orderDate: new Date().toISOString(),
        status: 'PAYMENT_WAITING',
        totalPrice: total,
        trackingNumber: '',
        items,
      })
      cartItems.clear()
      return delay({ orderId, orderKey: `HF-${orderId}`, orderName, totalAmount: total })
    },
    getMyOrders: (page, size) => delay(toCommonPage(myOrders, page, size)),
    getOrder: (orderId) => {
      const found = myOrders.find((o) => o.id === orderId)
      if (!found) return Promise.reject(new Error(`Order not found: ${orderId}`))
      return delay(found)
    },
    cancel: (orderId) => {
      const found = myOrders.find((o) => o.id === orderId)
      if (found) found.status = 'CANCELED'
      return delay(undefined)
    },
    confirm: (orderId) => {
      const found = myOrders.find((o) => o.id === orderId)
      if (found) found.status = 'PURCHASE_CONFIRMED'
      return delay(undefined)
    },
    getWrappers: () =>
      delay([
        { id: 1, name: '크라프트 포장', price: 1500 },
        { id: 2, name: '선물 포장(리본)', price: 3000 },
      ]),
    getDeliveryPolicy: () =>
      delay({
        id: 1,
        standardShippingFee: 3000,
        minOrderAmount: 30000,
        isActive: true,
        effectiveDate: '2026-01-01T00:00:00',
        remoteAreaSurcharge: 3000,
      }),
    getPaymentInfo: (orderKey) => {
      const orderId = Number(orderKey.replace('HF-', ''))
      const found = myOrders.find((o) => o.id === orderId)
      return delay({
        orderId,
        paymentAmount: found?.totalPrice ?? 0,
        orderKey,
        userId: 1,
        usedPoint: 0,
      })
    },
    checkPurchase: () => delay(myOrders.length > 0),
    checkReturnEligibility: () =>
      delay({ isEligible: true, estimatedReturnFee: 3000, estimatedRefundAmount: 13800, message: null }),
    requestReturn: (orderId) => {
      const found = myOrders.find((o) => o.id === orderId)
      if (found) found.status = 'RETURN_REQUESTED'
      return delay(undefined)
    },
    getGuestOrder: (orderId) =>
      delay({
        orderId,
        orderNumber: `HF-${orderId}`,
        orderDate: new Date().toISOString(),
        statusName: '배송중',
        receiverName: '봉식',
        receiverPhone: '010-1234-5678',
        address: '광주광역시 북구 어딘가로 123',
        addressDetail: '101동 202호',
        deliveryRequest: null,
        wrappingFee: 0,
        totalAmount: 16800,
        deliveryFee: 3000,
        couponDiscount: 0,
        pointDiscount: 0,
        paymentAmount: 19800,
        orderItems: [{ title: MOCK_BOOKS[0].title, quantity: 1, price: 16800, totalPrice: 16800, wrapperName: null }],
      }),
  },

  coupons: {
    getBookCoupons: () => delay([]),
    getIssuableCoupons: (page, size) => delay(toPage(MOCK_ISSUABLE_COUPONS, page, size)),
    issueCoupon: (couponId) => {
      const found = MOCK_ISSUABLE_COUPONS.find((c) => c.id === couponId)
      if (!found) return Promise.reject(new Error(`Coupon not found: ${couponId}`))
      if (found.remainingCount !== null) found.remainingCount = Math.max(0, found.remainingCount - 1)
      MOCK_MY_COUPONS.push({
        id: 200 + MOCK_MY_COUPONS.length,
        userId: 1,
        couponId: found.id,
        couponName: found.couponName,
        status: 'ACTIVE',
        issuedAt: new Date().toISOString(),
        usedAt: null,
        expiredAt: found.validEndAt ?? new Date(Date.now() + (found.validPeriodDate ?? 30) * 86400000).toISOString(),
        orderId: null,
        discountValue: found.couponType === 'PERCENT' ? 15 : 3000,
        discountType: found.couponType === 'PERCENT' ? 'PERCENT' : 'AMOUNT',
        condition: found.description ?? '',
        daysRemaining: found.validPeriodDate ?? 30,
      })
      return delay(undefined)
    },
    getMyCoupons: (page, size) => delay(toPage(MOCK_MY_COUPONS, page, size)),
    getUsableCoupons: () => delay(MOCK_MY_COUPONS),
    calculate: (_couponId, totalOrderPrice) => {
      const discountAmount = Math.floor(totalOrderPrice * 0.1)
      return delay({ discountAmount, finalPrice: totalOrderPrice - discountAmount })
    },
  },

  admin: {
    getStatsSummary: () =>
      delay({
        totalSalesAmount: 4823000,
        totalCancelAmount: 312000,
        netSalesAmount: 4511000,
        totalTransactionCount: 291,
        successCount: 274,
        cancelCount: 17,
      }),
    getDailySales: () => {
      const today = new Date()
      const days = Array.from({ length: 7 }, (_, i) => {
        const d = new Date(today)
        d.setDate(d.getDate() - (6 - i))
        return {
          date: d.toISOString().slice(0, 10),
          dailyTotalAmount: 400000 + ((i * 137) % 5) * 90000,
          dailyCount: 18 + ((i * 7) % 11),
        }
      })
      return delay(days)
    },
    getBooks: (page, size) => delay(toPage(MOCK_BOOKS, page, size)),
    createBook: (dto) => {
      const nextId = Math.max(...MOCK_BOOKS.map((b) => b.id)) + 1
      MOCK_BOOKS.push({
        id: nextId,
        title: dto.title,
        author: dto.authors.join(', '),
        isbn: dto.isbn,
        price: dto.price,
        imageUrl: dto.image,
        categories: null,
        tags: null,
        content: dto.description,
        publisher: dto.publisher,
        pubDate: dto.publishedDate,
        avgRating: null,
        reviewCount: 0,
        aiSummary: null,
        aiReviewSummary: null,
        categoryId: dto.categoryId,
        parentId: null,
      })
      return delay(dto)
    },
    updateBook: (bookId, dto) => {
      const idx = MOCK_BOOKS.findIndex((b) => b.id === bookId)
      if (idx < 0) return Promise.reject(new Error(`Book not found: ${bookId}`))
      MOCK_BOOKS[idx] = {
        ...MOCK_BOOKS[idx],
        title: dto.title,
        author: dto.authors.join(', '),
        isbn: dto.isbn,
        price: dto.price,
        imageUrl: dto.image,
        content: dto.description,
        publisher: dto.publisher,
        pubDate: dto.publishedDate,
        categoryId: dto.categoryId,
      }
      return delay(MOCK_BOOKS[idx])
    },
    deleteBook: (bookId) => {
      const idx = MOCK_BOOKS.findIndex((b) => b.id === bookId)
      if (idx >= 0) MOCK_BOOKS.splice(idx, 1)
      return delay(undefined)
    },
    searchBookByIsbn: (isbn) => {
      const dto: BookInfoDto = {
        isbn,
        title: '알라딘에서 찾은 책',
        authors: ['미상'],
        publisher: '알라딘',
        publishedDate: '2026-01-01',
        price: 15000,
        image: null,
        description: 'ISBN 조회로 채워진 데모 데이터입니다.',
        categoryId: 1,
      }
      return delay(dto)
    },
    getOrders: (page, size, status) => {
      const all = status ? myOrders.filter((o) => o.status === status) : myOrders
      return delay(toCommonPage(all, page, size))
    },
    updateOrderStatus: (orderId, request) => {
      const found = myOrders.find((o) => o.id === orderId)
      if (found) {
        found.status = request.status
        if (request.trackingNumber) found.trackingNumber = request.trackingNumber
      }
      return delay(undefined)
    },
  },

  payments: {
    getMethods: () =>
      delay([
        { id: 1, name: 'CARD', alias: '카드 결제', isActive: true },
        { id: 2, name: 'EASY_PAY', alias: '간편 결제', isActive: true },
        { id: 3, name: 'BANK_TRANSFER', alias: '무통장 입금', isActive: true },
      ]),
    confirm: (request) =>
      delay({
        paymentId: 1,
        status: 'DONE',
        amount: request.amount,
        orderId: Number(request.orderKey.replace('HF-', '')),
      }),
  },
}
