import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api'
import type { BookResponse, BookSortType, CategoryResponse, SpringPage } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { BookCard } from '../components/BookCard'
import { BookGridSkeleton, EmptyState, ErrorState } from '../components/states'
import { FadeIn } from '../components/motion/FadeIn'

const PAGE_SIZE = 12

const SORT_OPTIONS: { label: string; value: BookSortType }[] = [
  { label: '인기', value: 'POPULAR' },
  { label: '신간', value: 'NEW' },
  { label: '평점', value: 'RATING' },
  { label: '리뷰 많은', value: 'REVIEW' },
  { label: '낮은 가격', value: 'LOW_PRICE' },
  { label: '높은 가격', value: 'HIGH_PRICE' },
]

const PRICE_RANGES: { label: string; min: number; max: number }[] = [
  { label: '1만원 미만', min: 0, max: 9999 },
  { label: '1~2만원', min: 10000, max: 19999 },
  { label: '2~3만원', min: 20000, max: 29999 },
  { label: '3만원 이상', min: 30000, max: Number.MAX_SAFE_INTEGER },
]

/** 서버 목록 API에 정렬 파라미터가 없어(검색 API만 지원) 현재 페이지 기준 클라이언트 정렬로 보완 */
function sortLocally(books: BookResponse[], sort: BookSortType): BookResponse[] {
  const copy = [...books]
  switch (sort) {
    case 'NEW':
      return copy.sort((a, b) => (b.pubDate ?? '').localeCompare(a.pubDate ?? ''))
    case 'LOW_PRICE':
      return copy.sort((a, b) => a.price - b.price)
    case 'HIGH_PRICE':
      return copy.sort((a, b) => b.price - a.price)
    case 'RATING':
      return copy.sort((a, b) => (b.avgRating ?? 0) - (a.avgRating ?? 0))
    case 'REVIEW':
    case 'POPULAR':
    default:
      return copy.sort((a, b) => (b.reviewCount ?? 0) - (a.reviewCount ?? 0))
  }
}

function Chip({ label, selected, onClick }: { label: string; selected: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={selected}
      className={`cursor-pointer rounded-full border px-5 py-2 text-xs font-medium uppercase transition-colors duration-200 ${
        selected
          ? 'border-body bg-body text-ink'
          : 'border-[rgba(215,226,234,0.3)] text-body hover:bg-[rgba(215,226,234,0.1)]'
      }`}
      style={{ letterSpacing: '0.12em' }}
    >
      {label}
    </button>
  )
}

