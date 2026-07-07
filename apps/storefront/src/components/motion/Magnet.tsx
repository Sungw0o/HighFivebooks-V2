import { useEffect, useRef, useState } from 'react'
import { useReducedMotion } from 'framer-motion'
import type { ReactNode } from 'react'

interface MagnetProps {
  children: ReactNode
  /** 커서 반응 범위 padding(px) */
  padding?: number
  /** 클수록 약하게 끌림 */
  strength?: number
  className?: string
}

/**
 * 마그넷 호버 효과.
 * rect ±padding 안에서 translate3d((mx-cx)/strength, (my-cy)/strength, 0).
 * 진입 transition 0.3s ease-out, 이탈 0.6s ease-in-out. prefers-reduced-motion 시 비활성.
 */
export function Magnet({ children, padding = 150, strength = 3, className }: MagnetProps) {
  const ref = useRef<HTMLDivElement>(null)
  const reduced = useReducedMotion()
  const [transform, setTransform] = useState('translate3d(0, 0, 0)')
  const [active, setActive] = useState(false)

  useEffect(() => {
    if (reduced) return

    const onMove = (e: MouseEvent) => {
      const el = ref.current
      if (!el) return
      const rect = el.getBoundingClientRect()
      const inside =
        e.clientX > rect.left - padding &&
        e.clientX < rect.right + padding &&
        e.clientY > rect.top - padding &&
        e.clientY < rect.bottom + padding

      if (inside) {
        const centerX = rect.left + rect.width / 2
        const centerY = rect.top + rect.height / 2
        const x = (e.clientX - centerX) / strength
        const y = (e.clientY - centerY) / strength
        setActive(true)
        setTransform(`translate3d(${x.toFixed(1)}px, ${y.toFixed(1)}px, 0)`)
      } else {
        setActive(false)
        setTransform('translate3d(0, 0, 0)')
      }
    }

    window.addEventListener('mousemove', onMove, { passive: true })
    return () => window.removeEventListener('mousemove', onMove)
  }, [padding, strength, reduced])

  return (
    <div
      ref={ref}
      className={className}
      style={{
        transform,
        transition: active ? 'transform 0.3s ease-out' : 'transform 0.6s ease-in-out',
        willChange: 'transform',
      }}
    >
      {children}
    </div>
  )
}
