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
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useSystemStore } from '../stores/system'
const systemStore = useSystemStore(); const saving = ref(false)
async function save(){saving.value=true;try{await systemStore.saveSettings();ElMessage.success('模型参数已保存')}finally{saving.value=false}}
onMounted(() => void systemStore.loadRuntimeConfig(false))
</script>
<template><div class="parameter-page" v-loading="systemStore.loading"><header class="page-header"><div><h1>模型参数设置</h1><p>统一维护生成、Embedding、切块与检索参数。</p></div><el-button type="primary" :loading="saving" @click="save">保存参数</el-button></header><div class="grid"><el-card shadow="never" header="生成参数"><el-form label-position="top"><el-form-item label="Temperature"><el-slider v-model="systemStore.form.temperature" :min="0" :max="2" :step="0.1" show-input/></el-form-item><el-form-item label="Top P"><el-slider v-model="systemStore.form.topP" :min="0" :max="1" :step="0.05" show-input/></el-form-item><el-form-item label="最大 Token"><el-input-number v-model="systemStore.form.maxTokens" :min="128" :max="8192" :step="128"/></el-form-item></el-form></el-card><el-card shadow="never" header="检索与切块参数"><el-form label-position="top"><el-form-item label="Top K"><el-input-number v-model="systemStore.form.topK" :min="1" :max="20"/></el-form-item><el-form-item label="候选召回数 Top N"><el-input-number v-model="systemStore.form.topN" :min="1" :max="100"/></el-form-item><el-form-item label="相似度阈值"><el-slider v-model="systemStore.form.similarityThreshold" :min="0" :max="1" :step="0.05" show-input/></el-form-item><el-form-item label="Chunk Size / Overlap"><el-input-number v-model="systemStore.form.chunkSize" :min="100" :max="2000"/><span> &nbsp;/&nbsp; </span><el-input-number v-model="systemStore.form.chunkOverlap" :min="0" :max="500"/></el-form-item></el-form></el-card></div></div></template>
<style scoped>.parameter-page{max-width:1120px;margin:0 auto}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px}.page-header h1{margin:0;font-size:22px}.page-header p{margin:8px 0 0;color:#64748b}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}@media(max-width:800px){.grid{grid-template-columns:1fr}}</style>
