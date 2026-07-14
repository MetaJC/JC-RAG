<script setup lang="ts">
/**
 * 部门管理 (RBAC 组织维度) — el-table 普通模式 + 手动扁平化 + 展开/折叠.
 * <p>路径: /rbac/departments
 * <p>设计目标:
 * <ul>
 *   <li>列表采用 el-table 普通模式, 5 列稳定显示 (名称/排序/状态/创建时间/操作)</li>
 *   <li>手动维护 expandedIds 状态, 通过 padding-left 实现缩进, 与 el-table 树形表格样式一致</li>
 *   <li>SortableJS 拖拽扁平列表, 按物理位置计算 parentId / sort</li>
 *   <li>父级选择 (新增/编辑): popover + el-tree, 默认全部展开, 支持双击 / 确认按钮</li>
 *   <li>客户端关键字 / 状态过滤, 命中节点保留祖先链</li>
 * </ul>
 *
 * @author JC
 */
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  ArrowDown,
  ArrowRight,
  Check,
  CircleClose,
  Connection,
  Delete,
  Document,
  Edit,
  Expand,
  FolderOpened,
  Fold,
  OfficeBuilding,
  Plus,
  Refresh,
  Search,
  Sort,
  User,
} from '@element-plus/icons-vue'
import Sortable from 'sortablejs'
import { ElMessage, ElMessageBox, type TableInstance } from 'element-plus'

import {
  createRbacDepartment,
  deleteRbacDepartment,
  fetchRbacDepartment,
  fetchRbacDepartmentTree,
  moveRbacDepartment,
  updateRbacDepartment,
  updateRbacDepartmentStatus,
  updateRbacDepartmentStatusCascade,
} from '../api/rbac'

type DeptNode = {
  id: number
  parentId: number
  name: string
  sort?: number
  status: number
  remark?: string
  hasChildren?: boolean
  children?: DeptNode[]
  createdAt?: string
  updatedAt?: string
}

/** 扁平化行: 保留原 node 字段, 附加 level/hasChildren 用于渲染缩进与折叠 */
type FlatDept = DeptNode & { level: number; hasChildren: boolean }

const loading = ref(false)
const deptTree = ref<DeptNode[]>([])
/** 已展开的部门 id 集合 (点击行内展开/折叠按钮维护) */
const expandedIds = ref<Set<number>>(new Set())

/** 关键字 / 状态过滤后的树 */
const filteredTree = computed(() => applyClientFilter(deptTree.value, keyword.value, statusFilter.value))

/** 扁平化渲染列表: 遍历 filteredTree, 命中节点保留祖先链, 已展开节点的 children 继续展开 */
const flatList = computed<FlatDept[]>(() => flattenForDisplay(filteredTree.value, expandedIds.value))

const deptTableRef = ref<TableInstance | null>(null)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)

/* =====================================================================
 * CRUD 弹窗
 * ===================================================================== */
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  parentId: 0,
  name: '',
  sort: 0,
  status: 1,
  remark: '',
})

/* =====================================================================
 * 父级选择 (popover + tree)
 * ===================================================================== */
const parentTreeRef = ref<InstanceType<typeof import('element-plus').ElTree> | null>(null)
const parentPopoverVisible = ref(false)
const parentTreeKeyword = ref('')
const parentTreeCheckedId = ref<number | null>(null)
const parentTreeProps = { value: 'id', label: 'name', children: 'children' }
const parentOptions = ref<DeptNode[]>([])

/* =====================================================================
 * 数据加载
 * ===================================================================== */

async function loadDeptTree() {
  loading.value = true
  try {
    const data = (await fetchRbacDepartmentTree()) ?? []
    deptTree.value = data
    // 首次加载默认全部展开
    if (expandedIds.value.size === 0) {
      expandedIds.value = new Set(collectAllIds(data))
    }
  } finally {
    loading.value = false
  }
}

function collectAllIds(nodes: DeptNode[]): number[] {
  const out: number[] = []
  const dfs = (arr: DeptNode[]) => {
    for (const n of arr) {
      if (n.children?.length) {
        out.push(n.id)
        dfs(n.children)
      }
    }
  }
  dfs(nodes)
  return out
}

async function loadParentOptions() {
  const data = (await fetchRbacDepartmentTree()) ?? []
  parentOptions.value = [{ id: 0, name: '顶级 (无上级)', children: data } as any]
}

function applyClientFilter(nodes: DeptNode[], kw: string, status: number | undefined): DeptNode[] {
  if (!kw && status === undefined) return nodes
  const out: DeptNode[] = []
  for (const n of nodes) {
    const matchSelf = matchNode(n, kw, status)
    const filteredChildren = n.children ? applyClientFilter(n.children, kw, status) : []
    if (matchSelf || filteredChildren.length > 0) {
      out.push({ ...n, children: filteredChildren })
    }
  }
  return out
}

