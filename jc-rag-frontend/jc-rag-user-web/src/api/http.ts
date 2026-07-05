import axios from 'axios'
import type { AxiosError, AxiosResponse } from 'axios'

/**
 * 统一 HTTP 客户端（用户端）。
 * <p>特性同 admin：http.ts — 统一 baseURL / R&lt;T&gt; 解包 / 401 跳转。
 * <p>@author JC
 */
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

/** 暴露原始 baseURL，供下载/流式等非 axios 请求使用。 */
export const API_BASE_URL = apiBaseUrl

export const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 60000,
  withCredentials: true,
})

/** R&lt;T&gt; 响应体形态（后端字段为 message，这里兼容 msg）。 */
interface R<T> {
  code: number
  message?: string
  msg?: string
  data: T
}

/** 判断是否为 R&lt;T&gt; 包装，兼容后端 msg / message 两种字段（与 admin-web 一致）。 */
function isR<T>(value: unknown): value is R<T> {
  if (!value || typeof value !== 'object') return false
  const v = value as Record<string, unknown>
  return 'code' in v && ('msg' in v || 'message' in v)
}

request.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.config.responseType === 'stream' || response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    if (isR(body)) {
      if (body.code === 0) {
        return { ...response, data: body.data }
      }
      return Promise.reject(new Error(body.message || body.msg || `业务错误: code=${body.code}`))
    }
    return response
  },
  (error: AxiosError) => {
    const status = error.response?.status
    const config: any = error.config || {}
    // 冷启动兼容: 401 时延迟重定向, 给调用方 retry 的机会 (e.g. ChatHomeView 冷启动 cookie 同步)
    if (status === 401 && !config.__skip401Delay) {
      const loginPath = '/login'
      const onLoginPage = window.location.pathname === loginPath
      // 仅在以下情况立即重定向: (a) 已在 /login 页面 (b) 明确不 retry
      if (onLoginPage || config.__no401Redirect) {
        // 跳过
      } else {
        console.log('[http] 401 received, deferring redirect 200ms to allow caller retry...', config.url)
        // 抛出错误, 让 ChatHomeView 的 retry 接管; 200ms 后如果还在原页面, 才真正跳
        setTimeout(() => {
          if (window.location.pathname !== loginPath) {
            console.log('[http] 401 still unhandled, redirect to', loginPath)
            window.location.href = loginPath
          }
        }, 200)
        config.__skip401Delay = true // 防 retry 时再次延迟
        return Promise.reject(error)
      }
      // onLoginPage 情况, 不重定向
      if (window.location.pathname !== loginPath) {
        console.log('[http] 401, current path:', window.location.pathname, '→ redirect to', loginPath)
        window.location.href = loginPath
      }
    }
    const data = error.response?.data as { msg?: string; message?: string; error?: string } | undefined
    const message = data?.msg || data?.message || data?.error || error.message || '网络异常'
    console.warn('[http] request error:', status, message, config.url)
    return Promise.reject(new Error(message))
  },
)

/** 错误识别工具。 */
export function isUnauthorizedError(error: unknown) {
  return axios.isAxiosError(error) && error.response?.status === 401
}

/** 提取错误信息（兼容 R 解包前的形态，给历史 caller 用）。 */
export function extractErrorMessage(error: unknown, fallback = '请求失败') {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { msg?: string; message?: string; error?: string } | undefined
    return data?.error || data?.message || data?.msg || error.message || fallback
  }
  if (error instanceof Error) return error.message || fallback
  return fallback
}
