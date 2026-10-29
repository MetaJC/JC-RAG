<!--
  Copyright 2024-2026 OpenJC
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  
  本文件来源于 JC RAG Community Edition
  开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
  二次开发请保留此出处声明。
 -->


<script setup lang="ts">
/**
 * 偏好设置 Dialog (C 端, 齿轮 - 偏好设置)。
 * <p>左右布局: 左侧设置项导航, 右侧当前项内容。
 * <p>第一阶段实现 3 项: 记忆文件位置 / 默认模型 / 回答风格; 其他 7 项预留占位。
 * <p>不依赖 pinia: 弹窗 self-contained, 打开时拉取, 关闭即释放。
 * <p>记忆文件支持 4 种操作: 查看 / 编辑 / 复制路径 / (Chrome/Edge) 在系统中打开.
 * <p>编辑器弹窗独立成 {@code MemoryFileEditorDialog} 组件, 避免嵌套 dialog 引起的 z-index / 焦点问题.
 *
 * @author JC
 */
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  Brush,
  ChatLineSquare,
  Clock,
  Cpu,
  DataAnalysis,
  DataLine,
  Delete,
  Folder,
  FolderOpened,
  InfoFilled,
  Memo,
  Operation,
  Reading,
  RefreshRight,
  Setting,
  User,
} from '@element-plus/icons-vue'

import {
  CHAT_STYLE_OPTIONS,
  DEFAULT_CHAT_STYLE,
  DEFAULT_MODEL_CODE,
  fetchMemoryFile,
  fetchUserPreference,
  forgetMe,
  updateUserPreference,
  type UserPreferenceUpdateParam,
} from '../api/preference'
import MemoryFileEditorDialog from './MemoryFileEditorDialog.vue'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

/* ============== 左侧设置项 (含占位) ============== */

interface PreferenceItem {
  key: string
  label: string
  icon: unknown
  /** ok = 已实现, todo = 待办 */
  status: 'ok' | 'todo'
}

const items: PreferenceItem[] = [
  { key: 'memory-file-path', label: '记忆文件位置', icon: Folder, status: 'ok' },
  { key: 'default-model', label: '默认模型', icon: Cpu, status: 'ok' },
  { key: 'chat-style', label: '回答风格', icon: ChatLineSquare, status: 'ok' },
  { key: 'long-memory', label: '长期记忆', icon: DataLine, status: 'ok' },
  { key: 'memory-profile', label: '用户画像 (L3)', icon: User, status: 'ok' },
  { key: 'forget-me', label: '被遗忘权 (GDPR)', icon: Delete, status: 'ok' },
  { key: 'theme', label: '主题外观', icon: Brush, status: 'todo' },
  { key: 'language', label: '界面语言', icon: Memo, status: 'todo' },
  { key: 'font-size', label: '字号与排版', icon: Reading, status: 'todo' },
  { key: 'shortcut', label: '快捷键', icon: Operation, status: 'todo' },
  { key: 'notification', label: '消息通知', icon: Bell, status: 'todo' },
  { key: 'history-retention', label: '历史保留时长', icon: Clock, status: 'todo' },
  { key: 'usage-stats', label: '使用统计与报告', icon: DataAnalysis, status: 'todo' },
]

const activeKey = ref('memory-file-path')
const currentItem = computed(() => items.find((i) => i.key === activeKey.value))

function selectItem(key: string) {
  activeKey.value = key
}

/* ============== 表单 (3 个已实现项) ============== */

const form = reactive({
  memoryFilePath: '',
  defaultModelCode: DEFAULT_MODEL_CODE,
  chatStyle: DEFAULT_CHAT_STYLE,
  enableLongMemory: true,
  enableMemoryProfile: true,
})
const saving = ref(false)
const loaded = ref(false)
const forgetting = ref(false)

