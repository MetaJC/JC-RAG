<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { userApi } from '../../api/user'
const agents = ref<any[]>([])
onMounted(async () => {
  try { agents.value = await userApi.listAgents() } catch { /* ignore */ }
})
</script>
<template>
  <div class="c-shell">
    <header class="c-header">
      <div class="c-header-left"><router-link to="/chat" class="logo">RAG 问答</router-link></div>
      <nav class="c-header-nav">
        <router-link to="/chat">问答</router-link>
        <router-link to="/api-keys">API Keys</router-link>
        <router-link to="/profile">个人中心</router-link>
      </nav>
    </header>
    <main class="c-main">
      <h2>Agent 列表</h2>
      <div class="agent-grid">
        <el-card v-for="a in agents" :key="a.id" shadow="hover" class="agent-card">
          <h3>{{ a.agentName }}</h3>
          <p>{{ a.description }}</p>
        </el-card>
        <el-card v-if="!agents.length" shadow="never">
          <p style="color:#6b7280">暂无可用 Agent，请等待管理员配置</p>
        </el-card>
      </div>
    </main>
  </div>
</template>
<style scoped>
.c-shell { display: flex; flex-direction: column; min-height: 100vh; }
.c-header { display: flex; align-items: center; justify-content: space-between; height: 56px; padding: 0 24px; background: #0f2744; color: #fff; }
.c-header-nav { display: flex; gap: 16px; }
.c-header-nav a { color: rgba(255,255,255,.8); text-decoration: none; font-size: 14px; }
.c-main { flex: 1; max-width: 900px; width: 100%; margin: 0 auto; padding: 24px; }
.agent-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; margin-top: 16px; }
.agent-card { cursor: pointer; }
.agent-card h3 { margin-bottom: 8px; }
.agent-card p { color: #6b7280; font-size: 13px; margin-bottom: 12px; }
.logo { color: #fff; text-decoration: none; font-weight: 700; }
</style>
