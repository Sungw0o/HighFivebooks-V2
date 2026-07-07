import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import type { FormEvent } from 'react'
import { api, tokenStore } from '../../api'

const pillClass =
  'rounded-full border border-[rgba(215,226,234,0.3)] px-5 py-2 text-xs font-medium uppercase text-body transition-opacity duration-200 hover:opacity-70'

/** 목록/상세 등 서브 화면 슬림 헤더: 로고 + pill 검색바 + 로그인 상태 + Cart 배지 */
export function SlimHeader() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [keyword, setKeyword] = useState(searchParams.get('keyword') ?? '')
  const [loggedIn, setLoggedIn] = useState(tokenStore.memberId() !== null)
  const [cartCount, setCartCount] = useState(0)

  useEffect(() => {
    api.cart
      .getCart()
      .then((cart) => setCartCount(cart.items.length))
      .catch(() => setCartCount(0))
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

      <nav className="flex items-center gap-3" style={{ letterSpacing: '0.16em' }}>
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
