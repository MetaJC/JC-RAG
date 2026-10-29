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
