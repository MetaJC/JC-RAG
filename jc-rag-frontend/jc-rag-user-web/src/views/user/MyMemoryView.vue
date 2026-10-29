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
 * 我的记忆页 (C 端, L2 长期事实管理).
 * <p>列本用户在 ES jc_chat_memory 中的所有 active fact, 支持:
 * <ul>
 *   <li>单条软删 (DEL)</li>
 *   <li>批量清空 (清空本用户所有 active fact)</li>
 *   <li>按 category 筛选</li>
 *   <li>关键词搜索 (前端过滤, 服务端目前不提供)</li>
 * </ul>
 * <p>来源: UserChatServiceImpl 异步调 Python /chat/memory/extract 提取的事实.
 * <p>路由: /user/my-memory.
 *
 * @author JC
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Avatar, Cpu, DataAnalysis, Delete, Document,
  OfficeBuilding, Refresh, Search, Star, Trophy, User,
} from '@element-plus/icons-vue'

import {
  clearMyMemory,
  deleteMemory,
  fetchMyProfile,
  listMyMemory,
  resetMyProfile,
  type MemoryFactItem,
  type MemoryProfile,
  type MemoryProfileResult,
} from '../../api/memory'

const router = useRouter()

const loading = ref(false)
const total = ref(0)
const items = ref<MemoryFactItem[]>([])
const categoryFilter = ref<string>('')
const keyword = ref('')

/* ----------- L3 跨会话画像状态 ----------- */
const profileLoading = ref(true)
const hasFetchedProfile = ref(false)
const profile = ref<MemoryProfile | null>(null)
const profileMeta = ref<{ version?: number; factCount?: number; modelName?: string; lastExtractedAt?: string; updatedAt?: string }>({})

const categoryOptions = [
  { value: '', label: '全部' },
  { value: 'tech_stack', label: '技术栈' },
  { value: 'career', label: '职业' },
  { value: 'project', label: '项目背景' },
  { value: 'preference', label: '长期偏好' },
  { value: 'goal', label: '明确目标' },
  { value: 'other', label: '其他' },
]

const categoryLabelMap: Record<string, string> = {
  tech_stack: '技术栈',
  career: '职业',
  project: '项目背景',
  preference: '长期偏好',
  goal: '明确目标',
  other: '其他',
}

const categoryColorMap: Record<string, string> = {
  tech_stack: 'primary',
  career: 'success',
  project: 'warning',
  preference: 'info',
  goal: 'danger',
  other: '',
}

const filtered = computed(() => {
  if (!keyword.value.trim()) return items.value
  const kw = keyword.value.trim().toLowerCase()
  return items.value.filter(
    (it) =>
      (it.key || '').toLowerCase().includes(kw) ||
      (it.value || '').toLowerCase().includes(kw)
  )
})

async function refresh() {
  loading.value = true
  try {
    const data = await listMyMemory(1, 200)
    items.value = data.items || []
    total.value = data.total || items.value.length
  } catch (e) {
    console.error(e)
    ElMessage.error('加载失败: ' + (e as Error).message)
  } finally {
    loading.value = false
  }
  // 顺手拉 L3 画像
  await refreshProfile()
}

async function refreshProfile() {
  try {
    const r: MemoryProfileResult = await fetchMyProfile()
    if (r && r.exists && r.profile) {
      profile.value = r.profile
      profileMeta.value = {
        version: r.version,
        factCount: r.factCount,
        modelName: r.modelName,
        lastExtractedAt: r.lastExtractedAt,
        updatedAt: r.updatedAt,
      }
    } else {
      profile.value = null
      profileMeta.value = {}
    }
  } catch (e) {
    console.error(e)
    profile.value = null
  } finally {
    hasFetchedProfile.value = true
    profileLoading.value = false
  }
}

