import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import type { FormEvent } from 'react'
import { api } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { FadeIn } from '../components/motion/FadeIn'

const inputClass =
  'w-full rounded-2xl border border-[rgba(215,226,234,0.3)] bg-transparent px-5 py-3.5 text-sm font-light text-body placeholder:text-body/40 focus:border-body/70 focus:outline-none'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const signedUp = (location.state as { signedUp?: boolean } | null)?.signedUp ?? false

  // 백엔드 계약은 이메일이 아닌 loginId 기반 (docs/STOREFRONT_API_CONTRACT.md)
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(signedUp ? '가입이 완료되었습니다. 로그인해주세요.' : null)
  const [submitting, setSubmitting] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setInfo(null)
    if (!loginId.trim() || !password) {
      setError('아이디와 비밀번호를 입력해주세요.')
      return
    }
    setSubmitting(true)
    try {
      await api.auth.login(loginId.trim(), password)
      try {
        await api.cart.mergeGuestCart() // 비회원 장바구니 병합
      } catch {
        // 게스트 장바구니 없으면 무시
      }
      navigate('/')
    } catch {
      setError('아이디 또는 비밀번호가 올바르지 않습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-ink">
      <SlimHeader />

      <main className="flex flex-1 flex-col items-center justify-center px-10 py-20">
        <FadeIn>
          <h1
            className="text-center font-display font-black uppercase leading-none text-gradient-heading"
            style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
          >
            Login
          </h1>
        </FadeIn>

        <FadeIn delay={0.15} className="w-full max-w-[400px]">
          <form onSubmit={(e) => void submit(e)} className="mt-12 flex flex-col gap-4">
            <input
              className={inputClass}
              placeholder="아이디"
              autoComplete="username"
              value={loginId}
              onChange={(e) => setLoginId(e.target.value)}
            />
            <input
              type="password"
              className={inputClass}
              placeholder="비밀번호"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            {info && <p className="text-xs text-body/70">{info}</p>}
            {error && (
              <p role="alert" className="text-xs text-accent">
                {error}
              </p>
            )}

            <button type="submit" disabled={submitting} className="btn-cta mt-2 w-full cursor-pointer disabled:opacity-60">
              {submitting ? '로그인 중…' : '로그인'}
            </button>
            <button
              type="button"
              onClick={() => setInfo('소셜 로그인은 PAYCO OAuth 연동 후 활성화됩니다.')}
              className="btn-ghost w-full cursor-pointer text-center"
            >
              PAYCO로 로그인
            </button>
          </form>

          <div className="mt-8 flex items-center justify-center gap-6 text-xs font-light text-body/55">
            <Link to="/signup" className="transition-opacity hover:opacity-70">
              회원가입
            </Link>
            <span aria-hidden="true">·</span>
            <Link to="/account/find" className="transition-opacity hover:opacity-70">
              아이디/비밀번호 찾기
            </Link>
            <span aria-hidden="true">·</span>
            <Link to="/order/guest" className="transition-opacity hover:opacity-70">
              비회원 주문 조회
            </Link>
          </div>
        </FadeIn>
      </main>

      <SiteFooter />
    </div>
  )
}
