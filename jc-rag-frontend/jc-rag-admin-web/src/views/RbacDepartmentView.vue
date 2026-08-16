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
  Avatar,
  Check,
  CircleClose,
  Connection,
  Delete,
  Document,
  Edit,
  Expand,
  Folder,
  FolderOpened,
  Fold,
  OfficeBuilding,
  Plus,
  Refresh,
  Search,
  Sort,
  UserFilled,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDeptDrag, type DropResult } from '../composables/useDeptDrag'

import {
  assignRbacDepartmentUsers,
  createRbacDepartment,
  deleteRbacDepartment,
  fetchRbacDepartment,
  fetchRbacDepartmentTree,
  fetchRbacDepartmentUserIds,
  fetchRbacDepartmentUsers,
  fetchRbacRootDepartment,
  fetchRbacUserDetail,
  fetchRbacUsers,
  moveRbacDepartment,
  updateRbacCompanyName,
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
type FlatDept = DeptNode & { level: number; hasChildren: boolean; rowType: 'dept' }

/**
 * 部门用户行 (仅开关打开时, 紧随对应部门行下方插入).
 * <p>rowType='user' 用于 el-table column 区分渲染; 单独 className dept-user-row-{id}
 * <p>确保 useHoverHit 不会把它识别为可拖拽目标 (正则 dept-row-(\d+) 不匹配).
 */
type FlatUser = {
  rowType: 'user'
  id: number
  userId: number
  departmentId: number
  nickname: string
  username: string
  gender: number
  level: number
}

type FlatRow = FlatDept | FlatUser

/**
 * 类型守卫: 判断行是否为用户行.
 */
function isUserRow(row: FlatRow): row is FlatUser {
  return (row as FlatUser).rowType === 'user'
}

const loading = ref(false)
const deptTree = ref<DeptNode[]>([])
/** 已展开的部门 id 集合 (点击行内展开/折叠按钮维护) */
const expandedIds = ref<Set<number>>(new Set())

/* =====================================================================
 * 公司名称 (顶级部门 parent_id=0)
 * <p>展示在表格上方, 表格内不显示根节点; 仅有 super_admin / developer 可编辑.
 * ===================================================================== */
/**
 * 公司名称编辑权限: 直接采用后端 root 接口返回的 canEdit 字段
 * <p>避免前端硬编码 username / role 字符串; 后端按 developer / super_admin 决定
 */
const canEditCompanyName = ref(false)
const companyName = ref<string>('')
const companyNameLoading = ref(false)
const companyNameEditing = ref(false)
const companyNameDraft = ref('')

/**
 * 公司根 (parent_id=0) 节点 id; 表格内只隐藏这一个, 其他部门保持原状.
 * <p>- 加载时通过 fetchRbacRootDepartment() 查询, 拿不到 id 就视为还没有公司根, 表格内不隐藏任何节点.
 * <p>- 这避免了历史脏数据 (多 parent_id=0 节点) 时被误删.
 */
const companyRootId = ref<number | null>(null)

/**
 * 统计 parent_id=0 的节点数量 + 收集其名称.
 * <p>当前数据库里"公司根"应当唯一; 多于 1 个视为历史脏数据, 不当作"公司名".
 * <p>用于在"公司名称"行做兜底: 多根时显示"尚未设置"而不是误把第一个根当公司名.
 */
const rootCount = computed(() => {
  let c = 0
  const dfs = (ns: DeptNode[]) => {
    for (const n of ns) {
      if (n.parentId === 0) c++
      if (n.children?.length) dfs(n.children)
    }
  }
  dfs(deptTree.value)
  return c
})

/** 实际展示用的公司名: 仅当 parent_id=0 节点唯一时使用后端返回的名字, 否则空 */
const displayCompanyName = computed(() => (rootCount.value === 1 ? companyName.value : ''))

/** 编辑入口 tooltip 提示: 根据权限与 rootCount 给出准确原因 */
const editCompanyNameTooltip = computed(() => {
  if (!canEditCompanyName.value) return '仅 super_admin / developer 可编辑公司名称'
  if (companyNameLoading.value) return '加载中…'
  if (rootCount.value !== 1) {
    return '当前数据库中顶级 (parent_id=0) 部门不唯一, 不能直接编辑公司名称; 请先在数据层将多个顶级合并为 1 个'
  }
  return '编辑公司名称 (顶级部门)'
})

async function loadCompanyName() {
  companyNameLoading.value = true
  try {
    const data = (await fetchRbacRootDepartment()) as
      | { id: number; name: string; canEdit?: boolean }
      | null
    companyName.value = data?.name ?? ''
    companyRootId.value = data?.id ?? null
    canEditCompanyName.value = data?.canEdit === true
  } catch {
    companyName.value = ''
    companyRootId.value = null
    canEditCompanyName.value = false
  } finally {
    companyNameLoading.value = false
  }
}

function startEditCompanyName() {
  companyNameDraft.value = companyName.value
  companyNameEditing.value = true
}

function cancelEditCompanyName() {
  companyNameEditing.value = false
  companyNameDraft.value = ''
}

async function saveCompanyName() {
  const next = companyNameDraft.value.trim()
  if (!next) {
    ElMessage.warning('公司名称不能为空')
    return
  }
  if (next === companyName.value) {
    companyNameEditing.value = false
    return
  }
  try {
    await updateRbacCompanyName(next)
    companyName.value = next
    companyNameEditing.value = false
    ElMessage.success('公司名称已更新')
    // 顶级部门名称变了, 父级选择 popover 里也要刷新
    await loadParentOptions()
  } catch (e: any) {
    ElMessage.error(e?.message || '更新失败')
  }
}

/**
 * 表格展示用的树: 剥除唯一的根节点 (公司名称, parent_id=0), 把它的 children 上提为根.
 * <p>业务约束: 数据库中顶级节点唯一, 一定是公司名称, 表格里不需要重复展示.
 * <p>这是 <strong>整个系统里唯一隐藏根节点</strong> 的位置; 其他部门树
 * <p>(父级选择 popover / 用户部门下拉 / 用户页左侧部门树) 一律保持原样, 直接展示公司名称根.
 * <p>剥除方式: 当且仅当存在唯一一个 parent_id=0 节点时, 用其 children 替换.
 * <p>若历史脏数据出现多 parent_id=0 节点, 不剥除 (降级为原样展示), 避免误删部门.
 */
const displayTree = computed<DeptNode[]>(() => {
  const root = deptTree.value.length === 1 ? deptTree.value[0] : undefined
  if (root && root.parentId === 0) {
    return root.children ?? []
  }
  return deptTree.value
})

/** 关键字 / 状态过滤后的树 (在 displayTree 基础上过滤) */
const filteredTree = computed(() => applyClientFilter(displayTree.value, keyword.value, statusFilter.value))

/** 扁平化渲染列表: 遍历 filteredTree, 命中节点保留祖先链, 已展开节点的 children 继续展开 */
const flatList = computed<FlatDept[]>(() => flattenForDisplay(filteredTree.value, expandedIds.value))

/* =====================================================================
 * 显示部门用户 (开关 + 缓存 + 行数据混入)
 * <p>- 默认关闭; 打开后每个部门行下插入该部门的直接关联用户行
 * <p>- 数据来源: /api/rbac/user/list-by-department/{deptId} (单级, 不递归)
 * <p>- 用户行 className 用 dept-user-row-{userId}, useHoverHit 不会识别为拖拽目标
 * ===================================================================== */

/** 是否显示部门用户 */
const showDeptUsers = ref(false)

/** 部门用户缓存: Map<deptId, 用户列表>; 关闭开关时清空 */
const deptUsersMap = ref<Map<number, Array<{
  id: number
  username: string
  nickname: string
  gender: number
}>>>(new Map())

/** 用户行加载中标记, 用于 el-table 列表显示 loading 状态 */
const deptUsersLoading = ref(false)

/**
 * 加载指定部门的直接关联用户.
 * <p>单级, 不递归, 命中后端已排除 developer / super_admin.
 * <p>命中缓存直接返回; 失败时记录空列表避免反复请求.
 */
async function loadDeptUsers(deptId: number) {
  if (deptUsersMap.value.has(deptId)) return
  try {
    const users = await fetchRbacDepartmentUsers(deptId)
    deptUsersMap.value.set(deptId, users ?? [])
  } catch {
    deptUsersMap.value.set(deptId, [])
  }
}

/**
 * 开关打开时: 加载所有当前展示的部门 (filteredTree 内) 的用户.
 * <p>关闭时: 清空缓存, 用户行消失.
 */
async function onShowDeptUsersChange(val: boolean) {
  if (!val) {
    deptUsersMap.value = new Map()
    return
  }
  deptUsersLoading.value = true
  try {
    const ids: number[] = []
    const collect = (ns: DeptNode[]) => {
      for (const n of ns) {
        ids.push(n.id)
        if (n.children?.length) collect(n.children)
      }
    }
    collect(filteredTree.value)
    await Promise.all(ids.map((id) => loadDeptUsers(id)))
  } finally {
    deptUsersLoading.value = false
  }
}

/**
 * 部门行下方拼接用户行.
 * <p>仅在 showDeptUsers=true 时混入; level 比父部门多 1, 视觉上与子部门对齐.
 * <p>折叠逻辑: 部门被用户主动折叠 (expandedIds 不包含) 时, 其用户行也一起折叠,
 * <p>保持"部门 ↔ 用户"视觉绑定, 避免出现"部门收起但用户还挂着"的孤立行.
 */
const flatListWithUsers = computed<FlatRow[]>(() => {
  if (!showDeptUsers.value) return flatList.value
  const out: FlatRow[] = []
  for (const dept of flatList.value) {
    out.push(dept)
    // 仅展开状态的部门才显示其直接关联用户 (叶子部门 expandedIds 默认包含, 永远显示)
    if (!expandedIds.value.has(dept.id)) continue
    const users = deptUsersMap.value.get(dept.id) ?? []
    for (const u of users) {
      out.push({
        rowType: 'user',
        id: u.id,
        userId: u.id,
        departmentId: dept.id,
        nickname: u.nickname || u.username,
        username: u.username,
        gender: u.gender ?? 0,
        level: dept.level + 1,
      })
    }
  }
  return out
})

/* =====================================================================
 * 用户详情弹窗 (点部门树内用户行 → 弹窗显示详情)
 * ===================================================================== */
const userDetailDialogVisible = ref(false)
const userDetailLoading = ref(false)
const userDetail = ref<any | null>(null)

/**
 * 打开用户详情弹窗.
 * <p>调用 GET /api/rbac/user/{id}/detail 拉取最新数据; 失败弹 toast.
 */
async function openUserDetail(userRow: FlatUser) {
  userDetail.value = null
  userDetailDialogVisible.value = true
  userDetailLoading.value = true
  try {
    const data = await fetchRbacUserDetail(userRow.userId)
    userDetail.value = data
  } catch (e: any) {
    ElMessage.error(e?.message || '用户详情加载失败')
    userDetailDialogVisible.value = false
  } finally {
    userDetailLoading.value = false
  }
}

function closeUserDetail() {
  userDetailDialogVisible.value = false
  userDetail.value = null
}

/**
 * 性别 icon: 男蓝 / 女粉 / 未知灰 — 统一使用 Avatar (小圆脑袋+半个身子).
 * <p>颜色由 genderIconColor 决定, 与性别一一对应.
 */
function renderGenderIcon(_g: number) {
  return Avatar
}

/**
 * 性别 icon 颜色 (马卡龙色, 蓝/粉色调不变, 整体提亮).
 * <p>男: 极浅蓝 (#B3D8FF, macaron sky blue)
 * <p>女: 极浅粉 (#FFB3C6, macaron rose)
 * <p>未知: 极浅灰 (#DCDFE6)
 */
function genderIconColor(g: number) {
  if (g === 1) return '#B3D8FF'
  if (g === 2) return '#FFB3C6'
  return '#DCDFE6'
}

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
  // 收集所有部门 id (含叶子节点).
  // <p>为什么叶子也要收集:
  // <p>  - 叶子节点没有子级, 展开"子级"对它无意义, 但 flatListWithUsers
  // <p>    用 expandedIds.has(dept.id) 决定是否插入用户行, 叶子部门若不在
  // <p>    expandedIds 里, 它的直接关联用户就不会显示.
  // <p>  - 收集叶子节点后, 默认所有部门都在 expandedIds 中, 展开时用户行会显示.
  // <p>  - 用户无法 toggle 叶子 (toggleExpand 里 hasChildren=false 直接 return),
  // <p>    所以叶子一旦加入就不会被移除, 不会影响"全部折叠"按钮.
  const out: number[] = []
  const dfs = (arr: DeptNode[]) => {
    for (const n of arr) {
      out.push(n.id)
      if (n.children?.length) dfs(n.children)
    }
  }
  dfs(nodes)
  return out
}

