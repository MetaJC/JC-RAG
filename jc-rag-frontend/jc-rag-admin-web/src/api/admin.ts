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

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 60000,
  withCredentials: true,
})

/**
 * 临时杂项 API：仅包含暂未迁移到 sys/rag 模块的接口（如 data-permission，后续将归入 sys）。
 * <p>命名规则: 资源名单数，路径 /admin/&lt;resource&gt;，与旧版保持兼容。
 */
export const adminApi = {
  listDataPermissions: (params?: { roleId?: number; resourceType?: string; page?: number; size?: number; keyword?: string }) =>
    request.get('/admin/data-permissions', { params }).then((r) => r.data),
  saveDataPermission: (data: any) => request.post('/admin/data-permissions', data).then((r) => r.data),
  deleteDataPermission: (id: number) => request.delete(`/admin/data-permissions/${id}`).then((r) => r.data),
}
