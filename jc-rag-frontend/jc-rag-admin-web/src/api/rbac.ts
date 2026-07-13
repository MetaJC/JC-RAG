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

// =====================================================================
// 部门 / 岗位 (RBAC 组织维度)
//   控制器类级 @RequestMapping = /api/rbac/department | /api/rbac/position
// =====================================================================

export async function fetchRbacDepartments(params: { page?: number; size?: number; keyword?: string; status?: number; parentId?: number }) {
  const { data } = await request.get('/rbac/department', { params })
  return data
}

export async function fetchRbacDepartmentTree() {
  const { data } = await request.get('/rbac/department/tree')
  return data
}

export async function fetchRbacDepartment(id: number) {
  const { data } = await request.get(`/rbac/department/${id}`)
  return data
}

export async function createRbacDepartment(payload: Record<string, unknown>) {
  const { data } = await request.post('/rbac/department', payload)
  return data
}

export async function updateRbacDepartment(id: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/rbac/department/${id}`, payload)
  return data
}

export async function updateRbacDepartmentStatus(id: number, status: number) {
  const { data } = await request.put(`/rbac/department/${id}/status/${status}`)
  return data
}

/**
 * 级联切换部门状态: 父级启停时把后代 (含子级/孙级) 一起更新.
 * <p>返回受影响的部门名称列表, 供前端展示级联提示.
 */
export async function updateRbacDepartmentStatusCascade(id: number, status: number) {
  const { data } = await request.put(`/rbac/department/${id}/status/${status}/cascade`)
  return data as {
    rootId: number
    rootName: string
    status: number
    affectedCount: number
    affectedNames: string[]
  }
}

/**
 * 拖拽调整部门结构 (改变父级 / 排序).
 * <p>position: before=同级前位 / after=同级后位 / inner=变子级
 */
export async function moveRbacDepartment(id: number, payload: { targetId: number; position: 'before' | 'after' | 'inner' }) {
  const { data } = await request.put(`/rbac/department/${id}/move`, payload)
  return data
}

export async function deleteRbacDepartment(id: number) {
  const { data } = await request.delete(`/rbac/department/${id}`)
  return data
}

export async function fetchRbacPositions(params: { page?: number; size?: number; keyword?: string; status?: number; departmentId?: number; level?: number }) {
  const { data } = await request.get('/rbac/position', { params })
  return data
}

export async function fetchRbacActivePositions() {
  const { data } = await request.get('/rbac/position/active')
  return data
}

export async function fetchRbacPosition(id: number) {
  const { data } = await request.get(`/rbac/position/${id}`)
  return data
}

export async function createRbacPosition(payload: Record<string, unknown>) {
  const { data } = await request.post('/rbac/position', payload)
  return data
}

export async function updateRbacPosition(id: number, payload: Record<string, unknown>) {
  const { data } = await request.put(`/rbac/position/${id}`, payload)
  return data
}

export async function updateRbacPositionStatus(id: number, status: number) {
  const { data } = await request.put(`/rbac/position/${id}/status/${status}`)
  return data
}

export async function deleteRbacPosition(id: number) {
  const { data } = await request.delete(`/rbac/position/${id}`)
  return data
}
