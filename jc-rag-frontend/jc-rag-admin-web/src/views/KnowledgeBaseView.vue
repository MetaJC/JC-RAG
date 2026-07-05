<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, QuestionFilled, Refresh } from '@element-plus/icons-vue'

import {
  clearSystemDefaultKb,
  createKnowledgeBase,
  deleteKnowledgeBase,
  deleteKnowledgeDocument,
  fetchKnowledgeBases,
  fetchKnowledgeDocuments,
  fetchKnowledgeUploadPolicy,
  fetchDocumentParseProgress,
  cancelDocumentParse,
  getDocumentFileUrl,
  reprocessKnowledgeDocument,
  setSystemDefaultKb,
  updateKnowledgeBase,
  updateKnowledgeDocument,
  uploadKnowledgeDocuments,
  fetchSystemResources,
  fetchSystemDefaultKb,
} from '../api/knowledge'
import { useAuthStore } from '../stores/auth'
import type { KnowledgeBaseItem, KnowledgeDocumentItem, KnowledgeUploadPolicy } from '../types/knowledge'
import type { DocumentParseProgress } from '../types/system'

const router = useRouter()
const authStore = useAuthStore()
const currentUsername = computed(() => authStore.user?.username || '')
const isDeveloper = computed(() => currentUsername.value.toLowerCase() === 'developer')
/** 超管判定: 对齐后端 RbacDefaultsProperties.superAdminUsername (默认 'admin') */
const isSuperAdmin = computed(() => {
  const u = currentUsername.value.toLowerCase()
  return u === 'admin' || u === 'super_admin'
})

const loading = ref(false)
const uploading = ref(false)
const creating = ref(false)
const knowledgeBases = ref<KnowledgeBaseItem[]>([])
const documents = ref<KnowledgeDocumentItem[]>([])
const KB_SELECTED_ID_KEY = 'kb-selected-id'
const KB_ORDER_KEY = 'kb-order'

const selectedKbId = ref<number | null>(Number(localStorage.getItem(KB_SELECTED_ID_KEY)) || null)
const uploadFiles = ref<File[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)
const dragActive = ref(false)
const uploadProgress = ref(0)
const uploadStatusText = ref('')
const reprocessingDocumentIds = ref<number[]>([])
const editKbVisible = ref(false)
const createKbVisible = ref(false)
const editingKbId = ref<number | null>(null)
const uploadPolicy = reactive<KnowledgeUploadPolicy>({
  maxFileSizeBytes: 0,
  maxRequestSizeBytes: 0,
  maxFileSizeLabel: '',
  maxRequestSizeLabel: '',
  allowedExtensions: [],
  accept: '',
  parseTimeoutSeconds: 0,
})
const editKbForm = reactive({
  kbName: '',
  kbDesc: '',
  visibilityScope: 'PRIVATE',
})
const settingDefault = ref(false)
const systemDefaultKb = ref<KnowledgeBaseItem | null>(null)

/** 「允许客户端选择公开知识库」开关的提示文案 */
const clientSelectKbTip = computed(() => {
  if (!hasPublicKb.value) {
    return '需要至少一个「公开」可见范围的知识库才能开启此开关'
  }
  return '开启后, C 端用户可在客户端自行选择公开知识库; 此时将清除系统默认知识库. 关闭时必须先设置一个默认知识库.'
})

/** 是否存在「公开」可见范围的知识库 (开关可用的前提) */
const hasPublicKb = computed(() => knowledgeBases.value.some((k) => k.visibilityScope === 'PUBLIC'))

/** 当前编辑 KB 是否处于「允许客户端选择」状态 = (没有系统默认 KB) */
const clientSelectKbEnabled = computed(() => !systemDefaultKb.value)

/**
 * 切换「允许客户端选择公开知识库」开关。
 * <p>业务规则: 开启 -> 清除系统默认 KB; 关闭 -> 必须先有系统默认 KB 否则拒绝.
 */
async function onClientSelectKbChange(target: boolean) {
  if (!hasPublicKb.value) return
  if (target) {
    // 开启: 必须清除系统默认 KB
    if (systemDefaultKb.value) {
      try {
        await clearSystemDefaultKb()
        systemDefaultKb.value = null
        knowledgeBases.value = knowledgeBases.value.map((it) => ({ ...it, isDefault: false }))
      } catch (error) {
        console.error(error)
        ElMessage.error('清除默认知识库失败')
      }
    }
  } else {
    // 关闭: 必须存在系统默认 KB
    if (!systemDefaultKb.value) {
      ElMessage.warning('必须先设置一个默认知识库才能关闭此开关')
      return
    }
  }
}

/* ========= 左侧 sidebar 宽度拖动调节 (localStorage 缓存) ========= */
const SIDEBAR_WIDTH_KEY = 'kb-view-sidebar-width'
const SIDEBAR_MIN_PX = 200
const SIDEBAR_MAX_PX = 600
const SIDEBAR_DEFAULT_PX = 280
const sidebarWidth = ref<number>(SIDEBAR_DEFAULT_PX)
const kbLayoutRef = ref<HTMLElement | null>(null)

function loadSidebarWidth() {
  try {
    const raw = localStorage.getItem(SIDEBAR_WIDTH_KEY)
    if (raw) {
      const n = Number(raw)
      if (Number.isFinite(n) && n >= SIDEBAR_MIN_PX && n <= SIDEBAR_MAX_PX) {
        sidebarWidth.value = n
      }
    }
  } catch { /* 忽略 */ }
}

function saveSidebarWidth() {
  try {
    localStorage.setItem(SIDEBAR_WIDTH_KEY, String(sidebarWidth.value))
  } catch { /* 忽略 */ }
}

function onSplitterDown(ev: MouseEvent) {
  ev.preventDefault()
  const layout = kbLayoutRef.value
  if (!layout) return
  const rect = layout.getBoundingClientRect()
  const move = (e: MouseEvent) => {
    const x = e.clientX - rect.left
    const w = Math.min(SIDEBAR_MAX_PX, Math.max(SIDEBAR_MIN_PX, x))
    sidebarWidth.value = w
  }
  const up = () => {
    document.removeEventListener('mousemove', move)
    document.removeEventListener('mouseup', up)
    saveSidebarWidth()
  }
  document.addEventListener('mousemove', move)
  document.addEventListener('mouseup', up)
}

const docSearchKeyword = ref('')
const docSearchStatus = ref('')
const hoveredKbId = ref<number | null>(null)
const dragKbId = ref<number | null>(null)
const dragOverKbId = ref<number | null>(null)
const dropPosition = ref<'before' | 'after'>('after')
const parseDialogVisible = ref(false)
const parseProgress = ref<DocumentParseProgress | null>(null)
const parsePollingTimer = ref<ReturnType<typeof setInterval> | null>(null)
const selectedStageId = ref('')
const userManualSelected = ref(false)
const liveClockOffset = ref(0)
let liveClockTimer: ReturnType<typeof setInterval> | null = null
const parseLogsBodyRef = ref<HTMLElement | null>(null)
const sysResources = ref<any>(null)
let sysResTimer: ReturnType<typeof setInterval> | null = null

const DOC_PAGE_SIZE_KEY = 'kb-doc-page-size'
const docPage = ref(1)
const docSize = ref(Number(localStorage.getItem(DOC_PAGE_SIZE_KEY)) || 10)
const docTotal = ref(0)

const createForm = reactive({
  kbName: '',
  kbDesc: '',
})

const currentKnowledgeBase = computed(() =>
  knowledgeBases.value.find((item) => item.id === selectedKbId.value) ?? null,
)

const kbOrderRef = ref<number[]>([])

function loadKbOrderFromStorage(): number[] {
  try {
    const raw = localStorage.getItem(KB_ORDER_KEY)
    if (raw) return JSON.parse(raw) as number[]
  } catch { /* ignore */ }
  return []
}

function saveKbOrder(ids: number[]) {
  kbOrderRef.value = ids
  localStorage.setItem(KB_ORDER_KEY, JSON.stringify(ids))
}

function syncKbOrderFromList() {
  saveKbOrder(knowledgeBases.value.map((item) => item.id))
}

