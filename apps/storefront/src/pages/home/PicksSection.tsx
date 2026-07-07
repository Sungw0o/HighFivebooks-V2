import { useRef } from 'react'
import { Link } from 'react-router-dom'
import { motion, useReducedMotion, useScroll, useTransform } from 'framer-motion'
import type { MotionValue } from 'framer-motion'
import { FadeIn } from '../../components/motion/FadeIn'
import { BookCover } from '../../components/BookCover'
import type { BookResponse } from '../../api'

interface Pick {
  name: string
  books: BookResponse[] // [좌상, 좌하, 우 대형]
}

function PickCard({
  pick,
  index,
  total,
  progress,
  reduced,
}: {
  pick: Pick
  index: number
  total: number
  progress: MotionValue<number>
  reduced: boolean
}) {
  // 이전 카드가 쌓이면서 targetScale까지 축소
  const targetScale = 1 - (total - 1 - index) * 0.03
  const scale = useTransform(progress, [index / total, 1], [1, targetScale])
  const [top, left, right] = pick.books

  return (
    <div className="h-[85vh]" style={{ perspective: 1000 }}>
      <motion.article
        className="sticky flex flex-col gap-8 border-2 border-body bg-ink p-10"
        style={{
          top: `calc(6rem + ${index * 28}px)`,
          borderRadius: 60,
          scale: reduced ? 1 : scale,
          transformOrigin: 'top center',
        }}
      >
        <header className="flex items-center justify-between gap-6">
          <div className="flex items-baseline gap-6">
            <span className="font-display text-5xl font-black text-gradient-heading">
              {String(index + 1).padStart(2, '0')}
            </span>
            <h3 className="font-display text-2xl font-semibold uppercase" style={{ letterSpacing: '0.06em' }}>
              {pick.name}
            </h3>
          </div>
          <Link to="/books" className="btn-ghost shrink-0 px-8 py-3 text-sm">
            전체 보기
          </Link>
        </header>

        <div className="flex h-[46vh] gap-6">
          <div className="flex w-2/5 flex-col gap-6">
            {[top, left].filter(Boolean).map((book) => (
              <Link key={book.id} to={`/books/${book.id}`} className="min-h-0 flex-1">
                <BookCover
                  bookId={book.id}
                  title={book.title}
                  imageUrl={book.imageUrl}
                  radius={40}
                  className="h-full w-full"
                  style={{ aspectRatio: 'auto' }}
                />
              </Link>
            ))}
          </div>
          {right && (
            <Link to={`/books/${right.id}`} className="w-3/5">
              <BookCover
                bookId={right.id}
                title={right.title}
                author={right.author}
                imageUrl={right.imageUrl}
                radius={40}
                className="h-full w-full"
                style={{ aspectRatio: 'auto' }}
              />
            </Link>
          )}
        </div>
      </motion.article>
    </div>
  )
}

interface PicksSectionProps {
  books: BookResponse[]
}

/**
 * 다크 섹션, 상단 라운드 + -56px 겹침(z-10). 3장 스티키 스태킹 카드.
 * targetScale = 1 - (total-1-index) * 0.03, top offset index*28px.
 * 큐레이션 API가 없어 컬렉션 구성은 프론트 정적 큐레이션.
 */
export function PicksSection({ books }: PicksSectionProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const reduced = useReducedMotion()
  const { scrollYProgress } = useScroll({
    target: containerRef,
    offset: ['start start', 'end end'],
  })

  const picks: Pick[] = [
    { name: 'Editor’s Table', books: books.slice(0, 3) },
    { name: 'Midnight Shelf', books: books.slice(3, 6) },
    { name: 'First Highfive', books: books.slice(6, 9) },
  ].filter((p) => p.books.length > 0)

  return (
    <section
      className="relative z-10 -mt-14 bg-ink px-10 pb-32 pt-28"
      style={{ borderRadius: '60px 60px 0 0' }}
    >
      <FadeIn>
        <h2
          className="font-display font-black uppercase leading-none text-gradient-heading"
          style={{ fontSize: 'clamp(3rem, 12vw, 160px)', letterSpacing: '-0.03em' }}
        >
          Picks
        </h2>
      </FadeIn>

      <div ref={containerRef} className="mt-10">
        {picks.map((pick, i) => (
          <PickCard
            key={pick.name}
            pick={pick}
            index={i}
            total={picks.length}
            progress={scrollYProgress}
            reduced={reduced ?? false}
          />
        ))}
      </div>
    </section>
  )
}
