import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import type { BookResponse, BookReviewResponse, SpringPage } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { BookCover } from '../components/BookCover'
import { Magnet } from '../components/motion/Magnet'
import { FadeIn } from '../components/motion/FadeIn'
import { ErrorState } from '../components/states'
import { formatDate, formatPrice, formatRating } from '../lib/format'
import { cartBadge } from '../store/cartBadge'

type TabKey = 'reviews' | 'about' | 'toc' | 'author'

const TABS: { key: TabKey; label: string }[] = [
  { key: 'reviews', label: '리뷰' },
  { key: 'about', label: '책 소개' },
  { key: 'toc', label: '목차' },
  { key: 'author', label: '저자' },
]

function Stars({ rating, size = 14 }: { rating: number; size?: number }) {
  return (
    <span aria-label={`별점 ${rating}점`} className="text-accent" style={{ fontSize: size, letterSpacing: 2 }}>
      {'★'.repeat(Math.round(rating))}
      <span className="opacity-30">{'★'.repeat(5 - Math.round(rating))}</span>
    </span>
  )
}

function ReviewCard({ review }: { review: BookReviewResponse }) {
  return (
    <article className="border border-[rgba(215,226,234,0.18)] p-6" style={{ borderRadius: 24 }}>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="text-sm font-medium text-body">{review.loginId}</span>
          <Stars rating={review.rating} />
        </div>
        <time className="text-xs font-light text-body/45">{formatDate(review.createdAt)}</time>
      </div>
      <p className="mt-3 text-sm font-light leading-relaxed text-body/80">{review.content}</p>
      {review.likeCount !== null && review.likeCount > 0 && (
        <p className="mt-3 text-xs text-body/45">도움돼요 {review.likeCount}</p>
      )}
    </article>
  )
}

function DetailSkeleton() {
  return (
    <div className="grid gap-16 px-10 pt-14 md:grid-cols-[320px_1fr]" aria-busy="true">
      <div className="skeleton" style={{ width: 320, height: 440, borderRadius: 20 }} />
      <div>
        <div className="skeleton h-4 w-40 rounded" />
        <div className="skeleton mt-6 h-20 w-4/5 rounded" />
        <div className="skeleton mt-6 h-4 w-56 rounded" />
        <div className="skeleton mt-10 h-8 w-36 rounded" />
        <div className="skeleton mt-8 h-24 w-full rounded" />
      </div>
    </div>
  )
}

