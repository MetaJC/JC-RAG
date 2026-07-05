import type { BrowseDirectoryResult, DeviceItem, HealthItem, ModelVerifyResult, RuntimeConfigResponse, RuntimeConfigUpdateRequest } from '../types/system'
import { request } from './http'

/**
 * sys 模块 API：用户/角色/权限/菜单/Dashboard + 运行时配置/健康/扩展/Demo 模式/API Key 统一管理。
 * <p>路径规则: 控制器类级 @RequestMapping = /api/sys/&lt;resource&gt; (单数), 与文件路径一一对应。
 */

/* ============== Dashboard ============== */

export async function fetchSysDashboard() {
  const { data } = await request.get('/sys/dashboard')
  return data
}

/* ============== RBAC ============== */

export async function fetchSysUsers(params: { page?: number; size?: number; keyword?: string }) {
  const { data } = await request.get('/sys/user', { params })
  return data
}

export async function fetchSysRoles(params: { page?: number; size?: number; keyword?: string }) {
  const { data } = await request.get('/sys/role', { params })
  return data
}

export async function fetchSysPermissions(params: { page?: number; size?: number; keyword?: string; permType?: string }) {
  const { data } = await request.get('/sys/permission', { params })
  return data
}

export async function fetchSysMenus() {
  const { data } = await request.get('/sys/menu')
  return data
}

export async function fetchSysMenuTree() {
  const { data } = await request.get('/sys/menu/tree')
  return data
}

export async function createSysMenu(payload: Record<string, unknown>) {
  const { data } = await request.post('/sys/menu', payload)
  return data
}

export async function updateSysMenu(menuId: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/sys/menu/${menuId}`, payload)
  return data
}

export async function deleteSysMenu(menuId: number) {
  const { data } = await request.delete(`/sys/menu/${menuId}`)
  return data
}

/** 拖拽移动节点: parentId=0/null 移到根, sort 由前端按 drop position 计算 */
export async function moveSysMenu(menuId: number, payload: { parentId?: number | null; sort?: number }) {
  const { data } = await request.put(`/sys/menu/${menuId}/move`, payload)
  return data
}

export async function createSysUser(payload: Record<string, unknown>) {
  const { data } = await request.post('/sys/user', payload)
  return data
}

export async function updateSysUser(userId: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/sys/user/${userId}`, payload)
  return data
}

export async function deleteSysUser(userId: number) {
  const { data } = await request.delete(`/sys/user/${userId}`)
  return data
}

export async function resetSysUserPassword(userId: number, newPassword: string) {
  const { data } = await request.post(`/sys/user/${userId}/reset-password`, { newPassword })
  return data
}

export async function createSysRole(payload: Record<string, unknown>) {
  const { data } = await request.post('/sys/role', payload)
  return data
}

export async function updateSysRole(roleId: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/sys/role/${roleId}`, payload)
  return data
}

export async function deleteSysRole(roleId: number) {
  const { data } = await request.delete(`/sys/role/${roleId}`)
  return data
}

export async function createSysPermission(payload: Record<string, unknown>) {
  const { data } = await request.post('/sys/permission', payload)
  return data
}

export async function updateSysPermission(permissionId: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/sys/permission/${permissionId}`, payload)
  return data
}

export async function deleteSysPermission(permissionId: number) {
  const { data } = await request.delete(`/sys/permission/${permissionId}`)
  return data
}

/* ============== 运行时配置 ============== */

export async function fetchRuntimeConfig() {
  const { data } = await request.get<RuntimeConfigResponse>('/sys/runtime-config')
  return data
}

export async function saveRuntimeConfig(payload: RuntimeConfigUpdateRequest) {
  const { data } = await request.post<RuntimeConfigResponse>('/sys/runtime-config', payload)
  return data
}

/* ============== 服务健康 ============== */

export async function fetchServiceHealth() {
  const { data } = await request.get<HealthItem[]>('/sys/health/services')
  return data
}

/* ============== 系统扩展 (Ollama / 设备 / 模型验证 / 目录浏览) ============== */

export async function fetchOllamaModels() {
  const { data } = await request.get('/sys/ollama/models')
  return data as {
    status: 'SUCCESS' | 'FAILED' | string
    endpoint?: string
    message?: string
    models: any[]
  }
}

export async function fetchDevices() {
  const { data } = await request.get<DeviceItem[]>('/sys/devices')
  return data
}

export async function fetchSystemResources() {
  const { data } = await request.get('/sys/resources')
  return data
}

export async function verifyModel(modelPath: string) {
  const { data } = await request.post<ModelVerifyResult>('/sys/embedding/verify', { modelPath })
  return data
}

export async function browseDirectory(dirPath?: string) {
  const { data } = await request.post<BrowseDirectoryResult>('/sys/browse-directory', { path: dirPath || '' })
  return data
}

/* ============== Demo 模式 / 角色配额 ============== */

export async function fetchDemoConfig() {
  const { data } = await request.get('/sys/demo-config')
  return data
}

export async function updateDemoConfig(payload: Record<string, unknown>) {
  const { data } = await request.put('/sys/demo-config', payload)
  return data
}

