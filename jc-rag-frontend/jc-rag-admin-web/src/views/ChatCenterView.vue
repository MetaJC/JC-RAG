<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Loading } from '@element-plus/icons-vue'
import { marked } from 'marked'

import { askChatQuestionStream, createChatSession, fetchChatMessages, fetchChatSessions } from '../api/chat'
import { fetchKnowledgeBases } from '../api/knowledge'
import type { ChatMessageItem, ChatSessionItem } from '../types/chat'
import type { KnowledgeBaseItem } from '../types/knowledge'

const sessions = ref<ChatSessionItem[]>([])
type UiChatMessage = ChatMessageItem & { pending?: boolean }
const messages = ref<UiChatMessage[]>([])
const knowledgeBases = ref<KnowledgeBaseItem[]>([])
const selectedSessionId = ref<number | null>(null)
const asking = ref(false)
const streaming = ref(false)
const messageScroller = ref<HTMLDivElement | null>(null)

const sessionForm = reactive({
  sessionTitle: '',
  kbId: 0,
})

const askForm = reactive({
  question: '',
})

const currentSession = computed(() => sessions.value.find((item) => item.id === selectedSessionId.value) ?? null)

function pad2(value: number) {
  return value.toString().padStart(2, '0')
}

function formatDateTime(input: string | Date) {
  const date = typeof input === 'string' ? new Date(input.replace(' ', 'T')) : input
  if (Number.isNaN(date.getTime())) {
    return typeof input === 'string' ? input : ''
  }
  const y = date.getFullYear()
  const m = pad2(date.getMonth() + 1)
  const d = pad2(date.getDate())
  const hh = pad2(date.getHours())
  const mm = pad2(date.getMinutes())
  const ss = pad2(date.getSeconds())
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
}

