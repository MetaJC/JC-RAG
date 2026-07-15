<script setup lang="ts">
/**
 * 用户管理 (RBAC 拆 4 页面之一)
 * <p>原 RbacManageView 「用户管理」 tab 独立化, 独立 URL / 独立 perm_code
 *
 * @author JC
 */
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import {
  ArrowLeft,
  ArrowRight,
  Connection,
  Delete,
  Document,
  Edit,
  Expand,
  Fold,
  FolderOpened,
  Key,
  OfficeBuilding,
  Plus,
  Search,
  UserFilled,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { useAuthStore } from '../stores/auth'

import {
  assignRbacUserRoles,
  createRbacUser,
  deleteRbacUser,
  fetchRbacDashboard,
  fetchRbacDepartmentTree,
  fetchRbacGenderOptions,
  fetchRbacPositions,
  fetchRbacRoles,
  fetchRbacUsers,
  resetRbacUserPassword,
  updateRbacUser,
  updateRbacUserStatus,
} from '../api/rbac'

/* =====================================================================
 * 部门树 (左侧筛选, 含公司根节点)
 * <p>- 0 / null = 全公司, 不按部门过滤
 * <p>- 非 0 = 选中该部门, 后端按部门及所有后代过滤
 * ===================================================================== */
type DeptNode = {
  id: number
  parentId: number
  name: string
  children?: DeptNode[]
}
const deptTree = ref<DeptNode[]>([])
const selectedDeptId = ref<number | null>(null)
const deptTreeRef = ref<InstanceType<typeof import('element-plus').ElTree> | null>(null)
const deptTreeKeyword = ref('')
const deptTreeProps = { value: 'id', label: 'name', children: 'children' }

/**
 * 部门树节点过滤 (el-tree filter-node-method).
 * <p>命中或命中节点的后代都保留, 便于搜索时看到完整组织.
 */
function filterDeptNode(kw: string, data: DeptNode): boolean {
  if (!kw) return true
  return (data.name ?? '').toLowerCase().includes(kw.toLowerCase())
}

watch(deptTreeKeyword, (val) => {
  deptTreeRef.value?.filter(val)
  deptOptionTreeRef.value?.filter(val)
})

/** 选中部门回显文字 (含根节点名称) */
const selectedDeptName = computed(() => {
  if (selectedDeptId.value == null) return ''
  const findName = (ns: DeptNode[]): string | null => {
    for (const n of ns) {
      if (n.id === selectedDeptId.value) return n.name
      if (n.children?.length) {
        const r = findName(n.children)
        if (r) return r
      }
    }
    return null
  }
  return findName(deptTree.value) ?? ''
})

async function loadDeptTree() {
  try {
    const data = (await fetchRbacDepartmentTree()) ?? []
    deptTree.value = data as DeptNode[]
    // 首次挂载自动展开全部节点, 便于一眼看到组织结构
    await nextTick()
    if (deptTreeRef.value) {
      const nodes = (deptTreeRef.value as any).store?.nodesMap
      if (nodes) {
        Object.values(nodes).forEach((n: any) => n.expand())
      }
    }
  } catch {
    deptTree.value = []
  }
}

function onDeptNodeClick(node: DeptNode) {
  // 点根节点 = 全公司; 点其他 = 选中该部门
  selectedDeptId.value = node.id
  usersPage.value = 1
  loadUsers()
}

function clearDeptFilter() {
  selectedDeptId.value = null
  usersPage.value = 1
  loadUsers()
}

const loading = ref(false)
const users = ref<any[]>([])
const usersTotal = ref(0)
const usersPage = ref(1)
const usersSize = ref(10)
const usersKeyword = ref('')

const allRoles = ref<any[]>([])
/** 岗位下拉选项 (含启用 + 禁用, 用于用户弹窗的岗位多选) */
const allPositions = ref<any[]>([])

const userDialogVisible = ref(false)
const editingUserId = ref<number | null>(null)
const editingUserIsDeveloper = ref(false)

const userForm = reactive({
  username: '',
  nickname: '',
  realName: '',
  phone: '',
  email: '',
  userType: 'NORMAL',
  status: 1,
  password: '',
  departmentId: null as number | null,
  roleIds: [] as number[],
  positionIds: [] as number[],
  gender: 0 as number,
})

/** 性别字典 (0=未知 1=男 2=女) */
const genderOptions = ref<Array<{ value: number; label: string }>>([])

/** 分配角色对话框 */
const assignRoleDialogVisible = ref(false)
const assignRoleUserId = ref<number | null>(null)
const assignRoleUserName = ref('')
const assignRoleFormRoleIds = ref<number[]>([])
const assignRoleSaving = ref(false)

/** 批量选择 */
const selectedUserIds = ref<number[]>([])
const batchDeleting = ref(false)

/**
 * 左侧"组织架构"区折叠状态 (默认展开).
 * <p>折叠行为参考左侧导航菜单: 整个 aside 向左收起并淡出, 同时右侧出现一个浮动"展开"按钮.
 * <p>展开 / 折叠都带平滑过渡 (width + opacity + transform).
 * <p>持久化到 localStorage: rbac.user.deptPanelCollapsed.
 */
const deptPanelCollapsed = ref<boolean>(localStorage.getItem('rbac.user.deptPanelCollapsed') === '1')

function toggleDeptPanel() {
  deptPanelCollapsed.value = !deptPanelCollapsed.value
  localStorage.setItem('rbac.user.deptPanelCollapsed', deptPanelCollapsed.value ? '1' : '0')
}

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
    deptId: selectedDeptId.value ?? undefined,
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
    // 岗位下拉数据 (用于用户弹窗的岗位多选, 包含启用 + 禁用, 避免已分配但已禁用的岗位 ID 找不到 option label)
    try {
      const data = await fetchRbacPositions({ page: 1, size: 9999 })
      allPositions.value = data.records ?? []
    } catch {
      // 静默失败, 不影响列表展示
    }
    // 部门树单独加载, 不与用户列表互相阻塞
    loadDeptTree()
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
  userForm.realName = ''
  userForm.phone = ''
  userForm.email = ''
  userForm.userType = 'NORMAL'
  userForm.status = 1
  userForm.password = ''
  userForm.departmentId = null
  userForm.roleIds = []
  userForm.gender = 0
  // 同步 popover 输入框
  deptTreeCheckedId.value = null
  deptInputDisplay.value = ''
}

