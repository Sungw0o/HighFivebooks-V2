import { useEffect, useState } from 'react'
import { api } from '../api'
import type { BookResponse, CategoryResponse } from '../api'
import { HeroSection } from './home/HeroSection'
import { MarqueeSection } from './home/MarqueeSection'
import { AboutSection } from './home/AboutSection'
import { CategorySection } from './home/CategorySection'
import { PicksSection } from './home/PicksSection'
import { SiteFooter } from '../components/layout/SiteFooter'

interface HomeData {
  monthlyBook: BookResponse | null
  newBooks: BookResponse[]
  popularBooks: BookResponse[]
  categories: CategoryResponse[]
}

const EMPTY: HomeData = { monthlyBook: null, newBooks: [], popularBooks: [], categories: [] }

export function HomePage() {
  const [data, setData] = useState<HomeData>(EMPTY)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    Promise.all([
      api.books.getBestSellers(1),
      api.books.getNewBooks(8),
      api.books.getPopularBooks(9),
      api.books.getParentCategories(),
    ])
      .then(([best, newBooks, popularBooks, categories]) => {
        if (cancelled) return
        setData({ monthlyBook: best[0] ?? null, newBooks, popularBooks, categories })
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof Error ? e.message : '데이터를 불러오지 못했습니다.')
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <main className="bg-ink">
      <HeroSection monthlyBook={data.monthlyBook} />
      <MarqueeSection rowA={data.newBooks} rowB={data.popularBooks.slice(0, 8)} />
      <AboutSection />
      <CategorySection categories={data.categories} />
      <PicksSection books={data.popularBooks} />
      <SiteFooter />
      {error && (
        <p role="alert" className="px-10 pb-10 text-sm text-accent">
          {error}
        </p>
      )}
    </main>
  )
}
