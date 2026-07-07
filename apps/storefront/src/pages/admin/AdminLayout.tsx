import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'

const MENU = [
  { to: '/admin', label: '대시보드', end: true },
  { to: '/admin/books', label: '도서 관리', end: true },
  { to: '/admin/books/new', label: '도서 등록', end: true },
  { to: '/admin/orders', label: '주문 관리', end: true },
]

/** 관리자 공통 레이아웃: md↑ 좌측 고정 사이드바 230px + 콘텐츠, 모바일은 상단 햄버거 */
export function AdminLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="flex min-h-screen bg-ink">
      {/* 모바일 상단 바 */}
      <header className="fixed inset-x-0 top-0 z-50 flex items-center justify-between border-b border-[rgba(215,226,234,0.18)] bg-ink/95 px-4 py-3 backdrop-blur-sm md:hidden">
        <Link
          to="/admin"
          className="font-display text-lg font-black uppercase text-gradient-heading"
          style={{ letterSpacing: '-0.02em' }}
        >
          HF Admin
        </Link>
        <button
          type="button"
          onClick={() => setSidebarOpen((prev) => !prev)}
          className="flex cursor-pointer items-center justify-center text-body/70"
          aria-label="메뉴 토글"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            {sidebarOpen ? (
              <>
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </>
            ) : (
              <>
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </>
            )}
          </svg>
        </button>
      </header>

      {/* 모바일 오버레이 */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* 사이드바: md↑ 항상 보임, 모바일은 햄버거 토글 */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 flex flex-col border-r border-[rgba(215,226,234,0.18)] bg-ink px-5 py-8 transition-transform duration-200 md:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
        style={{ width: 230 }}
      >
        <Link
          to="/admin"
          className="px-3 font-display text-xl font-black uppercase text-gradient-heading"
          style={{ letterSpacing: '-0.02em' }}
          onClick={() => setSidebarOpen(false)}
        >
          HF Admin
        </Link>

        <nav className="mt-10 flex flex-col gap-1">
          {MENU.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                `rounded-xl px-4 py-3 text-sm transition-colors ${
                  isActive
                    ? 'bg-[rgba(215,226,234,0.16)] font-semibold text-body'
                    : 'font-light text-body/60 hover:bg-[rgba(215,226,234,0.08)]'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <Link
          to="/"
          className="mt-auto px-4 text-xs font-light uppercase text-body/55 transition-opacity hover:opacity-70"
          style={{ letterSpacing: '0.16em' }}
          onClick={() => setSidebarOpen(false)}
        >
          ← 스토어
        </Link>
      </aside>

      <main className="flex-1 px-4 pb-10 pt-16 sm:px-10 md:pt-10" style={{ marginLeft: 0 }}>
        {/* md↑ 사이드바 공간 확보 */}
        <div className="md:ml-[230px]">
          <Outlet />
        </div>
      </main>
    </div>
  )
}

export function AdminPageTitle({ children }: { children: string }) {
  return (
    <h1
      className="font-display font-black uppercase leading-none text-gradient-heading"
      style={{ fontSize: 'clamp(2rem, 5vw, 56px)', letterSpacing: '-0.03em' }}
    >
      {children}
    </h1>
  )
}
