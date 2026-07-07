import type { CSSProperties } from 'react'

/** 표지 placeholder 그라디언트 팔레트 (실서비스에서는 imageUrl 사용) */
const PALETTES = [
  'linear-gradient(160deg, #2b1a4e 0%, #7621b0 55%, #b600a8 100%)',
  'linear-gradient(160deg, #0f2b3a 0%, #1f6f8b 60%, #99ced3 100%)',
  'linear-gradient(160deg, #3a1207 0%, #be4c00 60%, #ffb27a 100%)',
  'linear-gradient(160deg, #101f10 0%, #2e7d4f 60%, #a8e6b3 100%)',
  'linear-gradient(160deg, #26243a 0%, #5c5470 60%, #b9b4c7 100%)',
  'linear-gradient(160deg, #331433 0%, #8e2d56 60%, #f2b6c1 100%)',
]

interface BookCoverProps {
  bookId: number
  title: string
  author?: string
  imageUrl?: string | null
  className?: string
  style?: CSSProperties
  radius?: number
}

/**
 * 책 표지. imageUrl이 없으면 그라디언트 placeholder + 좌측 inset 책등 하이라이트.
 */
export function BookCover({ bookId, title, author, imageUrl, className, style, radius = 16 }: BookCoverProps) {
  const background = imageUrl ? undefined : PALETTES[bookId % PALETTES.length]

  return (
    <div
      className={`book-cover flex flex-col justify-between ${className ?? ''}`}
      style={{ background, borderRadius: radius, aspectRatio: '3 / 4', ...style }}
      role="img"
      aria-label={`${title} 표지`}
    >
      {imageUrl ? (
        <img
          src={imageUrl}
          alt=""
          loading="lazy"
          className="h-full w-full object-cover"
          style={{ borderRadius: radius }}
        />
      ) : (
        <>
          <span
            className="px-4 pt-5 font-display text-[15px] font-semibold uppercase leading-snug text-white/90"
            style={{ letterSpacing: '0.02em' }}
          >
            {title}
          </span>
          {author && <span className="px-4 pb-4 text-[11px] text-white/60">{author}</span>}
        </>
      )}
    </div>
  )
}
