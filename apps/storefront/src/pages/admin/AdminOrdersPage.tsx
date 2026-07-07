import { useCallback, useEffect, useState } from 'react'
import { api } from '../../api'
import type { CommonPageResponse, DeliveryStatus, OrderResponse } from '../../api'
import { AdminPageTitle } from './AdminLayout'
import { EmptyState, ErrorState } from '../../components/states'
import { formatDate, formatPrice } from '../../lib/format'

const PAGE_SIZE = 10

/** order-server DeliveryStatus 원형 */
const STATUS_OPTIONS: { value: DeliveryStatus; label: string }[] = [
  { value: 'PAYMENT_WAITING', label: '결제 대기' },
  { value: 'PREPARING', label: '배송 준비 중' },
  { value: 'DELIVERING', label: '배송 중' },
  { value: 'DELIVERY_COMPLETED', label: '배송 완료' },
  { value: 'PURCHASE_CONFIRMED', label: '구매 확정' },
  { value: 'CANCELED', label: '주문 취소' },
  { value: 'RETURN_REQUESTED', label: '반품 요청' },
  { value: 'RETURN_COMPLETED', label: '반품 완료' },
]

const LABELS: Record<string, string> = Object.fromEntries(STATUS_OPTIONS.map((s) => [s.value, s.label]))

export function AdminOrdersPage() {
  const [statusFilter, setStatusFilter] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<CommonPageResponse<OrderResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const fetchOrders = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      setResult(await api.admin.getOrders(page, PAGE_SIZE, statusFilter ?? undefined))
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [page, statusFilter])

  useEffect(() => {
    void fetchOrders()
  }, [fetchOrders])

  const changeStatus = async (order: OrderResponse, status: DeliveryStatus) => {
    let trackingNumber: string | null = null
    if (status === 'DELIVERING') {
      trackingNumber = window.prompt('송장 번호를 입력하세요 (선택)', order.trackingNumber || '') ?? null
    }
    try {
      await api.admin.updateOrderStatus(order.id, { status, trackingNumber })
      await fetchOrders()
    } catch {
      window.alert('상태 변경에 실패했습니다.')
    }
  }

  return (
    <div>
      <AdminPageTitle>Orders</AdminPageTitle>

      {/* 상태 필터 칩 */}
      <div className="mt-8 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => {
            setStatusFilter(null)
            setPage(0)
          }}
          aria-pressed={statusFilter === null}
          className={`cursor-pointer rounded-full border px-4 py-1.5 text-xs font-medium transition-colors ${
            statusFilter === null
              ? 'border-body bg-body text-ink'
              : 'border-[rgba(215,226,234,0.3)] text-body hover:bg-[rgba(215,226,234,0.1)]'
          }`}
        >
          전체
        </button>
        {STATUS_OPTIONS.map((s) => (
          <button
            key={s.value}
            type="button"
            onClick={() => {
              setStatusFilter(s.value)
              setPage(0)
            }}
            aria-pressed={statusFilter === s.value}
            className={`cursor-pointer rounded-full border px-4 py-1.5 text-xs font-medium transition-colors ${
              statusFilter === s.value
                ? 'border-body bg-body text-ink'
                : 'border-[rgba(215,226,234,0.3)] text-body hover:bg-[rgba(215,226,234,0.1)]'
            }`}
          >
            {s.label}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="mt-10 flex flex-col gap-3" aria-busy="true">
          {Array.from({ length: 5 }, (_, i) => (
            <div key={i} className="skeleton h-16" style={{ borderRadius: 20 }} />
          ))}
        </div>
      ) : error ? (
        <ErrorState onRetry={() => void fetchOrders()} />
      ) : !result || result.data.length === 0 ? (
        <EmptyState message="조건에 맞는 주문이 없습니다." />
      ) : (
        <>
          <ul className="mt-10 flex flex-col gap-3">
            {result.data.map((order) => (
              <li
                key={order.id}
                className="flex flex-wrap items-center gap-5 border border-[rgba(215,226,234,0.18)] px-5 py-4"
                style={{ borderRadius: 20 }}
              >
                <span className="w-20 text-sm font-medium text-body">#{order.id}</span>
                <span className="w-24 truncate text-xs font-light text-body/55">
                  {order.userId !== null ? `회원 ${order.userId}` : '비회원'}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-light text-body/90">{order.orderName}</p>
                  <p className="text-xs font-light text-body/45">
                    {formatDate(order.orderDate)}
                    {order.trackingNumber && ` · 송장 ${order.trackingNumber}`}
                  </p>
                </div>
                <span className="w-24 text-right text-sm text-body">{formatPrice(order.totalPrice)}</span>
                <span
                  className={`rounded-full border px-4 py-1.5 text-xs font-medium ${
                    order.status === 'DELIVERING'
                      ? 'border-accent text-accent'
                      : 'border-[rgba(215,226,234,0.3)] text-body/70'
                  }`}
                >
                  {LABELS[order.status] ?? order.status}
                </span>
                <select
                  value=""
                  onChange={(e) => {
                    if (e.target.value) void changeStatus(order, e.target.value as DeliveryStatus)
                  }}
                  aria-label={`주문 ${order.id} 상태 변경`}
                  className="cursor-pointer rounded-full border border-[rgba(215,226,234,0.3)] bg-transparent px-3 py-1.5 text-xs text-body/70 focus:outline-none"
                >
                  <option value="" className="bg-ink">
                    상태 변경 ▾
                  </option>
                  {STATUS_OPTIONS.filter((s) => s.value !== order.status).map((s) => (
                    <option key={s.value} value={s.value} className="bg-ink">
                      {s.label}
                    </option>
                  ))}
                </select>
              </li>
            ))}
          </ul>

          {result.totalPages > 1 && (
            <div className="mt-8 flex items-center justify-center gap-4 text-sm text-body/70">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
                className="cursor-pointer transition-opacity hover:opacity-70 disabled:opacity-30"
              >
                ← 이전
              </button>
              <span>
                {page + 1} / {result.totalPages}
              </span>
              <button
                type="button"
                disabled={result.isLast}
                onClick={() => setPage((p) => p + 1)}
                className="cursor-pointer transition-opacity hover:opacity-70 disabled:opacity-30"
              >
                다음 →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
