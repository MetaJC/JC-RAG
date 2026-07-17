<script setup lang="ts">
/**
 * ChatHomeView (C 端聊天主页).
 * <p>在 App.vue 的 keep-alive 中按 name 匹配缓存, 用户从子页面返回时不重载.
 * <p>注意: keep-alive 下 onMounted 只在首次挂载触发一次, 后续"激活"由 onActivated 处理.
 * <p>当前需求: 不重载即不重载, onActivated 不做额外动作 (数据 / 滚动位置均由组件自身保留).
 */
defineOptions({ name: 'ChatHomeView' })
import { ref, reactive, onMounted, onActivated, nextTick, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, DataLine, MoreFilled, Edit, Delete, Setting } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import markdownItTaskLists from 'markdown-it-task-lists'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { userApi, userApiBase, isUnauthorizedError, extractErrorMessage } from '../../api/user'
import PreferencesDialog from '../../components/PreferencesDialog.vue'

const COPY_ICON = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>'
const QUOTE_ICON = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 14 21 3"/><path d="M21 3h-6"/><path d="M21 3v6"/><path d="M14 10H8a5 5 0 1 0 0 10h10"/></svg>'

const markdown = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true,
  typographer: true,
})
markdown.use(markdownItTaskLists, { enabled: true, label: true, labelAfter: true })
markdown.renderer.rules.fence = (tokens: Array<{ info?: string; content: string }>, idx: number) => {
  const token = tokens[idx] ?? { info: '', content: '' }
  const info = (token.info || '').trim()
  const rawLanguage = info.split(/\s+/)[0]?.toLowerCase() || ''
  const language = rawLanguage && hljs.getLanguage(rawLanguage) ? rawLanguage : ''
  const displayLanguage = language || rawLanguage || 'text'
  const highlighted = language
    ? hljs.highlight(token.content, { language, ignoreIllegals: true }).value
    : markdown.utils.escapeHtml(token.content)
  const encodedCode = encodeURIComponent(token.content)
  return `
    <div class="code-block-shell">
      <div class="code-toolbar">
        <span class="code-language">${markdown.utils.escapeHtml(displayLanguage)}</span>
        <div class="code-toolbar-actions">
          <button class="code-block-btn" type="button" title="复制代码" data-action="copy-code" data-code="${encodedCode}">
            ${COPY_ICON}
          </button>
          <button class="code-block-btn" type="button" title="引用代码" data-action="quote-code" data-code="${encodedCode}">
            ${QUOTE_ICON}
          </button>
        </div>
      </div>
      <pre class="hljs"><code class="hljs ${language ? `language-${language}` : ''}">${highlighted}</code></pre>
    </div>
  `
}

const router = useRouter()

const LS_KB_ID = 'rag_chat_kb_id'
const LS_AGENT_ID = 'rag_chat_agent_id'
const LS_CUSTOM_AGENT_ID = 'rag_chat_custom_agent_id'
const LS_DEEP_THINKING = 'rag_chat_deep_thinking'
const LS_SESSION_ID = 'rag_chat_session_id'
const LS_PAGE_STYLE = 'rag_chat_page_style'
const LS_FONT_SIZE = 'rag_chat_font_size'
const FONT_SIZES = [12, 14, 16, 17, 18, 19, 20, 22, 24]

/**
 * 是否 Pro 商业版构建.
 * <p>Pro 端 vite.config.ts 通过 {@code define} 注入 {@code import.meta.env.VITE_IS_PRO=true},
 * community build 默认 undefined / false, 公告入口不渲染. 公告路由由 Pro 端 addRoute 注入,
 * community build 时该路由不存在, 即使误渲染也会 404 (双重防护).</p>
 */
const isProEdition = import.meta.env.VITE_IS_PRO === 'true'

function lsGet(key: string): string | null { try { return localStorage.getItem(key) } catch { return null } }
function lsSet(key: string, val: string) { try { localStorage.setItem(key, val) } catch {} }
function lsRemove(key: string) { try { localStorage.removeItem(key) } catch {} }

function restoreFromCache() {
  if (kbSelectConfig.value?.mode !== 'FIXED') {
    const cachedKbId = lsGet(LS_KB_ID)
    if (cachedKbId !== null) {
      const id = Number(cachedKbId)
      if (knowledgeBases.value.some(kb => kb.id === id)) {
        selectedKnowledgeBaseId.value = id
      }
    }
  }
  const cachedAgentId = lsGet(LS_AGENT_ID)
  if (cachedAgentId !== null) {
    const id = Number(cachedAgentId)
    if (agents.value.some(a => a.id === id)) {
      selectedAgentId.value = id
    } else {
      selectedAgentId.value = null
    }
  } else {
    selectedAgentId.value = null
  }
  // 恢复自定义智能体选择
  const cachedCustomAgentId = lsGet(LS_CUSTOM_AGENT_ID)
  if (cachedCustomAgentId !== null) {
    const id = Number(cachedCustomAgentId)
    if (customAgents.value.some(a => a.id === id)) {
      activeCustomAgentId.value = id
      selectedAgentId.value = null
    } else {
      activeCustomAgentId.value = null
    }
  } else {
    activeCustomAgentId.value = null
  }
  const cachedDeepThinking = lsGet(LS_DEEP_THINKING)
  if (cachedDeepThinking !== null) {
    deepThinking.value = cachedDeepThinking === 'true'
  }
  const cachedSessionId = lsGet(LS_SESSION_ID)
  if (cachedSessionId !== null) {
    const id = Number(cachedSessionId)
    if (sessions.value.some(s => s.id === id)) {
      selectedSessionId.value = id
    }
  }
}

const sessions = ref<any[]>([])
const knowledgeBases = ref<any[]>([])
/** C 端知识库选择配置: FIXED=隐藏下拉, SELECTABLE=显示下拉, EMPTY=无可用 */
const kbSelectConfig = ref<{ mode: string; defaultKb: any; options: any[] }>({ mode: 'EMPTY', defaultKb: null, options: [] })
const agents = ref<any[]>([])
const selectedKnowledgeBaseId = ref<number | null>(null)
const selectedAgentId = ref<number | null>(null)
const selectedSessionId = ref<number | null>(null)
const messages = ref<Msg[]>([])
const question = ref('')
const streaming = ref(false)
const messageScroller = ref<HTMLDivElement | null>(null)
let currentAbortController: AbortController | null = null
const creating = ref(false)
const showSidebar = ref(true)
const deepThinking = ref(true)
const pageStyle = ref<'standard' | 'wide'>(lsGet(LS_PAGE_STYLE) === 'wide' ? 'wide' : 'standard')
function getInitialFontSize(): number {
  try {
    const cached = lsGet(LS_FONT_SIZE)
    if (cached) {
      const num = parseInt(cached, 10)
      if (!isNaN(num) && FONT_SIZES.includes(num)) return num
    }
  } catch {}
  return 16
}
const fontSize = ref<number>(getInitialFontSize())
const fontSizeIndex = computed<number>(() => {
  const i = FONT_SIZES.indexOf(fontSize.value)
  return i >= 0 ? i : 0
})
const thumbLeft = computed<number>(() => (fontSizeIndex.value / (FONT_SIZES.length - 1)) * 100)
const sliderRef = ref<HTMLElement | null>(null)
let dragging = false
function updateFromX(clientX: number) {
  const el = sliderRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const x = clientX - rect.left - 9
  const w = rect.width - 18
  if (w <= 0) return
  const ratio = Math.max(0, Math.min(1, x / w))
  const index = Math.round(ratio * (FONT_SIZES.length - 1))
  fontSize.value = FONT_SIZES[index ?? 0] ?? 0
}
function onTrackMouseDown(e: MouseEvent) {
  updateFromX(e.clientX)
  dragging = true
  window.addEventListener('mousemove', onWinMove)
  window.addEventListener('mouseup', onWinUp)
}
function onThumbMouseDown(_e: MouseEvent) {
  dragging = true
  window.addEventListener('mousemove', onWinMove)
  window.addEventListener('mouseup', onWinUp)
}
function onWinMove(e: MouseEvent) { if (dragging) updateFromX(e.clientX) }
function onWinUp() {
  dragging = false
  window.removeEventListener('mousemove', onWinMove)
  window.removeEventListener('mouseup', onWinUp)
}
const showSettingsMenu = ref(false)
const preferencesVisible = ref(false)
const hoveredSessionId = ref<number[]>([])
const openDropdownSessionId = ref<number | null>(null)
const quotedText = ref('')
const quoteSource = ref('')

// ============ 智能体 (内置 + 自定义) ============
const agentPopoverVisible = ref(false)
const customAgents = ref<any[]>([])
const activeCustomAgentId = ref<number | null>(null)
const customAgentDialogVisible = ref(false)
const customAgentDialogMode = ref<'create' | 'edit'>('create')
const editingCustomAgent = ref<any | null>(null)
const customAgentForm = reactive({ agentName: '', content: '' })
const customAgentSaving = ref(false)
const builtinAgents = ref<any[]>([])

const activeAgentName = computed(() => {
  if (activeCustomAgentId.value) {
    const c = customAgents.value.find((a: any) => a.id === activeCustomAgentId.value)
    return c ? c.agentName : '默认'
  }
  if (selectedAgentId.value) {
    const b = agents.value.find((a: any) => a.id === selectedAgentId.value)
    return b ? (b.agentName || b.name) : '默认'
  }
  return '默认'
})

async function loadCustomAgents() {
  try {
    customAgents.value = (await userApi.listCustomAgents()) || []
  } catch (e) {
    console.warn('[ChatHome] load custom agents failed', e)
    customAgents.value = []
  }
}
async function loadBuiltinAgents() {
  try {
    const list = (await userApi.listAgents()) || []
    // C 端用户对系统智能体透明: 仅展示 is_system=0 (内建用户智能体), is_system=1 一律过滤
    builtinAgents.value = (list || []).filter((a: any) => Number(a.isSystem ?? a.is_system ?? 0) === 0)
  } catch (e) {
    console.warn('[ChatHome] load builtin agents failed', e)
    builtinAgents.value = []
  }
}

