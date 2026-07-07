import { useEffect, useRef } from 'react'
import { useReducedMotion } from 'framer-motion'
import { Link } from 'react-router-dom'
import { BookCover } from '../../components/BookCover'
import type { BookResponse } from '../../api'

interface MarqueeSectionProps {
  rowA: BookResponse[]
  rowB: BookResponse[]
}

function MarqueeRow({
  books,
  rowRef,
}: {
  books: BookResponse[]
  rowRef: React.RefObject<HTMLDivElement | null>
}) {
  // 이음새 제거를 위해 3배 복제
  const tiles = [...books, ...books, ...books]
  return (
    <div
      ref={rowRef}
      className="flex w-max gap-3"
      style={{ willChange: 'transform', marginLeft: -600 }}
    >
      {tiles.map((book, i) => (
        <Link key={`${book.id}-${i}`} to={`/books/${book.id}`} className="shrink-0">
          <BookCover
            bookId={book.id}
            title={book.title}
            imageUrl={book.imageUrl}
            radius={16}
            style={{ width: 200, height: 270 }}
          />
        </Link>
      ))}
    </div>
  )
}

/**
 * 스크롤 연동 마퀴 2열.
 * offset = (scrollY - sectionTop + innerHeight) * 0.3
 * 1열 translateX(offset-200), 2열 translateX(-(offset-200)).
 * passive 리스너 + rAF 스로틀. prefers-reduced-motion 시 정적.
 */
export function MarqueeSection({ rowA, rowB }: MarqueeSectionProps) {
  const sectionRef = useRef<HTMLElement>(null)
  const row1Ref = useRef<HTMLDivElement>(null)
  const row2Ref = useRef<HTMLDivElement>(null)
  const reduced = useReducedMotion()

  useEffect(() => {
    if (reduced) return

    let rafId = 0
    let ticking = false

    const update = () => {
      ticking = false
      const section = sectionRef.current
      if (!section) return
      const sectionTop = section.offsetTop
      const offset = (window.scrollY - sectionTop + window.innerHeight) * 0.3
      if (row1Ref.current) row1Ref.current.style.transform = `translateX(${offset - 200}px)`
      if (row2Ref.current) row2Ref.current.style.transform = `translateX(${-(offset - 200)}px)`
    }

    const onScroll = () => {
      if (!ticking) {
        ticking = true
        rafId = requestAnimationFrame(update)
      }
    }

    update()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => {
      window.removeEventListener('scroll', onScroll)
      cancelAnimationFrame(rafId)
    }
  }, [reduced, rowA, rowB])

  return (
    <section ref={sectionRef} className="flex flex-col gap-3 overflow-hidden py-28">
      <MarqueeRow books={rowA} rowRef={row1Ref} />
      <MarqueeRow books={rowB} rowRef={row2Ref} />
    </section>
  )
}