function matchNode(n: DeptNode, kw: string, status: number | undefined): boolean {
  if (status !== undefined && n.status !== status) return false
  if (!kw) return true
  return (n.name ?? '').toLowerCase().includes(kw.toLowerCase())
}

/** 扁平化: 命中节点保留祖先链, 已展开节点继续展开子级.
 * <p>注意: 显式剔除 children 字段, 避免 el-table 误识别为树形表格重复出展开按钮.
 */
function flattenForDisplay(nodes: DeptNode[], expanded: Set<number>, level = 0, out: FlatDept[] = []): FlatDept[] {
  for (const n of nodes) {
    const hasChildren = !!(n.children?.length)
    out.push({
      id: n.id,
      parentId: n.parentId,
      name: n.name,
      sort: n.sort,
      status: n.status,
      remark: n.remark,
      createdAt: n.createdAt,
      updatedAt: n.updatedAt,
      level,
      hasChildren,
    } as FlatDept)
    if (hasChildren && expanded.has(n.id)) {
      flattenForDisplay(n.children!, expanded, level + 1, out)
    }
  }
  return out
}

/* =====================================================================
 * 展开 / 折叠
 * ===================================================================== */

function toggleExpand(row: FlatDept) {
  if (!row.hasChildren) return
  const next = new Set(expandedIds.value)
  if (next.has(row.id)) next.delete(row.id)
  else next.add(row.id)
  expandedIds.value = next
}

/**
 * el-table row-class-name: 给每行加 dept-row-{id} class,
 * <p>用于拖拽时通过 className 反查部门 id (SortableJS evt.item 没有 data-*).
 */
function rowClassName({ row }: { row: FlatDept }): string {
  return `dept-row-${row.id}`
}

function expandAll() {
  expandedIds.value = new Set(collectAllIds(deptTree.value))
}

function collapseAll() {
  expandedIds.value = new Set()
}

/* =====================================================================
 * CRUD
 * ===================================================================== */

function openCreate(parentId = 0) {
  editingId.value = null
  Object.assign(form, { parentId, name: '', sort: 0, status: 1, remark: '' })
  dialogVisible.value = true
}