function selectBuiltinAgent(a: any) {
  activeCustomAgentId.value = null
  selectedAgentId.value = a?.id ?? null
  lsSet(LS_AGENT_ID, a?.id ? String(a.id) : '')
  lsRemove(LS_CUSTOM_AGENT_ID)
  agentPopoverVisible.value = false
  ElMessage.success(`已切换: ${a?.agentName || a?.name}`)
}
function selectCustomAgent(a: any) {
  activeCustomAgentId.value = a.id
  selectedAgentId.value = null
  lsSet(LS_CUSTOM_AGENT_ID, String(a.id))
  lsRemove(LS_AGENT_ID)
  agentPopoverVisible.value = false
  ElMessage.success(`已激活: ${a.agentName}`)
}

function openCreateCustomAgent() {
  customAgentDialogMode.value = 'create'
  editingCustomAgent.value = null
  customAgentForm.agentName = ''
  customAgentForm.content = ''
  customAgentDialogVisible.value = true
  agentPopoverVisible.value = false
}
function openEditCustomAgent(a: any) {
  customAgentDialogMode.value = 'edit'
  editingCustomAgent.value = a
  customAgentForm.agentName = a.agentName || ''
  customAgentForm.content = a.content || ''
  customAgentDialogVisible.value = true
  agentPopoverVisible.value = false
}
async function saveCustomAgent() {
  if (!customAgentForm.agentName.trim()) { ElMessage.warning('请输入名称'); return }
  if (!customAgentForm.content.trim()) { ElMessage.warning('请输入提示词'); return }
  customAgentSaving.value = true
  try {
    if (customAgentDialogMode.value === 'create') {
      await userApi.createCustomAgent({ ...customAgentForm })
      ElMessage.success('创建成功')
    } else if (editingCustomAgent.value) {
      await userApi.updateCustomAgent(editingCustomAgent.value.id, { ...customAgentForm })
      ElMessage.success('更新成功')
    }
    customAgentDialogVisible.value = false
    await loadCustomAgents()
  } catch (e: any) {
    ElMessage.error(extractErrorMessage(e, '保存失败'))
  } finally {
    customAgentSaving.value = false
  }
}
async function deleteCustomAgent() {
  if (!editingCustomAgent.value) return
  try {
    await ElMessageBox.confirm(`确定删除智能体 [${editingCustomAgent.value.agentName}] 吗?`, '删除智能体',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' })
  } catch { return }
  try {
    await userApi.deleteCustomAgent(editingCustomAgent.value.id)
    ElMessage.success('已删除')
    customAgentDialogVisible.value = false
    if (activeCustomAgentId.value === editingCustomAgent.value.id) {
      activeCustomAgentId.value = null
    }
    await loadCustomAgents()
  } catch (e: any) {
    ElMessage.error(extractErrorMessage(e, '删除失败'))
  }
}

interface Msg {
  id: number
  roleType: string
  messageContent: string
  thinkingContent: string
  deepThinkingEnabled: boolean
  thinkingActive: boolean
  thinkingStartTime: number | null
  thinkingElapsedSec: number | null
  thinkingExpanded: boolean
  pending: boolean
  attachments?: AttachmentItem[]
  // 临时字段: 跨 chunk 缓存未匹配 <think>/</think> 标签前缀的内容
  thinkParseBuf?: string
  thinkParseState?: ThinkParseState
}

// 思考标签解析状态机
type ThinkParseState = 'NORMAL' | 'IN_THINK' | 'AFTER_THINK'
const THINK_OPEN = '<think>'
const THINK_CLOSE = '</think>'

/**
 * 解析 delta chunk 中的 <think>...</think> 标签.
 * <p>根据 deepThinking 开关决定是渲染思考内容还是直接跳过.
 * <p>chunk 可能跨多个 SSE 事件, 内部用 buffer 缓存未匹配的标签前缀.
 */
function processDeltaChunk(msg: Msg, chunk: string, deepThinking: boolean) {
  msg.thinkParseBuf = (msg.thinkParseBuf || '') + chunk

  while (msg.thinkParseBuf.length > 0) {
    if (msg.thinkParseState === 'NORMAL') {
      const idx = msg.thinkParseBuf.indexOf(THINK_OPEN)
      if (idx === -1) {
        // 没有 <think>, 检查末尾是否是 <think> 的前缀
        const partial = trailingPartialTag(msg.thinkParseBuf, THINK_OPEN)
        if (partial > 0) {
          msg.messageContent += msg.thinkParseBuf.substring(0, msg.thinkParseBuf.length - partial)
          msg.thinkParseBuf = msg.thinkParseBuf.substring(msg.thinkParseBuf.length - partial)
          return
        }
        msg.messageContent += msg.thinkParseBuf
        msg.thinkParseBuf = ''
        return
      }
      msg.messageContent += msg.thinkParseBuf.substring(0, idx)
      msg.thinkParseBuf = msg.thinkParseBuf.substring(idx + THINK_OPEN.length)
      msg.thinkParseState = 'IN_THINK'
      if (deepThinking) {
        msg.thinkingActive = true
        if (!msg.thinkingStartTime) msg.thinkingStartTime = Date.now()
      }
    } else if (msg.thinkParseState === 'IN_THINK') {
      const idx = msg.thinkParseBuf.indexOf(THINK_CLOSE)
      if (idx === -1) {
        const partial = trailingPartialTag(msg.thinkParseBuf, THINK_CLOSE)
        if (partial > 0) {
          if (deepThinking) {
            msg.thinkingContent += msg.thinkParseBuf.substring(0, msg.thinkParseBuf.length - partial)
          }
          msg.thinkParseBuf = msg.thinkParseBuf.substring(msg.thinkParseBuf.length - partial)
          return
        }
        if (deepThinking) {
          msg.thinkingContent += msg.thinkParseBuf
        }
        msg.thinkParseBuf = ''
        return
      }
      if (deepThinking) {
        msg.thinkingContent += msg.thinkParseBuf.substring(0, idx)
      }
      msg.thinkParseBuf = msg.thinkParseBuf.substring(idx + THINK_CLOSE.length)
      msg.thinkParseState = 'AFTER_THINK'
      msg.thinkingActive = false
      if (msg.thinkingStartTime) {
        msg.thinkingElapsedSec = Math.max(1, Math.round((Date.now() - msg.thinkingStartTime) / 1000))
      }
    } else {
      // AFTER_THINK: 后续内容全部进 messageContent
      msg.messageContent += msg.thinkParseBuf
      msg.thinkParseBuf = ''
      return
    }
  }
}

/** 返回 text 末尾与 tag 前缀匹配的最长字符数, 用于跨 chunk 标签检测 */
function trailingPartialTag(text: string, tag: string): number {
  const max = Math.min(text.length, tag.length - 1)
  for (let len = max; len > 0; len--) {
    if (text.endsWith(tag.substring(0, len))) return len
  }
  return 0
}

interface AttachmentItem {
  id: string
  name: string
  type: string
  url: string
  size: number
  thumbnailUrl?: string
  base64Data?: string
}

const SUPPORTED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/gif', 'image/webp', 'image/bmp']
const SUPPORTED_FILE_TYPES = ['text/plain', 'text/csv', 'text/markdown', 'application/pdf', 'application/json']

const pendingAttachments = ref<AttachmentItem[]>([])
const modelSupportsMultimodal = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const imagePreviewVisible = ref(false)
const imagePreviewUrl = ref('')
const isDragging = ref(false)

const quotedLabel = computed(() => {
  if (!quotedText.value) return ''
  const src = quoteSource.value ? `${quoteSource.value}：` : ''
  const preview = quotedText.value.length > 60 ? quotedText.value.slice(0, 60) + '…' : quotedText.value
  return `${src}${preview}`
})

function dedupeSessions(list: any[]) {
  const seen = new Set<number>()
  const result: any[] = []
  for (const item of list ?? []) {
    const id = Number(item?.id)
    if (!Number.isFinite(id) || seen.has(id)) continue
    seen.add(id)
    result.push(item)
  }
  return result
}

async function syncSessions(preferredSessionId?: number | null, skipAutoSelect?: boolean) {
  const list = dedupeSessions(await userApi.listSessions())
  sessions.value = list
  if (skipAutoSelect) return
  const hasCurrent = selectedSessionId.value != null && list.some(item => item.id === selectedSessionId.value)
  const nextSelectedId =
    preferredSessionId && list.some(item => item.id === preferredSessionId)
      ? preferredSessionId
      : hasCurrent
        ? selectedSessionId.value
        : (list[0]?.id ?? null)

  if (nextSelectedId) {
    await selectSession(nextSelectedId)
  } else {
    selectedSessionId.value = null
    messages.value = []
  }
}

async function scrollToBottom() {
  await nextTick()
  const el = messageScroller.value
  if (el) el.scrollTop = el.scrollHeight
}

onMounted(async () => {
  // 工具: 延迟
  const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))
  // 如果是从 LoginView 跳过来的 (新 session 刚建立), 多等一会再发请求
  // 解决"已登录浏览器残留旧 cookie"导致的 ChatHomeView 首次 401
  const fromLogin = sessionStorage.getItem('jc_rag_session_ready')
  if (fromLogin) {
    const ts = parseInt(fromLogin, 10)
    const elapsed = Date.now() - ts
    if (elapsed < 1500) {
      const wait = 1500 - elapsed
      console.log('[ChatHome] from-login detected, wait', wait, 'ms for cookie to stabilize')
      await sleep(wait)
    }
    sessionStorage.removeItem('jc_rag_session_ready')
  }
  // 工具: 带 retry 的 currentUser, 处理冷启动 cookie 同步慢导致首次 401
  const currentUserWithRetry = async (maxRetry = 2) => {
    let lastErr: any
    for (let i = 0; i <= maxRetry; i++) {
      try {
        const u = await userApi.currentUser()
        if (i > 0) console.log('[ChatHome] currentUser succeeded after', i, 'retry')
        return u
      } catch (err: any) {
        lastErr = err
        // 仅在 401 时 retry (cookie 未同步), 其他错误直接抛
        const status = err?.response?.status
        const is401 = status === 401
        if (!is401) throw err
        if (i < maxRetry) {
          console.warn('[ChatHome] currentUser 401, retry in 500ms (cold start cookie sync)')
          await sleep(500)
        }
      }
    }
    throw lastErr
  }
  try {
    // 串行: currentUser 先于一切, 避免冷启动时其他接口 cookie 还没同步
    // 冷启动时第一次可能 401 (cookie 未同步), retry 最多 2 次, 间隔 500ms
    const user = await currentUserWithRetry(2)
    console.log('[ChatHome] /auth/me response:', user)
    if (!user.authenticated) { console.log('[ChatHome] not authenticated, redirect'); router.push('/login'); return }
    // 串行拉基础数据: 任一失败立即弹错, 不静默
    try {
      knowledgeBases.value = await userApi.listKnowledgeBases()
      console.log('[ChatHome] listKnowledgeBases ok, count=', knowledgeBases.value.length)
    } catch (e) {
      console.error('[ChatHome] listKnowledgeBases failed:', e)
      throw e
    }
    try {
      kbSelectConfig.value = await userApi.getKbSelectConfig() || { mode: 'EMPTY', defaultKb: null, options: [] }
      console.log('[ChatHome] getKbSelectConfig ok, mode=', kbSelectConfig.value?.mode)
    } catch (e) {
      console.warn('[ChatHome] getKbSelectConfig failed, fallback:', e)
      kbSelectConfig.value = { mode: 'EMPTY', defaultKb: null, options: knowledgeBases.value }
    }
    try {
      agents.value = await userApi.listAgents()
      console.log('[ChatHome] listAgents ok, count=', agents.value.length)
    } catch (e) {
      console.error('[ChatHome] listAgents failed:', e)
      throw e
    }
    // 自建 + 内建 独立 try, 单个失败不影响主流程
    try { await loadCustomAgents() } catch (e) { console.warn('[ChatHome] loadCustomAgents failed:', e) }
    try { await loadBuiltinAgents() } catch (e) { console.warn('[ChatHome] loadBuiltinAgents failed:', e) }
    try {
      await syncSessions(null, true)
      console.log('[ChatHome] syncSessions ok, count=', sessions.value.length)
    } catch (e) {
      console.error('[ChatHome] syncSessions failed:', e)
      throw e
    }
    restoreFromCache()
    if (kbSelectConfig.value?.mode === 'FIXED') {
      selectedKnowledgeBaseId.value = kbSelectConfig.value?.defaultKb?.id ?? null
      lsRemove(LS_KB_ID)
    }
    // C 端可选 KB: 优先用后端配置 (config.options), 兼容旧接口
    if (!selectedKnowledgeBaseId.value) {
      const cfgOptions = kbSelectConfig.value?.options?.length
        ? kbSelectConfig.value.options
        : knowledgeBases.value
      selectedKnowledgeBaseId.value =
        kbSelectConfig.value?.defaultKb?.id
        ?? cfgOptions.find(item => item.isDefault)?.id
        ?? cfgOptions[0]?.id
        ?? null
    }
    if (!selectedSessionId.value && sessions.value.length > 0) {
      selectedSessionId.value = sessions.value[0].id
    }
    if (selectedSessionId.value && sessions.value.some(s => s.id === selectedSessionId.value)) {
      await selectSession(selectedSessionId.value)
    }
    checkMultimodalSupport()
    console.log('[ChatHome] onMounted done. agents=', agents.value.length, 'builtinAgents=', builtinAgents.value.length, 'customAgents=', customAgents.value.length, 'sessions=', sessions.value.length, 'kbs=', knowledgeBases.value.length)
  } catch (error) {
    console.error('[ChatHome] onMounted error:', error)
    if (isUnauthorizedError(error)) {
      router.push('/login')
      return
    }
    ElMessage.error(extractErrorMessage(error, '加载会话失败'))
  }
})