async function openCreateUser() {
  resetUserForm()
  await ensureDeptTreeLoaded()
  await ensurePositionsLoaded()
  userDialogVisible.value = true
}

async function openEditUser(user: any) {
  if (isLockedRow(user)) {
    ElMessage.warning('该用户 (developer / super_admin) 是系统保留, 不允许编辑')
    return
  }
  await ensureDeptTreeLoaded()
  await ensurePositionsLoaded()
  editingUserId.value = user.id
  editingUserIsDeveloper.value = user.username === 'developer' || user.userType === 'DEVELOPER'
  userForm.username = user.username
  userForm.nickname = user.nickname
  userForm.realName = user.realName ?? ''
  userForm.phone = user.phone || ''
  userForm.email = user.email || ''
  userForm.userType = user.userType
  userForm.status = user.status
  userForm.password = ''
  userForm.departmentId = user.departmentId ?? null
  userForm.roleIds = [...(user.roleIds || [])]
  userForm.positionIds = [...(user.positionIds || [])]
  userForm.gender = user.gender ?? 0
  // 同步 popover 输入框 + 选中状态
  deptTreeCheckedId.value = userForm.departmentId
  syncDeptDisplay()
  userDialogVisible.value = true
}

/**
 * 确保部门树已加载, 否则在打开弹窗前主动加载, 避免 el-tree-select 出现空数据.
 * <p>幂等: deptTree 已有数据则直接返回.
 */
async function ensureDeptTreeLoaded() {
  if (deptTree.value && deptTree.value.length > 0) return
  await loadDeptTree()
}

/**
 * 确保岗位下拉数据已加载, 避免 el-select v-model 中包含已分配但下拉里没有的岗位 ID 时抛错.
 * <p>幂等: allPositions 已有数据则直接返回.
 * <p>用分页接口 (含启用+禁用), 保证已分配但已禁用的岗位也能在下拉里找到 label.
 */
