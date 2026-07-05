<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi, isUnauthorizedError, extractErrorMessage } from '../../api/user'

const router = useRouter()
const profile = ref({ nickname: '', email: '', phone: '', username: '', userType: '', todayQueries: 0 })
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const saving = ref(false)

onMounted(async () => {
  try {
    const user = await userApi.currentUser()
    if (!user.authenticated) { router.push('/login'); return }
    const p = await userApi.getProfile()
    profile.value = { nickname: p.nickname || '', email: p.email || '', phone: p.phone || '', username: p.username || '', userType: p.userType || '', todayQueries: p.todayQueries || 0 }
  } catch (error) {
    if (isUnauthorizedError(error)) {
      router.push('/login')
      return
    }
    ElMessage.error(extractErrorMessage(error, '加载个人信息失败'))
  }
})

async function saveProfile() {
  saving.value = true
  try {
    await userApi.updateProfile({ nickname: profile.value.nickname, email: profile.value.email, phone: profile.value.phone })
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

async function changePassword() {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) { ElMessage.warning('请填写完整'); return }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  try {
    await userApi.changePassword({ oldPassword: passwordForm.value.oldPassword, newPassword: passwordForm.value.newPassword })
    ElMessage.success('密码已修改')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch { ElMessage.error('密码修改失败，旧密码可能不正确') }
}
</script>
<template>
  <div class="c-shell">
    <header class="c-header">
      <router-link to="/chat" class="logo">← 返回问答</router-link>
    </header>
    <main class="c-main">
      <h2>个人中心</h2>
      <div class="profile-grid">
        <el-card shadow="never" class="profile-card">
          <template #header>基本信息</template>
          <el-form label-position="top">
            <el-form-item label="用户名"><el-input :model-value="profile.username" disabled /></el-form-item>
            <el-form-item label="用户类型"><el-tag>{{ profile.userType }}</el-tag></el-form-item>
            <el-form-item label="昵称"><el-input v-model="profile.nickname" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="profile.email" /></el-form-item>
            <el-form-item label="电话"><el-input v-model="profile.phone" /></el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
          </el-form>
        </el-card>
        <el-card shadow="never" class="profile-card">
          <template #header>修改密码</template>
          <el-form label-position="top">
            <el-form-item label="旧密码"><el-input v-model="passwordForm.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="passwordForm.newPassword" type="password" show-password /></el-form-item>
            <el-form-item label="确认密码"><el-input v-model="passwordForm.confirmPassword" type="password" show-password /></el-form-item>
            <el-button type="primary" @click="changePassword">修改密码</el-button>
          </el-form>
        </el-card>
        <el-card shadow="never" class="profile-card">
          <template #header>使用统计</template>
          <div class="stat-row"><span>今日问答次数</span><strong>{{ profile.todayQueries }}</strong></div>
          <div class="stat-row"><span>账户状态</span><el-tag type="success">正常</el-tag></div>
        </el-card>
      </div>
    </main>
  </div>
</template>
<style scoped>
.c-shell { display: flex; flex-direction: column; min-height: 100vh; background: #f0f2f5; }
.c-header { display: flex; align-items: center; height: 52px; padding: 0 16px; background: #0f2744; color: #fff; }
.c-header a { color: #fff; text-decoration: none; font-size: 14px; }
.c-main { flex: 1; max-width: 900px; width: 100%; margin: 0 auto; padding: 24px; }
.c-main h2 { margin-bottom: 20px; }
.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.profile-card { border-radius: 12px; }
.profile-card:last-child { grid-column: 1 / -1; }
.stat-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f3f4f6; }
.stat-row:last-child { border-bottom: none; }
</style>
