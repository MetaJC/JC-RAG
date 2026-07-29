<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useSystemStore } from '../stores/system'

const systemStore = useSystemStore()
const config = computed(() => systemStore.runtimeConfig)
const embeddingDeviceLabel = computed(() => {
  if (!systemStore.devicesLoaded) return '正在获取设备信息...'
  const id = config.value?.rag.embedding.device
  if (!id) return '-'
  return systemStore.devices.find((device) => device.id === id)?.name || '设备信息不可用'
})
const activeModel = computed(() => config.value?.model.activeMode === 'online'
  ? config.value.model.online.modelName || '未选择'
  : config.value?.model.offline.ollama.defaultModel || '未选择')

onMounted(async () => {
  await Promise.all([systemStore.loadRuntimeConfig(false), systemStore.refreshDevices()])
})
</script>

<template>
  <div class="model-page" v-loading="systemStore.loading">
    <header class="page-header"><div><h1>模型仪表盘</h1><p>查看当前实际生效的模型、向量化配置与 RAG 运行参数。</p></div></header>
    <section class="summary-grid">
      <el-card shadow="never"><el-statistic title="当前模式" :value="config?.model.activeMode === 'online' ? '在线模型' : '离线模型'" /></el-card>
      <el-card shadow="never"><el-statistic title="主模型" :value="activeModel" /></el-card>
      <el-card shadow="never" v-loading="!systemStore.devicesLoaded"><el-statistic title="Embedding 设备" :value="embeddingDeviceLabel" /></el-card>
      <el-card shadow="never"><el-statistic title="向量维度" :value="config?.rag.embedding.dims || 0" /></el-card>
    </section>
    <section class="detail-grid">
      <el-card shadow="never" header="当前生效模型"><el-descriptions :column="1" border>
        <el-descriptions-item label="在线服务地址">{{ config?.model.online.baseUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="离线 Ollama">{{ config?.model.offline.ollama.baseUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Embedding 模型">{{ config?.rag.embedding.modelPath || '-' }}</el-descriptions-item>
      </el-descriptions></el-card>
      <el-card shadow="never" header="RAG 运行参数"><el-descriptions :column="1" border>
        <el-descriptions-item label="切块">{{ config?.rag.chunk.size }} / 重叠 {{ config?.rag.chunk.overlap }}</el-descriptions-item>
        <el-descriptions-item label="召回">Top K {{ config?.rag.retrieval.topK }}，Top N {{ config?.rag.retrieval.topN }}</el-descriptions-item>
        <el-descriptions-item label="相似度阈值">{{ config?.rag.retrieval.similarityThreshold }}</el-descriptions-item>
      </el-descriptions></el-card>
    </section>
  </div>
</template>

<style scoped>
.model-page{max-width:1440px;margin:0 auto}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px}.page-header h1{margin:0;font-size:22px}.page-header p{margin:8px 0 0;color:#64748b}.summary-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px;margin-bottom:14px}.detail-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}@media(max-width:900px){.summary-grid,.detail-grid{grid-template-columns:1fr}}
</style>
