/** Access/Refresh 토큰 보관 + JWT sub(memberId) 디코드 */

const STORAGE_KEY = 'highfive.tokens'

export interface StoredTokens {
  accessToken: string
  refreshToken: string
}

function decodeJwtSub(token: string): number | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as { sub?: string }
    const sub = Number(json.sub)
    return Number.isFinite(sub) ? sub : null
  } catch {
    return null
  }
}

export const tokenStore = {
  get(): StoredTokens | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      return raw ? (JSON.parse(raw) as StoredTokens) : null
    } catch {
      return null
    }
  },

  set(tokens: StoredTokens): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens))
  },

  clear(): void {
    localStorage.removeItem(STORAGE_KEY)
  },

  /** 로그인된 memberId (JWT sub). 미로그인 시 null */
  memberId(): number | null {
    const tokens = this.get()
    return tokens ? decodeJwtSub(tokens.accessToken) : null
  },
}
