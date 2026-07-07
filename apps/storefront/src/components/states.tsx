/** 와이어프레임 4c 공통 상태: 로딩 skeleton(shimmer) / 빈 상태 / 에러(재시도) */

export function BookGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <div
      className="grid gap-7"
      style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}
      aria-busy="true"
      aria-label="불러오는 중"
    >
      {Array.from({ length: count }, (_, i) => (
        <div key={i}>
          <div className="skeleton" style={{ aspectRatio: '3 / 4', borderRadius: 16 }} />
          <div className="skeleton mt-3 h-4 w-3/4 rounded" />
          <div className="skeleton mt-2 h-3 w-1/2 rounded" />
        </div>
      ))}
    </div>
  )
}

export function EmptyState({ message, actionLabel, onAction }: { message: string; actionLabel?: string; onAction?: () => void }) {
  return (
    <div className="flex flex-col items-center gap-6 py-32 text-center">
      <span aria-hidden="true" className="font-display text-6xl font-black text-body/25">
        ¯\_(ツ)_/¯
      </span>
      <p className="font-light text-body/70">{message}</p>
      {actionLabel && onAction && (
        <button type="button" onClick={onAction} className="btn-ghost cursor-pointer text-sm">
          {actionLabel}
        </button>
      )}
    </div>
  )
}

export function ErrorState({ message, onRetry }: { message?: string; onRetry: () => void }) {
  return (
    <div role="alert" className="flex flex-col items-center gap-6 py-32 text-center">
      <p className="font-light text-body/70">{message ?? '데이터를 불러오지 못했습니다.'}</p>
      <button type="button" onClick={onRetry} className="btn-ghost cursor-pointer text-sm">
        다시 시도
      </button>
    </div>
  )
}
