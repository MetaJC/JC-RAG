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
 * 敏感词管理页 (Admin).
 * <p>提供敏感词的 CRUD + 批量导入 + 启用/禁用. 数据源 sys_sensitive_word 表.
 * <p>词库全量同步到内存 Aho-Corasick 引擎, 实时生效.
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Search, Upload, Document, Download } from '@element-plus/icons-vue'
import {
  fetchSensitiveWords,
  createSensitiveWord,
  updateSensitiveWord,
  deleteSensitiveWord,
  toggleSensitiveWord,
  batchImportSensitiveWords,
  type SensitiveWordVO,
} from '../api/sys'

const loading = ref(false)
const list = ref<SensitiveWordVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', riskLevel: '' })

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editing = ref<Partial<SensitiveWordVO>>({})
const saving = ref(false)

const batchDialogVisible = ref(false)
const batchText = ref('')

const RISK_LEVELS = [
  { value: 'HIGH', label: '高', type: 'danger' },
  { value: 'MEDIUM', label: '中', type: 'warning' },
  { value: 'LOW', label: '低', type: 'info' },
]
const ACTIONS = [
  { value: 'DENY', label: '拒绝', desc: '直接拒绝请求' },
  { value: 'ALERT', label: '告警', desc: '放行 + 记录审计' },
  { value: 'MASK', label: '脱敏', desc: '替换为***后放行' },
  { value: 'PASS', label: '仅审计', desc: '放行 + 不动作' },
]
const CATEGORIES = [
  { value: 'PROMPT_INJECTION', label: '提示词注入' },
  { value: 'CREDENTIAL', label: '凭据泄露' },
  { value: 'PII', label: '个人敏感信息' },
  { value: 'ABUSE', label: '辱骂/违规' },
  { value: 'OTHER', label: '其他' },
]

