import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { CartListResponse, DeliveryPolicyResponse } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { BookCover } from '../components/BookCover'
import { EmptyState, ErrorState } from '../components/states'
import { FadeIn } from '../components/motion/FadeIn'
import { formatPrice } from '../lib/format'

function CartSkeleton() {
  return (
    <div className="mt-12 grid gap-10 lg:grid-cols-[1fr_360px]" aria-busy="true">
      <div className="flex flex-col gap-4">
        {Array.from({ length: 3 }, (_, i) => (
          <div key={i} className="skeleton h-32" style={{ borderRadius: 32 }} />
        ))}
      </div>
      <div className="skeleton h-72" style={{ borderRadius: 40 }} />
    </div>
  )
}

export function CartPage() {
  const navigate = useNavigate()
  const [cart, setCart] = useState<CartListResponse | null>(null)
  const [policy, setPolicy] = useState<DeliveryPolicyResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [busy, setBusy] = useState(false)

  const fetchCart = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      setCart(await api.cart.getCart())
      api.orders.getDeliveryPolicy().then(setPolicy).catch(() => undefined)
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void fetchCart()
  }, [fetchCart])

  const mutate = async (action: () => Promise<void>) => {
    if (busy) return
    setBusy(true)
    try {
      await action()
      setCart(await api.cart.getCart())
    } catch {
      setError(true)
    } finally {
      setBusy(false)
    }
  }

  const updateQuantity = (bookId: number, quantity: number) => {
    if (quantity < 1) return // 최소 1
    void mutate(() => api.cart.updateItem(bookId, quantity))
  }

  const removeItem = (bookId: number) => void mutate(() => api.cart.removeItem(bookId))

  const itemsTotal = cart?.totalCartPrice ?? 0
  const shippingFee =
    policy === null ? 0 : itemsTotal >= policy.minOrderAmount ? 0 : policy.standardShippingFee
  const grandTotal = itemsTotal + shippingFee

  return (
    <div className="min-h-screen bg-ink">
      <SlimHeader />

      <main className="px-10 pb-24">
        <FadeIn>
          <h1
            className="pt-10 font-display font-black uppercase leading-none text-gradient-heading"
            style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
          >
            Cart
          </h1>
        </FadeIn>

        {loading ? (
          <CartSkeleton />
        ) : error ? (
          <ErrorState onRetry={() => void fetchCart()} />
        ) : !cart || cart.items.length === 0 ? (
          <EmptyState
            message="장바구니가 비어 있습니다."
            actionLabel="책 둘러보기"
            onAction={() => navigate('/books')}
          />
        ) : (
          <div className="mt-12 grid items-start gap-10 lg:grid-cols-[1fr_360px]">
            {/* 아이템 리스트 */}
            <ul className="flex flex-col gap-4">
              {cart.items.map((item) => (
                <li
                  key={item.bookId}
                  className="flex items-center gap-6 border border-[rgba(215,226,234,0.18)] p-6"
                  style={{ borderRadius: 32 }}
                >
                  <BookCover
                    bookId={item.bookId}
                    title={item.title}
                    imageUrl={item.image}
                    radius={10}
                    style={{ width: 64, height: 88 }}
                  />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-normal text-body">{item.title}</p>
                    <p className="mt-1 text-xs font-light text-body/55">{formatPrice(item.price)}</p>
                  </div>

                  {/* 수량 스테퍼 pill */}
                  <div
                    className="flex items-center gap-4 rounded-full border border-[rgba(215,226,234,0.3)] px-4 py-2"
                    aria-label={`${item.title} 수량`}
                  >
                    <button
                      type="button"
                      onClick={() => updateQuantity(item.bookId, item.quantity - 1)}
                      disabled={busy || item.quantity <= 1}
                      aria-label="수량 줄이기"
                      className="cursor-pointer text-body transition-opacity hover:opacity-70 disabled:cursor-default disabled:opacity-30"
                    >
                      −
                    </button>
                    <span className="min-w-6 text-center text-sm text-body">{item.quantity}</span>
                    <button
                      type="button"
                      onClick={() => updateQuantity(item.bookId, item.quantity + 1)}
                      disabled={busy}
                      aria-label="수량 늘리기"
                      className="cursor-pointer text-body transition-opacity hover:opacity-70 disabled:opacity-30"
                    >
                      ＋
                    </button>
                  </div>

                  <span className="w-24 text-right text-sm font-medium text-body">
                    {formatPrice(item.totalPrice)}
                  </span>
                  <button
                    type="button"
                    onClick={() => removeItem(item.bookId)}
                    disabled={busy}
                    aria-label={`${item.title} 삭제`}
                    className="cursor-pointer text-body/45 transition-opacity hover:opacity-70"
                  >
                    ✕
                  </button>
                </li>
              ))}
            </ul>

            {/* 주문 요약 */}
            <aside className="border-2 border-body p-8" style={{ borderRadius: 40 }}>
              <h2 className="text-xs font-medium uppercase text-body/55" style={{ letterSpacing: '0.2em' }}>
                주문 요약
              </h2>
              <dl className="mt-6 flex flex-col gap-3 text-sm font-light text-body/80">
                <div className="flex justify-between">
                  <dt>상품 금액</dt>
                  <dd>{formatPrice(itemsTotal)}</dd>
                </div>
                <div className="flex justify-between">
                  <dt>배송비</dt>
                  <dd>
                    {shippingFee === 0 ? '무료' : formatPrice(shippingFee)}
                    {policy && shippingFee > 0 && (
                      <span className="ml-2 text-xs text-body/45">
                        ({formatPrice(policy.minOrderAmount)} 이상 무료)
                      </span>
                    )}
                  </dd>
                </div>
              </dl>
              <div className="mt-6 flex items-baseline justify-between border-t border-[rgba(215,226,234,0.18)] pt-5">
                <span className="text-sm text-body/70">총 결제 금액</span>
                <span className="font-semibold text-body" style={{ fontSize: 24 }}>
                  {formatPrice(grandTotal)}
                </span>
              </div>
              <button
                type="button"
                onClick={() => navigate('/checkout')}
                className="btn-cta mt-8 w-full cursor-pointer text-center"
              >
                주문하기
              </button>
            </aside>
          </div>
        )}
      </main>

      <SiteFooter />
    </div>
  )
}
