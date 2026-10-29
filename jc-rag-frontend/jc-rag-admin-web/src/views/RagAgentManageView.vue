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
 * 智能体管理 (拆自 AgentSkillManageView, 2026-07-25)
 * <p>集中管理: 系统智能体 (后台管控, C 端不可见) / 内建用户智能体 (后台创建, C 端可见) / C 端用户自建智能体 (审计).
 * <p>Skill 管理独立到 RagSkillManageView, 本页通过 skills ref 引用 Skill 列表供 Agent 绑定.
 *
 * @author JC
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, QuestionFilled, Search, Star, StarFilled, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import { fetchRbacRoles } from '../api/rbac'
import {
  fetchRagSkills,
  fetchRagAgents, createRagAgent, updateRagAgent, deleteRagAgent,
  publishRagAgent, unpublishRagAgent,
  setDefaultAgent, unsetDefaultAgent, getDefaultAgent,
  fetchAdminUserCustomAgents, getAdminUserCustomAgent, forceDeleteUserCustomAgent,
  updateAdminUserCustomAgentStatus,
} from '../api/rag'

const activeTab = ref('system')

/* ====================== Skills (供 Agent 绑定) ====================== */
const skills = ref<any[]>([])

async function loadSkills() {
  const skillRows = await fetchRagSkills()
  skills.value = (skillRows.records ?? (skillRows || [])).map((item: any) => ({
    id: item.id,
    skillName: item.skill_name ?? item.skillName,
  }))
}

/* ====================== 系统智能体 ====================== */
const systemAgents = ref<any[]>([])
const roles = ref<any[]>([])
const defaultAgentId = ref<number | null>(null)
const systemDialog = ref(false)
const editingSystemId = ref<number | null>(null)
const systemForm = ref({
  agentName: '', agentKey: '', description: '', systemPrompt: '', content: '',
  temperature: 0.7, maxTokens: 2048,
  skillIds: [] as number[], roleIds: [] as number[], isSystem: 1 as number,
})

async function loadSystemAgents() {
  const [agentRows, roleRows, defaultId] = await Promise.all([
    fetchRagAgents({ pageNum: 1, pageSize: 100, isSystem: 1 } as any),
    fetchRbacRoles({ page: 1, size: 100 }),
    getDefaultAgent().catch(() => null),
  ])
  systemAgents.value = (agentRows.records ?? (agentRows || [])).map((item: any) => ({
    id: item.id,
    agentName: item.agent_name ?? item.agentName,
    agentKey: item.agent_key ?? item.agentKey,
    description: item.description ?? '',
    systemPrompt: item.system_prompt ?? item.systemPrompt ?? '',
    temperature: item.temperature ?? 0.7,
    maxTokens: item.max_tokens ?? item.maxTokens ?? 2048,
    status: item.status ?? 0,
    skills: item.skills ?? [],
    roles: item.roles ?? [],
  }))
  roles.value = (roleRows.records ?? (roleRows || [])).map((item: any) => ({ id: item.id, roleName: item.roleName ?? item.role_name }))
  defaultAgentId.value = (defaultId as any)?.data ?? defaultId ?? null
}

function buildAgentKeyPreview(agentName: string) {
  const base = String(agentName ?? '').trim().toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '')
  return base || '保存后自动生成'
}

function openSystemAgent(item?: any) {
  editingSystemId.value = item?.id ?? null
  if (item) {
    systemForm.value = {
      agentName: item.agentName, agentKey: item.agentKey, description: item.description,
      systemPrompt: item.systemPrompt, content: '',
      temperature: item.temperature, maxTokens: item.maxTokens,
      skillIds: (item.skills || []).map((s: any) => s.id),
      roleIds: (item.roles || []).map((r: any) => r.id),
      isSystem: 1,
    }
  } else {
    systemForm.value = { agentName: '', agentKey: '', description: '', systemPrompt: '', content: '', temperature: 0.7, maxTokens: 2048, skillIds: [], roleIds: [], isSystem: 1 }
  }
  systemDialog.value = true
}

