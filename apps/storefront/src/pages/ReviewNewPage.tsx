import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api, tokenStore } from '../api'
import type { BookResponse } from '../api'
import { SlimHeader } from '../components/layout/SlimHeader'
import { SiteFooter } from '../components/layout/SiteFooter'
import { BookCover } from '../components/BookCover'
import { FadeIn } from '../components/motion/FadeIn'
import { formatPrice } from '../lib/format'

const MAX_IMAGES = 3
// 백엔드 ReviewCreateRequest 규칙: content 10~1000자, 제목 필드 없음 (계약 문서 참조)
const MIN_LEN = 10
const MAX_LEN = 1000

function CenteredNotice({ heading, message, action }: { heading: string; message: string; action?: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col bg-ink">
      <SlimHeader />
      <main className="flex flex-1 flex-col items-center justify-center gap-8 px-10 text-center">
        <h1
          className="font-display font-black uppercase leading-none text-gradient-heading"
          style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
        >
          {heading}
        </h1>
        <p className="font-light text-body/70">{message}</p>
        {action}
      </main>
      <SiteFooter />
    </div>
  )
}

export function ReviewNewPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const bookId = Number(searchParams.get('bookId')) || null
  const memberId = tokenStore.memberId()

  const [book, setBook] = useState<BookResponse | null>(null)
  const [purchased, setPurchased] = useState<boolean | null>(null)
  const [rating, setRating] = useState(0)
  const [hoverRating, setHoverRating] = useState(0)
  const [content, setContent] = useState('')
  const [images, setImages] = useState<File[]>([])
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (bookId === null || memberId === null) return
    api.books.getBook(bookId).then(setBook).catch(() => setBook(null))
    api.orders
      .checkPurchase(memberId, bookId)
      .then(setPurchased)
      .catch(() => setPurchased(null)) // 확인 실패 시 서버 검증에 위임
  }, [bookId, memberId])

  if (memberId === null) {
    return (
      <CenteredNotice
        heading="Review"
        message="리뷰 작성은 로그인이 필요합니다."
        action={
          <Link to="/login" className="btn-cta">
            로그인
          </Link>
        }
      />
    )
  }

  if (bookId === null) {
    return (
      <CenteredNotice
        heading="Review"
        message="리뷰를 작성할 책을 먼저 선택해주세요. 책 상세 화면의 '＋ 리뷰 작성'으로 진입합니다."
        action={
          <Link to="/books" className="btn-ghost">
            책 둘러보기
          </Link>
        }
      />
    )
  }

  const submit = async () => {
    setError(null)
    if (rating < 1) return setError('별점을 선택해주세요.')
    const trimmed = content.trim()
    if (trimmed.length < MIN_LEN || trimmed.length > MAX_LEN)
      return setError(`리뷰 내용은 ${MIN_LEN}자 이상 ${MAX_LEN}자 이하여야 합니다.`)

    setSubmitting(true)
    try {
      await api.reviews.createReview(bookId, { rating, content: trimmed }, images.length > 0 ? images : undefined)
      navigate(`/books/${bookId}`) // 상세 리뷰 탭(기본 탭)으로 복귀
    } catch {
      setError('리뷰 등록에 실패했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setSubmitting(false)
    }
  }

  const onPickImages = (files: FileList | null) => {
    if (!files) return
    setImages((prev) => [...prev, ...Array.from(files)].slice(0, MAX_IMAGES))
  }

  const shown = hoverRating || rating

  return (
    <div className="min-h-screen bg-ink">
      <SlimHeader />

      <main className="mx-auto max-w-[640px] px-10 pb-24">
        <FadeIn>
          <h1
            className="pt-10 font-display font-black uppercase leading-none text-gradient-heading"
            style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
          >
            Review
          </h1>
        </FadeIn>

        {/* 대상 도서 카드 */}
        {book && (
          <FadeIn delay={0.1}>
            <div
              className="mt-10 flex items-center gap-5 border border-[rgba(215,226,234,0.18)] p-5"
              style={{ borderRadius: 24 }}
            >
              <BookCover
                bookId={book.id}
                title={book.title}
                imageUrl={book.imageUrl}
                radius={10}
                style={{ width: 56, height: 76 }}
              />
              <div className="min-w-0">
                <p className="truncate text-sm font-normal text-body">{book.title}</p>
                <p className="mt-0.5 text-xs font-light text-body/55">
                  {book.author} · {formatPrice(book.price)}
                </p>
              </div>
            </div>
          </FadeIn>
        )}

        {purchased === false && (
          <p className="mt-4 text-xs text-accent">구매 이력이 확인되지 않았습니다. 구매한 책만 리뷰를 남길 수 있습니다.</p>
        )}

        <FadeIn delay={0.2}>
          {/* 별점 */}
          <div className="mt-10" role="radiogroup" aria-label="별점">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                type="button"
                role="radio"
                aria-checked={rating === star}
                aria-label={`${star}점`}
                onClick={() => setRating(star)}
                onMouseEnter={() => setHoverRating(star)}
                onMouseLeave={() => setHoverRating(0)}
                className="cursor-pointer transition-opacity hover:opacity-80"
                style={{ fontSize: 30, color: star <= shown ? '#BE4C00' : 'rgba(215,226,234,0.25)' }}
              >
                ★
              </button>
            ))}
            {rating > 0 && <span className="ml-3 align-middle text-sm text-body/55">{rating}점</span>}
          </div>

          {/* 본문 */}
          <div className="mt-6">
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={8}
              maxLength={MAX_LEN}
              placeholder={`이 책은 어땠나요? (${MIN_LEN}자 이상)`}
              aria-label="리뷰 내용"
              className="w-full resize-y rounded-3xl border border-[rgba(215,226,234,0.3)] bg-transparent px-6 py-5 text-sm font-light leading-relaxed text-body placeholder:text-body/40 focus:border-body/70 focus:outline-none"
            />
            <p className="mt-1 text-right text-xs font-light text-body/45">
              {content.trim().length} / {MAX_LEN}
            </p>
          </div>

          {/* 이미지 첨부 (백엔드 multipart images 지원) */}
          <div className="mt-4">
            <label className="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[rgba(215,226,234,0.3)] px-5 py-2 text-xs font-medium uppercase text-body/70 transition-opacity hover:opacity-70">
              사진 첨부 ({images.length}/{MAX_IMAGES})
              <input
                type="file"
                accept="image/*"
                multiple
                className="hidden"
                onChange={(e) => onPickImages(e.target.files)}
              />
            </label>
            {images.length > 0 && (
              <ul className="mt-3 flex flex-wrap gap-2">
                {images.map((file, i) => (
                  <li
                    key={`${file.name}-${i}`}
                    className="flex items-center gap-2 rounded-full border border-[rgba(215,226,234,0.18)] px-4 py-1.5 text-xs font-light text-body/70"
                  >
                    <span className="max-w-40 truncate">{file.name}</span>
                    <button
                      type="button"
                      onClick={() => setImages((prev) => prev.filter((_, idx) => idx !== i))}
                      aria-label={`${file.name} 제거`}
                      className="cursor-pointer text-body/45 hover:opacity-70"
                    >
                      ✕
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {error && (
            <p role="alert" className="mt-4 text-xs text-accent">
              {error}
            </p>
          )}

          <div className="mt-10 flex gap-4">
            <button
              type="button"
              onClick={() => void submit()}
              disabled={submitting}
              className="btn-cta cursor-pointer disabled:opacity-60"
            >
              {submitting ? '등록 중…' : '등록'}
            </button>
            <button type="button" onClick={() => navigate(-1)} className="btn-ghost cursor-pointer">
              취소
            </button>
          </div>
        </FadeIn>
      </main>

      <SiteFooter />
    </div>
  )
}
