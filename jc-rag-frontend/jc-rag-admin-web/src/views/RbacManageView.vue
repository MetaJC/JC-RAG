<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import {
  createRbacPermission,
  createRbacRole,
  createRbacUser,
  fetchRbacDashboard,
  fetchRbacPermissions,
  fetchRbacRoles,
  fetchRbacUsers,
  resetRbacUserPassword,
  updateRbacPermission,
  updateRbacRole,
  updateRbacUser,
} from '../api/rbac'

const loading = ref(false)
const activeTab = ref('users')

const users = ref<any[]>([])
const usersTotal = ref(0)
const usersPage = ref(1)
const usersSize = ref(10)
const usersKeyword = ref('')

const roles = ref<any[]>([])
const rolesTotal = ref(0)
const rolesPage = ref(1)
const rolesSize = ref(10)
const rolesKeyword = ref('')

const allRoles = ref<any[]>([])
const permissions = ref<any[]>([])
const menus = computed(() => permissions.value.filter((item) => item.permType === 'MENU'))
const actionPermissions = computed(() => permissions.value.filter((item) => item.permType !== 'MENU'))

const userDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const permissionDialogMode = ref<'MENU' | 'BUTTON' | 'API'>('MENU')
const editingUserId = ref<number | null>(null)
const editingRoleId = ref<number | null>(null)
const editingPermissionId = ref<number | null>(null)
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

const roleForm = reactive({
  roleCode: '',
  roleName: '',
  roleDesc: '',
  status: 1,
  permissionIds: [] as number[],
})

const permissionForm = reactive({
  permCode: '',
  permName: '',
  permType: 'MENU',
  parentId: null as number | null,
  path: '',
  method: '',
  status: 1,
})

