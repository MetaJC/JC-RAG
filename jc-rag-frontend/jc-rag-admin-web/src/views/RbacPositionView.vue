<script setup lang="ts">
/**
 * 岗位管理 (RBAC 组织维度)。
 * <p>路径: /rbac/position
 * <p>基础 CRUD + 级别 (level 1=初级 2=中级 3=高级) + 状态切换 + 分配用户
 * <p>岗位与部门解耦: 不再有所属部门, 用户的部门归属走 rbac_user_department.
 * <p>原 code 字段重命名为 displayName (显示名, 如: 岗位名=人力资源, 显示名=HR)
 *
 * @author JC
 */
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Delete,
  Edit,
  Plus,
  Search,
  User as UserIcon,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  assignRbacPositionUsers,
  createRbacPosition,
  deleteRbacPosition,
  fetchRbacPosition,
  fetchRbacPositionUserIds,
  fetchRbacPositions,
  fetchRbacUsers,
  updateRbacPosition,
  updateRbacPositionStatus,
} from '../api/rbac'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const levelFilter = ref<number | undefined>(undefined)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  name: '',
  displayName: '',
  level: 1,
  sort: 0,
  status: 1,
  remark: '',
})

async function loadList() {
  loading.value = true
  try {
    const data = await fetchRbacPositions({
      page: pageNum.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value,
      level: levelFilter.value,
    })
    list.value = data.records ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    displayName: '',
    level: 1,
    sort: 0,
    status: 1,
    remark: '',
  })
  dialogVisible.value = true
}