// keep-alive 激活时: 如果核心数据为空 (典型场景: 退出登录后重新登录, 组件被缓存激活), 重新加载
onActivated(async () => {
  console.log('[ChatHome] onActivated, sessions=', sessions.value.length, 'kbs=', knowledgeBases.value.length, 'agents=', agents.value.length)
  if (sessions.value.length === 0 && knowledgeBases.value.length === 0) {
    console.log('[ChatHome] onActivated: data empty (kept-alive from previous session), refreshing...')
    // 重新走一次"基础数据"流程
    try {
      const user = await userApi.currentUser()
      if (!user.authenticated) { router.push('/login'); return }
      knowledgeBases.value = await userApi.listKnowledgeBases()
      kbSelectConfig.value = await userApi.getKbSelectConfig() || { mode: 'EMPTY', defaultKb: null, options: [] }
      agents.value = await userApi.listAgents()
      try { await loadCustomAgents() } catch (e) { console.warn('[ChatHome] onActivated loadCustomAgents failed:', e) }
      try { await loadBuiltinAgents() } catch (e) { console.warn('[ChatHome] onActivated loadBuiltinAgents failed:', e) }
      await syncSessions(null, true)
      restoreFromCache()
      if (kbSelectConfig.value?.mode === 'FIXED') {
        selectedKnowledgeBaseId.value = kbSelectConfig.value?.defaultKb?.id ?? null
        lsRemove(LS_KB_ID)
      }
      // 重新选 KB / session
      if (!selectedKnowledgeBaseId.value) {
        const cfgOptions = kbSelectConfig.value?.options?.length ? kbSelectConfig.value.options : knowledgeBases.value
        selectedKnowledgeBaseId.value = kbSelectConfig.value?.defaultKb?.id
          ?? cfgOptions.find(item => item.isDefault)?.id
          ?? cfgOptions[0]?.id
          ?? null
      }
      if (!selectedSessionId.value && sessions.value.length > 0) {
        selectedSessionId.value = sessions.value[0].id
      }
      if (selectedSessionId.value && sessions.value.some(s => s.id === selectedSessionId.value)) {
        await selectSession(selectedSessionId.value)
      }
      console.log('[ChatHome] onActivated: refresh done. sessions=', sessions.value.length)
    } catch (e) {
      console.error('[ChatHome] onActivated refresh failed:', e)
    }
  }
})

watch(selectedKnowledgeBaseId, (v) => { if (v != null) { lsSet(LS_KB_ID, String(v)); } else { lsRemove(LS_KB_ID); } })
watch(selectedAgentId, (v) => { if (v != null) { lsSet(LS_AGENT_ID, String(v)); } else { lsRemove(LS_AGENT_ID); } })
watch(activeCustomAgentId, (v) => { if (v != null) { lsSet(LS_CUSTOM_AGENT_ID, String(v)); } else { lsRemove(LS_CUSTOM_AGENT_ID); } })
watch(deepThinking, (v) => { lsSet(LS_DEEP_THINKING, String(v)); })
watch(pageStyle, (v) => { lsSet(LS_PAGE_STYLE, v); })
watch(fontSize, (v) => { lsSet(LS_FONT_SIZE, String(v)); })
watch(selectedSessionId, (v) => { if (v != null) { lsSet(LS_SESSION_ID, String(v)); } else { lsRemove(LS_SESSION_ID); } })
// 多模态检测在 onMounted 中调用，不再依赖 Agent/知识库切换

async function selectSession(id: number) {
  if (!id) {
    selectedSessionId.value = null
    messages.value = []
    return
  }
  selectedSessionId.value = id
  const session = sessions.value.find(s => s.id === id)
  if (kbSelectConfig.value?.mode === 'FIXED') {
    selectedKnowledgeBaseId.value = kbSelectConfig.value?.defaultKb?.id ?? selectedKnowledgeBaseId.value
  } else if (session?.kbId) {
    selectedKnowledgeBaseId.value = session.kbId
  }
  const loaded = await userApi.getMessages(id)
  // 后端已持久化附件（含 base64），直接映射为前端格式
  for (const m of loaded) {
    if (m.attachments?.length) {
      m.attachments = m.attachments.map((att: any) => ({
        id: att.id || Math.random().toString(36).slice(2),
        name: att.name || '',
        type: att.type || '',
        size: att.size || 0,
        thumbnailUrl: att.base64Data || att.thumbnailUrl || '',
        url: att.base64Data || att.url || '',
        base64Data: att.base64Data || '',
      }))
    }
  }
  messages.value = loaded
  await scrollToBottom()
}

async function createSession() {
  creating.value = true
  try {
    const created = await userApi.createSession({
      kbId: selectedKnowledgeBaseId.value,
    })
    await syncSessions(created.id)
    ElMessage.success('新会话已创建')
  } catch { ElMessage.error('创建失败') }
  finally { creating.value = false }
}

async function removeSession(id: number) {
  try {
    await userApi.deleteSession(id)
    const nextSessionId = selectedSessionId.value === id
      ? sessions.value.find(item => item.id !== id)?.id ?? null
      : selectedSessionId.value
    await syncSessions(nextSessionId)
    ElMessage.success('会话已删除')
  } catch { ElMessage.error('删除失败') }
}

async function renameSession(session: any) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的会话名称', '重命名会话', {
      inputValue: session.sessionTitle || '',
      inputPlaceholder: '请输入会话名称',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValidator: (value) => value.trim() ? true : '会话名称不能为空',
    })
    await userApi.renameSession(session.id, { sessionTitle: value.trim() })
    await syncSessions(session.id)
    ElMessage.success('已重命名')
  } catch {}
}

