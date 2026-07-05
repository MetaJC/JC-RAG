<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Delete, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchRagPrompts, createRagPrompt, updateRagPrompt, deleteRagPrompt } from '../api/rag'

const prompts = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref({
  promptKey: '',
  promptName: '',
  promptContent: '',
  scope: 'global',
  scopeId: null as number | null,
  status: 1,
  description: '',
})

async function load() {
  const rows = await fetchRagPrompts()
  prompts.value = ((rows.records ?? rows) || []).map((item: any) => ({
    id: item.id,
    promptKey: item.prompt_key ?? item.promptKey,
    promptName: item.prompt_name ?? item.promptName,
    promptContent: item.prompt_content ?? item.promptContent,
    scope: item.scope,
    scopeId: item.scope_id ?? item.scopeId,
    status: item.status,
    description: item.description ?? '',
  }))
}

function openCreate() {
  editingId.value = null
  form.value = { promptKey: '', promptName: '', promptContent: '', scope: 'global', scopeId: null, status: 1, description: '' }
  dialogVisible.value = true
}

function openEdit(item: any) {
  editingId.value = item.id
  form.value = { ...item }
  dialogVisible.value = true
}

async function save() {
  try {
    if (editingId.value) {
      await updateRagPrompt(editingId.value, form.value)
    } else {
      await createRagPrompt(form.value)
    }
    dialogVisible.value = false
    await load()
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  }
}

async function removePrompt(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该提示词？', '删除确认', { type: 'warning' })
    await deleteRagPrompt(id)
    await load()
    ElMessage.success('删除成功')
  } catch {}
}

onMounted(load)
</script>

<template>
  <div class="page-wrap">
    <div class="page-head">
      <div>
        <h1>提示词管理</h1>
        <p>统一维护全局、角色级和 Agent 级提示词。</p>
      </div>
      <el-button type="primary" @click="openCreate">新建提示词</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="prompts" stripe>
        <el-table-column prop="promptKey" label="标识键" min-width="140" />
        <el-table-column prop="promptName" label="名称" min-width="180" />
        <el-table-column prop="scope" label="作用域" width="100" />
        <el-table-column prop="promptContent" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="编辑" placement="top">
              <el-button link type="primary" :icon="Edit" @click="openEdit(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button link type="danger" :icon="Delete" @click="removePrompt(row.id)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑提示词' : '新建提示词'" width="720px">
      <el-form label-position="top">
        <el-form-item label="标识键">
          <el-input v-model="form.promptKey" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.promptName" />
        </el-form-item>
        <el-form-item label="作用域">
          <el-select v-model="form.scope" style="width: 100%">
            <el-option label="全局" value="global" />
            <el-option label="角色" value="role" />
            <el-option label="Agent" value="agent" />
          </el-select>
        </el-form-item>
        <el-form-item label="作用域 ID">
          <el-input-number v-model="form.scopeId" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.promptContent" type="textarea" :rows="8" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-wrap { padding: 24px; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-head h1 { margin: 0 0 6px; font-size: 24px; }
.page-head p { margin: 0; color: #6b7280; }
</style>
