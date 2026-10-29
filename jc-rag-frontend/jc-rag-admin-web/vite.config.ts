/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


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