async function scrollToBottom() {
  await nextTick()
  const el = messageScroller.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

async function loadBaseData() {
  const kbResult = await fetchKnowledgeBases()
  knowledgeBases.value = kbResult.records ?? kbResult
  const sessResult = await fetchChatSessions()
  sessions.value = (sessResult.records ?? sessResult) as any[]
  const firstKnowledgeBase = knowledgeBases.value[0]
  const firstSession = sessions.value[0]
  if (!sessionForm.kbId && firstKnowledgeBase) {
    sessionForm.kbId = firstKnowledgeBase.id
  }
  if (!selectedSessionId.value && firstSession) {
    selectedSessionId.value = firstSession.id
  }
  if (selectedSessionId.value) {
    messages.value = await fetchChatMessages(selectedSessionId.value)
  }
}

async function onCreateSession() {
  if (!sessionForm.sessionTitle || !sessionForm.kbId) {
    ElMessage.warning('请填写会话名称并选择知识库')
    return
  }
  const created = await createChatSession(sessionForm.sessionTitle, sessionForm.kbId)
  sessions.value.unshift(created)
  selectedSessionId.value = created.id
  messages.value = await fetchChatMessages(created.id)
  sessionForm.sessionTitle = ''
  ElMessage.success('问答会话已创建')
}

async function onSelectSession(sessionId: number) {
  selectedSessionId.value = sessionId
  messages.value = await fetchChatMessages(sessionId)
  await scrollToBottom()
}

async function onAsk() {
  if (!selectedSessionId.value || !askForm.question.trim()) {
    ElMessage.warning('请先选择会话并输入问题')
    return
  }
  if (streaming.value) {
    return
  }
  asking.value = true
  streaming.value = true
  try {
    const sessionId = selectedSessionId.value
    const question = askForm.question.trim()
    askForm.question = ''
    const now = formatDateTime(new Date())
    const tempUserId = -Date.now()
    const tempAssistantId = tempUserId - 1
    messages.value.push({
      id: tempUserId,
      roleType: 'USER',
      messageContent: question,
      messageIndex: 0,
      tokenCount: 0,
      latencyMs: 0,
      createdAt: now,
    })
    messages.value.push({
      id: tempAssistantId,
      roleType: 'ASSISTANT',
      messageContent: '',
      messageIndex: 0,
      tokenCount: 0,
      latencyMs: 0,
      createdAt: now,
      pending: true,
    })
    await scrollToBottom()

    let assistantIndex = messages.value.findIndex((item) => item.id === tempAssistantId)
    await askChatQuestionStream(sessionId, question, {
      onMeta(data) {
        const userId = Number(data?.userMessageId)
        const assistantId = Number(data?.assistantMessageId)
        if (!Number.isNaN(userId)) {
          const idx = messages.value.findIndex((item) => item.id === tempUserId)
          const msg = idx >= 0 ? messages.value[idx] : undefined
          if (msg) {
            msg.id = userId
          }
        }
        if (!Number.isNaN(assistantId)) {
          assistantIndex = messages.value.findIndex((item) => item.id === tempAssistantId)
          const msg = assistantIndex >= 0 ? messages.value[assistantIndex] : undefined
          if (msg) {
            msg.id = assistantId
          }
        }
      },
      onDelta(delta) {
        const idx = assistantIndex >= 0 ? assistantIndex : messages.value.findIndex((item) => item.id === tempAssistantId)
        const msg = idx >= 0 ? messages.value[idx] : undefined
        if (msg) {
          msg.messageContent += delta
          msg.pending = false
          void scrollToBottom()
        }
      },
      onDone(data) {
        const idx = assistantIndex >= 0 ? assistantIndex : messages.value.findIndex((item) => item.id === tempAssistantId)
        const msg = idx >= 0 ? messages.value[idx] : undefined
        if (msg) {
          msg.pending = false
          const llmUsed = data?.llmUsed
          if (typeof llmUsed === 'boolean' && !llmUsed && !msg.messageContent) {
            msg.messageContent =
              '未命中知识库，且模型调用未返回有效内容。请检查模型服务配置或稍后重试。'
          }
        }
        streaming.value = false
      },
    })

    messages.value = await fetchChatMessages(sessionId)
    await scrollToBottom()
  } catch (error) {
    console.error(error)
    ElMessage.error('提问失败，请检查知识库或模型服务')
    const idx = messages.value.findIndex((item) => item.roleType === 'ASSISTANT' && item.pending)
    const msg = idx >= 0 ? messages.value[idx] : undefined
    if (msg) {
      msg.pending = false
      if (!msg.messageContent) {
        msg.messageContent = '提问失败，请检查知识库或模型服务。'
      }
    }
    if (selectedSessionId.value) {
      messages.value = await fetchChatMessages(selectedSessionId.value)
      await scrollToBottom()
    }
  } finally {
    asking.value = false
    streaming.value = false
  }
}

function onComposerKeydown(event: KeyboardEvent) {
  if (event.isComposing) {
    return
  }
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void onAsk()
  }
}

function renderMarkdown(text: string): string {
  if (!text) return ''
  try {
    return marked.parse(text, { async: false }) as string
  } catch {
    return text
  }
}

