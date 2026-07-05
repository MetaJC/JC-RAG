<script setup lang="ts">
/**
 * 菜单管理 (RBAC 拆 4 页面之一)
 * <p>支持 el-tree 拖拽调整父子级 / 排序; 含 Q3 三级深度限制 (超 3 级弹窗提示, sessionStorage 去重)
 *
 * @author JC
 */
import { computed, onMounted, ref } from 'vue'
import { Delete, Edit, FolderAdd, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import RbacPermissionDialog from '../components/RbacPermissionDialog.vue'
import {
  createRbacMenu,
  deleteRbacMenu,
  fetchRbacMenuTree,
  moveRbacMenu,
  updateRbacMenu,
} from '../api/rbac'

const loading = ref(false)
const menus = ref<any[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'CATALOG' | 'MENU' | 'BUTTON' | 'API'>('MENU')
const editingId = ref<number | null>(null)
const initialParentId = ref<number | null>(null)
const initial = ref<any>({})

// ============ Q3: 三级深度限制 ============
const MAX_DEPTH = 2 // 0/1/2 允许; 目标 depth > 2 弹提示
const PROMPT_KEY = 'rbac:level4-prompted'

/** 把树压成 {id -> depth} Map, depth=根=0 */
function buildDepthMap(nodes: any[], map = new Map<number, number>(), d = 0): Map<number, number> {
  for (const n of nodes || []) {
    map.set(n.id, d)
    if (n.children?.length) buildDepthMap(n.children, map, d + 1)
  }
  return map
}

/** 把树压成 [{id, title, depth}] 给父级下拉 */
function flattenMenus(nodes: any[], depth = 0, out: any[] = []): any[] {
  for (const n of nodes || []) {
    out.push({ id: n.id, title: n.title || n.permName, depth })
    if (n.children?.length) flattenMenus(n.children, depth + 1, out)
  }
  return out
}
const flatMenuOptions = computed(() => flattenMenus(menus.value))

/**
 * 目标 depth > MAX_DEPTH 时, 弹窗提示一次.
 * <p>已提示过 (sessionStorage 标记) 直接放行; 取消则返回 false (调用方不保存)
 * <p>sessionStorage 随浏览器会话结束 / 重新登录清空
 */
async function confirmDeepLevel(targetDepth: number): Promise<boolean> {
  if (targetDepth <= MAX_DEPTH) return true
  if (sessionStorage.getItem(PROMPT_KEY) === '1') return true
  try {
    await ElMessageBox.confirm(
      `菜单层级建议不超过 3 级, 当前目标为第 ${targetDepth + 1} 级. 继续保存吗?`,
      '层级过深提示',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    sessionStorage.setItem(PROMPT_KEY, '1')
    return true
  } catch {
    return false
  }
}

async function loadMenuTree() {
  const data = await fetchRbacMenuTree()
  menus.value = data ?? []
}

function openCreateMenu(mode: 'CATALOG' | 'MENU' = 'MENU') {
  editingId.value = null
  dialogMode.value = mode
  initialParentId.value = null
  initial.value = {}
  dialogVisible.value = true
}

/** 工具栏"新建目录"按钮 - 显式事件 + 日志, 避免 el-tree 内部态干扰 */
function onCreateCatalog() {
  console.info('[menu-toolbar] onCreateCatalog clicked')
  openCreateMenu('CATALOG')
}

/** 工具栏"新建菜单"按钮 */
function onCreateMenu() {
  console.info('[menu-toolbar] onCreateMenu clicked')
  openCreateMenu('MENU')
}

async function openCreateMenuWithParent(parent: any) {
  // 父级 depth + 1 即新节点 depth
  const depthMap = buildDepthMap(menus.value)
  const newDepth = (depthMap.get(parent.id) ?? 0) + 1
  if (!(await confirmDeepLevel(newDepth))) return
  editingId.value = null
  dialogMode.value = 'MENU'
  initialParentId.value = parent.id
  initial.value = {}
  dialogVisible.value = true
}

function openEditPermission(perm: any) {
  editingId.value = perm.id
  dialogMode.value = perm.permType || 'MENU'
  initialParentId.value = perm.parentId ?? null
  initial.value = {
    permCode: perm.permCode || perm.code,
    permName: perm.permName || perm.title,
    permType: perm.permType,
    path: perm.path || '',
    method: perm.method || '',
    icon: perm.icon || '',
    sort: perm.sort ?? 0,
    component: perm.component || '',
    query: perm.query || '',
    isExternal: perm.isExternal ?? 0,
    keepAlive: perm.keepAlive ?? 0,
    visible: perm.visible ?? 1,
    status: perm.status ?? 1,
  }
  dialogVisible.value = true
}

/** Dialog 提交: 拿到 form 后, 父级做深度校验 (新建时), 再调 API */
async function onDialogSubmit(form: any) {
  if (!editingId.value) {
    // 新建: 若指定了父级, 校验目标 depth
    if (form.parentId != null) {
      const depthMap = buildDepthMap(menus.value)
      const newDepth = (depthMap.get(form.parentId) ?? 0) + 1
      if (!(await confirmDeepLevel(newDepth))) return
    }
  }
  const isMenuLike = form.permType === 'MENU' || form.permType === 'CATALOG'
  if (isMenuLike) {
    const payload = {
      parentId: form.parentId || null,
      permType: form.permType,
      code: form.permCode,
      title: form.permName,
      path: form.path,
      icon: form.icon,
      sort: form.sort,
      component: form.component,
      query: form.query,
      isExternal: form.isExternal,
      keepAlive: form.keepAlive,
      visible: form.visible,
      status: form.status,
    }
    if (editingId.value) {
      await updateRbacMenu(editingId.value, payload)
      ElMessage.success('菜单已更新')
    } else {
      await createRbacMenu(payload)
      ElMessage.success('菜单已创建')
    }
  }
  dialogVisible.value = false
  await loadMenuTree()
}

async function deleteMenu(menu: any) {
  try {
    await ElMessageBox.confirm(`确定删除菜单 "${menu.title}" 吗?`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await deleteRbacMenu(menu.id)
  ElMessage.success('菜单已删除')
  await loadMenuTree()
}

/**
 * 拖拽落位处理
 * <p>el-tree position: 'before' | 'after' | 'inner'
 * <ul>
 *   <li>inner: 设为 target 的子节点, sort 追加到末尾</li>
 *   <li>before/after: 设为 target.parent 的子节点, sort 走"整段重排"</li>
 * </ul>
 * <p>策略: 步长 10 整段重排所有兄弟
 * <ul>
 *   <li>读 el-tree 内部 childNodes (drop 后已重排), 找拖动节点的新 index</li>
 *   <li>新 sort = (i + 1) * 10, 避免中点小数的精度爆炸</li>
 *   <li>所有兄弟 sort 变了的批量调 move API</li>
 * </ul>
 * <p>Q3: 若新 depth > 2 弹窗提示, 取消则不保存
 */
async function handleMenuDrop(node: any, target: any, position: 'before' | 'after' | 'inner') {
  const draggedId = node.data.id
  if (!draggedId) {
    await loadMenuTree()
    return
  }
  // 调试日志: 打印所有上下文, 排查拖拽 BUG
  console.log('[menu-move-debug] node=', {
    id: node.data?.id,
    title: node.data?.title,
    parentId: node.data?.parentId,
  })
  console.log('[menu-move-debug] target=', {
    id: target.data?.id,
    title: target.data?.title,
    parentId: target.data?.parentId,
  })
  console.log('[menu-move-debug] target.parent=', {
    hasParent: !!target.parent,
    parentDataId: target.parent?.data?.id,
    parentLevel: target.parent?.level,
  })
  console.log('[menu-move-debug] position=', position)
  // 防御: 拖到自己之下 -> 拒绝
  if (node.data.id === target.data?.id) {
    await loadMenuTree()
    return
  }
  // 计算新父级 id:
  //   inner: 新父级 = target
  //   before/after: 新父级 = target.parent (根级时 = null)
  // 关键: el-tree 在 before/after 位置时, 实际 target 可能是"新父级的第一个子节点"
  // 此时需要穿透到 target.parent 才是真正的父级
  let newParentId: number | null = null
  if (position === 'inner') {
    newParentId = target.data?.id ?? null
  } else {
    // 探测: target 是不是其父级的第一个子? (即兄弟列表的 [0])
    const tpChildNodes: any[] = (target.parent?.childNodes ?? []).filter(
      (n: any) => n && n.data && n.data.id != null
    )
    if (tpChildNodes.length > 0 && tpChildNodes[0].data.id === target.data?.id) {
      // target 是父级首个有效子 -> 视为要插到该父级下 (新父级 = target.parent)
      newParentId = target.parent?.data?.id ?? null
    } else {
      newParentId = target.parent?.data?.id ?? null
    }
  }
  console.log('[menu-move-debug] newParentId=', newParentId)
  // 防回环: 检查 newParentId 是否为 draggedId 的后代 (或自身)
  // 即: newParent 在 dragged 的子树里 -> 拒绝
  if (newParentId != null && isDescendantOrSelf(draggedId, newParentId, menus.value)) {
    ElMessage.warning('不能将菜单移动到自身或其子节点下')
    await loadMenuTree()
    return
  }
  // el-tree 内部"新父级"包装节点 + 新兄弟列表 (drop 时已重排)
  // 过滤掉 null/虚拟节点, 防止 .data 访问报错
  const rawSiblings: any[] = (position === 'inner' ? target : target.parent)?.childNodes ?? []
  const siblingNodes = rawSiblings.filter((s: any) => s && s.data && s.data.id != null)
  // 找到拖动节点在 el-tree 内部新兄弟列表中的 index
  const newIndex = siblingNodes.findIndex((s: any) => s.data.id === draggedId)
  if (newIndex < 0) {
    // 兜底: 拿不到 index, 重新拉树
    console.warn('[menu-move] newIndex < 0, draggedId=', draggedId, 'siblings=', siblingNodes.map(s => s.data?.id))
    await loadMenuTree()
    return
  }
  // 深度校验: inner 时新 depth = target.depth + 1, 否则与 target 同深度
  const depthMap = buildDepthMap(menus.value)
  const newDepth =
    position === 'inner'
      ? (depthMap.get(target.data.id) ?? 0) + 1
      : (depthMap.get(target.data.id) ?? 0)
  if (!(await confirmDeepLevel(newDepth))) {
    await loadMenuTree()
    return
  }
  console.info('[menu-move] start', { draggedId, newParentId, newIndex, position, siblingCount: siblingNodes.length })
  // 关键: el-tree drop 后, 节点 s.data.parentId 已被 el-tree 实时改写, 不可信
  // 必须用原始 menus 树 (loadMenuTree 拉到的) 中的真实 parentId
  const originalParentById = buildOriginalParentMap(menus.value)
  // 收集所有需要更新的兄弟 (sort 变了才动)
  // dragged 节点即便 sort 没变, 只要父级变了就必须发请求, 否则数据库不变, 刷新后还原
  const updates: Promise<any>[] = []
  let draggedUpdateScheduled = false
  siblingNodes.forEach((s: any, i: number) => {
    const targetSort = (i + 1) * 10
    if (s.data.id === draggedId) {
      draggedUpdateScheduled = true
      const originalParent = originalParentById.get(draggedId) ?? null
      // dragged 节点: 父级变了 或 sort 变了 -> 发请求
      if (originalParent === newParentId && s.data.sort === targetSort) return
      updates.push(moveRbacMenu(draggedId, { parentId: newParentId, sort: targetSort }))
      return
    }
    if (s.data.sort === targetSort) return
    const originalParent = originalParentById.get(s.data.id) ?? s.data.parentId ?? null
    updates.push(moveRbacMenu(s.data.id, { parentId: originalParent, sort: targetSort }))
  })
  // 兜底: dragged 没在兄弟列表里 (root 移动到 inner 等场景), 强制发一次
  if (!draggedUpdateScheduled) {
    const originalParent = originalParentById.get(draggedId) ?? node.data.parentId ?? null
    if (originalParent !== newParentId) {
      const newSort = (siblingNodes.length + 1) * 10
      updates.push(moveRbacMenu(draggedId, { parentId: newParentId, sort: newSort }))
    } else {
      // 父级和 sort 都没变, 不发
      console.info('[menu-move] noop: dragged not in siblings and parent/sort unchanged')
    }
  }
  if (updates.length === 0) {
    // 顺序没变, 仅 reload 同步
    await loadMenuTree()
    return
  }
  try {
    await Promise.all(updates)
    ElMessage.success('菜单已移动')
  } catch (e: any) {
    ElMessage.error(e?.message || '移动失败')
    console.error('[menu-move] failed', e)
  } finally {
    await loadMenuTree()
  }
}

/**
 * 构造 nodeId -> parentId 的映射, 用于从原始 menus 树中查询未污染的 parentId
 * <p>为什么需要: el-tree drop 后, 节点的 data.parentId 已被 el-tree 内部实时改写,
 * 不能再用作"原 parentId"
 */
function buildOriginalParentMap(nodes: any[], map: Map<number, number | null> = new Map()): Map<number, number | null> {
  for (const n of nodes ?? []) {
    if (n.id != null) map.set(n.id, n.parentId ?? null)
    if (n.children?.length) buildOriginalParentMap(n.children, map)
  }
  return map
}

/**
 * 判断 searchId 是否在 rootId 的子树里 (含 rootId 自身)
 * <p>用途: 防回环 - 若 newParentId 在 draggedId 子树中, 则会形成环, 必须拒绝
 */
function isDescendantOrSelf(rootId: number, searchId: number, nodes: any[]): boolean {
  for (const n of nodes) {
    if (n.id === rootId) {
      // 找到 root 节点, 在其子树中递归查找 searchId
      return findInSubtree(n, searchId)
    }
    if (n.children?.length && isDescendantOrSelf(rootId, searchId, n.children)) {
      return true
    }
  }
  return false
}

/** 在节点 n 的子树中查找 id == searchId 的节点 (含 n 自身) */
function findInSubtree(n: any, searchId: number): boolean {
  if (n.id === searchId) return true
  for (const c of n.children ?? []) {
    if (findInSubtree(c, searchId)) return true
  }
  return false
}

/** 禁止把目录拖到 API/按钮下, 防自拖 (前端兜底) */
function allowMenuDrop(draggingNode: any, dropNode: any, type: 'prev' | 'inner' | 'next') {
  if (draggingNode.data.id === dropNode.data.id) return false
  if (type === 'inner') {
    if (dropNode.data.permType && dropNode.data.permType !== 'MENU' && dropNode.data.permType !== 'CATALOG') {
      return false
    }
  }
  return true
}

onMounted(async () => {
  loading.value = true
  try {
    await loadMenuTree()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <header class="page-header">
      <div>
        <h1>菜单管理</h1>
        <span>维护菜单树结构、路由、图标; 拖拽可调整父子级与排序</span>
      </div>
      <!-- 工具栏提到 header 内, 避免被 el-tree 拖拽态覆盖 -->
      <div class="toolbar">
        <el-text type="info" size="middle" class="toolbar-hint">
          提示: 可拖拽节点调整父子级关系与排序; 建议层级 ≤ 3 级
        </el-text>
        <el-button @click="onCreateCatalog">新建目录</el-button>
        <el-button type="primary" @click="onCreateMenu">新建菜单</el-button>
      </div>
    </header>

    <el-card class="panel-card" shadow="never" v-loading="loading">
      <el-tree
        ref="menuTreeRef"
        :data="menus"
        node-key="id"
        :props="{ children: 'children', label: 'title' }"
        draggable
        :allow-drop="allowMenuDrop"
        :default-expand-all="true"
        empty-text="暂无菜单"
        class="menu-tree"
        @node-drop="handleMenuDrop"
      >
        <template #default="{ data }">
          <div class="menu-tree-row">
            <div class="menu-tree-info">
              <!-- 图标固定宽占位: 有图标渲染, 无图标空 div 占位, 标题左对齐 -->
              <span class="menu-icon-slot" :class="{ 'is-empty': !data.icon }">
                <el-icon v-if="data.icon" :size="14" color="#6b7280">
                  <component :is="data.icon" />
                </el-icon>
              </span>
              <span class="menu-title">{{ data.title }}</span>
              <el-tag v-if="data.permType === 'CATALOG'" size="small" type="warning" effect="plain" class="menu-tag">目录</el-tag>
              <el-tag v-else-if="data.visible === 0" size="small" type="info" effect="plain" class="menu-tag">隐藏</el-tag>
              <span class="menu-path">{{ data.path || '—' }}</span>
            </div>
            <div class="menu-tree-actions">
              <el-tooltip content="编辑" placement="top">
                <el-button link type="primary" :icon="Edit" @click.stop="openEditPermission(data)" />
              </el-tooltip>
              <el-tooltip content="新增下级" placement="top">
                <el-button link type="primary" :icon="Plus" @click.stop="openCreateMenuWithParent(data)" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button link type="danger" :icon="Delete" @click.stop="deleteMenu(data)" />
              </el-tooltip>
            </div>
          </div>
        </template>
      </el-tree>
    </el-card>

    <RbacPermissionDialog
      v-model:visible="dialogVisible"
      :mode="dialogMode"
      :editing-id="editingId"
      :flat-menu-options="flatMenuOptions"
      :initial-parent-id="initialParentId"
      :initial-perm-code="initial.permCode"
      :initial-perm-name="initial.permName"
      :initial-perm-type="initial.permType"
      :initial-path="initial.path"
      :initial-method="initial.method"
      :initial-icon="initial.icon"
      :initial-sort="initial.sort"
      :initial-component="initial.component"
      :initial-query="initial.query"
      :initial-is-external="initial.isExternal"
      :initial-keep-alive="initial.keepAlive"
      :initial-visible="initial.visible"
      :initial-status="initial.status"
      @submit="onDialogSubmit"
    />
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

.page-header p,
.page-header span {
  color: #6b7280;
  font-size: 13px;
  margin: 0;
  white-space: nowrap;
}

.panel-card {
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  justify-content: flex-end;
}

.toolbar-hint {
  margin-left: 4px;
  color: #6b7280;
}

.menu-tree {
  padding: 12px 8px;
}

/* 树节点整体: 适度行高, 不要挤在一起, 也不要太松 */
.menu-tree :deep(.el-tree-node__content) {
  height: 32px;
  padding: 0 10px;
  border-radius: 6px;
  margin: 1px 0;
  transition: background-color 0.2s ease;
}

.menu-tree :deep(.el-tree-node__content:hover) {
  background-color: #f3f4f6;
}

.menu-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: #eff6ff;
}

/* 展开/折叠三角图标放大 */
.menu-tree :deep(.el-tree-node__expand-icon) {
  font-size: 16px;
  padding: 4px;
  color: #6b7280;
}

.menu-tree-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 16px;
  gap: 12px;
}

.menu-tree-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.menu-icon {
  color: #4b5563;
  font-size: 16px;
}

/* 树节点图标固定宽占位: 没设置图标时也留出位置, 标题文字左对齐 */
.menu-icon-slot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  margin-right: 4px;
}
.menu-icon-slot.is-empty {
  background: transparent;
}

/* 菜单标题: 大气一点, 字号 15px, 字重 500 */
.menu-title {
  font-weight: 500;
  color: #1f2937;
  font-size: 15px;
  letter-spacing: 0.3px;
}

.menu-tag {
  margin-left: 6px;
}

.menu-path {
  color: #9ca3af;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  letter-spacing: 0.2px;
}

.menu-tree-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
</style>
