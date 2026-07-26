<script setup lang="ts">
import { onMounted } from 'vue'
import { useSystemStore } from '../stores/system'
const systemStore = useSystemStore()
onMounted(() => { void systemStore.refreshServiceHealth(); void systemStore.refreshDevices() })
</script>
<template><div class="developer-page"><header class="page-header"><div><h1>开发者选项</h1><p>仅集中展示部署、服务连接、设备与诊断信息。</p></div><el-button @click="systemStore.refreshServiceHealth()">刷新状态</el-button></header>
  <el-tabs><el-tab-pane label="服务状态"><el-table :data="systemStore.healthItems" stripe><el-table-column prop="name" label="服务" min-width="150"/><el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="row.status==='UP'?'success':row.status==='DOWN'?'danger':'info'">{{ row.status }}</el-tag></template></el-table-column><el-table-column prop="endpoint" label="地址" min-width="260"/><el-table-column prop="description" label="说明" min-width="220"/></el-table></el-tab-pane>
  <el-tab-pane label="设备与资源"><el-table :data="systemStore.devices"><el-table-column prop="name" label="设备"/><el-table-column prop="type" label="类型" width="120"/><el-table-column prop="detail" label="详情"/></el-table></el-tab-pane></el-tabs>
</div></template>
<style scoped>.developer-page{max-width:1440px;margin:0 auto}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px}.page-header h1{margin:0;font-size:22px}.page-header p{margin:8px 0 0;color:#64748b}</style>
