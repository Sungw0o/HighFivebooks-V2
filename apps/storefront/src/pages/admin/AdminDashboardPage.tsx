import { useEffect, useState } from 'react'
import { api } from '../../api'
import type { DailySalesResponse, PaymentStatsResponse } from '../../api'
import { AdminPageTitle } from './AdminLayout'
import { formatPrice } from '../../lib/format'

function StatCard({
  label,
  value,
  highlight = false,
}: {
  label: string
  value: string
  highlight?: boolean
}) {
  return (
    <div
      className="flex-1 p-6"
      style={
        highlight
          ? {
              borderRadius: 24,
              border: '2px solid #B600A8',
              background: 'linear-gradient(135deg, rgba(182,0,168,0.18) 0%, rgba(118,33,176,0.12) 100%)',
            }
          : { borderRadius: 24, border: '1px solid rgba(215,226,234,0.18)' }
      }
    >
      <p className="font-display text-3xl font-semibold text-body">{value}</p>
      <p className="mt-1 text-xs font-light uppercase text-body/55" style={{ letterSpacing: '0.16em' }}>
        {label}
      </p>
    </div>
  )
}

export function AdminDashboardPage() {
  const [stats, setStats] = useState<PaymentStatsResponse | null>(null)
  const [daily, setDaily] = useState<DailySalesResponse[]>([])
  const [error, setError] = useState(false)

  useEffect(() => {
    api.admin.getStatsSummary().then(setStats).catch(() => setError(true))
    api.admin.getDailySales().then(setDaily).catch(() => undefined)
  }, [])

  const today = daily.at(-1)
  const maxDaily = Math.max(1, ...daily.map((d) => d.dailyTotalAmount))

  return (
    <div>
      <AdminPageTitle>Dashboard</AdminPageTitle>

      {error && (
        <p role="alert" className="mt-6 text-sm text-accent">
          통계를 불러오지 못했습니다.
        </p>
      )}

      {/* 통계 카드 4개 — 오늘 주문 카드는 마젠타 보더 + 그라디언트 배경 */}
      <div className="mt-10 flex flex-col gap-5 lg:flex-row">
        <StatCard label="오늘 주문" value={today ? `${today.dailyCount}건` : '—'} highlight />
        <StatCard label="오늘 매출" value={today ? formatPrice(today.dailyTotalAmount) : '—'} />
        <StatCard label="순 매출 (누적)" value={stats ? formatPrice(stats.netSalesAmount) : '—'} />
        <StatCard label="결제 성공 / 취소" value={stats ? `${stats.successCount} / ${stats.cancelCount}` : '—'} />
      </div>

      {/* 최근 7일 매출 미니 차트 */}
      {daily.length > 0 && (
        <section className="mt-12">
          <h2 className="text-xs font-medium uppercase text-body/55" style={{ letterSpacing: '0.2em' }}>
            최근 일별 매출
          </h2>
          <div className="mt-5 flex h-40 items-end gap-3">
            {daily.map((d) => (
              <div key={d.date} className="flex flex-1 flex-col items-center gap-2">
                <span className="text-[10px] font-light text-body/55">{formatPrice(d.dailyTotalAmount)}</span>
                <div
                  className="w-full rounded-t-lg"
                  style={{
                    height: `${Math.max(6, (d.dailyTotalAmount / maxDaily) * 100)}%`,
                    background: 'linear-gradient(180deg, #B600A8 0%, #7621B0 100%)',
                  }}
                  aria-label={`${d.date} 매출 ${formatPrice(d.dailyTotalAmount)}`}
                />
                <span className="text-[10px] font-light text-body/45">{d.date.slice(5)}</span>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* 재고 부족 리스트: 전용 API 없음 + BookResponse에 stock 미노출 (계약 문서 '추가 필요' 항목) */}
      <section className="mt-12">
        <h2 className="text-xs font-medium uppercase text-body/55" style={{ letterSpacing: '0.2em' }}>
          재고 부족
        </h2>
        <p className="mt-4 text-sm font-light text-body/55">
          재고 부족 목록 API가 아직 없습니다. <span className="text-accent">book-server에 재고 노출 API 추가 필요</span>{' '}
          (docs/STOREFRONT_API_CONTRACT.md 참조).
        </p>
      </section>
    </div>
  )
}