async function loadDashboard() {
  loading.value = true
  try {
    const data = await fetchRbacDashboard()
    // 兼容旧接口（dashboard 可能不返回 roles/permissions）
    allRoles.value = data.roles ?? allRoles.value
    permissions.value = data.permissions ?? permissions.value
  } catch (e) {
    console.error('dashboard load failed, fallback to direct queries:', e)
  }
  // 独立接口兜底，确保菜单/权限 tab 即便 dashboard 失败也能正常加载
  try {
    if (allRoles.value.length === 0) {
      const roleData = await fetchRbacRoles({ page: 1, size: 9999 })
      allRoles.value = roleData.records ?? []
    }
    if (permissions.value.length === 0) {
      const permData = await fetchRbacPermissions({ page: 1, size: 9999 })
      permissions.value = permData.records ?? []
    }
  } catch (e) {
    console.error('rbac fallback list load failed:', e)
  }
  try {
    await Promise.all([loadUsers(), loadRoles()])
  } catch (e) {
    console.error('rbac list load failed:', e)
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  const data = await fetchRbacUsers({ page: usersPage.value, size: usersSize.value, keyword: usersKeyword.value || undefined })
  users.value = data.records ?? []
  usersTotal.value = data.total ?? 0
}

async function loadRoles() {
  const data = await fetchRbacRoles({ page: rolesPage.value, size: rolesSize.value, keyword: rolesKeyword.value || undefined })
  roles.value = data.records ?? []
  rolesTotal.value = data.total ?? 0
}

function onUsersSizeChange(size: number) {
  usersSize.value = size
  usersPage.value = 1
  loadUsers()
}

function onRolesSizeChange(size: number) {
  rolesSize.value = size
  rolesPage.value = 1
  loadRoles()
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
  await loadDashboard()
}

async function resetPassword(userId: number) {
  await resetRbacUserPassword(userId, '123456')
  ElMessage.success('密码已重置为 123456')
}

function resetRoleForm() {
  editingRoleId.value = null
  roleForm.roleCode = ''
  roleForm.roleName = ''
  roleForm.roleDesc = ''
  roleForm.status = 1
  roleForm.permissionIds = []
}

function openCreateRole() {
  resetRoleForm()
  roleDialogVisible.value = true
}

function openEditRole(role: any) {
  editingRoleId.value = role.id
  roleForm.roleCode = role.roleCode
  roleForm.roleName = role.roleName
  roleForm.roleDesc = role.roleDesc || ''
  roleForm.status = role.status
  roleForm.permissionIds = [...(role.permissionIds || [])]
  roleDialogVisible.value = true
}

function statusTagType(statusText?: string) {
  if (statusText === '启用' || statusText === '成功') return 'success'
  if (statusText === '禁用' || statusText === '失败') return 'danger'
  if (statusText === '处理中' || statusText === '待处理') return 'warning'
  return 'info'
}

async function submitRole() {
  const payload = { ...roleForm }
  if (editingRoleId.value) {
    await updateRbacRole(editingRoleId.value, payload)
    ElMessage.success('角色已更新')
  } else {
    await createRbacRole(payload)
    ElMessage.success('角色已创建')
  }
  roleDialogVisible.value = false
  resetRoleForm()
  await loadDashboard()
}

function resetPermissionForm(type: 'MENU' | 'BUTTON' | 'API') {
  editingPermissionId.value = null
  permissionDialogMode.value = type
  permissionForm.permCode = ''
  permissionForm.permName = ''
  permissionForm.permType = type
  permissionForm.parentId = null
  permissionForm.path = ''
  permissionForm.method = ''
  permissionForm.status = 1
}

function openCreatePermission(type: 'MENU' | 'BUTTON' | 'API') {
  resetPermissionForm(type)
  permissionDialogVisible.value = true
}

function openEditPermission(permission: any) {
  editingPermissionId.value = permission.id
  permissionDialogMode.value = permission.permType
  permissionForm.permCode = permission.permCode
  permissionForm.permName = permission.permName
  permissionForm.permType = permission.permType
  permissionForm.parentId = permission.parentId ?? null
  permissionForm.path = permission.path || ''
  permissionForm.method = permission.method || ''
  permissionForm.status = permission.status
  permissionDialogVisible.value = true
}

async function submitPermission() {
  const payload = {
    ...permissionForm,
    parentId: permissionForm.parentId || null,
  }
  if (editingPermissionId.value) {
    await updateRbacPermission(editingPermissionId.value, payload)
    ElMessage.success('权限已更新')
  } else {
    await createRbacPermission(payload)
    ElMessage.success('权限已创建')
  }
  permissionDialogVisible.value = false
  await loadDashboard()
}

onMounted(async () => {
  await loadDashboard()
})
</script>

<template>
  <div>
      <header class="page-header">
        <div>
          <h1>权限管理</h1>
          <p>提供用户、角色、权限和菜单的基础 RBAC 管理能力</p>
        </div>
      </header>

      <el-card class="panel-card" shadow="never" v-loading="loading">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="用户管理" name="users">
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
                  <el-button @click="loadUsers">搜索</el-button>
                </template>
              </el-input>
              <el-button type="primary" @click="openCreateUser">新建用户</el-button>
            </div>
            <el-table :data="users" stripe>
              <el-table-column prop="username" label="用户名" min-width="120" />
              <el-table-column prop="nickname" label="昵称" min-width="120" />
              <el-table-column prop="roleNames" label="角色" min-width="180" />
              <el-table-column label="类型" min-width="100">
                <template #default="{ row }">
                  <el-tag effect="plain">{{ row.userTypeText || row.userType }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" min-width="100">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.statusText)">{{ row.statusText || row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="180">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openEditUser(row)">编辑</el-button>
                  <el-button link type="warning" @click="resetPassword(row.id)">重置密码</el-button>
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
              @size-change="onUsersSizeChange"
              style="margin-top: 16px; justify-content: flex-end"
            />
          </el-tab-pane>

          <el-tab-pane label="角色管理" name="roles">
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
                  <el-button @click="loadRoles">搜索</el-button>
                </template>
              </el-input>
              <el-button type="primary" @click="openCreateRole">新建角色</el-button>
            </div>
            <el-table :data="roles" stripe>
              <el-table-column prop="roleCode" label="角色编码" min-width="140" />
              <el-table-column prop="roleName" label="角色名称" min-width="140" />
              <el-table-column prop="roleDesc" label="说明" min-width="180" />
              <el-table-column label="状态" min-width="100">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.statusText)">{{ row.statusText || row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="120">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openEditRole(row)">编辑</el-button>
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
              @size-change="onRolesSizeChange"
              style="margin-top: 16px; justify-content: flex-end"
            />
          </el-tab-pane>

          <el-tab-pane label="菜单管理" name="menus">
            <div class="toolbar">
              <el-button type="primary" @click="openCreatePermission('MENU')">新建菜单</el-button>
            </div>
            <el-table :data="menus" stripe>
              <el-table-column prop="permCode" label="菜单编码" min-width="160" />
              <el-table-column prop="permName" label="菜单名称" min-width="140" />
              <el-table-column prop="parentId" label="父级ID" min-width="100" />
              <el-table-column prop="path" label="路径" min-width="180" />
              <el-table-column label="操作" min-width="120">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openEditPermission(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="权限管理" name="permissions">
            <div class="toolbar">
              <el-button @click="openCreatePermission('BUTTON')">新建按钮权限</el-button>
              <el-button @click="openCreatePermission('API')">新建接口权限</el-button>
            </div>
            <el-table :data="actionPermissions" stripe>
              <el-table-column prop="permCode" label="权限编码" min-width="180" />
              <el-table-column prop="permName" label="权限名称" min-width="140" />
              <el-table-column prop="permType" label="类型" min-width="100" />
              <el-table-column prop="path" label="路径" min-width="200" />
              <el-table-column prop="method" label="方法" min-width="100" />
              <el-table-column label="操作" min-width="120">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openEditPermission(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>

    <el-dialog v-model="userDialogVisible" :title="editingUserId ? '编辑用户' : '新建用户'" width="560px" destroy-on-close>
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
              <el-option v-for="role in allRoles" :key="role.id" :label="role.roleName" :value="role.id" />
            </el-select>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="userForm.password" type="password" show-password placeholder="新建用户建议填写，编辑时留空表示不修改" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" :title="editingRoleId ? '编辑角色' : '新建角色'" width="620px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="角色编码">
          <el-input v-model.trim="roleForm.roleCode" :disabled="Boolean(editingRoleId)" placeholder="请输入小写角色编码，例如：admin_ops" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="roleForm.roleName" />
        </el-form-item>
        <el-form-item label="角色说明">
          <el-input v-model="roleForm.roleDesc" />
        </el-form-item>
        <el-form-item label="角色权限">
          <el-select v-model="roleForm.permissionIds" multiple>
            <el-option v-for="permission in permissions" :key="permission.id" :label="`${permission.permName} (${permission.permType})`" :value="permission.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionDialogVisible" :title="editingPermissionId ? '编辑权限/菜单' : '新建权限/菜单'" width="560px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="编码">
          <el-input v-model="permissionForm.permCode" :disabled="Boolean(editingPermissionId)" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="permissionForm.permName" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="permissionForm.permType">
            <el-option label="菜单" value="MENU" />
            <el-option label="按钮" value="BUTTON" />
            <el-option label="接口" value="API" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级菜单">
          <el-select v-model="permissionForm.parentId" clearable>
            <el-option label="无" :value="null" />
            <el-option v-for="menu in menus" :key="menu.id" :label="menu.permName" :value="menu.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="permissionForm.path" />
        </el-form-item>
        <el-form-item label="请求方法">
          <el-input v-model="permissionForm.method" placeholder="GET/POST/PUT/DELETE，菜单可留空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPermission">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header h1 {
  margin: 0 0 8px;
  font-size: 28px;
}

.page-header p {
  margin: 0 0 20px;
  color: #6b7280;
}

.panel-card {
  border-radius: 16px;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
</style>
