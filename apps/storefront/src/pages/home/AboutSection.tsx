import { Link } from 'react-router-dom'
import { FadeIn } from '../../components/motion/FadeIn'
import { AnimatedText } from '../../components/motion/AnimatedText'

const ABOUT_COPY =
  '하이파이브는 책과 사람이 손뼉을 마주치는 순간을 만듭니다. 매주 새로 고르는 컬렉션, 데이터가 아닌 사람이 쓰는 추천사, 그리고 밤새 읽고 싶어지는 책들. 당신의 다음 페이지를 우리가 먼저 펼쳐 둘게요.'

/** min-h-screen 중앙 정렬: ABOUT US 헤딩 + 글자 단위 스크롤 리빌 + CTA */
export function AboutSection() {
  return (
    <section className="flex min-h-screen flex-col items-center justify-center gap-12 px-10 py-40 text-center">
      <FadeIn>
        <h2
          className="font-display font-black uppercase leading-none text-gradient-heading"
          style={{ fontSize: 'clamp(3rem, 12vw, 160px)', letterSpacing: '-0.03em' }}
        >
          About Us
        </h2>
      </FadeIn>
      <AnimatedText
        text={ABOUT_COPY}
        className="max-w-[680px] text-lg font-light leading-loose text-body"
      />
      <FadeIn delay={0.2}>
        <Link to="/books" className="btn-cta">
          컬렉션 보러가기
        </Link>
      </FadeIn>
    </section>
  )
}