async function togglePinSession(session: any) {
  try {
    await userApi.pinSession(session.id, !session.isPinned)
    await syncSessions(session.id)
    ElMessage.success(session.isPinned ? '已取消置顶' : '已置顶')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleSessionCommand(command: any, session: { id: number; isPinned?: boolean; sessionTitle?: string }) {
  if (command === 'rename') {
    await renameSession(session)
    return
  }
  if (command === 'pin') {
    await togglePinSession(session)
    return
  }
  if (command === 'delete') {
    await removeSession(session.id)
  }
}

function copyMessage(text: string) {
  const plain = stripHtmlTags(text)
  navigator.clipboard.writeText(plain).then(
    () => ElMessage.success('已复制到剪贴板'),
    () => ElMessage.error('复制失败'),
  )
}

function quoteMessage(text: string, roleType: string) {
  const plain = stripHtmlTags(text)
  quotedText.value = plain
  quoteSource.value = roleType === 'USER' ? '用户提问' : 'AI 回答'
  ElMessage.success('已引用，发送时将附带引用内容')
}

function clearQuote() {
  quotedText.value = ''
  quoteSource.value = ''
}

function stripHtmlTags(text: string) {
  return String(text ?? '').replace(/<[^>]*>/g, '')
}

function onChatAreaClick(event: MouseEvent) {
  const target = event.target instanceof HTMLElement ? event.target.closest('[data-action]') as HTMLElement | null : null
  if (!target) return
  const action = target.dataset.action
  const code = decodeURIComponent(target.dataset.code ?? '')
  if (!code) return
  if (action === 'copy-code') {
    copyMessage(code)
  } else if (action === 'quote-code') {
    quoteMessage(code, 'ASSISTANT')
  }
}

async function onAsk() {
  if (!selectedSessionId.value || !question.value.trim()) { ElMessage.warning('请先选择会话并输入问题'); return }
  if (streaming.value) return

  let finalQuestion = question.value.trim()
  if (quotedText.value) {
    finalQuestion = `【引用内容】\n${quotedText.value}\n\n【当前问题】\n${finalQuestion}`
    clearQuote()
  }

  question.value = ''
  const q = finalQuestion
  const currentAttachments = [...pendingAttachments.value]
  pendingAttachments.value = []
  const userId = -Date.now()
  messages.value.push({ id: userId, roleType: 'USER', messageContent: q, thinkingContent: '', deepThinkingEnabled: false, thinkingActive: false, thinkingStartTime: null, thinkingElapsedSec: null, thinkingExpanded: true, pending: false, attachments: currentAttachments, thinkParseBuf: '', thinkParseState: 'NORMAL' })
  const assistantId = userId - 1
  const assistantMsg = reactive<Msg>({
    id: assistantId, roleType: 'ASSISTANT',
    messageContent: '', thinkingContent: '',
    deepThinkingEnabled: deepThinking.value,
    // 初始 thinkingActive = false, 避免非思考模型显示"思考中..."
    // 只有真正解析到 <think> 标签时, processDeltaChunk 才会把它置为 true
    thinkingActive: false, thinkingStartTime: null,
    thinkingElapsedSec: null, thinkingExpanded: true,
    pending: true,
    thinkParseBuf: '', thinkParseState: 'NORMAL',
  })
  messages.value.push(assistantMsg)
  streaming.value = true
  currentAbortController = new AbortController()
  await scrollToBottom()

  const finalizeThinking = () => {
    assistantMsg.thinkingActive = false
    if (assistantMsg.deepThinkingEnabled && assistantMsg.thinkingStartTime && !assistantMsg.thinkingElapsedSec)
      assistantMsg.thinkingElapsedSec = Math.max(1, Math.round((Date.now() - assistantMsg.thinkingStartTime) / 1000))
    // 思考结束后自动折叠
    if (!assistantMsg.thinkingActive) assistantMsg.thinkingExpanded = false
  }

  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null

  try {
    // 构建附件数据（仅发送必要字段）
    const attachmentsPayload = currentAttachments.map(a => ({
      name: a.name, type: a.type, base64Data: a.base64Data,
    })).filter(a => a.base64Data)

    const response = await fetch(`${userApiBase}/chat/sessions/${selectedSessionId.value}/ask/stream`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      signal: currentAbortController.signal,
      body: JSON.stringify({ question: q, deepThinking: deepThinking.value, agentId: selectedAgentId.value, customAgentId: activeCustomAgentId.value, kbId: selectedKnowledgeBaseId.value, attachments: attachmentsPayload }),
    })
    if (!response.ok) {
      let errMsg = '提问失败'
      try { const err = await response.json(); errMsg = err.error || err.message || errMsg } catch {}
      if (response.status === 401) errMsg = '登录已失效，请重新登录'
      if (response.status === 429) errMsg = '提问今日次数已达限制，请配置私有 API Keys 继续使用'
      assistantMsg.messageContent = errMsg; assistantMsg.pending = false; assistantMsg.thinkingActive = false
      streaming.value = false
      currentAbortController = null
      await scrollToBottom()
      ElMessage.error(errMsg)
      return
    }
    if (!response.body) throw new Error('stream not supported')

    reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buf = ''
    let isDone = false

    while (!isDone) {
      const { value, done } = await reader!.read()
      if (done) {
        finalizeThinking()
        break
      }
      buf += decoder.decode(value, { stream: true })
      const parts = buf.split('\n\n')
      buf = parts.pop() || ''

      for (const block of parts) {
        for (const line of block.split('\n')) {
          const trimmed = line.trim()
          if (!trimmed.startsWith('data:')) continue
          try {
            const parsed = JSON.parse(trimmed.slice(5).trim())
            if (parsed.error) {
              finalizeThinking()
              assistantMsg.pending = false
              // AGENT_NOT_AVAILABLE (code 2001) 给用户友好提示, 其他错误原样展示
              const userMsg = parsed.code === 2001 ? (parsed.error || '暂无可用智能体, 请联系管理员') : (parsed.error || '出错了')
              assistantMsg.messageContent = userMsg
              streaming.value = false
              currentAbortController = null
              await scrollToBottom()
              reader!.releaseLock()
              reader = null
              ElMessage.warning(userMsg)
              return
            }
            if (parsed.type === 'thinking') {
              // Python 预热事件: 标记请求已开始计时, 但不要直接置 thinkingActive=true.
              // 真正进入"思考中"需要 delta 中出现 <think> 标签 (processDeltaChunk 内部处理),
              // 避免非思考模型也闪一下"思考中..."
              if (!assistantMsg.thinkingStartTime) assistantMsg.thinkingStartTime = Date.now()
            } else if (parsed.type === 'delta') {
              const chunk = parsed.delta || ''
              if (chunk) {
                processDeltaChunk(assistantMsg, chunk, deepThinking.value)
                // 拿到第一个非 think 内容就停止 thinkingActive 状态
                if (assistantMsg.thinkingActive && (assistantMsg.messageContent || assistantMsg.thinkParseState === 'AFTER_THINK')) {
                  assistantMsg.thinkingActive = false
                  if (assistantMsg.thinkingStartTime && !assistantMsg.thinkingElapsedSec)
                    assistantMsg.thinkingElapsedSec = Math.max(1, Math.round((Date.now() - assistantMsg.thinkingStartTime) / 1000))
                }
              }
              assistantMsg.pending = false
            } else if (parsed.type === 'meta') {
              // 更新 USER 消息的真实 id
              if (parsed.userMessageId && userId < 0) {
                const userMsg = messages.value.find(m => m.id === userId)
                if (userMsg) userMsg.id = parsed.userMessageId
              }
              if (parsed.sessionTitle && selectedSessionId.value) {
                const session = sessions.value.find(s => s.id === selectedSessionId.value)
                if (session) {
                  session.sessionTitle = parsed.sessionTitle
                }
              }
            }
            if (parsed.type === 'done') {
              finalizeThinking()
              isDone = true
              break
            }
          } catch {}
        }
        if (isDone) break
      }
      await scrollToBottom()
    }

    if (selectedSessionId.value) {
      // 只刷新会话列表，不重新加载消息（避免覆盖本地附件数据）
      const list = dedupeSessions(await userApi.listSessions())
      sessions.value = list
    }
  } catch (e: any) {
    if (e?.name === 'AbortError') {
      // 用户主动中止, 不当作错误, 用省略号提示
      if (!assistantMsg.messageContent) assistantMsg.messageContent = '已中止'
    } else {
      finalizeThinking()
      assistantMsg.pending = false
      assistantMsg.messageContent = e.message || '提问失败'
      await scrollToBottom()
      ElMessage.error(assistantMsg.messageContent)
    }
  } finally {
    if (reader) {
      try { reader.releaseLock() } catch {}
    }
    streaming.value = false
    currentAbortController = null
  }
}

/** 中止当前流式响应 */
function abortAsk() {
  if (currentAbortController) {
    try { currentAbortController.abort() } catch {}
    currentAbortController = null
  }
  streaming.value = false
}

function renderMd(text: string) {
  if (!text) return ''
  try {
    return markdown.render(text)
  } catch {
    return text
  }
}

function logout() { router.push('/login') }

function onSettingsCommand(command: string) {
  console.log('[preferences-debug] onSettingsCommand:', command)
  if (command === 'api-keys') { router.push('/api-keys') }
  if (command === 'my-memory') { router.push('/my-memory') }
  if (command === 'preferences') {
    console.log('[preferences-debug] before set, preferencesVisible=', preferencesVisible.value)
    preferencesVisible.value = true
    console.log('[preferences-debug] after set, preferencesVisible=', preferencesVisible.value)
  }
}

async function checkMultimodalSupport() {
  try {
    const info = await userApi.getModelInfo()
    modelSupportsMultimodal.value = Boolean(info?.supportsVision)
  } catch {
    modelSupportsMultimodal.value = false
  }
}

function isSupportedFileType(fileType: string): boolean {
  return SUPPORTED_IMAGE_TYPES.includes(fileType) || SUPPORTED_FILE_TYPES.includes(fileType)
}

function isImageType(fileType: string): boolean {
  return SUPPORTED_IMAGE_TYPES.includes(fileType)
}

function addFileAsAttachment(file: File) {
  if (!modelSupportsMultimodal.value) {
    ElMessage.warning('当前模型不支持多模态，无法上传文件或图片')
    return
  }
  if (!isSupportedFileType(file.type)) {
    ElMessage.warning(`不支持的文件格式：${file.type || '未知'}，仅支持常见图片和文本文件`)
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 20MB')
    return
  }
  const id = `att-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const url = URL.createObjectURL(file)
  const attachment: AttachmentItem = {
    id, name: file.name, type: file.type, url, size: file.size,
    thumbnailUrl: isImageType(file.type) ? url : undefined,
  }
  // 读取 base64 数据用于发送给后端
  const reader = new FileReader()
  reader.onload = () => {
    attachment.base64Data = reader.result as string
  }
  reader.readAsDataURL(file)
  pendingAttachments.value.push(attachment)
}

function removeAttachment(id: string) {
  const idx = pendingAttachments.value.findIndex(a => a.id === id)
  const att = idx >= 0 ? pendingAttachments.value[idx] : undefined
  if (att) {
    URL.revokeObjectURL(att.url)
    pendingAttachments.value.splice(idx, 1)
  }
}

function triggerFileInput() {
  if (!modelSupportsMultimodal.value) {
    ElMessage.warning('当前模型不支持多模态，无法上传文件或图片')
    return
  }
  fileInputRef.value?.click()
}

function onFileInputChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files) return
  for (const file of Array.from(input.files)) {
    addFileAsAttachment(file)
  }
  input.value = ''
}

function onComposerDragOver(e: DragEvent) {
  e.preventDefault()
  isDragging.value = true
}

function onComposerDragLeave(e: DragEvent) {
  e.preventDefault()
  isDragging.value = false
}

function onComposerDrop(e: DragEvent) {
  e.preventDefault()
  isDragging.value = false
  if (!modelSupportsMultimodal.value) {
    ElMessage.warning('当前模型不支持多模态，无法上传文件或图片')
    return
  }
  const files = e.dataTransfer?.files
  if (!files) return
  for (const file of Array.from(files)) {
    addFileAsAttachment(file)
  }
}

function onComposerPaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (!items) return
  let hasImage = false
  for (const item of Array.from(items)) {
    if (item.type.startsWith('image/')) {
      hasImage = true
      e.preventDefault()
      const file = item.getAsFile()
      if (file) addFileAsAttachment(file)
    }
  }
  if (hasImage && !modelSupportsMultimodal.value) {
    pendingAttachments.value = pendingAttachments.value.filter(a => {
      URL.revokeObjectURL(a.url)
      return false
    })
  }
}

function openImagePreview(url: string) {
  imagePreviewUrl.value = url
  imagePreviewVisible.value = true
}

function getFileIcon(type: string): string {
  if (type.includes('pdf')) return '📄'
  if (type.includes('json')) return '📋'
  if (type.includes('text') || type.includes('csv') || type.includes('markdown')) return '📝'
  return '📎'
}

function retryQuestion(text: string) {
  if (streaming.value) return
  question.value = text
  onAsk()
}
</script>

<template>
  <div class="c-shell" :style="{ '--chat-font-size': fontSize + 'px' }">
    <header class="c-header">
      <div class="c-header-left">
        <button class="menu-toggle" @click="showSidebar = !showSidebar">☰</button>
        <span class="logo">RAG 问答</span>
      </div>
      <nav class="c-header-nav">
        <el-dropdown trigger="click" @command="onSettingsCommand" @visible-change="(v: boolean) => showSettingsMenu = v">
          <button class="settings-btn" type="button" title="设置">
            <el-icon :class="{ 'settings-spin': showSettingsMenu }"><Setting /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu class="settings-dropdown-menu">
              <el-dropdown-item command="api-keys">
                <el-icon style="margin-right:8px"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/></svg></el-icon>
                API Keys
              </el-dropdown-item>
              <el-dropdown-item divided disabled>
                <span style="color:#6b7280;font-size:12px">页面样式</span>
              </el-dropdown-item>
              <div class="page-style-options">
                <div class="style-option" :class="{ active: pageStyle === 'standard' }" @click="pageStyle = 'standard'">
                  <div class="style-option-preview standard-preview">
                    <div class="preview-container">
                      <div class="preview-msg" style="width:55%"></div>
                      <div class="preview-msg" style="width:45%;margin-left:auto"></div>
                    </div>
                  </div>
                  <span class="style-option-label">标准模式</span>
                </div>
                <div class="style-option" :class="{ active: pageStyle === 'wide' }" @click="pageStyle = 'wide'">
                  <div class="style-option-preview wide-preview">
                    <div class="preview-container">
                      <div class="preview-msg" style="width:55%"></div>
                      <div class="preview-msg" style="width:45%;margin-left:auto"></div>
                    </div>
                  </div>
                  <span class="style-option-label">超宽模式</span>
                </div>
              </div>
              <el-dropdown-item divided disabled>
                <span style="color:#6b7280;font-size:12px">正文大小</span>
              </el-dropdown-item>
              <div class="font-size-options">
                <div class="font-size-slider-wrapper" ref="sliderRef" @mousedown="onTrackMouseDown">
                  <div class="font-size-track"></div>
                  <div class="font-size-ticks">
                    <span v-for="(size, i) in FONT_SIZES" :key="i" class="font-size-tick" :style="{ left: ((i / (FONT_SIZES.length - 1)) * 100) + '%' }" :title="size + 'px'" @mousedown.stop @click.stop="fontSize = size"></span>
                    <div class="font-size-thumb" :style="{ left: thumbLeft + '%' }" @mousedown.stop="onThumbMouseDown"></div>
                  </div>
                </div>
                <span class="font-size-value">{{ fontSize }}px</span>
              </div>
              <el-dropdown-item command="preferences" divided>
                <el-icon style="margin-right: 8px;"><Setting /></el-icon>
                偏好设置
              </el-dropdown-item>
              <el-dropdown-item command="my-memory" divided>
                <el-icon style="margin-right: 8px;"><DataLine /></el-icon>
                我的记忆
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <router-link v-if="isProEdition" to="/announcement">系统公告</router-link>
        <router-link to="/profile">个人中心</router-link>
        <a href="#" @click.prevent="logout">退出</a>
      </nav>
    </header>
    <div class="c-body">
      <aside class="c-sidebar" v-show="showSidebar">
        <div class="sidebar-section">
          <h3>新会话</h3>
          <el-select
            v-if="kbSelectConfig.mode === 'SELECTABLE'"
            v-model="selectedKnowledgeBaseId"
            size="default"
            placeholder="请选择知识库"
            class="kb-select"
          >
            <el-option
              v-for="kb in kbSelectConfig.options"
              :key="kb.id"
              :label="kb.kbName"
              :value="kb.id"
            />
          </el-select>
          <el-select v-model="selectedAgentId" size="default" placeholder="不指定Agent" clearable class="kb-select" style="display: none;">
            <el-option
              v-for="a in agents"
              :key="a.id"
              :label="a.agentName"
              :value="a.id"
            />
          </el-select>
          <div class="default-kb" v-if="kbSelectConfig.mode !== 'EMPTY'">
            当前会话知识库：{{ kbSelectConfig.options.find(item => item.id === selectedKnowledgeBaseId)?.kbName || kbSelectConfig.defaultKb?.kbName || knowledgeBases.find(item => item.id === selectedKnowledgeBaseId)?.kbName || '未配置' }}
          </div>
          <el-button type="primary" size="default" :loading="creating" @click="createSession" class="new-session-btn">新会话</el-button>
        </div>
        <div class="sidebar-section">
          <h3>会话列表</h3>
          <div class="session-list">
            <div v-for="s in sessions" :key="s.id" class="session-item" :class="{ active: s.id === selectedSessionId }" @click="selectSession(s.id)" @mouseenter="hoveredSessionId.push(s.id)" @mouseleave="hoveredSessionId = hoveredSessionId.filter(id => id !== s.id)">
              <div class="session-title">
                <span>{{ s.sessionTitle || '新会话' }}</span>
              </div>
              <div class="session-actions">
                <span v-if="s.isPinned && !hoveredSessionId.includes(s.id) && openDropdownSessionId !== s.id" class="session-pin-tag">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 17v5"/><path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76z"/></svg>
                </span>
                <el-dropdown v-else trigger="click" @visible-change="(visible: boolean) => { openDropdownSessionId = visible ? s.id : null }" @command="(command: string) => handleSessionCommand(command, s)">
                  <button class="session-more" type="button" @click.stop>
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="rename">
                        <el-icon style="margin-right: 8px;"><Edit /></el-icon>
                        重命名
                      </el-dropdown-item>
                      <el-dropdown-item command="pin">
                        <el-icon style="margin-right: 8px;">
                          <svg v-if="s.isPinned" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="17" x2="12" y2="22"/><line x1="2" y1="2" x2="22" y2="22"/><path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V17h3.5"/><path d="M18.5 17H19v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1"/></svg>
                          <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 17v5"/><path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76z"/></svg>
                        </el-icon>
                        {{ s.isPinned ? '取消置顶' : '置顶' }}
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" divided>
                        <el-icon style="margin-right: 8px;"><Delete /></el-icon>
                        删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
            <div v-if="!sessions.length" class="empty-hint">暂无会话</div>
          </div>
        </div>
      </aside>
      <main class="c-main">
        <div ref="messageScroller" class="chat-area" @click="onChatAreaClick">
          <div class="welcome" v-if="!messages.length">
            <h2>RAG 问答系统</h2>
            <p>选择或创建会话，开始智能问答</p>
          </div>
          <div class="message-list" :class="{ 'wide-mode': pageStyle === 'wide' }" v-else>
            <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="msg.roleType === 'USER' ? 'user' : 'assistant'">
              <div class="msg-shell">
                <div v-if="msg.roleType === 'USER'" class="bubble user-bubble">
                  <div v-if="msg.attachments?.length" class="bubble-attachments">
                    <div v-for="att in msg.attachments" :key="att.id" class="bubble-att-item" @click="att.thumbnailUrl && openImagePreview(att.url)">
                      <img v-if="att.thumbnailUrl" :src="att.thumbnailUrl" class="bubble-att-thumb" />
                      <template v-else>
                        <span class="bubble-att-file-icon">{{ getFileIcon(att.type) }}</span>
                        <span class="bubble-att-name">{{ att.name }}</span>
                      </template>
                    </div>
                  </div>
                  <div class="bubble-text">{{ msg.messageContent }}</div>
                </div>

                <div v-else class="bubble assistant-bubble">
                  <div v-if="msg.deepThinkingEnabled && (msg.thinkingContent || msg.thinkingActive || msg.thinkingElapsedSec)" class="think-block">
                    <div class="think-header" @click="msg.thinkingExpanded = !msg.thinkingExpanded">
                      <span class="think-arrow" :class="{ expanded: msg.thinkingExpanded }">▶</span>
                      <span class="think-label" :class="{ active: msg.thinkingActive }">
                        <template v-if="msg.thinkingActive">
                          <span class="dots"><span>思</span><span>考</span><span>中</span><span>.</span><span>.</span><span>.</span></span>
                        </template>
                        <template v-else>
                          已深度思考(用时{{ msg.thinkingElapsedSec ?? 0 }}秒)
                        </template>
                      </span>
                    </div>
                    <div v-if="msg.thinkingExpanded && msg.thinkingContent" class="think-body" v-html="renderMd(msg.thinkingContent)"></div>
                  </div>
                  <div class="answer-body">
                    <template v-if="!msg.messageContent">
                      <span class="wait-loading">
                        <span class="wait-dots">
                          <span style="--i: 0">正</span>
                          <span style="--i: 1">在</span>
                          <span style="--i: 2">生</span>
                          <span style="--i: 3">成</span>
                          <span style="--i: 4">.</span>
                          <span style="--i: 5">.</span>
                          <span style="--i: 6">.</span>
                        </span>
                      </span>
                    </template>
                    <div v-else class="markdown-body" v-html="renderMd(msg.messageContent)"></div>
                  </div>
                </div>

                <div v-if="!msg.pending && msg.messageContent" class="bubble-actions">
                  <button class="bubble-btn" type="button" title="复制" @click="copyMessage(msg.messageContent)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                  </button>
                  <button class="bubble-btn" type="button" title="引用" @click="quoteMessage(msg.messageContent, msg.roleType === 'USER' ? 'USER' : 'ASSISTANT')">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                  </button>
                  <button v-if="msg.roleType === 'USER'" class="bubble-btn" type="button" title="重试" @click="retryQuestion(msg.messageContent)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="composer" :class="{ 'drag-over': isDragging }" @dragover="onComposerDragOver" @dragleave="onComposerDragLeave" @drop="onComposerDrop" @paste="onComposerPaste">
          <div v-if="quotedText" class="quote-tag-bar">
            <span class="quote-tag-label">引用</span>
            <span class="quote-tag-text">{{ quotedLabel }}</span>
            <button class="quote-tag-close" @click="clearQuote">
              <el-icon><Close /></el-icon>
            </button>
          </div>
          <div v-if="pendingAttachments.length" class="pending-attachments">
            <div v-for="att in pendingAttachments" :key="att.id" class="pending-att-item">
              <img v-if="att.thumbnailUrl" :src="att.thumbnailUrl" class="pending-att-thumb" @click="openImagePreview(att.url)" />
              <div v-else class="pending-att-file">
                <span class="pending-att-file-icon">{{ getFileIcon(att.type) }}</span>
                <span class="pending-att-file-name">{{ att.name }}</span>
              </div>
              <button class="pending-att-remove" @click="removeAttachment(att.id)">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          </div>
          <input ref="fileInputRef" type="file" multiple accept="image/png,image/jpeg,image/gif,image/webp,image/bmp,.txt,.csv,.md,.pdf,.json" style="display:none" @change="onFileInputChange" />
          <el-input
            v-model="question"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 10 }"
            resize="none"
            placeholder="请输入问题..."
            @keydown.enter.prevent="!streaming && onAsk()"
          />
          <div class="composer-actions">
            <div class="composer-left-actions">
              <button class="upload-btn" type="button" :title="modelSupportsMultimodal ? '上传文件或图片' : '当前模型不支持上传'" :disabled="!modelSupportsMultimodal" @click="triggerFileInput">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              </button>
              <el-switch v-model="deepThinking" active-text="深度思考" :disabled="streaming" />
            </div>
            <div class="composer-right-actions">
              <el-popover
                placement="top-end"
                :width="300"
                trigger="click"
                v-model:visible="agentPopoverVisible"
                popper-class="agent-popover"
              >
                <template #reference>
                  <button
                    class="agent-trigger-btn"
                    type="button"
                    :title="`当前智能体: ${activeAgentName}`"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="4"/><path d="M16 8v5a3 3 0 0 0 6 0v-1a10 10 0 1 0-3.92 7.94"/></svg>
                    <span class="agent-trigger-name">{{ activeAgentName }}</span>
                  </button>
                </template>

                <div class="agent-list-popover">
                  <!-- 内置智能体 (即后台创建的用户智能体) -->
                  <div class="agent-list-section">
                    <div class="agent-list-header">内置智能体</div>
                    <div
                      v-for="a in builtinAgents"
                      :key="`b-${a.id}`"
                      class="agent-list-item"
                      :class="{ active: activeCustomAgentId === null && selectedAgentId === a.id }"
                      @click="selectBuiltinAgent(a)"
                    >
                      <span class="agent-list-name">{{ a.agentName || a.name }}</span>
                    </div>
                  </div>

                  <div class="agent-list-divider" />

                  <!-- 自定义智能体 -->
                  <div class="agent-list-section">
                    <div class="agent-list-header">我的智能体</div>
                    <div v-if="customAgents.length === 0" class="agent-list-empty">暂无自定义智能体, 点击下方创建</div>
                    <div
                      v-for="a in customAgents"
                      :key="`c-${a.id}`"
                      class="agent-list-item custom-item"
                      :class="{ active: activeCustomAgentId === a.id }"
                      @click="selectCustomAgent(a)"
                    >
                      <span class="agent-list-name">{{ a.agentName }}</span>
                      <button
                        class="agent-item-edit"
                        type="button"
                        title="编辑"
                        @click.stop="openEditCustomAgent(a)"
                      >
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                      </button>
                    </div>
                  </div>

                  <div class="agent-list-divider" />

                  <div class="agent-list-footer">
                    <el-button type="primary" plain size="small" @click="openCreateCustomAgent">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                      <span style="margin-left: 4px">创建智能体</span>
                    </el-button>
                  </div>
                </div>
              </el-popover>

              <button
                class="composer-send-btn"
                :class="{ streaming }"
                :title="streaming ? '中止回答' : '发送'"
                :disabled="!streaming && !question.trim()"
                type="button"
                @click="streaming ? abortAsk() : onAsk()"
              >
                <!-- 发送: 纸飞机图标 -->
                <svg v-if="!streaming" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="22" y1="2" x2="11" y2="13"></line>
                  <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                </svg>
                <!-- 中止: 圆角方形 -->
                <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="6" y="6" width="12" height="12" rx="2"></rect>
                </svg>
              </button>
            </div>
          </div>
          <div v-if="isDragging" class="drag-overlay">
            <div class="drag-overlay-content">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
              <span>释放以添加文件</span>
            </div>
          </div>
        </div>
      </main>
    </div>
    <el-dialog v-model="imagePreviewVisible" :show-close="true" width="auto" class="image-preview-dialog" :modal-class="'image-preview-modal'" @close="imagePreviewVisible = false">
      <img :src="imagePreviewUrl" class="image-preview-img" />
    </el-dialog>
    <PreferencesDialog v-model="preferencesVisible" />

    <!-- 自定义智能体编辑/创建弹窗 -->
    <el-dialog
      v-model="customAgentDialogVisible"
      :title="customAgentDialogMode === 'create' ? '创建智能体' : '编辑智能体'"
      width="540px"
      :close-on-click-modal="false"
      class="custom-agent-dialog"
    >
      <div class="custom-agent-form">
        <div class="form-row">
          <label>名称</label>
          <el-input
            v-model="customAgentForm.agentName"
            placeholder="智能体名称 (≤30字)"
            maxlength="30"
            show-word-limit
          />
        </div>
        <div class="form-row">
          <label>提示词</label>
          <el-input
            v-model="customAgentForm.content"
            type="textarea"
            :autosize="{ minRows: 5, maxRows: 10 }"
            placeholder="这里是您的个性化指令 (只影响 user 层, ≤500字, 系统会自动脱敏身份证/手机/邮箱/银行卡)"
            maxlength="500"
            show-word-limit
          />
        </div>
      </div>
      <template #footer>
        <div class="custom-agent-footer">
          <el-button
            v-if="customAgentDialogMode === 'edit'"
            type="danger"
            plain
            @click="deleteCustomAgent"
          >删除智能体</el-button>
          <div class="footer-right">
            <el-button @click="customAgentDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="customAgentSaving" @click="saveCustomAgent">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.c-shell { display: flex; flex-direction: column; height: 100vh; background: #f5f5f5; }
.c-header { display: flex; align-items: center; justify-content: space-between; height: 48px; padding: 0 16px; background: #1a1a2e; color: #fff; flex-shrink: 0; }
.c-header-nav { display: flex; align-items: center; gap: 16px; }
.c-header-nav a { color: rgba(255,255,255,.75); text-decoration: none; font-size: 13px; display: inline-flex; align-items: center; }
.c-header-nav a:hover { color: #fff; }
.settings-btn { display: inline-flex; align-items: center; justify-content: center; width: 40px; height: 40px; border: none; border-radius: 8px; background: transparent; color: rgba(255,255,255,.75); cursor: pointer; transition: background .15s, color .15s; }
.settings-btn:hover { background: rgba(255,255,255,.1); color: #fff; }
.settings-btn .el-icon { font-size: 22px; }
.settings-spin { animation: settings-rotate 0.3s ease; }
@keyframes settings-rotate { from { transform: rotate(0deg); } to { transform: rotate(90deg); } }

.page-style-options { display: flex; gap: 10px; padding: 8px 16px 12px; }
.style-option { display: flex; flex-direction: column; align-items: center; gap: 6px; cursor: pointer; border: 2px solid #e5e7eb; border-radius: 8px; padding: 8px; transition: all .2s; }
.style-option:hover { border-color: #a0aec0; }
.style-option.active { border-color: #409eff; background: #ecf5ff; }
.style-option-preview { width: 80px; height: 72px; background: #f3f4f6; border-radius: 4px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.standard-preview .preview-container { width: 55%; height: 100%; display: flex; flex-direction: column; justify-content: center; gap: 4px; padding: 6px 0; }
.wide-preview .preview-container { width: 85%; height: 100%; display: flex; flex-direction: column; justify-content: center; gap: 4px; padding: 6px 0; }
.preview-msg { height: 6px; border-radius: 3px; background: #d1d5db; }
.style-option-label { font-size: 11px; color: #6b7280; }
.style-option.active .style-option-label { color: #409eff; font-weight: 600; }

.font-size-options { display: flex; align-items: center; gap: 12px; padding: 12px 16px; }
.font-size-slider-wrapper { position: relative; flex: 1; height: 20px; display: flex; align-items: center; cursor: pointer; user-select: none; }
.font-size-track { position: absolute; left: 9px; right: 9px; height: 6px; background: #e5e7eb; border-radius: 3px; }
.font-size-ticks { position: absolute; top: 50%; left: 9px; right: 9px; height: 8px; transform: translateY(-50%); pointer-events: none; }
.font-size-tick { position: absolute; top: 50%; width: 8px; height: 8px; background: #cbd5e1; border-radius: 50%; cursor: pointer; pointer-events: auto; transform: translate(-50%, -50%); transition: background-color .15s, transform .15s; }
.font-size-tick:hover { background: #94a3b8; transform: translate(-50%, -50%) scale(1.25); }
.font-size-thumb { position: absolute; top: 50%; width: 18px; height: 18px; background: #fff; border: 2px solid #fff; box-shadow: 0 0 0 1px #cbd5e1, 0 2px 8px rgba(0,0,0,.15); border-radius: 50%; transform: translate(-50%, -50%); cursor: grab; z-index: 3; pointer-events: auto; }
.font-size-thumb:active { cursor: grabbing; }
.font-size-value { font-size: 14px; color: #6b7280; font-weight: 500; min-width: 48px; text-align: right; }
.logo { font-weight: 700; font-size: 15px; }
.menu-toggle { background: none; border: none; color: #fff; font-size: 18px; cursor: pointer; margin-right: 8px; }
.c-body { display: flex; flex: 1; overflow: hidden; }
.c-sidebar { width: 260px; background: #fff; border-right: 1px solid #e5e7eb; padding: 12px; overflow-y: auto; flex-shrink: 0; }
.sidebar-section { margin-bottom: 16px; }
.sidebar-section h3 { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.kb-select { width: 100%; margin: 8px 0; }
.kb-select :deep(.el-select__wrapper) { min-height: 38px; padding: 4px 12px; box-shadow: 0 0 0 1px #d1d5db inset; border-radius: 8px; transition: box-shadow .15s; }
.kb-select :deep(.el-select__wrapper:hover) { box-shadow: 0 0 0 1px #409eff inset; }
.kb-select :deep(.el-select__wrapper.is-focused) { box-shadow: 0 0 0 1px #409eff inset; }
.kb-select :deep(.el-select__placeholder) { color: #9ca3af; }
.new-session-btn { width: 100%; height: 40px; font-size: 14px; font-weight: 500; border-radius: 8px; letter-spacing: 2px; margin-top: 4px; box-shadow: 0 1px 2px rgba(64,158,255,.15); transition: transform .12s, box-shadow .12s; }
.new-session-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 10px rgba(64,158,255,.25); }
.default-kb { margin: 8px 0; padding: 8px 10px; border-radius: 6px; background: #f3f4f6; color: #4b5563; font-size: 12px; }
.session-list { display: flex; flex-direction: column; gap: 4px; }
.session-item { display: flex; align-items: center; gap: 8px; padding: 10px 12px; border-radius: 8px; border: 1px solid transparent; cursor: pointer; transition: background .15s, border-color .15s; }
.session-item:hover { background: #f9fafb; border-color: #d1d5db; }
.session-item.active { background: #ecf5ff; border-color: #409eff; }
.session-title { flex: 1; display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-actions { display: flex; align-items: center; justify-content: flex-end; min-width: 26px; }
.session-pin-tag {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; color: #6b7280;
}
.session-more {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; border: none; border-radius: 50%;
  background: transparent; color: #6b7280; cursor: pointer;
  opacity: 0; visibility: hidden; transition: all .15s ease;
}
.session-item:hover .session-more {
  opacity: 1; visibility: visible;
}
.session-more:hover { background: #f3f4f6; color: #111827; }
.empty-hint { color: #9ca3af; text-align: center; padding: 20px; font-size: 13px; }
.c-main { flex: 1; display: flex; flex-direction: column; }
.chat-area { flex: 1; overflow-y: auto; padding: 24px; }
.welcome { text-align: center; padding: 100px 0; }
.welcome h2 { font-size: 24px; margin-bottom: 8px; color: #1f2937; }
.welcome p { color: #9ca3af; }
.message-list { max-width: 780px; margin: 0 auto; width: 100%; display: flex; flex-direction: column; gap: 20px; }
.message-list.wide-mode { max-width: 1100px; }
.message-list.wide-mode .msg-shell { max-width: 70%; }
.message-list.wide-mode .user-bubble { max-width: 600px; }
.message-list.wide-mode .assistant-bubble { max-width: 800px; }

.msg-row { display: flex; }
.msg-row.user { justify-content: flex-end; }
.msg-row.assistant { justify-content: flex-start; }

.msg-shell {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  max-width: 85%;
  padding-bottom: 34px;
}

.bubble { max-width: 100%; padding: 12px 16px; border-radius: 14px; line-height: 1.7; position: relative; }
.user-bubble { background: #dcfce7; color: #166534; border-radius: 14px 14px 4px 14px; font-size: var(--chat-font-size, 16px); white-space: pre-wrap; word-break: break-word; }
.assistant-bubble { background: #fff; border: 1px solid #e5e7eb; border-radius: 14px; font-size: var(--chat-font-size, 16px); }

.bubble-text { white-space: pre-wrap; word-break: break-word; }

.bubble-actions {
  display: flex; gap: 4px;
  position: absolute; right: 0; bottom: 0;
  opacity: 0; visibility: hidden; transition: opacity .2s, visibility .2s;
  z-index: 1;
}
.msg-shell:hover .bubble-actions { opacity: 1; visibility: visible; }

.bubble-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 28px; height: 28px;
  border: 1px solid #d1d5db; border-radius: 6px;
  background: #fff; color: #6b7280; cursor: pointer;
  transition: background .15s, color .15s, border-color .15s;
}
.bubble-btn:hover { background: #f3f4f6; color: #374151; border-color: #9ca3af; }

:deep(.code-block-shell) {
  margin: 10px 0 12px;
  border: 1px solid #d7deea;
  border-radius: 10px;
  overflow: hidden;
  background: #f8fafc;
  position: relative;
}

:deep(.code-toolbar) {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  background: linear-gradient(180deg, #eef3f8 0%, #e8eef6 100%);
  border-bottom: 1px solid #d7deea;
}

:deep(.code-language) {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #355070;
  font-size: 12px;
  font-weight: 600;
  text-transform: lowercase;
}

:deep(.code-toolbar-actions) {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 6px;
}

:deep(.code-block-btn) {
  position: relative;
  z-index: 4;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #cfd8e3;
  border-radius: 6px;
  background: #fff;
  color: #61758a;
  cursor: pointer;
  transition: background .15s, color .15s, border-color .15s, transform .15s;
}

:deep(.code-block-btn:hover) {
  background: #f3f6fa;
  color: #1f2d3d;
  border-color: #9fb2c7;
  transform: translateY(-1px);
}

.think-block { margin-bottom: 10px; padding: 8px 10px; border-left: 3px solid #d1d5db; background: #f9fafb; border-radius: 4px; }
.think-header { display: flex; align-items: center; gap: 6px; cursor: pointer; user-select: none; font-size: 12px; color: #6b7280; line-height: 1.65; }
.think-header:hover { color: #4b5563; }
.think-arrow { display: inline-block; font-size: 10px; transition: transform 0.18s ease; color: #9ca3af; transform: rotate(0deg); }
.think-arrow.expanded { transform: rotate(90deg); }
.think-label.active { color: #6b7280; }
.dots { display: inline-block; min-width: 18px; }
.dots span { animation: dot-blink 1.4s infinite both; }
.dots span:nth-child(2) { animation-delay: 0.2s; }
.dots span:nth-child(3) { animation-delay: 0.4s; }
.think-body { font-size: 0.75em; color: #6b7280; line-height: 1.65; margin-top: 6px; padding: 4px 0 0; }
.think-body :deep(p) { margin: 0 0 4px; }
.think-body :deep(*:last-child) { margin-bottom: 0; }

.answer-body { font-size: var(--chat-font-size, 16px); color: #111827; }
.answer-body :deep(p) { margin: 0 0 8px; }
.answer-body :deep(*:last-child) { margin-bottom: 0; }

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 20px; margin: 6px 0;
}
.markdown-body :deep(li) {
  margin-bottom: 2px;
}
.markdown-body :deep(li)::marker {
  color: #4b5563;
}
.markdown-body :deep(blockquote) {
  margin: 8px 0; padding: 6px 14px;
  border-left: 3px solid #d1d5db;
  background: #f9fafb; color: #4b5563;
  border-radius: 0 6px 6px 0;
}
.markdown-body :deep(table) {
  width: 100%; border-collapse: collapse; margin: 8px 0; font-size: 0.875em;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #e5e7eb; padding: 8px 12px; text-align: left;
}
.markdown-body :deep(th) {
  background: #f3f4f6; font-weight: 600;
}
.markdown-body :deep(tr:nth-child(even)) {
  background: #fafafa;
}
.markdown-body :deep(h1) { font-size: 1.35em; margin: 12px 0 8px; font-weight: 700; }
.markdown-body :deep(h2) { font-size: 1.2em; margin: 10px 0 6px; font-weight: 700; }
.markdown-body :deep(h3) { font-size: 1.1em; margin: 8px 0 4px; font-weight: 600; }
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) { font-size: 1em; margin: 6px 0 4px; font-weight: 600; }
.markdown-body :deep(hr) {
  margin: 12px 0; border: none; border-top: 1px solid #e5e7eb;
}
.markdown-body :deep(a) {
  color: #2563eb; text-decoration: underline;
}
.markdown-body :deep(img) {
  max-width: 100%; border-radius: 6px;
}
.markdown-body :deep(pre) {
  position: relative;
  z-index: 1;
  margin: 0;
  padding: 14px 16px;
  background: #0f172a;
  color: #e5edf5;
  border-radius: 0;
  overflow-x: auto;
  font-size: 0.8125em;
}

/* ============ 智能体 (内置 + 自定义) ============ */
.composer-right-actions { display: flex; align-items: center; gap: 8px; }
.agent-trigger-btn {
  display: inline-flex; align-items: center; gap: 4px;
  height: 36px; max-width: 180px;
  padding: 0 12px;
  border: 1px solid #dcdfe6; border-radius: 18px;
  background: #fff; color: #303133;
  font-size: 13px; cursor: pointer;
  transition: all 0.18s ease;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.agent-trigger-btn:hover { border-color: #409eff; color: #409eff; }
.agent-trigger-btn svg { flex-shrink: 0; }
.agent-trigger-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 140px; }

.agent-list-popover { font-size: 13px; }
.agent-list-section { padding: 4px 0; }
.agent-list-header { padding: 4px 8px; font-size: 11px; color: #909399; font-weight: 600; letter-spacing: 0.5px; }
.agent-list-divider { height: 1px; background: #ebeef5; margin: 4px 0; }
.agent-list-empty { padding: 12px 8px; color: #c0c4cc; text-align: center; font-size: 12px; }
.agent-list-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 10px; border-radius: 6px; cursor: pointer;
  transition: background 0.12s;
  position: relative;
}
.agent-list-item:hover { background: #f5f7fa; }
.agent-list-item.active { background: #ecf5ff; color: #409eff; }
.agent-list-item .agent-list-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.agent-list-item .agent-list-tag { font-size: 10px; padding: 1px 5px; background: #f0f9ff; color: #409eff; border-radius: 3px; flex-shrink: 0; margin-left: 6px; }
.agent-list-footer { padding: 6px 0 0 0; }
.agent-list-footer .el-button { width: 100%; }
.agent-item-edit {
  /* 不占位: absolute 定位, hover 时浮现, 离开时淡出 */
  position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  display: flex; align-items: center; justify-content: center;
  width: 22px; height: 22px;
  border: none; border-radius: 4px;
  background: transparent; color: #909399;
  cursor: pointer; opacity: 0;
  transition: opacity 0.15s, background 0.12s, color 0.12s;
}
.agent-list-item.custom-item:hover .agent-item-edit { opacity: 1; }
.agent-item-edit:hover { background: #409eff; color: #fff; }

/* 自定义智能体弹窗 */
.custom-agent-form .form-row { margin-bottom: 14px; }
.custom-agent-form .form-row label { display: block; font-size: 13px; color: #606266; margin-bottom: 4px; }
.custom-agent-form .form-tip {
  display: flex; align-items: flex-start; gap: 6px;
  padding: 8px 10px; margin-top: 4px;
  background: #f0f9ff; border-radius: 4px;
  font-size: 12px; color: #5a6d8a; line-height: 1.5;
}
.custom-agent-form .form-tip svg { flex-shrink: 0; margin-top: 2px; color: #409eff; }
.custom-agent-footer { display: flex; align-items: center; justify-content: space-between; }
.custom-agent-footer .footer-right { display: flex; gap: 8px; }
.markdown-body :deep(code) {
  background: #f1f3f5; padding: 1px 4px; border-radius: 3px; font-size: 0.875em;
}
.markdown-body :deep(pre code) {
  display: block;
  background: none;
  color: inherit;
  padding: 0;
  line-height: 1.7;
  position: relative;
  z-index: 1;
}
.markdown-body :deep(input[type="checkbox"]) {
  margin-right: 6px; vertical-align: middle;
}
.markdown-body :deep(.task-list-item) {
  list-style: none;
}

.wait-loading { display: inline-flex; align-items: baseline; gap: 0; font-size: 12px; color: #6b7280; }
.wait-dots { display: inline-block; }
.wait-dots span {
  display: inline-block;
  animation: dot-blink 1.4s infinite both;
  /* 自左向右错位闪烁: 第 i 个字符延迟 i * 0.15s */
  animation-delay: calc(var(--i, 0) * 0.15s);
}

@keyframes dot-blink { 0%, 80%, 100% { opacity: 0.2; } 40% { opacity: 1; } }

.composer { display: flex; flex-direction: column; gap: 10px; padding: 12px 20px; background: #fff; border-top: 1px solid #e5e7eb; }
.composer-actions { display: flex; align-items: center; justify-content: space-between; }

.composer-send-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 36px; height: 36px;
  border: none; border-radius: 8px;
  background: #409eff; color: #fff;
  cursor: pointer;
  transition: background-color .15s ease, transform .12s ease, box-shadow .15s ease, opacity .15s ease;
  box-shadow: 0 1px 2px rgba(64, 158, 255, .25);
}
.composer-send-btn:hover:not(:disabled) { background: #66b1ff; transform: translateY(-1px); box-shadow: 0 3px 6px rgba(64, 158, 255, .3); }
.composer-send-btn:active:not(:disabled) { transform: translateY(0); box-shadow: 0 1px 2px rgba(64, 158, 255, .25); }
.composer-send-btn:disabled { background: #c0d8f5; color: #fff; cursor: not-allowed; opacity: .7; }
.composer-send-btn.streaming { background: #f56c6c; box-shadow: 0 1px 2px rgba(245, 108, 108, .25); animation: send-pulse 1.2s ease-in-out infinite; }
.composer-send-btn.streaming:hover { background: #f78989; box-shadow: 0 3px 6px rgba(245, 108, 108, .3); }
@keyframes send-pulse { 0%, 100% { box-shadow: 0 1px 2px rgba(245, 108, 108, .25); } 50% { box-shadow: 0 0 0 6px rgba(245, 108, 108, .15); } }

.quote-tag-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: #f0f5ff; border: 1px solid #adc6ff;
  border-radius: 8px; font-size: 13px;
}
.quote-tag-label {
  display: inline-block; padding: 1px 8px; border-radius: 4px;
  background: #1677ff; color: #fff; font-size: 11px; font-weight: 600;
  flex-shrink: 0;
}
.quote-tag-text {
  flex: 1; color: #374151; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.quote-tag-close {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; border: none; border-radius: 4px;
  background: transparent; color: #6b7280; cursor: pointer; flex-shrink: 0;
  transition: background .15s, color .15s;
}
.quote-tag-close:hover { background: rgba(0,0,0,.08); color: #374151; }

.composer-left-actions { display: flex; align-items: center; gap: 12px; }
.upload-btn { display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; border: 1px solid #d1d5db; border-radius: 6px; background: #fff; color: #6b7280; cursor: pointer; transition: all .15s; }
.upload-btn:hover:not(:disabled) { background: #f3f4f6; color: #374151; border-color: #9ca3af; }
.upload-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.pending-attachments { display: flex; flex-wrap: wrap; gap: 8px; padding: 4px 0; }
.pending-att-item { position: relative; display: inline-flex; align-items: center; }
.pending-att-thumb { width: 64px; height: 64px; object-fit: cover; border-radius: 6px; border: 1px solid #e5e7eb; cursor: pointer; transition: border-color .15s; }
.pending-att-thumb:hover { border-color: #409eff; }
.pending-att-file { display: flex; align-items: center; gap: 6px; padding: 6px 10px; background: #f3f4f6; border-radius: 6px; border: 1px solid #e5e7eb; font-size: 12px; color: #374151; max-width: 160px; }
.pending-att-file-icon { font-size: 16px; flex-shrink: 0; }
.pending-att-file-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pending-att-remove { position: absolute; top: -6px; right: -6px; width: 18px; height: 18px; border-radius: 50%; background: #ef4444; color: #fff; border: none; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; font-size: 10px; transition: background .15s; }
.pending-att-remove:hover { background: #dc2626; }

.composer.drag-over { position: relative; border-color: #409eff; background: #f0f7ff; }
.drag-overlay { position: absolute; inset: 0; background: rgba(64,158,255,.08); border: 2px dashed #409eff; border-radius: 8px; display: flex; align-items: center; justify-content: center; z-index: 10; pointer-events: none; }
.drag-overlay-content { display: flex; flex-direction: column; align-items: center; gap: 8px; color: #409eff; font-size: 14px; font-weight: 500; }

.bubble-attachments { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.bubble-att-item { display: inline-flex; align-items: center; gap: 4px; padding: 4px 8px; background: rgba(0,0,0,.04); border-radius: 6px; cursor: pointer; transition: background .15s; max-width: 200px; }
.bubble-att-item:hover { background: rgba(0,0,0,.08); }
.bubble-att-thumb { width: 40px; height: 40px; object-fit: cover; border-radius: 4px; flex-shrink: 0; }
.bubble-att-file-icon { font-size: 16px; flex-shrink: 0; }
.bubble-att-name { font-size: 12px; color: #374151; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>

<style>
/* 非 scoped：dialog 被 teleport 到 body */
.image-preview-dialog { --el-dialog-padding-primary: 0; }
.image-preview-dialog .el-dialog__header { padding: 8px 12px 0 0; margin-right: 0; position: absolute; right: 0; top: 0; z-index: 1; }
.image-preview-dialog .el-dialog__body { padding: 0; }
.image-preview-dialog .el-dialog__headerbtn { width: 36px; height: 36px; }
.image-preview-img { display: block; max-width: 85vw; max-height: 85vh; object-fit: contain; border-radius: 4px; }
</style>
