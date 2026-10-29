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


import axios from 'axios'
import type { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

/**
 * 统一 HTTP 客户端。
 * <p>特性：
 * 1. 统一 baseURL / timeout / withCredentials
 * 2. 自动解包 R&lt;T&gt;：业务层只看到 data，R 包装对业务无感
 * 3. 401/业务码非 0：自动 toast + reject
 * <p>所有 api/*.ts 共享此实例。
 */
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

/** 暴露原始 baseURL，供下载/流式等非 axios 请求使用。 */
export const API_BASE_URL = apiBaseUrl

export const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 30000,
  withCredentials: true,
})

/** R&lt;T&gt; 响应体形态。 */
interface R<T> {
  code: number
  msg: string
  data: T
}

/** 判断是否为 R&lt;T&gt; 包装（兼容后端 msg/message 两种字段）。 */
function isR<T>(value: unknown): value is R<T> {
  if (!value || typeof value !== 'object') return false
  const v = value as Record<string, unknown>
  return 'code' in v && ('msg' in v || 'message' in v)
}

request.interceptors.response.use(
  (response: AxiosResponse) => {
    // 流式响应（如 SSE）直接透传
    if (response.config.responseType === 'stream' || response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    if (isR(body)) {
      if (body.code === 0) {
        // 业务层拿到的就是原 data，0 包装感知
        return { ...response, data: body.data }
      }
      // 业务错误：reject 出去，UI 层处理
      return Promise.reject(new Error(body.msg || `业务错误: code=${body.code}`))
    }
    // 非 R 包装：原样透传（兼容老接口）
    return response
  },
  (error: AxiosError) => {
    const status = error.response?.status
    if (status === 401) {
      // 未登录：跳登录页
      const loginPath = '/login'
      if (window.location.pathname !== loginPath) {
        window.location.href = loginPath
      }
    }
    const message =
      (error.response?.data as { msg?: string; message?: string } | undefined)?.msg
        || (error.response?.data as { message?: string } | undefined)?.message
        || error.message
        || '网络异常'
    return Promise.reject(new Error(message))
  }
)

/** 透传请求配置（如自定义超时）。 */
export function withTimeout(timeoutMs: number) {
  return (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    config.timeout = timeoutMs
    return config
  }
}