export function BooksPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = searchParams.get('keyword') ?? ''
  const categoryId = Number(searchParams.get('category')) || null
  const sort = (searchParams.get('sort') ?? 'POPULAR') as BookSortType
  const page = Number(searchParams.get('page')) || 0

  const [result, setResult] = useState<SpringPage<BookResponse> | null>(null)
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  // 클라이언트 필터 (백엔드에 가격대/출판사 필터 없음 — 계약 문서 '추가 필요' 항목)
  const [priceRangeIdx, setPriceRangeIdx] = useState<number | null>(null)
  const [publisherFilter, setPublisherFilter] = useState<string | null>(null)

  const setParam = (patch: Record<string, string | null>) => {
    const next = new URLSearchParams(searchParams)
    for (const [key, value] of Object.entries(patch)) {
      if (value === null) next.delete(key)
      else next.set(key, value)
    }
    setSearchParams(next)
  }

  const fetchBooks = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      let data: SpringPage<BookResponse>
      if (keyword) {
        data = await api.books.search(keyword, sort, page, PAGE_SIZE)
      } else if (categoryId) {
        data = await api.books.getBooksByCategory(categoryId, page, PAGE_SIZE)
        data = { ...data, content: sortLocally(data.content, sort) }
      } else {
        data = await api.books.getBooks(page, PAGE_SIZE)
        data = { ...data, content: sortLocally(data.content, sort) }
      }
      setResult(data)
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [keyword, categoryId, sort, page])

  useEffect(() => {
    void fetchBooks()
  }, [fetchBooks])

  useEffect(() => {
    api.books.getParentCategories().then(setCategories).catch(() => setCategories([]))
  }, [])

  const publishers = useMemo(() => {
    const set = new Set<string>()
    result?.content.forEach((b) => {
      if (b.publisher) set.add(b.publisher)
    })
    return [...set].sort()
  }, [result])

  const filteredBooks = useMemo(() => {
    let books = result?.content ?? []
    if (priceRangeIdx !== null) {
      const range = PRICE_RANGES[priceRangeIdx]
      books = books.filter((b) => b.price >= range.min && b.price <= range.max)
    }
    if (publisherFilter) {
      books = books.filter((b) => b.publisher === publisherFilter)
    }
    return books
  }, [result, priceRangeIdx, publisherFilter])

  const filterActive = priceRangeIdx !== null || publisherFilter !== null
  const totalPages = result?.totalPages ?? 0

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
              {keyword ? 'Search' : 'Books'}
            </h1>
            {!loading && result && (
              <p className="text-sm font-light text-body/55">
                {keyword && <>&ldquo;{keyword}&rdquo; </>}
                {filterActive
                  ? `${filteredBooks.length}권 (현재 페이지 필터 기준)`
                  : `${result.totalElements.toLocaleString('ko-KR')}권`}
              </p>
            )}
          </div>
        </FadeIn>

        {/* 정렬 / 카테고리 칩 */}
        <div className="mt-10 flex flex-wrap items-center gap-2">
          {SORT_OPTIONS.map((option) => (
            <Chip
              key={option.value}
              label={option.label}
              selected={sort === option.value}
              onClick={() => setParam({ sort: option.value, page: null })}
            />
          ))}
          <span aria-hidden="true" className="mx-3 h-5 w-px bg-[rgba(215,226,234,0.25)]" />
          <Chip label="전체" selected={categoryId === null} onClick={() => setParam({ category: null, page: null })} />
          {categories.map((category) => (
            <Chip
              key={category.categoryId}
              label={category.categoryName}
              selected={categoryId === category.categoryId}
              onClick={() => setParam({ category: String(category.categoryId), page: null })}
            />
          ))}
        </div>

        <div className="mt-12 grid gap-12 lg:grid-cols-[200px_1fr]">
          {/* 필터 사이드바 (와이어프레임 2d) — 서버 미지원으로 현재 페이지 클라이언트 필터 */}
          <details className="lg:open" open>
            <summary className="cursor-pointer list-none text-xs font-medium uppercase text-body/55 lg:hidden" style={{ letterSpacing: '0.2em' }}>
              필터 열기 ▾
            </summary>
            <aside className="mt-4 flex flex-col gap-10 lg:mt-0">
            <section>
              <h2 className="text-xs font-medium uppercase text-body/55" style={{ letterSpacing: '0.2em' }}>
                가격대
              </h2>
              <ul className="mt-4 flex flex-col gap-2.5">
                {PRICE_RANGES.map((range, i) => (
                  <li key={range.label}>
                    <label className="flex cursor-pointer items-center gap-2.5 text-sm font-light text-body/80">
                      <input
                        type="checkbox"
                        checked={priceRangeIdx === i}
                        onChange={() => setPriceRangeIdx(priceRangeIdx === i ? null : i)}
                        className="accent-[#BE4C00]"
                      />
                      {range.label}
                    </label>
                  </li>
                ))}
              </ul>
            </section>

            {publishers.length > 0 && (
              <section>
                <h2 className="text-xs font-medium uppercase text-body/55" style={{ letterSpacing: '0.2em' }}>
                  출판사
                </h2>
                <ul className="mt-4 flex flex-col gap-2.5">
                  {publishers.map((publisher) => (
                    <li key={publisher}>
                      <label className="flex cursor-pointer items-center gap-2.5 text-sm font-light text-body/80">
                        <input
                          type="checkbox"
                          checked={publisherFilter === publisher}
                          onChange={() => setPublisherFilter(publisherFilter === publisher ? null : publisher)}
                          className="accent-[#BE4C00]"
                        />
                        {publisher}
                      </label>
                    </li>
                  ))}
                </ul>
              </section>
            )}
            </aside>
          </details>

          {/* 결과 영역 */}
          <div>
            {loading ? (
              <BookGridSkeleton count={PAGE_SIZE} />
            ) : error ? (
              <ErrorState onRetry={() => void fetchBooks()} />
            ) : filteredBooks.length === 0 ? (
              <EmptyState
                message={keyword ? `"${keyword}"에 대한 검색 결과가 없습니다.` : '조건에 맞는 책이 없습니다.'}
                actionLabel="전체 보기"
                onAction={() => {
                  setPriceRangeIdx(null)
                  setPublisherFilter(null)
                  setSearchParams({})
                }}
              />
            ) : (
              <>
                <div
                  className="grid gap-7"
                  style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}
                >
                  {filteredBooks.map((book) => (
                    <BookCard key={book.id} book={book} />
                  ))}
                </div>

                {/* 페이지네이션 */}
                {totalPages > 1 && (
                  <nav aria-label="페이지" className="mt-16 flex items-center justify-center gap-2">
                    <button
                      type="button"
                      disabled={page === 0}
                      onClick={() => setParam({ page: String(page - 1) })}
                      className="cursor-pointer rounded-full border border-[rgba(215,226,234,0.3)] px-4 py-2 text-xs text-body transition-opacity hover:opacity-70 disabled:cursor-default disabled:opacity-30"
                    >
                      ←
                    </button>
                    {Array.from({ length: totalPages }, (_, i) => i)
                      .slice(Math.max(0, Math.min(page - 3, totalPages - 7)), Math.max(7, page + 4))
                      .map((p) => (
                        <button
                          key={p}
                          type="button"
                          aria-current={p === page ? 'page' : undefined}
                          onClick={() => setParam({ page: String(p) })}
                          className={`cursor-pointer rounded-full px-4 py-2 text-xs transition-colors ${
                            p === page
                              ? 'bg-body font-semibold text-ink'
                              : 'border border-[rgba(215,226,234,0.3)] text-body hover:bg-[rgba(215,226,234,0.1)]'
                          }`}
                        >
                          {p + 1}
                        </button>
                      ))}
                    <button
                      type="button"
                      disabled={page >= totalPages - 1}
                      onClick={() => setParam({ page: String(page + 1) })}
                      className="cursor-pointer rounded-full border border-[rgba(215,226,234,0.3)] px-4 py-2 text-xs text-body transition-opacity hover:opacity-70 disabled:cursor-default disabled:opacity-30"
                    >
                      →
                    </button>
                  </nav>
                )}
              </>
            )}
          </div>
        </div>
      </main>

      <SiteFooter />
    </div>
  )
}
