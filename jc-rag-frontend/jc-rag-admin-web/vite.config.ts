import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  // 必须用绝对 base: history 模式下后端子路由刷新时, 相对路径 './assets/...'
  // 会按当前 URL 目录解析 (如 /system/x/assets/...) 导致 JS 404 白屏.
  base: '/',
  build: {
    outDir: mode === 'production'
      ? path.resolve(__dirname, '../../jc-rag-backend/jc-rag-admin/src/main/resources/static')
      : 'dist',
    emptyOutDir: mode === 'production',
  },
  server: {
    host: '127.0.0.1',
    port: Number(process.env.VITE_FRONTEND_PORT ?? 5173),
    proxy: {
      '/api': {
        target: process.env.VITE_ADMIN_BACKEND_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: [
      {
        find: 'jc-rag-admin-web/config/modelProviderExtensions',
        replacement: path.resolve(__dirname, './src/config/modelProviderExtensions.ts'),
      },
    ],
  },
  customLogger: {
    info(msg) {
      console.log(msg)
      if (msg.includes('Local:')) {
        const admin = process.env.VITE_ADMIN_BACKEND_URL ?? 'http://localhost:8080'
        const py = process.env.VITE_PYTHON_RAG_URL ?? 'http://localhost:8001'
        console.log(`[jc-rag-admin-web] Frontend: http://localhost:${process.env.VITE_FRONTEND_PORT ?? 5173}/system/settings`)
        console.log(`[jc-rag-admin-web] Backend API: ${admin}/api/system/runtime-config`)
        console.log(`[jc-rag-admin-web] Backend Health: ${admin}/api/health/basic`)
        console.log(`[jc-rag-admin-web] Python RAG: ${py}/health`)
      }
    },
    warn(msg) {
      console.warn(msg)
    },
    warnOnce(msg) {
      console.warn(msg)
    },
    error(msg) {
      console.error(msg)
    },
    clearScreen() {
      console.clear()
    },
    hasWarned: false,
    hasErrorLogged() {
      return false
    },
  },
}))
