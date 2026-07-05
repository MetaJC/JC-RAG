import { request, API_BASE_URL, isUnauthorizedError, extractErrorMessage } from './http'

export const userApiBase = API_BASE_URL
export { isUnauthorizedError, extractErrorMessage }

function normalizeSession(item: any) {
  if (!item || typeof item !== 'object') return item
  return {
    ...item,
    sessionTitle: item.sessionTitle ?? item.session_title ?? '',
    kbId: item.kbId ?? item.kb_id ?? null,
    kbName: item.kbName ?? item.kb_name ?? '',
    isPinned: Boolean(item.isPinned ?? item.is_pinned ?? false),
    createdAt: item.createdAt ?? item.created_at ?? '',
    updatedAt: item.updatedAt ?? item.updated_at ?? '',
  }
}

function normalizeKnowledgeBase(item: any) {
  if (!item || typeof item !== 'object') return item
  return {
    ...item,
    id: item.id != null ? Number(item.id) : null,
    kbCode: item.kbCode ?? item.kb_code ?? '',
    kbName: item.kbName ?? item.kb_name ?? '',
    kbDesc: item.kbDesc ?? item.kb_desc ?? '',
    isDefault: Boolean(item.isDefault ?? item.is_default ?? false),
  }
}

export const userApi = {
  login: (data: any) => request.post('/auth/login', data).then(r => r.data),
  register: (data: any) => request.post('/auth/register', data).then(r => r.data),
  currentUser: () => request.get('/auth/me').then(r => r.data),
  logout: () => request.post('/auth/logout').then(r => r.data),
  // 个人中心
  getProfile: () => request.get('/user/profile').then(r => r.data),
  updateProfile: (data: any) => request.put('/user/profile', data).then(r => r.data),
  changePassword: (data: any) => request.post('/user/profile/change-password', data).then(r => r.data),
  // API Key
  listApiKeys: () => request.get('/user/api-keys').then(r => r.data),
  saveApiKey: (data: any) => request.post('/user/api-keys', data).then(r => r.data),
  deleteApiKey: (id: number) => request.delete(`/user/api-keys/${id}`).then(r => r.data),
  // 用量
  checkQuota: () => request.get('/user/quota').then(r => r.data),
  // 问答
  listSessions: () =>
    request.get('/chat/sessions').then(r => {
      const data = r.data
      const records = Array.isArray(data?.records) ? data.records : Array.isArray(data) ? data : []
      return records.map(normalizeSession)
    }),
  createSession: (data: any) => request.post('/chat/sessions', data).then(r => normalizeSession(r.data)),
  renameSession: (sessionId: number, data: any) => request.put(`/chat/sessions/${sessionId}`, data).then(r => normalizeSession(r.data)),
  pinSession: (sessionId: number, pinned: boolean) =>
    request.post(`/chat/sessions/${sessionId}/pin`, { pinned }).then(r => normalizeSession(r.data)),
  deleteSession: (sessionId: number) => request.delete(`/chat/sessions/${sessionId}`).then(r => r.data),
  getMessages: (sessionId: number) => request.get(`/chat/sessions/${sessionId}/messages`).then(r => r.data),
  listKnowledgeBases: () => request.get('/chat/knowledge-bases').then(r => {
    const data = Array.isArray(r.data) ? r.data : []
    return data.map(normalizeKnowledgeBase)
  }),
  /** C 端知识库选择配置: mode = FIXED / SELECTABLE / EMPTY */
  getKbSelectConfig: () => request.get('/chat/knowledge-bases/config').then(r => r.data),
  getModelInfo: () => request.get('/chat/model-info').then(r => r.data),
  // Agent (系统智能体, 后台管控, 用户只读)
  listAgents: () => request.get('/user/agents').then(r => r.data),
  // Agent (用户自定义智能体, 只存 user 层 prompt 片段)
  listCustomAgents: () => request.get('/user/custom-agents').then(r => r.data),
  createCustomAgent: (data: { agentName: string; content: string }) =>
    request.post('/user/custom-agents', data).then(r => r.data),
  updateCustomAgent: (id: number, data: { agentName: string; content: string; status?: number }) =>
    request.put(`/user/custom-agents/${id}`, data).then(r => r.data),
  deleteCustomAgent: (id: number) => request.delete(`/user/custom-agents/${id}`).then(r => r.data),
}
