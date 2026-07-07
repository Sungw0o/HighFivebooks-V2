import { useRef } from 'react'
import { motion, useReducedMotion, useScroll, useTransform } from 'framer-motion'
import type { MotionValue } from 'framer-motion'

interface AnimatedTextProps {
  text: string
  className?: string
}

function Char({
  char,
  progress,
  start,
  end,
}: {
  char: string
  progress: MotionValue<number>
  start: number
  end: number
}) {
  const opacity = useTransform(progress, [start, end], [0.2, 1])
  return <motion.span style={{ opacity }}>{char}</motion.span>
}

/**
 * 글자 단위 스크롤 리빌.
 * useScroll offset ['start 0.8','end 0.2'], 각 글자 opacity 0.2 → 1 순차 증가.
 */
export function AnimatedText({ text, className }: AnimatedTextProps) {
  const ref = useRef<HTMLParagraphElement>(null)
  const reduced = useReducedMotion()
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ['start 0.8', 'end 0.2'],
  })

  const chars = Array.from(text)

  if (reduced) {
    return <p className={className}>{text}</p>
  }

  return (
    <p ref={ref} className={className} aria-label={text}>
      {chars.map((char, i) => (
        <Char
          key={`${char}-${i}`}
          char={char}
          progress={scrollYProgress}
          start={i / chars.length}
          end={Math.min(1, (i + 1) / chars.length + 0.05)}
        />
      ))}
    </p>
  )
}
