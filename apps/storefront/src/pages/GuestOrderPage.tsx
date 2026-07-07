import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { FormEvent } from 'react'
import { api } from '../api'
import type { GuestOrderDetailResponse } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { FadeIn } from '../components/motion/FadeIn'
import { formatDate, formatPrice } from '../lib/format'

const inputClass =
  'w-full rounded-2xl border border-[rgba(215,226,234,0.3)] bg-transparent px-5 py-3.5 text-sm font-light text-body placeholder:text-body/40 focus:border-body/70 focus:outline-none'

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-6">
      <dt className="shrink-0 text-body/55">{label}</dt>
      <dd className="text-right">{value}</dd>
    </div>
  )
}

/** 비회원 주문 조회: 주문번호 + 주문 비밀번호 → 주문 상세 */
export function GuestOrderPage() {
  const [orderId, setOrderId] = useState('')
  const [password, setPassword] = useState('')
  const [order, setOrder] = useState<GuestOrderDetailResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    const id = Number(orderId.trim())
    if (!id || !password) {
      setError('주문번호와 주문 비밀번호를 입력해주세요.')
      return
    }
    setSubmitting(true)
    try {
      setOrder(await api.orders.getGuestOrder(id, password))
    } catch {
      setError('주문을 찾을 수 없습니다. 주문번호와 비밀번호를 확인해주세요.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-ink">
      <SlimHeader />

      <main className="flex flex-1 flex-col items-center px-10 py-20">
        <FadeIn>
          <h1
            className="text-center font-display font-black uppercase leading-none text-gradient-heading"
            style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
          >
            Guest Order
          </h1>
        </FadeIn>

        <FadeIn delay={0.15} className="w-full max-w-[440px]">
          {order === null ? (
            <>
              <form onSubmit={(e) => void submit(e)} className="mt-12 flex flex-col gap-4">
                <input
                  className={inputClass}
                  inputMode="numeric"
                  placeholder="주문번호"
                  value={orderId}
                  onChange={(e) => setOrderId(e.target.value)}
                />
                <input
                  type="password"
                  className={inputClass}
                  placeholder="주문 시 설정한 비밀번호"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                {error && (
                  <p role="alert" className="text-xs text-accent">
                    {error}
                  </p>
                )}
                <button type="submit" disabled={submitting} className="btn-cta mt-2 w-full cursor-pointer disabled:opacity-60">
                  {submitting ? '조회 중…' : '주문 조회'}
                </button>
              </form>
              <p className="mt-8 text-center text-xs font-light text-body/55">
                회원이신가요?{' '}
                <Link to="/login" className="text-body/80 transition-opacity hover:opacity-70">
                  로그인 후 주문 내역 보기
                </Link>
              </p>
            </>
          ) : (
            <div className="mt-12">
              <div className="flex items-baseline justify-between">
                <p className="text-sm font-light text-body/70">
                  주문번호 <span className="font-medium text-body">{order.orderNumber}</span>
                </p>
                <span className="rounded-full border border-accent px-4 py-1.5 text-xs font-medium text-accent">
                  {order.statusName}
                </span>
              </div>

              <dl
                className="mt-6 flex flex-col gap-3 border border-[rgba(215,226,234,0.25)] p-8 text-sm font-light text-body/80"
                style={{ borderRadius: 32 }}
              >
                <Row label="주문일" value={formatDate(order.orderDate)} />
                <Row label="받는 사람" value={`${order.receiverName} (${order.receiverPhone})`} />
                <Row label="배송지" value={`${order.address} ${order.addressDetail}`.trim()} />
                {order.deliveryRequest && <Row label="요청사항" value={order.deliveryRequest} />}

                <div className="my-2 border-t border-[rgba(215,226,234,0.18)]" />
                {order.orderItems.map((item, i) => (
                  <div key={i} className="flex justify-between gap-6">
                    <span className="min-w-0 truncate">
                      {item.title} × {item.quantity}
                      {item.wrapperName && <span className="text-body/45"> ({item.wrapperName})</span>}
                    </span>
                    <span className="shrink-0">{formatPrice(item.totalPrice)}</span>
                  </div>
                ))}

                <div className="my-2 border-t border-[rgba(215,226,234,0.18)]" />
                <Row label="상품 금액" value={formatPrice(order.totalAmount)} />
                <Row label="배송비" value={formatPrice(order.deliveryFee)} />
                {order.wrappingFee > 0 && <Row label="포장비" value={formatPrice(order.wrappingFee)} />}
                {order.couponDiscount > 0 && <Row label="쿠폰 할인" value={`-${formatPrice(order.couponDiscount)}`} />}
                {order.pointDiscount > 0 && <Row label="포인트 사용" value={`-${formatPrice(order.pointDiscount)}`} />}
                <div className="flex justify-between gap-6 pt-2">
                  <dt className="text-body/70">총 결제 금액</dt>
                  <dd className="font-semibold text-body" style={{ fontSize: 20 }}>
                    {formatPrice(order.paymentAmount)}
                  </dd>
                </div>
              </dl>

              <button
                type="button"
                onClick={() => {
                  setOrder(null)
                  setPassword('')
                }}
                className="btn-ghost mt-8 w-full cursor-pointer text-center text-sm"
              >
                다른 주문 조회
              </button>
            </div>
          )}
        </FadeIn>
      </main>

      <SiteFooter />
    </div>
  )
}
