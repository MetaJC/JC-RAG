<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '../stores/auth'
import SlideCaptcha from '../components/SlideCaptcha.vue'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const captchaRef = ref<InstanceType<typeof SlideCaptcha> | null>(null)

const form = reactive({
  username: 'admin',
  password: '123456',
})

/**
 * 滑块验证通过即触发登录
 */
async function onCaptchaChange(data: { passed: boolean; captchaToken?: string }) {
  if (!data.passed || !data.captchaToken) return
  if (loading.value) return
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    captchaRef.value?.reset()
    return
  }
  loading.value = true
  try {
    await authStore.loginWithPassword(form.username, form.password, data.captchaToken)
    ElMessage.success('登录成功')
    router.push('/system/settings')
  } catch (error) {
    console.error(error)
    ElMessage.error('登录失败，请检查用户名或密码')
  } finally {
    loading.value = false
    captchaRef.value?.reset()
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <template #header>
        <div class="login-title"> RAG 系统登录</div>
      </template>

      <el-form label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" :disabled="loading">
            <template #prefix>
              <el-icon class="input-icon"><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
            :disabled="loading"
          >
            <template #prefix>
              <el-icon class="input-icon"><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证登录">
          <SlideCaptcha
            ref="captchaRef"
            hint-text="将滑块拖动到最右侧以登录"
            @change="onCaptchaChange"
          />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #0f2744 0%, #dbe8f8 100%);
}

.login-card {
  width: 520px;
  border-radius: 18px;
  padding: 28px 32px 24px;
}
.login-card :deep(.el-form-item) {
  width: 100%;
  display: block;
}
.login-card :deep(.el-form-item__content) {
  width: 100%;
  display: block;
}
.login-card :deep(.el-input) {
  width: 100% !important;
  display: flex !important;
}
.login-card :deep(.el-input__wrapper) {
  width: 100%;
  padding: 4px 12px;
  box-sizing: border-box;
}
.login-card :deep(.el-input__inner) {
  width: 100%;
  font-size: 15px;
  height: 40px;
  line-height: 40px;
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  text-align: center;
  color: #0f2744;
}

.input-icon {
  display: inline-flex;
  align-items: center;
  height: 100%;
  color: #6b7280;
  font-size: 16px;
}
</style>
