import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  // 必须用绝对 base: history 模式下后端子路由刷新时, 相对路径 './assets/...'
  // 会按当前 URL 目录解析导致 JS 404 白屏.
  base: '/',
  build: {
    outDir: mode === 'production'
      ? path.resolve(__dirname, '../../jc-rag-backend/jc-rag-user/src/main/resources/static')
      : 'dist',
    emptyOutDir: mode === 'production',
  },
  server: {
    host: '127.0.0.1',
    // 让位给 Pro user dev (port 5175, jc-rag-pro/jc-rag-pro-frontend/jc-rag-pro-app-user),
    // 社区版 user dev 改 5176, 两个 dev server 共存不冲突. Pro user 是 VITE_IS_PRO=true,
    // 公告入口正常显示; community user dev 默认 VITE_IS_PRO=undefined, 入口不显示.
    port: 5176,
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            if (proxyRes.headers['content-type']?.includes('text/event-stream')) {
              proxyRes.headers['cache-control'] = 'no-cache, no-transform'
              delete proxyRes.headers['content-length']
            }
          })
        },
      },
    },
  },
}))