async function load() {
  loading.value = true
  try {
    const res: any = await fetchSensitiveWords({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      riskLevel: query.riskLevel || undefined,
    })
    list.value = res?.records ?? res ?? []
    total.value = res?.total ?? list.value.length
  } catch (e) {
    ElMessage.error('加载敏感词失败')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  editing.value = { word: '', riskLevel: 'HIGH', action: 'DENY', category: 'OTHER', enabled: 1, description: '' }
  dialogVisible.value = true
}

function openEdit(row: SensitiveWordVO) {
  dialogMode.value = 'edit'
  editing.value = { ...row }
  dialogVisible.value = true
}

async function save() {
  if (!editing.value.word?.trim()) {
    ElMessage.warning('请输入敏感词')
    return
  }
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      await createSensitiveWord(editing.value)
      ElMessage.success('创建成功')
    } else {
      await updateSensitiveWord(editing.value)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: SensitiveWordVO) {
  try {
    await ElMessageBox.confirm(`确定删除敏感词 [${row.word}] 吗?`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteSensitiveWord(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

async function toggle(row: SensitiveWordVO) {
  try {
    await toggleSensitiveWord(row.id)
    ElMessage.success(row.enabled === 1 ? '已禁用' : '已启用')
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function openBatchImport() {
  batchText.value = ''
  batchDialogVisible.value = true
}

async function submitBatch() {
  const lines = batchText.value
    .split('\n')
    .map((s) => s.trim())
    .filter((s) => s && !s.startsWith('#'))
  if (lines.length === 0) {
    ElMessage.warning('请输入至少一行内容')
    return
  }
  try {
    const res: any = await batchImportSensitiveWords(lines)
    ElMessage.success(`导入成功 ${res?.imported ?? 0} 条`)
    batchDialogVisible.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败')
  }
}

function loadPreset() {
  batchText.value = [
    '# 格式: 敏感词|风险等级|处置动作|分类|说明',
    '# 风险等级: HIGH/MEDIUM/LOW',
    '# 处置动作: DENY/ALERT/MASK/PASS',
    '# 分类: PROMPT_INJECTION/CREDENTIAL/PII/ABUSE/OTHER',
    '忽略之前的指令|HIGH|DENY|PROMPT_INJECTION|提示词注入',
    '系统提示词|HIGH|ALERT|PROMPT_INJECTION|试探系统提示',
    '请扮演|HIGH|ALERT|PROMPT_INJECTION|角色劫持',
    '你的密码是|HIGH|ALERT|CREDENTIAL|凭据试探',
    'API[Kk]ey|LOW|MASK|CREDENTIAL|密钥提示',
  ].join('\n')
}

function exportPreset() {
  const presets = [
    '忽略之前的指令|HIGH|DENY|PROMPT_INJECTION|提示词注入',
    '系统提示词|HIGH|ALERT|PROMPT_INJECTION|试探系统提示',
    '请扮演|HIGH|ALERT|PROMPT_INJECTION|角色劫持',
  ]
  const blob = new Blob([presets.join('\n')], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'sensitive-words-template.txt'
  a.click()
  URL.revokeObjectURL(url)
}

function getRiskTagType(level: string | null | undefined): 'danger' | 'warning' | 'info' {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

function getActionTagType(action: string | null | undefined): string {
  if (action === 'DENY') return 'danger'
  if (action === 'ALERT') return 'warning'
  if (action === 'MASK') return 'primary'
  return 'info'
}

onMounted(load)
</script>

<template>
  <div class="sensitive-page">
    <div class="page-header">
      <h2>敏感词管理</h2>
      <p class="page-desc">配置全局敏感词库, 系统自动拦截/告警/脱敏. 词库修改后即时生效 (无需重启).</p>
    </div>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索敏感词"
        clearable
        :prefix-icon="Search"
        class="search-input"
        @keyup.enter="() => { query.page = 1; load() }"
        @clear="() => { query.page = 1; load() }"
      />
      <el-select v-model="query.riskLevel" placeholder="风险等级" clearable class="filter-select" @change="() => { query.page = 1; load() }">
        <el-option v-for="l in RISK_LEVELS" :key="l.value" :label="l.label" :value="l.value" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="() => { query.page = 1; load() }">查询</el-button>
      <div class="toolbar-spacer" />
      <el-button type="primary" :icon="Plus" @click="openCreate">新增敏感词</el-button>
      <el-button :icon="Upload" @click="openBatchImport">批量导入</el-button>
      <el-button :icon="Download" @click="exportPreset">导出模板</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe class="word-table">
      <el-table-column type="index" label="#" width="60" :index="(idx: number) => (query.page - 1) * query.size + idx + 1" />
      <el-table-column prop="word" label="敏感词" min-width="180" />
      <el-table-column prop="riskLevel" label="风险等级" width="100">
        <template #default="{ row }">
          <el-tag :type="getRiskTagType(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="action" label="处置动作" width="100">
        <template #default="{ row }">
          <el-tag :type="getActionTagType(row.action)" size="small">{{ row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="140" />
      <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
      <el-table-column prop="enabled" label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled === 1" @change="toggle(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" link type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pager"
      @current-change="load"
      @size-change="load"
    />

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增敏感词' : '编辑敏感词'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="敏感词" required>
          <el-input v-model="editing.word" placeholder="请输入要拦截的词" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="风险等级" required>
          <el-radio-group v-model="editing.riskLevel">
            <el-radio-button v-for="l in RISK_LEVELS" :key="l.value" :value="l.value">{{ l.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处置动作" required>
          <el-select v-model="editing.action" placeholder="选择处置动作" style="width: 100%">
            <el-option v-for="a in ACTIONS" :key="a.value" :label="`${a.label} - ${a.desc}`" :value="a.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editing.category" placeholder="选择分类" clearable style="width: 100%">
            <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="editing.description" type="textarea" :rows="2" placeholder="备注 (可选)" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'edit'" label="启用">
          <el-switch v-model="editing.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="batchDialogVisible" title="批量导入敏感词" width="640px" :close-on-click-modal="false">
      <div class="batch-tips">
        <p>每行一条, 格式: <code>敏感词|风险等级|处置动作|分类|说明</code></p>
        <p>风险等级: HIGH / MEDIUM / LOW; 处置动作: DENY / ALERT / MASK / PASS</p>
        <p>以 <code>#</code> 开头的行为注释, 自动跳过; 重复词自动跳过 (不报错).</p>
        <el-button size="small" :icon="Document" @click="loadPreset">加载示例</el-button>
      </div>
      <el-input
        v-model="batchText"
        type="textarea"
        :rows="10"
        placeholder="一行一条, 格式: 敏感词|风险等级|处置动作|分类|说明"
      />
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatch">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.sensitive-page { padding: 16px 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { font-size: 20px; font-weight: 600; margin: 0 0 4px; color: #1f2937; }
.page-desc { color: #6b7280; font-size: 13px; margin: 0; }
.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; flex-wrap: wrap; }
.search-input { width: 220px; }
.filter-select { width: 130px; }
.toolbar-spacer { flex: 1; }
.word-table { font-size: 13px; }
.pager { margin-top: 16px; justify-content: flex-end; display: flex; }
.batch-tips { margin-bottom: 12px; font-size: 13px; color: #6b7280; line-height: 1.7; }
.batch-tips p { margin: 4px 0; }
.batch-tips code { background: #f3f4f6; padding: 1px 4px; border-radius: 3px; font-size: 12px; color: #b91c1c; }
</style>
