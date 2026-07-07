import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, tokenStore } from '../api'
import type {
  CartListResponse,
  DeliveryPolicyResponse,
  MemberCouponResponseDto,
  PaymentMethodResponse,
  PointBalanceResponse,
} from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { EmptyState, ErrorState } from '../components/states'
import { FadeIn } from '../components/motion/FadeIn'
import { formatPrice } from '../lib/format'
import { openDaumPostcode } from '../lib/useDaumPostcode'

/** 요청사항 옵션 — 백엔드 OrderCreateRequest에 필드 없음(추가 필요, 계약 문서 참조). 현재 UI 전용 */
const DELIVERY_REQUESTS = ['요청사항 없음', '문 앞에 놓아주세요', '경비실에 맡겨주세요', '배송 전 연락주세요']

const FALLBACK_METHODS: PaymentMethodResponse[] = [
  { id: 1, name: 'CARD', alias: '카드', isActive: true },
  { id: 2, name: 'EASY_PAY', alias: '간편 결제', isActive: true },
  { id: 3, name: 'BANK_TRANSFER', alias: '무통장', isActive: true },
]

const inputClass =
  'w-full rounded-2xl border border-[rgba(215,226,234,0.3)] bg-transparent px-5 py-3 text-sm font-light text-body placeholder:text-body/40 focus:border-body/70 focus:outline-none'

function SectionTitle({ children }: { children: string }) {
  return (
    <h2 className="text-xs font-medium uppercase text-body/55" style={{ letterSpacing: '0.2em' }}>
      {children}
    </h2>
  )
}

