<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import { useAuthStore } from '../stores/auth'
import PreferencesDialog from './PreferencesDialog.vue'

const router = useRouter()
const authStore = useAuthStore()
const dialogVisible = ref(false)
const preferencesVisible = ref(false)
const submitting = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
})

const user = computed(() => authStore.user)

function resetDialogState() {
  dialogVisible.value = false
  submitting.value = false
  form.oldPassword = ''
  form.newPassword = ''
}

async function onMenuCommand(command: string) {
  if (command === 'change-password') {
    dialogVisible.value = true
    return
  }
  if (command === 'preferences') {
    preferencesVisible.value = true
    return
  }
  if (command === 'logout') {
    await onLogout()
  }
}

async function onLogout() {
  resetDialogState()
  await authStore.logoutCurrentUser()
  await router.replace('/login')
}

async function onChangePassword() {
  if (!form.oldPassword || !form.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  submitting.value = true
  try {
    await authStore.changeCurrentPassword(form.oldPassword, form.newPassword)
    resetDialogState()
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    await authStore.logoutCurrentUser()
    await router.replace('/login')
  } catch (error) {
    console.error(error)
    ElMessage.error('密码修改失败，请确认原密码是否正确')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-toolbar" v-if="user">
    <el-dropdown trigger="click" @command="onMenuCommand">
      <el-button size="small" type="primary">
        账户：{{ user.nickname || user.username }}
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="change-password">修改密码</el-dropdown-item>
          <el-dropdown-item command="preferences" divided>偏好设置</el-dropdown-item>
          <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <el-dialog
      v-model="dialogVisible"
      title="修改密码"
      width="420px"
      destroy-on-close
      append-to-body
      @closed="resetDialogState"
    >
      <el-form label-position="top">
        <el-form-item label="原密码">
          <el-input v-model="form.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogState">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onChangePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <PreferencesDialog v-model:visible="preferencesVisible" />
  </div>
</template>

<style scoped>
.auth-toolbar {
  display: flex;
  align-items: center;
}
</style>
