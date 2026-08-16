<script setup lang="ts">
/**
 * 知识库权限管理页。
 * <p>布局: 左侧 A 卡片(角色 + 选中角色的用户) + 右侧 B 卡片(已勾选用户, 按角色 tab 归类)。
 * <p>支持左右比例拖拽调节 (默认 6:4); 用户列表无分页, 滚动触底动态加载。
 * <p>权限级别: 0 未授权 / 1 可查看 / 2 可编辑 / 3 可管理, 高级别覆盖低级别。
 * <p>联动: 角色与用户双向勾选同步; super_admin 角色锁定为可管理, 仅 developer 可改。
 *
 * @author JC
 */
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Close } from '@element-plus/icons-vue'

import { fetchKbAuthDetail, fetchKnowledgeBases, saveKbAuth } from '../api/knowledge'
import { useAuthStore } from '../stores/auth'
import type { KbAuthDetail, KbAuthUserItem, KbAuthRoleItem } from '../types/knowledge'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const currentUsername = computed(() => authStore.user?.username || '')
const isDeveloper = computed(() => currentUsername.value.toLowerCase() === 'developer')

/** 默认超级管理员角色编码 */
const SUPER_ADMIN_CODE = 'super_admin'
/** 不可选 0, 0 仅作 placeholder; 显示文案 */
const PERM_OPTIONS = [
  { value: 1, label: '可查看' },
  { value: 2, label: '可编辑' },
  { value: 3, label: '可管理' },
]
/** 滚动加载每批大小 */
const SCROLL_BATCH = 30

const loading = ref(false)
const saving = ref(false)
const detail = ref<KbAuthDetail | null>(null)
const selectedRoleId = ref<number | null>(null)
const kbList = ref<{ id: number; kbName: string; visibilityScope: string }[]>([])
const rawKbId = route.query.kbId
const queryKbId = Array.isArray(rawKbId) ? Number(rawKbId[0]) : Number(rawKbId)
const activeKbId = ref<number | null>(Number.isFinite(queryKbId) && queryKbId > 0 ? queryKbId : null)

/** 用户搜索关键字 (A 卡片右侧) */
const userSearch = ref('')
/** B 卡片激活 tab (字符串: '' = 全部用户, 否则为 roleId 字符串) */
const activeTabRoleId = ref<string>('')

/** 角色授权编辑态 */
const rolePermMap = reactive<Record<number, number>>({})
/** 用户授权编辑态 (直接授权) */
const userPermMap = reactive<Record<number, number>>({})
/** 用户所属角色 (与详情一致) */
const userRoleMap = reactive<Record<number, number[]>>({})
/** 角色码映射 */
const roleCodeMap = reactive<Record<number, string>>({})

/** 左右分栏比例, A 卡片占比 (0~1), 默认 0.6 */
const aPaneRatio = ref(0.6)
const SPLITTER_PX = 6
const layoutRef = ref<HTMLElement | null>(null)
const dragging = ref(false)

/** 滚动加载已显示数量 */
const aVisibleCount = ref(SCROLL_BATCH)
const bVisibleCount = ref(SCROLL_BATCH)
const aScrollRef = ref<HTMLElement | null>(null)
const bScrollRef = ref<HTMLElement | null>(null)
const aSentinelRef = ref<HTMLElement | null>(null)
const bSentinelRef = ref<HTMLElement | null>(null)
let aObserver: IntersectionObserver | null = null
let bObserver: IntersectionObserver | null = null

/** 排序后: super_admin 置首, 其他按 id 升序 */
const sortedRoles = computed(() => {
  const list = detail.value?.roleAuths ?? []
  return [...list].sort((a, b) => {
    const aSuper = String(a.roleCode || '').toLowerCase() === SUPER_ADMIN_CODE
    const bSuper = String(b.roleCode || '').toLowerCase() === SUPER_ADMIN_CODE
    if (aSuper && !bSuper) return -1
    if (bSuper && !aSuper) return 1
    return a.roleId - b.roleId
  })
})

/** A 卡片右侧: 当前选中角色的用户 (受搜索过滤) */
const aFilteredUsers = computed<KbAuthUserItem[]>(() => {
  const all = detail.value?.userAuths ?? []
  const roleId = selectedRoleId.value
  if (roleId == null) return []
  const kw = userSearch.value.trim().toLowerCase()
  return all.filter((u) => {
    if (!Array.isArray(u.roleIds) || !u.roleIds.includes(roleId)) return false
    if (!kw) return true
    return (u.username || '').toLowerCase().includes(kw) || (u.nickname || '').toLowerCase().includes(kw)
  })
})