async function loadForm() {
  try {
    const vo = await fetchUserPreference()
    form.memoryFilePath = vo.memoryFilePath || ''
    form.defaultModelCode = vo.defaultModelCode ?? DEFAULT_MODEL_CODE
    form.chatStyle = vo.chatStyle || DEFAULT_CHAT_STYLE
    form.enableLongMemory = vo.enableLongMemory !== false
    form.enableMemoryProfile = vo.enableMemoryProfile !== false
  } catch (e) {
    console.warn('[preference] load failed:', e)
  } finally {
    loaded.value = true
  }
}

watch(
  () => props.modelValue,
  (v) => {
    if (v) loadForm()
  },
  { immediate: true }
)

async function onSaveAll() {
  if (!form.memoryFilePath || !form.memoryFilePath.trim()) {
    ElMessage.warning('记忆文件路径不能为空')
    activeKey.value = 'memory-file-path'
    return
  }
  saving.value = true
  try {
    const payload: UserPreferenceUpdateParam = {
      memoryFilePath: form.memoryFilePath.trim(),
      defaultModelCode: form.defaultModelCode,
      chatStyle: form.chatStyle,
      enableLongMemory: form.enableLongMemory,
      enableMemoryProfile: form.enableMemoryProfile,
    }
    const vo = await updateUserPreference(payload)
    if (vo.memoryFilePath) form.memoryFilePath = vo.memoryFilePath
    if (vo.defaultModelCode != null) form.defaultModelCode = vo.defaultModelCode
    if (vo.chatStyle) form.chatStyle = vo.chatStyle
    if (vo.enableLongMemory != null) form.enableLongMemory = vo.enableLongMemory
    if (vo.enableMemoryProfile != null) form.enableMemoryProfile = vo.enableMemoryProfile
    ElMessage.success('已保存')
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function onResetAll() {
  try {
    await ElMessageBox.confirm(
      '将恢复 3 项偏好的默认设置 (记忆文件位置 / 默认模型 / 回答风格), 是否继续?',
      '恢复默认',
      { confirmButtonText: '恢复', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  saving.value = true
  try {
    const payload: UserPreferenceUpdateParam = {
      memoryFilePath: '',
      defaultModelCode: DEFAULT_MODEL_CODE,
      chatStyle: DEFAULT_CHAT_STYLE,
      enableLongMemory: true,
      enableMemoryProfile: true,
    }
    const vo = await updateUserPreference(payload)
    if (vo.memoryFilePath) form.memoryFilePath = vo.memoryFilePath
    if (vo.defaultModelCode != null) form.defaultModelCode = vo.defaultModelCode
    if (vo.chatStyle) form.chatStyle = vo.chatStyle
    if (vo.enableLongMemory != null) form.enableLongMemory = vo.enableLongMemory
    if (vo.enableMemoryProfile != null) form.enableMemoryProfile = vo.enableMemoryProfile
    ElMessage.success('已恢复默认')
  } catch (e) {
    console.error(e)
    ElMessage.error('恢复失败')
  } finally {
    saving.value = false
  }
}

/* ============== 记忆文件: 4 种操作 (编辑/查看拆到独立组件) ============== */

const editorVisible = ref(false)
const editorMode = ref<'view' | 'edit'>('view')

/** File System Access API 运行时探测 (仅 Chrome 86+ / Edge 支持) */
const supportsShowDirectoryPicker =
  typeof window !== 'undefined' && typeof (window as unknown as { showDirectoryPicker?: unknown }).showDirectoryPicker === 'function'

/** 打开编辑器 (查看模式) */
function onViewMemoryFile() {
  editorMode.value = 'view'
  editorVisible.value = true
}

/** 打开编辑器 (编辑模式) */
function onEditMemoryFile() {
  editorMode.value = 'edit'
  editorVisible.value = true
}

/** 复制路径到剪贴板 */
async function onCopyPath() {
  try {
    // 主动拉一次确保拿到最新 resolvedPath
    const vo = await fetchMemoryFile()
    const text = vo.resolvedPath || vo.rawPath || ''
    if (!text) {
      ElMessage.warning('路径为空')
      return
    }
    await navigator.clipboard.writeText(text)
    ElMessage.success('路径已复制, 可在资源管理器中粘贴打开')
  } catch (e) {
    console.warn('[preference] clipboard failed:', e)
    ElMessage.warning('剪贴板不可用, 请手动复制路径')
  }
}

/**
 * 在系统中打开 (File System Access API, 仅 Chrome/Edge).
 * <p>浏览器安全策略: Web 应用无法"任意打开本地目录", 必须由用户主动授权选目录.
 */
async function onOpenInSystem() {
  if (!supportsShowDirectoryPicker) {
    ElMessage.warning('当前浏览器不支持在系统中打开, 请使用复制路径后手动打开')
    return
  }
  try {
    let vo
    try {
      vo = await fetchMemoryFile()
    } catch {
      vo = null
    }
    const hint = vo?.parentDir || '~/.jc/jc-rag/memory'
    await ElMessageBox.confirm(
      `即将请求授权打开一个目录 (提示路径: ${hint}). Web 端不能"自动定位", 你需要在弹出的窗口中选择正确的目录, 之后可在该目录内读写 user_profile.md.`,
      '在系统中打开',
      { confirmButtonText: '授权并选择', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }
  try {
    const w = window as unknown as {
      showDirectoryPicker?: (opts: { mode: string }) => Promise<{
        getFileHandle: (name: string, opts: { create: boolean }) => Promise<{
          getFile: () => Promise<File>
        }>
      }>
    }
    if (!w.showDirectoryPicker) {
      ElMessage.warning('当前浏览器不支持此能力')
      return
    }
    const dirHandle = await w.showDirectoryPicker({ mode: 'readwrite' })
    let fileHandle: { getFile: () => Promise<File> } | null = null
    try {
      fileHandle = await dirHandle.getFileHandle('user_profile.md', { create: false })
    } catch {
      try {
        await ElMessageBox.confirm(
          '所选目录中没有 user_profile.md, 是否创建?',
          '创建文件',
          { confirmButtonText: '创建', cancelButtonText: '取消', type: 'warning' }
        )
        fileHandle = await dirHandle.getFileHandle('user_profile.md', { create: true })
      } catch {
        return
      }
    }
    if (!fileHandle) return
    const file = await fileHandle.getFile()
    // 加载到编辑器 (用临时 view 模式, 用户可切到 edit 后另存到后端真实路径)
    ElMessage.success(`已加载本地文件 (${file.size} 字节), 可在编辑器中继续操作`)
    // 这里简化处理: 打开编辑模式, 内容用本地文件, 关闭时再保存到后端真实路径
    // 实际保存由 MemoryFileEditorDialog 的 onSave 负责, 但内容已被覆盖
    // 因此这里只做"提示用户已加载", 不替换 editorContent (避免与后端真实路径冲突)
  } catch (e: unknown) {
    const err = e as { name?: string }
    if (err?.name === 'AbortError') return
    console.error(e)
    ElMessage.error('打开目录失败: ' + (e as Error).message)
  }
}

function onClose() {
  dialogVisible.value = false
}

/**
 * 一键忘记我: 物理删除当前用户全部对话数据 (L0+L1+L2+L3+偏好+记忆文件)。
 * <p>流程: 输入 "FORGET" → 二次确认 → 调用 /user/preference/forget-me → 提示统计。
 * <p>不可逆, 弹窗关闭前不再保留任何历史。
 */
async function onForgetMe() {
  // 1) 第一次确认: 要求输入 FORGET 三个字母
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入大写 FORGET 以确认永久删除你的全部对话数据 (L0 消息 + L1 摘要 + L2 长期事实 + L3 画像 + 偏好 + 记忆文件). 此操作不可恢复.',
      '一键忘记我 (被遗忘权)',
      {
        inputPlaceholder: '请输入: FORGET',
        inputValidator: (v: string) => (v === 'FORGET' ? true : '必须输入大写 FORGET 三个字母'),
        confirmButtonText: '下一步',
        cancelButtonText: '取消',
        inputType: 'text',
        type: 'warning',
      }
    )
    if (value !== 'FORGET') return
  } catch {
    return
  }
  // 2) 第二次确认: 弹窗按钮确认
  try {
    await ElMessageBox.confirm(
      '最后确认: 将物理删除你的全部对话数据, 是否继续?',
      '再次确认',
      { confirmButtonText: '是的, 永久删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch {
    return
  }
  // 3) 调用后端
  forgetting.value = true
  try {
    const r = await forgetMe()
    const total = r.totalDeleted ?? 0
    const parts: string[] = []
    if (r.chatMessageDeleted) parts.push(`消息 ${r.chatMessageDeleted} 条`)
    if (r.chatSessionDeleted) parts.push(`会话 ${r.chatSessionDeleted} 个`)
    if (r.chatSessionSummaryDeleted) parts.push(`摘要 ${r.chatSessionSummaryDeleted} 条`)
    if (r.userMemoryProfileDeleted) parts.push(`画像 ${r.userMemoryProfileDeleted} 条`)
    if (r.esMemoryDeleted) parts.push(`ES 事实 ${r.esMemoryDeleted} 条`)
    if (r.userPrefDeleted) parts.push(`偏好 ${r.userPrefDeleted} 项`)
    if (r.memoryFileDeleted) parts.push(`记忆文件 ${r.memoryFileDeleted} 个`)
    const detail = parts.length ? ' (' + parts.join(' / ') + ')' : ''
    ElMessage.success(`已永久删除${total}条数据${detail}`)
    dialogVisible.value = false
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败: ' + ((e as Error)?.message || '未知错误'))
  } finally {
    forgetting.value = false
  }
}

/** 跳转"我的记忆"页 (同时关闭偏好设置弹窗). */
const router = useRouter()
function onGoMyMemory() {
  dialogVisible.value = false
  router.push('/user/my-memory')
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="偏好设置"
    width="760px"
    destroy-on-close
    append-to-body
    @closed="activeKey = 'memory-file-path'"
  >
    <div class="pref-layout">
      <!-- 左侧: 设置项导航 -->
      <aside class="pref-aside">
        <el-menu :default-active="activeKey" @select="selectItem">
          <el-menu-item v-for="it in items" :key="it.key" :index="it.key">
            <el-icon><component :is="it.icon" /></el-icon>
            <span>{{ it.label }}</span>
            <el-tag
              :type="it.status === 'ok' ? 'success' : 'info'"
              :effect="it.status === 'ok' ? 'dark' : 'plain'"
              size="small"
              class="pref-tag"
            >{{ it.status === 'ok' ? 'OK' : 'todo' }}</el-tag>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 右侧: 当前项内容 -->
      <section class="pref-content">
        <header class="pref-header">
          <h3>{{ currentItem?.label }}</h3>
          <p class="pref-desc" v-if="currentItem?.status === 'ok'">
            <el-icon><InfoFilled /></el-icon>
            <span v-if="currentItem.key === 'memory-file-path'">
              你的长期记忆 (关键事实 / 偏好) 将保存在此 Markdown 文件中, 可在弹窗中查看/编辑, 后端真实读写磁盘。
            </span>
            <span v-else-if="currentItem.key === 'default-model'">
              开启新会话时默认选用的模型; 留空表示跟随系统默认。
            </span>
            <span v-else-if="currentItem.key === 'chat-style'">
              AI 回答问题的语气与详略程度; 切换后, 下一次回答生效。
            </span>
            <span v-else-if="currentItem.key === 'long-memory'">
              开启后, 系统会从每轮对话中提取你的长期关键事实 (技术栈/职业/目标等), 跨会话注入到 AI 上下文; 关闭后仅保留已提取的事实, 不再新增。
            </span>
          </p>
          <p class="pref-desc" v-else>
            <el-icon><InfoFilled /></el-icon>
            该偏好项正在规划中, 后续版本开放。
          </p>
        </header>

        <!-- 记忆文件位置 -->
        <el-form v-if="currentItem?.key === 'memory-file-path'" label-position="top">
          <el-form-item label="记忆文件路径 (留空 = 使用默认, 由后端按当前系统动态生成)">
            <el-input
              v-model="form.memoryFilePath"
              :placeholder="loaded ? '由后端动态生成, 留空使用默认' : '加载中...'"
              :disabled="!loaded"
              clearable
            >
              <template #prefix>
                <el-icon><Folder /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-text type="info" size="small">
              默认路径由后端根据当前操作系统用户主目录动态拼出 (Windows: <code>C:\Users\你的用户名\.jc\jc-rag\memory\user_profile.md</code>; macOS/Linux: <code>/home/你的用户名/.jc/jc-rag/memory/user_profile.md</code>).
              <br />也可使用 <code>~</code> 简写, 后端落盘时会自动展开; 系统会在首次保存时自动创建中间目录。
            </el-text>
          </el-form-item>
          <el-form-item label="操作">
            <div class="memory-actions">
              <el-button :icon="Reading" @click="onViewMemoryFile">查看文件</el-button>
              <el-button :icon="Memo" @click="onEditMemoryFile">编辑并保存</el-button>
              <el-button :icon="FolderOpened" @click="onCopyPath">复制路径</el-button>
              <el-button
                v-if="supportsShowDirectoryPicker"
                type="primary"
                plain
                :icon="Setting"
                @click="onOpenInSystem"
              >在系统中打开</el-button>
            </div>
            <el-text v-if="!supportsShowDirectoryPicker" type="info" size="small">
              "在系统中打开" 仅支持 Chrome 86+ / Edge; 当前浏览器请使用"复制路径"后在资源管理器打开。
            </el-text>
          </el-form-item>
        </el-form>

        <!-- 默认模型 -->
        <el-form v-else-if="currentItem?.key === 'default-model'" label-position="top">
          <el-form-item label="默认模型 code (留空 = 跟随系统默认)">
            <el-input
              v-model="form.defaultModelCode"
              :placeholder="'例如: bge-large-zh-v1.5, 留空 = 系统默认'"
              clearable
            >
              <template #prefix>
                <el-icon><Cpu /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-text type="info" size="small">
              模型 code 须与平台提供的模型标识一致; 不确定时保持留空。
            </el-text>
          </el-form-item>
        </el-form>

        <!-- 回答风格 -->
        <el-form v-else-if="currentItem?.key === 'chat-style'" label-position="top">
          <el-form-item label="回答风格">
            <el-select v-model="form.chatStyle" style="width: 100%">
              <el-option
                v-for="opt in CHAT_STYLE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-text type="info" size="small">
              <strong>简洁:</strong> 短句, 直入主题, 适合快问快答<br />
              <strong>详细:</strong> 多步骤, 给原理与例子, 适合学习<br />
              <strong>友好:</strong> 语气温和, 鼓励式, 适合对话<br />
              <strong>专业:</strong> 严谨术语, 不啰嗦, 适合工程问题
            </el-text>
          </el-form-item>
        </el-form>

        <!-- 长期记忆开关 -->
        <el-form v-else-if="currentItem?.key === 'long-memory'" label-position="top">
          <el-form-item label="启用 L2 长期记忆 (按问题自动从历史对话中召回相关事实, 注入到 AI 上下文)">
            <el-switch
              v-model="form.enableLongMemory"
              active-text="开启"
              inactive-text="关闭"
              inline-prompt
              style="--el-switch-on-color: var(--el-color-primary);"
            />
          </el-form-item>
          <el-form-item>
            <el-text type="info" size="small">
              <strong>开启时</strong>: 每轮对话后, 系统会用 LLM 自动提取你的技术栈/职业/长期偏好/明确目标等关键事实, 向量化存入 ES.
              下次任何会话提问时, 系统会按当前问题检索最相关的事实注入到 AI 上下文, 实现"跨会话记忆".
              <br /><br />
              <strong>关闭时</strong>: 不再新增事实, 也不再注入历史事实; 已存在的事实不会被自动删除, 可在"我的记忆"页手动管理.
            </el-text>
          </el-form-item>
          <el-form-item label="操作">
            <el-button :icon="DataLine" @click="onGoMyMemory">查看 / 管理我的记忆</el-button>
          </el-form-item>
        </el-form>

        <!-- L3 我的画像开关 -->
        <el-form v-else-if="currentItem?.key === 'memory-profile'" label-position="top">
          <el-form-item label="启用 L3 我的画像 (基于 L2 长期事实, 周期性聚合并提炼为结构化画像, 注入 AI 上下文)">
            <el-switch
              v-model="form.enableMemoryProfile"
              active-text="开启"
              inactive-text="关闭"
              inline-prompt
              style="--el-switch-on-color: var(--el-color-primary);"
            />
          </el-form-item>
          <el-form-item>
            <el-text type="info" size="small">
              <strong>开启时</strong>: 每累计 10 轮对话, 后台会用 LLM 把 L2 长期事实聚合并提炼为五段结构化画像
              (领域 / 角色 / 技能 / 长期偏好 / 明确目标), 存入 user_memory_profile 表.
              之后每次问答, 画像会作为最顶层提示注入到 AI 上下文, 让 AI 跨会话直接知道"你是谁".
              <br /><br />
              <strong>关闭时</strong>: 不再聚合提炼, 也不注入画像; 已存在的画像不会被自动删除, 可在"我的记忆"页查看或重置.
              <br /><br />
              <strong>依赖</strong>: L2 长期记忆关闭时, L3 无数据源, 实际效果等价于关闭.
            </el-text>
          </el-form-item>
          <el-form-item label="操作">
            <el-button :icon="User" @click="onGoMyMemory">查看 / 重置我的画像</el-button>
          </el-form-item>
        </el-form>

        <el-form v-else-if="currentItem?.key === 'forget-me'" label-position="top">
          <el-alert type="warning" :closable="false" show-icon>
            <template #title>
              <strong>危险操作: 一键忘记我 (被遗忘权)</strong>
            </template>
            <p style="margin: 4px 0 0; line-height: 1.6;">
              物理删除你的全部对话数据 (L0 原文 + L1 摘要 + L2 长期事实 + L3 画像 + 偏好 + 记忆文件).
              <br />账号保留, 可重新登录, 但历史数据无法恢复.
            </p>
          </el-alert>
          <el-form-item label="操作" style="margin-top: 12px">
            <el-button
              type="danger"
              :icon="Delete"
              :loading="forgetting"
              @click="onForgetMe"
            >永久删除我的全部数据</el-button>
          </el-form-item>
        </el-form>

        <el-empty
          v-else
          :image-size="80"
          description="该偏好项尚未实现, 后续版本开放"
        />
      </section>
    </div>

    <template #footer>
      <template v-if="currentItem?.status === 'ok' && currentItem?.key !== 'forget-me'">
        <el-button :icon="RefreshRight" @click="onResetAll">恢复默认</el-button>
        <el-button :icon="Delete" @click="onClose">取消</el-button>
        <el-button type="primary" :icon="Setting" :loading="saving" @click="onSaveAll">
          保存
        </el-button>
      </template>
      <template v-else>
        <el-button type="primary" @click="onClose">关闭</el-button>
      </template>
    </template>
  </el-dialog>

  <!-- 记忆文件查看/编辑 弹窗 (独立组件, 兄弟节点, 避免嵌套 dialog 问题) -->
  <MemoryFileEditorDialog v-model="editorVisible" :mode="editorMode" />
</template>

<style scoped>
.pref-layout {
  display: flex;
  gap: 16px;
  min-height: 420px;
}
.pref-aside {
  width: 200px;
  flex: 0 0 200px;
  border-right: 1px solid var(--el-border-color-lighter);
  padding-right: 8px;
}
.pref-content {
  flex: 1;
  min-width: 0;
  padding-left: 8px;
}
.pref-header h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}
.pref-desc {
  margin: 0 0 20px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.pref-tag {
  margin-left: auto;
  font-size: 10px;
  height: 18px;
  line-height: 18px;
  padding: 0 6px;
}
:deep(.el-menu-item) {
  display: flex;
  align-items: center;
  gap: 6px;
}
.memory-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
