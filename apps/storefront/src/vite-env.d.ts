/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_ADAPTER?: 'mock' | 'real'
  readonly VITE_API_BASE_URL?: string
  /** Toss Payments client key. Empty value falls back to demo pseudo payment. */
  readonly VITE_TOSS_CLIENT_KEY?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
