<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '../../api/user'

const apiKeys = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const isEdit = computed(() => editingId.value !== null)
const emptyForm = () => ({ id: null as number | null, provider: 'openai', baseUrl: '', apiKey: '', modelName: '' })
const form = ref(emptyForm())

async function load() {
  try { apiKeys.value = await userApi.listApiKeys() } catch { /* ignore */ }
}
function openAdd() {
  editingId.value = null
  form.value = emptyForm()
  dialogVisible.value = true
}
function openEdit(row: any) {
  editingId.value = row.id
  form.value = { id: row.id, provider: row.provider, baseUrl: row.baseUrl ?? '', apiKey: '', modelName: row.modelName ?? '' }
  dialogVisible.value = true
}
async function save() {
  try {
    await userApi.saveApiKey(form.value)
    ElMessage.success(isEdit.value ? '已更新' : '已保存，请启用后生效')
    dialogVisible.value = false
    await load()
  } catch { ElMessage.error('保存失败') }
}
async function remove(id: number) {
  try { await userApi.deleteApiKey(id); ElMessage.success('已删除'); await load() }
  catch { ElMessage.error('删除失败') }
}
async function toggle(row: any) {
  try {
    await userApi.toggleApiKey(row.id, row.active)
    ElMessage.success(row.active ? '已启用' : '已停用')
    // 启用某条时后端会同时关闭其他 Key, 重拉列表刷新全部开关状态
    await load()
  } catch {
    row.active = !row.active
    ElMessage.error('操作失败')
  }
}
onMounted(load)
</script>
<template>
  <div class="c-shell">
    <header class="c-header">
      <div class="c-header-left"><router-link to="/chat" class="logo">RAG 问答</router-link></div>
      <nav class="c-header-nav">
        <router-link to="/chat">问答</router-link>
        <router-link to="/agents">Agent</router-link>
        <router-link to="/profile">个人中心</router-link>
      </nav>
    </header>
    <main class="c-main">
      <h2>API Key 管理</h2>
      <p style="color:#6b7280;margin-bottom:16px">配置你自己的大模型 API Key，获取更高使用额度；可配置多条，同一时刻仅一条生效</p>
      <el-button type="primary" @click="openAdd" style="margin-bottom:16px">添加 API Key</el-button>
      <el-table :data="apiKeys" border stripe>
        <el-table-column prop="provider" label="提供商" width="120" />
        <el-table-column prop="baseUrl" label="API 地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="modelName" label="模型" width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.active" @change="toggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 API Key' : '添加 API Key'" width="500px">
        <el-form label-position="top">
          <el-form-item label="提供商"><el-select v-model="form.provider"><el-option label="OpenAI" value="openai" /><el-option label="Ollama" value="ollama" /><el-option label="Azure" value="azure" /><el-option label="自定义" value="custom" /></el-select></el-form-item>
          <el-form-item label="API 地址"><el-input v-model="form.baseUrl" placeholder="https://api.openai.com/v1" /></el-form-item>
          <el-form-item label="API Key"><el-input v-model="form.apiKey" type="password" show-password :placeholder="isEdit ? '留空则保持原 Key 不变' : 'sk-...'" /></el-form-item>
          <el-form-item label="模型名称"><el-input v-model="form.modelName" placeholder="gpt-4o-mini" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
      </el-dialog>
    </main>
  </div>
</template>
<style scoped>
.c-shell { display: flex; flex-direction: column; min-height: 100vh; }
.c-header { display: flex; align-items: center; justify-content: space-between; height: 56px; padding: 0 24px; background: #0f2744; color: #fff; }
.c-header-nav { display: flex; gap: 16px; }
.c-header-nav a { color: rgba(255,255,255,.8); text-decoration: none; font-size: 14px; }
.c-main { flex: 1; max-width: 900px; width: 100%; margin: 0 auto; padding: 24px; }
.logo { color: #fff; text-decoration: none; font-weight: 700; }
</style>
