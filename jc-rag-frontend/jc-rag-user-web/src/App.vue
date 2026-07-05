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