async function onResetProfile() {
  if (!profile.value) return
  try {
    await ElMessageBox.confirm(
      '重置后, 你的 L3 我的画像会被清空, 下次累计 10 轮对话后系统会重新生成. L2 长期事实不会受影响.\n确定要重置吗?',
      '重置画像',
      { confirmButtonText: '重置', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const r = await resetMyProfile()
    if (r.status === 'SUCCESS') {
      ElMessage.success(`已重置画像 (清理 ${r.removed} 条)`)
      await refreshProfile()
    } else {
      ElMessage.error('重置失败: ' + (r as { message?: string }).message)
    }
  } catch (e) {
    ElMessage.error('重置失败: ' + (e as Error).message)
  }
}

async function onDelete(row: MemoryFactItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除这条记忆吗?\n[${categoryLabelMap[row.category] || row.category}] ${row.key}: ${row.value}`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const r = await deleteMemory(row.memoryId)
    if (r.deleted) {
      ElMessage.success('已删除')
      await refresh()
    } else {
      ElMessage.error('删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败: ' + (e as Error).message)
  }
}

async function onClearAll() {
  try {
    await ElMessageBox.confirm(
      `确定要清空所有 ${total.value} 条记忆吗? 此操作只对当前用户生效, 不可撤销。`,
      '清空确认',
      { confirmButtonText: '清空', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const r = await clearMyMemory()
    ElMessage.success(`已清空 ${r.cleared} 条`)
    await refresh()
  } catch (e) {
    ElMessage.error('清空失败: ' + (e as Error).message)
  }
}

function onBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/chat')
  }
}

onMounted(refresh)
</script>

<template>
  <div class="memory-page">
    <header class="memory-header">
      <el-button :icon="ArrowLeft" plain @click="onBack">返回聊天</el-button>
      <h2>我的记忆</h2>
      <el-text type="info" size="small">
        L2 长期事实 + L3 跨会话画像 (由系统从历史对话中自动聚合并提炼; 跨会话生效)
      </el-text>
    </header>

    <!-- L3 跨会话画像: 大卡片 + 5 段彩色徽章 -->
    <section class="profile-section">
      <!-- 加载中: 简化卡 (只有 head + "加载中..."), 避免切真实内容时的视觉闪动 -->
      <div v-if="profileLoading" class="profile-card profile-card-loading">
        <div class="profile-card-head">
          <div class="profile-head-left">
            <el-avatar :size="48" class="profile-avatar">
              <el-icon><Avatar /></el-icon>
            </el-avatar>
            <div>
              <div class="profile-title">我的画像 (L3)</div>
              <div class="profile-sub">加载中...</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载完: 完整卡 (有 profile → 5 段; 无 profile → el-empty) -->
      <div v-else class="profile-card" :class="{ 'is-empty': !profile }">
        <div class="profile-card-head">
          <div class="profile-head-left">
            <el-avatar :size="48" class="profile-avatar">
              <el-icon><Avatar /></el-icon>
            </el-avatar>
            <div>
              <div class="profile-title">我的画像 (L3)</div>
              <div class="profile-sub">
                <template v-if="profile">
                  基于 {{ profileMeta.factCount ?? 0 }} 条长期事实聚合提炼 ·
                  版本 v{{ profileMeta.version ?? 1 }} ·
                  {{ profileMeta.modelName || 'LLM' }}
                </template>
                <template v-else>
                  暂无画像 · 每累计 10 轮对话后系统自动生成
                </template>
              </div>
            </div>
          </div>
          <div class="profile-head-right">
            <el-button
              v-if="profile"
              type="danger"
              plain
              :icon="Delete"
              size="small"
              @click="onResetProfile"
            >重置画像</el-button>
          </div>
        </div>

        <el-empty
          v-if="!profile"
          :image-size="80"
          description="开始和 AI 对话, 累计 10 轮后系统会从 L2 长期事实聚合并生成你的结构化画像"
        />

        <div v-else class="profile-segments">
          <!-- domain -->
          <div class="seg seg-domain">
            <div class="seg-icon"><el-icon><OfficeBuilding /></el-icon></div>
            <div class="seg-body">
              <div class="seg-label">从事领域</div>
              <div class="seg-value">{{ profile?.domain || '—' }}</div>
            </div>
          </div>
          <!-- role -->
          <div class="seg seg-role">
            <div class="seg-icon"><el-icon><User /></el-icon></div>
            <div class="seg-body">
              <div class="seg-label">当前角色</div>
              <div class="seg-value">{{ profile?.role || '—' }}</div>
            </div>
          </div>
          <!-- skills -->
          <div class="seg seg-skills">
            <div class="seg-icon"><el-icon><Cpu /></el-icon></div>
            <div class="seg-body">
              <div class="seg-label">技能栈</div>
              <div class="seg-tags">
                <el-tag
                  v-for="(s, i) in (profile?.skills || [])"
                  :key="i"
                  type="primary"
                  effect="dark"
                  round
                  class="seg-badge"
                >{{ s }}</el-tag>
                <span v-if="!(profile?.skills || []).length" class="seg-empty">—</span>
              </div>
            </div>
          </div>
          <!-- preferences -->
          <div class="seg seg-prefs">
            <div class="seg-icon"><el-icon><DataAnalysis /></el-icon></div>
            <div class="seg-body">
              <div class="seg-label">长期偏好</div>
              <div class="seg-tags">
                <el-tag
                  v-for="(v, k) in (profile?.preferences || {})"
                  :key="String(k)"
                  type="info"
                  effect="dark"
                  round
                  class="seg-badge"
                >{{ k }}: {{ v }}</el-tag>
                <span v-if="!Object.keys(profile?.preferences || {}).length" class="seg-empty">—</span>
              </div>
            </div>
          </div>
          <!-- goals -->
          <div class="seg seg-goals">
            <div class="seg-icon"><el-icon><Trophy /></el-icon></div>
            <div class="seg-body">
              <div class="seg-label">明确目标</div>
              <div class="seg-tags">
                <el-tag
                  v-for="(g, i) in (profile?.goals || [])"
                  :key="i"
                  type="warning"
                  effect="dark"
                  round
                  class="seg-badge"
                >{{ g }}</el-tag>
                <span v-if="!(profile?.goals || []).length" class="seg-empty">—</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="profile && profileMeta.lastExtractedAt" class="profile-foot">
          最近提炼: {{ profileMeta.lastExtractedAt }} · 更新于 {{ profileMeta.updatedAt }}
        </div>
      </div>
    </section>

    <div class="memory-toolbar">
      <el-input
        v-model="keyword"
        :prefix-icon="Search"
        placeholder="搜索 key 或 value"
        clearable
        style="width: 240px"
      />
      <el-select v-model="categoryFilter" placeholder="按分类筛选" clearable style="width: 160px">
        <el-option
          v-for="opt in categoryOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-button :icon="Refresh" :loading="loading" @click="refresh">刷新</el-button>
      <el-button
        type="danger"
        plain
        :icon="Delete"
        :disabled="total === 0"
        @click="onClearAll"
      >清空全部</el-button>
    </div>

    <el-empty
      v-if="!loading && filtered.length === 0"
      :image-size="100"
      description="暂无记忆; 开始和 AI 对话, 系统会自动提取你的关键事实"
    />

    <div v-else class="memory-grid">
      <div v-for="it in filtered" :key="it.memoryId" class="memory-card">
        <div class="card-top">
          <el-tag
            :type="(categoryColorMap[it.category] || 'info') as 'primary' | 'success' | 'warning' | 'info' | 'danger' | ''"
            size="small"
            effect="light"
          >{{ categoryLabelMap[it.category] || it.category }}</el-tag>
          <el-text type="info" size="small">
            <el-icon><Document /></el-icon>
            <span>{{ it.source === 'auto' ? '自动' : '手动' }}</span>
          </el-text>
        </div>
        <div class="card-key">
          <el-icon><Star /></el-icon>
          <strong>{{ it.key }}</strong>
        </div>
        <div class="card-value">{{ it.value }}</div>
        <div class="card-bottom">
          <el-text type="info" size="small">
            <span v-if="it.updatedAt">更新: {{ it.updatedAt }}</span>
          </el-text>
          <el-button
            type="danger"
            link
            :icon="Delete"
            size="small"
            @click="onDelete(it)"
          >删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.memory-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 16px;
}
.memory-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.memory-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.memory-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
  flex-wrap: wrap;
}
.memory-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}
.memory-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px 14px;
  background: var(--el-bg-color);
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: box-shadow 0.2s;
}
.memory-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.card-top .el-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.card-key {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.card-key strong {
  font-weight: 600;
}
.card-value {
  font-size: 14px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  word-break: break-word;
}
.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

/* ----------- L3 用户画像大卡 + 5 段彩色徽章 ----------- */
.profile-section {
  margin-bottom: 20px;
}
.profile-card {
  position: relative;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  padding: 18px 20px 14px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
}
.profile-card:hover {
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.08);
}
.profile-card.is-empty {
  background: var(--el-bg-color);
}
.profile-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.profile-head-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.profile-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
.profile-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.4;
}
.profile-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  margin-top: 2px;
}
.profile-foot {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-lighter);
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: right;
}
.profile-segments {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.seg {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--el-bg-color);
  border-left: 3px solid;
}
.seg-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  flex-shrink: 0;
}
.seg-body {
  flex: 1;
  min-width: 0;
}
.seg-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}
.seg-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  word-break: break-word;
}
.seg-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
.seg-empty {
  color: var(--el-text-color-placeholder);
  font-size: 14px;
}
.seg-badge {
  font-weight: 500;
}

/* 5 段主题色: 蓝/绿/紫/青/橙, 配左 border + 图标背景 */
.seg-domain {
  border-left-color: #409eff;
}
.seg-domain .seg-icon { background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%); }
.seg-role {
  border-left-color: #67c23a;
}
.seg-role .seg-icon { background: linear-gradient(135deg, #67c23a 0%, #95d475 100%); }
.seg-skills {
  border-left-color: #9c27b0;
}
.seg-skills .seg-icon { background: linear-gradient(135deg, #9c27b0 0%, #ba68c8 100%); }
.seg-prefs {
  border-left-color: #00bcd4;
}
.seg-prefs .seg-icon { background: linear-gradient(135deg, #00bcd4 0%, #4dd0e1 100%); }
.seg-goals {
  border-left-color: #e6a23c;
}
.seg-goals .seg-icon { background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%); }
</style>
