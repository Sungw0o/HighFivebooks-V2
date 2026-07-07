import axios, { AxiosError } from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { tokenStore } from './tokenStore'
import type { TokenDto } from './contracts'

const BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * 공용 axios 인스턴스.
 * - withCredentials: 비회원 장바구니 guestCookie(Set-Cookie) 유지
 * - 요청: Authorization Bearer + X-USER-ID(JWT sub) 주입
 *   ※ 백엔드는 X-USER-ID 헤더를 신뢰한다(구 gateway 계약). K8s 전환 시 Ingress/BFF에서
 *     JWT 검증 후 주입하는 구조 확정 필요 — docs/STOREFRONT_API_CONTRACT.md 참고.
 * - 응답 401: X-Refresh-Token 헤더로 /api/auth/reissue 1회 재발급 후 원 요청 재시도
 */
export const http = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
})

http.interceptors.request.use((config) => {
  const tokens = tokenStore.get()
  if (tokens) {
    config.headers.set('Authorization', `Bearer ${tokens.accessToken}`)
    const memberId = tokenStore.memberId()
    if (memberId !== null) {
      config.headers.set('X-USER-ID', String(memberId))
    }
  }
  return config
})

let refreshing: Promise<TokenDto> | null = null

async function reissue(refreshToken: string): Promise<TokenDto> {
  // 인터셉터 루프 방지를 위해 기본 axios 사용
  const res = await axios.post<TokenDto>(`${BASE_URL}/api/auth/reissue`, null, {
    headers: { 'X-Refresh-Token': refreshToken },
    withCredentials: true,
  })
  return res.data
}

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

http.interceptors.response.use(undefined, async (error: AxiosError) => {
  const original = error.config as RetriableConfig | undefined
  const tokens = tokenStore.get()

  if (error.response?.status === 401 && original && !original._retried && tokens?.refreshToken) {
    original._retried = true
    try {
      refreshing ??= reissue(tokens.refreshToken).finally(() => {
        refreshing = null
      })
      const dto = await refreshing
      tokenStore.set({ accessToken: dto.accessToken, refreshToken: dto.refreshToken })
      return http(original)
    } catch {
      tokenStore.clear()
    }
  }
  return Promise.reject(error)
})
