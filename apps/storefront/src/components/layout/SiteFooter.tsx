import { Link } from 'react-router-dom'

export function SiteFooter() {
  return (
    <footer className="relative z-10 bg-ink px-10 pb-14 pt-24">
      <div
        className="font-display text-[clamp(2.4rem,7vw,88px)] font-black uppercase leading-none text-gradient-heading"
        style={{ letterSpacing: '-0.03em' }}
      >
        HighFive
      </div>
      <div className="mt-8 flex flex-col gap-6 text-sm text-body/55 md:flex-row md:items-end md:justify-between">
        <div className="leading-relaxed">
          <p>(주)하이파이브북스 · 대표 봉식</p>
          <p>광주광역시 어딘가로 123 · 사업자등록번호 000-00-00000</p>
          <p>© 2026 HighFive Books. All rights reserved.</p>
        </div>
        <Link
          to="/admin"
          className="uppercase text-body/70 transition-opacity duration-200 hover:opacity-70"
          style={{ letterSpacing: '0.16em' }}
        >
          Admin →
        </Link>
      </div>
    </footer>
  )
}
