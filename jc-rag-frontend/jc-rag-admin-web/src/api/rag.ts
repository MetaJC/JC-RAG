import { request } from './http'

/**
 * rag 模块 API：智能体 / 技能 / 提示词统一管理。
 * <p>路径规则: 控制器类级 @RequestMapping = /api/rag/&lt;resource&gt; (单数), 与文件路径一一对应。
 */
export async function fetchRagAgents(params: { page?: number; size?: number; keyword?: string; status?: string | number } = {}) {
  const { data } = await request.get('/rag/agent', { params })
  return data
}

export async function getRagAgent(id: number) {
  const { data } = await request.get(`/rag/agent/${id}`)
  return data
}

export async function createRagAgent(payload: Record<string, unknown>) {
  const { data } = await request.post('/rag/agent', payload)
  return data
}

export async function updateRagAgent(id: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/rag/agent/${id}`, payload)
  return data
}

export async function publishRagAgent(id: number) {
  const { data } = await request.post(`/rag/agent/${id}/publish`)
  return data
}

export async function unpublishRagAgent(id: number) {
  const { data } = await request.post(`/rag/agent/${id}/unpublish`)
  return data
}

export async function deleteRagAgent(id: number) {
  const { data } = await request.delete(`/rag/agent/${id}`)
  return data
}

/* ----------- 系统默认智能体 (C 端强制使用, 后台管控) ----------- */

/** 查询系统默认智能体 ID (无默认返回 null) */
export async function getDefaultAgent() {
  const { data } = await request.get('/rag/agent/default')
  return data
}

/** 设置系统默认智能体 (全局唯一, 覆盖旧值) */
export async function setDefaultAgent(id: number) {
  const { data } = await request.post(`/rag/agent/${id}/set-default`)
  return data
}

/** 取消系统默认智能体 (回到无默认状态) */
export async function unsetDefaultAgent() {
  const { data } = await request.post('/rag/agent/unset-default')
  return data
}

export async function fetchRagSkills(params: { page?: number; size?: number; keyword?: string; status?: string | number } = {}) {
  const { data } = await request.get('/rag/skill', { params })
  return data
}

export async function getRagSkill(id: number) {
  const { data } = await request.get(`/rag/skill/${id}`)
  return data
}

export async function createRagSkill(payload: Record<string, unknown>) {
  const { data } = await request.post('/rag/skill', payload)
  return data
}

export async function updateRagSkill(id: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/rag/skill/${id}`, payload)
  return data
}

export async function deleteRagSkill(id: number) {
  const { data } = await request.delete(`/rag/skill/${id}`)
  return data
}

export async function fetchRagPrompts(params: { page?: number; size?: number; scope?: string; keyword?: string } = {}) {
  const { data } = await request.get('/rag/prompt', { params })
  return data
}

export async function getRagPrompt(id: number) {
  const { data } = await request.get(`/rag/prompt/${id}`)
  return data
}

export async function createRagPrompt(payload: Record<string, unknown>) {
  const { data } = await request.post('/rag/prompt', payload)
  return data
}

export async function updateRagPrompt(id: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/rag/prompt/${id}`, payload)
  return data
}

export async function deleteRagPrompt(id: number) {
  const { data } = await request.delete(`/rag/prompt/${id}`)
  return data
}

/* ----------- Admin 端用户智能体 (C 端用户自建, 后台审计/管理/查看) ----------- */

export interface AdminUserCustomAgentVO {
  id: number
  userId: number
  username?: string
  nickname?: string
  agentName: string
  content: string
  status: number
  createdAt?: string
  updatedAt?: string
}

/** Admin 端分页查询用户智能体 (含创建者信息) */
export async function fetchAdminUserCustomAgents(params: { pageNum?: number; pageSize?: number; keyword?: string; status?: number } = {}) {
  const { data } = await request.get('/admin/user-custom-agent', { params })
  return data
}

/** Admin 端查单条 */
export async function getAdminUserCustomAgent(id: number) {
  const { data } = await request.get(`/admin/user-custom-agent/${id}`)
  return data
}

/** Admin 端强制删除 (带审计日志) */
export async function forceDeleteUserCustomAgent(id: number) {
  const { data } = await request.delete(`/admin/user-custom-agent/${id}`)
  return data
}

/** Admin 端启停用户智能体 (1=启用 0=禁用) */
export async function updateAdminUserCustomAgentStatus(id: number, status: number) {
  const { data } = await request.put(`/admin/user-custom-agent/${id}/status`, { status })
  return data
}
