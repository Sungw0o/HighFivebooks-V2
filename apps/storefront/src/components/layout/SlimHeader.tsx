import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import type { FormEvent } from 'react'
import { api, tokenStore } from '../../api'
import { cartBadge, useCartBadge } from '../../store/cartBadge'

const pillClass =
  'rounded-full border border-[rgba(215,226,234,0.3)] px-3 py-1.5 text-xs font-medium uppercase text-body transition-opacity duration-200 hover:opacity-70 sm:px-5 sm:py-2'

/** 목록/상세 등 서브 화면 슬림 헤더: 로고 + pill 검색바 + 로그인 상태 + Cart 배지 */
export function SlimHeader() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [keyword, setKeyword] = useState(searchParams.get('keyword') ?? '')
  const [loggedIn, setLoggedIn] = useState(tokenStore.memberId() !== null)
  const cartCount = useCartBadge()

  useEffect(() => {
    cartBadge.refresh()
  }, [])

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    const trimmed = keyword.trim()
    navigate(trimmed ? `/books?keyword=${encodeURIComponent(trimmed)}` : '/books')
  }

  const logout = async () => {
    try {
      await api.auth.logout()
    } catch {
      // 서버 실패해도 로컬 토큰은 제거됨 (adapter가 처리)
    }
    setLoggedIn(false)
    navigate('/')
  }

  return (
    <header className="sticky top-0 z-40 flex items-center justify-between gap-3 bg-ink/90 px-4 py-4 backdrop-blur-sm sm:gap-6 sm:px-10 sm:py-5">
      <Link
        to="/"
        className="font-display text-xl font-black uppercase leading-none text-gradient-heading sm:text-2xl"
        style={{ letterSpacing: '-0.02em' }}
      >
        HighFive
      </Link>

      <form onSubmit={onSubmit} className="hidden max-w-[480px] flex-1 sm:block">
        <input
          type="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="책 제목, 저자 검색"
          aria-label="도서 검색"
          className="w-full rounded-full border border-[rgba(215,226,234,0.3)] bg-transparent px-6 py-2.5 text-sm font-light text-body placeholder:text-body/45 focus:border-body/70 focus:outline-none"
        />
      </form>
      {/* 모바일 검색 아이콘: /books 검색 화면으로 이동 */}
      <Link
        to="/books"
        className="flex items-center justify-center text-body/70 transition-opacity hover:opacity-70 sm:hidden"
        aria-label="도서 검색"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>
      </Link>

      <nav className="flex items-center gap-2 sm:gap-3" style={{ letterSpacing: '0.16em' }}>
        {loggedIn ? (
          <>
            <Link to="/my" className={pillClass}>
              My
            </Link>
            <button type="button" onClick={() => void logout()} className={`${pillClass} cursor-pointer`}>
              Logout
            </button>
          </>
        ) : (
          <Link to="/login" className={pillClass}>
            Login
          </Link>
        )}
        <Link to="/cart" className={`${pillClass} relative`}>
          Cart
          {cartCount > 0 && (
            <span
              className="absolute -right-1.5 -top-1.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-accent px-1 text-[10px] font-semibold text-white"
              aria-label={`장바구니 ${cartCount}종`}
            >
              {cartCount > 9 ? '9+' : cartCount}
            </span>
          )}
        </Link>
      </nav>
    </header>
  )
}
