import { Link, useLocation } from 'react-router-dom'
import type { OrderCreateResponse, PaymentConfirmResponse } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { FadeIn } from '../components/motion/FadeIn'
import { formatPrice } from '../lib/format'

interface CompleteState {
  order?: OrderCreateResponse
  payment?: PaymentConfirmResponse
  receiverName?: string
  address?: string
}

export function OrderCompletePage() {
  const location = useLocation()
  const state = (location.state ?? {}) as CompleteState
  const { order, payment } = state

  return (
    <div className="flex min-h-screen flex-col bg-ink">
      <SlimHeader />

      <main className="flex flex-1 flex-col items-center justify-center px-10 py-24 text-center">
        <FadeIn>
          <h1
            className="font-display font-black uppercase leading-none text-gradient-heading"
            style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
          >
            Thank You
          </h1>
        </FadeIn>

        {order ? (
          <FadeIn delay={0.15}>
            <p className="mt-6 text-sm font-light text-body/70">
              주문번호 <span className="font-medium text-body">{order.orderKey}</span>
            </p>

            <dl
              className="mx-auto mt-10 flex w-full max-w-[440px] flex-col gap-3 border border-[rgba(215,226,234,0.25)] p-8 text-left text-sm font-light text-body/80"
              style={{ borderRadius: 32 }}
            >
              <div className="flex justify-between gap-6">
                <dt className="shrink-0 text-body/55">상품</dt>
                <dd className="truncate">{order.orderName}</dd>
              </div>
              <div className="flex justify-between gap-6">
                <dt className="text-body/55">결제 금액</dt>
                <dd className="font-medium text-body">{formatPrice(payment?.amount ?? order.totalAmount)}</dd>
              </div>
              {state.receiverName && (
                <div className="flex justify-between gap-6">
                  <dt className="text-body/55">받는 사람</dt>
                  <dd>{state.receiverName}</dd>
                </div>
              )}
              {state.address && (
                <div className="flex justify-between gap-6">
                  <dt className="shrink-0 text-body/55">배송지</dt>
                  <dd className="text-right">{state.address}</dd>
                </div>
              )}
              {payment && (
                <div className="flex justify-between gap-6">
                  <dt className="text-body/55">결제 상태</dt>
                  <dd>{payment.status}</dd>
                </div>
              )}
            </dl>
          </FadeIn>
        ) : (
          <p className="mt-6 text-sm font-light text-body/55">주문 정보를 찾을 수 없습니다.</p>
        )}

        <FadeIn delay={0.3}>
          <div className="mt-12 flex flex-wrap items-center justify-center gap-4">
            <Link to="/my" className="btn-cta">
              주문 내역 보기
            </Link>
            <Link to="/books" className="btn-ghost">
              계속 쇼핑
            </Link>
          </div>
        </FadeIn>
      </main>

      <SiteFooter />
    </div>
  )
}
