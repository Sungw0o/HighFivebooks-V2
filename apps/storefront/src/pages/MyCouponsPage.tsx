import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, tokenStore } from '../api'
import type { CouponResponseDto, MemberCouponResponseDto, SpringPage } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { EmptyState, ErrorState } from '../components/states'
import { FadeIn } from '../components/motion/FadeIn'
import { formatDate } from '../lib/format'

type TabKey = 'owned' | 'issuable'

const TABS: { key: TabKey; label: string }[] = [
  { key: 'owned', label: '보유 쿠폰' },
  { key: 'issuable', label: '발급받기' },
]

const STATUS_STYLES: Record<string, string> = {
  ACTIVE: 'border-accent text-accent',
  USED: 'border-[rgba(215,226,234,0.3)] text-body/45',
  EXPIRED: 'border-[rgba(215,226,234,0.3)] text-body/45',
}

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: '사용 가능',
  USED: '사용 완료',
  EXPIRED: '만료됨',
}

function CouponStatusPill({ status }: { status: string }) {
  return (
    <span
      className={`rounded-full border px-4 py-1.5 text-xs font-medium uppercase ${STATUS_STYLES[status] ?? 'border-[rgba(215,226,234,0.3)] text-body/70'}`}
      style={{ letterSpacing: '0.1em' }}
    >
      {STATUS_LABELS[status] ?? status}
    </span>
  )
}

function OwnedCouponCard({ coupon }: { coupon: MemberCouponResponseDto }) {
  const discountLabel =
    coupon.discountType === 'PERCENT'
      ? `${coupon.discountValue}% 할인`
      : `${coupon.discountValue.toLocaleString('ko-KR')}원 할인`

  return (
    <article
      className="flex flex-wrap items-center gap-5 border border-[rgba(215,226,234,0.18)] p-6"
      style={{ borderRadius: 24 }}
    >
      {/* 할인 뱃지 */}
      <div
        className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-[rgba(190,76,0,0.15)]"
        aria-hidden="true"
      >
        <span className="font-display text-lg font-bold text-accent">
          {coupon.discountType === 'PERCENT' ? `${coupon.discountValue}%` : '₩'}
        </span>
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-normal text-body">{coupon.couponName}</p>
        <p className="mt-1 text-xs font-light text-body/55">{discountLabel}</p>
        <p className="mt-0.5 text-xs font-light text-body/45">{coupon.condition}</p>
      </div>

      <div className="flex flex-col items-end gap-2">
        <CouponStatusPill status={coupon.status} />
        {coupon.expiredAt && (
          <p className="text-xs font-light text-body/45">
            {formatDate(coupon.expiredAt)} 만료
            {coupon.daysRemaining > 0 && (
              <span className="ml-1 text-accent">({coupon.daysRemaining}일 남음)</span>
            )}
          </p>
        )}
      </div>
    </article>
  )
}

function IssuableCouponCard({
  coupon,
  onIssue,
  issuing,
}: {
  coupon: CouponResponseDto
  onIssue: () => void
  issuing: boolean
}) {
  return (
    <article
      className="flex flex-wrap items-center gap-5 border border-[rgba(215,226,234,0.18)] p-6"
      style={{ borderRadius: 24 }}
    >
      {/* 아이콘 */}
      <div
        className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-[rgba(215,226,234,0.08)]"
        aria-hidden="true"
      >
        <span className="font-display text-2xl font-bold text-body/50">🎟</span>
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-normal text-body">{coupon.couponName}</p>
        {coupon.description && (
          <p className="mt-1 text-xs font-light text-body/55">{coupon.description}</p>
        )}
        <div className="mt-1 flex flex-wrap gap-3 text-xs font-light text-body/45">
          {coupon.remainingCount !== null && <span>잔여 {coupon.remainingCount}장</span>}
          {coupon.issueEndAt && <span>{formatDate(coupon.issueEndAt)}까지</span>}
        </div>
      </div>

      <button
        type="button"
        onClick={onIssue}
        disabled={issuing || coupon.remainingCount === 0}
        className="btn-cta cursor-pointer px-6 py-2.5 text-xs disabled:cursor-default disabled:opacity-50"
      >
        {issuing ? '발급 중…' : '발급받기'}
      </button>
    </article>
  )
}

