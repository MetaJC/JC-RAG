<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Connection, Cpu, DataAnalysis, Download, Edit, MagicStick, Microphone, RefreshRight, Sort, Tools, TopRight, VideoCamera, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useSystemStore } from '../stores/system'
import { fetchModelCatalog, fetchModelProviders, refreshModelCatalog, refreshProviderModels, saveModelProvider, testModelProvider, type CatalogModelItem, type ModelCatalog, type ModelProviderConfig } from '../api/sys'
import { modelProviderExtensions, type ModelProviderExtension } from '../config/modelProviderExtensions'
import './ModelProviderConfigView.css'

type Capability = '工具' | '视觉' | '联网' | '推理' | '向量' | '重排序' | '生图' | '生视频' | '多模态' | '语音' | '编程' | '结构化'
const systemStore = useSystemStore()
const saving = ref(false), testing = ref(false), refreshing = ref(false), updatingCatalog = ref(false), keyword = ref('')
const providerMode = ref<'online' | 'offline'>('online')
/** 能力筛选标签 (多选, 空集合表示全部) */
const selectedCapabilities = ref<Set<Capability>>(new Set())
const enabled = reactive<Record<string, boolean>>({})
const savedProviders = ref<ModelProviderConfig[]>([])
const logoVisible = reactive<Record<string, boolean>>({})
/** 模型目录 (L1/L2), 服务商模型列表的唯一数据源 */
const catalog = ref<ModelCatalog>({ providers: [] })
/** L3 账户级实时刷新结果 (按服务商 id 覆盖目录) */
const liveModels = reactive<Record<string, CatalogModelItem[]>>({})
type Provider = ModelProviderExtension & { logoSource?: 'official' | 'community' | 'wikimedia' }
/** 保留 lobe-icons（社区近似）的服务商，前端展示时给悬停提示 */
const COMMUNITY_LOGO_IDS = new Set(['deepseek', 'gemini', 'zhipu', 'bailian'])
const providers: Provider[] = [
  { id:'deepseek',name:'DeepSeek',logo:'D',logoUrl:'/provider-logos/deepseek-color.svg',logoSource:'community',type:'online',baseUrl:'https://api.deepseek.com',websiteUrl:'https://platform.deepseek.com/',apiKeyUrl:'https://platform.deepseek.com/api_keys',models:['deepseek-v4-pro','deepseek-v4-flash','deepseek-v4-vision','deepseek-ocr'] },
  { id:'qwen',name:'阿里云百炼 / 通义千问',logo:'Q',logoUrl:'/provider-logos/bailian-color.svg',logoSource:'community',type:'online',baseUrl:'https://dashscope.aliyuncs.com/compatible-mode/v1',websiteUrl:'https://bailian.console.aliyun.com/',apiKeyUrl:'https://bailian.console.aliyun.com/?apiKey=1',models:['qwen-max','qwen-plus','qwen-turbo','qwen-vl-max','qwen3-235b-a22b'] },
  { id:'openai',name:'OpenAI',logo:'O',logoUrl:'/provider-logos/openai-color.svg',logoSource:'official',type:'online',baseUrl:'https://api.openai.com/v1',websiteUrl:'https://platform.openai.com/',apiKeyUrl:'https://platform.openai.com/api-keys',models:['gpt-4.1','gpt-4o','gpt-4o-mini','o3','o4-mini'] },
  { id:'anthropic',name:'Anthropic',logo:'A',logoUrl:'/provider-logos/anthropic-color.svg',logoSource:'official',type:'online',baseUrl:'https://api.anthropic.com/v1',websiteUrl:'https://console.anthropic.com/',apiKeyUrl:'https://console.anthropic.com/settings/keys',models:['claude-opus-4-1-20250805','claude-sonnet-4-20250514','claude-3-7-sonnet-20250219'] },
  { id:'gemini',name:'Google Gemini',logo:'G',logoUrl:'/provider-logos/gemini-color.svg',logoSource:'community',type:'online',baseUrl:'https://generativelanguage.googleapis.com/v1beta',websiteUrl:'https://aistudio.google.com/',apiKeyUrl:'https://aistudio.google.com/app/apikey',models:['gemini-2.5-pro','gemini-2.5-flash','gemini-2.0-flash'] },
  { id:'zhipu',name:'智谱 AI',logo:'Z',logoUrl:'/provider-logos/zhipu-color.svg',logoSource:'community',type:'online',baseUrl:'https://open.bigmodel.cn/api/paas/v4',websiteUrl:'https://open.bigmodel.cn/',apiKeyUrl:'https://open.bigmodel.cn/usercenter/apikeys',models:['glm-4.5','glm-4-plus','glm-4-air','glm-4v-plus'] },
  { id:'moonshot',name:'Moonshot AI / Kimi',logo:'K',logoUrl:'/provider-logos/moonshot-color.svg',logoSource:'official',type:'online',baseUrl:'https://api.moonshot.cn/v1',websiteUrl:'https://platform.moonshot.cn/',apiKeyUrl:'https://platform.moonshot.cn/console/api-keys',models:['kimi-k2','moonshot-v1-128k','moonshot-v1-32k','moonshot-v1-8k'] },
  { id:'siliconflow',name:'硅基流动',logo:'S',logoUrl:'/provider-logos/siliconflow-color.svg',logoSource:'community',type:'online',baseUrl:'https://api.siliconflow.cn/v1',websiteUrl:'https://siliconflow.cn/',apiKeyUrl:'https://cloud.siliconflow.cn/account/ak',models:['deepseek-ai/DeepSeek-V3','deepseek-ai/DeepSeek-R1','Qwen/Qwen2.5-72B-Instruct','Qwen/Qwen2.5-VL-72B-Instruct'] },
  { id:'groq',name:'Groq',logo:'Gr',logoUrl:'/provider-logos/groq-color.svg',logoSource:'wikimedia',type:'online',baseUrl:'https://api.groq.com/openai/v1',websiteUrl:'https://console.groq.com/',apiKeyUrl:'https://console.groq.com/keys',models:['llama-3.3-70b-versatile','qwen-qwq-32b','llama-3.1-8b-instant'] },
  { id:'mistral',name:'Mistral AI',logo:'Mi',logoUrl:'/provider-logos/mistral-color.svg',logoSource:'official',type:'online',baseUrl:'https://api.mistral.ai/v1',websiteUrl:'https://console.mistral.ai/',apiKeyUrl:'https://console.mistral.ai/api-keys/',models:['mistral-large-latest','mistral-small-latest','codestral-latest','pixtral-large-latest'] },
  { id:'cohere',name:'Cohere',logo:'C',logoUrl:'/provider-logos/cohere-color.svg',logoSource:'community',type:'online',baseUrl:'https://api.cohere.com/compatibility/v1',websiteUrl:'https://dashboard.cohere.com/',apiKeyUrl:'https://dashboard.cohere.com/api-keys',models:['command-a-03-2025','command-r-plus-08-2024','command-r-08-2024','embed-multilingual-v3.0'] },
  { id:'custom',name:'自定义 OpenAI 兼容服务',logo:'+',type:'online',baseUrl:'',websiteUrl:'https://platform.openai.com/docs/api-reference',models:[] },
  { id:'ollama',name:'Ollama',logo:'Ol',logoUrl:'/provider-logos/ollama-color.svg',logoSource:'official',type:'offline',baseUrl:'http://localhost:11434',websiteUrl:'https://ollama.com/',models:[] },
  ...modelProviderExtensions,
]
const defaultProvider = providers[0]!
const selectedId = ref(defaultProvider.id)
const selected = computed(() => providers.find(item => item.id === selectedId.value) ?? defaultProvider)
const filtered = computed(() => providers.filter(item => item.type === providerMode.value && item.name.toLowerCase().includes(keyword.value.toLowerCase())))
/** 按名称推断模型类型 (用于 L3 实时刷新与兼容旧内置列表) */
function guessType(name: string): string { const v = name.toLowerCase(); if (/(embed|bge|e5|m3e|gte|text2vec)/.test(v)) return 'embedding'; if (/rerank/.test(v)) return 'rerank'; if (/(dall-e|flux|stable-diffusion|imagen|cogview|image-gen)/.test(v)) return 'image'; if (/(video|wan2|cogvideo|sora)/.test(v)) return 'video'; if (/(whisper|tts|speech|audio)/.test(v)) return 'audio'; return 'chat' }
/** 服务商当前生效的模型列表: L3 实时结果 > L1/L2 目录 > 内置兑底 */
/** Ollama 原始 capabilities (vision/completion/tools/thinking) → 目录模型的 inputs/features 结构, 供 capabilitiesFor 展示能力标签 */
function ollamaCapsToItem(name: string): CatalogModelItem {
  const caps = systemStore.offlineModelDetails[name] ?? []
  const inputs: string[] = []
  const features: string[] = []
  if (caps.includes('vision')) inputs.push('image')
  if (caps.includes('thinking')) features.push('thinking')
  if (caps.includes('tools')) features.push('tool-call')
  return { name, type: guessType(name), inputs, features }
}
/** 服务商当前生效的模型列表: L3 实时结果 > L1/L2 目录 > 内置兑底 */
function modelsFor(providerId: string): CatalogModelItem[] {
  // ollama: 使用全局列表 (含 /api/tags 返回的真实 capabilities); liveModels 只有名称没有能力, 不参与 ollama
  if (providerId === 'ollama') {
    return systemStore.offlineModelOptions.map(ollamaCapsToItem)
  }
  if (liveModels[providerId]?.length) return liveModels[providerId]
  const entry = catalog.value.providers.find(item => item.id === providerId)
  if (entry?.models?.length) return entry.models
  const legacy: string[] = providers.find(item => item.id === providerId)?.models ?? []
  return legacy.map((name: string) => ({ name, type: guessType(name) }))
}
const allModels = computed(() => modelsFor(selected.value.id))
const visibleModels = computed(() => {
  if (!selectedCapabilities.value.size) return allModels.value
  return allModels.value.filter(model => capabilitiesFor(model).some(cap => selectedCapabilities.value.has(cap)))
})
const form = reactive({ baseUrl:'',apiKey:'',model:'',enabled:false })
/** API Key 编辑态: true 时输入框绑定用户输入的明文 (form.apiKey); false 时仅展示脱敏串 (不参与保存) */
const apiKeyEditing = ref(false)
const apiKeyMask = ref('')
/** 客户端脱敏 (仅作为后端未返回 apiKeyMasked 时的兜底, 规则与后端一致) */
function maskApiKey(key: string): string { if (!key) return ''; if (key.length <= 8) return `${key.charAt(0)}****${key.charAt(key.length - 1)}`; return `${key.slice(0, 4)}****${key.slice(-4)}` }
function onApiKeyFocus() { if (!apiKeyEditing.value) { apiKeyEditing.value = true; form.apiKey = '' } }
function onApiKeyInput(value: string | number) { apiKeyEditing.value = true; form.apiKey = String(value) }
function isEnabled(value: unknown) { return value === true || value === 1 || value === '1' }
function saved(provider: Provider) { return savedProviders.value.find(item => item.providerName === provider.id) }
/** 目录功能特性编码 → 展示能力 */
const FEATURE_CODES: Array<[string, Capability]> = [['tool-call','工具'],['web-search','联网'],['thinking','推理'],['coding','编程'],['structured-output','结构化']]
const MODALITY_TEXT: Record<string, string> = { text:'文本', image:'图像', video:'视频', audio:'语音' }
function hasCatalogCapabilities(model: CatalogModelItem): boolean { return !!(model.inputs?.length || model.features?.length) }
/** 优先使用目录维护的能力数据, 目录缺失时才按名称启发式兜底 (仅限 L3 实时刷新与旧内置列表) */
function capabilitiesFor(model: CatalogModelItem): Capability[] {
  const byType: Record<string, Capability[]> = { embedding: ['向量'], rerank: ['重排序'], image: ['生图'], video: ['生视频'], audio: ['语音'] }
  const typed = byType[model.type]
  if (typed) return typed
  if (hasCatalogCapabilities(model)) {
    const tags: Capability[] = []
    const inputs = model.inputs ?? []
    const outputs = model.outputs ?? []
    if (outputs.includes('image')) tags.push('生图')
    if (outputs.includes('video')) tags.push('生视频')
    if (outputs.includes('audio')) tags.push('语音')
    if (inputs.includes('image') || inputs.includes('video')) { tags.push('视觉'); tags.push('多模态') }
    if (inputs.includes('audio')) tags.push('语音')
    for (const feature of model.features ?? []) { const match = FEATURE_CODES.find(([code]) => code === feature); if (match) tags.push(match[1]) }
    return Array.from(new Set(tags))
  }
  const v=model.name.toLowerCase(); const tags:Capability[]=[]; if (/(embed|bge|e5|m3e|gte|text2vec)/.test(v)) tags.push('向量'); if(/rerank/.test(v))tags.push('重排序'); if (/(dall-e|flux|stable-diffusion|image-gen)/.test(v)) tags.push('生图'); if (/(video|wan2|cogvideo|sora)/.test(v)) tags.push('生视频'); if(/(gpt-4o|vision|\bvl\b|llava|glm-4v|gemini|pixtral|multimodal)/.test(v)){tags.push('视觉');tags.push('多模态')} if(/(whisper|tts|speech|audio)/.test(v)) tags.push('语音'); if (/(reasoner|reasoning|deepseek-r1|(^|[-_/])r1|\bo1\b|qwq|\bo3\b|\bo4\b|thinking)/.test(v)) tags.push('推理'); if (/(search|web)/.test(v)) tags.push('联网'); if (/coder/.test(v)) tags.push('编程'); if (/(gpt|qwen|max|plus|chat|instruct|coder|kimi|glm|deepseek|claude|command|mistral|llama|hunyuan|ernie)/.test(v) && !v.includes('reasoner')) tags.push('工具'); return Array.from(new Set(tags))
}
/** 输入/输出模态描述 (目录未提供时返回空) */
function modalitiesFor(model: CatalogModelItem): string {
  if (!model.inputs?.length && !model.outputs?.length) return ''
  const label = (list?: string[]) => (list ?? []).map(item => MODALITY_TEXT[item] ?? item).join('/')
  return `输入:${label(model.inputs)} → 输出:${label(model.outputs)}`
}
function toggleCapability(cap: Capability) { const next = new Set(selectedCapabilities.value); if (next.has(cap)) next.delete(cap); else next.add(cap); selectedCapabilities.value = next }
function clearCapabilityFilter() { selectedCapabilities.value = new Set() }
function iconFor(capability:Capability) { if(capability==='工具')return Tools;if(capability==='视觉')return View;if(capability==='联网')return Connection;if(capability==='推理')return Cpu;if(capability==='向量')return DataAnalysis;if(capability==='重排序')return Sort;if(capability==='生图')return MagicStick;if(capability==='生视频')return VideoCamera;if(capability==='语音')return Microphone;if(capability==='编程')return Edit;if(capability==='结构化')return DataAnalysis;return Connection }
function openExternal(url?: string) { if (url) window.open(url, '_blank', 'noopener,noreferrer') }
function selectProvider(provider:Provider) { const record=saved(provider); selectedId.value=provider.id; clearCapabilityFilter(); form.baseUrl=record?.endpoint || (provider.id==='ollama'?systemStore.form.ollamaBaseUrl:(provider.baseUrl||systemStore.form.onlineBaseUrl)); form.apiKey=''; apiKeyMask.value=record?.apiKeyMasked ?? ''; apiKeyEditing.value=false; form.model=record?.modelName || (provider.id==='ollama'?systemStore.form.offlineModel:modelsFor(provider.id)[0]?.name||''); form.enabled=enabled[provider.id]??false; if(provider.id==='ollama'&&!systemStore.offlineModelOptions.length) void systemStore.refreshOfflineModels() }
async function loadProviders() { savedProviders.value=await fetchModelProviders(); for (const provider of providers) enabled[provider.id]=isEnabled(saved(provider)?.enabled) }
async function loadCatalog() { try { catalog.value=await fetchModelCatalog() } catch { /* 目录加载失败时沿用内置兑底列表 */ } }
async function persist(message=false) {
  if(saving.value||!form.baseUrl||!form.model)return
  saving.value=true
  try {
    const plainKey = apiKeyEditing.value ? form.apiKey : ''
    const record=await saveModelProvider({id:saved(selected.value)?.id,providerType:selected.value.id==='ollama'?'OLLAMA':selected.value.type==='offline'?'OFFLINE':'ONLINE',providerName:selected.value.id,endpoint:form.baseUrl,modelName:form.model,enabled:form.enabled,priorityNo:100,apiKey:plainKey})
    savedProviders.value=[...savedProviders.value.filter(item=>item.id!==record.id),record]
    enabled[selected.value.id]=form.enabled
    if(plainKey) apiKeyMask.value = record.apiKeyMasked || maskApiKey(plainKey)
    apiKeyEditing.value=false
    form.apiKey=''
    if(message)ElMessage.success('已自动保存')
  } catch { ElMessage.error('自动保存失败') } finally { saving.value=false }
}
async function testConnection() { if(!form.baseUrl){ElMessage.warning('请先填写服务地址');return}; testing.value=true; try { const plainKey = apiKeyEditing.value ? form.apiKey : ''; const result=await testModelProvider({id:saved(selected.value)?.id,providerName:selected.value.id,providerType:selected.value.id==='ollama'?'OLLAMA':selected.value.type==='offline'?'OFFLINE':'ONLINE',endpoint:form.baseUrl,apiKey:plainKey}); if(result.success){ if(plainKey) await persist(false); ElMessage.success(result.message) } else { ElMessage.error(result.message) } } finally { testing.value=false } }
async function refreshModels() { const record=saved(selected.value); if(!record){ElMessage.warning('请先完成配置');return}; refreshing.value=true; try { const models=await refreshProviderModels(record.id); if(selected.value.id==='ollama')await systemStore.refreshOfflineModels(); else if(models.length)liveModels[selected.value.id]=models.map(name=>({name,type:guessType(name)})); ElMessage.success(models.length?`已获取 ${models.length} 个模型`:'未获取到模型') } finally { refreshing.value=false } }
async function updateCatalog() { updatingCatalog.value=true; try { catalog.value=await refreshModelCatalog(); ElMessage.success(`模型目录已更新${catalog.value.version?`（${catalog.value.version}）`:''}`) } catch (error) { ElMessage.error((error as Error).message||'模型目录更新失败，请检查远程目录地址配置') } finally { updatingCatalog.value=false } }
watch(providerMode,()=>{const next=filtered.value[0];if(next)selectProvider(next)})
onMounted(async()=>{await Promise.all([systemStore.loadRuntimeConfig(false),loadProviders(),loadCatalog()]);selectProvider(defaultProvider)})
</script>
<template><div class="provider-page" v-loading="systemStore.loading"><header class="page-header"><div><h1>模型服务配置</h1><p>分别配置在线与离线模型服务。启用服务后，模型设置中才可选择其模型。目录来源：{{catalog.source==='remote'?`远程（${catalog.version||'已缓存'}）`:'内置'}}{{catalog.updatedAt?` · ${catalog.updatedAt}`:''}}</p></div><el-tooltip content="从远程地址拉取最新模型目录（需在系统配置中设置 model.catalog.remoteUrl，未配置时使用内置目录）"><el-button :icon="Download" :loading="updatingCatalog" @click="updateCatalog">更新目录</el-button></el-tooltip></header><div class="workspace"><aside><el-tabs v-model="providerMode" stretch><el-tab-pane label="在线模型" name="online"/><el-tab-pane label="离线模型" name="offline"/></el-tabs><el-input v-model="keyword" placeholder="搜索服务商" clearable/><div class="provider-list"><button v-for="item in filtered" :key="item.id" class="provider-item" :class="{active:item.id===selectedId}" @click="selectProvider(item)"><span class="provider-logo"><el-tooltip v-if="COMMUNITY_LOGO_IDS.has(item.id)" placement="top" content="图标为社区近似版本，非服务商官方品牌资产"><img v-if="item.logoUrl&&logoVisible[item.id]!==false" :src="item.logoUrl" :alt="item.name" @error="logoVisible[item.id]=false"/><span v-else>{{item.logo}}</span></el-tooltip><template v-else><img v-if="item.logoUrl&&logoVisible[item.id]!==false" :src="item.logoUrl" :alt="item.name" @error="logoVisible[item.id]=false"/><span v-else>{{item.logo}}</span></template></span><span class="provider-name">{{item.name}}</span><el-tag v-if="enabled[item.id]" type="success" size="small">已启用</el-tag></button></div></aside><main><div class="section-title"><div><div class="heading-line"><h2>{{selected.name}}</h2><el-tooltip content="前往服务商官网"><el-button circle text :icon="TopRight" @click="openExternal(selected.websiteUrl)"/></el-tooltip></div><p>{{selected.type==='online'?'在线模型服务':'离线模型服务'}}</p></div><el-tooltip content="启用后，可在模型设置中选择此服务商的模型"><el-switch v-model="form.enabled" @change="persist(true)"/></el-tooltip></div><el-form label-position="top"><el-form-item label="服务地址"><el-input v-model="form.baseUrl" placeholder="https://..." @change="persist(true)"/></el-form-item><el-form-item v-if="selected.type==='online'"><template #label>API Key <el-tooltip v-if="selected.apiKeyUrl" content="前往获取 API Key"><el-button text :icon="TopRight" @click="openExternal(selected.apiKeyUrl)"/></el-tooltip></template><el-input :model-value="apiKeyEditing ? form.apiKey : apiKeyMask" :type="apiKeyEditing ? 'password' : 'text'" :show-password="apiKeyEditing" :placeholder="apiKeyMask ? '输入新密钥以替换，留空保留现有密钥' : '输入 API Key'" @update:model-value="onApiKeyInput" @focus="onApiKeyFocus" @change="persist(true)"><template #append><el-button :loading="testing" @click="testConnection">检测</el-button></template></el-input></el-form-item><el-form-item label="默认模型"><el-select v-model="form.model" filterable allow-create style="width:100%" @change="persist(true)"><el-option v-for="model in allModels" :key="model.name" :label="model.name" :value="model.name"/></el-select></el-form-item><el-form-item><template #label><div class="models-label"><span>模型</span><el-button text type="primary" :icon="RefreshRight" :loading="refreshing" @click="refreshModels">刷新模型列表</el-button></div></template><div class="capability-filter"><el-check-tag :checked="!selectedCapabilities.size" @change="clearCapabilityFilter()">全部</el-check-tag><el-check-tag v-for="tag in ['工具','视觉','联网','推理','向量','重排序','生图','生视频','多模态','语音','编程','结构化']" :key="tag" :checked="selectedCapabilities.has(tag as Capability)" @change="toggleCapability(tag as Capability)">{{tag}}</el-check-tag></div><div class="model-list"><div class="model-scroll"><div v-for="model in visibleModels" :key="model.name" class="model-row"><div class="model-main"><span class="model-name">{{model.name}}</span><el-tooltip v-if="modalitiesFor(model)" :content="modalitiesFor(model)"><span class="model-modalities">{{modalitiesFor(model)}}</span></el-tooltip></div><span class="capability-icons"><el-tooltip v-for="capability in capabilitiesFor(model)" :key="capability" :content="capability"><el-icon><component :is="iconFor(capability)"/></el-icon></el-tooltip></span></div><div v-if="!visibleModels.length" class="empty-models"><el-icon><Connection/></el-icon> 当前筛选条件下没有模型</div></div></div></el-form-item></el-form></main></div></div></template>
<style scoped>.provider-page{max-width:1440px;margin:0 auto}.page-header{display:flex;align-items:baseline;justify-content:space-between;gap:24px;margin-bottom:20px}.page-header h1{margin:0;font-size:22px}.page-header p,.section-title p{margin:0;color:#64748b}.workspace{display:grid;grid-template-columns:300px minmax(0,1fr);border:1px solid #e2e8f0;min-height:620px;background:#fff}.workspace aside{padding:14px;border-right:1px solid #e2e8f0}.provider-list{margin-top:12px;display:grid;gap:3px}.provider-item{width:100%;display:flex;align-items:center;gap:10px;padding:9px 10px;border:0;border-radius:5px;background:transparent;text-align:left;cursor:pointer;color:#334155}.provider-item:hover{background:#f8fafc}.provider-item.active{background:#eff6ff;color:#1d4ed8}.provider-item .el-tag{margin-left:auto}.provider-name{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.provider-logo{width:28px;height:28px;flex:0 0 28px;border:1px solid #e2e8f0;border-radius:5px;background:#fff;display:inline-flex;align-items:center;justify-content:center;overflow:hidden;font-weight:700;font-size:11px;color:#0f3b67}.provider-logo img{width:20px;height:20px;object-fit:contain}.workspace main{padding:24px}.section-title{display:flex;justify-content:space-between;align-items:center;margin-bottom:18px}.section-title h2{margin:0;font-size:18px}.heading-line{display:flex;align-items:center;gap:4px}.models-label{display:flex;align-items:center;justify-content:space-between;width:100%}.capability-filter{display:flex;gap:8px;margin:-4px 0 8px}.model-list{border:1px solid #e2e8f0;border-radius:5px;overflow:hidden}.model-scroll{max-height:330px;overflow-y:auto}.model-row{display:flex;align-items:center;justify-content:space-between;min-height:46px;padding:0 12px;border-bottom:1px solid #eef2f7}.model-row:last-child{border-bottom:0}.model-main{display:flex;align-items:center;gap:12px;min-width:0}.model-name{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:13px;color:#1e293b}.model-modalities{font-size:12px;color:#94a3b8;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.capability-icons{display:flex;gap:10px;color:#2563eb}.capability-icons .el-icon{font-size:17px}.empty-models{display:flex;align-items:center;gap:8px;padding:18px;color:#64748b;font-size:13px}@media(max-width:800px){.page-header{align-items:flex-start;flex-direction:column;gap:6px}.workspace{grid-template-columns:1fr}.workspace aside{border-right:0;border-bottom:1px solid #e2e8f0}}</style>
