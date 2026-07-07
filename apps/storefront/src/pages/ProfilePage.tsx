import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, tokenStore } from '../api'
import type { AddressRequest, AddressResponse, MemberResponse } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { FadeIn } from '../components/motion/FadeIn'
import { ErrorState } from '../components/states'
import { openDaumPostcode } from '../lib/useDaumPostcode'

const inputClass =
  'w-full rounded-2xl border border-[rgba(215,226,234,0.3)] bg-transparent px-5 py-3 text-sm font-light text-body placeholder:text-body/40 focus:border-body/70 focus:outline-none'

const smallBtnClass =
  'cursor-pointer rounded-full border border-[rgba(215,226,234,0.3)] px-4 py-1.5 text-xs text-body/70 transition-opacity hover:opacity-70'

const EMPTY_ADDRESS: AddressRequest = {
  alias: '',
  recipient: '',
  phone: '',
  zipCode: '',
  roadAddress: '',
  detailAddress: '',
  defaultAddress: false,
}

function SectionTitle({ children }: { children: string }) {
  return (
    <h2 className="text-xs font-medium uppercase text-body/55" style={{ letterSpacing: '0.2em' }}>
      {children}
    </h2>
  )
}

/** 배송지 추가/수정 공용 폼 */
function AddressForm({
  initial,
  onSubmit,
  onCancel,
}: {
  initial: AddressRequest
  onSubmit: (request: AddressRequest) => Promise<void>
  onCancel: () => void
}) {
  const [form, setForm] = useState<AddressRequest>(initial)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const set = <K extends keyof AddressRequest>(key: K, value: AddressRequest[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const submit = async () => {
    setError(null)
    if (!form.recipient.trim() || !form.roadAddress.trim()) {
      setError('받는 사람과 주소는 필수입니다.')
      return
    }
    setSubmitting(true)
    try {
      await onSubmit({
        ...form,
        alias: form.alias.trim() || '배송지',
        recipient: form.recipient.trim(),
        phone: form.phone.trim(),
        zipCode: form.zipCode.trim(),
        roadAddress: form.roadAddress.trim(),
        detailAddress: form.detailAddress.trim(),
      })
    } catch {
      setError('저장에 실패했습니다.')
      setSubmitting(false)
    }
  }

  return (
    <div className="mt-4 flex flex-col gap-3 border border-[rgba(215,226,234,0.18)] p-6" style={{ borderRadius: 24 }}>
      <div className="grid gap-3 sm:grid-cols-2">
        <input className={inputClass} placeholder="별칭 (예: 집)" value={form.alias} onChange={(e) => set('alias', e.target.value)} />
        <input className={inputClass} placeholder="받는 사람" value={form.recipient} onChange={(e) => set('recipient', e.target.value)} />
        <input className={inputClass} placeholder="연락처 (010-0000-0000)" value={form.phone} onChange={(e) => set('phone', e.target.value)} />
        <div className="flex gap-2">
          <input className={`${inputClass} flex-1`} placeholder="우편번호" value={form.zipCode} onChange={(e) => set('zipCode', e.target.value)} />
          <button
            type="button"
            className={`${smallBtnClass} shrink-0 whitespace-nowrap`}
            onClick={() => {
              void openDaumPostcode((result) => {
                set('zipCode', result.zonecode)
                set('roadAddress', result.roadAddress)
              })
            }}
          >
            주소 검색
          </button>
        </div>
        <input className={`${inputClass} sm:col-span-2`} placeholder="도로명 주소" value={form.roadAddress} onChange={(e) => set('roadAddress', e.target.value)} />
        <input className={`${inputClass} sm:col-span-2`} placeholder="상세 주소" value={form.detailAddress} onChange={(e) => set('detailAddress', e.target.value)} />
      </div>
      <label className="flex cursor-pointer items-center gap-2.5 text-xs font-light text-body/70">
        <input
          type="checkbox"
          checked={form.defaultAddress}
          onChange={(e) => set('defaultAddress', e.target.checked)}
          className="accent-[#BE4C00]"
        />
        기본 배송지로 설정
      </label>
      {error && (
        <p role="alert" className="text-xs text-accent">
          {error}
        </p>
      )}
      <div className="flex gap-3">
        <button type="button" onClick={() => void submit()} disabled={submitting} className="btn-cta cursor-pointer px-8 py-2.5 text-sm disabled:opacity-60">
          {submitting ? '저장 중…' : '저장'}
        </button>
        <button type="button" onClick={onCancel} className="btn-ghost cursor-pointer px-8 py-2 text-sm">
          취소
        </button>
      </div>
    </div>
  )
}

export function ProfilePage() {
  const memberId = tokenStore.memberId()

  const [me, setMe] = useState<MemberResponse | null>(null)
  const [addresses, setAddresses] = useState<AddressResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  // 내 정보 폼
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [birthDate, setBirthDate] = useState('')
  const [profileMessage, setProfileMessage] = useState<string | null>(null)
  const [savingProfile, setSavingProfile] = useState(false)

  // 배송지 폼 상태: 'closed' | 'new' | addressId
  const [addressForm, setAddressForm] = useState<'closed' | 'new' | number>('closed')

  const fetchAll = useCallback(async () => {
    if (memberId === null) {
      setLoading(false)
      return
    }
    setLoading(true)
    setError(false)
    try {
      const meData = await api.members.getMe()
      setMe(meData)
      setName(meData.name)
      setEmail(meData.email)
      setPhone(meData.phone)
      setBirthDate(meData.birthDate)
      api.members
        .getAddresses()
        .then((res) => setAddresses(res.addressList))
        .catch(() => setAddresses([]))
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [memberId])

  useEffect(() => {
    void fetchAll()
  }, [fetchAll])

  const saveProfile = async () => {
    setProfileMessage(null)
    setSavingProfile(true)
    try {
      const updated = await api.members.updateMe({
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim(),
        birthDate,
      })
      setMe(updated)
      setProfileMessage('저장되었습니다.')
    } catch {
      setProfileMessage('저장에 실패했습니다. 입력값을 확인해주세요.')
    } finally {
      setSavingProfile(false)
    }
  }

  const refreshAddresses = async () => {
    setAddressForm('closed')
    try {
      setAddresses((await api.members.getAddresses()).addressList)
    } catch {
      // 유지
    }
  }

  const removeAddress = async (address: AddressResponse) => {
    if (!window.confirm(`'${address.alias}' 배송지를 삭제할까요?`)) return
    try {
      await api.members.deleteAddress(address.addressId)
      await refreshAddresses()
    } catch {
      window.alert('삭제에 실패했습니다.')
    }
  }

  const makeDefault = async (address: AddressResponse) => {
    try {
      await api.members.setDefaultAddress(address.addressId)
      await refreshAddresses()
    } catch {
      window.alert('기본 배송지 설정에 실패했습니다.')
    }
  }

  if (memberId === null) {
    return (
      <div className="flex min-h-screen flex-col bg-ink">
        <SlimHeader />
        <main className="flex flex-1 flex-col items-center justify-center gap-8 px-10 text-center">
          <h1
            className="font-display font-black uppercase leading-none text-gradient-heading"
            style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
          >
            Profile
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

  return (
    <div className="min-h-screen bg-ink">
      <SlimHeader />

      <main className="mx-auto max-w-[720px] px-4 pb-24 sm:px-10">
        <FadeIn>
          <div className="flex flex-wrap items-baseline gap-x-8 gap-y-2 pt-10">
            <h1
              className="font-display font-black uppercase leading-none text-gradient-heading"
              style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
            >
              Profile
            </h1>
            {me && (
              <p className="text-sm font-light text-body/55">
                {me.gradeName} · {me.status}
              </p>
            )}
          </div>
        </FadeIn>

        {loading ? (
          <div className="mt-12 flex flex-col gap-4" aria-busy="true">
            <div className="skeleton h-64" style={{ borderRadius: 24 }} />
            <div className="skeleton h-40" style={{ borderRadius: 24 }} />
          </div>
        ) : error ? (
          <ErrorState onRetry={() => void fetchAll()} />
        ) : (
          <>
            {/* 내 정보 수정 */}
            <section className="mt-12">
              <SectionTitle>내 정보</SectionTitle>
              <div className="mt-5 grid gap-4 sm:grid-cols-2">
                <input className={inputClass} placeholder="이름" value={name} onChange={(e) => setName(e.target.value)} />
                <input className={inputClass} placeholder="이메일" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
                <input className={inputClass} placeholder="휴대폰 (010-0000-0000)" value={phone} onChange={(e) => setPhone(e.target.value)} />
                <input className={`${inputClass} cursor-pointer`} type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} aria-label="생년월일" />
              </div>
              {profileMessage && <p className="mt-3 text-xs text-body/70">{profileMessage}</p>}
              <button
                type="button"
                onClick={() => void saveProfile()}
                disabled={savingProfile}
                className="btn-cta mt-5 cursor-pointer px-10 py-3 text-sm disabled:opacity-60"
              >
                {savingProfile ? '저장 중…' : '저장'}
              </button>
            </section>

            {/* 배송지 관리 */}
            <section className="mt-16">
              <div className="flex items-center justify-between">
                <SectionTitle>배송지 관리</SectionTitle>
                {addressForm === 'closed' && (
                  <button type="button" onClick={() => setAddressForm('new')} className={smallBtnClass}>
                    ＋ 배송지 추가
                  </button>
                )}
              </div>

              {addressForm === 'new' && (
                <AddressForm
                  initial={EMPTY_ADDRESS}
                  onCancel={() => setAddressForm('closed')}
                  onSubmit={async (request) => {
                    await api.members.createAddress(request)
                    await refreshAddresses()
                  }}
                />
              )}

              <ul className="mt-5 flex flex-col gap-3">
                {addresses.length === 0 && addressForm === 'closed' && (
                  <p className="text-sm font-light text-body/55">등록된 배송지가 없습니다.</p>
                )}
                {addresses.map((address) =>
                  addressForm === address.addressId ? (
                    <li key={address.addressId}>
                      <AddressForm
                        initial={{
                          alias: address.alias,
                          recipient: address.recipient,
                          phone: address.phone,
                          zipCode: address.zipCode,
                          roadAddress: address.roadAddress,
                          detailAddress: address.detailAddress,
                          defaultAddress: address.isDefault,
                        }}
                        onCancel={() => setAddressForm('closed')}
                        onSubmit={async (request) => {
                          await api.members.updateAddress(address.addressId, request)
                          await refreshAddresses()
                        }}
                      />
                    </li>
                  ) : (
                    <li
                      key={address.addressId}
                      className="flex flex-wrap items-center gap-4 border border-[rgba(215,226,234,0.18)] px-6 py-4"
                      style={{ borderRadius: 20 }}
                    >
                      <div className="min-w-0 flex-1">
                        <p className="text-sm text-body">
                          {address.alias}
                          {address.isDefault && (
                            <span className="ml-2 rounded-full border border-accent px-2.5 py-0.5 text-[10px] font-medium text-accent">
                              기본
                            </span>
                          )}
                        </p>
                        <p className="mt-1 truncate text-xs font-light text-body/55">
                          {address.recipient} · {address.phone}
                        </p>
                        <p className="truncate text-xs font-light text-body/55">
                          [{address.zipCode}] {address.roadAddress} {address.detailAddress}
                        </p>
                      </div>
                      <div className="flex gap-2">
                        {!address.isDefault && (
                          <button type="button" onClick={() => void makeDefault(address)} className={smallBtnClass}>
                            기본 지정
                          </button>
                        )}
                        <button type="button" onClick={() => setAddressForm(address.addressId)} className={smallBtnClass}>
                          수정
                        </button>
                        <button type="button" onClick={() => void removeAddress(address)} className={`${smallBtnClass} text-accent`}>
                          삭제
                        </button>
                      </div>
                    </li>
                  ),
                )}
              </ul>
            </section>

            <p className="mt-16 text-center text-xs font-light text-body/55">
              <Link to="/my" className="transition-opacity hover:opacity-70">
                ← 마이페이지로 돌아가기
              </Link>
            </p>
          </>
        )}
      </main>

      <SiteFooter />
    </div>
  )
}