/** A 卡片右侧可见用户 (滚动加载) */
const aVisibleUsers = computed(() => aFilteredUsers.value.slice(0, aVisibleCount.value))

/** B 卡片用户: 已勾选 (effective > 0) */
const grantedUsers = computed<KbAuthUserItem[]>(() => {
  const all = detail.value?.userAuths ?? []
  return all.filter((u) => effectiveUserLevel(u) > 0)
})

/** B 卡片: 当前 tab 下用户 (全部 / 单角色) */
const bFilteredUsers = computed<KbAuthUserItem[]>(() => {
  const all = grantedUsers.value
  const roleId = activeTabRoleId.value ? Number(activeTabRoleId.value) : null
  if (roleId == null) return all
  return all.filter((u) => Array.isArray(u.roleIds) && u.roleIds.includes(roleId))
})

const bVisibleUsers = computed(() => bFilteredUsers.value.slice(0, bVisibleCount.value))

/** B 卡片 tab 列表: 全部 + 有用户的角色 (按角色分组, 不管角色本身是否已授权) */
const bTabs = computed(() => {
  // 收集所有有用户的角色 id
  const roleIdsWithUsers = new Set<number>()
  for (const u of grantedUsers.value) {
    if (Array.isArray(u.roleIds)) {
      for (const rid of u.roleIds) roleIdsWithUsers.add(rid)
    }
  }
  const list = sortedRoles.value.filter((r) => roleIdsWithUsers.has(r.roleId))
  return [
    { roleId: '', roleName: '全部用户' },
    ...list.map((r) => ({ roleId: String(r.roleId), roleName: r.roleName })),
  ]
})

const activeKbName = computed(() => {
  if (!activeKbId.value) return ''
  const found = kbList.value.find((k) => k.id === activeKbId.value)
  return found?.kbName || detail.value?.kbName || ''
})

const activeKbScope = computed(() => {
  if (!activeKbId.value) return ''
  const found = kbList.value.find((k) => k.id === activeKbId.value)
  return found?.visibilityScope || detail.value?.visibilityScope || ''
})

function visibilityLabel(scope?: string): string {
  if (scope === 'PUBLIC') return '公开'
  if (scope === 'INTERNAL' || scope === 'ROLE') return '内部'
  return '私有'
}

function permLabel(level: number): string {
  if (level >= 3) return '可管理'
  if (level === 2) return '可编辑'
  if (level === 1) return '可查看'
  return '未授权'
}

/** 角色是否超级管理员 (锁定为可管理) */
function isSuperAdminRole(role: { roleCode?: string }): boolean {
  return String(role.roleCode || '').toLowerCase() === SUPER_ADMIN_CODE
}

/** 用户是否属于 super_admin 角色 */
function isSuperAdminUser(user: KbAuthUserItem): boolean {
  const rids = userRoleMap[user.userId] ?? user.roleIds ?? []
  return rids.some((rid) => String(roleCodeMap[rid] || '').toLowerCase() === SUPER_ADMIN_CODE)
}

/** 用户直接授权级别 (0 表示未直接授权) */
function userDirectLevel(user: KbAuthUserItem): number {
  return userPermMap[user.userId] ?? 0
}

/** 当前用户有效权限 = max(用户直接授权, 用户所有角色的最大授权) */
function effectiveUserLevel(user: KbAuthUserItem): number {
  const direct = userPermMap[user.userId] ?? 0
  const roleIds = userRoleMap[user.userId] ?? user.roleIds ?? []
  let maxRole = 0
  for (const rid of roleIds) {
    if ((rolePermMap[rid] ?? 0) > maxRole) maxRole = rolePermMap[rid]!
  }
  return Math.max(direct, maxRole)
}

function isUserChecked(user: KbAuthUserItem): boolean {
  return effectiveUserLevel(user) > 0
}

/** 复选框直接授权态: 反映 userPermMap, 不被角色反向牵动 */
function isUserDirectChecked(user: KbAuthUserItem): boolean {
  return userDirectLevel(user) > 0
}

/** 角色下用户授权统计 (用于角色复选框三态) — 基于用户复选框直选态 (userPermMap) */
function roleUserAuthStats(roleId: number): { authorized: number; total: number } {
  const users = (detail.value?.userAuths ?? []).filter((u) => {
    const rids = userRoleMap[u.userId] ?? u.roleIds ?? []
    return rids.includes(roleId)
  })
  if (!users.length) return { authorized: 0, total: 0 }
  const authorized = users.filter((u) => userDirectLevel(u) > 0).length
  return { authorized, total: users.length }
}