async function saveSystemAgent() {
  if (!systemForm.value.agentName?.trim()) { ElMessage.warning('请填写名称'); return }
  if (!systemForm.value.systemPrompt?.trim()) { ElMessage.warning('系统智能体必须填写 systemPrompt'); return }
  try {
    if (editingSystemId.value) {
      await updateRagAgent(editingSystemId.value, systemForm.value)
    } else {
      await createRagAgent(systemForm.value)
    }
    systemDialog.value = false
    await loadSystemAgents()
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
}

async function publishSystemAgent(id: number) { await publishRagAgent(id); await loadSystemAgents(); ElMessage.success('发布成功') }
async function unpublishSystemAgent(id: number) { await unpublishRagAgent(id); await loadSystemAgents(); ElMessage.success('下架成功') }

async function removeSystemAgent(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该系统智能体？', '删除确认', { type: 'warning' })
    await deleteRagAgent(id)
    if (defaultAgentId.value === id) defaultAgentId.value = null
    await loadSystemAgents()
    ElMessage.success('删除成功')
  } catch {}
}

async function markAsDefault(id: number) {
  try {
    await setDefaultAgent(id)
    defaultAgentId.value = id
    ElMessage.success('已设为默认智能体 (C 端无感知, 强制使用)')
  } catch (e: any) { ElMessage.error(e?.message || '设置失败') }
}

async function clearDefault() {
  try { await ElMessageBox.confirm('确定取消默认智能体吗？', '取消默认', { type: 'warning' }) } catch { return }
  try { await unsetDefaultAgent(); defaultAgentId.value = null; ElMessage.success('已取消默认智能体') }
  catch (e: any) { ElMessage.error(e?.message || '取消失败') }
}

/* ====================== 内建用户智能体 ====================== */
const builtInAgents = ref<any[]>([])
const builtInDialog = ref(false)
const editingBuiltInId = ref<number | null>(null)
const builtInForm = ref({
  agentName: '', agentKey: '', description: '', systemPrompt: '', content: '',
  temperature: 0.7, maxTokens: 2048,
  skillIds: [] as number[], roleIds: [] as number[], isSystem: 0 as number,
})

async function loadBuiltInAgents() {
  const rows: any = await fetchRagAgents({ pageNum: 1, pageSize: 100, isSystem: 0 } as any)
  builtInAgents.value = (rows.records ?? rows ?? []).map((item: any) => ({
    id: item.id,
    agentName: item.agent_name ?? item.agentName,
    agentKey: item.agent_key ?? item.agentKey,
    description: item.description ?? '',
    content: item.content ?? '',
    temperature: item.temperature ?? 0.7,
    maxTokens: item.max_tokens ?? item.maxTokens ?? 2048,
    status: item.status ?? 0,
    skills: item.skills ?? [],
    roles: item.roles ?? [],
  }))
}

function openBuiltInAgent(item?: any) {
  editingBuiltInId.value = item?.id ?? null
  if (item) {
    builtInForm.value = {
      agentName: item.agentName, agentKey: item.agentKey, description: item.description,
      systemPrompt: '', content: item.content,
      temperature: item.temperature, maxTokens: item.maxTokens,
      skillIds: (item.skills || []).map((s: any) => s.id),
      roleIds: (item.roles || []).map((r: any) => r.id),
      isSystem: 0,
    }
  } else {
    builtInForm.value = { agentName: '', agentKey: '', description: '', systemPrompt: '', content: '', temperature: 0.7, maxTokens: 2048, skillIds: [], roleIds: [], isSystem: 0 }
  }
  builtInDialog.value = true
}

