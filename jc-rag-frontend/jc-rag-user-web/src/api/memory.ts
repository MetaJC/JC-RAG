/**
 * L2 长期记忆前端接口.
 * <p>所有调用都先走 Spring Boot (jc-rag-user), 再由 Spring Boot 转发到 jc-rag-service (Python).
 * <p>前端不直接调 Python (跨服务/跨域, 不便维护).
 *
 * @author JC
 */
import { request } from './http'

/** 长期记忆项 (ES jc_chat_memory 文档). */
export interface MemoryFactItem {
  memoryId: string
  key: string
  value: string
  category: string
  source: 'auto' | 'manual'
  createdAt?: string
  updatedAt?: string
  lastUsedAt?: string
}

export interface MemoryListResult {
  total: number
  items: MemoryFactItem[]
  page: number
  size: number
}

/**
 * 列本用户所有 active 长期记忆 (前端"我的记忆"页).
 */
export async function listMyMemory(page = 1, size = 50): Promise<MemoryListResult> {
  const { data } = await request.get('/user/preference/memory/list', {
    params: { page, size },
  })
  return (data || { total: 0, items: [], page, size }) as MemoryListResult
}

/**
 * 软删单条记忆.
 */
export async function deleteMemory(memoryId: string): Promise<{ deleted: boolean }> {
  const { data } = await request.post('/user/preference/memory/delete', { memoryId })
  return (data || { deleted: false }) as { deleted: boolean }
}

/**
 * 清空本用户所有 active 记忆 (软删, 保留 ES doc 便于审计).
 */
export async function clearMyMemory(): Promise<{ cleared: number }> {
  const { data } = await request.post('/user/preference/memory/clear', {})
  return (data || { cleared: 0 }) as { cleared: number }
}

/* ----------- L3 跨会话画像 ----------- */

/** L3 画像五段结构 (五段均可空, 与 Python /chat/profile/extract 响应结构一致) */
export interface MemoryProfile {
  domain?: string | null
  role?: string | null
  skills?: string[]
  preferences?: Record<string, string>
  goals?: string[]
}

/** 拉取 L3 画像响应 (后端从 user_memory_profile 表读) */
export interface MemoryProfileResult {
  exists: boolean
  version?: number
  factCount?: number
  modelName?: string
  lastExtractedAt?: string
  updatedAt?: string
  profile?: MemoryProfile | null
}

/** 取本用户 L3 画像 (无画像时 exists=false). */
export async function fetchMyProfile(): Promise<MemoryProfileResult> {
  const { data } = await request.get('/user/preference/memory/profile')
  return (data || { exists: false }) as MemoryProfileResult
}

/** 重置 (清空) 本用户 L3 画像. */
export async function resetMyProfile(): Promise<{ status: string; removed: number }> {
  const { data } = await request.delete('/user/preference/memory/profile')
  return (data || { status: 'FAILED', removed: 0 }) as { status: string; removed: number }
}
