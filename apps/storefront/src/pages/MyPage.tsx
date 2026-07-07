import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, tokenStore } from '../api'
import type {
  BookResponse,
  CommonPageResponse,
  MemberResponse,
  MyPageReviewResponse,
  OrderResponse,
  OrderReturnCheckResponse,
  PointBalanceResponse,
  PointHistoryResponse,
  ReturnReason,
  SpringPage,
} from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { BookCard } from '../components/BookCard'
import { BookCover } from '../components/BookCover'
import { EmptyState, ErrorState } from '../components/states'
import { FadeIn } from '../components/motion/FadeIn'
import { formatDate, formatPrice } from '../lib/format'

type TabKey = 'orders' | 'likes' | 'reviews' | 'points'

const TABS: { key: TabKey; label: string }[] = [
  { key: 'orders', label: '주문 내역' },
  { key: 'likes', label: '찜한 책' },
  { key: 'reviews', label: '내 리뷰' },
  { key: 'points', label: '포인트' },
]

/** order-server DeliveryStatus enum → 표시 라벨 (실제 enum 원형) */
const STATUS_LABELS: Record<string, string> = {
  PAYMENT_WAITING: '결제 대기',
  PREPARING: '배송 준비 중',
  DELIVERING: '배송중',
  DELIVERY_COMPLETED: '배송 완료',
  PURCHASE_CONFIRMED: '구매 확정',
  CANCELED: '주문 취소',
  RETURN_REQUESTED: '반품 요청',
  RETURN_COMPLETED: '반품 완료',
}

const DONE_STATUSES = new Set(['DELIVERY_COMPLETED', 'PURCHASE_CONFIRMED', 'CANCELED', 'RETURN_COMPLETED'])
const CANCELABLE_STATUSES = new Set(['PAYMENT_WAITING', 'PREPARING'])
const RETURNABLE_STATUSES = new Set(['DELIVERY_COMPLETED', 'PURCHASE_CONFIRMED'])

const RETURN_REASONS: { value: ReturnReason; label: string }[] = [
  { value: 'SIMPLE_CHANGE', label: '단순 변심' },
  { value: 'PRODUCT_DEFECT', label: '상품 불량' },
  { value: 'DELIVERY_DELAY', label: '배송 지연' },
  { value: 'WRONG_DELIVERY', label: '오배송' },
]