async function openEdit(row: any) {
  editingId.value = row.id
  const detail = await fetchRbacPosition(row.id)
  Object.assign(form, {
    name: detail.name,
    displayName: detail.displayName ?? '',
    level: detail.level ?? 1,
    sort: detail.sort ?? 0,
    status: detail.status ?? 1,
    remark: detail.remark ?? '',
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写岗位名称')
    return
  }
  const payload = { ...form }
  if (editingId.value) {
    await updateRbacPosition(editingId.value, payload)
    ElMessage.success('更新成功')
  } else {
    await createRbacPosition(payload)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  loadList()
}

async function toggleStatus(row: any) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(
    `确认${next === 1 ? '启用' : '禁用'}岗位 [${row.name}]?`,
    '提示',
    { type: 'warning' },
  )
  await updateRbacPositionStatus(row.id, next)
  ElMessage.success('状态已更新')
  loadList()
}

async function remove(row: any) {
  await ElMessageBox.confirm(
    `确认删除岗位 [${row.name}]?`,
    '危险操作',
    { type: 'error' },
  )
  await deleteRbacPosition(row.id)
  ElMessage.success('删除成功')
  loadList()
}

const levelText = computed(() => (lv: number) => ({ 1: '初级', 2: '中级', 3: '高级' }[lv] ?? '-'))
const statusText = computed(() => (s: number) => (s === 1 ? '启用' : '禁用'))

/* =====================================================================
 * 分配用户 弹窗
 * <p>左侧: 可选用户 (按用户名/昵称/手机号搜索, 排除系统保留账号)
 * <p>右侧: 已选用户 (可移除)
 * <p>保存: 调用 assignRbacPositionUsers 覆盖式写入
 * ===================================================================== */
const assignDialogVisible = ref(false)
const assignPositionId = ref<number | null>(null)
const assignPositionName = ref('')
const assignKeyword = ref('')
const assignCandidateUsers = ref<any[]>([])
const assignSelectedIds = ref<number[]>([])
const assignLoading = ref(false)
const assignSaving = ref(false)

async function openAssignUsers(row: any) {
  assignPositionId.value = row.id
  assignPositionName.value = row.name
  assignKeyword.value = ''
  assignSelectedIds.value = []
  assignDialogVisible.value = true
  // 拉取已分配 + 候选用户
  await Promise.all([loadAssignAssigned(row.id), loadAssignCandidates()])
}

async function loadAssignAssigned(positionId: number) {
  try {
    const ids = (await fetchRbacPositionUserIds(positionId)) ?? []
    assignSelectedIds.value = Array.isArray(ids) ? ids.slice() : []
  } catch {
    assignSelectedIds.value = []
  }
}

async function loadAssignCandidates() {
  assignLoading.value = true
  try {
    const data = await fetchRbacUsers({
      page: 1,
      size: 1000,
      keyword: assignKeyword.value || undefined,
    })
    assignCandidateUsers.value = (data.records ?? []).filter(
      (u: any) => u.username !== 'developer' && u.username !== 'super_admin',
    )
  } finally {
    assignLoading.value = false
  }
}

function isUserChecked(uid: number): boolean {
  return assignSelectedIds.value.includes(uid)
}

function toggleUser(uid: number, checked: boolean) {
  const set = new Set(assignSelectedIds.value)
  if (checked) set.add(uid)
  else set.delete(uid)
  assignSelectedIds.value = Array.from(set)
}

async function submitAssignUsers() {
  if (assignPositionId.value == null) return
  assignSaving.value = true
  try {
    await assignRbacPositionUsers(assignPositionId.value, assignSelectedIds.value)
    ElMessage.success('分配已更新')
    assignDialogVisible.value = false
  } finally {
    assignSaving.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="rbac-pos">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索岗位名称 / 显示名"
          :prefix-icon="Search"
          clearable
          style="width: 240px"
          @keyup.enter="loadList"
          @clear="loadList"
        />
        <el-select v-model="levelFilter" placeholder="级别" clearable style="width: 120px" @change="loadList">
          <el-option :value="1" label="初级" />
          <el-option :value="2" label="中级" />
          <el-option :value="3" label="高级" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px" @change="loadList">
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="禁用" />
        </el-select>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增岗位</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe row-key="id">
        <el-table-column prop="name" label="岗位名称" min-width="160" />
        <el-table-column prop="displayName" label="显示名" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.displayName">{{ row.displayName }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ levelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">
            <span class="text-muted">{{ row.createdAt || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-tooltip content="编辑" placement="top">
              <el-button size="small" type="primary" link :icon="Edit" @click="openEdit(row)" />
            </el-tooltip>
            <el-tooltip :content="row.status === 1 ? '禁用' : '启用'" placement="top">
              <el-button
                size="small"
                :type="row.status === 1 ? 'warning' : 'success'"
                link
                :icon="row.status === 1 ? 'CircleClose' : 'CircleCheck'"
                @click="toggleStatus(row)"
              />
            </el-tooltip>
            <el-tooltip content="分配用户" placement="top">
              <el-button size="small" type="primary" link :icon="UserIcon" @click="openAssignUsers(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button size="small" type="danger" link :icon="Delete" @click="remove(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 12px; justify-content: flex-end"
        @current-change="loadList"
        @size-change="loadList"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑岗位' : '新增岗位'"
      width="520px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item label="岗位名称" required>
          <el-input v-model="form.name" placeholder="如: 人力资源" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="form.displayName" placeholder="如: HR (简称/英文名, 可选)" maxlength="50" />
        </el-form-item>
        <el-form-item label="级别">
          <el-radio-group v-model="form.level">
            <el-radio :value="1">初级</el-radio>
            <el-radio :value="2">中级</el-radio>
            <el-radio :value="3">高级</el-radio>
          </el-radio-group>
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

    <el-dialog
      v-model="assignDialogVisible"
      :title="`分配用户 - ${assignPositionName}`"
      width="780px"
      destroy-on-close
    >
      <div class="assign-wrap">
        <div class="assign-col">
          <div class="assign-col-title">候选用户（{{ assignCandidateUsers.length }}）</div>
          <el-input
            v-model="assignKeyword"
            placeholder="搜索用户名 / 昵称 / 手机号"
            clearable
            style="margin-bottom: 8px"
            @keyup.enter="loadAssignCandidates"
            @clear="loadAssignCandidates"
          />
          <el-table
            v-loading="assignLoading"
            :data="assignCandidateUsers"
            height="380"
            size="small"
            border
            stripe
            @row-click="(row: any) => toggleUser(row.id, !isUserChecked(row.id))"
          >
            <el-table-column label="选择" width="64" align="center">
              <template #default="{ row }">
                <el-checkbox :model-value="isUserChecked(row.id)" @change="(v: boolean) => toggleUser(row.id, v)" />
              </template>
            </el-table-column>
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
          </el-table>
        </div>
        <div class="assign-col">
          <div class="assign-col-title">已选用户（{{ assignSelectedIds.length }}）</div>
          <el-table :data="assignCandidateUsers.filter((u) => assignSelectedIds.includes(u.id))" height="420" size="small" border>
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-button size="small" type="danger" link :icon="Delete" @click="toggleUser(row.id, false)" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignSaving" @click="submitAssignUsers">保存分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.text-muted {
  color: #909399;
  font-size: 12px;
}
.assign-wrap {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.assign-col-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: #303133;
}
</style>
