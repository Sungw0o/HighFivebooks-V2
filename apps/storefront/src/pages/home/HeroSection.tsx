import { Link } from 'react-router-dom'
import { FadeIn } from '../../components/motion/FadeIn'
import { Magnet } from '../../components/motion/Magnet'
import { BookCover } from '../../components/BookCover'
import { HomeNav } from '../../components/layout/HomeNav'
import type { BookResponse } from '../../api'

interface HeroSectionProps {
  monthlyBook: BookResponse | null
}

/**
 * 100vh 히어로: 내비 → 초대형 HIGHFIVE 그라디언트 헤딩 → 좌측 태그라인 + 우측 CTA.
 * 하단 중앙 340×470 이달의 책(floaty 7s + 마그넷).
 * 진입: 내비 0(y-20) → 헤딩 .15(y40) → 태그라인 .35 → CTA .5 → 표지 .6(y30)
 */
export function HeroSection({ monthlyBook }: HeroSectionProps) {
  return (
    <section className="relative flex h-screen flex-col justify-between overflow-hidden px-10 pb-10 pt-8">
      <FadeIn delay={0} y={-20}>
        <HomeNav />
      </FadeIn>

      <FadeIn delay={0.15} y={40} className="overflow-hidden">
        <h1
          className="whitespace-nowrap text-center font-display text-[16.5vw] font-black uppercase leading-none text-gradient-heading"
          style={{ letterSpacing: '-0.03em' }}
        >
          HighFive
        </h1>
      </FadeIn>

      <div className="flex items-end justify-between">
        <FadeIn delay={0.35}>
          <p
            className="max-w-[260px] text-sm font-light uppercase leading-relaxed text-body/70"
            style={{ letterSpacing: '0.12em' }}
          >
            당신의 다음 책과
            <br />
            가장 먼저 하이파이브하는 서점
          </p>
        </FadeIn>
        <FadeIn delay={0.5}>
          <Link to="/books" className="btn-cta">
            지금 둘러보기
          </Link>
        </FadeIn>
      </div>

      {monthlyBook && (
        <div className="pointer-events-none absolute inset-x-0 bottom-[-60px] z-10 flex justify-center">
          <FadeIn delay={0.6} y={30}>
            <div className="pointer-events-auto animate-floaty">
              <Magnet>
                <Link to={`/books/${monthlyBook.id}`} aria-label={`이달의 책: ${monthlyBook.title}`}>
                  <BookCover
                    bookId={monthlyBook.id}
                    title={monthlyBook.title}
                    author={monthlyBook.author}
                    imageUrl={monthlyBook.imageUrl}
                    radius={20}
                    style={{ width: 340, height: 470 }}
                  />
                </Link>
              </Magnet>
            </div>
          </FadeIn>
        </div>
      )}
    </section>
  )
}
