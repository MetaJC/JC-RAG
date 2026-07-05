<script setup lang="ts">
/**
 * 用户管理 (RBAC 拆 4 页面之一)
 * <p>原 RbacManageView 「用户管理」 tab 独立化, 独立 URL / 独立 perm_code
 *
 * @author JC
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Key, Plus, Search, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { useAuthStore } from '../stores/auth'

import {
  assignRbacUserRoles,
  createRbacUser,
  deleteRbacUser,
  fetchRbacDashboard,
  fetchRbacRoles,
  fetchRbacUsers,
  resetRbacUserPassword,
  updateRbacUser,
  updateRbacUserStatus,
} from '../api/rbac'

const loading = ref(false)
const users = ref<any[]>([])
const usersTotal = ref(0)
const usersPage = ref(1)
const usersSize = ref(10)
const usersKeyword = ref('')

const allRoles = ref<any[]>([])

const userDialogVisible = ref(false)
const editingUserId = ref<number | null>(null)
const editingUserIsDeveloper = ref(false)

const userForm = reactive({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  userType: 'NORMAL',
  status: 1,
  password: '',
  roleIds: [] as number[],
})

/** 分配角色对话框 */
const assignRoleDialogVisible = ref(false)
const assignRoleUserId = ref<number | null>(null)
const assignRoleUserName = ref('')
const assignRoleFormRoleIds = ref<number[]>([])
const assignRoleSaving = ref(false)

/** 批量选择 */
const selectedUserIds = ref<number[]>([])
const batchDeleting = ref(false)

/** 当前登录用户 */
const authStore = useAuthStore()
/** 是否为 developer 账号 (developer 可操作 super_admin 用户) */
const isDeveloper = computed(() => authStore.user?.username === 'developer')
/** 行级锁定: developer + super_admin 用户行不允许普通系统管理员操作 */
function isLockedRow(row: any): boolean {
  if (isDeveloper.value) return false
  const u = String(row?.username || '').toLowerCase()
  return u === 'developer' || u === 'super_admin'
}

async function loadUsers() {
  const data = await fetchRbacUsers({
    page: usersPage.value,
    size: usersSize.value,
    keyword: usersKeyword.value || undefined,
  })
  users.value = data.records ?? []
  usersTotal.value = data.total ?? 0
  // 数据刷新后清空选中
  selectedUserIds.value = []
}

async function loadRolesForDropdown() {
  const data = await fetchRbacRoles({ page: 1, size: 9999 })
  allRoles.value = data.records ?? []
}

async function loadDashboard() {
  loading.value = true
  try {
    try {
      const data = await fetchRbacDashboard()
      allRoles.value = data.roles ?? allRoles.value
    } catch {
      // dashboard 接口不可用, 降级走分页接口
      try {
        await loadRolesForDropdown()
      } catch {
        // 角色加载失败不影响用户列表展示
      }
    }
    if (allRoles.value.length === 0) {
      try {
        await loadRolesForDropdown()
      } catch {
        // 同上: 兜底静默
      }
    }
    try {
      await loadUsers()
    } catch {
      // 用户列表加载失败由内部 toast 提示
    }
  } finally {
    loading.value = false
  }
}

function onSizeChange(size: number) {
  usersSize.value = size
  usersPage.value = 1
  loadUsers()
}