async function saveBuiltInAgent() {
  if (!builtInForm.value.agentName?.trim()) { ElMessage.warning('请填写名称'); return }
  if (!builtInForm.value.content?.trim()) { ElMessage.warning('内建用户智能体必须填写 content (user prompt)'); return }
  try {
    if (editingBuiltInId.value) {
      await updateRagAgent(editingBuiltInId.value, builtInForm.value)
    } else {
      await createRagAgent(builtInForm.value)
    }
    builtInDialog.value = false
    await loadBuiltInAgents()
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
}

async function publishBuiltInAgent(id: number) { await publishRagAgent(id); await loadBuiltInAgents(); ElMessage.success('发布成功') }
async function unpublishBuiltInAgent(id: number) { await unpublishRagAgent(id); await loadBuiltInAgents(); ElMessage.success('下架成功') }

async function removeBuiltInAgent(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该内建智能体？', '删除确认', { type: 'warning' })
    await deleteRagAgent(id)
    await loadBuiltInAgents()
    ElMessage.success('删除成功')
  } catch {}
}

/* ====================== C 端用户自建智能体 (审计) ====================== */
const userAgents = ref<any[]>([])
const userAgentTotal = ref(0)
const userAgentPageNum = ref(1)
const userAgentPageSize = ref(20)
const userAgentKeyword = ref('')
const userAgentStatus = ref<number | ''>('')

async function loadUserAgents() {
  try {
    const data: any = await fetchAdminUserCustomAgents({
      pageNum: userAgentPageNum.value,
      pageSize: userAgentPageSize.value,
      keyword: userAgentKeyword.value || undefined,
      status: userAgentStatus.value === '' ? undefined : (userAgentStatus.value as number),
    })
    userAgents.value = (data.records ?? []).map((item: any) => ({
      id: item.id, userId: item.userId,
      username: item.username || '-', nickname: item.nickname || '',
      agentName: item.agentName, content: item.content || '',
      status: item.status ?? 1, createdAt: item.createdAt, updatedAt: item.updatedAt,
    }))
    userAgentTotal.value = data.total ?? 0
  } catch (e: any) {
    ElMessage.error(e?.message || '加载用户智能体失败')
  }
}

function onUserAgentSearch() { userAgentPageNum.value = 1; loadUserAgents() }
function onUserAgentPageChange(p: number) { userAgentPageNum.value = p; loadUserAgents() }
function onUserAgentSizeChange(s: number) { userAgentPageSize.value = s; userAgentPageNum.value = 1; loadUserAgents() }

const userAgentDetailDialog = ref(false)
const userAgentDetail = ref<any>(null)

async function viewUserAgent(id: number) {
  try {
    userAgentDetail.value = await getAdminUserCustomAgent(id)
    userAgentDetailDialog.value = true
  } catch (e: any) { ElMessage.error(e?.message || '加载失败') }
}

