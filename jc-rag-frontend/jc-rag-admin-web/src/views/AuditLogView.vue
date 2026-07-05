<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { fetchExceptionLogs, fetchOperationLogs, fetchRagCallLogs, fetchSecurityAuditLogs, fetchSecurityAuditSummary } from '../api/audit'

const activeTab = ref('operation')

interface LogItem {
  id: number
  [key: string]: any
}

const operationLogs = ref<LogItem[]>([])
const ragCallLogs = ref<LogItem[]>([])
const exceptionLogs = ref<LogItem[]>([])
const securityAuditLogs = ref<LogItem[]>([])

const operationTotal = ref(0)
const ragCallTotal = ref(0)
const exceptionTotal = ref(0)
const securityAuditTotal = ref(0)
const securityAuditSummary = ref<{ deny24h?: number; mask24h?: number }>({})

const loading = reactive({
  operation: false,
  ragCall: false,
  exception: false,
  securityAudit: false,
})

const pagination = reactive({
  operation: { page: 1, size: 10 },
  ragCall: { page: 1, size: 10 },
  exception: { page: 1, size: 10 },
  securityAudit: { page: 1, size: 10 },
})

const searchKeyword = reactive({
  operation: '',
  ragCall: '',
  exception: '',
  securityAudit: '',
})

const securityAuditFilter = reactive({ auditType: '', riskLevel: '' })

async function loadOperationLogs() {
  loading.operation = true
  try {
    const data = await fetchOperationLogs({
      page: pagination.operation.page,
      size: pagination.operation.size,
      keyword: searchKeyword.operation || undefined,
    })
    operationLogs.value = data.records ?? []
    operationTotal.value = data.total ?? 0
  } finally {
    loading.operation = false
  }
}

async function loadRagCallLogs() {
  loading.ragCall = true
  try {
    const data = await fetchRagCallLogs({
      page: pagination.ragCall.page,
      size: pagination.ragCall.size,
      keyword: searchKeyword.ragCall || undefined,
    })
    ragCallLogs.value = data.records ?? []
    ragCallTotal.value = data.total ?? 0
  } finally {
    loading.ragCall = false
  }
}

async function loadExceptionLogs() {
  loading.exception = true
  try {
    const data = await fetchExceptionLogs({
      page: pagination.exception.page,
      size: pagination.exception.size,
      keyword: searchKeyword.exception || undefined,
    })
    exceptionLogs.value = data.records ?? []
    exceptionTotal.value = data.total ?? 0
  } finally {
    loading.exception = false
  }
}

async function loadSecurityAuditLogs() {
  loading.securityAudit = true
  try {
    const data = await fetchSecurityAuditLogs({
      page: pagination.securityAudit.page,
      size: pagination.securityAudit.size,
      keyword: searchKeyword.securityAudit || undefined,
      auditType: securityAuditFilter.auditType || undefined,
      riskLevel: securityAuditFilter.riskLevel || undefined,
    })
    securityAuditLogs.value = data?.records ?? data ?? []
    securityAuditTotal.value = data?.total ?? securityAuditLogs.value.length
  } catch {
    securityAuditLogs.value = []
    securityAuditTotal.value = 0
  } finally {
    loading.securityAudit = false
  }
  try {
    securityAuditSummary.value = await fetchSecurityAuditSummary()
  } catch {
    securityAuditSummary.value = {}
  }
}

function onTabChange(tabName: string) {
  if (tabName === 'operation') loadOperationLogs()
  else if (tabName === 'ragCall') loadRagCallLogs()
  else if (tabName === 'exception') loadExceptionLogs()
  else if (tabName === 'securityAudit') loadSecurityAuditLogs()
}

function onOperationSizeChange(size: number) {
  pagination.operation.size = size
  pagination.operation.page = 1
  loadOperationLogs()
}

function onRagCallSizeChange(size: number) {
  pagination.ragCall.size = size
  pagination.ragCall.page = 1
  loadRagCallLogs()
}

function onExceptionSizeChange(size: number) {
  pagination.exception.size = size
  pagination.exception.page = 1
  loadExceptionLogs()
}

