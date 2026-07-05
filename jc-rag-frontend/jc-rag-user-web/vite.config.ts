import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  base: './',
  build: {
    outDir: mode === 'production'
      ? path.resolve(__dirname, '../../jc-rag-backend/jc-rag-user/src/main/resources/static')
      : 'dist',
    emptyOutDir: mode === 'production',
  },
  server: {
    host: '127.0.0.1',
    port: 5175,
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