function CouponSkeleton() {
  return (
    <div className="flex flex-col gap-4" aria-busy="true">
      {Array.from({ length: 3 }, (_, i) => (
        <div key={i} className="skeleton h-28" style={{ borderRadius: 24 }} />
      ))}
    </div>
  )
}

export function MyCouponsPage() {
  const memberId = tokenStore.memberId()

  const [tab, setTab] = useState<TabKey>('owned')
  const [owned, setOwned] = useState<SpringPage<MemberCouponResponseDto> | null>(null)
  const [issuable, setIssuable] = useState<SpringPage<CouponResponseDto> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [issuingId, setIssuingId] = useState<number | null>(null)

  const fetchAll = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      const [ownedData, issuableData] = await Promise.all([
        api.coupons.getMyCoupons(0, 20),
        api.coupons.getIssuableCoupons(0, 20),
      ])
      setOwned(ownedData)
      setIssuable(issuableData)
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (memberId !== null) void fetchAll()
    else setLoading(false)
  }, [memberId, fetchAll])

  const issueCoupon = async (couponId: number) => {
    setIssuingId(couponId)
    try {
      await api.coupons.issueCoupon(couponId)
      // 발급 성공: 보유 탭 갱신 + 발급 목록 갱신 + 보유 탭으로 전환
      const [ownedData, issuableData] = await Promise.all([
        api.coupons.getMyCoupons(0, 20),
        api.coupons.getIssuableCoupons(0, 20),
      ])
      setOwned(ownedData)
      setIssuable(issuableData)
      setTab('owned')
    } catch {
      window.alert('쿠폰 발급에 실패했습니다.')
    } finally {
      setIssuingId(null)
    }
  }

  // 로그인 가드
  if (memberId === null) {
    return (
      <div className="flex min-h-screen flex-col bg-ink">
        <SlimHeader />
        <main className="flex flex-1 flex-col items-center justify-center gap-8 px-4 text-center sm:px-10">
          <h1
            className="font-display font-black uppercase leading-none text-gradient-heading"
            style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
          >
            Coupons
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

      <main className="px-4 pb-24 sm:px-10">
        <FadeIn>
          <div className="flex flex-wrap items-baseline gap-x-8 gap-y-2 pt-10">
            <h1
              className="font-display font-black uppercase leading-none text-gradient-heading"
              style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
            >
              Coupons
            </h1>
            <Link
              to="/my"
              className="text-sm font-light text-body/55 underline-offset-4 transition-opacity hover:opacity-70"
            >
              ← 마이페이지
            </Link>
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
              {t.key === 'owned' && owned && ` (${owned.totalElements})`}
              {t.key === 'issuable' && issuable && ` (${issuable.totalElements})`}
            </button>
          ))}
        </div>

        <div className="mt-10 max-w-[820px]">
          {loading ? (
            <CouponSkeleton />
          ) : error ? (
            <ErrorState onRetry={() => void fetchAll()} />
          ) : (
            <>
              {/* 보유 쿠폰 */}
              {tab === 'owned' &&
                (owned && owned.content.length > 0 ? (
                  <div className="flex flex-col gap-4">
                    {owned.content.map((coupon) => (
                      <OwnedCouponCard key={coupon.id} coupon={coupon} />
                    ))}
                  </div>
                ) : (
                  <EmptyState
                    message="보유 중인 쿠폰이 없습니다."
                    actionLabel="쿠폰 발급받기"
                    onAction={() => setTab('issuable')}
                  />
                ))}

              {/* 발급받기 */}
              {tab === 'issuable' &&
                (issuable && issuable.content.length > 0 ? (
                  <div className="flex flex-col gap-4">
                    {issuable.content.map((coupon) => (
                      <IssuableCouponCard
                        key={coupon.id}
                        coupon={coupon}
                        onIssue={() => void issueCoupon(coupon.id)}
                        issuing={issuingId === coupon.id}
                      />
                    ))}
                  </div>
                ) : (
                  <EmptyState message="현재 발급 가능한 쿠폰이 없습니다." />
                ))}
            </>
          )}
        </div>
      </main>

      <SiteFooter />
    </div>
  )
}
