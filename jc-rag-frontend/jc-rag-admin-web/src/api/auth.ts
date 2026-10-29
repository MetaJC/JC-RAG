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


import { request } from './http'

import type { AuthUser, ChangePasswordRequest, LoginRequest } from '../types/auth'

/** 登录：后端裸返回 AuthUserVO（不带 R 包装），使用 http.ts 解 R 后取 data。 */
export async function login(payload: LoginRequest) {
  const { data } = await request.post<AuthUser>('/auth/login', payload)
  return data
}

/** 当前用户：401 由 http.ts 拦截器统一跳登录。 */
export async function fetchCurrentUser() {
  const { data } = await request.get<AuthUser>('/auth/me')
  return data
}

export async function logout() {
  await request.post('/auth/logout')
}

export async function changePassword(payload: ChangePasswordRequest) {
  await request.post('/auth/change-password', payload)
}