async function loadParentOptions() {
  // 部门树根节点就是唯一的「公司名称」, 不再包装"顶级 (无上级)"假节点
  parentOptions.value = (await fetchRbacDepartmentTree()) ?? []
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
      rowType: 'dept',
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

function toggleExpandById(id: number) {
  const next = new Set(expandedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedIds.value = next
}

function toggleExpand(row: FlatDept) {
  if (!row.hasChildren) return
  toggleExpandById(row.id)
}

/**
 * el-table row-class-name: 给每行加 dept-row-{id} class,
 * <p>用于拖拽时通过 className 反查部门 id (SortableJS evt.item 没有 data-*).
 * <p>用户行使用 dept-user-row-{userId}, useHoverHit 不会匹配, 不会成为拖拽目标.
 */
function rowClassName({ row }: { row: FlatRow }): string {
  if (isUserRow(row)) return `dept-user-row dept-user-row-${row.userId}`
  return `dept-row dept-row-${row.id}`
}

function expandAll() {
  expandedIds.value = new Set(collectAllIds(deptTree.value))
}

function collapseAll() {
  expandedIds.value = new Set()
}

/** 是否全部展开 (用于切换按钮的图标与 tooltip) */
const allExpanded = computed(() => {
  const all = collectAllIds(deptTree.value)
  if (all.length === 0) return false
  return all.every((id) => expandedIds.value.has(id))
})

/** 切换全部展开/折叠 (合并为单个按钮) */
function toggleExpandAll() {
  if (allExpanded.value) collapseAll()
  else expandAll()
}

/* =====================================================================
 * CRUD
 * ===================================================================== */

/**
 * 打开新增部门弹窗.
 * <p>默认挂在公司名称 (唯一根节点) 下, 而不是 parent_id=0.
 * <p>先 await loadCompanyName() 确保 companyRootId 已加载, 避免用户首次打开时拿到 null.
 */
async function openCreate(parentId?: number) {
  await loadCompanyName()
  editingId.value = null
  const defaultParentId = parentId ?? companyRootId.value ?? 0
  Object.assign(form, { parentId: defaultParentId, name: '', sort: 0, status: 1, remark: '' })
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
 * 分配用户 (部门 -> 用户) 弹窗
 * <p>左侧: 候选用户列表 (按关键字懒加载, 排除 developer / super_admin);
 * <p>右侧: 已选用户 chips; 确认后覆盖式保存.
 * ===================================================================== */
type AssignUser = {
  id: number
  username: string
  nickname: string
  userType?: string
  userTypeText?: string
}

const assignUsersDialogVisible = ref(false)
const assignUsersDialogDeptId = ref<number | null>(null)
const assignUsersDialogDeptName = ref('')
const assignUsersDialogTitle = computed(
  () => `分配用户 - ${assignUsersDialogDeptName.value || ''}`,
)

const candidateUsers = ref<AssignUser[]>([])
const candidatePage = ref(1)
const candidatePageSize = 20
const candidateTotal = ref(0)
const candidateLoadMore = ref(false)
const assignUsersLoading = ref(false)
const assignUserSearch = ref('')
let assignUserSearchTimer: number | null = null

/** 已选用户 id 列表 (本次会话内编辑) */
const assignedDraftIds = ref<number[]>([])
const selectedUserIdsSet = computed(() => new Set(assignedDraftIds.value))

/** 已选用户的展示信息 (从 candidateUsers + 初始回显合并) */
const assignedDraftItems = computed<AssignUser[]>(() => {
  const map = new Map<number, AssignUser>()
  for (const u of candidateUsers.value) map.set(u.id, u)
  return assignedDraftIds.value
    .map((id) => map.get(id))
    .filter((u): u is AssignUser => Boolean(u))
})

const assignUsersSaving = ref(false)

async function openAssignUsers(row: FlatDept) {
  assignUsersDialogDeptId.value = row.id
  assignUsersDialogDeptName.value = row.name
  // 重置状态
  candidateUsers.value = []
  candidatePage.value = 1
  candidateTotal.value = 0
  assignUserSearch.value = ''
  assignedDraftIds.value = []
  // 回显已分配 (含子部门后代的用户)
  try {
    const ids = await fetchRbacDepartmentUserIds(row.id)
    assignedDraftIds.value = ids
  } catch {
    assignedDraftIds.value = []
  }
  assignUsersDialogVisible.value = true
}

function onAssignUsersDialogOpened() {
  // 弹窗挂载完后再加载候选, 避免初次列表空
  loadMoreCandidates()
}

async function loadMoreCandidates() {
  if (assignUsersLoading.value) return
  assignUsersLoading.value = true
  try {
    const res: any = await fetchRbacUsers({
      page: candidatePage.value,
      size: candidatePageSize,
      keyword: assignUserSearch.value || undefined,
    })
    const records: any[] = res.records ?? []
    // 排除 developer / super_admin (系统保留)
    const filtered = records.filter(
      (u) =>
        u.username !== 'developer' &&
        u.username !== 'super_admin' &&
        String(u.userType).toUpperCase() !== 'DEVELOPER',
    )
    if (candidatePage.value === 1) {
      candidateUsers.value = filtered.map(toAssignUser)
    } else {
      // 追加, 去重
      const seen = new Set(candidateUsers.value.map((u) => u.id))
      for (const u of filtered) {
        if (!seen.has(u.id)) candidateUsers.value.push(toAssignUser(u))
      }
    }
    candidateTotal.value = res.total ?? 0
    candidateLoadMore.value = candidateUsers.value.length < candidateTotal.value
  } catch {
    /* 错误由请求层处理 */
  } finally {
    assignUsersLoading.value = false
  }
}

function toAssignUser(u: any): AssignUser {
  return {
    id: u.id,
    username: u.username,
    nickname: u.nickname ?? '',
    userType: u.userType,
    userTypeText: u.userTypeText,
  }
}

function onAssignUserSearchInput() {
  if (assignUserSearchTimer) window.clearTimeout(assignUserSearchTimer)
  assignUserSearchTimer = window.setTimeout(() => {
    candidatePage.value = 1
    candidateUsers.value = []
    loadMoreCandidates()
  }, 250)
}

function toggleAssignUser(u: AssignUser) {
  const set = new Set(assignedDraftIds.value)
  if (set.has(u.id)) set.delete(u.id)
  else set.add(u.id)
  assignedDraftIds.value = Array.from(set)
}

function onAssignUserCheckChange(u: AssignUser, val: boolean) {
  const set = new Set(assignedDraftIds.value)
  if (val) set.add(u.id)
  else set.delete(u.id)
  assignedDraftIds.value = Array.from(set)
}

function removeAssignUser(uid: number) {
  assignedDraftIds.value = assignedDraftIds.value.filter((id) => id !== uid)
}

function clearAssignSelection() {
  assignedDraftIds.value = []
}

async function submitAssignUsers() {
  if (assignUsersDialogDeptId.value == null) return
  assignUsersSaving.value = true
  try {
    await assignRbacDepartmentUsers(assignUsersDialogDeptId.value, assignedDraftIds.value)
    ElMessage.success('分配成功')
    assignUsersDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    assignUsersSaving.value = false
  }
}

/* =====================================================================
 * 行拖拽交互层 (复用现有 SortableJS + move API)
 * <p>仅重构 Drag Interaction Layer, 不改动部门树数据结构 / 保存逻辑 / 接口协议.
 * ===================================================================== */

async function handleDeptDrop(result: DropResult) {
  const dragged = flatList.value.find((item) => item.id === result.draggedId)
  if (!dragged) {
    await loadDeptTree()
    return
  }
  try {
    await moveRbacDepartment(result.draggedId, {
      targetId: result.targetId,
      position: result.position,
    })
    ElMessage.success(
      result.position === 'inner'
        ? `已将 [${dragged.name}] 移入目标部门下`
        : `已将 [${dragged.name}] 移动到目标位置 (同级)`
    )
  } catch (e: any) {
    ElMessage.error(e?.message || '移动失败')
  } finally {
    await loadDeptTree()
    await loadParentOptions()
  }
}

const { dragState, initSortable, destroySortable } = useDeptDrag({
  getFlatList: () => flatList.value,
  getExpandedIds: () => expandedIds.value,
  toggleExpand: toggleExpandById,
  isDescendantOrSelf,
  deptTree: () => deptTree.value,
  onDrop: handleDeptDrop,
})

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

watch(flatListWithUsers, () => {
  // 列表变化 (展开/折叠/过滤/数据更新/用户行混入) 后, 重新初始化 SortableJS
  if (dragState.value.dragging) return
  nextTick(() => initSortable())
})

/* =====================================================================
 * 生命周期
 * ===================================================================== */

onMounted(async () => {
  await loadCompanyName()
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
      </div>
    </header>

    <el-card class="panel-card" shadow="never">
      <!--
        工具栏 + 公司名称合并为同一行:
        <p>左侧: 公司名称 + 编辑入口 (icon 按钮, hover 可见)
        <p>右侧: 关键字搜索 / 状态过滤 / 展开折叠切换 / 新增
        <p>分两段用竖线分隔, 保持一定距离
      -->
      <div class="toolbar-merged">
        <div class="toolbar-merged__left">
          <el-icon class="company-name-icon" :size="18" color="#409eff">
            <OfficeBuilding />
          </el-icon>
          <template v-if="companyNameEditing">
            <el-input
              v-model="companyNameDraft"
              size="default"
              class="company-name-input"
              placeholder="请输入公司名称"
              maxlength="50"
              show-word-limit
              @keyup.enter="saveCompanyName"
              @keyup.esc="cancelEditCompanyName"
            />
            <el-button type="primary" :icon="Check" @click="saveCompanyName">保存</el-button>
            <el-button :icon="CircleClose" @click="cancelEditCompanyName">取消</el-button>
          </template>
          <template v-else class="company-name-display">
            <span v-if="companyNameLoading" class="company-name-value text-muted">加载中…</span>
            <span v-else-if="displayCompanyName" class="company-name-value">{{ displayCompanyName }}</span>
            <span v-else class="company-name-value text-muted">尚未设置</span>
            <span class="company-name-edit-trigger">
              <el-tooltip
                :content="editCompanyNameTooltip"
                placement="top"
              >
                <el-button
                  link
                  type="primary"
                  :icon="Edit"
                  class="company-name-edit-btn"
                  :disabled="!canEditCompanyName || companyNameLoading || rootCount !== 1"
                  @click="startEditCompanyName"
                />
              </el-tooltip>
            </span>
          </template>
          <!--
            显示部门用户开关 (与公司名称同行, 紧跟编辑入口右侧)
            <p>默认关闭; 打开后每个部门行下插入该部门直接关联的用户行
          -->
          <el-switch
            v-model="showDeptUsers"
            class="dept-users-switch"
            active-text="显示部门用户"
            inactive-text="隐藏部门用户"
            inline-prompt
            @change="onShowDeptUsersChange"
          />
        </div>

        <div class="toolbar-merged__divider" />

        <div class="toolbar-merged__right">
          <el-input
            v-model="keyword"
            placeholder="搜索部门名称"
            :prefix-icon="Search"
            clearable
            style="width: 220px"
          />
          <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 110px">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
          <el-tooltip
            :content="allExpanded ? '全部折叠' : '全部展开'"
            placement="top"
          >
            <el-button
              :icon="allExpanded ? Fold : Expand"
              @click="toggleExpandAll"
            />
          </el-tooltip>
          <el-button type="primary" :icon="Plus" @click="openCreate()">新增部门</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading || deptUsersLoading"
        :data="flatListWithUsers"
        row-key="id"
        border
        class="dept-table-wrap"
        empty-text="暂无部门"
        :row-class-name="rowClassName"
      >
        <el-table-column label="部门名称 / 用户" min-width="360">
          <template #default="{ row }">
            <!--
              用户行: 性别 icon + nickname, 整行可点击打开详情.
              <p>无 drag handle, 无展开按钮, 浅灰背景, 视觉上明显区别于部门行.
              <p>padding-left = row.level * 24 + 44 (44 = expand-btn 18 + gap 6 + drag-handle 14 + gap 6),
              <p>与子部门行 "expand-btn 之后的内容 (Folder icon)" 起点对齐 (统一缩进粒度 = 1 级 24px).
            -->
            <div
              v-if="isUserRow(row)"
              class="dept-user-cell"
              :style="{ paddingLeft: row.level * 24 + 44 + 'px' }"
              @click="openUserDetail(row)"
            >
              <el-icon
                class="dept-user-gender"
                :size="14"
                :color="genderIconColor(row.gender)"
              >
                <component :is="renderGenderIcon(row.gender)" />
              </el-icon>
              <span class="dept-user-nickname">{{ row.nickname }}</span>
              <span class="dept-user-username">@{{ row.username }}</span>
              <el-tag size="small" effect="plain" type="info" class="dept-user-tag">用户</el-tag>
            </div>
            <!-- 部门行: 顶级 OfficeBuilding 蓝 / 普通 Folder 绿; 与用户行视觉分离 -->
            <div
              v-else
              class="dept-name-cell"
              :style="{ paddingLeft: row.level * 24 + 'px' }"
              @click="toggleExpand(row)"
            >
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
                :size="15"
                :color="row.parentId === 0 ? '#409eff' : '#67c23a'"
                class="dept-name-icon"
              >
                <OfficeBuilding v-if="row.parentId === 0" />
                <Folder v-else />
              </el-icon>
              <span :class="{ 'dept-name-top': row.parentId === 0 }">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="排序" prop="sort" width="90" align="center">
          <template #default="{ row }">
            <span v-if="!isUserRow(row)">{{ row.sort }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="!isUserRow(row)"
              size="small"
              :type="row.status === 1 ? 'success' : 'info'"
              effect="plain"
            >{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">
            <span v-if="!isUserRow(row)">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="290" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="!isUserRow(row)">
              <el-tooltip content="新增子部门" placement="top">
                <el-button link type="primary" :icon="Plus" @click.stop="openCreate(row.id)" />
              </el-tooltip>
              <el-tooltip content="编辑" placement="top">
                <el-button link type="primary" :icon="Edit" @click.stop="openEdit(row)" />
              </el-tooltip>
              <el-tooltip content="分配用户" placement="top">
                <el-button link type="success" :icon="UserFilled" @click.stop="openAssignUsers(row)" />
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
            <template v-else>
              <el-tooltip content="查看用户详情" placement="top">
                <el-button link type="primary" :icon="UserFilled" @click.stop="openUserDetail(row)" />
              </el-tooltip>
            </template>
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

    <!--
      分配用户弹窗 (与角色分配用户同款布局).
      <p>左侧: 可搜索用户列表 (排除 developer / super_admin).
      <p>右侧: 已选用户 chips, 移除即取消选择.
      <p>底部: 确认覆盖式保存到后端.
    -->
    <el-dialog
      v-model="assignUsersDialogVisible"
      :title="assignUsersDialogTitle"
      width="880px"
      destroy-on-close
      @opened="onAssignUsersDialogOpened"
    >
      <div class="assign-users-layout">
        <!-- 左侧: 可选用户 (含搜索) -->
        <div class="assign-users-left">
          <el-input
            v-model="assignUserSearch"
            placeholder="搜索用户名 / 昵称"
            :prefix-icon="Search"
            clearable
            size="default"
            class="assign-users-search"
            @input="onAssignUserSearchInput"
          />
          <div v-loading="assignUsersLoading" class="assign-users-list-wrap">
            <template v-if="candidateUsers.length > 0">
              <div
                v-for="u in candidateUsers"
                :key="u.id"
                class="assign-user-item"
                :class="{ 'is-selected': selectedUserIdsSet.has(u.id) }"
                @click="toggleAssignUser(u)"
              >
                <el-checkbox
                  :model-value="selectedUserIdsSet.has(u.id)"
                  @change="(val: boolean) => onAssignUserCheckChange(u, val)"
                  @click.stop
                />
                <div class="assign-user-info">
                  <span class="assign-user-name">{{ u.nickname || u.username }}</span>
                  <span class="assign-user-username">@{{ u.username }}</span>
                  <el-tag v-if="u.userType" size="small" effect="plain" class="assign-user-type">
                    {{ u.userTypeText || u.userType }}
                  </el-tag>
                </div>
              </div>
              <div v-if="candidateLoadMore" class="assign-users-loadmore">
                <el-button link type="primary" :loading="assignUsersLoading" @click="loadMoreCandidates">
                  加载更多
                </el-button>
              </div>
            </template>
            <el-empty v-else-if="!assignUsersLoading" description="无可选用户" :image-size="60" />
          </div>
        </div>

        <!-- 右侧: 已选用户 chips -->
        <div class="assign-users-right">
          <div class="assign-users-right-header">
            <span>已选 ({{ assignedDraftIds.length }})</span>
            <el-button
              v-if="assignedDraftIds.length > 0"
              link
              type="danger"
              size="small"
              @click="clearAssignSelection"
            >清空</el-button>
          </div>
          <div class="assign-users-chips">
            <template v-if="assignedDraftItems.length > 0">
              <el-tag
                v-for="u in assignedDraftItems"
                :key="u.id"
 closable
                class="assign-user-chip"
                @close="removeAssignUser(u.id)"
              >
                {{ u.nickname || u.username }}
                <span class="assign-user-chip-username">@{{ u.username }}</span>
              </el-tag>
            </template>
            <el-empty v-else description="尚未选择用户" :image-size="60" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="assignUsersDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignUsersSaving" @click="submitAssignUsers">保存</el-button>
      </template>
    </el-dialog>
    <!--
      用户详情弹窗 (点部门树内用户行 / 操作列 UserFilled 按钮)
      <p>展示用户只读信息: 账号/昵称/性别/手机/邮箱/部门/状态/类型/角色/最后登录/创建时间
    -->
    <el-dialog
      v-model="userDetailDialogVisible"
      title="用户详情"
      width="560px"
      destroy-on-close
      @close="closeUserDetail"
    >
      <div v-loading="userDetailLoading" class="user-detail-wrap">
        <template v-if="userDetail">
          <div class="user-detail-header">
            <el-icon :size="28" :color="genderIconColor(userDetail.gender ?? 0)">
              <component :is="renderGenderIcon(userDetail.gender ?? 0)" />
            </el-icon>
            <div class="user-detail-title">
              <span class="user-detail-nickname">{{ userDetail.nickname || userDetail.username }}</span>
              <span class="user-detail-username">@{{ userDetail.username }}</span>
            </div>
          </div>
          <el-descriptions :column="2" border size="default" class="user-detail-descs">
            <el-descriptions-item label="账号">{{ userDetail.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ userDetail.nickname || '—' }}</el-descriptions-item>
            <el-descriptions-item label="真实姓名">{{ userDetail.realName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="性别">
              {{ userDetail.genderText || '未知' }}
            </el-descriptions-item>
            <el-descriptions-item label="手机">{{ userDetail.phone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ userDetail.email || '—' }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ userDetail.departmentName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag
                size="small"
                :type="userDetail.status === 1 ? 'success' : 'info'"
                effect="plain"
              >{{ userDetail.statusText || (userDetail.status === 1 ? '启用' : '禁用') }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="类型">
              {{ userDetail.userTypeText || userDetail.userType || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="角色" :span="2">
              <template v-if="userDetail.roleNames && userDetail.roleNames.length > 0">
                <el-tag
                  v-for="rn in userDetail.roleNames"
                  :key="rn"
                  size="small"
                  effect="plain"
                  type="info"
                  class="user-detail-role-tag"
                >{{ rn }}</el-tag>
              </template>
              <span v-else>—</span>
            </el-descriptions-item>
            <el-descriptions-item label="最后登录">{{ formatDate(userDetail.lastLoginTime) }}</el-descriptions-item>
            <el-descriptions-item label="最后登录 IP">{{ userDetail.lastLoginIp || '—' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ formatDate(userDetail.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else-if="!userDetailLoading" description="暂无数据" :image-size="60" />
      </div>
      <template #footer>
        <el-button @click="closeUserDetail">关闭</el-button>
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

/* =====================================================================
 * 公司名称行 (顶级部门, 表格上方单独展示)
 * <p>与表格视觉一致, 但不进入树形表格, 仅一行卡片.
 * ===================================================================== */
/* 公司名称 + 工具栏合并为同一行 */
.toolbar-merged {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #f0f7ff 0%, #e6f2ff 100%);
  border: 1px solid #d6e8ff;
  border-radius: 6px;
  min-height: 48px;
  flex-wrap: nowrap;
}
.toolbar-merged__left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1 1 auto;
  min-width: 0;
}
.toolbar-merged__divider {
  width: 1px;
  height: 22px;
  background: #dcdfe6;
  flex-shrink: 0;
}
.toolbar-merged__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.company-name-icon {
  flex-shrink: 0;
}
.company-name-label {
  font-size: 16px;
  color: #1f2937;
  font-weight: 600;
  flex-shrink: 0;
  letter-spacing: 0.3px;
}
.company-name-value {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  letter-spacing: 0.3px;
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.company-name-input {
  flex: 1 1 auto;
  max-width: 320px;
}
/* 编辑按钮: 紧贴公司名称值, 只保留 4px 距离; hover 公司名称左半段时显现 */
.company-name-edit-trigger {
  opacity: 0;
  transition: opacity 0.2s ease;
  flex-shrink: 0;
  margin-left: 4px;
}
.toolbar-merged__left:hover .company-name-edit-trigger {
  opacity: 1;
}
/* disabled 状态在 hover 时也保持半透明, 避免误以为可点击 */
.company-name-edit-btn.is-disabled,
.company-name-edit-btn:disabled {
  cursor: not-allowed;
}
.text-muted {
  color: #9ca3af;
  font-weight: 400;
}

/* =====================================================================
 * 显示部门用户开关
 * ===================================================================== */
.dept-users-switch {
  margin-left: 16px;
  flex-shrink: 0;
  --el-switch-on-color: #409eff;
}
.dept-users-switch :deep(.el-switch__core) {
  min-width: 50px;
}

/* =====================================================================
 * 部门树内用户行
 * <p>视觉上区别于部门行: 浅灰底 + 性别 icon (Male 蓝 / Female 粉 / User 灰) + 缩小字号
 * <p>部门行: 顶部白底 + Folder 绿/OfficeBuilding 蓝
 * ===================================================================== */
.dept-user-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-height: 26px;
  cursor: pointer;
  padding-right: 8px;
  border-radius: 4px;
  transition: background-color 0.15s;
}
.dept-user-cell:hover {
  background-color: #eef5ff;
}
.dept-user-gender {
  flex-shrink: 0;
}
.dept-user-nickname {
  font-size: 13px;
  color: #4b5563;
  font-weight: 400;
}
.dept-user-username {
  font-size: 12px;
  color: #9ca3af;
}
.dept-user-tag {
  margin-left: 4px;
  height: 18px;
  line-height: 17px;
  padding: 0 6px;
  font-size: 11px;
}
/* 用户行: 整行浅灰底, hover 浅蓝 */
.dept-table-wrap tr.dept-user-row > .el-table__cell {
  background-color: #f5f7fa !important;
}
.dept-table-wrap tr.dept-user-row:hover > .el-table__cell {
  background-color: #eef5ff !important;
}

/* 部门行文字 (深色 + 加粗, 区别于用户) */
.dept-name-cell .dept-name-icon {
  margin: 0 2px;
}
.dept-name-cell > span:not(.dept-expand-placeholder) {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
}

/* =====================================================================
 * 用户详情弹窗
 * ===================================================================== */
.user-detail-wrap {
  min-height: 220px;
}
.user-detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 4px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 12px;
}
.user-detail-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.user-detail-nickname {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  letter-spacing: 0.3px;
}
.user-detail-username {
  font-size: 12px;
  color: #9ca3af;
}
.user-detail-descs {
  margin-top: 4px;
}
.user-detail-role-tag {
  margin-right: 4px;
  margin-bottom: 2px;
}

/* =====================================================================
 * 分配用户弹窗 (左选右显)
 * ===================================================================== */
.assign-users-layout {
  display: flex;
  gap: 16px;
  min-height: 480px;
}
.assign-users-left {
  flex: 1.4;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  overflow: hidden;
}
.assign-users-search {
  margin: 8px 8px 4px;
  flex-shrink: 0;
}
.assign-users-list-wrap {
  flex: 1;
  overflow: auto;
  padding: 4px 8px 8px;
}
.assign-user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}
.assign-user-item:hover {
  background: #f5f7fa;
}
.assign-user-item.is-selected {
  background: #ecf5ff;
}
.assign-user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}
.assign-user-name {
  font-size: 13px;
  color: #1f2937;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.assign-user-username {
  font-size: 12px;
  color: #9ca3af;
}
.assign-user-type {
  margin-left: 4px;
}
.assign-users-loadmore {
  text-align: center;
  padding: 8px 0;
}
.assign-users-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  overflow: hidden;
}
.assign-users-right-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: #fafbfc;
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
}
.assign-users-chips {
  flex: 1;
  overflow: auto;
  padding: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-content: flex-start;
}
.assign-user-chip {
  margin: 0;
}
.assign-user-chip-username {
  margin-left: 4px;
  font-size: 11px;
  color: #909399;
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
 * 拖拽交互层视觉
 * <p>目标: 保留现有拖拽卡片样式, 将落点指示器独立渲染到 body wrapper 内,
 * 实现 before/after 蓝线、inside 高亮与 hover 反馈.
 * ===================================================================== */
.dept-table-wrap .el-table__body-wrapper {
  position: relative;
}
.dept-table-wrap tr.dept-drop-source > .el-table__cell,
.dept-table-wrap tr.dept-drop-source .dept-name-cell {
  background-color: #f0f7ff !important;
}
.dept-table-wrap tr.dept-hover-target .dept-name-cell {
  background: #f5f8ff;
  border-radius: 6px;
  transition:
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}
.jc-dept-indicator-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 8;
}
.jc-dept-indicator-line,
.jc-dept-indicator-box {
  position: absolute;
  opacity: 0;
  will-change: transform, width, height, opacity;
  transition:
    transform 0.18s ease,
    width 0.18s ease,
    height 0.18s ease,
    opacity 0.16s ease;
}
.jc-dept-indicator-line {
  height: 2px;
  border-radius: 999px;
  background: #1677ff;
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.08);
}
.jc-dept-indicator-box {
  border: 1px solid #1677ff;
  border-radius: 8px;
  background: rgba(22, 119, 255, 0.08);
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.06);
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
