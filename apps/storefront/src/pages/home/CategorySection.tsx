import { useNavigate } from 'react-router-dom'
import { FadeIn } from '../../components/motion/FadeIn'
import type { CategoryResponse } from '../../api'

/** 카테고리 설명 카피 (백엔드 CategoryResponse에는 설명 필드가 없어 프론트 카피로 유지) */
const CATEGORY_DESCRIPTIONS: Record<string, string> = {
  소설: '밤을 잊게 만드는 이야기들',
  인문: '생각의 근육을 키우는 시간',
  과학: '세계를 다시 보게 하는 렌즈',
  예술: '감각을 깨우는 색과 소리',
  에세이: '어깨에 힘을 뺀 문장들',
}

interface CategorySectionProps {
  categories: CategoryResponse[]
}

/** 화이트 섹션, 상단 라운드 60px. 01~05 번호 + 이름/설명 리스트, i*0.1 스태거 */
export function CategorySection({ categories }: CategorySectionProps) {
  const navigate = useNavigate()
  const items = categories.slice(0, 5)

  return (
    <section className="relative bg-white px-10 pb-40 pt-28 text-ink" style={{ borderRadius: '60px 60px 0 0' }}>
      <FadeIn>
        <h2
          className="font-display font-black uppercase leading-none"
          style={{ fontSize: 'clamp(3rem, 12vw, 160px)', letterSpacing: '-0.03em' }}
        >
          Category
        </h2>
      </FadeIn>

      <ul className="mt-16">
        {items.map((category, i) => (
          <FadeIn key={category.categoryId} delay={i * 0.1}>
            <li
              className="cursor-pointer border-t border-[rgba(12,12,12,0.15)] py-6 transition-opacity duration-200 hover:opacity-65 last:border-b"
              onClick={() => navigate(`/books?category=${category.categoryId}`)}
            >
              <div className="flex items-baseline gap-8">
                <span
                  className="font-display font-black leading-none"
                  style={{ fontSize: 'clamp(3rem, 10vw, 140px)', letterSpacing: '-0.03em' }}
                >
                  {String(i + 1).padStart(2, '0')}
                </span>
                <div>
                  <p className="font-display text-2xl font-semibold uppercase" style={{ letterSpacing: '0.04em' }}>
                    {category.categoryName}
                  </p>
                  <p className="mt-1 text-sm font-light text-ink/60">
                    {CATEGORY_DESCRIPTIONS[category.categoryName] ?? '새로운 발견이 기다리는 서가'}
                  </p>
                </div>
              </div>
            </li>
          </FadeIn>
        ))}
      </ul>
    </section>
  )
}
