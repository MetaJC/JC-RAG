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