const sortedKnowledgeBases = computed(() => {
  const order = kbOrderRef.value.length ? kbOrderRef.value : loadKbOrderFromStorage()
  if (!order.length) return knowledgeBases.value
  const orderMap = new Map(order.map((id, idx) => [id, idx]))
  return [...knowledgeBases.value].sort((a, b) => {
    const ai = orderMap.get(a.id)
    const bi = orderMap.get(b.id)
    if (ai !== undefined && bi !== undefined) return ai - bi
    if (ai !== undefined) return -1
    if (bi !== undefined) return 1
    return 0
  })
})

const totalSelectedBytes = computed(() =>
  uploadFiles.value.reduce((sum, file) => sum + file.size, 0),
)
const uploadSummaryText = computed(() => {
  if (!uploadFiles.value.length) {
    return uploadPolicy.maxFileSizeLabel && uploadPolicy.maxRequestSizeLabel
      ? `单文件上限 ${uploadPolicy.maxFileSizeLabel}，单次总量上限 ${uploadPolicy.maxRequestSizeLabel}`
      : '上传限制由服务端配置决定，正在加载当前策略...'
  }
  return `已选择 ${uploadFiles.value.length} 个文件，共 ${formatFileSize(totalSelectedBytes.value)}`
})
const uploadButtonText = computed(() => {
  if (uploading.value) {
    return uploadProgress.value >= 100 ? '服务端处理中...' : '上传中...'
  }
  return '开始上传'
})
let documentPollTimer: number | null = null
let pollingInFlight = false

function normalizeDocumentsResult(result: unknown): KnowledgeDocumentItem[] {
  if (Array.isArray(result)) {
    return result as KnowledgeDocumentItem[]
  }
  const pageResult = result as { records?: KnowledgeDocumentItem[]; total?: number } | null
  const records = pageResult?.records
  const total = pageResult?.total
  if (typeof total === 'number') {
    docTotal.value = total
  }
  return Array.isArray(records) ? records : []
}

function isProcessingStatus(status?: string) {
  return status === 'PENDING' || status === 'PROCESSING'
}

function isClickableStatus(status?: string) {
  return status === 'PROCESSING' || status === 'SUCCESS' || status === 'FAILED'
}

function hasRunningDocuments(items: KnowledgeDocumentItem[] = documents.value) {
  return items.some((item) => isProcessingStatus(item.parseStatus))
}

function isReprocessSubmitting(documentId: number) {
  return reprocessingDocumentIds.value.includes(documentId)
}

function isDocumentBusy(row: KnowledgeDocumentItem) {
  return isProcessingStatus(row.parseStatus) || isReprocessSubmitting(row.id)
}

function stopDocumentPolling() {
  if (documentPollTimer !== null) {
    window.clearInterval(documentPollTimer)
    documentPollTimer = null
  }
}

function syncDocumentPolling(items: KnowledgeDocumentItem[] = documents.value) {
  if (!selectedKbId.value || !hasRunningDocuments(items)) {
    stopDocumentPolling()
    return
  }
  if (documentPollTimer !== null) {
    return
  }
  documentPollTimer = window.setInterval(() => {
    void loadDocuments(true)
  }, 5000)
}

function applyDocuments(items: KnowledgeDocumentItem[]) {
  documents.value = items
  reprocessingDocumentIds.value = reprocessingDocumentIds.value.filter((documentId) =>
    items.some((item) => item.id === documentId && isProcessingStatus(item.parseStatus)),
  )
  syncDocumentPolling(items)
}

async function loadDocuments(silent = false) {
  if (!selectedKbId.value) {
    applyDocuments([])
    return
  }
  if (silent && pollingInFlight) {
    return
  }
  if (silent) {
    pollingInFlight = true
  } else {
    loading.value = true
  }
  try {
    const params: Record<string, unknown> = { page: docPage.value, size: docSize.value, keyword: docSearchKeyword.value || undefined, parseStatus: docSearchStatus.value || undefined }
    const docsResult = await fetchKnowledgeDocuments(selectedKbId.value, params)
    applyDocuments(normalizeDocumentsResult(docsResult))
  } finally {
    if (silent) {
      pollingInFlight = false
    } else {
      loading.value = false
    }
  }
}

function onDocSearch() {
  docPage.value = 1
  loadDocuments()
}

function onDocSearchReset() {
  docSearchKeyword.value = ''
  docSearchStatus.value = ''
  docPage.value = 1
  loadDocuments()
}

function onDocPageChange(page: number) {
  docPage.value = page
  loadDocuments()
}

function onDocSizeChange(size: number) {
  docSize.value = size
  docPage.value = 1
  localStorage.setItem(DOC_PAGE_SIZE_KEY, String(size))
  loadDocuments()
}

function onKbItemMouseLeave(_id: number) {
  hoveredKbId.value = null
}

function onKbDragStart(e: DragEvent, id: number) {
  dragKbId.value = id
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(id))
  }
}

function onKbDragOver(e: DragEvent, id: number) {
  e.preventDefault()
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
  dragOverKbId.value = id
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const y = e.clientY - rect.top
  dropPosition.value = y < rect.height / 2 ? 'before' : 'after'
}

function onKbDragLeave(e: DragEvent) {
  const target = e.currentTarget as HTMLElement
  const related = e.relatedTarget as HTMLElement | null
  if (related && target.contains(related)) return
  dragOverKbId.value = null
}

function onKbDrop(e: DragEvent, targetId: number) {
  e.preventDefault()
  dragOverKbId.value = null
  const sourceId = dragKbId.value
  if (!sourceId || sourceId === targetId) return

  const currentOrder = kbOrderRef.value.length ? [...kbOrderRef.value] : knowledgeBases.value.map(item => item.id)
  const srcIdx = currentOrder.indexOf(sourceId)
  let dstIdx = currentOrder.indexOf(targetId)
  if (srcIdx < 0 || dstIdx < 0) return

  if (dropPosition.value === 'after') dstIdx++
  if (srcIdx < dstIdx) dstIdx--

  currentOrder.splice(srcIdx, 1)
  currentOrder.splice(dstIdx, 0, sourceId)
  saveKbOrder(currentOrder)
}

function onKbDragEnd() {
  dragKbId.value = null
  dragOverKbId.value = null
}

async function loadKnowledgeBases() {
  const result = await fetchKnowledgeBases()
  knowledgeBases.value = result.records ?? result
  if (!kbOrderRef.value.length) {
    kbOrderRef.value = loadKbOrderFromStorage()
  }
  const existingIds = new Set(knowledgeBases.value.map(item => item.id))
  if (selectedKbId.value && !existingIds.has(selectedKbId.value)) {
    selectedKbId.value = knowledgeBases.value[0]?.id ?? null
    if (selectedKbId.value) localStorage.setItem(KB_SELECTED_ID_KEY, String(selectedKbId.value))
    else localStorage.removeItem(KB_SELECTED_ID_KEY)
  }
  if (!selectedKbId.value && knowledgeBases.value.length > 0) {
    selectedKbId.value = knowledgeBases.value[0]?.id ?? null
    if (selectedKbId.value) localStorage.setItem(KB_SELECTED_ID_KEY, String(selectedKbId.value))
    else localStorage.removeItem(KB_SELECTED_ID_KEY)
  }
  await loadDocuments(true)
}

async function loadUploadPolicy() {
  const policy = await fetchKnowledgeUploadPolicy()
  uploadPolicy.maxFileSizeBytes = policy.maxFileSizeBytes
  uploadPolicy.maxRequestSizeBytes = policy.maxRequestSizeBytes
  uploadPolicy.maxFileSizeLabel = policy.maxFileSizeLabel
  uploadPolicy.maxRequestSizeLabel = policy.maxRequestSizeLabel
  uploadPolicy.allowedExtensions = policy.allowedExtensions ?? []
  uploadPolicy.accept = policy.accept ?? ''
  uploadPolicy.parseTimeoutSeconds = policy.parseTimeoutSeconds ?? 600
}

