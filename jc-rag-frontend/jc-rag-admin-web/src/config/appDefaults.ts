/**
 * 前端运行时配置默认值（构建期注入）。
 * <p>所有 URL/路径在 .env.development / .env.production 中声明，
 * 运行时仍以后端 /api/sys/runtime-config 为准；本文件仅提供表单与展示的兜底。
 */
export const appDefaults = {
  frontendPort: Number(import.meta.env.VITE_FRONTEND_PORT ?? 5173),
  adminBackendUrl: import.meta.env.VITE_ADMIN_BACKEND_URL ?? 'http://localhost:8080',
  pythonRagUrl: import.meta.env.VITE_PYTHON_RAG_URL ?? 'http://localhost:8001',
  ollamaUrl: import.meta.env.VITE_OLLAMA_URL ?? 'http://localhost:11434',
  ollamaChatPath: import.meta.env.VITE_OLLAMA_CHAT_PATH ?? '/api/chat',
  ollamaTagsPath: import.meta.env.VITE_OLLAMA_TAGS_PATH ?? '/api/tags',
  lmstudioUrl: import.meta.env.VITE_LMSTUDIO_URL ?? 'http://localhost:1234',
  elasticsearchUrl: import.meta.env.VITE_ELASTICSEARCH_URL ?? 'http://localhost:9200',
  minioUrl: import.meta.env.VITE_MINIO_URL ?? 'http://localhost:9000',
  embeddingModelPath:
    import.meta.env.VITE_DEFAULT_EMBEDDING_MODEL_PATH ?? '.jc-rag/models/embedding/bge-m3',
} as const
