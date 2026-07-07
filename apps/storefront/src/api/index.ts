import type { StorefrontApi } from './port'
import { mockApi } from './mock'
import { realApi } from './real'

const adapter: string = import.meta.env.VITE_API_ADAPTER ?? 'mock'

export const api: StorefrontApi = adapter === 'real' ? realApi : mockApi

export * from './contracts'
export { tokenStore } from './tokenStore'
export type {
  AuthApi,
  BooksApi,
  CartApi,
  CouponsApi,
  MembersApi,
  OrdersApi,
  PaymentsApi,
  ReviewsApi,
  StorefrontApi,
} from './port'
