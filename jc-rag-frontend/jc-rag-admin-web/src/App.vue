<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AuthToolbar from './components/AuthToolbar.vue'

const route = useRoute()
const showToolbar = computed(() => route.path !== '/login')
</script>

<template>
  <div class="app-shell">
    <header v-if="showToolbar" class="top-bar">
      <div class="top-bar-left">RAG 系统</div>
      <div class="top-bar-right">
        <AuthToolbar />
      </div>
    </header>
    <router-view />
  </div>
</template>

<style scoped>
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 18px;
  background: #0f2744;
  color: #fff;
}

.top-bar-left {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.4px;
}

.top-bar-right {
  display: flex;
  align-items: center;
}
</style>