async function ensurePositionsLoaded() {
  if (allPositions.value && allPositions.value.length > 0) return
  try {
    const data = await fetchRbacPositions({ page: 1, size: 9999 })
    allPositions.value = data.records ?? []
  } catch {
    // 静默失败: 不阻断弹窗打开
  }
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

/* =====================================================================
 * 所属部门下拉框 (popover + tree) — 与部门管理页面的父级部门选择一致
 * <p>单选: 选中的部门 id 写入 userForm.departmentId; 支持搜索 + 双击/确认选择.
 * ===================================================================== */
const deptPopoverVisible = ref(false)
const deptOptionTreeRef = ref<InstanceType<typeof import('element-plus').ElTree> | null>(null)
const deptTreeCheckedId = ref<number | null>(null)
const deptInputDisplay = ref('')

/** 实时同步输入框显示 (部门名) */
function syncDeptDisplay() {
  if (userForm.departmentId == null) {
    deptInputDisplay.value = ''
    return
  }
  const findName = (ns: DeptNode[]): string | null => {
    for (const n of ns) {
      if (n.id === userForm.departmentId) return n.name
      if (n.children?.length) {
        const r = findName(n.children)
        if (r) return r
      }
    }
    return null
  }
  deptInputDisplay.value = findName(deptTree.value) ?? ''
}

function onDeptPopoverShow() {
  deptTreeCheckedId.value = userForm.departmentId ?? null
  nextTick(() => {
    if (deptTreeCheckedId.value != null) {
      deptOptionTreeRef.value?.setCurrentKey(deptTreeCheckedId.value as any)
    }
  })
}

function onDeptPopoverHide() {
  deptTreeKeyword.value = ''
}

function onDeptOptionNodeClick(node: DeptNode) {
  deptTreeCheckedId.value = node.id
}

function onDeptOptionNodeDblClick(node: DeptNode) {
  userForm.departmentId = node.id
  deptPopoverVisible.value = false
  syncDeptDisplay()
}

function confirmDept() {
  if (deptTreeCheckedId.value == null) {
    ElMessage.warning('请先在树上选择部门')
    return
  }
  userForm.departmentId = deptTreeCheckedId.value
  deptPopoverVisible.value = false
  syncDeptDisplay()
}

function onClearDept() {
  userForm.departmentId = null
  deptTreeCheckedId.value = null
  deptInputDisplay.value = ''
}

function expandAllDeptOptions() {
  deptOptionTreeRef.value?.store?.nodesMap &&
    Object.values(deptOptionTreeRef.value.store.nodesMap).forEach((n: any) => n.expand())
}

function collapseAllDeptOptions() {
  deptOptionTreeRef.value?.store?.nodesMap &&
    Object.values(deptOptionTreeRef.value.store.nodesMap).forEach((n: any) => n.collapse())
}

/** 弹窗打开后, 同步选中 + 文本回显 */
watch(deptPopoverVisible, async (val) => {
  if (val) {
    await nextTick()
    syncDeptDisplay()
  }
})

onMounted(() => {
  // 性别字典: 编辑用户时可选; 失败不影响主流程
  fetchRbacGenderOptions()
    .then((opts) => (genderOptions.value = opts))
    .catch(() => {})
  loadDashboard()
})
</script>

<template>
  <div>
    <header class="page-header">
      <div>
        <h1>用户管理</h1>
        <p>按左侧组织架构筛选, 默认显示全公司用户</p>
      </div>
    </header>

    <el-card class="panel-card" shadow="never" v-loading="loading">
      <div class="user-layout">
        <!--
          左侧: 部门树 (含公司根节点).
          点根节点 = 全公司 (不按部门过滤); 点子部门 = 后端按部门及后代递归过滤.
          折叠行为: 整个 aside 向左收起 (width → 0, opacity → 0, 略向左滑),
          折叠后原位置出现一个浮动"展开组织架构"按钮, 像左侧导航菜单一样.
        -->
        <aside class="dept-tree-panel" :class="{ 'is-collapsed': deptPanelCollapsed }">
          <div class="dept-tree-header">
            <el-icon v-if="!deptPanelCollapsed" :size="16" color="#409eff"><OfficeBuilding /></el-icon>
            <span v-if="!deptPanelCollapsed">组织架构</span>
            <!-- 折叠瞬间的过渡占位, 让 panel 在收起过程中仍有标题 (会被 overflow:hidden 裁掉) -->
            <span v-else class="dept-tree-title-vertical">组织架构</span>
            <div class="dept-tree-header__actions">
              <el-button
                v-if="!deptPanelCollapsed && selectedDeptId != null"
                link
                type="primary"
                size="small"
                @click="clearDeptFilter"
              >清除筛选</el-button>
              <el-tooltip :content="deptPanelCollapsed ? '展开组织架构' : '折叠组织架构'" placement="right">
                <el-button
                  link
                  type="primary"
                  :icon="deptPanelCollapsed ? Expand : Fold"
                  @click="toggleDeptPanel"
                />
              </el-tooltip>
            </div>
          </div>
          <div v-show="!deptPanelCollapsed" class="dept-tree-body">
            <el-input
              v-model="deptTreeKeyword"
              placeholder="搜索部门"
              :prefix-icon="Search"
              clearable
              size="small"
              class="dept-tree-search"
            />
            <el-tree
              ref="deptTreeRef"
              class="dept-tree"
              :data="deptTree"
              :props="deptTreeProps"
              node-key="id"
              :filter-node-method="filterDeptNode"
              :default-expand-all="true"
              :expand-on-click-node="false"
              :highlight-current="true"
              :current-node-key="selectedDeptId ?? undefined"
              @node-click="onDeptNodeClick"
            >
              <template #default="{ node, data }">
                <span class="dept-tree-node-label">
                  <el-icon :size="14" color="#909399">
                    <OfficeBuilding v-if="data.parentId === 0" />
                    <UserFilled v-else />
                  </el-icon>
                  <span :class="{ 'dept-tree-node-root': data.parentId === 0 }">
                    {{ node.label }}
                  </span>
                </span>
              </template>
            </el-tree>
          </div>
        </aside>

        <!--
          永久展开 / 折叠开关: 始终位于原 panel 与右侧用户列表的"夹缝"中, 垂直居中.
          <p>箭头方向随状态切换:
          <p>- 展开态 → ArrowLeft (指向左, 提示"点击可向左折叠")
          <p>- 折叠态 → ArrowRight (指向右, 提示"点击可向右展开")
          <p>宽度窄 (~18px), 透明背景, 不挡内容; hover 时浅蓝高亮.
          <p>不再用 v-if 隐藏, 让用户随时可见操作入口.
        -->
        <button
          class="dept-panel-handle"
          type="button"
          :aria-label="deptPanelCollapsed ? '展开组织架构' : '折叠组织架构'"
          :title="deptPanelCollapsed ? '展开组织架构' : '折叠组织架构'"
          @click="toggleDeptPanel"
        >
          <el-icon :size="13">
            <ArrowRight v-if="deptPanelCollapsed" />
            <ArrowLeft v-else />
          </el-icon>
        </button>

        <!-- 右侧: 用户列表 -->
        <main class="user-table-panel">
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
            <el-tag
              v-if="selectedDeptId != null"
              type="info"
              effect="plain"
              closable
              class="dept-filter-chip"
              @close="clearDeptFilter"
            >
              已筛选: {{ selectedDeptName }}
            </el-tag>
            <el-tag v-else type="warning" effect="plain" class="dept-filter-chip">
              全公司
            </el-tag>
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
            <el-table-column prop="realName" label="真实姓名" min-width="120" show-overflow-tooltip />
            <el-table-column label="性别" min-width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.gender === 1" size="small" type="primary" effect="plain">男</el-tag>
                <el-tag v-else-if="row.gender === 2" size="small" type="danger" effect="plain">女</el-tag>
                <span v-else class="text-muted">未知</span>
              </template>
            </el-table-column>
            <el-table-column label="部门" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="row.departmentName">{{ row.departmentName }}</span>
                <span v-else class="text-muted">未分配</span>
              </template>
            </el-table-column>
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
            <el-table-column label="岗位" min-width="160">
              <template #default="{ row }">
                <template v-if="row.positionNames && row.positionNames.length">
                  <el-tag
                    v-for="(name, idx) in row.positionNames"
                    :key="idx"
                    size="small"
                    type="info"
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
            <!--
              操作列: 4 个 icon 按钮 (编辑 / 分配角色 / 重置密码 / 删除) 各 ~32px
              <p>固定 width=180, 刚好放下 4 个 icon 按钮 + 表格内边距, 不留多余空白
            -->
            <el-table-column label="操作" width="180" fixed="right">
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
        </main>
      </div>
    </el-card>

    <el-dialog
      v-model="userDialogVisible"
      :title="editingUserId ? '编辑用户' : '新建用户'"
      width="780px"
      destroy-on-close
    >
      <!--
        左右布局, 创新结构:
        <p>label 框 = 14px 星号位 (单独一列) + 6px 间距 + label 文字 (右对齐)
        <p>- 星号位: required 时显示红 *, 否则留空 (占位 14px), 保证所有 label 文字对齐
        <p>- label 文字: text-align:right, 紧贴 input 左边缘
        <p>- 所有列内 label 文字右边缘 X 坐标一致, input 起点 X 坐标一致
      -->
      <el-form label-position="right" label-width="80px" class="user-dialog-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="userForm.username" :disabled="Boolean(editingUserId)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称">
              <el-input v-model="userForm.nickname" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="真实姓名" required>
              <el-input
                v-model="userForm.realName"
                maxlength="50"
                show-word-limit
                placeholder="请输入用户真实姓名"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="userForm.gender" placeholder="选择性别" clearable style="width: 100%">
                <el-option v-for="opt in genderOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="userForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="userForm.email" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户类型">
              <el-select v-model="userForm.userType" :disabled="editingUserIsDeveloper" style="width: 100%">
                <el-option v-if="editingUserIsDeveloper" label="开发者账号" value="DEVELOPER" />
                <el-option label="管理员" value="ADMIN" />
                <el-option label="普通用户" value="NORMAL" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="userForm.roleIds" multiple :disabled="editingUserIsDeveloper" style="width: 100%">
                <el-option
                  v-for="role in allRoles"
                  :key="role.id"
                  :label="role.roleName"
                  :value="role.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <!--
          部门 / 岗位 同一行两列展示.
          <p>左: 所属部门 (popover + tree, 单选)
          <p>右: 岗位 (多选, 含启用 + 禁用, 避免已分配但已禁用的岗位 ID 找不到 label)
        -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属部门">
              <!--
                部门下拉框: popover + tree, 与部门管理页面编辑弹窗的父级部门下拉框一致.
                <p>单选; 选中的 id 写入 userForm.departmentId.
              -->
              <el-popover
                :visible="deptPopoverVisible"
                placement="bottom-start"
                :width="380"
                trigger="manual"
                :teleported="true"
                popper-class="jc-parent-tree-popover"
                @show="onDeptPopoverShow"
                @hide="onDeptPopoverHide"
              >
                <template #reference>
                  <el-input
                    :model-value="deptInputDisplay"
                    placeholder="点击选择所属部门 (可留空)"
                    readonly
                    clearable
                    @click="deptPopoverVisible = !deptPopoverVisible"
                    @clear="onClearDept"
                  >
                    <template #prefix>
                      <el-icon><Connection /></el-icon>
                    </template>
                  </el-input>
                </template>

                <div class="jc-parent-tree-panel">
                  <div class="jc-parent-tree-toolbar">
                    <el-input
                      v-model="deptTreeKeyword"
                      placeholder="搜索部门名称"
                      :prefix-icon="Search"
                      clearable
                      size="small"
                    />
                    <el-button-group size="small">
                      <el-button :icon="Expand" @click="expandAllDeptOptions" />
                      <el-button :icon="Fold" @click="collapseAllDeptOptions" />
                    </el-button-group>
                  </div>
                  <div class="jc-parent-tree-body">
                    <el-tree
                      ref="deptOptionTreeRef"
                      :data="deptTree"
                      :props="deptTreeProps"
                      node-key="id"
                      :filter-node-method="filterDeptNode"
                      :expand-on-click-node="false"
                      :default-expand-all="true"
                      highlight-current
                      @node-click="onDeptOptionNodeClick"
                      @node-dblclick="onDeptOptionNodeDblClick"
                    >
                      <template #default="{ node, data }">
                        <span class="jc-parent-tree-node">
                          <el-icon :size="14" class="jc-parent-tree-node-icon">
                            <FolderOpened v-if="data.children?.length" />
                            <Document v-else />
                          </el-icon>
                          <span class="jc-parent-tree-node-label">{{ node.label }}</span>
                        </span>
                      </template>
                    </el-tree>
                  </div>
                  <div class="jc-parent-tree-footer">
                    <span class="jc-parent-tree-hint">双击节点快速选择 / 点击确认按钮选择</span>
                    <div>
                      <el-button size="small" @click="deptPopoverVisible = false">取消</el-button>
                      <el-button size="small" type="primary" @click="confirmDept">确认选择</el-button>
                    </div>
                  </div>
                </div>
              </el-popover>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-select
                v-model="userForm.positionIds"
                multiple
                clearable
                filterable
                placeholder="可多选 (不分配岗位留空即可)"
                style="width: 100%"
              >
                <el-option
                  v-for="pos in allPositions"
                  :key="pos.id"
                  :label="pos.name"
                  :value="pos.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="密码">
              <el-input
                v-model="userForm.password"
                type="password"
                show-password
                placeholder="新建用户建议填写，编辑时留空表示不修改"
              />
            </el-form-item>
          </el-col>
        </el-row>
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

/* =====================================================================
 * 左树右表布局
 * <p>左侧: 部门树 (含公司根节点), 右侧: 用户列表
 * <p>左侧面板参考左侧导航菜单: 可向左整体收起, 带平滑过渡动画
 * ===================================================================== */
.user-layout {
  display: flex;
  gap: 0;
  min-height: 600px;
  align-items: stretch;
  /* 让绝对定位的 .dept-panel-handle 参照此容器 (而不是 body / 最近的定位祖先) */
  position: relative;
}
.dept-tree-panel {
  width: 260px;
  flex-shrink: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fafbfc;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* 主过渡: width / opacity / transform 一起动, 像 nav menu 一样 */
  transition:
    width 0.32s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.28s ease,
    transform 0.32s cubic-bezier(0.4, 0, 0.2, 1),
    border-color 0.28s ease;
  opacity: 1;
  transform: translateX(0);
}
.dept-tree-panel.is-collapsed {
  width: 0;
  border-color: transparent;
  opacity: 0;
  /* 折叠瞬间再向左滑 12px, 形成"向左收起"的方向感 */
  transform: translateX(-12px);
  pointer-events: none;
}
.dept-tree-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: #fff;
  font-weight: 600;
  font-size: 13px;
  color: #1f2937;
  flex-shrink: 0;
}
.dept-tree-header > span:not(.dept-tree-header__actions) {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dept-tree-header__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
/* 折叠瞬间占位: 极窄宽度下用竖排标题 (会随 panel 一起被裁掉) */
.dept-tree-title-vertical {
  writing-mode: vertical-rl;
  letter-spacing: 4px;
  color: #409eff;
  font-size: 12px;
  font-weight: 500;
  text-align: center;
}
.dept-tree-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.dept-tree-search {
  margin: 8px 12px 4px;
  flex-shrink: 0;
}
.dept-tree {
  flex: 1;
  overflow: auto;
  padding: 4px 6px 8px;
  background: #fff;
}
.dept-tree-node-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  width: 100%;
}
.dept-tree-node-root {
  font-weight: 600;
  color: #409eff;
  letter-spacing: 0.3px;
}

