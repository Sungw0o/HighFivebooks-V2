export function formatPrice(price: number): string {
  return `${price.toLocaleString('ko-KR')}원`
}

export function formatRating(rating: number | null): string {
  return rating === null ? '-' : rating.toFixed(1)
}

export function formatDate(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}
