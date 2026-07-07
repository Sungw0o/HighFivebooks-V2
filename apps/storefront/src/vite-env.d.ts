/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_ADAPTER?: 'mock' | 'real'
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