async function openEdit(row: FlatDept) {
  editingId.value = row.id
  const detail: any = await fetchRbacDepartment(row.id)
  Object.assign(form, {
    parentId: detail.parentId ?? 0,
    name: detail.name,
    sort: detail.sort ?? 0,
    status: detail.status ?? 1,
    remark: detail.remark ?? '',
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.name?.trim()) {
    ElMessage.warning('部门名称不能为空')
    return
  }
  if (editingId.value) {
    await updateRbacDepartment(editingId.value, { ...form })
    ElMessage.success('更新成功')
  } else {
    await createRbacDepartment({ ...form })
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  await loadDeptTree()
  await loadParentOptions()
}

/* =====================================================================
 * 行拖拽 (SortableJS + el-table 扁平列表)
 * <p>扁平列表上拖动, 按物理位置计算 newParentId / newSort.
 * <p>位置视觉提示 (before/inner) 不依赖 SortableJS onMove (forceFallback 模式下不可靠),
 * 改用全局 mousemove 监听器 + elementsFromPoint 自己命中目标 tr.
 * <p>核心交互 (参考 Ruoyi 菜单拖拽):
 * <ul>
 *   <li>鼠标在目标行 B 的下半部分 (Y >= 50%) → B 整行蓝框 → A 将成为 B 的子级</li>
 *   <li>鼠标在目标行 B 的上半部分 (Y < 50%) → B 上边界蓝线 → A 将成为 B 的同级, 排在 B 之前</li>
 *   <li>鼠标在 tbody 内的行间隙 (两行之间) → 间隙蓝线 → A 将成为同级, 排在间隙之后的行之前</li>
 *   <li>上下移动切换目标行/间隙; 左右移动不直接影响视觉</li>
 *   <li>禁止拖到源行自身 / 源部门的后代 (循环)</li>
 * </ul>
 * ===================================================================== */

let sortableInstance: Sortable | null = null

/** 全局 mousemove 监听器引用, 拖动开始挂载, 结束卸载 */
let moveListener: ((e: MouseEvent) => void) | null = null

/** 拖动开始时的源行 (dragged 行) */
let sourceTr: HTMLElement | null = null

/** 拖动开始时源行在 flatList 中的索引 (用于调用接口时定位被拖动部门) */
let sourceOldIndex = -1

/** 当前鼠标位置的拖放提示状态 */
type DropMode = 'inner' | 'before' | 'none'
let dropMode: DropMode = 'none'
/** 蓝框所在的 tr (inner 模式) */
let dropTargetTr: HTMLElement | null = null
/** 蓝线所在的 tr (before 模式: 蓝线在该 tr 上边界) */
let dropGapTr: HTMLElement | null = null

/**
 * 从 tr 的 className 解析部门 id.
 * <p>el-table 不支持 data-* 属性传给 tr, 用 row-class-name 加 dept-row-{id} class.
 */
function getDeptIdFromTr(tr: HTMLElement | null | undefined): number | null {
  if (!tr) return null
  const cls = tr.className || ''
  const m = cls.match(/dept-row-(\d+)/)
  return m ? Number(m[1]) : null
}

/**
 * 全局 mousemove: 命中鼠标下的 tr, 判定 inner/before, 应用视觉.
 */
function handleGlobalMove(e: MouseEvent) {
  if (!sourceTr) return

  const tbody = sourceTr.closest('tbody') as HTMLElement | null
  if (!tbody) {
    clearDropHints()
    return
  }

  // 隐藏 drag 元素以便 elementsFromPoint 命中真实目标
  // (forceFallback 模式下 drag 元素有 pointer-events:none, 这里再次兜底)
  const elems = document.elementsFromPoint(e.clientX, e.clientY)
  let tr: HTMLElement | null = null
  for (const el of elems) {
    const t = (el as HTMLElement).closest?.('tr.el-table__row') as HTMLElement | null
    if (t && tbody.contains(t)) {
      tr = t
      break
    }
  }

  // 鼠标不在任何 tr 上 → 视为 tbody 内行间隙 → 找最近的 tr (在它之上)
  if (!tr) {
    handleGapMove(e, tbody)
    return
  }

  // 不能拖到源行自身
  if (tr === sourceTr) {
    clearDropHints()
    return
  }

  const targetDeptId = getDeptIdFromTr(tr)
  if (targetDeptId == null) {
    clearDropHints()
    return
  }

  // 禁止拖到源部门的后代 (循环)
  const dragged = flatList.value[sourceOldIndex]
  if (dragged && isDescendantOrSelf(dragged.id, targetDeptId, deptTree.value)) {
    clearDropHints()
    return
  }

  // 计算鼠标在 tr 内的 Y 位置 → inner(下半) / before(上半)
  const rect = tr.getBoundingClientRect()
  const offsetY = e.clientY - rect.top
  const h = rect.height || 1

  // 清理旧视觉
  clearDropHints()

  if (offsetY < h * 0.5) {
    // 上半: 蓝线在该 tr 上边界
    tr.classList.add('dept-drop-gap-top')
    dropMode = 'before'
    dropGapTr = tr
  } else {
    // 下半: 蓝框
    tr.classList.add('dept-drop-target')
    dropMode = 'inner'
    dropTargetTr = tr
  }
}

/**
 * 鼠标不在任何 tr 上时, 按 Y 位置找最近的两个 tr,
 * <p>蓝线画在"上方 tr 下边界"或"下方 tr 上边界" (间隙指示).
 */
function handleGapMove(e: MouseEvent, tbody: HTMLElement) {
  const rows = Array.from(tbody.querySelectorAll('tr.el-table__row')) as HTMLElement[]
  if (!rows.length) {
    clearDropHints()
    return
  }
  // 跳过源行
  const candidates = rows.filter((r) => r !== sourceTr)
  if (!candidates.length) {
    clearDropHints()
    return
  }
  let prevTr: HTMLElement | null = null
  let nextTr: HTMLElement | null = null
  for (const r of candidates) {
    const rect = r.getBoundingClientRect()
    if (e.clientY < rect.top) {
      nextTr = r
      break
    }
    prevTr = r
  }
  // 优先级: 鼠标上方有 tr → 蓝线在该 tr 下边界; 否则在 nextTr 上边界
  if (prevTr) {
    clearDropHints()
    prevTr.classList.add('dept-drop-gap-bottom')
    dropMode = 'before'
    dropGapTr = prevTr
  } else if (nextTr) {
    clearDropHints()
    nextTr.classList.add('dept-drop-gap-top')
    dropMode = 'before'
    dropGapTr = nextTr
  } else {
    clearDropHints()
  }
}

function clearDropHints() {
  if (dropTargetTr) {
    dropTargetTr.classList.remove('dept-drop-target')
    dropTargetTr = null
  }
  if (dropGapTr) {
    dropGapTr.classList.remove('dept-drop-gap-top', 'dept-drop-gap-bottom')
    dropGapTr = null
  }
  dropMode = 'none'
}

function attachMoveListener() {
  if (!moveListener) {
    moveListener = (e: MouseEvent) => handleGlobalMove(e)
    document.addEventListener('mousemove', moveListener)
  }
}

function detachMoveListener() {
  if (moveListener) {
    document.removeEventListener('mousemove', moveListener)
    moveListener = null
  }
  // 清理目标位置视觉
  clearDropHints()
  // 清理源行视觉
  if (sourceTr) {
    sourceTr.classList.remove('dept-drop-source')
    sourceTr = null
  }
  sourceOldIndex = -1
}

async function handleSortEnd(evt: Sortable.SortableEvent) {
  const oldIndex = evt.oldIndex ?? -1
  const releaseEvent = evt.originalEvent as MouseEvent | undefined

  const list = flatList.value
  const dragged = list[oldIndex]
  // 记录被拖动部门 (detach 前快照)
  const draggedSnapshot = dragged

  // 清理视觉 + 解绑监听
  detachMoveListener()

  if (!draggedSnapshot) {
    await loadDeptTree()
    await nextTick()
    initSortable()
    return
  }

  // 没有命中有效目标: 保持原位
  if (dropMode === 'none' || (!dropTargetTr && !dropGapTr)) {
    await loadDeptTree()
    return
  }

  // 计算目标部门和位置
  const targetTr = dropTargetTr || dropGapTr
  const targetDeptId = getDeptIdFromTr(targetTr)
  if (targetDeptId == null) {
    await loadDeptTree()
    return
  }
  // 二次校验: 目标不能是源部门自身或后代
  if (isDescendantOrSelf(draggedSnapshot.id, targetDeptId, deptTree.value)) {
    ElMessage.warning('不能将部门移动到自身或其后代下')
    await loadDeptTree()
    return
  }

  const position = dropMode === 'inner' ? 'inner' : 'before'
  try {
    await moveRbacDepartment(draggedSnapshot.id, { targetId: targetDeptId, position })
    ElMessage.success(
      position === 'inner'
        ? `已将 [${draggedSnapshot.name}] 移入目标部门下`
        : `已将 [${draggedSnapshot.name}] 移动到目标位置 (同级)`
    )
  } catch (e: any) {
    ElMessage.error(e?.message || '移动失败')
  } finally {
    await loadDeptTree()
    await loadParentOptions()
    await nextTick()
    initSortable()
  }
}

function initSortable() {
  destroySortable()
  nextTick(() => {
    const tbody = document.querySelector(
      '.dept-table-wrap .el-table__body-wrapper tbody'
    ) as HTMLElement | null
    if (!tbody) return
    sortableInstance = Sortable.create(tbody, {
      animation: 150,
      handle: '.dept-drag-handle',
      ghostClass: 'dept-sortable-ghost',
      chosenClass: 'dept-sortable-chosen',
      dragClass: 'dept-sortable-drag',
      forceFallback: true,
      fallbackOnBody: true,
      fallbackTolerance: 3,
      onStart: (evt) => {
        // 记录源行 + 源行索引
        const item = evt.item as HTMLElement
        sourceTr = item.closest('tr.el-table__row') as HTMLElement
        sourceOldIndex = evt.oldIndex ?? -1
        if (sourceTr) {
          sourceTr.classList.add('dept-drop-source')
        }
        attachMoveListener()
      },
      onEnd: (evt) => {
        detachMoveListener()
        return handleSortEnd(evt)
      },
    })
  })
}

function destroySortable() {
  if (sortableInstance) {
    sortableInstance.destroy()
    sortableInstance = null
  }
}

function isDescendantOrSelf(rootId: number, searchId: number, nodes: DeptNode[]): boolean {
  const findInSubtree = (n: DeptNode): boolean => {
    if (n.id === searchId) return true
    for (const c of n.children ?? []) {
      if (findInSubtree(c)) return true
    }
    return false
  }
  for (const n of nodes) {
    if (n.id === rootId) return findInSubtree(n)
    if (n.children?.length && isDescendantOrSelf(rootId, searchId, n.children)) return true
  }
  return false
}

/* =====================================================================
 * 状态切换 (含级联)
 * ===================================================================== */

function collectDescendantNames(row: DeptNode): string[] {
  const out: string[] = []
  const stack: DeptNode[] = []
  if (row.children?.length) stack.push(...row.children)
  while (stack.length) {
    const n = stack.pop()!
    out.push(n.name)
    if (n.children?.length) stack.push(...n.children)
  }
  return out
}

async function toggleStatus(row: FlatDept) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '禁用'
  const descendantNames = collectDescendantNames(row)

  let confirmMsg = `确认${action}部门 [${row.name}]?`
  if (descendantNames.length > 0) {
    const tail =
      descendantNames.length <= 5
        ? descendantNames.join('、')
        : descendantNames.slice(0, 5).join('、') + ` 等 ${descendantNames.length} 个子部门`
    confirmMsg = `确认${action}部门 [${row.name}]?\n\n该操作将级联${action}以下 ${descendantNames.length} 个子部门:\n${tail}\n\n${action}后这些部门及其下级将无法访问, 是否继续?`
  }

  try {
    await ElMessageBox.confirm(confirmMsg, `${action}部门`, {
      type: 'warning',
      confirmButtonText: action,
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  if (descendantNames.length > 0) {
    const result = await updateRbacDepartmentStatusCascade(row.id, next)
    const tip = `已${action}: ${(result.affectedNames || []).join('、')}`
    ElMessage.success({ message: tip, duration: 3000 })
  } else {
    await updateRbacDepartmentStatus(row.id, next)
    ElMessage.success('状态已更新')
  }
  await loadDeptTree()
  await loadParentOptions()
  await nextTick()
  initSortable()
}

async function remove(row: FlatDept) {
  try {
    await ElMessageBox.confirm(
      `确认删除部门 [${row.name}]? 存在子部门或启用岗位时不允许删除`,
      '危险操作',
      { type: 'error' }
    )
  } catch {
    return
  }
  await deleteRbacDepartment(row.id)
  ElMessage.success('删除成功')
  await loadDeptTree()
  await loadParentOptions()
  await nextTick()
  initSortable()
}

/* =====================================================================
 * 父级部门 (popover + tree) 交互
 * ===================================================================== */

function onParentPopoverShow() {
  parentTreeKeyword.value = ''
  parentTreeCheckedId.value = form.parentId ?? null
  nextTick(() => {
    if (parentTreeCheckedId.value != null) {
      parentTreeRef.value?.setCurrentKey(parentTreeCheckedId.value as any)
    }
  })
}

function onParentPopoverHide() {
  parentTreeKeyword.value = ''
}

function onParentNodeClick(node: DeptNode) {
  if (
    editingId.value &&
    (node.id === editingId.value ||
      isDescendantInTree(parentOptions.value, node.id, editingId.value))
  ) {
    ElMessage.warning('不能选择自身或自身的后代作为父级')
    return
  }
  parentTreeCheckedId.value = node.id
}

function onParentNodeDblClick(node: DeptNode) {
  if (
    editingId.value &&
    (node.id === editingId.value ||
      isDescendantInTree(parentOptions.value, node.id, editingId.value))
  ) {
    ElMessage.warning('不能选择自身或自身的后代作为父级')
    return
  }
  form.parentId = node.id
  parentPopoverVisible.value = false
}

function confirmParent() {
  if (parentTreeCheckedId.value == null) {
    ElMessage.warning('请先在树上选择父级部门')
    return
  }
  form.parentId = parentTreeCheckedId.value
  parentPopoverVisible.value = false
}

function clearParent() {
  form.parentId = 0
  parentTreeCheckedId.value = null
}

function onClearParent() {
  clearParent()
  parentPopoverVisible.value = false
}

function filterParentNode(kw: string, data: DeptNode): boolean {
  if (!kw) return true
  return (data.name ?? '').toLowerCase().includes(kw.toLowerCase())
}

function expandAllParentTree() {
  parentTreeRef.value?.store?.nodesMap &&
    Object.values(parentTreeRef.value.store.nodesMap).forEach((n: any) => n.expand())
}

function collapseAllParentTree() {
  parentTreeRef.value?.store?.nodesMap &&
    Object.values(parentTreeRef.value.store.nodesMap).forEach((n: any) => n.collapse())
}

function isDescendantInTree(nodes: DeptNode[], candidateId: number, rootId: number): boolean {
  const findNode = (ns: any[], id: number): any | null => {
    for (const n of ns) {
      if (n.id === id) return n
      if (n.children?.length) {
        const f = findNode(n.children, id)
        if (f) return f
      }
    }
    return null
  }
  const root = findNode(nodes, rootId)
  if (!root) return false
  const stack = [...(root.children ?? [])]
  while (stack.length) {
    const n = stack.pop()
    if (n.id === candidateId) return true
    if (n.children?.length) stack.push(...n.children)
  }
  return false
}

/* =====================================================================
 * 父级输入框实时回显
 * ===================================================================== */

const parentInputDisplay = ref('')
function syncParentDisplay() {
  if (!form.parentId) {
    parentInputDisplay.value = '顶级 (无上级)'
    return
  }
  if (!parentOptions.value?.length) {
    parentInputDisplay.value = `ID:${form.parentId}`
    return
  }
  const findName = (ns: { id: number; name: string; children?: any[] }[]): string | null => {
    for (const n of ns) {
      if (n.id === form.parentId) return n.name
      if (n.children?.length) {
        const r = findName(n.children)
        if (r) return r
      }
    }
    return null
  }
  parentInputDisplay.value = findName(parentOptions.value) ?? `ID:${form.parentId}`
}

async function onDialogOpen() {
  await loadParentOptions()
  await nextTick()
  syncParentDisplay()
}

function formatDate(d?: string) {
  if (!d) return '—'
  const dt = new Date(d)
  if (isNaN(dt.getTime())) return d
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

/* =====================================================================
 * 监听器
 * ===================================================================== */

watch(() => form.parentId, () => syncParentDisplay())

watch(flatList, () => {
  // 列表变化 (展开/折叠/过滤/数据更新) 后, 重新初始化 SortableJS
  nextTick(() => initSortable())
})

/* =====================================================================
 * 生命周期
 * ===================================================================== */

onMounted(async () => {
  await loadDeptTree()
  await loadParentOptions()
})

onBeforeUnmount(() => {
  destroySortable()
})
</script>

<template>
  <div class="rbac-dept">
    <header class="page-header">
      <div>
        <h1>部门管理</h1>
        <span>维护组织架构; 拖拽节点可调整父子级关系与同级排序</span>
      </div>
    </header>

    <el-card class="panel-card" shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索部门名称"
          :prefix-icon="Search"
          clearable
          style="width: 240px"
        />
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px">
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="禁用" />
        </el-select>
        <el-button :icon="Expand" @click="expandAll">全部展开</el-button>
        <el-button :icon="Fold" @click="collapseAll">全部折叠</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate()">新增部门</el-button>
        <span class="hint">提示: 点击行首箭头展开/折叠; 拖拽手柄调整顺序</span>
      </div>

      <el-table
        ref="deptTableRef"
        v-loading="loading"
        :data="flatList"
        row-key="id"
        border
        class="dept-table-wrap"
        empty-text="暂无部门"
        :row-class-name="rowClassName"
      >
        <el-table-column label="部门名称" min-width="320">
          <template #default="{ row }">
            <div class="dept-name-cell" :style="{ paddingLeft: row.level * 24 + 'px' }">
              <el-icon
                v-if="row.hasChildren"
                class="dept-expand-btn"
                :size="14"
                @click.stop="toggleExpand(row)"
              >
                <ArrowDown v-if="expandedIds.has(row.id)" />
                <ArrowRight v-else />
              </el-icon>
              <span v-else class="dept-expand-placeholder" />
              <el-icon class="dept-drag-handle" :size="14" color="#c0c4cc">
                <Sort />
              </el-icon>
              <el-icon
                :size="14"
                :color="row.parentId === 0 ? '#409eff' : '#909399'"
                class="dept-name-icon"
              >
                <OfficeBuilding v-if="row.parentId === 0" />
                <User v-else />
              </el-icon>
              <span :class="{ 'dept-name-top': row.parentId === 0 }">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="排序" prop="sort" width="90" align="center" />

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.status === 1 ? 'success' : 'info'"
              effect="plain"
            >{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="新增子部门" placement="top">
              <el-button link type="primary" :icon="Plus" @click.stop="openCreate(row.id)" />
            </el-tooltip>
            <el-tooltip content="编辑" placement="top">
              <el-button link type="primary" :icon="Edit" @click.stop="openEdit(row)" />
            </el-tooltip>
            <el-tooltip :content="row.status === 1 ? '禁用' : '启用'" placement="top">
              <el-button
                link
                :type="row.status === 1 ? 'warning' : 'success'"
                :icon="row.status === 1 ? CircleClose : Check"
                @click.stop="toggleStatus(row)"
              />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button link type="danger" :icon="Delete" @click.stop="remove(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑部门' : '新增部门'"
      width="560px"
      destroy-on-close
      @open="onDialogOpen"
    >
      <el-form label-width="100px">
        <el-form-item label="父级部门">
          <el-popover
            :visible="parentPopoverVisible"
            placement="bottom-start"
            :width="380"
            trigger="manual"
            :teleported="true"
            popper-class="jc-parent-tree-popover"
            @show="onParentPopoverShow"
            @hide="onParentPopoverHide"
          >
            <template #reference>
              <el-input
                :model-value="parentInputDisplay"
                placeholder="点击选择父级部门"
                readonly
                clearable
                @click="parentPopoverVisible = !parentPopoverVisible"
                @clear="onClearParent"
              >
                <template #prefix>
                  <el-icon><Connection /></el-icon>
                </template>
              </el-input>
            </template>

            <div class="jc-parent-tree-panel">
              <div class="jc-parent-tree-toolbar">
                <el-input
                  v-model="parentTreeKeyword"
                  placeholder="搜索部门名称"
                  :prefix-icon="Search"
                  clearable
                  size="small"
                />
                <el-button-group size="small">
                  <el-button :icon="Expand" @click="expandAllParentTree" />
                  <el-button :icon="Fold" @click="collapseAllParentTree" />
                  <el-button :icon="Refresh" @click="loadParentOptions" />
                </el-button-group>
              </div>
              <div class="jc-parent-tree-body">
                <el-tree
                  ref="parentTreeRef"
                  :data="parentOptions"
                  :props="parentTreeProps"
                  node-key="id"
                  :filter-node-method="filterParentNode"
                  :expand-on-click-node="false"
                  :default-expand-all="true"
                  highlight-current
                  @node-click="onParentNodeClick"
                  @node-dblclick="onParentNodeDblClick"
                >
                  <template #default="{ node, data }">
                    <span class="jc-parent-tree-node">
                      <el-icon :size="14" class="jc-parent-tree-node-icon">
                        <FolderOpened v-if="data.children?.length" />
                        <Document v-else />
                      </el-icon>
                      <span class="jc-parent-tree-node-label">{{ node.label }}</span>
                      <el-tag
                        v-if="data.id === 0"
                        type="warning"
                        size="small"
                        effect="plain"
                      >顶级</el-tag>
                      <el-tag
                        v-else-if="editingId && data.id === editingId"
                        type="info"
                        size="small"
                        effect="plain"
                      >当前</el-tag>
                    </span>
                  </template>
                </el-tree>
              </div>
              <div class="jc-parent-tree-footer">
                <span class="jc-parent-tree-hint">双击节点快速选择 / 点击确认按钮选择</span>
                <div>
                  <el-button size="small" @click="parentPopoverVisible = false">取消</el-button>
                  <el-button size="small" type="primary" @click="confirmParent">确认选择</el-button>
                </div>
              </div>
            </div>
          </el-popover>
        </el-form-item>
        <el-form-item label="部门名称" required>
          <el-input v-model="form.name" placeholder="如: 研发部" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
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
.page-header span {
  color: #6b7280;
  font-size: 13px;
}

.panel-card {
  border-radius: 8px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.hint {
  margin-left: auto;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.dept-table-wrap {
  padding: 4px 0;
  width: 100%;
}

/* 部门名称单元格 */
.dept-name-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-height: 24px;
}
.dept-expand-btn {
  cursor: pointer;
  padding: 2px;
  border-radius: 3px;
  color: var(--el-color-primary);
  flex-shrink: 0;
  transition: background-color 0.15s;
}
.dept-expand-btn:hover {
  background-color: #e6f2ff;
}
.dept-expand-placeholder {
  display: inline-block;
  width: 18px;
  flex-shrink: 0;
}
.dept-drag-handle {
  cursor: move;
  padding: 2px;
  border-radius: 3px;
  transition: background-color 0.15s;
  flex-shrink: 0;
}
.dept-drag-handle:hover {
  background-color: #f3f4f6;
  color: var(--el-color-primary) !important;
}
.dept-name-icon {
  flex-shrink: 0;
}
.dept-name-root,
.dept-name-top {
  font-weight: 600;
  color: #409eff;
  letter-spacing: 0.5px;
}

/* SortableJS 拖拽样式 - 见下方 <style> 全局块 (非 scoped)
 * <p>原因: forceFallback=true 时 drag 元素在 body 下, scoped + :deep() 失效,
 * 必须用全局选择器才能命中. ghost 也在原位置, 同样需全局样式保证生效.
 */

/* 父级部门 popover 面板内容样式 (不依赖 popper z-index) */
.jc-parent-tree-panel {
  display: flex;
  flex-direction: column;
  max-height: 420px;
}
.jc-parent-tree-toolbar {
  display: flex;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  align-items: center;
}
.jc-parent-tree-toolbar .el-input {
  flex: 1;
  min-width: 0;
}
.jc-parent-tree-body {
  flex: 1;
  min-height: 240px;
  max-height: 300px;
  overflow: auto;
  padding: 4px 0;
}
.jc-parent-tree-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.jc-parent-tree-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.jc-parent-tree-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  font-size: 13px;
}
.jc-parent-tree-node-icon {
  color: #c0c4cc;
  flex-shrink: 0;
}
.jc-parent-tree-node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

<!-- =====================================================================
 * 全局样式 (非 scoped): SortableJS 拖拽视觉
 * <p>为什么用非 scoped 块: forceFallback=true 时, drag 元素被挂到 body 下,
 * scoped + :deep() 只能命中组件作用域内的子元素, 无法覆盖 body 子节点.
 * <p>同时 ghost / chosen 应用在原位置 tr (el-table 内), 也用全局类保证 HMR 生效.
 * ===================================================================== -->
<style>
.dept-sortable-ghost {
  /* 源位置: 几乎不可见, 仅保留 1px 虚线轮廓 + 行高, 视觉上"空出" */
  opacity: 0 !important;
  background: #fafbfc !important;
  color: transparent !important;
  border: 1px dashed #c0c4cc !important;
  box-shadow: none !important;
  text-shadow: none !important;
}
.dept-sortable-ghost * {
  visibility: hidden !important;
}
.dept-sortable-chosen {
  /* 鼠标按下选中的源元素 (forceFallback 模式下随 drag 一起浮动) */
  background: #fff !important;
  color: #303133 !important;
  border: 1px solid #409eff !important;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3) !important;
  cursor: grabbing !important;
}
.dept-sortable-drag {
  /* 跟随光标的拖动元素 (forceFallback=true 时在 body 下)
   * 关键: pointer-events: none 让鼠标事件穿透到下层 tr,
   * elementsFromPoint 才能命中目标行 (否则返回 drag 元素本身, 永远命中不了)
   */
  pointer-events: none !important;
  background: #fff !important;
  color: #303133 !important;
  border: 1px solid #409eff !important;
  box-shadow: 0 12px 24px rgba(64, 158, 255, 0.4) !important;
  cursor: grabbing !important;
  z-index: 9999 !important;
  border-radius: 4px;
  font-size: 13px;
}
.dept-sortable-drag * {
  visibility: visible !important;
  color: #303133 !important;
}
.dept-sortable-drag .dept-name-icon,
.dept-sortable-drag .dept-expand-placeholder,
.dept-sortable-drag .dept-drag-handle {
  visibility: visible !important;
}

/* =====================================================================
 * 拖放位置视觉 (图1: 平级蓝线 / 图2: 子级蓝框)
 * <p><b>用户最终需求</b>:
 * <ul>
 *   <li>平级移动: 鼠标在两个行之间的间隙 (或目标行上半部分) → 蓝线 (左对齐目标行)</li>
 *   <li>移动到子级: 鼠标在目标行下半部分 → 目标行整行蓝框 (字段间无蓝线)</li>
 * </ul>
 * <p><b>视觉规则</b>:
 * <ul>
 *   <li>蓝线 .dept-drop-gap-top: 在目标 tr 上边界 2px 蓝色实线, 宽度对齐 tr 内容</li>
 *   <li>蓝框 .dept-drop-target: 整行一个蓝色边框, 字段间不画线</li>
 *   <li>源行 .dept-drop-source: 浅蓝底, 表示"被拖动"</li>
 * </ul>
 * ===================================================================== */

/* 源行: 浅蓝底 (被拖动标识) */
.dept-table-wrap tr.dept-drop-source,
.dept-table-wrap tr.dept-drop-source > .el-table__cell {
  background-color: #f0f7ff !important;
}

/* =====================================================================
 * 间隙蓝线: 用 tr 的 ::before / ::after 伪元素画一条横线, 左对齐
 * <p>tr 不能直接 box-shadow, 改用 td:first-child 的 ::before / ::after 画线.
 * <p>position: relative 让伪元素定位生效.
 * ===================================================================== */
.dept-table-wrap tr.dept-drop-gap-top > .el-table__cell:first-child,
.dept-table-wrap tr.dept-drop-gap-bottom > .el-table__cell:first-child {
  position: relative !important;
}
.dept-table-wrap tr.dept-drop-gap-top > .el-table__cell:first-child::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 3px;
  background: #409eff;
  z-index: 5;
  pointer-events: none;
}
.dept-table-wrap tr.dept-drop-gap-bottom > .el-table__cell:first-child::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: #409eff;
  z-index: 5;
  pointer-events: none;
}

/* 整行浅蓝底 (蓝框模式) */
.dept-table-wrap tr.dept-drop-target,
.dept-table-wrap tr.dept-drop-target > .el-table__cell {
  background-color: #ecf5ff !important;
}

/* 第一个 td: 左 + 上 + 下 边框 */
.dept-table-wrap tr.dept-drop-target > .el-table__cell:first-child {
  box-shadow:
    inset 2px 0 0 0 #409eff,
    inset 0 2px 0 0 #409eff,
    inset 0 -2px 0 0 #409eff !important;
}

/* 中间 td: 仅上 + 下 边框 (字段间无蓝线) */
.dept-table-wrap tr.dept-drop-target > .el-table__cell:not(:first-child):not(:last-child) {
  box-shadow:
    inset 0 2px 0 0 #409eff,
    inset 0 -2px 0 0 #409eff !important;
}

/* 最后一个 td: 右 + 上 + 下 边框 */
.dept-table-wrap tr.dept-drop-target > .el-table__cell:last-child {
  box-shadow:
    inset -2px 0 0 0 #409eff,
    inset 0 2px 0 0 #409eff,
    inset 0 -2px 0 0 #409eff !important;
}

/* 兜底: 弹窗 z-index 一定高于 drag 元素, 防止新增/编辑弹窗被挡住 */
.el-overlay,
.el-overlay-dialog {
  z-index: 99999 !important;
}
.el-dialog {
  z-index: 99999 !important;
}

/* =====================================================================
 * 父级部门选择 popover z-index 兜底
 * <p>关键: popover 必须在 el-dialog 之上, 否则点击"父级部门"输入框时,
 * 弹出的 popper 会被 dialog 的 modal 遮罩挡住.
 * <p>必须放在全局 <style> 块 (非 scoped), 因为 popper 节点被 teleport 到 body 下,
 * 没有 [data-v-xxx] 属性, scoped 选择器不命中.
 * <p>el-dialog z-index 已设为 99999, popper 必须更高.
 * ===================================================================== */
.jc-parent-tree-popover,
.jc-parent-tree-popover.el-popper,
.jc-parent-tree-popover.el-popper.is-light,
.jc-parent-tree-popover.el-popper[data-popper-placement] {
  z-index: 9999999 !important;
}
</style>
