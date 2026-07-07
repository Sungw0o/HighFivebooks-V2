import { Link } from 'react-router-dom'
import { BookCover } from './BookCover'
import { formatPrice, formatRating } from '../lib/format'
import type { BookResponse } from '../api'

/** 목록 그리드 카드: 표지(3/4) + 제목/저자 + 가격 + ★평점. hover opacity .8 */
export function BookCard({ book }: { book: BookResponse }) {
  return (
    <Link
      to={`/books/${book.id}`}
      className="group block transition-opacity duration-200 hover:opacity-80"
    >
      <BookCover bookId={book.id} title={book.title} author={book.author} imageUrl={book.imageUrl} radius={16} />
      <div className="mt-3">
        <p className="truncate text-sm font-normal text-body">{book.title}</p>
        <p className="mt-0.5 truncate text-xs font-light text-body/55">{book.author}</p>
        <div className="mt-1.5 flex items-center justify-between">
          <span className="text-sm font-medium text-body">{formatPrice(book.price)}</span>
          <span className="text-xs text-accent">★ {formatRating(book.avgRating)}</span>
        </div>
      </div>
    </Link>
  )
}
