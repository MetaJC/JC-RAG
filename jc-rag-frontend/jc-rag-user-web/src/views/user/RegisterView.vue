<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '../../api/user'

const router = useRouter()
const form = ref({ username: '', nickname: '', password: '', confirmPassword: '', email: '' })
const loading = ref(false)

async function onRegister() {
  if (!form.value.username || !form.value.password) { ElMessage.warning('用户名和密码不能为空'); return }
  if (form.value.password !== form.value.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  loading.value = true
  try {
    const res = await userApi.register({ username: form.value.username, password: form.value.password, nickname: form.value.nickname || form.value.username, email: form.value.email })
    if (res.success) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch { ElMessage.error('注册失败，请稍后重试') }
  finally { loading.value = false }
}
</script>
<template>
  <div class="register-wrapper">
    <el-card class="register-card" shadow="never">
      <h2>用户注册</h2>
      <p class="desc">创建 JC RAG 智能问答系统账号</p>
      <el-form label-position="top">
        <el-form-item label="用户名"><el-input v-model="form.username" placeholder="登录用" /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" placeholder="显示名称" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="选填" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="form.confirmPassword" type="password" show-password /></el-form-item>
        <el-button type="primary" :loading="loading" @click="onRegister" style="width:100%">注册</el-button>
      </el-form>
      <div class="links"><router-link to="/login">已有账号？去登录</router-link></div>
    </el-card>
  </div>
</template>
<style scoped>
.register-wrapper { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: linear-gradient(135deg, #0f2744, #1a4a7a); }
.register-card { width: 420px; padding: 30px; border-radius: 16px; }
.register-card h2 { text-align: center; margin-bottom: 4px; }
.desc { text-align: center; color: #6b7280; margin-bottom: 20px; font-size: 13px; }
.links { text-align: center; margin-top: 16px; }
.links a { color: #409eff; text-decoration: none; font-size: 13px; }
</style>
