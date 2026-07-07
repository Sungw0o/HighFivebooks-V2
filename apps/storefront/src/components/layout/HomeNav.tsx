import { Link } from 'react-router-dom'

const NAV_ITEMS: { label: string; to: string }[] = [
  { label: 'Best', to: '/books?sort=POPULAR' },
  { label: 'New', to: '/books?sort=NEWEST' },
  { label: 'Category', to: '/books' },
  { label: 'My', to: '/my' },
  { label: 'Cart', to: '/cart' },
]

/** 홈 히어로 상단 내비: 필 5개 justify-between, uppercase, hover opacity .7 */
export function HomeNav() {
  return (
    <nav className="flex w-full items-center justify-between">
      {NAV_ITEMS.map((item) => (
        <Link
          key={item.label}
          to={item.to}
          className="rounded-full border border-[rgba(215,226,234,0.3)] px-6 py-2 text-sm font-medium uppercase text-body transition-opacity duration-200 hover:opacity-70"
          style={{ letterSpacing: '0.16em' }}
        >
          {item.label}
        </Link>
      ))}
    </nav>
  )
}
