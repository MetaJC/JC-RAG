import { request } from './http'

import type {
  KbAuthDetail,
  KbAuthSaveRequest,
  KnowledgeBaseCreateRequest,
  KnowledgeBaseItem,
  KnowledgeBaseUpdateRequest,
  KnowledgeDocumentItem,
  KnowledgeDocumentUpdateRequest,
  KnowledgeUploadPolicy,
} from '../types/knowledge'
import type { DocumentParseProgress } from '../types/system'

/**
 * 知识库 API。
 * 统一使用共享 request（自动解 R 包装），视图拿到的就是 data。
 */
export async function fetchKnowledgeBases(params?: { page?: number; size?: number; keyword?: string }) {
  const { data } = await request.get('/knowledge-bases', { params })
  return data
}

export async function createKnowledgeBase(payload: KnowledgeBaseCreateRequest) {
  const { data } = await request.post<KnowledgeBaseItem>('/knowledge-bases', payload)
  return data
}

export async function updateKnowledgeBase(kbId: number, payload: KnowledgeBaseUpdateRequest) {
  const { data } = await request.put<KnowledgeBaseItem>(`/knowledge-bases/${kbId}`, payload)
  return data
}

export async function deleteKnowledgeBase(kbId: number) {
  const { data } = await request.delete(`/knowledge-bases/${kbId}`)
  return data
}

export async function fetchKnowledgeDocuments(kbId: number, params?: { page?: number; size?: number; keyword?: string; parseStatus?: string }) {
  const { data } = await request.get(`/knowledge-bases/${kbId}/documents`, { params })
  return data
}

export async function fetchKnowledgeUploadPolicy() {
  const { data } = await request.get<KnowledgeUploadPolicy>('/knowledge-bases/upload-policy')
  return data
}

export async function uploadKnowledgeDocuments(
  kbId: number,
  files: File[],
  options?: { onProgress?: (percent: number, loaded: number, total: number) => void },
) {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  const { data } = await request.post<KnowledgeDocumentItem[]>(`/knowledge-bases/${kbId}/documents/upload`, formData, {
    timeout: 0,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (event) => {
      const loaded = event.loaded ?? 0
      const total = event.total ?? 0
      const percent = total > 0 ? Math.min(100, Math.round((loaded / total) * 100)) : 0
      options?.onProgress?.(percent, loaded, total)
    },
  })
  return data
}

export async function reprocessKnowledgeDocument(documentId: number) {
  const { data } = await request.post(`/knowledge-bases/documents/${documentId}/reprocess`)
  return data
}

export async function updateKnowledgeDocument(documentId: number, payload: KnowledgeDocumentUpdateRequest) {
  const { data } = await request.put(`/knowledge-bases/documents/${documentId}`, payload)
  return data
}

export async function deleteKnowledgeDocument(documentId: number) {
  const { data } = await request.delete(`/knowledge-bases/documents/${documentId}`)
  return data
}

export function getDocumentFileUrl(documentId: number) {
  return `${import.meta.env.VITE_API_BASE_URL || '/api'}/knowledge-bases/documents/${documentId}/file`
}

export async function fetchDocumentParseProgress(documentId: number) {
  const { data } = await request.get<DocumentParseProgress>(`/knowledge-bases/documents/${documentId}/parse-progress`)
  return data
}

export async function cancelDocumentParse(documentId: number) {
  const { data } = await request.post(`/knowledge-bases/documents/${documentId}/cancel-parse`)
  return data
}

export async function fetchSystemResources() {
  const { data } = await request.get('/sys/resources', { timeout: 5000 })
  return data
}

/** 获取系统级 C 端默认知识库 (超管 / developer 可见) */
export async function fetchSystemDefaultKb() {
  const { data } = await request.get<KnowledgeBaseItem | null>('/admin/system/kb-default')
  return data
}

/** 设置系统级 C 端默认知识库 (超管 / developer 可用) */
export async function setSystemDefaultKb(kbId: number) {
  const { data } = await request.put<KnowledgeBaseItem>('/admin/system/kb-default', null, { params: { kbId } })
  return data
}

/** 清除系统级 C 端默认知识库 */
export async function clearSystemDefaultKb() {
  const { data } = await request.delete('/admin/system/kb-default')
  return data
}

/** 查询知识库授权详情 (全量角色 + 用户 + 选中状态) */
export async function fetchKbAuthDetail(kbId: number) {
  const { data } = await request.get<KbAuthDetail>(`/knowledge-bases/${kbId}/auth`)
  return data
}

/** 全量替换保存知识库授权 */
export async function saveKbAuth(kbId: number, payload: KbAuthSaveRequest) {
  const { data } = await request.post(`/knowledge-bases/${kbId}/auth`, payload)
  return data
}