/* 永久展开 / 折叠开关: 始终存在的窄小箭头按钮, 嵌在 panel 与用户列表的"夹缝"中.
 * <p>展开态: 箭头向左 (提示可折叠); 折叠态: 箭头向右 (提示可展开).
 * <p>作为 flex item 处于布局流中, 跟随 panel 的 width 变化自动左右挪位.
 * <p>垂直居中, 宽度窄 (~18px), 透明背景, 不挡内容.
 */
.dept-panel-handle {
  flex-shrink: 0;
  align-self: center;
  width: 18px;
  height: 48px;
  margin: 0 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 4px;
  cursor: pointer;
  color: #909399;
  font-size: 0;
  line-height: 1;
  transition: all 0.18s ease;
  user-select: none;
}
.dept-panel-handle:hover {
  background: #ecf5ff;
  color: #409eff;
  border-color: #d6e8ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}
.dept-panel-handle:active {
  background: #d6e8ff;
  color: #409eff;
}

.user-table-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.dept-filter-chip {
  margin-right: 4px;
  font-size: 12px;
}

/* =====================================================================
 * 用户弹窗 (新建/编辑) form 布局
 * <p>label 框 = 14px 星号位 (单独一列) + 6px 间距 + label 文字 (右对齐)
 * <p>- 星号位: required 时显示红 *, 否则留空 (占位 14px), 保证所有 label 文字对齐
 * <p>- label 文字: text-align:right, 紧贴 input 左边缘
 * <p>- 所有列内 label 文字右边缘 X 坐标一致, input 起点 X 坐标一致
 * <p>- 第一列 (奇数 col) label 宽 90px, 兼容 4 字符中文 + 星号位
 * <p>- 第二列 (偶数 col) label 宽 40px, 短 label 不需要那么宽
 * <p>- 沿用 Element Plus 原生 float 布局, 不破坏 el-input / el-select 内部宽度计算
 * ===================================================================== */
.user-dialog-form .el-form-item {
  margin-right: 0 !important;
  margin-bottom: 16px;
}
.user-dialog-form .el-form-item__label {
  display: inline-flex !important;
  align-items: center;
  justify-content: flex-end;
  padding: 0 10px 0 0 !important;
  text-align: right;
  color: #606266;
  vertical-align: middle;
}
/* 第一列 (奇数 col) 沿用 el-form label-width=90px, 不额外覆盖 */
/* 第二列 (偶数 col) label 改 40px, 短 label 不需要那么宽 */
.user-dialog-form .el-row > .el-col:nth-child(even) .el-form-item__label {
  width: 40px !important;
}
.user-dialog-form .el-row > .el-col:nth-child(even) .el-form-item__content {
  margin-left: 40px !important;
}
/* 14px 星号位: required 时显示红 *, 否则留空 (占位, 不挤占 label 文字) */
.user-dialog-form .el-form-item__label::before {
  content: '';
  display: inline-block;
  width: 14px;
  margin-right: 6px;
  text-align: left;
  flex-shrink: 0;
  line-height: 1;
}
.user-dialog-form .el-form-item.is-required .el-form-item__label::before {
  content: '*';
  color: #f56c6c;
  font-size: 14px;
}
.user-dialog-form .el-form-item__content > .el-input,
.user-dialog-form .el-form-item__content > .el-select,
.user-dialog-form .el-form-item__content > .el-popover {
  width: 100%;
}
.user-dialog-form .el-form-item__error {
  padding-top: 2px;
}
</style>

<!-- =====================================================================
 * 部门下拉框 popover 样式 (非 scoped)
 * <p>为什么用非 scoped 块: popper 节点被 teleport 到 body 下, 没有 [data-v-xxx] 属性,
 * scoped 选择器不命中. 与部门管理页面 (RbacDepartmentView.vue) 共用同一套类名.
 * ===================================================================== -->
<style>
.jc-parent-tree-popover,
.jc-parent-tree-popover.el-popper,
.jc-parent-tree-popover.el-popper.is-light,
.jc-parent-tree-popover.el-popper[data-popper-placement] {
  z-index: 9999999 !important;
}

/* =====================================================================
 * 分配角色弹窗内 el-select 下拉 z-index 兜底
 * <p>el-overlay / el-dialog 全局 z-index=99999 (RbacDepartmentView 设的),
 * el-select popper 默认 9999 会被 dialog 遮住, 必须提到 9999999.
 * ===================================================================== */
.el-select__popper.el-popper,
.el-select-dropdown.el-popper {
  z-index: 9999999 !important;
}
</style>
