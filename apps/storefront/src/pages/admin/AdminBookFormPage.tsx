import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../../api'
import type { BookInfoDto, CategoryResponse } from '../../api'
import { AdminPageTitle } from './AdminLayout'

const inputClass =
  'w-full rounded-2xl border border-[rgba(215,226,234,0.3)] bg-transparent px-5 py-3 text-sm font-light text-body placeholder:text-body/40 focus:border-body/70 focus:outline-none'

const EMPTY: BookInfoDto = {
  isbn: '',
  title: '',
  authors: [],
  publisher: '',
  publishedDate: '',
  price: 0,
  image: null,
  description: null,
  categoryId: null,
}

/** 도서 등록(/admin/books/new)과 수정(/admin/books/:id/edit) 공용 폼 */
export function AdminBookFormPage() {
  const { id } = useParams()
  const bookId = Number(id) || null
  const isEdit = bookId !== null
  const navigate = useNavigate()

  const [form, setForm] = useState<BookInfoDto>(EMPTY)
  const [authorsText, setAuthorsText] = useState('')
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    api.books.getParentCategories().then(setCategories).catch(() => setCategories([]))
    if (bookId !== null) {
      api.books
        .getBook(bookId)
        .then((book) => {
          setForm({
            isbn: book.isbn,
            title: book.title,
            authors: book.author.split(',').map((a) => a.trim()),
            publisher: book.publisher ?? '',
            publishedDate: book.pubDate ?? '',
            price: book.price,
            image: book.imageUrl,
            description: book.content,
            categoryId: book.categoryId,
          })
          setAuthorsText(book.author)
        })
        .catch(() => setError('도서 정보를 불러오지 못했습니다.'))
    }
  }, [bookId])

  const set = <K extends keyof BookInfoDto>(key: K, value: BookInfoDto[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const lookupIsbn = async () => {
    setMessage(null)
    setError(null)
    if (!form.isbn.trim()) return setError('ISBN을 먼저 입력해주세요.')
    try {
      const dto = await api.admin.searchBookByIsbn(form.isbn.trim())
      setForm(dto)
      setAuthorsText(dto.authors.join(', '))
      setMessage('알라딘에서 도서 정보를 불러왔습니다.')
    } catch {
      setError('ISBN 조회에 실패했습니다.')
    }
  }

  const submit = async () => {
    setError(null)
    setMessage(null)
    if (!form.title.trim()) return setError('제목을 입력해주세요.')
    if (!authorsText.trim()) return setError('저자를 입력해주세요.')
    if (form.price <= 0) return setError('가격을 입력해주세요.')

    const dto: BookInfoDto = {
      ...form,
      title: form.title.trim(),
      authors: authorsText.split(',').map((a) => a.trim()).filter(Boolean),
      image: form.image?.trim() ? form.image.trim() : null,
      description: form.description?.trim() ? form.description.trim() : null,
    }

    setSubmitting(true)
    try {
      if (isEdit && bookId !== null) {
        await api.admin.updateBook(bookId, dto)
      } else {
        await api.admin.createBook(dto)
      }
      navigate('/admin/books')
    } catch {
      setError('저장에 실패했습니다. 입력값을 확인해주세요.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-[640px]">
      <AdminPageTitle>{isEdit ? 'Edit Book' : 'New Book'}</AdminPageTitle>

      <div className="mt-10 flex flex-col gap-4">
        {/* 표지: 파일 업로드 API 미확인 → URL 입력 방식 (계약 문서 '확인 필요' 항목) */}
        <div
          className="flex flex-col items-center justify-center gap-2 border border-dashed border-[rgba(215,226,234,0.35)] p-8 text-center"
          style={{ borderRadius: 24 }}
        >
          <p className="text-xs font-light text-body/55">표지 이미지 URL (파일 업로드 API 미지원)</p>
          <input
            className={`${inputClass} max-w-[420px]`}
            placeholder="https://…"
            value={form.image ?? ''}
            onChange={(e) => set('image', e.target.value)}
          />
        </div>

        <div className="flex gap-3">
          <input
            className={inputClass}
            placeholder="ISBN"
            value={form.isbn}
            onChange={(e) => set('isbn', e.target.value)}
          />
          <button
            type="button"
            onClick={() => void lookupIsbn()}
            className="shrink-0 cursor-pointer rounded-full border border-[rgba(215,226,234,0.3)] px-5 py-2 text-xs font-medium uppercase text-body transition-opacity hover:opacity-70"
          >
            알라딘 조회
          </button>
        </div>

        <input
          className={inputClass}
          placeholder="제목"
          value={form.title}
          onChange={(e) => set('title', e.target.value)}
        />
        <input
          className={inputClass}
          placeholder="저자 (쉼표로 구분)"
          value={authorsText}
          onChange={(e) => setAuthorsText(e.target.value)}
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <input
            className={inputClass}
            placeholder="출판사"
            value={form.publisher}
            onChange={(e) => set('publisher', e.target.value)}
          />
          <input
            type="date"
            className={`${inputClass} cursor-pointer`}
            value={form.publishedDate}
            onChange={(e) => set('publishedDate', e.target.value)}
            aria-label="출간일"
          />
          <input
            className={inputClass}
            inputMode="numeric"
            placeholder="가격"
            value={form.price === 0 ? '' : String(form.price)}
            onChange={(e) => set('price', Number(e.target.value.replace(/\D/g, '')) || 0)}
          />
          <select
            className={`${inputClass} cursor-pointer appearance-none`}
            value={form.categoryId ?? ''}
            onChange={(e) => set('categoryId', e.target.value ? Number(e.target.value) : null)}
            aria-label="카테고리"
          >
            <option value="" className="bg-ink">
              카테고리 선택
            </option>
            {categories.map((c) => (
              <option key={c.categoryId} value={c.categoryId} className="bg-ink">
                {c.categoryName}
              </option>
            ))}
          </select>
        </div>

        <textarea
          rows={6}
          className="w-full resize-y rounded-3xl border border-[rgba(215,226,234,0.3)] bg-transparent px-6 py-5 text-sm font-light leading-relaxed text-body placeholder:text-body/40 focus:border-body/70 focus:outline-none"
          placeholder="소개"
          value={form.description ?? ''}
          onChange={(e) => set('description', e.target.value)}
        />

        {/* 할인율/재고/판매 상태: BookInfoDto에 필드 없음 (계약 문서 '추가 필요' 항목) */}
        <p className="text-xs font-light text-body/45">
          할인율·재고·판매 상태 입력은 백엔드 필드 추가 후 지원됩니다.
        </p>

        {message && <p className="text-xs text-body/70">{message}</p>}
        {error && (
          <p role="alert" className="text-xs text-accent">
            {error}
          </p>
        )}

        <div className="mt-4 flex gap-4">
          <button
            type="button"
            onClick={() => void submit()}
            disabled={submitting}
            className="btn-cta cursor-pointer px-10 py-3.5 text-sm disabled:opacity-60"
          >
            {submitting ? '저장 중…' : '저장'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/admin/books')}
            className="btn-ghost cursor-pointer px-10 py-3 text-sm"
          >
            취소
          </button>
        </div>
      </div>
    </div>
  )
}