function onSecurityAuditSizeChange(size: number) {
  pagination.securityAudit.size = size
  pagination.securityAudit.page = 1
  loadSecurityAuditLogs()
}

function getAuditTypeTagType(t: string): string {
  if (t === 'INPUT_DENY' || t === 'OUTPUT_DENY') return 'danger'
  if (t === 'INPUT_MASK' || t === 'OUTPUT_MASK') return 'warning'
  if (t === 'INPUT_ALERT') return 'primary'
  return 'info'
}

function getRiskTagType(level: string): string {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

onMounted(() => {
  loadOperationLogs()
})
</script>

<template>
  <div>
    <header class="page-header">
      <div>
        <h1>日志审计</h1>
        <p>查看系统操作日志、RAG 调用日志和异常日志。</p>
      </div>
    </header>

    <el-card class="panel-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="操作日志" name="operation">
          <div class="tab-toolbar">
            <el-input
              v-model="searchKeyword.operation"
              placeholder="搜索模块/操作类型/操作说明"
              clearable
              style="width: 300px"
              @clear="loadOperationLogs"
              @keyup.enter="loadOperationLogs"
            >
              <template #append>
                <el-button @click="loadOperationLogs">搜索</el-button>
              </template>
            </el-input>
          </div>
          <el-table :data="operationLogs" stripe v-loading="loading.operation">
            <el-table-column prop="moduleName" label="模块" min-width="120" />
            <el-table-column prop="operationType" label="操作类型" min-width="140" />
            <el-table-column prop="operationDesc" label="操作说明" min-width="240" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" min-width="100" />
            <el-table-column prop="createdAt" label="时间" min-width="180" />
          </el-table>
          <el-pagination
            v-model:current-page="pagination.operation.page"
            v-model:page-size="pagination.operation.size"
            :total="operationTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadOperationLogs"
            @size-change="onOperationSizeChange"
            style="margin-top: 16px; justify-content: flex-end"
          />
        </el-tab-pane>

        <el-tab-pane label="RAG 调用日志" name="ragCall">
          <div class="tab-toolbar">
            <el-input
              v-model="searchKeyword.ragCall"
              placeholder="搜索问题/模型名称"
              clearable
              style="width: 300px"
              @clear="loadRagCallLogs"
              @keyup.enter="loadRagCallLogs"
            >
              <template #append>
                <el-button @click="loadRagCallLogs">搜索</el-button>
              </template>
            </el-input>
          </div>
          <el-table :data="ragCallLogs" stripe v-loading="loading.ragCall">
            <el-table-column prop="userName" label="用户" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.userName || row.userNickname || (row.userId ? `#${row.userId}` : '-') }}
              </template>
            </el-table-column>
            <el-table-column prop="sessionName" label="会话名称" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.sessionName || (row.sessionId ? `#${row.sessionId}` : '-') }}
              </template>
            </el-table-column>
            <el-table-column prop="queryText" label="问题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="modelMode" label="模型模式" min-width="100" />
            <el-table-column prop="modelName" label="模型名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="retrievedChunks" label="召回片段" min-width="90" />
            <el-table-column prop="latencySec" label="耗时(秒)" min-width="100" />
            <el-table-column label="成功" min-width="80">
              <template #default="{ row }">
                <el-tag :type="row.successFlag === 1 ? 'success' : 'danger'" size="small">
                  {{ row.successFlag === 1 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" min-width="180" />
          </el-table>
          <el-pagination
            v-model:current-page="pagination.ragCall.page"
            v-model:page-size="pagination.ragCall.size"
            :total="ragCallTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadRagCallLogs"
            @size-change="onRagCallSizeChange"
            style="margin-top: 16px; justify-content: flex-end"
          />
        </el-tab-pane>

        <el-tab-pane label="异常日志" name="exception">
          <div class="tab-toolbar">
            <el-input
              v-model="searchKeyword.exception"
              placeholder="搜索模块/异常类型/异常信息"
              clearable
              style="width: 300px"
              @clear="loadExceptionLogs"
              @keyup.enter="loadExceptionLogs"
            >
              <template #append>
                <el-button @click="loadExceptionLogs">搜索</el-button>
              </template>
            </el-input>
          </div>
          <el-table :data="exceptionLogs" stripe v-loading="loading.exception">
            <el-table-column prop="moduleName" label="模块" min-width="120" />
            <el-table-column prop="exceptionType" label="异常类型" min-width="180" show-overflow-tooltip />
            <el-table-column prop="exceptionMessage" label="异常信息" min-width="280" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" min-width="180" />
          </el-table>
          <el-pagination
            v-model:current-page="pagination.exception.page"
            v-model:page-size="pagination.exception.size"
            :total="exceptionTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadExceptionLogs"
            @size-change="onExceptionSizeChange"
            style="margin-top: 16px; justify-content: flex-end"
          />
        </el-tab-pane>

        <el-tab-pane label="安全审计" name="securityAudit">
          <div class="audit-summary" v-if="securityAuditSummary.deny24h !== undefined">
            <el-tag type="danger" size="large">近 24h 拒绝 {{ securityAuditSummary.deny24h ?? 0 }} 次</el-tag>
            <el-tag type="warning" size="large" style="margin-left: 8px">近 24h 脱敏 {{ securityAuditSummary.mask24h ?? 0 }} 次</el-tag>
          </div>
          <div class="tab-toolbar" style="margin-top: 12px">
            <el-input
              v-model="searchKeyword.securityAudit"
              placeholder="搜索关键词/敏感词/脱敏内容"
              clearable
              style="width: 280px"
              @clear="() => { pagination.securityAudit.page = 1; loadSecurityAuditLogs() }"
              @keyup.enter="() => { pagination.securityAudit.page = 1; loadSecurityAuditLogs() }"
            >
              <template #append>
                <el-button @click="() => { pagination.securityAudit.page = 1; loadSecurityAuditLogs() }">搜索</el-button>
              </template>
            </el-input>
            <el-select v-model="securityAuditFilter.auditType" placeholder="审计类型" clearable style="width: 160px; margin-left: 8px" @change="() => { pagination.securityAudit.page = 1; loadSecurityAuditLogs() }">
              <el-option label="输入拒绝" value="INPUT_DENY" />
              <el-option label="输入告警" value="INPUT_ALERT" />
              <el-option label="输入脱敏" value="INPUT_MASK" />
              <el-option label="输出拒绝" value="OUTPUT_DENY" />
              <el-option label="输出脱敏" value="OUTPUT_MASK" />
            </el-select>
            <el-select v-model="securityAuditFilter.riskLevel" placeholder="风险等级" clearable style="width: 130px; margin-left: 8px" @change="() => { pagination.securityAudit.page = 1; loadSecurityAuditLogs() }">
              <el-option label="高" value="HIGH" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="低" value="LOW" />
            </el-select>
          </div>
          <el-table :data="securityAuditLogs" stripe v-loading="loading.securityAudit" border>
            <el-table-column prop="id" label="#" width="70" />
            <el-table-column prop="userId" label="用户ID" width="80" />
            <el-table-column prop="auditType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getAuditTypeTagType(row.auditType)" size="small">{{ row.auditType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="riskLevel" label="风险" width="80">
              <template #default="{ row }">
                <el-tag :type="getRiskTagType(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="matchedWord" label="命中词" min-width="140" show-overflow-tooltip />
            <el-table-column prop="action" label="处置" width="80" />
            <el-table-column prop="maskedContent" label="脱敏内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="clientIp" label="客户端" min-width="120" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" min-width="170" />
          </el-table>
          <el-pagination
            v-model:current-page="pagination.securityAudit.page"
            v-model:page-size="pagination.securityAudit.size"
            :total="securityAuditTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadSecurityAuditLogs"
            @size-change="onSecurityAuditSizeChange"
            style="margin-top: 16px; justify-content: flex-end"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
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

.panel-card {
  border-radius: 16px;
}

.tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
</style>