async function loadSystemDefaultKb() {
  if (!canConfigDefaultKb.value) return
  try {
    systemDefaultKb.value = await fetchSystemDefaultKb()
  } catch (error) {
    console.warn('加载系统默认知识库失败:', error)
    systemDefaultKb.value = null
  }
}

async function onSelectKb(id: number) {
  selectedKbId.value = id
  localStorage.setItem(KB_SELECTED_ID_KEY, String(id))
  docSearchKeyword.value = ''
  docSearchStatus.value = ''
  docPage.value = 1
  await loadDocuments()
}

function onEditKb(item: KnowledgeBaseItem) {
  editingKbId.value = item.id
  editKbForm.kbName = item.kbName
  editKbForm.kbDesc = item.kbDesc || ''
  editKbForm.visibilityScope = item.visibilityScope || 'PRIVATE'
  editKbVisible.value = true
}

async function onSaveKbEdit() {
  if (!editingKbId.value) {
    return
  }
  // 校验: 默认知识库不允许降低可见范围 (否则会导致没有默认 KB, 客户端不可用)
  const editingKb = knowledgeBases.value.find((it) => it.id === editingKbId.value)
  if (editingKb?.isDefault && editKbForm.visibilityScope !== 'PUBLIC') {
    ElMessage.warning('当前知识库是默认知识库，不允许操作！请将其他知识库设置为默认知识库再次尝试。')
    return
  }
  try {
    const updated = await updateKnowledgeBase(editingKbId.value, {
      kbName: editKbForm.kbName,
      kbDesc: editKbForm.kbDesc,
      visibilityScope: editKbForm.visibilityScope,
    })
    const idx = knowledgeBases.value.findIndex((item) => item.id === updated.id)
    if (idx >= 0) {
      knowledgeBases.value[idx] = updated
    }
    editKbVisible.value = false
    ElMessage.success('知识库已更新')
  } catch (error) {
    console.error(error)
    ElMessage.error('知识库更新失败')
  }
}

/** 是否可以编辑 KB 基本信息 (需 EDIT 及以上) */
function canEditKb(item: KnowledgeBaseItem): boolean {
  const lv = item.myPermLevel ?? 0
  return isDeveloper.value || isSuperAdmin.value || lv >= 2
}

/** 是否可以删除 KB (需 MANAGE) */
function canDeleteKb(item: KnowledgeBaseItem): boolean {
  const lv = item.myPermLevel ?? 0
  return isDeveloper.value || isSuperAdmin.value || lv >= 3
}

/** 是否可以管理 KB 授权 (需 MANAGE) */
function canManageAuth(item: KnowledgeBaseItem): boolean {
  const lv = item.myPermLevel ?? 0
  return isDeveloper.value || isSuperAdmin.value || lv >= 3
}

/** 是否可以设置/清除系统默认 KB (仅超管/developer) */
const canConfigDefaultKb = computed(() => isDeveloper.value || isSuperAdmin.value)

function onOpenAuth(item: KnowledgeBaseItem) {
  router.push({ path: '/knowledge/auth', query: { kbId: String(item.id) } })
}

function visibilityLabel(scope?: string): string {
  if (scope === 'PUBLIC') return '公开'
  if (scope === 'INTERNAL' || scope === 'ROLE') return '内部'
  return '私有'
}

/** 可见范围 tag 颜色 */
function visibilityTagType(scope?: string): 'success' | 'warning' | 'info' {
  if (scope === 'PUBLIC') return 'success'
  if (scope === 'INTERNAL' || scope === 'ROLE') return 'warning'
  return 'info'
}

async function onSetDefaultKb(item: KnowledgeBaseItem) {
  if (!canConfigDefaultKb.value) {
    ElMessage.warning('仅超级管理员或 developer 可设置默认知识库')
    return
  }
  const scope = item.visibilityScope
  if (scope !== 'PUBLIC') {
    const upgraded = await ElMessageBox.confirm(
      `当前知识库为「${visibilityLabel(scope)}」，默认知识库必须为「公开」可见范围，所有客户端用户将以该知识库作为默认知识库。\n\n是否将当前知识库升级为「公开」并设为系统默认？`,
      '升级并设为默认',
      { type: 'warning', confirmButtonText: '升级并设为默认', cancelButtonText: '取消' }
    ).then(() => true).catch(() => false)
    if (!upgraded) return
    try {
      settingDefault.value = true
      await updateKnowledgeBase(item.id, { kbName: item.kbName, kbDesc: item.kbDesc, visibilityScope: 'PUBLIC' })
      // 同步更新列表中该 KB 的 visibilityScope (否则可见范围 tag 和 hasPublicKb 不会立即生效)
      const idx = knowledgeBases.value.findIndex((k) => k.id === item.id)
      if (idx >= 0) {
        knowledgeBases.value[idx] = { ...knowledgeBases.value[idx], visibilityScope: 'PUBLIC' }
      }
      const updated = await setSystemDefaultKb(item.id)
      applyDefaultKbLocally(updated)
      ElMessage.success('已将当前知识库升级为公开，并设为系统默认')
    } catch (error) {
      console.error(error)
      ElMessage.error('设为默认知识库失败')
    } finally {
      settingDefault.value = false
    }
    return
  }
  try {
    settingDefault.value = true
    const updated = await setSystemDefaultKb(item.id)
    applyDefaultKbLocally(updated)
    ElMessage.success('已设为系统默认知识库')
  } catch (error) {
    console.error(error)
    ElMessage.error('设为默认知识库失败')
  } finally {
    settingDefault.value = false
  }
}

/** 在列表和 banner 上同步默认 KB 标识 */
function applyDefaultKbLocally(updated: KnowledgeBaseItem) {
  systemDefaultKb.value = updated
  knowledgeBases.value = knowledgeBases.value.map((it) => ({
    ...it,
    isDefault: it.id === updated.id,
  }))
}

async function onClearDefaultKb() {
  try {
    await ElMessageBox.confirm('确认清除当前系统默认知识库？清除后 C 端将回退到「允许用户自行选择」或「无 KB」模式。', '清除默认', {
      type: 'warning',
      confirmButtonText: '清除', cancelButtonText: '取消',
    })
  } catch { return }
  try {
    await clearSystemDefaultKb()
    systemDefaultKb.value = null
    // 列表上 isDefault 也需要清掉
    knowledgeBases.value = knowledgeBases.value.map((it) => ({ ...it, isDefault: false }))
    ElMessage.success('已清除系统默认知识库')
  } catch (error) {
    console.error(error)
    ElMessage.error('清除失败')
  }
}

async function onDeleteKb(item: KnowledgeBaseItem) {
  // 校验: 默认知识库不允许删除 (否则会导致没有默认 KB, 客户端不可用)
  if (item.isDefault) {
    ElMessage.warning('当前知识库是默认知识库，不允许操作！请将其他知识库设置为默认知识库再次尝试。')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除知识库「${item.kbName}」？将同时删除该知识库下的文档与索引数据。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteKnowledgeBase(item.id)
    knowledgeBases.value = knowledgeBases.value.filter((kb) => kb.id !== item.id)
    if (selectedKbId.value === item.id) {
      selectedKbId.value = knowledgeBases.value[0]?.id ?? null
      if (selectedKbId.value) localStorage.setItem(KB_SELECTED_ID_KEY, String(selectedKbId.value))
      else localStorage.removeItem(KB_SELECTED_ID_KEY)
      await loadDocuments()
    }
    syncKbOrderFromList()
    ElMessage.success('知识库已删除')
  } catch (error) {
    console.error(error)
    ElMessage.error('知识库删除失败')
  }
}

function openCreateKbDialog() {
  createForm.kbName = ''
  createForm.kbDesc = ''
  createKbVisible.value = true
}

