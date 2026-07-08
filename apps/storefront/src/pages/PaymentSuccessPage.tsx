import { useEffect, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { FadeIn } from '../components/motion/FadeIn'

export function PaymentSuccessPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const done = useRef(false)

  useEffect(() => {
    if (done.current) return
    done.current = true

    const paymentKey = params.get('paymentKey')
    const orderKey = params.get('orderId')
    const amountRaw = params.get('amount')
    const method = params.get('method') ?? 'CARD'

    if (!paymentKey || !orderKey || !amountRaw) {
      navigate('/order/fail', {
        replace: true,
        state: { message: '결제 정보가 올바르지 않습니다.' },
      })
      return
    }

    const amount = Number(amountRaw)
    if (!Number.isFinite(amount) || amount <= 0) {
      navigate('/order/fail', {
        replace: true,
        state: { message: '결제 금액이 올바르지 않습니다.' },
      })
      return
    }

    api.payments
      .confirm({ paymentKey, orderKey, amount, paymentMethod: method })
      .then((payment) => {
        navigate('/order/complete', {
          replace: true,
          state: {
            order: { orderId: payment.orderId, orderKey, orderName: '주문 상품', totalAmount: amount },
            payment,
          },
        })
      })
      .catch(() => {
        navigate('/order/fail', {
          replace: true,
          state: { message: '결제 승인에 실패했습니다. 결제 상태를 확인해주세요.' },
        })
      })
  }, [params, navigate])

  return (
    <div className="flex min-h-screen flex-col bg-ink">
      <SlimHeader />
      <main className="flex flex-1 flex-col items-center justify-center px-10 py-24 text-center">
        <FadeIn>
          <p className="text-sm font-light text-body/70">결제를 승인하고 있습니다.</p>
        </FadeIn>
      </main>
      <SiteFooter />
    </div>
  )
}
