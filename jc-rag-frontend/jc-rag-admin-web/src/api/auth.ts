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