async function onCreateKb() {
  if (!createForm.kbName) {
    ElMessage.warning('请填写知识库名称')
    return
  }
  creating.value = true
  try {
    const created = await createKnowledgeBase({ kbName: createForm.kbName, kbDesc: createForm.kbDesc })
    knowledgeBases.value.unshift(created)
    selectedKbId.value = created.id
    localStorage.setItem(KB_SELECTED_ID_KEY, String(created.id))
    syncKbOrderFromList()
    applyDocuments([])
    createKbVisible.value = false
    ElMessage.success('知识库创建成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('知识库创建失败')
  } finally {
    creating.value = false
  }
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  appendFiles(Array.from(input.files ?? []))
  input.value = ''
}

function openFilePicker() {
  fileInputRef.value?.click()
}

function clearUploadFiles() {
  uploadFiles.value = []
  uploadProgress.value = 0
  uploadStatusText.value = ''
}

function removeUploadFile(index: number) {
  uploadFiles.value.splice(index, 1)
  if (!uploadFiles.value.length && !uploading.value) {
    uploadProgress.value = 0
    uploadStatusText.value = ''
  }
}

function onDropFiles(event: DragEvent) {
  dragActive.value = false
  appendFiles(Array.from(event.dataTransfer?.files ?? []))
}

function appendFiles(files: File[]) {
  if (!files.length) {
    return
  }
  const merged = dedupeFiles([...uploadFiles.value, ...files])
  const validationMessage = validateUploadFiles(merged)
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }
  uploadFiles.value = merged
  if (!uploading.value) {
    uploadProgress.value = 0
    uploadStatusText.value = ''
  }
}

function dedupeFiles(files: File[]) {
  const seen = new Set<string>()
  return files.filter((file) => {
    const key = [file.name, file.size, file.lastModified].join(':')
    if (seen.has(key)) {
      return false
    }
    seen.add(key)
    return true
  })
}

function validateUploadFiles(files: File[]) {
  if (!uploadPolicy.maxFileSizeBytes) {
    return null
  }
  let totalBytes = 0
  for (const file of files) {
    if (file.size > uploadPolicy.maxFileSizeBytes) {
      return `文件「${file.name}」(${formatFileSize(file.size)}) 超过单个文件大小上限 ${uploadPolicy.maxFileSizeLabel}`
    }
    totalBytes += file.size
  }
  if (uploadPolicy.maxRequestSizeBytes && totalBytes > uploadPolicy.maxRequestSizeBytes) {
    return `总大小 ${formatFileSize(totalBytes)} 超过单次请求上限 ${uploadPolicy.maxRequestSizeLabel}`
  }
  if (uploadPolicy.allowedExtensions.length) {
    for (const file of files) {
      const ext = file.name.includes('.') ? file.name.substring(file.name.lastIndexOf('.') + 1).toLowerCase() : ''
      if (ext && !uploadPolicy.allowedExtensions.includes(ext)) {
        return `文件「${file.name}」格式不支持（支持: ${uploadPolicy.allowedExtensions.join(', ')}）`
      }
    }
  }
  return null
}

async function onUpload() {
  if (!selectedKbId.value || !uploadFiles.value.length) {
    return
  }
  uploading.value = true
  uploadProgress.value = 0
  uploadStatusText.value = '正在上传文件到服务器...'
  try {
    documents.value = await uploadKnowledgeDocuments(selectedKbId.value, uploadFiles.value, {
      onProgress: (percent) => {
        if (percent >= 100) {
          uploadProgress.value = 100
          uploadStatusText.value = '文件已上传，服务端正在解析并建立索引，页面会自动刷新状态...'
          return
        }
        uploadProgress.value = Math.max(percent, 1)
        uploadStatusText.value = `正在上传文件到服务器... ${uploadProgress.value}%`
      },
    })
    applyDocuments(documents.value)
    uploadProgress.value = 100
    uploadStatusText.value = '上传完成，文档已入库，后台解析结果将自动刷新'
    uploadFiles.value = []
    ElMessage.success('文档上传完成')
  } catch (error) {
    console.error(error)
    uploadStatusText.value = resolveErrorMessage(error, '文档上传失败')
    ElMessage.error(uploadStatusText.value)
  } finally {
    uploading.value = false
  }
}

async function onReprocess(row: KnowledgeDocumentItem) {
  if (isDocumentBusy(row)) {
    ElMessage.warning('该文档当前正在处理中，请等待后台任务完成后再重试')
    return
  }
  reprocessingDocumentIds.value = [...new Set([...reprocessingDocumentIds.value, row.id])]
  try {
    await reprocessKnowledgeDocument(row.id)
    await loadDocuments()
    ElMessage.success('已触发重新索引')
  } catch (error) {
    console.error(error)
    ElMessage.error('重建索引失败')
    reprocessingDocumentIds.value = reprocessingDocumentIds.value.filter((documentId) => documentId !== row.id)
  }
}

async function onRenameDocument(row: KnowledgeDocumentItem) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的文档名称', '重命名', {
      inputValue: row.fileName,
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    const name = (typeof value === 'string' ? value : String(value)).trim()
    if (!name || name === row.fileName) {
      return
    }
    await updateKnowledgeDocument(row.id, { fileName: name })
    const idx = documents.value.findIndex((item) => item.id === row.id)
    if (idx >= 0) {
      documents.value[idx] = { ...documents.value[idx], fileName: name } as KnowledgeDocumentItem
    }
    ElMessage.success('文档名称已更新')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error(error)
      ElMessage.error('重命名失败')
    }
  }
}

