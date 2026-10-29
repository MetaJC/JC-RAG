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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Check, CircleCheck, Download, Loading, QuestionFilled, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useSystemStore } from '../stores/system'
import { downloadLocalModel, fetchLocalModels, fetchModelCatalog, fetchModelProviders, testModelProvider, type CatalogProvider, type LocalModelItem, type ModelProviderConfig } from '../api/sys'

const systemStore = useSystemStore()
const saving = ref(false), verifyingEmbedding = ref(false), embeddingVerified = ref(false)
const providers = ref<ModelProviderConfig[]>([]), localModels = ref<LocalModelItem[]>([]), catalogProviders = ref<CatalogProvider[]>([])
const primaryModel = ref<(string | number)[]>([]), embeddingProvider = ref<(string | number)[]>([])
const localModelId = ref('')
const embeddingSource = ref<'local' | 'service'>('local')
let pollTimer: number | undefined
function isEnabled(item: ModelProviderConfig) { return item.enabled === true || item.enabled === 1 || String(item.enabled) === '1' }
const enabledProviders = computed(() => providers.value.filter(item => isEnabled(item)))
function catalogFor(providerName: string) { return catalogProviders.value.find(entry => entry.id === providerName) }
/** AI 问答主模型候选: 目录中 chat 类型模型; 目录未覆盖时回退已配置的默认模型 (排除向量/重排序模型) */
const modelTree = computed(() => enabledProviders.value.map(item => {
  const entry = catalogFor(item.providerName)
  const chatModels = entry?.models?.filter(model => (model.type || 'chat') === 'chat').map(model => model.name) ?? []
  const fallback = !entry && item.modelName && !/(embed|bge|e5|m3e|gte|text2vec|jina-embeddings|rerank)/i.test(item.modelName) ? [item.modelName] : []
  const names = Array.from(new Set([...chatModels, ...fallback]))
  return names.length ? { value: item.id, label: item.providerName, children: names.map(name => ({ value: name, label: name })) } : null
}).filter((node): node is NonNullable<typeof node> => node !== null))
/** 文本嵌入模型候选: 仅目录中 embedding 类型模型, 不出现其他类型 */
const embeddingTree = computed(() => enabledProviders.value.map(item => {
  const names = catalogFor(item.providerName)?.models?.filter(model => model.type === 'embedding').map(model => model.name) ?? []
  return names.length ? { value: item.id, label: item.providerName, children: names.map(name => ({ value: name, label: name })) } : null
}).filter((node): node is NonNullable<typeof node> => node !== null))
const activeDownload = computed(() => localModels.value.find(item => item.status === 'downloading') ?? localModels.value.find(item => item.status === 'queued'))
const deviceOptionsReady = computed(() => systemStore.devicesLoaded)
function modelVendor(item: LocalModelItem) { return (item.repoId || '').split('/')[0] || '本地模型' }
function deviceSummary(item: { type: string; detail: string }) { return item.detail || (item.type === 'cpu' ? 'CPU 计算' : 'GPU 加速') }
async function loadLocalModels() { localModels.value = await fetchLocalModels(); const active = localModels.value.some(item => item.status === 'queued' || item.status === 'downloading'); if (active && !pollTimer) pollTimer = window.setInterval(loadLocalModels, 1200); if (!active && pollTimer) { clearInterval(pollTimer); pollTimer = undefined } }
async function startDownload(item: LocalModelItem) { if (item.status === 'queued' || item.status === 'downloading') return; await downloadLocalModel(item.id); ElMessage.success(`${item.name} 已加入下载队列`); await loadLocalModels() }
function optionStyle(item: LocalModelItem) { return item.status === 'downloading' ? { background: `linear-gradient(to right,#e5f7eb ${item.progress}%,transparent ${item.progress}%)` } : undefined }
function chooseLocalModel(id: string) { const item = localModels.value.find(model => model.id === id); if (!item?.installed) { localModelId.value = ''; ElMessage.warning('该模型尚未下载，请点击右侧下载按钮'); return }; systemStore.form.embeddingModelPath = item.path; embeddingVerified.value = false }
/** AI 问答主模型选择变化时同步到运行时配置表单 (保存按钮统一落盘) */
function onPrimaryModelChange(value: (string | number)[]) {
  const provider = enabledProviders.value.find(item => item.id === Number(value?.[0]))
  if (!provider || value?.[1] == null) return
  if (provider.providerType === 'OLLAMA' || provider.providerType === 'OFFLINE') {
    systemStore.form.activeMode = 'offline'
    systemStore.form.offlineModel = String(value[1])
    if (provider.endpoint) systemStore.form.ollamaBaseUrl = provider.endpoint
  } else {
    systemStore.form.activeMode = 'online'
    systemStore.form.onlineBaseUrl = provider.endpoint
    systemStore.form.onlineModelName = String(value[1])
  }
}
/** Embedding 服务选择变化时同步到运行时配置表单 (保存按钮统一落盘) */
function onEmbeddingServiceChange(value: (string | number)[]) {
  const provider = enabledProviders.value.find(item => item.id === Number(value?.[0]))
  systemStore.form.embeddingSource = provider && value?.[1] ? 'service' : 'local'
  systemStore.form.embeddingServiceProvider = provider?.providerName ?? ''
  systemStore.form.embeddingServiceModel = value?.[1] != null ? String(value[1]) : ''
  embeddingVerified.value = false
}
function onEmbeddingTabChange(tab: string | number) {
  if (tab === 'local') {
    systemStore.form.embeddingSource = 'local'
    systemStore.form.embeddingServiceProvider = ''
    systemStore.form.embeddingServiceModel = ''
    embeddingVerified.value = false
  } else if (embeddingProvider.value.length === 2) {
    onEmbeddingServiceChange(embeddingProvider.value)
  }
}
function onDeviceChange(id: string) { const device = systemStore.devices.find(item => item.id === id); if (device && !device.available) { systemStore.form.embeddingDevice = 'auto'; ElMessage.warning(`“${device.name}”当前不可用：${device.detail}`) } }
async function verifyEmbedding() { verifyingEmbedding.value = true; embeddingVerified.value = false; try { if (embeddingSource.value === 'local') { await systemStore.verifyModel(systemStore.form.embeddingModelPath); embeddingVerified.value = systemStore.modelVerifyStatus === 'success'; if (!embeddingVerified.value) ElMessage.error(systemStore.modelVerifyMessage || '模型验证失败') } else { const provider = enabledProviders.value.find(item => item.id === Number(embeddingProvider.value[0])); if (!provider) throw new Error('请选择 Embedding 模型服务'); const result = await testModelProvider({ id: provider.id, providerName: provider.providerName, providerType: provider.providerType, endpoint: provider.endpoint }); embeddingVerified.value = result.success; if (!result.success) ElMessage.error(result.message) } } finally { verifyingEmbedding.value = false } }
async function save() { saving.value = true; try { await systemStore.saveSettings(); ElMessage.success('模型设置已保存并生效') } catch (error) { ElMessage.error((error as Error).message || '保存失败') } finally { saving.value = false } }
onMounted(async () => { const devicesPromise = systemStore.refreshDevices(); const [,, rows] = await Promise.all([systemStore.loadRuntimeConfig(false), loadLocalModels(), fetchModelProviders()]); providers.value = rows; try { const catalog = await fetchModelCatalog(); catalogProviders.value = catalog.providers ?? [] } catch { catalogProviders.value = [] }; const installed = localModels.value.find(item => item.installed && item.path === systemStore.form.embeddingModelPath) ?? localModels.value.find(item => item.installed); if (installed) { localModelId.value = installed.id; systemStore.form.embeddingModelPath = installed.path } else { localModelId.value = ''; systemStore.form.embeddingModelPath = '' }; await devicesPromise; const current = systemStore.runtimeConfig?.model.activeMode === 'online' ? systemStore.runtimeConfig?.model.online.modelName : systemStore.runtimeConfig?.model.offline.ollama.defaultModel; const active = enabledProviders.value.find(item => item.modelName === current); if (active) primaryModel.value = [active.id, active.modelName]; const serviceConfig = systemStore.runtimeConfig?.rag.embedding; if (serviceConfig?.source === 'service' && serviceConfig.service?.provider && serviceConfig.service?.modelName) { embeddingSource.value = 'service'; const serviceProvider = enabledProviders.value.find(item => item.providerName === serviceConfig.service.provider); if (serviceProvider) embeddingProvider.value = [serviceProvider.id, serviceConfig.service.modelName] } })
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<template>
  <div class="model-settings" v-loading="systemStore.loading">
    <header class="page-header"><div><h1>模型设置</h1><p>只显示已启用且符合当前用途的模型。</p></div><el-button type="primary" :loading="saving" @click="save">保存</el-button></header>
    <el-card shadow="never" header="文本模型"><el-form label-position="top"><el-form-item><template #label>AI 问答主模型</template><el-cascader v-model="primaryModel" :options="modelTree" :show-all-levels="true" placeholder="选择已启用的文本模型" class="wide-control" @change="onPrimaryModelChange" /></el-form-item></el-form></el-card>
    <el-card shadow="never" header="文本嵌入模型" class="mt"><el-tabs v-model="embeddingSource" @tab-change="onEmbeddingTabChange"><el-tab-pane label="本地模型（默认）" name="local"><el-form label-position="top">
      <el-form-item><template #label><span>本地 Embedding 模型 <el-tooltip content="模型下载到当前系统用户目录 .jc-rag/models/embedding/"><el-icon class="help"><QuestionFilled /></el-icon></el-tooltip></span></template>
        <div class="model-input-row"><el-select v-model="localModelId" class="download-select" filterable placeholder="查找并选择已下载的模型" @change="chooseLocalModel"><el-option v-for="item in localModels" :key="item.id" :label="item.name" :value="item.id"><div class="download-option" :style="optionStyle(item)"><div class="model-option-copy"><strong>{{ item.name }}</strong><small>{{ modelVendor(item) }} · {{ item.repoId }}</small></div><span v-if="item.installed" class="installed" title="已安装"><el-icon><CircleCheck /></el-icon></span><el-button v-else class="download-action" circle plain :icon="Download" :loading="item.status === 'downloading'" :title="item.status === 'queued' ? '等待下载' : item.status === 'downloading' ? `下载中 ${item.progress}%` : '下载模型'" @mousedown.prevent.stop @click.stop="startDownload(item)" /></div></el-option></el-select><el-button class="verify-button" :type="embeddingVerified ? 'success' : 'default'" :icon="embeddingVerified ? CircleCheck : Check" :loading="verifyingEmbedding" @click="verifyEmbedding">{{ embeddingVerified ? '验证通过' : '验证模型' }}</el-button></div>
        <div v-if="activeDownload" class="download-status">{{ activeDownload.name }}：{{ activeDownload.message }} {{ activeDownload.progress }}%</div>
      </el-form-item>
      <el-form-item><template #label><span>运行设备 <el-tooltip content="自动选择当前可用的最佳设备：外接/独立 GPU、集成 GPU、CPU。不可用设备保留显示并说明原因。"><el-icon class="help"><QuestionFilled /></el-icon></el-tooltip></span></template>
        <div class="model-input-row"><el-select v-if="deviceOptionsReady" v-model="systemStore.form.embeddingDevice" class="download-select" :loading="systemStore.devicesLoading" @change="onDeviceChange"><el-option v-for="item in systemStore.devices" :key="item.id" :label="item.name" :value="item.id" :disabled="!item.available"><div class="device-option"><div><strong>{{ item.name }}</strong><small>{{ deviceSummary(item) }}</small></div><el-tag size="small" :type="item.available ? 'success' : 'info'">{{ item.available ? '可用' : '不可用' }}</el-tag></div></el-option></el-select><div v-else class="device-loading"><el-icon class="is-loading"><Loading /></el-icon><span>正在获取设备信息...</span></div><el-button text :icon="RefreshRight" :loading="systemStore.devicesLoading" @click="systemStore.refreshDevices">刷新设备</el-button></div>
      </el-form-item>
    </el-form></el-tab-pane><el-tab-pane label="使用模型服务" name="service"><div class="model-input-row"><el-cascader v-model="embeddingProvider" :options="embeddingTree" :show-all-levels="true" placeholder="仅显示已启用服务商的文本嵌入模型" class="download-select" @change="onEmbeddingServiceChange" /><el-button class="verify-button" :type="embeddingVerified ? 'success' : 'default'" :icon="embeddingVerified ? CircleCheck : Check" :loading="verifyingEmbedding" @click="verifyEmbedding">{{ embeddingVerified ? '验证通过' : '验证模型' }}</el-button></div></el-tab-pane></el-tabs></el-card>
  </div>