/** 반품 신청 모달: 자격 조회 → 사유/설명 입력 → 신청 */
function ReturnModal({ order, onClose, onDone }: { order: OrderResponse; onClose: () => void; onDone: () => void }) {
  const [eligibility, setEligibility] = useState<OrderReturnCheckResponse | null>(null)
  const [reason, setReason] = useState<ReturnReason>('SIMPLE_CHANGE')
  const [description, setDescription] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    api.orders
      .checkReturnEligibility(order.id)
      .then(setEligibility)
      .catch(() => setError('반품 가능 여부를 확인하지 못했습니다.'))
  }, [order.id])

  const submit = async () => {
    setError(null)
    setSubmitting(true)
    try {
      await api.orders.requestReturn(order.id, { returnReason: reason, description: description.trim() })
      onDone()
    } catch {
      setError('반품 신청에 실패했습니다.')
      setSubmitting(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-6"
      role="dialog"
      aria-modal="true"
      aria-label="반품 신청"
      onClick={onClose}
    >
      <div
        className="w-full max-w-[440px] border border-[rgba(215,226,234,0.25)] bg-ink p-8"
        style={{ borderRadius: 32 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="font-display text-xl font-semibold text-body">반품 신청</h2>
        <p className="mt-1 truncate text-xs font-light text-body/55">{order.orderName}</p>

        {eligibility && !eligibility.isEligible ? (
          <p className="mt-6 text-sm font-light text-accent">{eligibility.message ?? '반품이 불가능한 주문입니다.'}</p>
        ) : (
          <>
            {eligibility && (
              <dl className="mt-5 flex flex-col gap-1.5 text-xs font-light text-body/70">
                <div className="flex justify-between">
                  <dt>예상 환불 금액</dt>
                  <dd>{formatPrice(eligibility.estimatedRefundAmount ?? 0)}</dd>
                </div>
                <div className="flex justify-between">
                  <dt>반품 배송비</dt>
                  <dd>{formatPrice(eligibility.estimatedReturnFee ?? 0)}</dd>
                </div>
              </dl>
            )}
            <select
              className="mt-5 w-full cursor-pointer appearance-none rounded-2xl border border-[rgba(215,226,234,0.3)] bg-transparent px-5 py-3 text-sm font-light text-body focus:border-body/70 focus:outline-none"
              value={reason}
              onChange={(e) => setReason(e.target.value as ReturnReason)}
              aria-label="반품 사유"
            >
              {RETURN_REASONS.map((r) => (
                <option key={r.value} value={r.value} className="bg-ink">
                  {r.label}
                </option>
              ))}
            </select>
            <textarea
              rows={3}
              className="mt-3 w-full resize-y rounded-2xl border border-[rgba(215,226,234,0.3)] bg-transparent px-5 py-3 text-sm font-light text-body placeholder:text-body/40 focus:border-body/70 focus:outline-none"
              placeholder="상세 사유 (선택)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </>
        )}

        {error && (
          <p role="alert" className="mt-3 text-xs text-accent">
            {error}
          </p>
        )}

        <div className="mt-6 flex gap-3">
          {(!eligibility || eligibility.isEligible) && (
            <button
              type="button"
              onClick={() => void submit()}
              disabled={submitting || !eligibility}
              className="btn-cta cursor-pointer px-8 py-3 text-sm disabled:opacity-50"
            >
              {submitting ? '신청 중…' : '신청'}
            </button>
          )}
          <button type="button" onClick={onClose} className="btn-ghost cursor-pointer px-8 py-2.5 text-sm">
            닫기
          </button>
        </div>
      </div>
    </div>
  )
}

function StatusPill({ status }: { status: string }) {
  const label = STATUS_LABELS[status] ?? status
  const shipping = label === '배송중'
  return (
    <span
      className={`rounded-full border px-4 py-1.5 text-xs font-medium uppercase ${
        shipping ? 'border-accent text-accent' : 'border-[rgba(215,226,234,0.3)] text-body/70'
      }`}
      style={{ letterSpacing: '0.1em' }}
    >
      {label}
    </span>
  )
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex-1 border border-[rgba(215,226,234,0.18)] p-6" style={{ borderRadius: 24 }}>
      <p className="font-display font-semibold text-body" style={{ fontSize: 36 }}>
        {value}
      </p>
      <p className="mt-1 text-xs font-light uppercase text-body/55" style={{ letterSpacing: '0.16em' }}>
        {label}
      </p>
    </div>
  )
}

export function MyPage() {
  const navigate = useNavigate()
  const memberId = tokenStore.memberId()

  const [me, setMe] = useState<MemberResponse | null>(null)
  const [orders, setOrders] = useState<CommonPageResponse<OrderResponse> | null>(null)
  const [likes, setLikes] = useState<BookResponse[]>([])
  const [reviews, setReviews] = useState<SpringPage<MyPageReviewResponse> | null>(null)
  const [pointBalance, setPointBalance] = useState<PointBalanceResponse | null>(null)
  const [pointHistory, setPointHistory] = useState<SpringPage<PointHistoryResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [tab, setTab] = useState<TabKey>('orders')
  const [returnTarget, setReturnTarget] = useState<OrderResponse | null>(null)

  const fetchAll = useCallback(async () => {
    if (memberId === null) {
      setLoading(false)
      return
    }
    setLoading(true)
    setError(false)
    try {
      const [meData, orderData] = await Promise.all([api.members.getMe(), api.orders.getMyOrders(0, 10)])
      setMe(meData)
      setOrders(orderData)
      api.books.getMyLikedBooks().then(setLikes).catch(() => setLikes([]))
      api.reviews.getMyReviews(0, 10).then(setReviews).catch(() => undefined)
      api.members.getPointBalance().then(setPointBalance).catch(() => undefined)
      api.members.getPointHistory(0, 10).then(setPointHistory).catch(() => undefined)
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [memberId])

  useEffect(() => {
    void fetchAll()
  }, [fetchAll])

  const cancelOrder = async (orderId: number) => {
    if (!window.confirm('이 주문을 취소할까요?')) return
    try {
      await api.orders.cancel(orderId)
      setOrders(await api.orders.getMyOrders(0, 10))
    } catch {
      window.alert('주문 취소에 실패했습니다.')
    }
  }

  // 로그인 가드 (와이어프레임 4c)
  if (memberId === null) {
    return (
      <div className="flex min-h-screen flex-col bg-ink">
        <SlimHeader />
        <main className="flex flex-1 flex-col items-center justify-center gap-8 px-10 text-center">
          <h1
            className="font-display font-black uppercase leading-none text-gradient-heading"
            style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
          >
            My Page
          </h1>
          <p className="font-light text-body/70">로그인이 필요한 페이지입니다.</p>
          <Link to="/login" className="btn-cta">
            로그인
          </Link>
        </main>
        <SiteFooter />
      </div>
    )
  }

  const inProgressCount = orders?.data.filter((o) => !DONE_STATUSES.has(o.status)).length ?? 0

  return (
    <div className="min-h-screen bg-ink">
      <SlimHeader />

      <main className="px-10 pb-24">
        <FadeIn>
          <div className="flex flex-wrap items-baseline gap-x-8 gap-y-2 pt-10">
            <h1
              className="font-display font-black uppercase leading-none text-gradient-heading"
              style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
            >
              My Page
            </h1>
            {me && (
              <p className="text-sm font-light text-body/55">
                {me.name} · {me.gradeName} · {me.email}
              </p>
            )}
          </div>
        </FadeIn>

        {loading ? (
          <div className="mt-12 flex flex-col gap-6" aria-busy="true">
            <div className="flex gap-6">
              {[0, 1, 2].map((i) => (
                <div key={i} className="skeleton h-28 flex-1" style={{ borderRadius: 24 }} />
              ))}
            </div>
            <div className="skeleton h-64" style={{ borderRadius: 32 }} />
          </div>
        ) : error ? (
          <ErrorState onRetry={() => void fetchAll()} />
        ) : (
          <>
            {/* 통계 카드 */}
            <FadeIn delay={0.1}>
              <div className="mt-12 flex flex-col gap-6 sm:flex-row">
                <StatCard label="진행중 주문" value={inProgressCount} />
                <StatCard label="찜한 책" value={likes.length} />
                <StatCard label="작성한 리뷰" value={reviews?.totalElements ?? 0} />
              </div>
            </FadeIn>

            {/* 탭 */}
            <div className="mt-16 flex gap-8 border-b border-[rgba(215,226,234,0.18)]">
              {TABS.map((t) => (
                <button
                  key={t.key}
                  type="button"
                  onClick={() => setTab(t.key)}
                  className={`cursor-pointer pb-4 text-sm uppercase transition-opacity ${
                    tab === t.key
                      ? 'border-b-2 border-body font-semibold text-body'
                      : 'font-light text-body/55 hover:opacity-70'
                  }`}
                  style={{ letterSpacing: '0.12em' }}
                >
                  {t.label}
                </button>
              ))}
            </div>

            <div className="mt-10">
              {/* 주문 내역 */}
              {tab === 'orders' &&
                (orders && orders.data.length > 0 ? (
                  <ul className="flex flex-col gap-4">
                    {orders.data.map((order) => (
                      <li
                        key={order.id}
                        className="flex flex-wrap items-center gap-6 border border-[rgba(215,226,234,0.18)] p-6"
                        style={{ borderRadius: 24 }}
                      >
                        {/* OrderItemResponse에 bookId/imageUrl 없음 → placeholder 표지 (계약 문서 참조) */}
                        <BookCover
                          bookId={order.id}
                          title={order.items[0]?.bookTitle ?? order.orderName}
                          radius={10}
                          style={{ width: 56, height: 76 }}
                        />
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-normal text-body">{order.orderName}</p>
                          <p className="mt-1 text-xs font-light text-body/55">
                            {formatDate(order.orderDate)} · 주문번호 {order.id}
                          </p>
                        </div>
                        <StatusPill status={order.status} />
                        <span className="w-28 text-right text-sm font-medium text-body">
                          {formatPrice(order.totalPrice)}
                        </span>
                        <div className="flex gap-2">
                          {CANCELABLE_STATUSES.has(order.status) && (
                            <button
                              type="button"
                              onClick={() => void cancelOrder(order.id)}
                              className="cursor-pointer rounded-full border border-[rgba(215,226,234,0.3)] px-4 py-1.5 text-xs text-body/70 transition-opacity hover:opacity-70"
                            >
                              주문 취소
                            </button>
                          )}
                          {RETURNABLE_STATUSES.has(order.status) && (
                            <button
                              type="button"
                              onClick={() => setReturnTarget(order)}
                              className="cursor-pointer rounded-full border border-[rgba(215,226,234,0.3)] px-4 py-1.5 text-xs text-body/70 transition-opacity hover:opacity-70"
                            >
                              반품 신청
                            </button>
                          )}
                          {DONE_STATUSES.has(order.status) && order.status !== 'CANCELED' && (
                            <Link
                              to="/review/new"
                              className="rounded-full border border-[rgba(215,226,234,0.3)] px-4 py-1.5 text-xs text-body/70 transition-opacity hover:opacity-70"
                            >
                              리뷰 작성
                            </Link>
                          )}
                        </div>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <EmptyState
                    message="주문 내역이 없습니다."
                    actionLabel="책 둘러보기"
                    onAction={() => navigate('/books')}
                  />
                ))}

              {/* 찜한 책 */}
              {tab === 'likes' &&
                (likes.length > 0 ? (
                  <div className="grid gap-7" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))' }}>
                    {likes.map((book) => (
                      <BookCard key={book.id} book={book} />
                    ))}
                  </div>
                ) : (
                  <EmptyState
                    message="찜한 책이 없습니다."
                    actionLabel="책 둘러보기"
                    onAction={() => navigate('/books')}
                  />
                ))}

              {/* 내 리뷰 */}
              {tab === 'reviews' &&
                (reviews && reviews.content.length > 0 ? (
                  <ul className="flex max-w-[720px] flex-col gap-3">
                    {reviews.content.map((review) => (
                      <li
                        key={review.reviewId}
                        className="flex items-center justify-between gap-6 border border-[rgba(215,226,234,0.18)] px-6 py-4"
                        style={{ borderRadius: 20 }}
                      >
                        <Link
                          to={`/books/${review.bookId}`}
                          className="truncate text-sm text-body transition-opacity hover:opacity-70"
                        >
                          {review.bookTitle}
                        </Link>
                        <time className="shrink-0 text-xs font-light text-body/45">
                          {formatDate(review.createdAt)}
                        </time>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <EmptyState message="작성한 리뷰가 없습니다." />
                ))}

              {/* 포인트 */}
              {tab === 'points' && (
                <div className="max-w-[720px]">
                  <p className="text-sm font-light text-body/70">
                    보유 포인트{' '}
                    <span className="font-display text-2xl font-semibold text-body">
                      {formatPrice(pointBalance?.currentPoint ?? 0)}
                    </span>
                    <span className="ml-4 text-xs text-body/45">
                      누적 적립 {formatPrice(pointBalance?.totalEarnedPoint ?? 0)}
                    </span>
                  </p>
                  {pointHistory && pointHistory.content.length > 0 ? (
                    <ul className="mt-6 flex flex-col gap-3">
                      {pointHistory.content.map((h) => (
                        <li
                          key={h.id}
                          className="flex items-center justify-between gap-6 border border-[rgba(215,226,234,0.18)] px-6 py-4"
                          style={{ borderRadius: 20 }}
                        >
                          <div className="min-w-0">
                            <p className="truncate text-sm font-light text-body/80">{h.description}</p>
                            <time className="text-xs font-light text-body/45">{formatDate(h.transactionDate)}</time>
                          </div>
                          <span className={`shrink-0 text-sm font-medium ${h.amount >= 0 ? 'text-body' : 'text-accent'}`}>
                            {h.amount >= 0 ? '+' : ''}
                            {h.amount.toLocaleString('ko-KR')}P
                          </span>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="mt-6 text-sm font-light text-body/55">포인트 이력이 없습니다.</p>
                  )}
                </div>
              )}
            </div>
          </>
        )}
      </main>

      {returnTarget && (
        <ReturnModal
          order={returnTarget}
          onClose={() => setReturnTarget(null)}
          onDone={() => {
            setReturnTarget(null)
            void api.orders.getMyOrders(0, 10).then(setOrders)
          }}
        />
      )}

      <SiteFooter />
    </div>
  )
}
