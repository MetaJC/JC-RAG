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
