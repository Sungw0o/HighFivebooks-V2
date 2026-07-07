import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { FadeIn } from '../components/motion/FadeIn'

const inputClass =
  'w-full rounded-2xl border border-[rgba(215,226,234,0.3)] bg-transparent px-5 py-3.5 text-sm font-light text-body placeholder:text-body/40 focus:border-body/70 focus:outline-none'

const smallBtnClass =
  'shrink-0 cursor-pointer rounded-full border border-[rgba(215,226,234,0.3)] px-5 py-2 text-xs font-medium uppercase text-body transition-opacity hover:opacity-70 disabled:cursor-default disabled:opacity-40'

// 백엔드 PasswordResetRequest 규칙: 8~20자, 영문+숫자 포함
const NEW_PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/

type TabKey = 'id' | 'password'

function FindIdTab() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [code, setCode] = useState('')
  const [foundId, setFoundId] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const send = async () => {
    setMessage(null)
    if (!email.trim()) return setMessage('이메일을 입력해주세요.')
    try {
      await api.auth.sendFindIdCode(email.trim())
      setSent(true)
      setMessage('인증 코드를 이메일로 보냈습니다.')
    } catch {
      setMessage('인증 코드 발송에 실패했습니다. 가입된 이메일인지 확인해주세요.')
    }
  }

  const verify = async () => {
    setMessage(null)
    try {
      const loginId = await api.auth.findLoginId({ email: email.trim(), code: code.trim(), type: 'FIND_ID' })
      setFoundId(loginId)
    } catch {
      setMessage('인증 코드가 올바르지 않습니다.')
    }
  }

  if (foundId) {
    return (
      <div className="flex flex-col items-center gap-6 py-10 text-center">
        <p className="text-sm font-light text-body/70">
          회원님의 아이디는 <span className="font-display text-lg font-semibold text-body">{foundId}</span> 입니다.
        </p>
        <Link to="/login" className="btn-cta">
          로그인하러 가기
        </Link>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-3">
        <input
          type="email"
          className={inputClass}
          placeholder="가입한 이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <button type="button" onClick={() => void send()} className={smallBtnClass}>
          코드 발송
        </button>
      </div>
      {sent && (
        <div className="flex gap-3">
          <input className={inputClass} placeholder="인증 코드" value={code} onChange={(e) => setCode(e.target.value)} />
          <button type="button" onClick={() => void verify()} className={smallBtnClass}>
            아이디 확인
          </button>
        </div>
      )}
      {message && <p className="text-xs text-body/70">{message}</p>}
    </div>
  )
}

function ResetPasswordTab() {
  const [loginId, setLoginId] = useState('')
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [authCode, setAuthCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [done, setDone] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const send = async () => {
    setMessage(null)
    if (!loginId.trim() || !email.trim()) return setMessage('아이디와 이메일을 입력해주세요.')
    try {
      await api.auth.sendPasswordResetCode(email.trim())
      setSent(true)
      setMessage('인증 코드를 이메일로 보냈습니다.')
    } catch {
      setMessage('인증 코드 발송에 실패했습니다.')
    }
  }

  const reset = async () => {
    setMessage(null)
    if (!NEW_PASSWORD_RE.test(newPassword)) return setMessage('새 비밀번호는 8~20자, 영문과 숫자를 포함해야 합니다.')
    try {
      await api.auth.resetPassword({
        loginId: loginId.trim(),
        email: email.trim(),
        authCode: authCode.trim(),
        newPassword,
      })
      setDone(true)
    } catch {
      setMessage('비밀번호 재설정에 실패했습니다. 인증 코드를 확인해주세요.')
    }
  }

  if (done) {
    return (
      <div className="flex flex-col items-center gap-6 py-10 text-center">
        <p className="text-sm font-light text-body/70">비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요.</p>
        <Link to="/login" className="btn-cta">
          로그인하러 가기
        </Link>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <input className={inputClass} placeholder="아이디" value={loginId} onChange={(e) => setLoginId(e.target.value)} />
      <div className="flex gap-3">
        <input
          type="email"
          className={inputClass}
          placeholder="가입한 이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <button type="button" onClick={() => void send()} className={smallBtnClass}>
          코드 발송
        </button>
      </div>
      {sent && (
        <>
          <input
            className={inputClass}
            placeholder="인증 코드"
            value={authCode}
            onChange={(e) => setAuthCode(e.target.value)}
          />
          <input
            type="password"
            className={inputClass}
            placeholder="새 비밀번호 (8~20자, 영문+숫자)"
            autoComplete="new-password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />
          <button type="button" onClick={() => void reset()} className="btn-cta w-full cursor-pointer">
            비밀번호 재설정
          </button>
        </>
      )}
      {message && <p className="text-xs text-body/70">{message}</p>}
    </div>
  )
}

export function AccountFindPage() {
  const [tab, setTab] = useState<TabKey>('id')

  return (
    <div className="flex min-h-screen flex-col bg-ink">
      <SlimHeader />

      <main className="flex flex-1 flex-col items-center justify-center px-10 py-20">
        <FadeIn>
          <h1
            className="text-center font-display font-black uppercase leading-none text-gradient-heading"
            style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
          >
            Find
          </h1>
        </FadeIn>

        <FadeIn delay={0.15} className="w-full max-w-[420px]">
          <div className="mt-12 flex gap-8 border-b border-[rgba(215,226,234,0.18)]">
            {(
              [
                { key: 'id', label: '아이디 찾기' },
                { key: 'password', label: '비밀번호 재설정' },
              ] as { key: TabKey; label: string }[]
            ).map((t) => (
              <button
                key={t.key}
                type="button"
                onClick={() => setTab(t.key)}
                className={`cursor-pointer pb-4 text-sm transition-opacity ${
                  tab === t.key
                    ? 'border-b-2 border-body font-semibold text-body'
                    : 'font-light text-body/55 hover:opacity-70'
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>

          <div className="mt-8">{tab === 'id' ? <FindIdTab /> : <ResetPasswordTab />}</div>

          <p className="mt-8 text-center text-xs font-light text-body/55">
            <Link to="/login" className="transition-opacity hover:opacity-70">
              ← 로그인으로 돌아가기
            </Link>
          </p>
        </FadeIn>
      </main>

      <SiteFooter />
    </div>
  )
}
