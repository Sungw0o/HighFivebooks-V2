import { Link, NavLink, Outlet } from 'react-router-dom'

const MENU = [
  { to: '/admin', label: '대시보드', end: true },
  { to: '/admin/books', label: '도서 관리', end: true },
  { to: '/admin/books/new', label: '도서 등록', end: true },
  { to: '/admin/orders', label: '주문 관리', end: true },
]

/** 관리자 공통 레이아웃: 좌측 고정 사이드바 230px + 콘텐츠 */
export function AdminLayout() {
  return (
    <div className="flex min-h-screen bg-ink">
      <aside
        className="fixed inset-y-0 left-0 flex flex-col border-r border-[rgba(215,226,234,0.18)] px-5 py-8"
        style={{ width: 230 }}
      >
        <Link
          to="/admin"
          className="px-3 font-display text-xl font-black uppercase text-gradient-heading"
          style={{ letterSpacing: '-0.02em' }}
        >
          HF Admin
        </Link>

        <nav className="mt-10 flex flex-col gap-1">
          {MENU.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
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
        >
          ← 스토어
        </Link>
      </aside>

      <main className="flex-1 px-10 py-10" style={{ marginLeft: 230 }}>
        <Outlet />
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
