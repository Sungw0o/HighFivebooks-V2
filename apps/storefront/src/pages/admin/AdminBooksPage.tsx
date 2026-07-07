import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../api'
import type { BookResponse, SpringPage } from '../../api'
import { AdminPageTitle } from './AdminLayout'
import { BookCover } from '../../components/BookCover'
import { ErrorState } from '../../components/states'
import { formatPrice } from '../../lib/format'

const PAGE_SIZE = 10

export function AdminBooksPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<SpringPage<BookResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const fetchBooks = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      setResult(await api.admin.getBooks(page, PAGE_SIZE))
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    void fetchBooks()
  }, [fetchBooks])

  const removeBook = async (book: BookResponse) => {
    if (!window.confirm(`'${book.title}' 도서를 삭제할까요?`)) return
    try {
      await api.admin.deleteBook(book.id)
      await fetchBooks()
    } catch {
      window.alert('삭제에 실패했습니다.')
    }
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <AdminPageTitle>Books</AdminPageTitle>
        <Link to="/admin/books/new" className="btn-cta px-8 py-3 text-sm">
          ＋ 도서 등록
        </Link>
      </div>

      {loading ? (
        <div className="mt-10 flex flex-col gap-3" aria-busy="true">
          {Array.from({ length: 5 }, (_, i) => (
            <div key={i} className="skeleton h-20" style={{ borderRadius: 20 }} />
          ))}
        </div>
      ) : error ? (
        <ErrorState onRetry={() => void fetchBooks()} />
      ) : (
        <>
          <ul className="mt-10 flex flex-col gap-3">
            {result?.content.map((book) => (
              <li
                key={book.id}
                className="flex items-center gap-5 border border-[rgba(215,226,234,0.18)] px-5 py-4"
                style={{ borderRadius: 20 }}
              >
                <BookCover
                  bookId={book.id}
                  title={book.title}
                  imageUrl={book.imageUrl}
                  radius={8}
                  style={{ width: 44, height: 60 }}
                />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm text-body">{book.title}</p>
                  <p className="mt-0.5 truncate text-xs font-light text-body/55">
                    {book.author} · {book.publisher ?? '-'}
                  </p>
                </div>
                <span className="w-24 text-right text-sm text-body">{formatPrice(book.price)}</span>
                {/* BookResponse에 stock 미노출 — 계약 문서 '추가 필요' 항목 */}
                <span className="w-16 text-right text-xs font-light text-body/45" title="재고 노출 API 필요">
                  재고 —
                </span>
                <span
                  className="rounded-full border border-[rgba(215,226,234,0.3)] px-4 py-1.5 text-xs text-body/70"
                  style={{ letterSpacing: '0.1em' }}
                >
                  판매중
                </span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => navigate(`/admin/books/${book.id}/edit`)}
                    className="cursor-pointer rounded-full border border-[rgba(215,226,234,0.3)] px-4 py-1.5 text-xs text-body/70 transition-opacity hover:opacity-70"
                  >
                    수정
                  </button>
                  <button
                    type="button"
                    onClick={() => void removeBook(book)}
                    className="cursor-pointer rounded-full border border-[rgba(215,226,234,0.3)] px-4 py-1.5 text-xs text-accent transition-opacity hover:opacity-70"
                  >
                    삭제
                  </button>
                </div>
              </li>
            ))}
          </ul>

          {result && result.totalPages > 1 && (
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
                disabled={result.last}
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