async function toggleUserAgentStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确认将该用户智能体「${row.agentName}」${newStatus === 1 ? '启用' : '禁用'}？`,
      '状态变更确认', { type: 'warning' }
    )
    await updateAdminUserCustomAgentStatus(row.id, newStatus)
    ElMessage.success('已更新')
    await loadUserAgents()
  } catch {}
}

async function forceRemoveUserAgent(id: number, name: string) {
  try {
    await ElMessageBox.confirm(
      `确认强制删除用户智能体「${name}」？此操作会写入审计日志且不可恢复。`,
      '强制删除确认', { type: 'error' }
    )
    await forceDeleteUserCustomAgent(id)
    ElMessage.success('已强制删除')
    await loadUserAgents()
  } catch {}
}

/* ====================== 初始加载 ====================== */
onMounted(async () => {
  await Promise.all([loadSkills(), loadSystemAgents(), loadBuiltInAgents()])
  await loadUserAgents()
})
</script>

<template>
  <div class="page-wrap">
    <div class="page-head">
      <div>
        <h1>智能体管理</h1>
        <p>集中管理智能体: 系统管控智能体 / 内建用户智能体 / C 端用户自建智能体审计。Skill 管理请前往「技能管理」页面。</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- 系统智能体 -->
        <el-tab-pane label="系统智能体" name="system">
          <div class="agent-tip" v-if="defaultAgentId">
            <el-alert type="success" :closable="false" show-icon>
              <template #title>
                <span>当前默认智能体已设置, C 端用户无感知强制使用。可在列表中点击 <el-icon><StarFilled /></el-icon> 切换 (仅系统智能体可设默认)。</span>
              </template>
            </el-alert>
          </div>
          <div class="filter-bar">
            <el-text type="info" size="small">
              系统智能体 = 后台管控, C 端不可见, 可作为默认强制使用, 走 system prompt。
            </el-text>
            <div class="toolbar-spacer" />
            <el-button v-if="defaultAgentId" link type="warning" @click="clearDefault">取消默认</el-button>
            <el-button type="primary" @click="openSystemAgent()">新建系统智能体</el-button>
          </div>
          <el-table :data="systemAgents" stripe>
            <el-table-column label="默认" width="80" align="center">
              <template #default="{ row }">
                <el-tooltip :content="row.id === defaultAgentId ? '当前默认智能体' : (row.status === 1 ? '设为默认' : '需先发布')" placement="top">
                  <el-button link :type="row.id === defaultAgentId ? 'warning' : 'default'"
                    :icon="row.id === defaultAgentId ? StarFilled : Star"
                    :disabled="row.status !== 1"
                    @click="row.id !== defaultAgentId && markAsDefault(row.id)" />
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="agentName" label="名称" min-width="160" />
            <el-table-column prop="agentKey" label="标识" min-width="160" />
            <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">{{ ['草稿', '已发布', '已下架'][row.status] || '未知' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-tooltip content="编辑" placement="top">
                  <el-button link type="primary" :icon="Edit" @click="openSystemAgent(row)" />
                </el-tooltip>
                <el-tooltip :content="row.status === 1 ? '下架' : '发布'" placement="top">
                  <el-button v-if="row.status !== 1" link type="success" :icon="VideoPlay" @click="publishSystemAgent(row.id)" />
                  <el-button v-else link type="warning" :icon="VideoPause" @click="unpublishSystemAgent(row.id)" />
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button link type="danger" :icon="Delete" @click="removeSystemAgent(row.id)" />
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 内建用户智能体 -->
        <el-tab-pane label="内建用户智能体" name="built-in">
          <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
            <template #title>
              内建用户智能体 = 后台创建, 所有 C 端用户可见可选, 走 user prompt, 不可作为默认智能体。
            </template>
          </el-alert>
          <div class="sub-section-head">
            <h3>内建用户智能体</h3>
            <el-button type="primary" @click="openBuiltInAgent()">新建内建智能体</el-button>
          </div>
          <el-table :data="builtInAgents" stripe>
            <el-table-column prop="agentName" label="名称" min-width="160" />
            <el-table-column prop="agentKey" label="标识" min-width="160" />
            <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">{{ ['草稿', '已发布', '已下架'][row.status] || '未知' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-tooltip content="编辑" placement="top">
                  <el-button link type="primary" :icon="Edit" @click="openBuiltInAgent(row)" />
                </el-tooltip>
                <el-tooltip :content="row.status === 1 ? '下架' : '发布'" placement="top">
                  <el-button v-if="row.status !== 1" link type="success" :icon="VideoPlay" @click="publishBuiltInAgent(row.id)" />
                  <el-button v-else link type="warning" :icon="VideoPause" @click="unpublishBuiltInAgent(row.id)" />
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button link type="danger" :icon="Delete" @click="removeBuiltInAgent(row.id)" />
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- C 端用户自建智能体 (审计) -->
        <el-tab-pane label="C 端用户智能体" name="user-agents">
          <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
            <template #title>
              C 端用户自建智能体 = C 端用户创建, 仅本人可见, 后台仅做审计/启停/强制删除, 不允许编辑。
            </template>
          </el-alert>
          <div class="filter-bar">
            <el-input v-model="userAgentKeyword" placeholder="搜索智能体名 / 提示词 / 用户名" clearable
              :prefix-icon="Search" style="width: 280px"
              @keyup.enter="onUserAgentSearch" @clear="onUserAgentSearch" />
            <el-select v-model="userAgentStatus" placeholder="状态" clearable style="width: 140px" @change="onUserAgentSearch">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="onUserAgentSearch">查询</el-button>
            <el-text type="info" size="small">共 {{ userAgentTotal }} 条; 此处仅做审计/管理/查看, 不允许编辑用户内容</el-text>
          </div>
          <el-table :data="userAgents" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="agentName" label="智能体名" min-width="160" />
            <el-table-column label="创建者" min-width="180">
              <template #default="{ row }">
                <div class="creator-cell">
                  <span class="creator-username">{{ row.username }}</span>
                  <span v-if="row.nickname" class="creator-nickname">({{ row.nickname }})</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="提示词内容" min-width="280" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="content-preview">{{ row.content || '(空)' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="160" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-tooltip content="查看详情" placement="top">
                  <el-button link type="primary" :icon="Search" @click="viewUserAgent(row.id)" />
                </el-tooltip>
                <el-tooltip :content="row.status === 1 ? '禁用' : '启用'" placement="top">
                  <el-button link :type="row.status === 1 ? 'warning' : 'success'"
                    :icon="row.status === 1 ? VideoPause : VideoPlay"
                    @click="toggleUserAgentStatus(row)" />
                </el-tooltip>
                <el-tooltip content="强制删除 (审计)" placement="top">
                  <el-button link type="danger" :icon="Delete" @click="forceRemoveUserAgent(row.id, row.agentName)" />
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="userAgentPageNum"
            v-model:page-size="userAgentPageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="userAgentTotal"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="onUserAgentPageChange"
            @size-change="onUserAgentSizeChange"
            style="margin-top: 16px; justify-content: flex-end"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 系统智能体 Dialog -->
    <el-dialog v-model="systemDialog" :title="editingSystemId ? '编辑系统智能体' : '新建系统智能体'" width="760px">
      <el-form label-position="top">
        <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
          <template #title>系统智能体 = 后台管控, C 端不可见; 走 system prompt, 可作为默认强制使用。</template>
        </el-alert>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span class="required-mark">*</span><span>名称</span>
              <el-tooltip content="智能体名称, 在后台列表展示。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-input v-model="systemForm.agentName" />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span>标识键</span>
              <el-tooltip content="由系统根据名称自动生成, 用于程序识别, 无需手动录入。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-input :model-value="editingSystemId ? systemForm.agentKey : buildAgentKeyPreview(systemForm.agentName)" readonly />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span>描述</span>
              <el-tooltip content="说明该 Agent 定位/适用场景, 便于管理员区分。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-input v-model="systemForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span class="required-mark">*</span><span>系统提示词 (system prompt)</span>
              <el-tooltip content="定义 Agent 的角色/行为边界/回答风格, 是该智能体的核心指令。系统智能体走 system message。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-input v-model="systemForm.systemPrompt" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span>关联 Skills</span>
              <el-tooltip content="为 Agent 绑定可调用的技能能力。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-select v-model="systemForm.skillIds" multiple style="width: 100%">
            <el-option v-for="item in skills" :key="item.id" :label="item.skillName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span>关联角色</span>
              <el-tooltip content="控制哪些角色可见/可用该 Agent。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-select v-model="systemForm.roleIds" multiple style="width: 100%">
            <el-option v-for="item in roles" :key="item.id" :label="item.roleName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="systemDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSystemAgent">保存</el-button>
      </template>
    </el-dialog>

    <!-- 内建用户智能体 Dialog -->
    <el-dialog v-model="builtInDialog" :title="editingBuiltInId ? '编辑内建用户智能体' : '新建内建用户智能体'" width="760px">
      <el-form label-position="top">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
          <template #title>内建用户智能体 = 后台创建, C 端所有用户可见可选; 走 user prompt, 不可作默认。</template>
        </el-alert>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span class="required-mark">*</span><span>名称</span>
              <el-tooltip content="智能体名称, 在 C 端智能体列表展示给用户。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-input v-model="builtInForm.agentName" />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span>标识键</span>
              <el-tooltip content="由系统根据名称自动生成, 用于程序识别, 无需手动录入。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-input :model-value="editingBuiltInId ? builtInForm.agentKey : buildAgentKeyPreview(builtInForm.agentName)" readonly />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span>描述</span>
              <el-tooltip content="说明该 Agent 定位/适用场景, 便于用户选用。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-input v-model="builtInForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span class="required-mark">*</span><span>用户提示词 (user prompt)</span>
              <el-tooltip content="作为 user message 增强片段, 拼接到系统消息之后。内建用户智能体走 user layer, 所有 C 端用户可见可选用。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-input v-model="builtInForm.content" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span>关联 Skills</span>
              <el-tooltip content="为 Agent 绑定可调用的技能能力。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-select v-model="builtInForm.skillIds" multiple style="width: 100%">
            <el-option v-for="item in skills" :key="item.id" :label="item.skillName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <template #label>
            <span class="field-label">
              <span>关联角色</span>
              <el-tooltip content="控制哪些角色可见/可用该 Agent。" placement="top">
                <el-icon class="field-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-select v-model="builtInForm.roleIds" multiple style="width: 100%">
            <el-option v-for="item in roles" :key="item.id" :label="item.roleName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="builtInDialog = false">取消</el-button>
        <el-button type="primary" @click="saveBuiltInAgent">保存</el-button>
      </template>
    </el-dialog>

    <!-- C 端用户智能体详情 Dialog -->
    <el-dialog v-model="userAgentDetailDialog" title="用户智能体详情" width="640px">
      <div v-if="userAgentDetail" class="detail-view">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">{{ userAgentDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="智能体名">{{ userAgentDetail.agentName }}</el-descriptions-item>
          <el-descriptions-item label="创建者">
            {{ userAgentDetail.username }}<span v-if="userAgentDetail.nickname"> ({{ userAgentDetail.nickname }})</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="userAgentDetail.status === 1 ? 'success' : 'info'" size="small">
              {{ userAgentDetail.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ userAgentDetail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ userAgentDetail.updatedAt }}</el-descriptions-item>
          <el-descriptions-item label="提示词内容">
            <pre class="content-pre">{{ userAgentDetail.content }}</pre>
          </el-descriptions-item>
        </el-descriptions>
        <el-alert type="info" :closable="false" show-icon style="margin-top: 12px">
          <template #title>用户数据归用户所有, 后台仅做审计/管理/查看, 不允许编辑。</template>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="userAgentDetailDialog = false">关闭</el-button>
        <el-button v-if="userAgentDetail" type="danger" plain
          @click="userAgentDetailDialog = false; forceRemoveUserAgent(userAgentDetail.id, userAgentDetail.agentName)">
          强制删除
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-wrap { padding: 24px; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-head h1 { margin: 0 0 6px; font-size: 24px; }
.page-head p { margin: 0; color: #6b7280; }
.toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; gap: 8px; }
.toolbar-spacer { flex: 1; }
.field-label { display: inline-flex; align-items: center; gap: 6px; }
.field-help { color: #909399; cursor: help; }
.required-mark { color: #f56c6c; font-weight: 700; }
.agent-tip { margin-bottom: 12px; }
.filter-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
.creator-cell { display: flex; flex-direction: column; line-height: 1.4; }
.creator-username { font-weight: 600; }
.creator-nickname { color: #909399; font-size: 12px; }
.content-preview { color: #4b5563; }
.content-pre { white-space: pre-wrap; word-break: break-word; margin: 0; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 13px; max-height: 320px; overflow-y: auto; background: #f9fafb; padding: 12px; border-radius: 6px; }
.detail-view { padding: 0 8px; }
.sub-section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.sub-section-head h3 { margin: 0; font-size: 16px; font-weight: 600; }
</style>
