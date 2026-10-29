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
/**
 * App 根组件。
 * <p>keep-alive 缓存 ChatHomeView, 用户从子页面 (我的记忆 / 偏好设置 / 个人中心) 返回聊天时,
 * 滚动位置/输入内容/已加载数据均不丢失 (onMounted 不会再触发, 数据由组件自身保留).
 * <p>不缓存的视图 (登录/注册等) 正常销毁重建, 不浪费内存.
 */
const keepAliveInclude = ['ChatHomeView']
</script>

<template>
  <router-view v-slot="{ Component }">
    <keep-alive :include="keepAliveInclude">
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>
