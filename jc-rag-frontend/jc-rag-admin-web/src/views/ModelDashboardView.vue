<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useSystemStore } from '../stores/system'
import { fetchModelCatalog, fetchModelProviders, type ModelCatalog, type ModelProviderConfig } from '../api/sys'

const systemStore = useSystemStore()
const config = computed(() => systemStore.runtimeConfig)
/** 已保存的服务商配置 (用于按服务地址反查当前生效对话模型的服务商) */
const providers = ref<ModelProviderConfig[]>([])
/** 模型目录 (用于服务商 id → 展示名称映射) */
const catalog = ref<ModelCatalog>({ providers: [] })
const embeddingDeviceLabel = computed(() => {
  if (!systemStore.devicesLoaded) return '正在获取设备信息...'
  const id = config.value?.rag.embedding.device
  if (!id) return '-'
  return systemStore.devices.find((device) => device.id === id)?.name || '设备信息不可用'
})
function normalizeEndpoint(url?: string): string { return (url || '').trim().replace(/\/+$/, '') }
/** 服务商 id → 展示名称 (目录未命中时回退 id 本身) */
function providerDisplayName(id?: string): string {
  if (!id) return '自定义服务'
  return catalog.value.providers.find(entry => entry.id === id)?.name || id
}
/** 对话模型服务商: 在线按服务地址反查 llm_provider_config; 离线(本地模型服务)按地址反查, 兼容 Ollama/vLLM 等扩展, 未命中回退 Ollama; 仅展示当前生效模式 */
const chatProviderLabel = computed(() => {
  const cfg = config.value
  if (!cfg) return '-'
  if (cfg.model.activeMode === 'online') {
    const baseUrl = normalizeEndpoint(cfg.model.online.baseUrl)
    const record = providers.value.find(item => normalizeEndpoint(item.endpoint) === baseUrl)
    return record ? providerDisplayName(record.providerName) : (cfg.model.online.baseUrl || '在线服务')
  }
  const offlineUrl = normalizeEndpoint(cfg.model.offline.ollama.baseUrl)
  const record = providers.value.find(item =>
    (item.providerType === 'OLLAMA' || item.providerType === 'OFFLINE') && normalizeEndpoint(item.endpoint) === offlineUrl)
  return record ? providerDisplayName(record.providerName) : 'Ollama'
})
/** 对话模型名称 (模型 ID) */
const chatModelNameLabel = computed(() => {
  const cfg = config.value
  if (!cfg) return '-'
  return cfg.model.activeMode === 'online'
    ? (cfg.model.online.modelName || '未选择')
    : (cfg.model.offline.ollama.defaultModel || '未选择')
})
/** 嵌入模型服务商: 在线服务反查目录名称, 本地固定"本地模型" */
const embeddingProviderLabel = computed(() => {
  const embedding = config.value?.rag.embedding
  if (!embedding) return '-'
  if (embedding.source === 'service' && embedding.service?.provider) return providerDisplayName(embedding.service.provider)
  return '本地模型'
})
/** 嵌入模型名称: 在线为模型 ID, 本地为模型目录名 */
const embeddingModelNameLabel = computed(() => {
  const embedding = config.value?.rag.embedding
  if (!embedding) return '-'
  if (embedding.source === 'service' && embedding.service?.provider) return embedding.service.modelName || '未选择'
  const path = embedding.modelPath || ''
  return path.split(/[\\/]/).filter(Boolean).pop() || path || '未配置'
})

interface EffectiveModelRow { type: string; provider: string; model: string }
/** 当前生效模型表格数据: 按模型类型分行, 每类仅一行唯一生效配置; 新增模型类型(如多模态)只需在此追加一行 */
const effectiveModels = computed<EffectiveModelRow[]>(() => [
  { type: '对话模型', provider: chatProviderLabel.value, model: chatModelNameLabel.value },
  { type: '嵌入模型', provider: embeddingProviderLabel.value, model: embeddingModelNameLabel.value },
])

onMounted(async () => {
  await Promise.all([systemStore.loadRuntimeConfig(false), systemStore.refreshDevices()])
  try { providers.value = await fetchModelProviders() } catch { /* 服务商列表加载失败时对话模型仅显示地址与模型名 */ }
  try { catalog.value = await fetchModelCatalog() } catch { /* 目录加载失败时服务商名称回退为 id */ }
})
</script>

<template>
  <div class="model-page" v-loading="systemStore.loading">
    <header class="page-header"><div><h1>模型仪表盘</h1><p>查看当前实际生效的模型、向量化配置与 RAG 运行参数。</p></div></header>
    <section class="summary-grid">
      <el-card shadow="never"><el-statistic title="当前模式" :value="config?.model.activeMode === 'online' ? '在线模型' : '离线模型'" /></el-card>
      <el-card shadow="never"><el-statistic title="主模型" :value="chatModelNameLabel" /></el-card>
      <el-card shadow="never" v-loading="!systemStore.devicesLoaded"><el-statistic title="Embedding 设备" :value="embeddingDeviceLabel" /></el-card>
      <el-card shadow="never"><el-statistic title="向量维度" :value="config?.rag.embedding.dims || 0" /></el-card>
    </section>
    <section class="detail-grid">
      <el-card shadow="never" header="当前生效模型"><el-table :data="effectiveModels" border class="effective-table">
        <el-table-column prop="type" label="模型类型" width="140" />
        <el-table-column prop="provider" label="服务商 / 来源" />
        <el-table-column prop="model" label="模型名称" />
      </el-table><p class="card-note">每类模型仅展示当前唯一生效的配置：在线模型服务、本地模型服务（如 Ollama）或本地下载模型直接调用（当前仅嵌入模型支持）。</p></el-card>
      <el-card shadow="never" header="RAG 运行参数"><el-descriptions :column="1" border>
        <el-descriptions-item label="切块">{{ config?.rag.chunk.size }} / 重叠 {{ config?.rag.chunk.overlap }}</el-descriptions-item>
        <el-descriptions-item label="召回">Top K {{ config?.rag.retrieval.topK }}，Top N {{ config?.rag.retrieval.topN }}</el-descriptions-item>
        <el-descriptions-item label="相似度阈值">{{ config?.rag.retrieval.similarityThreshold }}</el-descriptions-item>
      </el-descriptions></el-card>
    </section>
  </div>
</template>

<style scoped>
.model-page{max-width:1440px;margin:0 auto}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px}.page-header h1{margin:0;font-size:22px}.page-header p{margin:8px 0 0;color:#64748b}.summary-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px;margin-bottom:14px}.detail-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.card-note{margin:10px 0 0;color:#94a3b8;font-size:12px}@media(max-width:900px){.summary-grid,.detail-grid{grid-template-columns:1fr}}
</style>