/** 角色复选框 checked: 该角色下用户全部已授权 */
function isRoleChecked(roleId: number): boolean {
  const { authorized, total } = roleUserAuthStats(roleId)
  return total > 0 && authorized === total
}

/** 角色复选框 indeterminate: 该角色下用户部分已授权 */
function isRoleIndeterminate(roleId: number): boolean {
  const { authorized, total } = roleUserAuthStats(roleId)
  return authorized > 0 && authorized < total
}

/** 角色基础级别: 优先取用户编辑值 (rolePermMap), 否则取后端原始值 (role.permLevel) */
function baseRoleLevel(role: KbAuthRoleItem): number {
  if (isSuperAdminRole(role) && !isDeveloper.value) {
    const edited = rolePermMap[role.roleId]
    if (edited != null) return edited
    return role.permLevel == null ? 3 : role.permLevel
  }
  return rolePermMap[role.roleId] ?? role.permLevel ?? 0
}

/** 用户基础级别: 优先取用户编辑值 (userPermMap), 否则取后端原始值 (user.permLevel) */
function baseUserLevel(user: KbAuthUserItem): number {
  return userPermMap[user.userId] ?? user.permLevel ?? 0
}

/** 角色展示用级别: max(角色自己的, 该角色下所有用户的基础级别) */
function displayRolePerm(role: KbAuthRoleItem): number {
  const roleOwnLevel = baseRoleLevel(role)
  const usersInRole = (detail.value?.userAuths ?? []).filter(
      (u) => Array.isArray(u.roleIds) && u.roleIds.includes(role.roleId),
  )
  const maxUserLevel = usersInRole.reduce(
      (max, u) => Math.max(max, baseUserLevel(u)),
      0,
  )
  return Math.max(roleOwnLevel, maxUserLevel)
}

/** 用户展示用级别: max(用户自己的, 其所有角色的展示级别) */
function displayUserPerm(user: KbAuthUserItem): number {
  const direct = baseUserLevel(user)
  const rids = Array.isArray(user.roleIds) ? user.roleIds : []
  let maxRole = 0
  for (const rid of rids) {
    const role = (detail.value?.roleAuths ?? []).find((r) => r.roleId === rid)
    if (!role) continue
    const rl = displayRolePerm(role)
    if (rl > maxRole) maxRole = rl
  }
  return Math.max(direct, maxRole)
}

function userDisplayName(user: KbAuthUserItem): string {
  return user.nickname || user.username
}

/** 用户当前显示权限 (下拉) */
function currentUserPerm(user: KbAuthUserItem): number {
  const direct = userPermMap[user.userId] ?? 0
  if (direct > 0) return direct
  const roleIds = userRoleMap[user.userId] ?? user.roleIds ?? []
  let maxRole = 0
  for (const rid of roleIds) {
    if ((rolePermMap[rid] ?? 0) > maxRole) maxRole = rolePermMap[rid]!
  }
  return maxRole
}

async function loadKbList() {
  try {
    const data: any = await fetchKnowledgeBases({ page: 1, size: 1000 })
    kbList.value = (data.records ?? data ?? []).map((it: any) => ({
      id: it.id,
      kbName: it.kbName,
      visibilityScope: it.visibilityScope,
    }))
  } catch (error) {
    console.warn('加载知识库列表失败', error)
  }
}

