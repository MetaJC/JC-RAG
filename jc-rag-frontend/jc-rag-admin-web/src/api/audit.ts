import { request } from './http'

export async function fetchAuditDashboard() {
  const { data } = await request.get('/audit/dashboard')
  return data
}

export interface AuditPageParams {
  page?: number
  size?: number
  keyword?: string
}

export async function fetchOperationLogs(params: AuditPageParams) {
  const { data } = await request.get('/sys/operation-log', { params })
  return data
}

export async function fetchRagCallLogs(params: AuditPageParams) {
  const { data } = await request.get('/audit/rag-call-logs', { params })
  return data
}

export async function fetchExceptionLogs(params: AuditPageParams) {
  const { data } = await request.get('/audit/exception-logs', { params })
  return data
}

/** 安全审计日志 (敏感词命中/拒绝/脱敏全留痕). */
export interface SecurityAuditQuery {
  page?: number
  size?: number
  userId?: number
  auditType?: string
  riskLevel?: string
  keyword?: string
  since?: string
}

export async function fetchSecurityAuditLogs(params: SecurityAuditQuery) {
  const { data } = await request.get('/admin/security/audit', { params })
  return data
}

export async function fetchSecurityAuditSummary() {
  const { data } = await request.get('/admin/security/audit/summary')
  return data
}
