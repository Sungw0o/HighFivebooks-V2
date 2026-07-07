import { motion } from 'framer-motion'
import type { ReactNode } from 'react'

const EASE = [0.25, 0.1, 0.25, 1] as const

interface FadeInProps {
  children: ReactNode
  /** 초 단위 지연 */
  delay?: number
  /** 시작 y 오프셋(px). 음수면 위에서 내려옴 */
  y?: number
  className?: string
}

/** whileInView 진입 애니메이션. viewport once, duration 0.7, easing [0.25,0.1,0.25,1] */
export function FadeIn({ children, delay = 0, y = 24, className }: FadeInProps) {
  return (
    <motion.div
      className={className}
      initial={{ opacity: 0, y }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.7, delay, ease: EASE }}
    >
      {children}
    </motion.div>
  )
}
