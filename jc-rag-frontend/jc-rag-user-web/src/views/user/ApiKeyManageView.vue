<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '../../api/user'

const apiKeys = ref<any[]>([])
const dialogVisible = ref(false)
const form = ref({ provider: 'openai', baseUrl: '', apiKey: '', modelName: '' })

async function load() {
  try { apiKeys.value = await userApi.listApiKeys() } catch { /* ignore */ }
}
async function save() {
  try {
    await userApi.saveApiKey(form.value)
    ElMessage.success('API Key 已保存')
    dialogVisible.value = false
    await load()
  } catch { ElMessage.error('保存失败') }
}
async function remove(id: number) {
  try { await userApi.deleteApiKey(id); ElMessage.success('已删除'); await load() }
  catch { ElMessage.error('删除失败') }
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
      <p style="color:#6b7280;margin-bottom:16px">配置你自己的大模型 API Key，获取更高使用额度</p>
      <el-button type="primary" @click="dialogVisible = true" style="margin-bottom:16px">添加 API Key</el-button>
      <el-table :data="apiKeys" border stripe>
        <el-table-column prop="provider" label="提供商" width="120" />
        <el-table-column prop="baseUrl" label="API 地址" width="200" />
        <el-table-column prop="modelName" label="模型" width="160" />
        <el-table-column prop="isActive" label="状态" width="80">
          <template #default="{ row }">{{ row.isActive === 1 ? '启用' : '禁用' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-dialog v-model="dialogVisible" title="添加 API Key" width="500px">
        <el-form label-position="top">
          <el-form-item label="提供商"><el-select v-model="form.provider"><el-option label="OpenAI" value="openai" /><el-option label="Ollama" value="ollama" /><el-option label="Azure" value="azure" /><el-option label="自定义" value="custom" /></el-select></el-form-item>
          <el-form-item label="API 地址"><el-input v-model="form.baseUrl" placeholder="https://api.openai.com/v1" /></el-form-item>
          <el-form-item label="API Key"><el-input v-model="form.apiKey" type="password" show-password /></el-form-item>
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
