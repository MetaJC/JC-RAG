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


import { ref } from 'vue'
import { defineStore } from 'pinia'

import { changePassword, fetchCurrentUser, login, logout } from '../api/auth'
import type { AuthUser } from '../types/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const initialized = ref(false)

  async function init() {
    if (initialized.value) {
      return
    }
    try {
      user.value = await fetchCurrentUser()
    } catch {
      user.value = null
    } finally {
      initialized.value = true
    }
  }

  async function loginWithPassword(username: string, password: string, captchaToken?: string) {
    user.value = await login({ username, password, captchaToken })
    initialized.value = true
  }

  async function refreshUser() {
    user.value = await fetchCurrentUser()
    initialized.value = true
  }

  async function logoutCurrentUser() {
    await logout()
    user.value = null
    initialized.value = true
  }

  async function changeCurrentPassword(oldPassword: string, newPassword: string) {
    await changePassword({ oldPassword, newPassword })
  }

  /**
   * 权限判定：developer 永远放行；其余检查 permCodes。
   * @param code 权限码（单个）或数组（任一命中即放行）
   */
  function hasPerm(code: string | string[]): boolean {
    if (!user.value) return false
    // developer 跳过一切
    if (user.value.username === 'developer') return true
    const codes = user.value.permCodes ?? []
    if (Array.isArray(code)) {
      return code.some((c) => codes.includes(c))
    }
    return codes.includes(code)
  }

  return {
    initialized,
    user,
    init,
    loginWithPassword,
    refreshUser,
    logoutCurrentUser,
    changeCurrentPassword,
    hasPerm,
  }
})
