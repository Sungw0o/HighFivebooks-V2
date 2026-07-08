import { Link, useLocation, useSearchParams } from 'react-router-dom'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { FadeIn } from '../components/motion/FadeIn'

interface FailState {
  message?: string
}

export function OrderFailPage() {
  const [params] = useSearchParams()
  const location = useLocation()
  const state = (location.state ?? {}) as FailState

  const code = params.get('code')
  const message = params.get('message') ?? state.message ?? '결제가 정상적으로 처리되지 않았습니다.'

  return (
    <div className="flex min-h-screen flex-col bg-ink">
      <SlimHeader />

      <main className="flex flex-1 flex-col items-center justify-center px-10 py-24 text-center">
        <FadeIn>
          <h1
            className="font-display font-black uppercase leading-none text-gradient-heading"
            style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
          >
            Payment Failed
          </h1>
        </FadeIn>

        <FadeIn delay={0.15}>
          <p className="mt-6 max-w-[440px] text-sm font-light text-body/70">{message}</p>
          {code && <p className="mt-2 text-xs font-light text-body/40">오류 코드: {code}</p>}
        </FadeIn>

        <FadeIn delay={0.3}>
          <div className="mt-12 flex flex-wrap items-center justify-center gap-4">
            <Link to="/checkout" className="btn-cta">
              다시 시도
            </Link>
            <Link to="/cart" className="btn-ghost">
              장바구니
            </Link>
            <Link to="/my" className="btn-ghost">
              주문 내역
            </Link>
          </div>
        </FadeIn>
      </main>

      <SiteFooter />
    </div>
  )
}