export async function fetchRoleQuotas() {
  const { data } = await request.get('/sys/role-quotas')
  return data
}

export async function saveRoleQuota(payload: Record<string, unknown>) {
  const { data } = await request.post('/sys/role-quotas', payload)
  return data
}

export async function checkQuota() {
  const { data } = await request.get('/sys/quota/check')
  return data
}

/* ============== 用户自定义 API Key ============== */

export async function fetchUserApiKeys(keyword?: string) {
  const { data } = await request.get('/sys/api-keys', { params: { keyword } })
  return data
}

export async function fetchActiveApiKey(provider: string = 'openai') {
  const { data } = await request.get('/sys/api-keys/active', { params: { provider } })
  return data
}

export async function saveUserApiKey(payload: Record<string, unknown>) {
  const { data } = await request.post('/sys/api-keys', payload)
  return data
}

export async function toggleUserApiKey(id: number, active: boolean) {
  const { data } = await request.post(`/sys/api-keys/${id}/toggle`, { active })
  return data
}

export async function deleteUserApiKey(id: number) {
  const { data } = await request.delete(`/sys/api-keys/${id}`)
  return data
}

/* ============== 协议管理 (用户协议 / 隐私政策) ============== */

export type LegalDocType = 'USER_AGREEMENT' | 'PRIVACY_POLICY'
export type LegalDocStatus = 'DRAFT' | 'PUBLISHED'

export interface LegalDocVO {
  id: number | null
  docType: LegalDocType
  title: string
  contentMd: string
  sourceFilename?: string | null
  status: LegalDocStatus
  publishedAt?: string | null
  publishedBy?: number | null
  updatedBy?: number | null
  createdAt?: string | null
  updatedAt?: string | null
}

export async function listLegalDocs(): Promise<LegalDocVO[]> {
  const { data } = await request.get('/sys/legal-doc')
  return (data?.data ?? data) as LegalDocVO[]
}

export async function getLegalDoc(docType: LegalDocType): Promise<LegalDocVO> {
  const { data } = await request.get(`/sys/legal-doc/${docType}`)
  return (data?.data ?? data) as LegalDocVO
}

export async function saveLegalDoc(payload: { docType: LegalDocType; title: string; contentMd: string; status: LegalDocStatus }) {
  const { data } = await request.post('/sys/legal-doc', payload)
  return data
}

export async function publishLegalDoc(docType: LegalDocType) {
  const { data } = await request.post(`/sys/legal-doc/${docType}/publish`)
  return data
}

export async function uploadLegalDocFile(file: File): Promise<string> {
  const fd = new FormData()
  fd.append('file', file)
  const { data } = await request.post('/sys/legal-doc/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data?.data ?? data
}

/* ============== 安全审计日志 ============== */

export interface SecurityAuditLogVO {
  id: number
  userId: number | null
  sessionId: number | null
  auditType: string
  riskLevel: string | null
  matchedWordId: number | null
  matchedWord: string | null
  originalContentHash: string | null
  maskedContent: string | null
  action: string | null
  clientIp: string | null
  userAgent: string | null
  createdAt: string | null
}

export interface AuditSummaryVO {
  deny24h: number
  mask24h: number
  since: string
}

export async function fetchAuditLogs(params: {
  page?: number
  size?: number
  userId?: number
  auditType?: string
  riskLevel?: string
  keyword?: string
  since?: string
} = {}) {
  const { data } = await request.get('/admin/security/audit', { params })
  return data
}

export async function fetchAuditSummary() {
  const { data } = await request.get('/admin/security/audit/summary')
  return data
}

/* ============== 敏感词 ============== */

export interface SensitiveWordVO {
  id: number
  word: string
  riskLevel: string
  category: string | null
  action: string
  enabled: number
  description: string | null
  createdBy: number | null
  createdAt: string | null
  updatedAt: string | null
}

export async function fetchSensitiveWords(params: {
  page?: number
  size?: number
  keyword?: string
  riskLevel?: string
} = {}) {
  const { data } = await request.get('/admin/security/words', { params })
  return data
}

export async function createSensitiveWord(payload: Record<string, unknown>) {
  const { data } = await request.post('/admin/security/words', payload)
  return data
}

export async function updateSensitiveWord(payload: Record<string, unknown>) {
  const { data } = await request.put('/admin/security/words', payload)
  return data
}

export async function deleteSensitiveWord(id: number) {
  const { data } = await request.delete(`/admin/security/words/${id}`)
  return data
}

export async function toggleSensitiveWord(id: number) {
  const { data } = await request.post(`/admin/security/words/${id}/toggle`)
  return data
}

export async function batchImportSensitiveWords(lines: string[]) {
  const { data } = await request.post('/admin/security/words/batch', { lines })
  return data
}

/* ============== Agent 默认值 ============== */

export async function setDefaultAgent(id: number) {
  const { data } = await request.post(`/rag/agent/${id}/set-default`)
  return data
}

export async function unsetDefaultAgent() {
  const { data } = await request.post('/rag/agent/unset-default')
  return data
}

export async function getDefaultAgent() {
  const { data } = await request.get('/rag/agent/default')
  return data
}
