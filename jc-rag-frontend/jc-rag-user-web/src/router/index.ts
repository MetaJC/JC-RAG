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


import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// Pro 商业版构建注入 (inject-pro-frontend.ps1 生成 pro-extension.ts.built)
// community 独立运行无 .built 时, glob 返回空对象, 降级为空数组
const builtMod = import.meta.glob<{ proExtensionRoutesBuilt: RouteRecordRaw[] }>(
  './pro-extension.built.ts',
  { eager: true }
)
const proExtensionRoutesBuilt: RouteRecordRaw[] =
  builtMod['./pro-extension.built.ts']?.proExtensionRoutesBuilt ?? []

const router = createRouter({
  // base 跟随 vite base (import.meta.env.BASE_URL), dev/生产自动对齐
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/chat' },
    { path: '/login', name: 'login', component: () => import('../views/user/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('../views/user/RegisterView.vue') },
    { path: '/chat', name: 'chat', component: () => import('../views/user/ChatHomeView.vue') },
    { path: '/profile', name: 'profile', component: () => import('../views/user/ProfileView.vue') },
    { path: '/api-keys', name: 'api-keys', component: () => import('../views/user/ApiKeyManageView.vue') },
    { path: '/agents', name: 'user-agents', component: () => import('../views/user/AgentListView.vue') },
    { path: '/my-memory', name: 'my-memory', component: () => import('../views/user/MyMemoryView.vue') },
    { path: '/agreement', name: 'agreement', component: () => import('../views/user/AgreementView.vue') },
    ...proExtensionRoutesBuilt,
  ],
})
export default router