/** 状态切换 (el-switch) */
async function toggleUserStatus(row: any, val: number | string | boolean) {
  const newStatus = val ? 1 : 0
  if (newStatus === row.status) return
  try {
    await updateRbacUserStatus(row.id, newStatus)
    row.status = newStatus
    row.statusText = newStatus === 1 ? '启用' : '禁用'
    ElMessage.success(`已${newStatus === 1 ? '启用' : '禁用'}: ${row.username}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '状态更新失败')
  }
}

function resetUserForm() {
  editingUserId.value = null
  editingUserIsDeveloper.value = false
  userForm.username = ''
  userForm.nickname = ''
  userForm.phone = ''
  userForm.email = ''
  userForm.userType = 'NORMAL'
  userForm.status = 1
  userForm.password = ''
  userForm.roleIds = []
}

function openCreateUser() {
  resetUserForm()
  userDialogVisible.value = true
}

function openEditUser(user: any) {
  if (isLockedRow(user)) {
    ElMessage.warning('该用户 (developer / super_admin) 是系统保留, 不允许编辑')
    return
  }
  editingUserId.value = user.id
  editingUserIsDeveloper.value = user.username === 'developer' || user.userType === 'DEVELOPER'
  userForm.username = user.username
  userForm.nickname = user.nickname
  userForm.phone = user.phone || ''
  userForm.email = user.email || ''
  userForm.userType = user.userType
  userForm.status = user.status
  userForm.password = ''
  userForm.roleIds = [...(user.roleIds || [])]
  userDialogVisible.value = true
}

async function submitUser() {
  const payload = { ...userForm }
  if (editingUserId.value) {
    await updateRbacUser(editingUserId.value, payload)
    ElMessage.success('用户已更新')
  } else {
    await createRbacUser(payload)
    ElMessage.success('用户已创建')
  }
  userDialogVisible.value = false
  resetUserForm()
  await loadUsers()
}

async function resetPassword(user: any) {
  if (isLockedRow(user)) {
    ElMessage.warning('该用户 (developer / super_admin) 是系统保留, 不允许重置密码')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定将用户 [${user.username}] 的密码重置为 123456 吗?`,
      '重置密码',
      { type: 'warning', confirmButtonText: '确定重置', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  await resetRbacUserPassword(user.id, '123456')
  ElMessage.success('密码已重置为 123456')
}

async function deleteUser(user: any) {
  if (isLockedRow(user)) {
    ElMessage.warning('该用户 (developer / super_admin) 是系统保留, 不允许删除')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除用户 [${user.username}] 吗? 该操作不可恢复`,
      '删除用户',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  await deleteRbacUser(user.id)
  ElMessage.success('用户已删除')
  await loadUsers()
}

function openAssignRole(user: any) {
  if (isLockedRow(user)) {
    ElMessage.warning('该用户 (developer / super_admin) 是系统保留, 不允许分配角色')
    return
  }
  assignRoleUserId.value = user.id
  assignRoleUserName.value = user.username
  assignRoleFormRoleIds.value = [...(user.roleIds || [])]
  assignRoleDialogVisible.value = true
}

async function submitAssignRole() {
  if (assignRoleUserId.value == null) return
  assignRoleSaving.value = true
  try {
    await assignRbacUserRoles(assignRoleUserId.value, assignRoleFormRoleIds.value)
    ElMessage.success('角色分配已更新')
    assignRoleDialogVisible.value = false
    await loadUsers()
  } finally {
    assignRoleSaving.value = false
  }
}

async function batchDeleteUsers() {
  const ids = selectedUserIds.value
  if (ids.length === 0) {
    ElMessage.warning('请先勾选要删除的用户')
    return
  }
  // P0 越权修复: 二次校验 developer / super_admin 用户
  if (!isDeveloper.value) {
    const hit = users.value.filter(
      (u) => ids.includes(u.id) && isLockedRow(u),
    )
    if (hit.length > 0) {
      ElMessage.warning('已勾选的用户中包含系统保留账号 (developer / super_admin), 不允许删除')
      return
    }
  }
  try {
    await ElMessageBox.confirm(
      `确定删除已勾选的 ${ids.length} 个用户吗? 该操作不可恢复`,
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
        await deleteRbacUser(id)
        ok++
      } catch {
        fail++
      }
    }
    if (fail === 0) {
      ElMessage.success(`已删除 ${ok} 个用户`)
    } else {
      ElMessage.warning(`成功 ${ok} 个, 失败 ${fail} 个`)
    }
    await loadUsers()
  } finally {
    batchDeleting.value = false
  }
}

function onSelectionChange(rows: any[]) {
  // P0 越权修复: 选中时过滤掉 developer / super_admin 行
  if (!isDeveloper.value) {
    const filtered = rows.filter((r) => !isLockedRow(r))
    selectedUserIds.value = filtered.map((r) => r.id).filter((x) => x != null)
    return
  }
  selectedUserIds.value = rows.map((r) => r.id).filter((x) => x != null)
}

/** 行 checkbox 是否勾选 */
function isSelected(row: any): boolean {
  return row && row.id != null && selectedUserIds.value.includes(row.id)
}

/** 行 checkbox 切换 (过滤掉 developer / super_admin) */
function onRowCheckChange(row: any, val: boolean) {
  if (!row || row.id == null) return
  if (isLockedRow(row)) return
  const set = new Set(selectedUserIds.value)
  if (val) set.add(row.id)
  else set.delete(row.id)
  selectedUserIds.value = Array.from(set)
}

onMounted(loadDashboard)
</script>

<template>
  <div>
    <header class="page-header">
      <div>
        <h1>用户管理</h1>
        <p>管理系统用户、角色绑定、密码重置</p>
      </div>
    </header>

    <el-card class="panel-card" shadow="never" v-loading="loading">
      <div class="toolbar">
        <el-input
          v-model="usersKeyword"
          placeholder="搜索用户名/昵称/手机号"
          clearable
          style="width: 280px"
          @clear="loadUsers"
          @keyup.enter="loadUsers"
        >
          <template #append>
            <el-button :icon="Search" @click="loadUsers">搜索</el-button>
          </template>
        </el-input>
        <div class="toolbar-spacer" />
        <el-button
          type="danger"
          plain
          :icon="Delete"
          :disabled="selectedUserIds.length === 0"
          :loading="batchDeleting"
          @click="batchDeleteUsers"
        >
          批量删除
          <span v-if="selectedUserIds.length > 0" class="batch-count">({{ selectedUserIds.length }})</span>
        </el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateUser">新建用户</el-button>
      </div>
      <el-table :data="users" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48">
          <template #default="{ row }">
            <el-checkbox
              :model-value="isSelected(row)"
              :disabled="isLockedRow(row)"
              @change="(val: boolean) => onRowCheckChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <template v-if="row.roleNames && row.roleNames.length">
              <el-tag
                v-for="(name, idx) in row.roleNames"
                :key="idx"
                size="small"
                effect="plain"
                class="role-tag"
              >
                {{ name }}
              </el-tag>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" min-width="100">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.userTypeText || row.userType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="isLockedRow(row)"
              inline-prompt
              active-text="启用"
              inactive-text="禁用"
              @update:model-value="(val: number | string | boolean) => toggleUserStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="{ row }">
            <el-tooltip :content="isLockedRow(row) ? '该用户 (developer / super_admin) 是系统保留, 不允许编辑' : '编辑'" placement="top">
              <el-button link type="primary" :icon="Edit" :disabled="isLockedRow(row)" @click="openEditUser(row)" />
            </el-tooltip>
            <el-tooltip :content="isLockedRow(row) ? '该用户 (developer / super_admin) 是系统保留, 不允许分配角色' : '分配角色'" placement="top">
              <el-button
                link
                type="success"
                :icon="UserFilled"
                :disabled="isLockedRow(row)"
                @click="openAssignRole(row)"
              />
            </el-tooltip>
            <el-tooltip :content="isLockedRow(row) ? '该用户 (developer / super_admin) 是系统保留, 不允许重置密码' : '重置密码'" placement="top">
              <el-button
                link
                type="warning"
                :icon="Key"
                :disabled="isLockedRow(row)"
                @click="resetPassword(row)"
              />
            </el-tooltip>
            <el-tooltip :content="isLockedRow(row) ? '该用户 (developer / super_admin) 是系统保留, 不允许删除' : '删除'" placement="top">
              <el-button
                link
                type="danger"
                :icon="Delete"
                :disabled="isLockedRow(row)"
                @click="deleteUser(row)"
              />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="usersPage"
        v-model:page-size="usersSize"
        :total="usersTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadUsers"
        @size-change="onSizeChange"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <el-dialog
      v-model="userDialogVisible"
      :title="editingUserId ? '编辑用户' : '新建用户'"
      width="560px"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" :disabled="Boolean(editingUserId)" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="userForm.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="userForm.userType" :disabled="editingUserIsDeveloper">
            <el-option v-if="editingUserIsDeveloper" label="开发者账号" value="DEVELOPER" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="NORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.roleIds" multiple :disabled="editingUserIsDeveloper">
            <el-option
              v-for="role in allRoles"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="userForm.password"
            type="password"
            show-password
            placeholder="新建用户建议填写，编辑时留空表示不修改"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="assignRoleDialogVisible"
      :title="`分配角色 - ${assignRoleUserName}`"
      width="480px"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="角色（可多选）">
          <el-select v-model="assignRoleFormRoleIds" multiple filterable style="width: 100%">
            <el-option
              v-for="role in allRoles"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignRoleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignRoleSaving" @click="submitAssignRole">保存</el-button>
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

.role-tag {
  margin-right: 4px;
}

.role-tag:last-child {
  margin-right: 0;
}

.text-muted {
  color: #9ca3af;
}
</style>