export function CheckoutPage() {
  const navigate = useNavigate()
  const memberId = tokenStore.memberId()

  const [cart, setCart] = useState<CartListResponse | null>(null)
  const [policy, setPolicy] = useState<DeliveryPolicyResponse | null>(null)
  const [coupons, setCoupons] = useState<MemberCouponResponseDto[]>([])
  const [point, setPoint] = useState<PointBalanceResponse | null>(null)
  const [methods, setMethods] = useState<PaymentMethodResponse[]>(FALLBACK_METHODS)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  // 폼 상태
  const [receiverName, setReceiverName] = useState('')
  const [receiverPhone, setReceiverPhone] = useState('')
  const [address, setAddress] = useState('')
  const [addressDetail, setAddressDetail] = useState('')
  const [deliveryRequest, setDeliveryRequest] = useState(DELIVERY_REQUESTS[0])
  const [requestDeliveryDate, setRequestDeliveryDate] = useState('')
  const [orderPassword, setOrderPassword] = useState('') // 비회원 전용
  const [couponId, setCouponId] = useState<number | null>(null)
  const [couponDiscount, setCouponDiscount] = useState(0)
  const [usedPoint, setUsedPoint] = useState(0)
  const [methodName, setMethodName] = useState<string>(FALLBACK_METHODS[0].name)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const fetchAll = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      const cartData = await api.cart.getCart()
      setCart(cartData)
      api.orders.getDeliveryPolicy().then(setPolicy).catch(() => undefined)
      api.payments.getMethods().then((m) => {
        const active = m.filter((x) => x.isActive)
        if (active.length > 0) {
          setMethods(active)
          setMethodName(active[0].name)
        }
      }).catch(() => undefined)
      if (memberId !== null) {
        api.coupons
          .getUsableCoupons(cartData.items.map((i) => i.bookId))
          .then(setCoupons)
          .catch(() => undefined)
        api.members.getPointBalance().then(setPoint).catch(() => undefined)
        api.members
          .getDefaultAddress()
          .then((addr) => {
            if (!addr) return
            setReceiverName((v) => v || addr.recipient)
            setReceiverPhone((v) => v || addr.phone)
            setAddress((v) => v || `[${addr.zipCode}] ${addr.roadAddress}`)
            setAddressDetail((v) => v || addr.detailAddress)
          })
          .catch(() => undefined)
      }
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [memberId])

  useEffect(() => {
    void fetchAll()
  }, [fetchAll])

  const itemsTotal = cart?.totalCartPrice ?? 0
  const shippingFee =
    policy === null ? 0 : itemsTotal >= policy.minOrderAmount ? 0 : policy.standardShippingFee
  const maxPoint = Math.min(point?.currentPoint ?? 0, Math.max(0, itemsTotal - couponDiscount))
  const payTotal = Math.max(0, itemsTotal - couponDiscount - usedPoint) + shippingFee

  // 쿠폰 선택 → 서버 계산
  const selectCoupon = async (id: number | null) => {
    setCouponId(id)
    if (id === null) {
      setCouponDiscount(0)
      return
    }
    try {
      const result = await api.coupons.calculate(id, itemsTotal)
      setCouponDiscount(result.discountAmount)
    } catch {
      setCouponDiscount(0)
      setCouponId(null)
    }
  }

  const onPointChange = (raw: string) => {
    const value = Number(raw.replace(/\D/g, '')) || 0
    setUsedPoint(Math.min(value, maxPoint))
  }

  const submit = async () => {
    setFormError(null)
    if (!cart || cart.items.length === 0) return
    if (!receiverName.trim()) return setFormError('받는 사람을 입력해주세요.')
    if (!receiverPhone.trim()) return setFormError('연락처를 입력해주세요.')
    if (!address.trim()) return setFormError('주소를 입력해주세요.')
    if (memberId === null && orderPassword.trim().length < 4)
      return setFormError('비회원 주문 비밀번호(4자 이상)를 입력해주세요.')

    setSubmitting(true)
    try {
      const order = await api.orders.create({
        userId: memberId,
        orderPassword: memberId === null ? orderPassword : null,
        receiverName: receiverName.trim(),
        receiverAddress: `${address.trim()} ${addressDetail.trim()}`.trim(),
        requestDeliveryDate: requestDeliveryDate || null,
        couponId,
        usedPoint: usedPoint > 0 ? usedPoint : null,
        orderItems: cart.items.map((item) => ({ bookId: item.bookId, quantity: item.quantity })),
      })
      // 데모용 pseudo paymentKey — 실서비스는 PG(토스 등) 위젯이 발급 (계약 문서 참조)
      const payment = await api.payments.confirm({
        paymentKey: `DEMO-${order.orderKey}`,
        orderKey: order.orderKey,
        amount: order.totalAmount,
        paymentMethod: methodName,
      })
      navigate('/order/complete', {
        state: { order, payment, receiverName, address: `${address} ${addressDetail}`.trim() },
      })
    } catch {
      setFormError('주문 처리에 실패했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setSubmitting(false)
    }
  }

  const activeCoupons = useMemo(() => coupons.filter((c) => c.status === 'ACTIVE'), [coupons])

  return (
    <div className="min-h-screen bg-ink">
      <SlimHeader />

      <main className="px-4 pb-24 sm:px-10">
        <FadeIn>
          <h1
            className="pt-10 font-display font-black uppercase leading-none text-gradient-heading"
            style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
          >
            Checkout
          </h1>
        </FadeIn>

        {loading ? (
          <div className="mt-12 grid gap-10 lg:grid-cols-[1fr_360px]" aria-busy="true">
            <div className="skeleton h-96" style={{ borderRadius: 32 }} />
            <div className="skeleton h-72" style={{ borderRadius: 40 }} />
          </div>
        ) : error ? (
          <ErrorState onRetry={() => void fetchAll()} />
        ) : !cart || cart.items.length === 0 ? (
          <EmptyState message="주문할 상품이 없습니다." actionLabel="책 둘러보기" onAction={() => navigate('/books')} />
        ) : (
          <div className="mt-12 grid items-start gap-10 lg:grid-cols-[1fr_360px]">
            <div className="flex flex-col gap-12">
              {/* 배송지 */}
              <section>
                <SectionTitle>배송지</SectionTitle>
                <div className="mt-5 grid gap-4 sm:grid-cols-2">
                  <input
                    className={inputClass}
                    placeholder="받는 사람"
                    value={receiverName}
                    onChange={(e) => setReceiverName(e.target.value)}
                  />
                  <input
                    className={inputClass}
                    placeholder="연락처 (010-0000-0000)"
                    value={receiverPhone}
                    onChange={(e) => setReceiverPhone(e.target.value)}
                  />
                  <div className="flex gap-2 sm:col-span-2">
                    <input
                      className={`${inputClass} flex-1`}
                      placeholder="주소"
                      value={address}
                      onChange={(e) => setAddress(e.target.value)}
                    />
                    <button
                      type="button"
                      className="shrink-0 cursor-pointer whitespace-nowrap rounded-full border border-[rgba(215,226,234,0.3)] px-4 py-1.5 text-xs text-body/70 transition-opacity hover:opacity-70"
                      onClick={() => {
                        void openDaumPostcode((result) => {
                          setAddress(result.roadAddress)
                        })
                      }}
                    >
                      주소 검색
                    </button>
                  </div>
                  <input
                    className={`${inputClass} sm:col-span-2`}
                    placeholder="상세 주소"
                    value={addressDetail}
                    onChange={(e) => setAddressDetail(e.target.value)}
                  />
                  <select
                    className={`${inputClass} cursor-pointer appearance-none`}
                    value={deliveryRequest}
                    onChange={(e) => setDeliveryRequest(e.target.value)}
                    aria-label="배송 요청사항"
                  >
                    {DELIVERY_REQUESTS.map((option) => (
                      <option key={option} value={option} className="bg-ink">
                        {option}
                      </option>
                    ))}
                  </select>
                  <input
                    type="date"
                    className={`${inputClass} cursor-pointer`}
                    value={requestDeliveryDate}
                    onChange={(e) => setRequestDeliveryDate(e.target.value)}
                    aria-label="희망 배송일"
                  />
                  {memberId === null && (
                    <input
                      type="password"
                      className={`${inputClass} sm:col-span-2`}
                      placeholder="비회원 주문 비밀번호 (주문 조회용)"
                      value={orderPassword}
                      onChange={(e) => setOrderPassword(e.target.value)}
                    />
                  )}
                </div>
              </section>

              {/* 쿠폰 / 포인트 (회원 전용) */}
              {memberId !== null && (
                <section>
                  <SectionTitle>쿠폰 / 포인트</SectionTitle>
                  <div className="mt-5 grid gap-4 sm:grid-cols-2">
                    <select
                      className={`${inputClass} cursor-pointer appearance-none`}
                      value={couponId ?? ''}
                      onChange={(e) => void selectCoupon(e.target.value ? Number(e.target.value) : null)}
                      aria-label="쿠폰 선택"
                    >
                      <option value="" className="bg-ink">
                        쿠폰 선택 안 함
                      </option>
                      {activeCoupons.map((coupon) => (
                        <option key={coupon.id} value={coupon.couponId} className="bg-ink">
                          {coupon.couponName} ({coupon.condition})
                        </option>
                      ))}
                    </select>
                    <div className="relative">
                      <input
                        className={inputClass}
                        inputMode="numeric"
                        placeholder="사용할 포인트"
                        value={usedPoint === 0 ? '' : String(usedPoint)}
                        onChange={(e) => onPointChange(e.target.value)}
                        aria-label="사용할 포인트"
                      />
                      <button
                        type="button"
                        onClick={() => setUsedPoint(maxPoint)}
                        className="absolute right-4 top-1/2 -translate-y-1/2 cursor-pointer text-xs text-body/55 hover:opacity-70"
                      >
                        전액 사용
                      </button>
                    </div>
                  </div>
                  <p className="mt-2 text-xs font-light text-body/45">
                    보유 포인트 {formatPrice(point?.currentPoint ?? 0)}
                  </p>
                </section>
              )}

              {/* 결제 수단 */}
              <section>
                <SectionTitle>결제 수단</SectionTitle>
                <div className="mt-5 flex flex-wrap gap-3">
                  {methods.map((method) => (
                    <button
                      key={method.id}
                      type="button"
                      onClick={() => setMethodName(method.name)}
                      aria-pressed={methodName === method.name}
                      className={`cursor-pointer rounded-full border px-7 py-3 text-sm font-medium uppercase transition-colors duration-200 ${
                        methodName === method.name
                          ? 'border-body bg-body text-ink'
                          : 'border-[rgba(215,226,234,0.3)] text-body hover:bg-[rgba(215,226,234,0.1)]'
                      }`}
                      style={{ letterSpacing: '0.1em' }}
                    >
                      {method.alias}
                    </button>
                  ))}
                </div>
              </section>
            </div>

            {/* 결제 금액 요약 */}
            <aside className="border-2 border-body p-8" style={{ borderRadius: 40 }}>
              <SectionTitle>결제 금액</SectionTitle>
              <dl className="mt-6 flex flex-col gap-3 text-sm font-light text-body/80">
                <div className="flex justify-between">
                  <dt>상품 금액 ({cart.items.length}종)</dt>
                  <dd>{formatPrice(itemsTotal)}</dd>
                </div>
                <div className="flex justify-between">
                  <dt>배송비</dt>
                  <dd>{shippingFee === 0 ? '무료' : formatPrice(shippingFee)}</dd>
                </div>
                {couponDiscount > 0 && (
                  <div className="flex justify-between text-accent">
                    <dt>쿠폰 할인</dt>
                    <dd>-{formatPrice(couponDiscount)}</dd>
                  </div>
                )}
                {usedPoint > 0 && (
                  <div className="flex justify-between text-accent">
                    <dt>포인트 사용</dt>
                    <dd>-{formatPrice(usedPoint)}</dd>
                  </div>
                )}
              </dl>
              <div className="mt-6 flex items-baseline justify-between border-t border-[rgba(215,226,234,0.18)] pt-5">
                <span className="text-sm text-body/70">총 결제 금액</span>
                <span className="font-semibold text-body" style={{ fontSize: 24 }}>
                  {formatPrice(payTotal)}
                </span>
              </div>
              {formError && (
                <p role="alert" className="mt-4 text-xs text-accent">
                  {formError}
                </p>
              )}
              <button
                type="button"
                onClick={() => void submit()}
                disabled={submitting}
                className="btn-cta mt-8 w-full cursor-pointer text-center disabled:opacity-60"
              >
                {submitting ? '처리 중…' : '결제하기'}
              </button>
            </aside>
          </div>
        )}
      </main>

      <SiteFooter />
    </div>
  )
}
