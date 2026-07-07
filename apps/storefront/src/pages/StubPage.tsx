import { Link } from 'react-router-dom'

interface StubPageProps {
  heading: string
  description: string
}

/** 다음 단계에서 구현할 화면의 자리표시자. 라우팅/전환 확인용. */
export function StubPage({ heading, description }: StubPageProps) {
  return (
    <main className="flex min-h-screen flex-col justify-center px-10">
      <h1
        className="font-display font-black uppercase leading-none text-gradient-heading"
        style={{ fontSize: 'clamp(2.6rem, 9vw, 110px)', letterSpacing: '-0.03em' }}
      >
        {heading}
      </h1>
      <p className="mt-6 max-w-[480px] font-light text-body/70">{description}</p>
      <div className="mt-10">
        <Link to="/" className="btn-ghost">
          ← 홈으로
        </Link>
      </div>
    </main>
  )
}