async function loadAuthDetail(kbId: number) {
  loading.value = true
  try {
    const data = await fetchKbAuthDetail(kbId)
    console.log('[KB-AUTH] detail loaded', {
      kbId,
      isDeveloper: isDeveloper.value,
      roles: (data?.roleAuths ?? []).map((r) => ({
        roleId: r.roleId, roleCode: r.roleCode, roleName: r.roleName,
        permLevel: r.permLevel, permLevelType: typeof r.permLevel,
      })),
    })
    detail.value = data
    syncStateFromDetail(data)
  } catch (error) {
    console.error(error)
    ElMessage.error('加载授权详情失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

function syncStateFromDetail(data: KbAuthDetail) {
  for (const k of Object.keys(rolePermMap)) delete rolePermMap[Number(k)]
  for (const k of Object.keys(userPermMap)) delete userPermMap[Number(k)]
  for (const k of Object.keys(userRoleMap)) delete userRoleMap[Number(k)]
  for (const k of Object.keys(roleCodeMap)) delete roleCodeMap[Number(k)]
  for (const r of data.roleAuths ?? []) {
    // 后端 null = 未保存过; 非 null = 实际保存的级别 (可被 dev 置为 0 表示取消)
    rolePermMap[r.roleId] = r.permLevel ?? 0
    roleCodeMap[r.roleId] = r.roleCode || ''
  }
  for (const u of data.userAuths ?? []) {
    userPermMap[u.userId] = u.permLevel ?? 0
    userRoleMap[u.userId] = Array.isArray(u.roleIds) ? [...u.roleIds] : []
  }
  // super_admin 角色:
  //   - 未保存 (permLevel === undefined): 非 developer 默认显示可管理 (3); developer 显示未授权
  //   - 已保存 (含 0): 按实际值展示 (dev 取消后非 dev 也能看到 0/未授权)
  for (const r of data.roleAuths ?? []) {
    if (isSuperAdminRole(r)) {
      if (r.permLevel == null && !isDeveloper.value) {
        rolePermMap[r.roleId] = 3
      } else {
        rolePermMap[r.roleId] = r.permLevel ?? 0
      }
    }
  }
  // 默认选中第一个角色 (优先 super_admin)
  if (!selectedRoleId.value && sortedRoles.value.length) {
    selectedRoleId.value = sortedRoles.value[0]!.roleId
  }
  activeTabRoleId.value = ''
  aVisibleCount.value = SCROLL_BATCH
  bVisibleCount.value = SCROLL_BATCH
  console.log('[KB-AUTH] syncStateFromDetail', {
    isDeveloper: isDeveloper.value,
    superAdmin: (data.roleAuths ?? []).find((r) => isSuperAdminRole(r)),
    superAdminRolePerm: (() => {
      const r = (data.roleAuths ?? []).find((x) => isSuperAdminRole(x))
      return r ? { roleId: r.roleId, raw: r.permLevel, final: rolePermMap[r.roleId] } : null
    })(),
  })
}

watch(activeKbId, async (val) => {
  if (val != null) {
    selectedRoleId.value = null
    await loadAuthDetail(val)
  } else {
    detail.value = null
  }
})

/* ========= 联动: 角色 <-> 用户 ========= */

/** 切换角色授权 (联动其下所有用户, 升降都跟, 保证 max(role,user) 一致) */
function onRolePermChange(roleId: number, level: number | string | undefined) {
  const newLevel = Number(level ?? 0)
  rolePermMap[roleId] = newLevel
  if (!detail.value) return
  for (const u of detail.value.userAuths ?? []) {
    if (Array.isArray(u.roleIds) && u.roleIds.includes(roleId)) {
      if (newLevel > 0) {
        // 联动: 该用户的直接授权 = newLevel (升降都跟, 与角色 max 一致)
        userPermMap[u.userId] = newLevel
      } else {
        userPermMap[u.userId] = 0
      }
    }
  }
}

/** 角色复选框点击 (toggle) */
function onRoleCheckToggle(roleId: number, checked: boolean) {
  if (checked) {
    onRolePermChange(roleId, Math.max(1, rolePermMap[roleId] ?? 1))
  } else {
    onRolePermChange(roleId, 0)
  }
}

/** 用户授权下拉变更 (联动其所有角色, 升降都跟, 保证 max(role,user) 一致) */
function onUserPermChange(userId: number, level: number | string | undefined) {
  const newLevel = Number(level ?? 0)
  userPermMap[userId] = newLevel
  const roleIds = userRoleMap[userId] ?? []
  for (const rid of roleIds) {
    if (newLevel > 0) {
      // 联动: 该角色授权 = newLevel (升降都跟)
      rolePermMap[rid] = newLevel
    } else {
      rolePermMap[rid] = 0
    }
  }
}

/** 用户复选框点击 (toggle) — 仅影响直接授权 + 联动角色授权 (向上) */
function onUserCheckToggle(user: KbAuthUserItem, checked: boolean) {
  if (checked) {
    const current = Math.max(currentUserPerm(user) || 0, 1)
    onUserPermChange(user.userId, current)
  } else {
    // 仅清直接授权: 角色授权保留 (避免反向清掉整角色)
    userPermMap[user.userId] = 0
  }
}

/** B 卡片取消授权: 全量清除 (直接授权 + 联动到的非超管角色) */
function revokeUserAuth(user: KbAuthUserItem) {
  userPermMap[user.userId] = 0
  const roleIds = userRoleMap[user.userId] ?? user.roleIds ?? []
  for (const rid of roleIds) {
    if (String(roleCodeMap[rid] || '').toLowerCase() === SUPER_ADMIN_CODE) continue
    rolePermMap[rid] = 0
  }
}

function onRoleClick(roleId: number) {
  selectedRoleId.value = roleId
  aVisibleCount.value = SCROLL_BATCH
  nextTick(() => resetSentinelObserver('a'))
}

/* ========= 拖拽分栏 ========= */

function onSplitterDown(ev: MouseEvent) {
  ev.preventDefault()
  dragging.value = true
  const layout = layoutRef.value
  if (!layout) return
  const rect = layout.getBoundingClientRect()
  const move = (e: MouseEvent) => {
    const x = e.clientX - rect.left
    const ratio = Math.min(0.85, Math.max(0.15, x / rect.width))
    aPaneRatio.value = ratio
  }
  const up = () => {
    dragging.value = false
    document.removeEventListener('mousemove', move)
    document.removeEventListener('mouseup', up)
    nextTick(() => {
      resetSentinelObserver('a')
      resetSentinelObserver('b')
    })
  }
  document.addEventListener('mousemove', move)
  document.addEventListener('mouseup', up)
}

/* ========= 滚动加载 ========= */

function resetSentinelObserver(which: 'a' | 'b') {
  const sentinel = which === 'a' ? aSentinelRef.value : bSentinelRef.value
  const observer = which === 'a' ? aObserver : bObserver
  if (observer) observer.disconnect()
  if (!sentinel) return
  const obs = new IntersectionObserver((entries) => {
    if (entries[0]?.isIntersecting) {
      if (which === 'a') {
        if (aVisibleCount.value < aFilteredUsers.value.length) {
          aVisibleCount.value = Math.min(aVisibleCount.value + SCROLL_BATCH, aFilteredUsers.value.length)
        }
      } else {
        if (bVisibleCount.value < bFilteredUsers.value.length) {
          bVisibleCount.value = Math.min(bVisibleCount.value + SCROLL_BATCH, bFilteredUsers.value.length)
        }
      }
    }
  }, { root: which === 'a' ? aScrollRef.value : bScrollRef.value, rootMargin: '0px 0px 40px 0px' })
  obs.observe(sentinel)
  if (which === 'a') aObserver = obs
  else bObserver = obs
}

watch(aFilteredUsers, () => {
  aVisibleCount.value = Math.min(aVisibleCount.value, aFilteredUsers.value.length) || SCROLL_BATCH
  nextTick(() => resetSentinelObserver('a'))
})

watch(bFilteredUsers, () => {
  bVisibleCount.value = Math.min(bVisibleCount.value, bFilteredUsers.value.length) || SCROLL_BATCH
  nextTick(() => resetSentinelObserver('b'))
})

watch(activeTabRoleId, () => {
  bVisibleCount.value = SCROLL_BATCH
  nextTick(() => resetSentinelObserver('b'))
})

watch(userSearch, () => {
  aVisibleCount.value = SCROLL_BATCH
  nextTick(() => resetSentinelObserver('a'))
})

/* 用户直接授权变更: 若某角色下已无被勾选用户, 自动清零该角色授权 (super_admin 例外) */
watch(userPermMap, () => {
  if (!detail.value) return
  for (const r of sortedRoles.value) {
    if (isSuperAdminRole(r)) continue
    const { authorized, total } = roleUserAuthStats(r.roleId)
    if (total > 0 && authorized === 0 && (rolePermMap[r.roleId] ?? 0) > 0) {
      rolePermMap[r.roleId] = 0
    }
  }
}, { deep: true })

/* ========= 保存 / 重置 / 返回 ========= */

async function onSave() {
  if (!activeKbId.value || !detail.value) return
  try {
    await ElMessageBox.confirm('保存后将覆盖该知识库现有的全部角色 / 用户授权, 确认保存？', '保存授权', {
      type: 'warning',
      confirmButtonText: '保存', cancelButtonText: '取消',
    })
  } catch { return }
  saving.value = true
  try {
    const roleAuths = (detail.value.roleAuths ?? []).map((r) => ({
      roleId: r.roleId,
      permLevel: rolePermMap[r.roleId] ?? 0,
    }))
    // 用户层只提交有直接授权的 (effective 来自角色的不重复保存)
    const userAuths = (detail.value.userAuths ?? [])
      .filter((u) => (userPermMap[u.userId] ?? 0) > 0)
      .map((u) => ({ userId: u.userId, permLevel: userPermMap[u.userId] ?? 0 }))
    await saveKbAuth(activeKbId.value, { roleAuths, userAuths })
    ElMessage.success('授权已保存')
    await loadAuthDetail(activeKbId.value)
  } catch (error) {
    console.error(error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function onReset() {
  if (!detail.value) return
  syncStateFromDetail(detail.value)
  ElMessage.info('已重置为当前保存状态')
}

function onBack() {
  router.push('/knowledge/base')
}

onMounted(async () => {
  await loadKbList()
  if (activeKbId.value != null) {
    await loadAuthDetail(activeKbId.value)
  } else if (kbList.value.length) {
    activeKbId.value = kbList.value[0]!.id
  }
  nextTick(() => {
    resetSentinelObserver('a')
    resetSentinelObserver('b')
  })
})

onBeforeUnmount(() => {
  aObserver?.disconnect()
  bObserver?.disconnect()
})
</script>

<template>
  <div class="kb-auth-layout">
    <header class="page-header">
      <div class="page-header-left">
        <el-button :icon="ArrowLeft" link @click="onBack">返回</el-button>
        <h1>知识库授权管理</h1>
      </div>
      <div class="page-header-right">
        <el-select v-model="activeKbId" placeholder="选择知识库" style="width: 280px" filterable>
          <el-option
            v-for="kb in kbList"
            :key="kb.id"
            :label="kb.kbName + ' (' + visibilityLabel(kb.visibilityScope) + ')'"
            :value="kb.id"
          />
        </el-select>
        <el-button :disabled="!detail" @click="onReset">重置</el-button>
        <el-button type="primary" :loading="saving" :disabled="!detail" @click="onSave">保存授权</el-button>
      </div>
    </header>

    <div v-if="activeKbId" class="kb-auth-summary">
      <span>当前知识库: <b>{{ activeKbName }}</b></span>
      <span>可见范围:
        <el-tag size="small" :type="activeKbScope === 'PUBLIC' ? 'success' : activeKbScope === 'INTERNAL' ? 'warning' : 'info'">
          {{ visibilityLabel(activeKbScope) }}
        </el-tag>
      </span>
      <span class="auth-tip">勾选角色或用户即授权, 高级别覆盖低级别; 仅 super_admin 角色锁定为可管理</span>
    </div>

    <div
      v-if="activeKbId"
      v-loading="loading"
      ref="layoutRef"
      class="kb-auth-body"
      :class="{ dragging }"
    >
      <!-- A 卡片: 角色 + 选中角色的用户 -->
          <div class="auth-pane pane-a" :style="{ flexBasis: (aPaneRatio * 100) + '%' }">
            <div class="pane-split">
          <!-- A 左: 角色列表 -->
          <div class="role-pane">
            <div class="role-pane-header">角色</div>
            <div class="role-list">
              <div
                v-for="role in sortedRoles"
                :key="role.roleId"
                class="role-item"
                :class="{
                  active: selectedRoleId === role.roleId,
                  granted: (rolePermMap[role.roleId] ?? 0) > 0,
                  locked: isSuperAdminRole(role) && !isDeveloper,
                }"
                @click="onRoleClick(role.roleId)"
              >
                <el-checkbox
                  :model-value="isRoleChecked(role.roleId)"
                  :indeterminate="isRoleIndeterminate(role.roleId)"
                  :disabled="isSuperAdminRole(role) && !isDeveloper"
                  @update:model-value="(v: boolean | string | number) => { onRoleCheckToggle(role.roleId, Boolean(v)); onRoleClick(role.roleId) }"
                  @click.stop="onRoleClick(role.roleId)"
                />
                <div class="role-info">
                  <div class="role-name">{{ role.roleName }}</div>
                  <div class="role-code">{{ role.roleCode }}</div>
                </div>
                <div v-if="isSuperAdminRole(role)" class="role-lock">
                  <el-tag size="small" type="danger">超管</el-tag>
                </div>
                <div class="role-perm" @click.stop>
                  <!-- super_admin 角色: 非 developer 显示文本, developer 显示下拉 (锁定 3) -->
                  <template v-if="isSuperAdminRole(role) && !isDeveloper">
                    <el-tag size="small" :type="displayRolePerm(role) >= 3 ? 'danger' : displayRolePerm(role) >= 2 ? 'warning' : displayRolePerm(role) >= 1 ? 'success' : 'info'">{{ permLabel(displayRolePerm(role)) }}</el-tag>
                  </template>
                  <template v-else>
                    <el-select
                      :model-value="baseRoleLevel(role) || undefined"
                      placeholder="未授权"
                      size="small"
                      style="width: 100px"
                      :disabled="baseRoleLevel(role) === 0 || (isSuperAdminRole(role) && !isDeveloper)"
                      @update:model-value="(v: number | string | undefined) => onRolePermChange(role.roleId, v)"
                    >
                      <el-option v-for="opt in PERM_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                    </el-select>
                  </template>
                </div>
              </div>
              <div v-if="!sortedRoles.length" class="pane-empty">暂无可授权角色</div>
            </div>
          </div>

          <!-- A 右: 选中角色的用户 (含搜索 + 滚动加载) -->
          <div class="user-pane">
            <div class="user-pane-header">
              <el-input
                v-model="userSearch"
                placeholder="搜索用户名 / 昵称"
                clearable
                size="small"
              />
            </div>
            <div ref="aScrollRef" class="user-scroll">
              <div
                v-for="user in aVisibleUsers"
                :key="user.userId"
                class="user-row"
                :class="{ granted: isUserChecked(user) }"
              >
                <el-checkbox
                  :model-value="isUserDirectChecked(user)"
                  :disabled="isSuperAdminUser(user) && !isDeveloper"
                  @update:model-value="(v: boolean | string | number) => onUserCheckToggle(user, Boolean(v))"
                />
                <div class="user-info">
                  <div class="user-name">
                    {{ userDisplayName(user) }}
                    <span class="user-username">@{{ user.username }}</span>
                  </div>
                  <div v-if="user.roleNames?.length" class="user-roles">
                    <el-tag v-for="r in user.roleNames" :key="r" size="small" effect="plain" type="info">{{ r }}</el-tag>
                  </div>
                </div>
                <div class="user-perm">
                  <!-- super_admin 用户: 非 developer 显示文本, developer 显示下拉 (锁定 3) -->
                  <template v-if="isSuperAdminUser(user) && !isDeveloper">
                    <el-tag size="small" :type="displayUserPerm(user) >= 3 ? 'danger' : displayUserPerm(user) >= 2 ? 'warning' : displayUserPerm(user) >= 1 ? 'success' : 'info'">{{ permLabel(displayUserPerm(user)) }}</el-tag>
                  </template>
                  <template v-else>
                    <el-select
                      :model-value="baseUserLevel(user) || undefined"
                      :placeholder="isUserChecked(user) ? permLabel(baseUserLevel(user)) : '未授权'"
                      size="small"
                      style="width: 110px"
                      :disabled="!isUserChecked(user)"
                      @update:model-value="(v: number | string | undefined) => onUserPermChange(user.userId, v)"
                    >
                      <el-option v-for="opt in PERM_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                    </el-select>
                  </template>
                </div>
              </div>
              <div v-if="!aFilteredUsers.length" class="pane-empty">当前角色下无用户</div>
              <div v-else-if="aVisibleUsers.length < aFilteredUsers.length" ref="aSentinelRef" class="scroll-sentinel">加载中…</div>
              <div v-else class="scroll-end">— 已加载全部 —</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分隔条 -->
      <div class="splitter" :style="{ width: SPLITTER_PX + 'px' }" @mousedown="onSplitterDown">
        <div class="splitter-handle" />
      </div>

      <!-- B 卡片: 已勾选用户 (按角色 tab 归类) -->
          <div class="auth-pane pane-b" :style="{ flexBasis: ((1 - aPaneRatio) * 100) + '%' }">
            <el-tabs v-model="activeTabRoleId" class="b-tabs">
          <el-tab-pane
            v-for="tab in bTabs"
            :key="tab.roleId || 'all'"
            :name="tab.roleId"
            :label="tab.roleId === '' ? `${tab.roleName} (${grantedUsers.length})` : `${tab.roleName} (${grantedUsers.filter(u => Array.isArray(u.roleIds) && u.roleIds.includes(Number(tab.roleId))).length})`"
          />
        </el-tabs>
        <div ref="bScrollRef" class="user-scroll b-scroll">
          <div
            v-for="user in bVisibleUsers"
            :key="user.userId"
            class="user-row"
            :class="{ granted: isUserChecked(user) }"
          >
            <div class="user-info">
              <div class="user-name">
                {{ userDisplayName(user) }}
                <span class="user-username">@{{ user.username }}</span>
              </div>
              <div v-if="user.roleNames?.length" class="user-roles">
                <el-tag v-for="r in user.roleNames" :key="r" size="small" effect="plain" type="info">{{ r }}</el-tag>
              </div>
            </div>
            <div class="user-perm">
              <el-tag :type="displayUserPerm(user) >= 3 ? 'danger' : displayUserPerm(user) >= 2 ? 'warning' : 'success'" size="small">
                {{ permLabel(displayUserPerm(user)) }}
              </el-tag>
              <el-tooltip content="取消授权" placement="top">
                <el-button
                  link
                  size="small"
                  class="revoke-btn"
                  :disabled="isSuperAdminUser(user) && !isDeveloper"
                  @click="revokeUserAuth(user)"
                >
                  <el-icon class="revoke-icon"><Close /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </div>
          <div v-if="!bFilteredUsers.length" class="pane-empty">当前 tab 下无已授权用户</div>
          <div v-else-if="bVisibleUsers.length < bFilteredUsers.length" ref="bSentinelRef" class="scroll-sentinel">加载中…</div>
          <div v-else class="scroll-end">— 已加载全部 —</div>
        </div>
      </div>
    </div>
    <el-empty v-else description="请选择一个知识库" />
  </div>
</template>

<style scoped>
.kb-auth-layout {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  padding: 16px 24px 24px;
  min-width: 880px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header-left h1 {
  margin: 0;
  font-size: 22px;
  color: #0f2744;
}

.page-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.kb-auth-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #303133;
}

.kb-auth-summary .auth-tip {
  color: #909399;
  font-size: 12px;
  flex: 1;
  text-align: right;
}

.kb-auth-body {
  flex: 1;
  display: flex;
  align-items: stretch;
  overflow: hidden;
  user-select: none;
}

.kb-auth-body.dragging {
  cursor: col-resize;
}

.auth-pane {
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
  min-width: 0;
  flex-grow: 0;
  flex-shrink: 1;
}

.pane-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fafbfc;
}

.pane-sub {
  font-size: 12px;
  color: #909399;
}

.pane-empty {
  padding: 40px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.pane-split {
  flex: 1;
  display: flex;
  min-height: 0;
}

.role-pane {
  width: 360px;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fbfcfd;
}

.role-pane-header {
  padding: 10px 14px;
  font-size: 12px;
  color: #909399;
  font-weight: 600;
  border-bottom: 1px solid #f0f2f5;
  background: #fff;
}

.role-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

.role-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  border: 1px solid transparent;
}

.user-perm {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.revoke-btn {
  color: #f56c6c;
  padding: 2px 4px;
  font-size: 16px;
}

.revoke-btn:hover:not(:disabled) {
  color: #c45656;
}

.revoke-btn:disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}

.revoke-icon {
  font-weight: 700;
}

.role-item:hover {
  background: #f0f6ff;
}

.role-item.active {
  background: #ecf5ff;
  border-color: #409eff;
}

.role-item.granted {
  background: #f0f9eb;
}

.role-item.locked {
  cursor: not-allowed;
}

.role-info {
  flex: 1;
  min-width: 0;
}

.dbg-perm {
  font-size: 10px;
  color: #c0c4cc;
  margin-left: 4px;
  font-family: monospace;
  cursor: help;
}

.role-name {
  font-weight: 600;
  color: #0f2744;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-code {
  font-size: 11px;
  color: #909399;
  font-family: monospace;
}

.user-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-pane-header {
  padding: 10px 14px;
  border-bottom: 1px solid #f0f2f5;
  background: #fff;
}

.user-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.user-row.granted {
  background: #f0f9eb;
  border-color: #c2e7b0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 600;
  color: #0f2744;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-username {
  color: #909399;
  font-weight: 400;
  font-size: 12px;
  margin-left: 4px;
}

.user-roles {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.user-perm {
  display: flex;
  align-items: center;
  gap: 8px;
}

.scroll-sentinel,
.scroll-end {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
  padding: 8px 0;
}

.splitter {
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.splitter-handle {
  width: 2px;
  height: 40%;
  background: #dcdfe6;
  border-radius: 2px;
  transition: background 0.15s;
}

.splitter:hover .splitter-handle,
.kb-auth-body.dragging .splitter-handle {
  background: #409eff;
}

.b-tabs {
  padding: 0 12px;
  margin-top: 4px;
}

.b-tabs :deep(.el-tabs__nav-wrap)::after {
  background: #ebeef5;
}

.b-scroll {
  padding-top: 6px;
}
</style>