export function BookDetailPage() {
  const { id } = useParams()
  const bookId = Number(id)
  const navigate = useNavigate()

  const [book, setBook] = useState<BookResponse | null>(null)
  const [reviews, setReviews] = useState<SpringPage<BookReviewResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [tab, setTab] = useState<TabKey>('reviews')
  const [liked, setLiked] = useState(false)
  const [cartAdded, setCartAdded] = useState(false)

  const fetchAll = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      const [bookData, reviewData] = await Promise.all([
        api.books.getBook(bookId),
        api.reviews.getReviews(bookId, 0, 10),
      ])
      setBook(bookData)
      setReviews(reviewData)
      // 비로그인 시 실패할 수 있으므로 조용히 무시
      api.books.getLikeStatus(bookId).then(setLiked).catch(() => undefined)
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [bookId])

  useEffect(() => {
    void fetchAll()
  }, [fetchAll])

  const addToCart = async () => {
    await api.cart.addItem(bookId, 1)
    cartBadge.increment()
    cartBadge.refresh()
    setCartAdded(true)
    setTimeout(() => setCartAdded(false), 2000)
  }

  const buyNow = async () => {
    await api.cart.addItem(bookId, 1)
    cartBadge.refresh()
    navigate('/checkout')
  }

  const toggleLike = () => {
    api.books
      .toggleLike(bookId)
      .then(setLiked)
      .catch(() => undefined)
  }

  const category = book?.categories?.[0]?.categoryName ?? '도서'

  return (
    <div className="min-h-screen bg-ink">
      <SlimHeader />

      {loading ? (
        <DetailSkeleton />
      ) : error || !book ? (
        <ErrorState onRetry={() => void fetchAll()} />
      ) : (
        <main className="px-4 pb-24 sm:px-10">
          <div className="grid gap-16 pt-14 md:grid-cols-[320px_1fr]">
            {/* 좌: 표지 (마그넷) */}
            <FadeIn>
              <Magnet>
                <BookCover
                  bookId={book.id}
                  title={book.title}
                  author={book.author}
                  imageUrl={book.imageUrl}
                  radius={20}
                  style={{ width: 320, height: 440 }}
                />
              </Magnet>
            </FadeIn>

            {/* 우: 정보 */}
            <div>
              <FadeIn delay={0.1}>
                <p className="text-xs font-medium uppercase text-body/55" style={{ letterSpacing: '0.2em' }}>
                  {category}
                  {book.pubDate && <> · {formatDate(book.pubDate)} 출간</>}
                  {book.publisher && <> · {book.publisher}</>}
                </p>
                <h1
                  className="mt-4 font-display font-black uppercase leading-[1.05] text-gradient-heading"
                  style={{ fontSize: 'clamp(3rem, 7vw, 96px)', letterSpacing: '-0.03em' }}
                >
                  {book.title}
                </h1>
                <div className="mt-5 flex items-center gap-4 text-sm text-body/70">
                  <span>{book.author}</span>
                  {book.avgRating !== null && (
                    <>
                      <Stars rating={book.avgRating} />
                      <span>
                        {formatRating(book.avgRating)} ({book.reviewCount ?? 0})
                      </span>
                    </>
                  )}
                </div>
                {/* 정가/할인율 필드는 백엔드 미지원 — 단일 price 표기 (계약 문서 참조) */}
                <p className="mt-8 font-medium text-body" style={{ fontSize: 38 }}>
                  {formatPrice(book.price)}
                </p>
                {book.content && (
                  <p className="mt-6 max-w-[560px] text-[15px] font-light leading-loose text-body/70">
                    {book.content}
                  </p>
                )}

                <div className="mt-10 flex flex-wrap items-center gap-4">
                  <button type="button" onClick={() => void buyNow()} className="btn-cta cursor-pointer">
                    바로 구매
                  </button>
                  <button type="button" onClick={() => void addToCart()} className="btn-ghost cursor-pointer">
                    {cartAdded ? '담았습니다 ✓' : '장바구니'}
                  </button>
                  <button
                    type="button"
                    onClick={toggleLike}
                    aria-pressed={liked}
                    aria-label="찜하기"
                    className={`flex cursor-pointer items-center justify-center rounded-full border-2 transition-colors duration-200 ${
                      liked ? 'border-accent text-accent' : 'border-body/60 text-body/60 hover:bg-[rgba(215,226,234,0.1)]'
                    }`}
                    style={{ width: 52, height: 52, fontSize: 20 }}
                  >
                    {liked ? '♥' : '♡'}
                  </button>
                </div>
              </FadeIn>
            </div>
          </div>

          {/* 하단 탭 */}
          <section className="mt-24">
            <div className="flex items-center justify-between border-b border-[rgba(215,226,234,0.18)]">
              <div className="flex gap-8">
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
                    {t.key === 'reviews' && book.reviewCount !== null && ` (${book.reviewCount})`}
                  </button>
                ))}
              </div>
              <Link
                to={`/review/new?bookId=${book.id}`}
                className="pb-4 text-sm font-light text-body/70 transition-opacity hover:opacity-70"
              >
                ＋ 리뷰 작성
              </Link>
            </div>

            <div className="mt-10">
              {tab === 'reviews' &&
                (reviews && reviews.content.length > 0 ? (
                  <div className="flex max-w-[820px] flex-col gap-4">
                    {reviews.content.map((review) => (
                      <ReviewCard key={review.reviewId} review={review} />
                    ))}
                  </div>
                ) : (
                  <p className="py-10 text-sm font-light text-body/55">
                    아직 리뷰가 없습니다. 첫 번째 리뷰를 남겨보세요.
                  </p>
                ))}
              {tab === 'about' && (
                <p className="max-w-[720px] text-[15px] font-light leading-loose text-body/80">
                  {book.aiSummary ?? book.content ?? '등록된 책 소개가 없습니다.'}
                </p>
              )}
              {tab === 'toc' && (
                <p className="py-10 text-sm font-light text-body/55">목차 정보가 아직 등록되지 않았습니다.</p>
              )}
              {tab === 'author' && (
                <div className="max-w-[720px]">
                  <p className="font-display text-xl font-semibold text-body">{book.author}</p>
                  <p className="mt-3 text-sm font-light leading-relaxed text-body/70">
                    저자 소개가 아직 등록되지 않았습니다.
                  </p>
                </div>
              )}
            </div>
          </section>
        </main>
      )}

      <SiteFooter />
    </div>
  )
}
