import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import type { FormEvent } from 'react'

/** 목록/상세 등 서브 화면 슬림 헤더: 로고 + pill 검색바 + My/Cart */
export function SlimHeader() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [keyword, setKeyword] = useState(searchParams.get('keyword') ?? '')

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    const trimmed = keyword.trim()
    navigate(trimmed ? `/books?keyword=${encodeURIComponent(trimmed)}` : '/books')
  }

  return (
    <header className="sticky top-0 z-40 flex items-center justify-between gap-6 bg-ink/90 px-10 py-5 backdrop-blur-sm">
      <Link
        to="/"
        className="font-display text-2xl font-black uppercase leading-none text-gradient-heading"
        style={{ letterSpacing: '-0.02em' }}
      >
        HighFive
      </Link>

      <form onSubmit={onSubmit} className="max-w-[480px] flex-1">
        <input
          type="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="책 제목, 저자 검색"
          aria-label="도서 검색"
          className="w-full rounded-full border border-[rgba(215,226,234,0.3)] bg-transparent px-6 py-2.5 text-sm font-light text-body placeholder:text-body/45 focus:border-body/70 focus:outline-none"
        />
      </form>

      <nav className="flex items-center gap-3">
        {[
          { label: 'My', to: '/my' },
          { label: 'Cart', to: '/cart' },
        ].map((item) => (
          <Link
            key={item.label}
            to={item.to}
            className="rounded-full border border-[rgba(215,226,234,0.3)] px-5 py-2 text-xs font-medium uppercase text-body transition-opacity duration-200 hover:opacity-70"
            style={{ letterSpacing: '0.16em' }}
          >
            {item.label}
          </Link>
        ))}
      </nav>
    </header>
  )
}
