import { request } from './http'

/**
 * rbac 模块 API：用户/角色/权限/菜单/Dashboard 统一管理。
 * <p>路径规则: 控制器类级 @RequestMapping = /api/rbac/&lt;resource&gt; (单数), 与文件路径一一对应。
 */
export async function fetchRbacDashboard() {
  const { data } = await request.get('/rbac/dashboard')
  return data
}

export async function fetchRbacUsers(params: { page?: number; size?: number; keyword?: string }) {
  const { data } = await request.get('/rbac/user', { params })
  return data
}

export async function fetchRbacRoles(params: { page?: number; size?: number; keyword?: string }) {
  const { data } = await request.get('/rbac/role', { params })
  return data
}

export async function fetchRbacPermissions(params: { page?: number; size?: number; keyword?: string; permType?: string }) {
  const { data } = await request.get('/rbac/permission', { params })
  return data
}

export async function fetchRbacMenus() {
  const { data } = await request.get('/rbac/menu')
  return data
}

export async function fetchRbacMenuTree() {
  const { data } = await request.get('/rbac/menu/tree')
  return data
}

/** 当前用户权限范围内的菜单树 (P0 越权修复) */
export async function fetchRbacUserMenuTree() {
  const { data } = await request.get('/rbac/menu/user-tree')
  return data
}

export async function createRbacMenu(payload: Record<string, unknown>) {
  const { data } = await request.post('/rbac/menu', payload)
  return data
}

export async function updateRbacMenu(menuId: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/rbac/menu/${menuId}`, payload)
  return data
}

export async function deleteRbacMenu(menuId: number) {
  const { data } = await request.delete(`/rbac/menu/${menuId}`)
  return data
}

/** 拖拽移动节点: parentId=0/null 移到根, sort 由前端按 drop position 计算 */
export async function moveRbacMenu(menuId: number, payload: { parentId?: number | null; sort?: number }) {
  const { data } = await request.put(`/rbac/menu/${menuId}/move`, payload)
  return data
}

export async function createRbacUser(payload: Record<string, unknown>) {
  const { data } = await request.post('/rbac/user', payload)
  return data
}

export async function updateRbacUser(userId: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/rbac/user/${userId}`, payload)
  return data
}

/** 仅切换用户状态 (后端不做全量校验) */
export async function updateRbacUserStatus(userId: number, status: number) {
  const { data } = await request.put(`/rbac/user/${userId}/status`, { status })
  return data
}

export async function deleteRbacUser(userId: number) {
  const { data } = await request.delete(`/rbac/user/${userId}`)
  return data
}

export async function resetRbacUserPassword(userId: number, newPassword: string) {
  const { data } = await request.post(`/rbac/user/${userId}/reset-password`, { newPassword })
  return data
}

export async function createRbacRole(payload: Record<string, unknown>) {
  const { data } = await request.post('/rbac/role', payload)
  return data
}

export async function updateRbacRole(roleId: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/rbac/role/${roleId}`, payload)
  return data
}

/** 仅切换角色状态 (后端不做全量校验) */
export async function updateRbacRoleStatus(roleId: number, status: number) {
  const { data } = await request.put(`/rbac/role/${roleId}/status`, { status })
  return data
}

export async function deleteRbacRole(roleId: number) {
  const { data } = await request.delete(`/rbac/role/${roleId}`)
  return data
}

/** 查询该角色下已绑定的用户 ID 列表 */
export async function fetchRbacRoleUsers(roleId: number) {
  const { data } = await request.get(`/rbac/role/${roleId}/users`)
  return data
}

/** 覆盖式为用户分配角色 */
export async function assignRbacUserRoles(userId: number, roleIds: number[]) {
  const { data } = await request.put(`/rbac/user/${userId}/roles`, { roleIds })
  return data
}

/** 覆盖式为该角色分配用户 */
export async function assignRbacRoleUsers(roleId: number, userIds: number[]) {
  const { data } = await request.put(`/rbac/role/${roleId}/users`, { userIds })
  return data
}

export async function createRbacPermission(payload: Record<string, unknown>) {
  const { data } = await request.post('/rbac/permission', payload)
  return data
}

export async function updateRbacPermission(permissionId: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/rbac/permission/${permissionId}`, payload)
  return data
}

export async function deleteRbacPermission(permissionId: number) {
  const { data } = await request.delete(`/rbac/permission/${permissionId}`)
  return data
}
