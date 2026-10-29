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

/**
 * rbac 模块 API：用户/角色/权限/菜单/Dashboard 统一管理。
 * <p>路径规则: 控制器类级 @RequestMapping = /api/rbac/&lt;resource&gt; (单数), 与文件路径一一对应。
 */
export async function fetchRbacDashboard() {
  const { data } = await request.get('/rbac/dashboard')
  return data
}

export async function fetchRbacUsers(params: {
  page?: number
  size?: number
  keyword?: string
  /**
   * 部门 ID 过滤 (可选).
   * <p>不传 = 不按部门过滤 (全公司);
   * <p>传入部门 id = 后端按该部门及所有后代部门递归过滤用户.
   */
  deptId?: number
}) {
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

/**
 * 查询公司名称 (顶级部门).
 * <p>部门管理页表格上方单独展示用; null 表示无顶级部门.
 */
export async function fetchRbacRootDepartment() {
  const { data } = await request.get('/rbac/department/root')
  return data as { id: number; name: string } | null
}

/**
 * 修改公司名称 (顶级部门 name).
 * <p>仅 super_admin / developer 可调用.
 */
export async function updateRbacCompanyName(name: string) {
  const { data } = await request.put('/rbac/department/company-name', { name })
  return data
}

/** 查询某部门 (含后代) 关联的用户 ID 列表 */
export async function fetchRbacDepartmentUserIds(deptId: number) {
  const { data } = await request.get(`/rbac/department/${deptId}/users`)
  return (data ?? []) as number[]
}

/** 覆盖式为某部门 (含后代) 分配用户 */
export async function assignRbacDepartmentUsers(deptId: number, userIds: number[]) {
  const { data } = await request.put(`/rbac/department/${deptId}/users`, { userIds })
  return data
}

/**
 * 单级部门直接关联用户 (含 id / username / nickname / gender / phone / email / status / userType).
 * <p>不递归, 不含子部门用户; 已排除 developer / super_admin.
 * <p>用于"部门管理→显示部门用户"开关展开时拉取.
 */
export async function fetchRbacDepartmentUsers(deptId: number) {
  const { data } = await request.get(`/rbac/user/list-by-department/${deptId}`)
  return (data ?? []) as Array<{
    id: number
    username: string
    nickname: string
    gender: number
    phone?: string
    email?: string
    status: number
    userType: string
  }>
}

/**
 * 单用户详情 (只读 VO, 不含 passwordHash).
 * <p>用于"部门管理→部门树内点用户 nickname"弹窗.
 */
export async function fetchRbacUserDetail(userId: number) {
  const { data } = await request.get(`/rbac/user/${userId}/detail`)
  return data as {
    id: number
    username: string
    nickname: string
    gender: number
    genderText?: string
    phone?: string
    email?: string
    status: number
    statusText?: string
    userType: string
    userTypeText?: string
    departmentId?: number
    departmentName?: string
    lastLoginTime?: string
    lastLoginIp?: string
    createdAt?: string
  }
}

/** 性别字典 (0=未知 1=男 2=女) */
export async function fetchRbacGenderOptions() {
  const { data } = await request.get('/rbac/user/gender-options')
  return (data ?? []) as Array<{ value: number; label: string }>
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

export async function fetchRbacPositions(params: { page?: number; size?: number; keyword?: string; status?: number; level?: number }) {
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

/** 查询岗位已分配的用户 ID 列表 */
export async function fetchRbacPositionUserIds(id: number) {
  const { data } = await request.get(`/rbac/position/${id}/users`)
  return data
}

/** 覆盖式为岗位分配用户 */
export async function assignRbacPositionUsers(id: number, userIds: number[]) {
  const { data } = await request.put(`/rbac/position/${id}/users`, { userIds })
  return data
}
