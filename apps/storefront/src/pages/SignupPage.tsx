import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import type { FormEvent } from 'react'
import { api } from '../api'
import type { Gender } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { FadeIn } from '../components/motion/FadeIn'

const inputClass =
  'w-full rounded-2xl border border-[rgba(215,226,234,0.3)] bg-transparent px-5 py-3.5 text-sm font-light text-body placeholder:text-body/40 focus:border-body/70 focus:outline-none'

const smallBtnClass =
  'shrink-0 cursor-pointer rounded-full border border-[rgba(215,226,234,0.3)] px-5 py-2 text-xs font-medium uppercase text-body transition-opacity hover:opacity-70 disabled:cursor-default disabled:opacity-40'

// 백엔드 MemberCreateRequest와 동일한 규칙
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/
const PHONE_RE = /^010-\d{4}-\d{4}$/

export function SignupPage() {
  const navigate = useNavigate()

  const [loginId, setLoginId] = useState('')
  const [idChecked, setIdChecked] = useState<boolean | null>(null)
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [emailCodeSent, setEmailCodeSent] = useState(false)
  const [emailCode, setEmailCode] = useState('')
  const [emailVerified, setEmailVerified] = useState(false)
  const [phone, setPhone] = useState('')
  const [gender, setGender] = useState<Gender>('UNKNOWN')
  const [birthDate, setBirthDate] = useState('')

  // 약관 (백엔드 필드 없음 — 프론트 검증만)
  const [agreeTerms, setAgreeTerms] = useState(false)
  const [agreePrivacy, setAgreePrivacy] = useState(false)
  const [agreeMarketing, setAgreeMarketing] = useState(false)
  const allAgreed = agreeTerms && agreePrivacy && agreeMarketing

  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const toggleAll = () => {
    const next = !allAgreed
    setAgreeTerms(next)
    setAgreePrivacy(next)
    setAgreeMarketing(next)
  }

  const checkId = async () => {
    setMessage(null)
    if (!loginId.trim()) return
    try {
      const available = await api.auth.checkLoginId(loginId.trim())
      setIdChecked(available)
      setMessage(available ? '사용 가능한 아이디입니다.' : '이미 사용 중인 아이디입니다.')
    } catch {
      setIdChecked(null)
      setMessage('아이디 확인에 실패했습니다.')
    }
  }

  const sendEmailCode = async () => {
    setMessage(null)
    if (!email.trim()) return
    try {
      // 휴대폰 인증 API 부재 → 이메일 인증으로 대체 (계약 문서 참조)
      await api.auth.sendSignupEmailCode(email.trim())
      setEmailCodeSent(true)
      setMessage('인증 코드를 이메일로 보냈습니다.')
    } catch {
      setMessage('인증 코드 발송에 실패했습니다.')
    }
  }

  const verifyEmail = async () => {
    setMessage(null)
    try {
      await api.auth.verifyEmailCode({ email: email.trim(), code: emailCode.trim(), type: 'SIGNUP' })
      setEmailVerified(true)
      setMessage('이메일 인증이 완료되었습니다.')
    } catch {
      setMessage('인증 코드가 올바르지 않습니다.')
    }
  }

  const validationError = useMemo((): string | null => {
    if (!loginId.trim()) return '아이디를 입력해주세요.'
    if (idChecked === false) return '이미 사용 중인 아이디입니다.'
    if (!PASSWORD_RE.test(password)) return '비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다.'
    if (password !== passwordConfirm) return '비밀번호가 일치하지 않습니다.'
    if (!name.trim()) return '이름을 입력해주세요.'
    if (!email.trim()) return '이메일을 입력해주세요.'
    if (!PHONE_RE.test(phone)) return '전화번호는 010-0000-0000 형식이어야 합니다.'
    if (!birthDate) return '생년월일을 입력해주세요.'
    if (!agreeTerms || !agreePrivacy) return '필수 약관에 동의해주세요.'
    return null
  }, [loginId, idChecked, password, passwordConfirm, name, email, phone, birthDate, agreeTerms, agreePrivacy])

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (validationError) {
      setError(validationError)
      return
    }
    setSubmitting(true)
    try {
      await api.auth.signup({
        loginId: loginId.trim(),
        password,
        name: name.trim(),
        phone: phone.trim(),
        email: email.trim(),
        gender,
        birthDate,
      })
      navigate('/login', { state: { signedUp: true } })
    } catch {
      setError('회원가입에 실패했습니다. 입력값을 확인해주세요.')
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
            Sign Up
          </h1>
        </FadeIn>

        <FadeIn delay={0.15} className="w-full max-w-[480px]">
          <form onSubmit={(e) => void submit(e)} className="mt-12 flex flex-col gap-4">
            <div className="flex gap-3">
              <input
                className={inputClass}
                placeholder="아이디"
                autoComplete="username"
                value={loginId}
                onChange={(e) => {
                  setLoginId(e.target.value)
                  setIdChecked(null)
                }}
              />
              <button type="button" onClick={() => void checkId()} className={smallBtnClass}>
                중복 확인
              </button>
            </div>

            <input
              type="password"
              className={inputClass}
              placeholder="비밀번호 (8~20자, 영문+숫자+특수문자)"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <input
              type="password"
              className={inputClass}
              placeholder="비밀번호 확인"
              autoComplete="new-password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
            />

            <input className={inputClass} placeholder="이름" value={name} onChange={(e) => setName(e.target.value)} />

            <div className="flex gap-3">
              <input
                type="email"
                className={inputClass}
                placeholder="이메일"
                autoComplete="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value)
                  setEmailVerified(false)
                  setEmailCodeSent(false)
                }}
              />
              <button type="button" onClick={() => void sendEmailCode()} disabled={emailVerified} className={smallBtnClass}>
                {emailVerified ? '인증 완료' : '인증'}
              </button>
            </div>
            {emailCodeSent && !emailVerified && (
              <div className="flex gap-3">
                <input
                  className={inputClass}
                  placeholder="인증 코드"
                  value={emailCode}
                  onChange={(e) => setEmailCode(e.target.value)}
                />
                <button type="button" onClick={() => void verifyEmail()} className={smallBtnClass}>
                  확인
                </button>
              </div>
            )}

            <input
              className={inputClass}
              placeholder="휴대폰 (010-0000-0000)"
              autoComplete="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />

            <div className="flex gap-3">
              <select
                className={`${inputClass} cursor-pointer appearance-none`}
                value={gender}
                onChange={(e) => setGender(e.target.value as Gender)}
                aria-label="성별"
              >
                <option value="UNKNOWN" className="bg-ink">
                  성별 선택 안 함
                </option>
                <option value="MALE" className="bg-ink">
                  남성
                </option>
                <option value="FEMALE" className="bg-ink">
                  여성
                </option>
              </select>
              <input
                type="date"
                className={`${inputClass} cursor-pointer`}
                value={birthDate}
                onChange={(e) => setBirthDate(e.target.value)}
                aria-label="생년월일"
              />
            </div>

            {/* 약관 동의 */}
            <fieldset className="mt-2 flex flex-col gap-2.5 border border-[rgba(215,226,234,0.18)] p-5" style={{ borderRadius: 20 }}>
              <label className="flex cursor-pointer items-center gap-2.5 text-sm font-medium text-body">
                <input type="checkbox" checked={allAgreed} onChange={toggleAll} className="accent-[#BE4C00]" />
                전체 동의
              </label>
              <label className="flex cursor-pointer items-center gap-2.5 text-xs font-light text-body/70">
                <input type="checkbox" checked={agreeTerms} onChange={(e) => setAgreeTerms(e.target.checked)} className="accent-[#BE4C00]" />
                [필수] 이용약관 동의
              </label>
              <label className="flex cursor-pointer items-center gap-2.5 text-xs font-light text-body/70">
                <input type="checkbox" checked={agreePrivacy} onChange={(e) => setAgreePrivacy(e.target.checked)} className="accent-[#BE4C00]" />
                [필수] 개인정보 수집·이용 동의
              </label>
              <label className="flex cursor-pointer items-center gap-2.5 text-xs font-light text-body/70">
                <input type="checkbox" checked={agreeMarketing} onChange={(e) => setAgreeMarketing(e.target.checked)} className="accent-[#BE4C00]" />
                [선택] 마케팅 정보 수신 동의
              </label>
            </fieldset>

            {message && <p className="text-xs text-body/70">{message}</p>}
            {error && (
              <p role="alert" className="text-xs text-accent">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={submitting || !agreeTerms || !agreePrivacy}
              className="btn-cta mt-2 w-full cursor-pointer disabled:opacity-50"
            >
              {submitting ? '가입 중…' : '가입하기'}
            </button>
          </form>

          <p className="mt-8 text-center text-xs font-light text-body/55">
            이미 계정이 있으신가요?{' '}
            <Link to="/login" className="text-body/80 transition-opacity hover:opacity-70">
              로그인
            </Link>
          </p>
        </FadeIn>
      </main>

      <SiteFooter />
    </div>
  )
}
