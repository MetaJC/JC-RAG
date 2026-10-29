<!--
  Copyright 2024-2026 OpenJC
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  
  本文件来源于 JC RAG Community Edition
  开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
  二次开发请保留此出处声明。
 -->


<script setup lang="ts">
/**
 * 角色管理 (RBAC 拆 4 页面之一)
 * <p>原 RbacManageView 「角色管理」 tab 独立化
 *
 * @author JC
 */
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { Delete, Edit, Plus, QuestionFilled, Search, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { useAuthStore } from '../stores/auth'

import {
  assignRbacRoleUsers,
  createRbacRole,
  deleteRbacRole,
  fetchRbacDashboard,
  fetchRbacPermissions,
  fetchRbacRoleUsers,
  fetchRbacRoles,
  fetchRbacUserMenuTree,
  fetchRbacUsers,
  updateRbacRole,
  updateRbacRoleStatus,
} from '../api/rbac'

const loading = ref(false)
const roles = ref<any[]>([])
const rolesTotal = ref(0)
const rolesPage = ref(1)
const rolesSize = ref(10)
const rolesKeyword = ref('')

const menuTree = ref<any[]>([])
const permissions = ref<any[]>([])

const roleDialogVisible = ref(false)
const editingRoleId = ref<number | null>(null)
const menuTreeRef = ref<any>(null)
const apiPermIds = ref<number[]>([])

const roleForm = reactive({
  roleCode: '',
  roleName: '',
  roleDesc: '',
  status: 1,
  isAdmin: 0,
  permissionIds: [] as number[],
})

/** 当前编辑角色是否为 super_admin (锁定 isAdmin 开关) */
const isSuperAdminRole = computed(() => {
  return String(roleForm.roleCode || '').toLowerCase() === 'super_admin'
})

/** 当前登录用户 */
const authStore = useAuthStore()
/** 是否为 developer 账号 (developer 可修改 super_admin 的 isAdmin) */
const isDeveloper = computed(() => authStore.user?.username === 'developer')
/** super_admin 角色 isAdmin 开关: 非 developer 锁定 */
const isSuperAdminIsAdminLocked = computed(() => isSuperAdminRole.value && !isDeveloper.value)
/** 行级: 是否锁住 super_admin 行的所有按钮 (developer 跳过) */
function isSuperAdminRowLocked(row: any): boolean {
  if (isDeveloper.value) return false
  return String(row?.roleCode || '').toLowerCase() === 'super_admin'
}

/** 批量选择 */
const selectedRoleIds = ref<number[]>([])
const batchDeleting = ref(false)

/** 菜单树：全选/全不选、折叠/展开 状态（与 el-checkbox v-model 双向绑定） */
const menuAllChecked = ref(false)
const menuAllExpanded = ref(true)

/** 分配用户对话框 */
const assignUserDialogVisible = ref(false)
const assignUserRoleId = ref<number | null>(null)
const assignUserRoleName = ref('')
const allUsers = ref<any[]>([])
const assignUserSelectedIds = ref<number[]>([])
const assignUserKeyword = ref('')
const assignUserSaving = ref(false)

async function loadRoles() {
  const data = await fetchRbacRoles({
    page: rolesPage.value,
    size: rolesSize.value,
    keyword: rolesKeyword.value || undefined,
  })
  roles.value = data.records ?? []
  rolesTotal.value = data.total ?? 0
  selectedRoleIds.value = []
}

async function loadMenuTree() {
  // P0 越权修复: 用 user-tree, 菜单按当前用户权限过滤, 避免系统管理员给角色分配自己都没权限的菜单
  const data = await fetchRbacUserMenuTree()
  menuTree.value = data ?? []
}

async function loadPermissions() {
  const data = await fetchRbacPermissions({ page: 1, size: 9999 })
  permissions.value = data.records ?? []
}

async function loadDashboard() {
  loading.value = true
  try {
    try {
      const data = await fetchRbacDashboard()
      permissions.value = data.permissions ?? permissions.value
    } catch {
      // ignore
    }
    if (permissions.value.length === 0) await loadPermissions()
    await Promise.all([loadRoles(), loadMenuTree()])
  } finally {
    loading.value = false
  }
}

function onSizeChange(size: number) {
  rolesSize.value = size
  rolesPage.value = 1
  loadRoles()
}

/** 状态切换 (el-switch) */
async function toggleRoleStatus(row: any, val: number | string | boolean) {
  const newStatus = val ? 1 : 0
  if (newStatus === row.status) return
  try {
    await updateRbacRoleStatus(row.id, newStatus)
    row.status = newStatus
    row.statusText = newStatus === 1 ? '启用' : '禁用'
    ElMessage.success(`已${newStatus === 1 ? '启用' : '禁用'}: ${row.name}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '状态更新失败')
  }
}

function resetRoleForm() {
  editingRoleId.value = null
  roleForm.roleCode = ''
  roleForm.roleName = ''
  roleForm.roleDesc = ''
  roleForm.status = 1
  roleForm.isAdmin = 0
  roleForm.permissionIds = []
  apiPermIds.value = []
}

function openCreateRole() {
  resetRoleForm()
  roleDialogVisible.value = true
}

function openEditRole(role: any) {
  if (isSuperAdminRowLocked(role)) {
    ElMessage.warning('超级管理员角色 (super_admin) 是系统保留, 不允许编辑')
    return
  }
  editingRoleId.value = role.id
  roleForm.roleCode = role.roleCode
  roleForm.roleName = role.roleName
  roleForm.roleDesc = role.roleDesc || ''
  roleForm.status = role.status
  roleForm.isAdmin = role.isAdmin == null ? 0 : role.isAdmin
  roleForm.permissionIds = [...(role.permissionIds || [])]
  // API/按钮权限: 按 permType 过滤后赋值; 菜单权限由 el-tree 的 default-checked-keys 自动回显
  apiPermIds.value = (role.permissionIds || []).filter((id: number) => {
    const p = permissions.value.find((x: any) => x.id === id)
    return p && p.permType !== 'MENU'
  })
  roleDialogVisible.value = true
}

/** 递归收集所有节点 id */
function collectAllMenuIds(nodes: any[], acc: number[]) {
  for (const n of nodes) {
    if (n && n.id != null) acc.push(n.id)
    if (n?.children?.length) collectAllMenuIds(n.children, acc)
  }
}

function totalMenuCount(): number {
  const ids: number[] = []
  collectAllMenuIds(menuTree.value, ids)
  return ids.length
}

/** 复选框 v-model → 全选/全不选 */
function onCheckAllMenu(checked: boolean) {
  if (!menuTreeRef.value) return
  const ids: number[] = []
  collectAllMenuIds(menuTree.value, ids)
  menuTreeRef.value.setCheckedKeys(checked ? ids : [])
}

/** el-tree 节点勾选变化 → 同步复选框 v-model */
function onMenuTreeCheck() {
  const total = totalMenuCount()
  if (total === 0) {
    menuAllChecked.value = false
    return
  }
  const checked = new Set<number>([
    ...((menuTreeRef.value?.getCheckedKeys?.() || []) as number[]),
    ...((menuTreeRef.value?.getHalfCheckedKeys?.() || []) as number[]),
  ])
  menuAllChecked.value = checked.size >= total
}

/** 复选框 v-model → 展开/折叠 */
function onToggleExpandMenu(expand: boolean) {
  const store = (menuTreeRef.value as any)?.store?.nodesMap
  if (!store) return
  Object.values(store).forEach((node: any) => {
    if (node && typeof node.expanded === 'boolean') node.expanded = expand
  })
}

/** el-tree 节点展开变化 → 同步复选框 v-model */
function onMenuTreeExpand() {
  const store = (menuTreeRef.value as any)?.store?.nodesMap
  if (!store) {
    menuAllExpanded.value = true
    return
  }
  const nodes = Object.values(store) as any[]
  menuAllExpanded.value = nodes.length > 0 && nodes.every((n) => n?.expanded === true)
}

/** 弹窗打开时刷新复选框初始态（destroy-on-close 后 menuTreeRef 会重建） */
watch(roleDialogVisible, (visible) => {
  if (!visible) return
  nextTick(() => {
    onMenuTreeCheck()
    onMenuTreeExpand()
  })
})

async function submitRole() {
  // super_admin 兜底: 前端双保险, 即便被绕过, 后端仍会拒绝 (developer 不受此限)
  if (isSuperAdminIsAdminLocked.value && roleForm.isAdmin !== 1) {
    ElMessage.warning('超级管理员角色 (super_admin) 必须允许登录后台')
    return
  }
  // 合并菜单树勾选 + API/按钮下拉
  const menuChecked: number[] = (menuTreeRef.value?.getCheckedKeys?.() || []) as number[]
  const menuHalfChecked: number[] = (menuTreeRef.value?.getHalfCheckedKeys?.() || []) as number[]
  const allMenuIds = [...new Set([...menuChecked, ...menuHalfChecked])]
  const merged = [...new Set([...allMenuIds, ...apiPermIds.value])]
  const payload = { ...roleForm, permissionIds: merged }
  if (editingRoleId.value) {
    await updateRbacRole(editingRoleId.value, payload)
    ElMessage.success('角色已更新')
  } else {
    await createRbacRole(payload)
    ElMessage.success('角色已创建')
  }
  roleDialogVisible.value = false
  resetRoleForm()
  await loadRoles()
}

async function deleteRole(row: any) {
  if (isSuperAdminRowLocked(row)) {
    ElMessage.warning('超级管理员角色 (super_admin) 是系统保留, 不允许删除')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除角色 [${row.roleName}] 吗? 该操作不可恢复`,
      '删除角色',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  await deleteRbacRole(row.id)
  ElMessage.success('角色已删除')
  await loadRoles()
}

async function openAssignUser(row: any) {
  if (isSuperAdminRowLocked(row)) {
    ElMessage.warning('超级管理员角色 (super_admin) 是系统保留, 不允许分配用户')
    return
  }
  assignUserRoleId.value = row.id
  assignUserRoleName.value = row.roleName
  assignUserSelectedIds.value = []
  assignUserKeyword.value = ''
  assignUserDialogVisible.value = true
  if (allUsers.value.length === 0) {
    const data = await fetchRbacUsers({ page: 1, size: 9999 })
    allUsers.value = data.records ?? []
  }
  const bound = await fetchRbacRoleUsers(row.id)
  assignUserSelectedIds.value = Array.isArray(bound) ? bound : []
}

async function submitAssignUser() {
  if (assignUserRoleId.value == null) return
  assignUserSaving.value = true
  try {
    await assignRbacRoleUsers(assignUserRoleId.value, assignUserSelectedIds.value)
    ElMessage.success('用户分配已更新')
    assignUserDialogVisible.value = false
  } finally {
    assignUserSaving.value = false
  }
}

const filteredUsersForAssign = computed(() => {
  const kw = assignUserKeyword.value.trim().toLowerCase()
  if (!kw) return allUsers.value
  return allUsers.value.filter((u: any) => {
    return (u.username || '').toLowerCase().includes(kw)
      || (u.nickname || '').toLowerCase().includes(kw)
      || (u.email || '').toLowerCase().includes(kw)
  })
})

async function batchDeleteRoles() {
  const ids = selectedRoleIds.value
  if (ids.length === 0) {
    ElMessage.warning('请先勾选要删除的角色')
    return
  }
  // P0 越权修复: 二次校验, 即便 onSelectionChange 被绕过, 也不允许删 super_admin
  if (!isDeveloper.value) {
    const hit = roles.value.filter(
      (r) => ids.includes(r.id) && String(r?.roleCode || '').toLowerCase() === 'super_admin',
    )
    if (hit.length > 0) {
      ElMessage.warning('已勾选的角色中包含超级管理员角色 (super_admin), 不允许删除')
      return
    }
  }
  try {
    await ElMessageBox.confirm(
      `确定删除已勾选的 ${ids.length} 个角色吗? 该操作不可恢复`,
      '批量删除',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  batchDeleting.value = true
  try {
    let ok = 0
    let fail = 0
    for (const id of ids) {
      try {
        await deleteRbacRole(id)
        ok++
      } catch {
        fail++
      }
    }
    if (fail === 0) {
      ElMessage.success(`已删除 ${ok} 个角色`)
    } else {
      ElMessage.warning(`成功 ${ok} 个, 失败 ${fail} 个`)
    }
    await loadRoles()
  } finally {
    batchDeleting.value = false
  }
}

function onSelectionChange(rows: any[]) {
  // P0 越权修复: 选中时过滤掉 super_admin 行, 防止批量误删
  if (!isDeveloper.value) {
    const filtered = rows.filter(
      (r) => String(r?.roleCode || '').toLowerCase() !== 'super_admin',
    )
    selectedRoleIds.value = filtered.map((r) => r.id).filter((x) => x != null)
    return
  }
  selectedRoleIds.value = rows.map((r) => r.id).filter((x) => x != null)
}

/** 行 checkbox 是否勾选 */
function isSelected(row: any): boolean {
  return row && row.id != null && selectedRoleIds.value.includes(row.id)
}

/** 行 checkbox 切换 (过滤掉 super_admin) */
function onRowCheckChange(row: any, val: boolean) {
  if (!row || row.id == null) return
  if (isSuperAdminRowLocked(row)) return
  const set = new Set(selectedRoleIds.value)
  if (val) set.add(row.id)
  else set.delete(row.id)
  selectedRoleIds.value = Array.from(set)
}

onMounted(loadDashboard)
</script>

<template>
  <div>
    <header class="page-header">
      <div>
        <h1>角色管理</h1>
        <p>管理角色、绑定菜单与 API/按钮权限</p>
      </div>
    </header>

    <el-card class="panel-card" shadow="never" v-loading="loading">
      <div class="toolbar">
        <el-input
          v-model="rolesKeyword"
          placeholder="搜索角色名称/编码"
          clearable
          style="width: 280px"
          @clear="loadRoles"
          @keyup.enter="loadRoles"
        >
          <template #append>
            <el-button :icon="Search" @click="loadRoles">搜索</el-button>
          </template>
        </el-input>
        <div class="toolbar-spacer" />
        <el-button
          type="danger"
          plain
          :icon="Delete"
          :disabled="selectedRoleIds.length === 0"
          :loading="batchDeleting"
          @click="batchDeleteRoles"
        >
          批量删除
          <span v-if="selectedRoleIds.length > 0" class="batch-count">({{ selectedRoleIds.length }})</span>
        </el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateRole">新建角色</el-button>
      </div>
      <el-table :data="roles" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48">
          <template #default="{ row }">
            <el-checkbox
              :model-value="isSelected(row)"
              :disabled="isSuperAdminRowLocked(row)"
              @change="(val: boolean) => onRowCheckChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="roleCode" label="角色编码" min-width="140" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleDesc" label="说明" min-width="180" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              inline-prompt
              active-text="启用"
              inactive-text="禁用"
              :disabled="isSuperAdminRowLocked(row)"
              @update:model-value="(val: number | string | boolean) => toggleRoleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="允许登录后台" min-width="120">
          <template #default="{ row }">
            <el-tag
              :type="row.isAdmin === 1 ? 'success' : 'info'"
              size="small"
              effect="light"
            >
              {{ row.isAdmin === 1 ? '允许' : '禁止' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-tooltip :content="isSuperAdminRowLocked(row) ? '超级管理员角色 (super_admin) 是系统保留, 不允许编辑' : '编辑'" placement="top">
              <el-button link type="primary" :icon="Edit" :disabled="isSuperAdminRowLocked(row)" @click="openEditRole(row)" />
            </el-tooltip>
            <el-tooltip :content="isSuperAdminRowLocked(row) ? '超级管理员角色 (super_admin) 是系统保留, 不允许分配用户' : '分配用户'" placement="top">
              <el-button link type="success" :icon="User" :disabled="isSuperAdminRowLocked(row)" @click="openAssignUser(row)" />
            </el-tooltip>
            <el-tooltip :content="isSuperAdminRowLocked(row) ? '超级管理员角色 (super_admin) 是系统保留, 不允许删除' : '删除'" placement="top">
              <el-button link type="danger" :icon="Delete" :disabled="isSuperAdminRowLocked(row)" @click="deleteRole(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="rolesPage"
        v-model:page-size="rolesSize"
        :total="rolesTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadRoles"
        @size-change="onSizeChange"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <el-dialog
      v-model="roleDialogVisible"
      :title="editingRoleId ? '编辑角色' : '新建角色'"
      width="720px"
      custom-class="role-edit-dialog"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="角色编码">
          <el-input v-model="roleForm.roleCode" :disabled="Boolean(editingRoleId)" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="roleForm.roleName" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="roleForm.roleDesc" />
        </el-form-item>
        <el-form-item label="">
          <el-row :gutter="24" class="role-switch-row">
            <el-col :span="12" class="switch-cell">
              <div class="cell-label">状态</div>
              <el-switch
                v-model="roleForm.status"
                :active-value="1"
                :inactive-value="0"
                active-text="启用"
                inactive-text="禁用"
                inline-prompt
              />
            </el-col>
            <el-col :span="12" class="switch-cell">
              <div class="cell-label">
                <span>允许登录后台</span>
                <el-tooltip
                  v-if="!isSuperAdminRole"
                  content="仅勾选『允许登录后台』的角色才可登录管理端, 普通角色默认禁止"
                  placement="top"
                >
                  <el-icon class="hint-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
                <el-tooltip
                  v-else
                  content="超级管理员角色 (super_admin) 强制允许登录后台, 且不可关闭"
                  placement="top"
                >
                  <el-icon class="hint-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <el-switch
                v-model="roleForm.isAdmin"
                :active-value="1"
                :inactive-value="0"
                active-text="允许"
                inactive-text="禁止"
                inline-prompt
                :disabled="isSuperAdminIsAdminLocked"
              />
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="菜单权限">
          <div class="menu-toolbar">
            <el-checkbox v-model="menuAllChecked" @change="onCheckAllMenu">
              {{ menuAllChecked ? '全不选' : '全选' }}
            </el-checkbox>
            <el-checkbox v-model="menuAllExpanded" @change="onToggleExpandMenu">
              {{ menuAllExpanded ? '折叠' : '展开' }}
            </el-checkbox>
          </div>
          <el-tree
            ref="menuTreeRef"
            :data="menuTree"
            node-key="id"
            :props="{ children: 'children', label: 'title' }"
            show-checkbox
            :default-checked-keys="roleForm.permissionIds"
            :default-expand-all="true"
            empty-text="暂无菜单"
            class="role-menu-tree"
            @check="onMenuTreeCheck"
            @expand="onMenuTreeExpand"
          />
        </el-form-item>
        <el-form-item label="API / 按钮权限">
          <el-select v-model="apiPermIds" multiple filterable placeholder="按 permCode 筛选">
            <el-option
              v-for="p in permissions.filter((x: any) => x.permType !== 'MENU')"
              :key="p.id"
              :label="`${p.permCode} (${p.permType})`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="assignUserDialogVisible"
      :title="`分配用户 - ${assignUserRoleName}`"
      width="560px"
      destroy-on-close
    >
      <el-input
        v-model="assignUserKeyword"
        placeholder="按用户名/昵称/邮箱搜索"
        clearable
        style="margin-bottom: 12px"
      />
      <el-checkbox-group v-model="assignUserSelectedIds" class="assign-user-list">
        <el-checkbox
          v-for="u in filteredUsersForAssign"
          :key="u.id"
          :value="u.id"
          class="assign-user-item"
        >
          <span class="assign-user-name">{{ u.username }}</span>
          <span v-if="u.nickname" class="assign-user-nick">（{{ u.nickname }}）</span>
          <span v-if="u.email" class="assign-user-email">{{ u.email }}</span>
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="assignUserDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignUserSaving" @click="submitAssignUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #1f2937;
}

.page-header p {
  color: #6b7280;
  font-size: 13px;
  margin: 0;
}

.panel-card {
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.toolbar-spacer {
  flex: 1;
}

.batch-count {
  margin-left: 2px;
}

/* 角色编辑弹窗：宽 720px × 高 509px ≈ √2 : 1 */
.role-edit-dialog {
  height: 509px;
  margin: 5vh auto 0 !important;
}

.role-edit-dialog .el-dialog__body {
  height: calc(100% - 124px);
  padding: 12px 24px 0;
  overflow-y: auto;
}

.role-menu-tree {
  display: block;
  width: 100%;
  max-height: 280px;
  min-height: 220px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 12px;
  background: #fafbfc;
}

.menu-toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  justify-content: flex-start;
}

.form-hint {
  display: inline-block;
  margin-left: 12px;
  color: #94a3b8;
  font-size: 12px;
}

.assign-user-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 360px;
  overflow-y: auto;
  padding: 8px 4px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.assign-user-item {
  margin-right: 0;
}

.assign-user-name {
  font-weight: 500;
  color: #1f2937;
}

.assign-user-nick {
  color: #6b7280;
  margin-left: 2px;
}

.assign-user-email {
  color: #9ca3af;
  margin-left: 8px;
  font-size: 12px;
}

/* 状态设置 行: 两列开关横排 */
.role-switch-row {
  width: 100%;
}
.switch-cell {
  display: flex;
  flex-direction: column;
}
.cell-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  height: 22px;
  line-height: 22px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.hint-icon {
  color: #94a3b8;
  cursor: help;
  font-size: 14px;
}
.hint-icon:hover {
  color: #3b82f6;
}
</style>
