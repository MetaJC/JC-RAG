<script setup lang="ts">
/**
 * 岗位管理 (RBAC 组织维度)。
 * <p>路径: /rbac/position
 * <p>基础 CRUD + 部门关联 + 级别 (level 1=初级 2=中级 3=高级) + 状态切换
 *
 * @author JC
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  createRbacPosition,
  deleteRbacPosition,
  fetchRbacActivePositions,
  fetchRbacDepartmentTree,
  fetchRbacPosition,
  fetchRbacPositions,
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
const departmentFilter = ref<number | undefined>(undefined)
const levelFilter = ref<number | undefined>(undefined)

const departmentOptions = ref<any[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  departmentId: undefined as number | undefined,
  name: '',
  code: '',
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
      departmentId: departmentFilter.value,
      level: levelFilter.value,
    })
    list.value = data.records ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

async function loadDepartments() {
  const tree = (await fetchRbacDepartmentTree()) ?? []
  departmentOptions.value = flatten(tree)
  // 兜底: 树为空时尝试 active 接口
  if (departmentOptions.value.length === 0) {
    const arr = (await fetchRbacActivePositions()) ?? []
    departmentOptions.value = arr.map((p: any) => ({ id: p.departmentId, name: p.departmentName }))
  }
}

function flatten(nodes: any[]): any[] {
  const out: any[] = []
  for (const n of nodes) {
    out.push({ id: n.id, name: n.name })
    if (n.children?.length) out.push(...flatten(n.children))
  }
  return out
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    departmentId: undefined,
    name: '',
    code: '',
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
    departmentId: detail.departmentId,
    name: detail.name,
    code: detail.code,
    level: detail.level ?? 1,
    sort: detail.sort ?? 0,
    status: detail.status ?? 1,
    remark: detail.remark ?? '',
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.departmentId) {
    ElMessage.warning('请选择所属部门')
    return
  }
  if (!form.name?.trim() || !form.code?.trim()) {
    ElMessage.warning('岗位名称和编码不能为空')
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

onMounted(() => {
  loadDepartments()
  loadList()
})
</script>

<template>
  <div class="rbac-pos">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索岗位名称 / 编码"
          :prefix-icon="Search"
          clearable
          style="width: 240px"
          @keyup.enter="loadList"
          @clear="loadList"
        />
        <el-select v-model="departmentFilter" placeholder="部门" clearable filterable style="width: 180px" @change="loadList">
          <el-option
            v-for="opt in departmentOptions"
            :key="opt.id"
            :value="opt.id"
            :label="opt.name"
          />
        </el-select>
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
        <el-table-column prop="code" label="岗位编码" width="140" />
        <el-table-column prop="departmentName" label="所属部门" min-width="140" show-overflow-tooltip />
        <el-table-column label="级别" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ levelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" link :icon="Delete" @click="remove(row)">删除</el-button>
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
        <el-form-item label="所属部门" required>
          <el-select v-model="form.departmentId" placeholder="请选择" filterable style="width: 100%">
            <el-option
              v-for="opt in departmentOptions"
              :key="opt.id"
              :value="opt.id"
              :label="opt.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位名称" required>
          <el-input v-model="form.name" placeholder="如: 高级工程师" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="岗位编码" required>
          <el-input v-model="form.code" placeholder="如: SE" maxlength="50" />
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
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
</style>