async function onDeleteDocument(row: KnowledgeDocumentItem) {
  try {
    await ElMessageBox.confirm(`确认删除文档「${row.fileName}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteKnowledgeDocument(row.id)
    documents.value = documents.value.filter((item) => item.id !== row.id)
    ElMessage.success('文档已删除')
  } catch (error) {
    console.error(error)
    ElMessage.error('文档删除失败')
  }
}

function parseStatusText(status?: string) {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'PENDING') return '待处理'
  if (status === 'PROCESSING') return '处理中'
  if (status === 'DELETED') return '已删除'
  return status || '未知'
}

const STAGE_NAME_MAP: Record<string, string> = {
  download: '文档下载',
  extract: '文档提取',
  ocr: 'OCR识别',
  chunk: '文档分块',
  embedding: '向量化',
  es_index: 'ES索引',
}

function resolveStageName(stage: { stageId: string; stageName?: string }): string {
  return stage.stageName || STAGE_NAME_MAP[stage.stageId] || stage.stageId
}

function formatStageDuration(startAt: string, endAt: string): string {
  const dur = new Date(endAt).getTime() - new Date(startAt).getTime()
  return dur >= 1000 ? `${(dur / 1000).toFixed(1)} 秒` : `${dur} 毫秒`
}

function formatRunningDuration(startAt: string): string {
  const dur = Date.now() - new Date(startAt).getTime()
  return dur >= 1000 ? `${(dur / 1000).toFixed(1)} 秒` : `${dur} 毫秒`
}

function shortenDeviceName(fullName: string): string {
  if (!fullName) return ''
  const patterns: [RegExp, (m: RegExpMatchArray) => string][] = [
    [/Intel\(R\)\s+Core\(TM\)\s+(.+)/i, (m) => m[1]!.trim().replace(/\s+\d+-Core\s*/i, '')],
    [/Intel\(R\)\s+Arc\(TM\)\s+(\d+T)\s+GPU\s*\(.*?\)/i, (m) => 'Arc ' + m[1]!],
    [/Intel\(R\)\s+Arc\(TM\)\s+(.+)/i, (m) => 'Arc ' + m[1]!.replace(/\s*\(.*?\)/, '').trim()],
    [/NVIDIA\s+GeForce\s+(RTX|GTX)\s+(.+)/i, (m) => m[1]! + ' ' + m[2]!.trim()],
    [/AMD\s+Ryzen\s+(.+)/i, (m) => m[1]!.trim()],
    [/Apple\s+M(\d)/i, (m) => 'M' + m[1]!],
  ]
  for (const [p, fn] of patterns) {
    const m = fullName.match(p as RegExp)
    if (m) return (fn as (m: RegExpMatchArray) => string)(m)
  }
  return fullName.length > 12 ? fullName.slice(0, 10) + '…' : fullName
}

async function loadSysResources() {
  try {
    const raw = await fetchSystemResources()
    if (raw) {
      const cpuName = (raw as any).cpu?.name || ''
      raw.cpuShort = shortenDeviceName(cpuName)
      raw.cpuFullName = cpuName
      const gpus: any[] = (raw as any).gpus || []
      for (const g of gpus) {
        g.short = shortenDeviceName(g.name || '')
        g.fullName = g.name || ''
      }
      raw.gpuList = gpus
      sysResources.value = raw
    }
  } catch { /* ignore */ }
}

function startSysResTimer() {
  stopSysResTimer()
  loadSysResources()
  sysResTimer = setInterval(loadSysResources, 1000)
}

function stopSysResTimer() {
  if (sysResTimer) { clearInterval(sysResTimer); sysResTimer = null }
}

function formatFileSize(size?: number) {
  const value = Number(size ?? 0)
  if (value >= 1024 * 1024 * 1024) return `${(value / 1024 / 1024 / 1024).toFixed(2)} GB`
  if (value >= 1024 * 1024) return `${(value / 1024 / 1024).toFixed(2)} MB`
  if (value >= 1024) return `${(value / 1024).toFixed(2)} KB`
  return `${value} B`
}

function resolveErrorMessage(error: unknown, fallback: string) {
  const responseMessage = (error as { response?: { data?: { message?: string; error?: string; detail?: string } } })?.response?.data
  return responseMessage?.message || responseMessage?.detail || responseMessage?.error || fallback
}

const parseStatusOptions = [
  { label: '全部', value: '' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
]

onMounted(async () => {
  loadSidebarWidth()
  loading.value = true
  try {
    await Promise.all([loadKnowledgeBases(), loadUploadPolicy(), loadSystemDefaultKb()])
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  stopDocumentPolling()
  stopParsePolling()
})

async function openParseDialog(row: KnowledgeDocumentItem) {
  parseDialogVisible.value = true
  parseProgress.value = null
  selectedStageId.value = ''
  userManualSelected.value = false
  liveClockOffset.value = 0
  if (liveClockTimer) clearInterval(liveClockTimer)
  liveClockTimer = setInterval(() => { liveClockOffset.value += 1000 }, 1000)
  startSysResTimer()
  await fetchParseProgressData(row.id)
  if (isProcessingStatus(row.parseStatus)) {
    const pid = row.id
    parsePollingTimer.value = setInterval(() => fetchParseProgressData(pid), 2000)
  }
}

async function fetchParseProgressData(documentId: number) {
  try {
    const data = await fetchDocumentParseProgress(documentId)
    const prevProgress = parseProgress.value
    parseProgress.value = data
    const stages = data.stages || []
    if (stages.length === 0) return
    const firstNonCompleted = stages.find(s => s.status !== 'completed')
    const autoTarget = firstNonCompleted?.stageId || stages[stages.length - 1]?.stageId || ''
    if (!userManualSelected.value || (prevProgress && _hasNewlyCompleted(prevProgress.stages, stages))) {
      selectedStageId.value = autoTarget
      userManualSelected.value = false
    }
    nextTick(() => {
      const el = parseLogsBodyRef.value
      if (el) el.scrollTop = el.scrollHeight
    })
  } catch {
  }
}

function _hasNewlyCompleted(prevStages: { stageId: string; status: string }[] | undefined, currStages: { stageId: string; status: string }[]): boolean {
  if (!prevStages) return false
  const prevMap = new Map(prevStages.map(s => [s.stageId, s.status]))
  return currStages.some(s => s.status === 'completed' && prevMap.get(s.stageId) !== 'completed')
}

function onCancelParse() {
  const progress = parseProgress.value
  if (!progress?.documentId) return
  ElMessageBox.confirm('确定要强制终止当前文档的解析任务吗？终止后将自动回滚已写入的 ES 索引数据。', '确认终止', {
    confirmButtonText: '确定终止',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await cancelDocumentParse(progress.documentId!)
    stopParsePolling()
    await fetchParseProgressData(progress.documentId!)
    ElMessage.warning('任务已终止，ES 索引已回滚')
  }).catch(() => {})
}

function onParseDialogClose() {
  stopParsePolling()
  if (liveClockTimer) {
    clearInterval(liveClockTimer)
    liveClockTimer = null
  }
}

function stopParsePolling() {
  if (parsePollingTimer.value) {
    clearInterval(parsePollingTimer.value)
    parsePollingTimer.value = null
  }
  stopSysResTimer()
}

const filteredLogs = computed(() => {
  if (!parseProgress.value || !parseProgress.value.logs) return []
  return parseProgress.value.logs.filter(log => !log.stageId || log.stageId === selectedStageId.value)
})

const latestLogByDoc = computed(() => {
  const map: Record<number, string> = {}
  if (!parseProgress.value || !parseProgress.value.logs) return map
  const logs = parseProgress.value.logs
  if (logs.length > 0) {
    const docId = parseProgress.value.documentId
    if (docId) map[docId] = logs[logs.length - 1]!.message
  }
  return map
})
</script>

<template>
  <div class="kb-layout">
    <header class="page-header">
      <h1>知识库管理</h1>
    </header>

    <div v-if="canConfigDefaultKb" class="kb-default-banner">
      <span class="kb-default-label">系统默认知识库 (所有用户共享):</span>
      <span v-if="systemDefaultKb" class="kb-default-name">{{ systemDefaultKb.kbName }}</span>
      <span v-else class="kb-default-empty">暂未设置</span>
      <span class="kb-default-tip">仅超管可配置, 设为默认后所有客户端用户将统一使用该知识库</span>
      <el-button v-if="systemDefaultKb" link type="danger" @click="onClearDefaultKb">清除</el-button>
    </div>

    <div class="kb-body" ref="kbLayoutRef">
      <div class="kb-sidebar" :style="{ width: sidebarWidth + 'px' }">
        <div class="sidebar-header">
          <span>知识库列表</span>
          <el-button size="small" class="sidebar-add-btn" :icon="Plus" @click="openCreateKbDialog" />
        </div>
        <div class="kb-nav-list">
          <template v-for="(item, _index) in sortedKnowledgeBases" :key="item.id">
            <div
              v-if="dragKbId !== null && dragOverKbId === item.id && dropPosition === 'before'"
              class="kb-drop-line"
            ></div>
            <div
              class="kb-nav-item"
              :class="{ active: item.id === selectedKbId }"
              draggable="true"
              @click="onSelectKb(item.id)"
              @mouseenter="hoveredKbId = item.id"
              @mouseleave="onKbItemMouseLeave(item.id)"
              @dragstart="onKbDragStart($event, item.id)"
              @dragover.prevent="onKbDragOver($event, item.id)"
              @dragleave="onKbDragLeave"
              @drop="onKbDrop($event, item.id)"
              @dragend="onKbDragEnd"
            >
              <span class="kb-drag-handle" :class="{ visible: hoveredKbId === item.id || dragKbId === item.id }" @mousedown.stop></span>
              <div class="kb-nav-name">
                {{ item.kbName }}
                <el-button
                  v-if="!item.isDefault && canConfigDefaultKb"
                  class="kb-set-default-btn"
                  link
                  type="primary"
                  size="small"
                  @click.stop="onSetDefaultKb(item)"
                >设为默认</el-button>
              </div>
              <div class="kb-nav-tags">
                <el-tooltip v-if="item.isDefault" content="当前系统默认知识库 (C 端用户共享)" placement="top">
                  <el-tag size="small" type="warning" effect="plain">默认</el-tag>
                </el-tooltip>
                <el-tooltip :content="`可见范围: ${visibilityLabel(item.visibilityScope)}`" placement="top">
                  <el-tag size="small" :type="visibilityTagType(item.visibilityScope)" effect="plain">
                    {{ visibilityLabel(item.visibilityScope) }}
                  </el-tag>
                </el-tooltip>
              </div>
              <el-dropdown trigger="click" :teleported="true" popper-class="kb-item-popper" :class="{ 'kb-item-menu': true, 'kb-item-menu-visible': hoveredKbId === item.id || dragKbId === item.id }" @command="(cmd: string) => { if (cmd === 'edit') onEditKb(item); else if (cmd === 'auth') onOpenAuth(item); else if (cmd === 'delete') onDeleteKb(item) }">
                <span class="kb-item-dots" @click.stop>⋯</span>
                <template #dropdown>
                  <el-dropdown-item command="edit" :disabled="!canEditKb(item)">编辑</el-dropdown-item>
                  <el-dropdown-item command="auth" :disabled="!canManageAuth(item)" divided>权限</el-dropdown-item>
                  <el-dropdown-item command="delete" :disabled="!canDeleteKb(item)" style="color: #f56c6c">删除</el-dropdown-item>
                </template>
              </el-dropdown>
            </div>
            <div
              v-if="dragKbId !== null && dragOverKbId === item.id && dropPosition === 'after'"
              class="kb-drop-line"
            ></div>
          </template>
          <div v-if="!sortedKnowledgeBases.length" class="kb-nav-empty">暂无知识库</div>
        </div>
        <div v-if="canConfigDefaultKb" class="kb-sidebar-footer">
          <el-tooltip :content="clientSelectKbTip" placement="top">
            <div class="kb-sidebar-footer-row">
              <span class="kb-sidebar-footer-label">允许客户端选择公开知识库</span>
              <el-switch
                :model-value="clientSelectKbEnabled"
                :disabled="!hasPublicKb"
                @change="(v: boolean | number | string) => onClientSelectKbChange(Boolean(v))"
              />
            </div>
          </el-tooltip>
        </div>
      </div>

      <!-- 分隔条: 拖动调节 sidebar 宽度 -->
      <div class="kb-splitter" @mousedown="onSplitterDown">
        <div class="kb-splitter-handle" />
      </div>

      <div class="kb-main">
        <div class="doc-search-bar">
          <el-input
            v-model="docSearchKeyword"
            placeholder="搜索文档名称..."
            clearable
            style="width: 260px"
            @keyup.enter="onDocSearch"
            @clear="onDocSearchReset"
          >
            <template #prefix>
              <span style="color: #909399">🔍</span>
            </template>
          </el-input>
          <el-select v-model="docSearchStatus" placeholder="解析状态" clearable style="width: 140px" @change="onDocSearch" @clear="onDocSearchReset">
            <el-option v-for="opt in parseStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-button @click="onDocSearch">搜索</el-button>
        </div>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-header-actions">
              <span>文档管理</span>
              <span v-if="currentKnowledgeBase" class="kb-current-label">当前知识库：{{ currentKnowledgeBase.kbName }}</span>
            </div>
          </template>

          <div class="upload-panel">
            <input
              ref="fileInputRef"
              class="hidden-file-input"
              type="file"
              multiple
              :accept="uploadPolicy.accept"
              @change="onFileChange"
            />
            <div
              class="upload-dropzone"
              :class="{ active: dragActive, disabled: !selectedKbId }"
              @click="selectedKbId && openFilePicker()"
              @dragenter.prevent="dragActive = true"
              @dragover.prevent="dragActive = true"
              @dragleave.prevent="dragActive = false"
              @drop.prevent="onDropFiles"
            >
              <div class="upload-title">拖拽文件到这里，或点击选择文件</div>
              <div class="upload-subtitle">
                {{ uploadSummaryText }}
              </div>
              <div class="upload-rules">
                支持格式：{{ uploadPolicy.allowedExtensions.join(' / ') }}
              </div>
            </div>

            <div class="upload-toolbar">
              <div class="upload-toolbar-info">
                <span>目标知识库：{{ currentKnowledgeBase?.kbName || '请先选择知识库' }}</span>
                <span v-if="uploadPolicy.parseTimeoutSeconds > 0">解析超时：{{ Math.ceil(uploadPolicy.parseTimeoutSeconds / 60) }} 分钟</span>
              </div>
              <div class="upload-toolbar-actions">
                <el-button :disabled="uploading" @click="openFilePicker">选择文件</el-button>
                <el-button :disabled="uploading || !uploadFiles.length" @click="clearUploadFiles">清空列表</el-button>
                <el-button type="primary" :loading="uploading" :disabled="!selectedKbId || !uploadFiles.length" @click="onUpload">
                  {{ uploadButtonText }}
                </el-button>
              </div>
            </div>

            <div v-if="uploadFiles.length" class="upload-file-list">
              <div v-for="(file, index) in uploadFiles" :key="`${file.name}-${file.size}-${file.lastModified}`" class="upload-file-item">
                <div class="upload-file-main">
                  <div class="upload-file-name">{{ file.name }}</div>
                  <div class="upload-file-meta">{{ formatFileSize(file.size) }}</div>
                </div>
                <el-button link type="danger" :disabled="uploading" @click="removeUploadFile(index)">移除</el-button>
              </div>
            </div>

            <div v-if="uploadStatusText" class="upload-progress-card">
              <div class="upload-progress-header">
                <span>上传状态</span>
                <span>{{ uploadStatusText }}</span>
              </div>
              <el-progress
                :percentage="uploadProgress"
                :status="uploading ? undefined : uploadProgress === 100 ? 'success' : 'exception'"
                :indeterminate="uploading && uploadProgress === 100"
                :duration="2"
                :stroke-width="14"
              />
            </div>
          </div>

          <el-table :data="documents" stripe v-loading="loading">
            <el-table-column label="文件名" min-width="220">
              <template #default="{ row }">
                <a :href="getDocumentFileUrl(row.id)" target="_blank" class="doc-link">
                  {{ row.fileName }}
                </a>
              </template>
            </el-table-column>
            <el-table-column label="大小" min-width="110">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="parseStatus" label="解析状态" min-width="120">
              <template #default="{ row }">
                <el-tag
                  :type="row.parseStatus === 'SUCCESS' ? 'success' : row.parseStatus === 'FAILED' ? 'danger' : 'warning'"
                  :style="isClickableStatus(row.parseStatus) ? 'cursor: pointer' : ''"
                  @click="isClickableStatus(row.parseStatus) ? openParseDialog(row) : undefined"
                >
                  {{ parseStatusText(row.parseStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="处理说明" min-width="300">
              <template #default="{ row }">
                <span v-if="isProcessingStatus(row.parseStatus) && latestLogByDoc[row.id]">{{ latestLogByDoc[row.id] }}</span>
                <span v-else>{{ row.parseMessage }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="160" fixed="right">
              <template #default="{ row }">
                <el-tooltip content="重建" placement="top">
                  <el-button link type="primary" :icon="Refresh" :disabled="isDocumentBusy(row)" @click="onReprocess(row)" />
                </el-tooltip>
                <el-tooltip content="重命名" placement="top">
                  <el-button link type="primary" :icon="Edit" @click="onRenameDocument(row)" />
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button link type="danger" :icon="Delete" @click="onDeleteDocument(row)" />
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="docPage"
            v-model:page-size="docSize"
            :total="docTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @current-change="onDocPageChange"
            @size-change="onDocSizeChange"
          />
        </el-card>
      </div>
    </div>

    <el-dialog v-model="editKbVisible" title="编辑知识库" width="560px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="知识库名称">
          <el-input v-model="editKbForm.kbName" />
        </el-form-item>
        <el-form-item label="知识库说明">
          <el-input v-model="editKbForm.kbDesc" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span>可见范围
              <el-tooltip placement="top">
                <template #content>
                  <div style="line-height: 1.6">
                    <div>● <b>私有</b>: 知识库所有者, 拥有知识库的所有权限</div>
                    <div>● <b>内部</b>: 后端管理系统中, 管理角色可见, 但只有被赋予对应权限的用户可以执行对应的操作. 比如: 被授权可编辑的, 具备编辑权限但不可管理; 被授权可管理权限的, 可管理 / 可编辑 / 可查看</div>
                    <div>● <b>公开</b>: 所有人可见, 对所有管理用户开放管理权限 (包括可编辑和可查看权限)</div>
                  </div>
                </template>
                <el-icon style="cursor: help; color: #909399; margin-left: 2px"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-select v-model="editKbForm.visibilityScope" style="width: 100%">
            <el-option label="私有" value="PRIVATE" />
            <el-option label="内部" value="INTERNAL" />
            <el-option label="公开" value="PUBLIC" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="canConfigDefaultKb && editingKbId && currentKnowledgeBase && currentKnowledgeBase.id === editingKbId">
          <template #label>
            <span>设为默认知识库
              <el-tooltip content="所有客户端用户将以当前知识库作为默认知识库, 默认知识库必须为公开可见范围" placement="top">
                <el-icon style="cursor: help; color: #909399; margin-left: 2px"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-button :loading="settingDefault" :disabled="currentKnowledgeBase.isDefault" @click="onSetDefaultKb(currentKnowledgeBase)">
            {{ currentKnowledgeBase.isDefault ? '当前已是默认知识库' : '设为默认' }}
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editKbVisible = false">取消</el-button>
        <el-button type="primary" @click="onSaveKbEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createKbVisible" title="创建知识库" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="知识库名称">
          <el-input v-model="createForm.kbName" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="知识库说明">
          <el-input v-model="createForm.kbDesc" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createKbVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreateKb">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseDialogVisible" title="文档解析进度" width="960px" :close-on-click-modal="true" destroy-on-close @close="onParseDialogClose">
      <div v-if="parseProgress" class="parse-dialog-body">
        <div class="parse-header">
          <div class="parse-header-item"><span class="parse-header-label">文件名：</span>{{ parseProgress.fileName }}</div>
          <div class="parse-header-item"><span class="parse-header-label">文件类型：</span>{{ parseProgress.fileExt?.toUpperCase() }}</div>
          <div class="parse-header-item"><span class="parse-header-label">文件大小：</span>{{ formatFileSize(parseProgress.fileSize) }}</div>
          <div class="parse-header-item"><span class="parse-header-label">解析状态：</span>
            <el-tag :type="parseProgress.parseStatus === 'SUCCESS' ? 'success' : parseProgress.parseStatus === 'FAILED' ? 'danger' : 'warning'" size="small">{{ parseStatusText(parseProgress.parseStatus) }}</el-tag>
          </div>
        </div>
        <div class="parse-total-progress">
          <div class="parse-total-label">总进度：{{ parseProgress.overallProgress }}%</div>
          <div class="total-progress-bar" :class="{ 'is-done': parseProgress.overallProgress >= 100 }">
            <div class="total-progress-fill" :style="{ width: parseProgress.overallProgress + '%' }">
              <div v-if="parseProgress.overallProgress < 100" class="total-progress-streak"></div>
            </div>
          </div>
        </div>
        <div class="parse-stages">
          <template v-for="(stage, idx) in parseProgress.stages" :key="stage.stageId">
            <div v-if="idx > 0" class="stage-arrow">
              <svg width="20" height="20" viewBox="0 0 20 20"><polygon points="2,2 18,10 2,18" fill="#409eff" /></svg>
            </div>
            <div
              class="parse-stage-item"
              :class="{ 'is-selected': selectedStageId === stage.stageId, 'is-completed': stage.status === 'completed', 'is-running': stage.status === 'running', 'is-failed': stage.status === 'failed' }"
              @click="selectedStageId = stage.stageId; userManualSelected = true"
            >
              <div class="stage-circle">
                <svg width="60" height="60" viewBox="0 0 60 60" style="overflow:visible">
                  <circle cx="30" cy="30" r="26" stroke="#e5e7eb" stroke-width="4" fill="none" />
                  <circle
                    cx="30" cy="30" r="26"
                    stroke="currentColor"
                    stroke-width="4"
                    fill="none"
                    stroke-linecap="round"
                    :stroke-dasharray="`${2 * Math.PI * 26}`"
                    :stroke-dashoffset="`${2 * Math.PI * 26 * (1 - stage.progress / 100)}`"
                    class="stage-circle-progress"
                  />
                  <circle v-if="stage.status === 'completed'" cx="30" cy="30" r="24" fill="rgba(103, 194, 58, 0.12)" />
                  <path v-if="stage.status === 'completed'" d="M18 30 L26 38 L42 22" stroke="#67c23a" stroke-width="3.5" fill="none" stroke-linecap="round" stroke-linejoin="round" />
                  <path v-else-if="stage.status === 'failed'" d="M20 20 L40 40 M40 20 L20 40" stroke="#f56c6c" stroke-width="3" fill="none" stroke-linecap="round" />
                  <text v-else x="30" y="36" text-anchor="middle" fill="#0f2744" font-size="14" font-weight="600">{{ stage.progress }}%</text>
                </svg>
                <div class="stage-name">{{ resolveStageName(stage) }}</div>
                <div v-if="stage.status === 'completed' && stage.startedAt && stage.completedAt" class="stage-duration">
                  {{ formatStageDuration(stage.startedAt, stage.completedAt) }}
                </div>
                <div v-else-if="stage.status === 'running' && stage.startedAt" class="stage-duration stage-duration-running">
                  {{ formatRunningDuration(stage.startedAt) }}
                </div>
              </div>
            </div>
          </template>
        </div>
        <div class="parse-logs">
          <div class="parse-logs-header">
            <span class="parse-logs-title">处理日志</span>
            <span class="parse-logs-resources">
              <template v-if="sysResources">
                <el-tooltip v-if="sysResources.cpuFullName" :content="sysResources.cpuFullName" placement="top">
                  <span class="res-tag">CPU（{{ sysResources.cpuShort }}）：{{ sysResources.cpu.percent }}%</span>
                </el-tooltip>
                <span v-else class="res-tag">CPU：{{ sysResources.cpu.percent }}%</span>
                <template v-for="g in sysResources.gpuList" :key="g.name">
                  <el-tooltip v-if="g.fullName" :content="g.fullName" placement="top">
                    <span class="res-tag">GPU（{{ g.short }}）：{{ g.percent }}%</span>
                  </el-tooltip>
                  <span v-else class="res-tag">GPU（{{ g.short }}）：{{ g.percent }}%</span>
                </template>
                <span class="res-tag">RAM {{ sysResources.ram.usedGb }}/{{ sysResources.ram.totalGb }}G：{{ sysResources.ram.percent }}%</span>
              </template>
            </span>
          </div>
          <div class="parse-logs-body" ref="parseLogsBodyRef">
            <div v-if="filteredLogs.length === 0" class="parse-logs-empty">暂无日志</div>
            <div v-for="(log, idx) in filteredLogs" :key="idx" class="parse-log-entry" :class="'log-' + log.level">
              <span class="log-time">{{ log.timestamp }}</span>
              <span class="log-msg">{{ log.message }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="parse-dialog-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <template #footer>
        <el-button type="danger" :disabled="!parseProgress || parseProgress.parseStatus !== 'PROCESSING'" @click="onCancelParse">强制终止</el-button>
        <el-button @click="parseDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.kb-layout {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 12px 3px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #0f2744;
}

.kb-default-banner {
  margin: 0 24px 12px 3px;
  padding: 10px 16px;
  background: linear-gradient(90deg, #fff7e6 0%, #fffbe6 100%);
  border: 1px solid #ffd591;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #874d00;
}

.kb-default-label {
  font-weight: 600;
}

.kb-default-name {
  color: #d46b08;
  font-weight: 700;
}

.kb-default-empty {
  color: #999;
  font-style: italic;
}

.kb-default-tip {
  color: #b37c00;
  font-size: 12px;
  flex: 1;
}

.kb-body {
  display: flex;
  flex: 1;
  overflow: hidden;
  padding: 0 24px 24px 3px;
  gap: 8px;
}

.kb-sidebar {
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/** 分隔条: 拖动调节 sidebar 宽度 */
.kb-splitter {
  flex-shrink: 0;
  width: 6px;
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  user-select: none;
}
.kb-splitter:hover .kb-splitter-handle,
.kb-splitter:active .kb-splitter-handle {
  background: #409eff;
}
.kb-splitter-handle {
  width: 2px;
  height: 32px;
  background: #dcdfe6;
  border-radius: 1px;
  transition: background 0.15s;
}

/** 列表底部: 「允许客户端选择公开知识库」开关 */
.kb-sidebar-footer {
  border-top: 1px solid #f0f2f5;
  padding: 10px 12px;
  background: #fafbfc;
}
.kb-sidebar-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #4b5563;
  cursor: help;
}
.kb-sidebar-footer-label {
  flex: 1;
  line-height: 1.4;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  font-size: 13px;
  font-weight: 700;
  color: #0f2744;
  border-bottom: 1px solid #f0f2f5;
}

.sidebar-add-btn {
  border-radius: 8px !important;
  background: #ecf5ff !important;
  border-color: #b3d8ff !important;
  color: #409eff !important;
}

.sidebar-add-btn:hover {
  background: #d9ecff !important;
  border-color: #409eff !important;
}

.kb-nav-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px;
}

.kb-nav-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 2px;
  user-select: none;
}

.kb-nav-item:hover {
  background: #f5f7fa;
}

.kb-nav-item.active {
  background: #ecf5ff;
}

.kb-drop-line {
  height: 2px;
  margin: 2px 8px;
  background: #409eff;
  border-radius: 1px;
}

.kb-drag-handle {
  width: 14px;
  height: 18px;
  flex-shrink: 0;
  margin-right: 6px;
  color: #6b7280;
  opacity: 0;
  transition: opacity 0.15s;
  cursor: grab;
  background-image:
    radial-gradient(circle at center, currentColor 35%, transparent 35%),
    radial-gradient(circle at center, currentColor 35%, transparent 35%),
    radial-gradient(circle at center, currentColor 35%, transparent 35%),
    radial-gradient(circle at center, currentColor 35%, transparent 35%),
    radial-gradient(circle at center, currentColor 35%, transparent 35%),
    radial-gradient(circle at center, currentColor 35%, transparent 35%);
  background-size: 5px 5px;
  background-position:
    0 0, 5px 0,
    0 6px, 5px 6px,
    0 12px, 5px 12px;
  background-repeat: no-repeat;
}

.kb-drag-handle.visible {
  opacity: 0.6;
}

.kb-nav-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
  word-break: break-all;
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

/** KB item 右侧 tags 容器: 默认 tag + 可见范围 tag, 向右浮动 */
.kb-nav-tags {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  margin-left: auto;
  padding-right: 32px;
}
.kb-nav-tags .el-tag {
  font-size: 11px;
  height: 20px;
  line-height: 18px;
  padding: 0 6px;
}

/** 「设为默认」按钮: 默认 display:none 不占位不遮挡, hover 父级 item 时显示 */
.kb-set-default-btn {
  display: none !important;
  font-size: 12px !important;
  padding: 0 4px !important;
}
.kb-nav-item:hover .kb-set-default-btn {
  display: inline-flex !important;
}

.kb-item-menu {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.15s;
}

.kb-item-menu-visible {
  opacity: 1;
}

.kb-item-dots {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 700;
  color: #6b7280;
  cursor: pointer;
  line-height: 1;
}

.kb-item-dots:hover {
  background: #e5e7eb;
  color: #374151;
}

.kb-nav-empty {
  padding: 20px;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}

.kb-main {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.doc-search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 0 2px;
}

.kb-current-label {
  font-size: 13px;
  color: #6b7280;
}

.panel-card {
  border-radius: 12px;
  flex: 1;
}

.panel-header-actions,
.upload-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.upload-panel {
  display: grid;
  gap: 16px;
  margin-bottom: 16px;
}

.hidden-file-input {
  display: none;
}

.upload-dropzone {
  padding: 24px;
  border: 1px dashed #b8c7db;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fbff 0%, #f4f8fd 100%);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.upload-dropzone:hover,
.upload-dropzone.active {
  border-color: #409eff;
  box-shadow: 0 12px 24px rgba(64, 158, 255, 0.12);
  transform: translateY(-1px);
}

.upload-dropzone.disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.upload-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f2744;
}

.upload-subtitle,
.upload-rules {
  margin-top: 8px;
  color: #5b6b7f;
  font-size: 13px;
}

.upload-toolbar {
  flex-wrap: wrap;
}

.upload-toolbar-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #5b6b7f;
  font-size: 13px;
}

.upload-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-file-list {
  display: grid;
  gap: 10px;
}

.upload-file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #fafcff;
}

.upload-file-main {
  min-width: 0;
  flex: 1;
}

.upload-file-name {
  font-weight: 600;
  color: #172554;
  word-break: break-all;
}

.upload-file-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.upload-progress-card {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #dbe3ef;
  background: #fbfdff;
}

.upload-progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: #34495e;
  font-size: 13px;
}

.doc-link {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}

.doc-link:hover {
  text-decoration: underline;
  color: #337ecc;
}

.parse-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 78vh;
  overflow: hidden;
}

.parse-header {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.parse-header-item {
  font-size: 13px;
  color: #374151;
}

.parse-header-label {
  font-weight: 600;
  color: #0f2744;
}

.parse-total-progress {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.total-progress-bar {
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.total-progress-bar.is-done .total-progress-fill {
  background: #67c23a;
}

.total-progress-fill {
  height: 100%;
  background: #409eff;
  border-radius: 4px;
  position: relative;
  overflow: hidden;
  transition: width 0.5s ease;
}

.total-progress-streak {
  position: absolute;
  top: 0;
  left: -60%;
  width: 60%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.15) 30%,
    rgba(255, 255, 255, 0.5) 50%,
    rgba(255, 255, 255, 0.15) 70%,
    transparent 100%
  );
  animation: streak-slide 1.8s ease-in-out infinite;
}

@keyframes streak-slide {
  0% { left: -60%; }
  100% { left: 100%; }
}

.parse-total-label {
  font-size: 14px;
  font-weight: 600;
  color: #0f2744;
}

.parse-stages {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}

.parse-stage-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  padding: 4px;
  border-radius: 10px;
  border: 2px solid transparent;
  transition: background 0.2s;
  min-width: 70px;
  overflow: visible;
}

.parse-stage-item:hover {
  background: #f0f5ff;
}

.parse-stage-item.is-selected {
  background: #e6f0ff;
}

.stage-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.parse-stage-item.is-completed .stage-circle-progress {
  color: #67c23a;
}

.parse-stage-item.is-running .stage-circle-progress {
  color: #409eff;
  animation: ring-breathe 2s ease-in-out infinite;
}

@keyframes ring-breathe {
  0%, 100% { stroke-width: 4; filter: drop-shadow(0 0 2px rgba(64, 158, 255, 0.3)); }
  50% { stroke-width: 4; filter: drop-shadow(0 0 6px rgba(64, 158, 255, 1)) drop-shadow(0 0 12px rgba(64, 158, 255, 0.35)); }
}

.parse-stage-item.is-failed .stage-circle-progress {
  color: #f56c6c;
}

.stage-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  overflow: visible;
}

.stage-circle-progress {
  color: #e5e7eb;
  transform: rotate(-90deg);
  transform-origin: 30px 30px;
  transition: stroke-dashoffset 0.5s ease;
}

.stage-name {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  margin-top: 2px;
}

.stage-duration {
  font-size: 10px;
  color: #67c23a;
  font-weight: 500;
  margin-top: 1px;
}

.stage-duration-running {
  color: #409eff;
}

.parse-logs {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.parse-logs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
}

.parse-logs-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f2744;
}

.parse-logs-device {
  font-size: 12px;
  color: #409eff;
  font-weight: 500;
}

.parse-logs-resources {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.res-tag {
  font-size: 11px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 1px 5px;
  border-radius: 3px;
  white-space: nowrap;
  letter-spacing: -0.3px;
}

.parse-logs-body {
  height: 250px;
  overflow-y: auto;
  padding: 8px 12px;
  background: #1e1e2e;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
}

.parse-logs-empty {
  color: #6b7280;
  font-style: italic;
  padding: 12px 0;
}

.parse-log-entry {
  padding: 4px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #cdd6f4;
}

.parse-log-entry.log-info {
  color: #a6e3a1;
}

.parse-log-entry.log-error {
  color: #f38ba8;
}

.parse-log-entry.log-warn {
  color: #f9e2af;
}

.log-time {
  color: #89b4fa;
  min-width: 140px;
}

.log-msg {
  flex: 1;
}

.parse-dialog-loading {
  padding: 24px;
}
</style>

<style>
.kb-item-popper {
  z-index: 3000 !important;
}
</style>