</template>

<style scoped>
.model-settings{max-width:1120px;margin:0 auto}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px}.page-header h1{margin:0;font-size:22px}.page-header p{margin:8px 0 0;color:#64748b}.mt{margin-top:14px}.help{color:#909399;cursor:help;vertical-align:-2px}.wide-control{width:100%;max-width:760px}.model-input-row{display:grid;grid-template-columns:minmax(520px,760px) auto;align-items:center;gap:10px;width:100%}.download-select{width:100%!important;min-width:520px}.verify-button{height:32px;white-space:nowrap}.download-option,.device-option{display:flex;align-items:center;justify-content:space-between;gap:16px;width:100%;min-height:64px;padding:10px 14px;box-sizing:border-box}.model-option-copy,.device-option>div{display:flex;flex-direction:column;justify-content:center;gap:5px;min-width:0}.download-option strong,.download-option small,.device-option strong,.device-option small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.download-option small,.device-option small{color:#909399;line-height:18px}.installed{display:flex;align-items:center;justify-content:center;width:32px;height:32px;color:#16a34a}.installed :deep(svg){width:20px;height:20px}.download-action{width:32px;height:32px;border-color:#d6e4ff;color:#2563eb}.download-status{margin-top:7px;color:#16a34a;font-size:13px}@media(max-width:800px){.model-input-row{grid-template-columns:1fr}.download-select{min-width:0}}
.device-loading{height:32px;min-width:520px;display:flex;align-items:center;gap:8px;padding:0 12px;color:#909399;border:1px solid #dcdfe6;border-radius:4px;box-sizing:border-box}
</style>
