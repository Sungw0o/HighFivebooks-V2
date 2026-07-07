import { useSyncExternalStore } from 'react'
import { api } from '../api'

/**
 * 카트 배지 전역 상태.
 * 외부 라이브러리 없이 EventTarget + useSyncExternalStore 로 구현.
 * cart.addItem/updateItem/removeItem/clear 후 refresh()를 호출하면
 * SlimHeader 배지가 즉시 갱신된다.
 */

let count = 0
let refreshing = false
const emitter = new EventTarget()
const CHANGE = 'change'

function notify() {
  emitter.dispatchEvent(new Event(CHANGE))
}

export const cartBadge = {
  /** 서버(또는 mock)에서 카트를 다시 조회하고 배지 수를 갱신한다 */
  refresh(): void {
    if (refreshing) return
    refreshing = true
    api.cart
      .getCart()
      .then((cart) => {
        count = cart.items.length
      })
      .catch(() => {
        count = 0
      })
      .finally(() => {
        refreshing = false
        notify()
      })
  },

  /** 서버 호출 없이 즉시 1 증가 (낙관적 업데이트). refresh()도 같이 호출 권장 */
  increment(): void {
    count += 1
    notify()
  },

  getSnapshot(): number {
    return count
  },

  subscribe(cb: () => void): () => void {
    emitter.addEventListener(CHANGE, cb)
    return () => emitter.removeEventListener(CHANGE, cb)
  },
}

/** SlimHeader, BookDetailPage 등에서 배지 수를 구독하는 훅 */
export function useCartBadge(): number {
  return useSyncExternalStore(cartBadge.subscribe, cartBadge.getSnapshot, () => 0)
}
