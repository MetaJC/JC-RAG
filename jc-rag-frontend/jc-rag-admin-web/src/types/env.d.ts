/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_EDITION: 'oss' | 'pro'
  readonly VITE_EDITION_LABEL: string
  readonly VITE_ADMIN_BACKEND_URL: string
  readonly VITE_PYTHON_RAG_URL: string
  readonly VITE_FRONTEND_PORT: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