function onCopy(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

onMounted(async () => {
  await loadBaseData()
  await scrollToBottom()
})
</script>

<template>
  <div>
      <header class="page-header">
        <div>
          <h1>问答中心</h1>
          <p>选择知识库创建会话，并基于已入库文档进行检索问答。</p>
        </div>
      </header>

      <section class="content-grid">
        <el-card class="panel-card" shadow="never">
          <template #header>创建会话</template>
          <el-form label-position="top">
            <el-form-item label="会话名称">
              <el-input v-model="sessionForm.sessionTitle" placeholder="如 作战条令问答" />
            </el-form-item>
            <el-form-item label="绑定知识库">
              <el-select v-model="sessionForm.kbId" placeholder="请选择知识库">
                <el-option v-for="item in knowledgeBases" :key="item.id" :label="item.kbName" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-button type="primary" @click="onCreateSession">创建会话</el-button>
          </el-form>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header>会话列表</template>
          <div class="session-list">
            <button
              v-for="item in sessions"
              :key="item.id"
              class="session-item"
              :class="{ active: item.id === selectedSessionId }"
              @click="onSelectSession(item.id)"
            >
              <div class="session-title">{{ item.sessionTitle }}</div>
              <div class="session-desc">{{ item.kbName || '未绑定知识库' }}</div>
            </button>
          </div>
        </el-card>
      </section>

      <section>
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-header-actions">
              <span>当前会话</span>
              <span>{{ currentSession?.sessionTitle || '未选择会话' }}</span>
            </div>
          </template>

          <div class="chat-shell">
            <div ref="messageScroller" class="message-scroller">
              <div class="message-list">
                <div v-for="item in messages" :key="item.id" class="message-item" :class="item.roleType.toLowerCase()">
                  <div class="message-meta">
                    <span class="message-role">{{ item.roleType === 'USER' ? '用户' : '助手' }}</span>
                    <span class="message-time">{{ formatDateTime(item.createdAt) }}</span>
                    <el-tooltip content="复制" placement="top">
                      <el-button class="copy-btn" text size="small" @click="onCopy(item.messageContent)">
                        <el-icon><CopyDocument /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </div>
                  <div class="message-content">
                    <template v-if="item.pending">
                      <el-icon class="message-loading is-loading">
                        <Loading />
                      </el-icon>
                      <span v-if="item.messageContent" v-html="renderMarkdown(item.messageContent)"></span>
                    </template>
                    <template v-else>
                      <div v-html="renderMarkdown(item.messageContent)"></div>
                    </template>
                  </div>
                </div>
              </div>
            </div>

            <div class="composer">
              <el-input
                v-model="askForm.question"
                type="textarea"
                :rows="2"
                resize="none"
                placeholder="请输入问题，例如：装甲部队协同作战的关键要点是什么？"
                @keydown="onComposerKeydown"
              />
              <el-button type="primary" :disabled="streaming" @click="onAsk">发送</el-button>
            </div>
          </div>
        </el-card>
      </section>
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

.content-grid {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-card {
  border-radius: 16px;
}

.session-list {
  display: grid;
  gap: 12px;
}

.session-item {
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.session-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.session-title {
  font-weight: 700;
  color: #0f2744;
}

.session-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.panel-header-actions,
.composer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chat-shell {
  display: flex;
  flex-direction: column;
  height: 60vh;
  min-height: 420px;
}

.message-scroller {
  flex: 1;
  overflow-y: auto;
  padding-right: 6px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 12px;
  padding-bottom: 12px;
}

.message-item {
  display: flex;
  flex-direction: column;
  max-width: 78%;
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
}

.message-item.user {
  align-self: flex-end;
  background: #dcfce7;
}

.message-item.assistant {
  align-self: flex-start;
  background: #ecf5ff;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #6b7280;
}

.message-time {
  color: #94a3b8;
}

.message-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.message-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
  font-size: 13px;
}

.message-content :deep(th),
.message-content :deep(td) {
  border: 1px solid #d0d5dd;
  padding: 6px 10px;
  text-align: left;
}

.message-content :deep(th) {
  background: #f2f4f7;
  font-weight: 600;
}

.message-content :deep(code) {
  background: #f2f4f7;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.message-content :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.5;
}

.message-content :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.message-content :deep(p) {
  margin: 6px 0;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  margin: 6px 0;
  padding-left: 20px;
}

.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3),
.message-content :deep(h4) {
  margin: 10px 0 6px;
  font-size: inherit;
  font-weight: 600;
}

.message-loading {
  margin-right: 8px;
  vertical-align: middle;
  color: #64748b;
}

.copy-btn {
  color: #94a3b8;
  padding: 0 4px;
}

.copy-btn:hover {
  color: #409eff;
}

.composer {
  padding-top: 12px;
  border-top: 1px solid #eef2f7;
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.7;
}
</style>
