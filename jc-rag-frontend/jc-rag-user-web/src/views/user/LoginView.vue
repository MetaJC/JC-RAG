<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock, Message, User } from '@element-plus/icons-vue'
import { userApi } from '../../api/user'
import SlideCaptcha from '../../components/SlideCaptcha.vue'

type TabKey = 'account' | 'code' | 'third'
type ThirdProvider = 'wechat' | 'alipay' | 'huawei'

const router = useRouter()
const activeTab = ref<TabKey>('account')

// 账号密码登录
const accountForm = reactive({ username: 'user01', password: '123456' })
// 验证码登录
const codeForm = reactive({ phone: '', code: '', agree: false })
const codeCountdown = ref(0)
const codeLoading = ref(false)
const captchaPassed = ref(false)
const captchaToken = ref('')
const loading = ref(false)
const captchaRef = ref<InstanceType<typeof SlideCaptcha> | null>(null)

// 连续失败计数: 后端告知是否需要滑动验证
const failCount = ref(0)
const needCaptcha = ref(false)

// 协议勾选 (账号密码 tab)
const accountAgree = ref(false)

function onCaptchaChange(data: { passed: boolean; captchaToken?: string }) {
  captchaPassed.value = data.passed
  captchaToken.value = data.captchaToken || ''
  // 验证码 tab 验证通过后自动触发发送
  if (data.passed && activeTab.value === 'code' && codeCountdown.value === 0 && !codeLoading.value) {
    sendCode()
  }
}

async function onAccountLogin() {
  if (!accountForm.username || !accountForm.password) {
    ElMessage.warning('请输入用户名和密码'); return
  }
  if (needCaptcha.value && (!captchaPassed.value || !captchaToken.value)) {
    ElMessage.warning('请先完成滑动验证'); return
  }
  if (!accountAgree.value) {
    ElMessage.warning('请先勾选并同意《用户协议》与《隐私政策》'); return
  }
  loading.value = true
  try {
    const payload: Record<string, string> = { ...accountForm }
    if (captchaToken.value) payload.captchaToken = captchaToken.value
    const res = await userApi.login(payload) as { success?: boolean; failCount?: number; needCaptcha?: boolean; message?: string }
    console.log('[Login] response:', res, 'success:', res?.success)
    if (res && res.success === false) {
      if (typeof res.failCount === 'number') failCount.value = res.failCount
      if (typeof res.needCaptcha === 'boolean') needCaptcha.value = res.needCaptcha
      ElMessage.error(res.message || '登录失败')
      captchaPassed.value = false
      captchaToken.value = ''
      captchaRef.value?.reset()
      return
    }
    if (res && typeof res === 'object') {
      if (typeof res.failCount === 'number') failCount.value = res.failCount
      if (typeof res.needCaptcha === 'boolean') needCaptcha.value = res.needCaptcha
    }
    ElMessage.success('登录成功')
    console.log('[Login] success, verifying session before navigation, pathname:', window.location.pathname)
    // 关键修复: 登录后先验证会话已就绪再跳转, 避免后端 session 写入与浏览器 cookie 应用存在时差
    // 导致 ChatHomeView 首次 /auth/me 请求 401 → 被 http 拦截器重定向回 /login
    try {
      const me = await userApi.currentUser()
      if (!me?.authenticated) {
        console.warn('[Login] /auth/me not authenticated after login, retrying once...')
        await new Promise(resolve => setTimeout(resolve, 150))
        const me2 = await userApi.currentUser()
        if (!me2?.authenticated) throw new Error('会话未生效, 请重试')
      }
    } catch (verifyErr) {
      console.error('[Login] session verify failed:', verifyErr)
      ElMessage.error('登录状态同步失败, 请重试')
      loading.value = false
      return
    }
  } catch (e: any) {
    console.error('[Login] caught error:', e?.message, 'response:', e?.response?.data)
    const data = e?.response?.data
    if (data && typeof data === 'object') {
      if (typeof data.failCount === 'number') failCount.value = data.failCount
      if (typeof data.needCaptcha === 'boolean') needCaptcha.value = data.needCaptcha
      ElMessage.error(data.message || '登录失败')
    } else {
      ElMessage.error('登录失败，请检查用户名和密码')
    }
    captchaPassed.value = false
    captchaToken.value = ''
    captchaRef.value?.reset()
    return
  } finally {
    loading.value = false
  }
  // 主动验证 session cookie 已生效: 调用 currentUser, 拿不到 user.authenticated=true 就等
  // 解决"已登录浏览器残留旧 cookie"导致的 ChatHomeView 首次 401
  const SESSION_READY_KEY = 'jc_rag_session_ready'
  let verified = false
  for (let i = 0; i < 10; i++) {
    try {
      const u = await userApi.currentUser()
      if (u && u.authenticated) {
        verified = true
        console.log('[LoginView] session cookie verified after', i, 'attempts')
        break
      }
    } catch (e) {
      console.warn('[LoginView] currentUser verify attempt', i, 'failed:', (e as any)?.message)
    }
    await new Promise(resolve => setTimeout(resolve, 200))
  }
  if (!verified) {
    ElMessage.error('登录态验证失败, 请重试')
    return
  }
  // 通知 ChatHomeView 走"从登录来"分支, 延迟更久再发请求
  sessionStorage.setItem(SESSION_READY_KEY, String(Date.now()))
  await router.push('/chat').catch(err => console.error('[LoginView] router.push error:', err))
}

async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(codeForm.phone)) {
    ElMessage.warning('请输入正确的手机号'); return
  }
  if (!captchaPassed.value) {
    ElMessage.warning('请先完成滑动验证'); return
  }
  codeLoading.value = true
  // 预留接口, 后端实现短信发送
  // await userApi.sendSmsCode(codeForm.phone)
  ElMessage.success('验证码已发送（演示）')
  codeLoading.value = false
  codeCountdown.value = 60
  const timer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function onSendCode() {
  sendCode()
}

async function onCodeLogin() {
  if (!codeForm.phone || !codeForm.code) {
    ElMessage.warning('请输入手机号和验证码'); return
  }
  if (!codeForm.agree) {
    ElMessage.warning('请先勾选并同意《用户协议》与《隐私政策》'); return
  }
  ElMessage.warning('验证码登录功能待实现')
}

function onThirdPartyLogin(provider: ThirdProvider) {
  thirdProvider.value = provider
  drawQrCode()
}

function onThirdPartyClick() {
  ElMessage.info('功能正在开发中，敬请期待')
}

function onForgotPassword() {
  ElMessage.info('功能开发中')
}

function openAgreement(docType: 'USER_AGREEMENT' | 'PRIVACY_POLICY') {
  router.push({ path: '/agreement', query: { type: docType } })
}

// 第三方登录
const thirdProvider = ref<ThirdProvider>('wechat')
const qrCanvas = ref<HTMLCanvasElement | null>(null)

const thirdProviderName = (p: ThirdProvider) =>
  p === 'wechat' ? '微信' : p === 'alipay' ? '支付宝' : '华为'

/**
 * Canvas 绘制假二维码
 */
function drawQrCode() {
  nextTick(() => {
    const canvas = qrCanvas.value
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    const size = 200
    const grid = 25
    const cell = size / grid
    const dpr = window.devicePixelRatio || 1
    canvas.width = size * dpr
    canvas.height = size * dpr
    canvas.style.width = size + 'px'
    canvas.style.height = size + 'px'
    ctx.scale(dpr, dpr)
    ctx.fillStyle = '#fff'
    ctx.fillRect(0, 0, size, size)
    ctx.fillStyle = '#111'

    const drawFinder = (x: number, y: number) => {
      ctx.fillRect(x, y, 7 * cell, cell)
      ctx.fillRect(x, y + 6 * cell, 7 * cell, cell)
      ctx.fillRect(x, y, cell, 7 * cell)
      ctx.fillRect(x + 6 * cell, y, cell, 7 * cell)
      ctx.fillRect(x + 2 * cell, y + 2 * cell, 3 * cell, 3 * cell)
    }
    drawFinder(0, 0)
    drawFinder((grid - 7) * cell, 0)
    drawFinder(0, (grid - 7) * cell)

    const isFinderArea = (gx: number, gy: number) => {
      if (gx < 8 && gy < 8) return true
      if (gx > grid - 9 && gy < 8) return true
      if (gx < 8 && gy > grid - 9) return true
      return false
    }
    const centerStart = Math.floor(grid / 2) - 3
    const centerEnd = centerStart + 7
    const isCenterArea = (gx: number, gy: number) =>
      gx >= centerStart && gx < centerEnd && gy >= centerStart && gy < centerEnd

    for (let y = 0; y < grid; y++) {
      for (let x = 0; x < grid; x++) {
        if (isFinderArea(x, y)) continue
        if (isCenterArea(x, y)) continue
        if (Math.random() > 0.5) {
          ctx.fillRect(x * cell, y * cell, cell, cell)
        }
      }
    }

    const cx = size / 2
    const cy = size / 2
    const r = 22
    ctx.fillStyle = '#fff'
    ctx.beginPath()
    ctx.arc(cx, cy, r, 0, Math.PI * 2)
    ctx.fill()
    const colors: Record<ThirdProvider, string> = {
      wechat: '#07c160',
      alipay: '#1677ff',
      huawei: '#c7000b',
    }
    ctx.fillStyle = colors[thirdProvider.value]
    ctx.beginPath()
    ctx.arc(cx, cy, r - 4, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 18px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const label = thirdProvider.value === 'wechat' ? '微' : thirdProvider.value === 'alipay' ? '支' : '华'
    ctx.fillText(label, cx, cy + 1)
  })
}

watch(activeTab, (tab) => {
  if (tab === 'third') drawQrCode()
})
onMounted(() => {
  if (activeTab.value === 'third') drawQrCode()
})

function switchTab(key: TabKey) {
  activeTab.value = key
  captchaPassed.value = false
  captchaToken.value = ''
  captchaRef.value?.reset()
}
</script>

<template>
  <div class="login-wrapper">
    <el-card class="login-card" shadow="never">
      <h2>用户登录</h2>
      <p class="login-desc">JC RAG 智能问答系统</p>

      <el-tabs v-model="activeTab" class="login-tabs" @tab-change="(k: any) => switchTab(k as TabKey)">
        <!-- 账号密码 -->
        <el-tab-pane label="账号密码" name="account">
          <el-form label-position="top" class="login-form">
            <el-form-item label="用户名">
              <el-input v-model="accountForm.username" placeholder="请输入用户名" @keydown.enter="onAccountLogin">
                <template #prefix>
                  <el-icon class="input-icon"><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="accountForm.password"
                type="password"
                show-password
                placeholder="请输入密码"
                @keydown.enter="onAccountLogin"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item v-if="needCaptcha" label="安全验证">
              <SlideCaptcha ref="captchaRef" @change="onCaptchaChange" />
              <div class="captcha-hint">连续登录失败 {{ failCount }} 次, 请完成滑动验证</div>
            </el-form-item>
            <el-button type="primary" :loading="loading" class="login-btn" @click="onAccountLogin">登录</el-button>
            <div class="agreement-row">
              <el-checkbox v-model="accountAgree">
                我已阅读并同意
                <a class="link" @click.prevent="openAgreement('USER_AGREEMENT')">《用户协议》</a>
                与
                <a class="link" @click.prevent="openAgreement('PRIVACY_POLICY')">《隐私政策》</a>
              </el-checkbox>
            </div>
          </el-form>
        </el-tab-pane>

        <!-- 验证码登录 -->
        <el-tab-pane label="验证码登录" name="code">
          <el-form label-position="top" class="login-form">
            <el-form-item label="手机号">
              <el-input v-model="codeForm.phone" placeholder="请输入手机号" maxlength="11">
                <template #prefix>
                  <el-icon class="input-icon"><Iphone /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="安全验证">
              <SlideCaptcha ref="captchaRef" @change="onCaptchaChange" />
            </el-form-item>
            <el-form-item label="验证码">
              <el-input v-model="codeForm.code" placeholder="请输入 6 位短信验证码" maxlength="6">
                <template #prefix>
                  <el-icon class="input-icon"><Message /></el-icon>
                </template>
                <template #append>
                  <el-button
                    :disabled="codeCountdown > 0"
                    :loading="codeLoading"
                    class="code-btn"
                    @click="onSendCode"
                  >
                    {{ codeCountdown > 0 ? `${codeCountdown}s 后重试` : '获取验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-button type="primary" :loading="loading" class="login-btn" @click="onCodeLogin">登录</el-button>
            <div class="agreement-row">
              <el-checkbox v-model="codeForm.agree">
                我已阅读并同意
                <a class="link" @click.prevent="openAgreement('USER_AGREEMENT')">《用户协议》</a>
                与
                <a class="link" @click.prevent="openAgreement('PRIVACY_POLICY')">《隐私政策》</a>
              </el-checkbox>
              <a class="link forgot" @click.prevent="onForgotPassword">忘记密码</a>
            </div>
          </el-form>
        </el-tab-pane>

        <!-- 第三方登录 -->
        <el-tab-pane label="第三方登录" name="third">
          <div class="third-party">
            <p class="third-party__hint">
              请使用 <b>{{ thirdProviderName(thirdProvider) }}</b> 扫描下方二维码登录
            </p>
            <div class="third-party__qrcode">
              <canvas ref="qrCanvas" class="third-party__canvas" @click="onThirdPartyClick"></canvas>
              <div class="third-party__overlay">
                <span>功能正在开发中<br />敬请期待</span>
              </div>
            </div>
            <div class="third-party__list">
              <div
                class="third-party__item"
                :class="{ 'is-active': thirdProvider === 'wechat' }"
                @click="onThirdPartyLogin('wechat')"
              >
                <div class="third-party__icon third-party__icon--wechat">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
                    <path d="M8.69 4C5.05 4 2 6.5 2 9.74c0 1.78.97 3.36 2.49 4.41l-.62 1.86 2.18-1.1c.65.18 1.34.27 2.06.27.21 0 .41-.01.62-.03-.13-.42-.2-.86-.2-1.31 0-2.62 2.43-4.74 5.42-4.74.18 0 .35.01.52.03C13.78 6.46 11.46 4 8.69 4M6.4 7.7c.46 0 .83.37.83.83a.83.83 0 0 1-1.66 0c0-.46.37-.83.83-.83m4.6 0c.46 0 .83.37.83.83a.83.83 0 0 1-1.66 0c0-.46.37-.83.83-.83m6.05 2.7c-2.7 0-4.88 1.88-4.88 4.21 0 2.32 2.18 4.21 4.88 4.21.62 0 1.21-.1 1.76-.26l1.81.95-.51-1.55c1.27-.85 2.1-2.13 2.1-3.59 0-2.33-2.18-4.21-4.88-4.21m-1.6 2.4c.39 0 .71.32.71.71 0 .4-.32.72-.71.72-.4 0-.72-.32-.72-.72 0-.39.32-.71.72-.71m3.2 0c.39 0 .71.32.71.71 0 .4-.32.72-.71.72-.4 0-.72-.32-.72-.72 0-.39.32-.71.72-.71z" />
                  </svg>
                </div>
                <span class="third-party__label">微信</span>
              </div>
              <div
                class="third-party__item"
                :class="{ 'is-active': thirdProvider === 'alipay' }"
                @click="onThirdPartyLogin('alipay')"
              >
                <div class="third-party__icon third-party__icon--alipay">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
                    <path d="M19 5H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2M7.5 9.5c.5 0 1 .5 1 1s-.5 1-1 1-1-.5-1-1 .5-1 1-1m9 0c.5 0 1 .5 1 1s-.5 1-1 1-1-.5-1-1 .5-1 1-1m-9 5c.5 0 1 .5 1 1s-.5 1-1 1-1-.5-1-1 .5-1 1-1m9 0c.5 0 1 .5 1 1s-.5 1-1 1-1-.5-1-1 .5-1 1-1M7 13.5h10v1.5H7z" />
                  </svg>
                </div>
                <span class="third-party__label">支付宝</span>
              </div>
              <div
                class="third-party__item"
                :class="{ 'is-active': thirdProvider === 'huawei' }"
                @click="onThirdPartyLogin('huawei')"
              >
                <div class="third-party__icon third-party__icon--huawei">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
                    <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2m0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8m-1-12h2v3h3v2h-3v3h-2v-3H8v-2h3z" />
                  </svg>
                </div>
                <span class="third-party__label">华为</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <div class="login-links">
        <router-link to="/register">没有账号？去注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #0f2744 0%, #1a4a7a 100%);
}
.login-card {
  width: 540px;
  padding: 36px 36px 30px;
  border-radius: 16px;
}
.login-card h2 {
  text-align: center;
  margin-bottom: 4px;
}
.login-desc {
  text-align: center;
  color: #6b7280;
  margin-bottom: 24px;
  font-size: 13px;
}
.login-tabs {
  margin-top: 4px;
}
.login-tabs :deep(.el-tabs__nav-wrap)::after {
  display: none;
}
.login-tabs :deep(.el-tabs__nav) {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  width: 100%;
  float: none;
}
.login-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  padding: 0 12px;
  text-align: center;
}
.login-form {
  margin-top: 8px;
}
/* ===== 输入框 / 按钮: 强制占满容器, 跟管理端一致 ===== */
.login-form :deep(.el-form-item) {
  width: 100%;
  display: block;
}
.login-form :deep(.el-form-item__content) {
  width: 100%;
  display: block;
}
.login-form :deep(.el-input) {
  width: 100% !important;
  display: flex !important;
}
.login-form :deep(.el-input__wrapper) {
  width: 100%;
  padding: 4px 12px;
  box-sizing: border-box;
}
.login-form :deep(.el-input__inner) {
  width: 100%;
  font-size: 15px;
  height: 40px;
  line-height: 40px;
}
.input-icon {
  display: inline-flex;
  align-items: center;
  height: 100%;
  color: #6b7280;
  font-size: 16px;
}
.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 1px;
  margin-top: 8px;
}
.code-btn {
  width: 110px;
  color: #409eff;
  font-size: 13px;
}
.captcha-hint {
  font-size: 12px;
  color: #f59e0b;
  margin-top: 6px;
  line-height: 1.4;
}
.agreement-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  font-size: 12px;
  color: #6b7280;
}
.agreement-row :deep(.el-checkbox) {
  color: #6b7280;
}
.agreement-row :deep(.el-checkbox__label) {
  font-size: 12px;
  color: #6b7280;
}
.link {
  color: #409eff;
  cursor: pointer;
  text-decoration: none;
}
.link:hover {
  text-decoration: underline;
}
.forgot {
  flex-shrink: 0;
  margin-left: 12px;
}
.login-links {
  text-align: center;
  margin-top: 16px;
}
.login-links a {
  color: #409eff;
  text-decoration: none;
  font-size: 13px;
}
.third-party {
  padding: 8px 0 4px;
}
.third-party__hint {
  text-align: center;
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 16px;
}
.third-party__hint b {
  color: #111;
}
.third-party__qrcode {
  position: relative;
  width: 200px;
  height: 200px;
  margin: 0 auto 20px;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
}
.third-party__canvas {
  display: block;
  width: 200px;
  height: 200px;
}
.third-party__overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.7;
  text-align: center;
  transition: background 0.2s;
}
.third-party__overlay span {
  padding: 8px 14px;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  background: #fff;
}
.third-party__qrcode:hover .third-party__overlay {
  background: rgba(255, 255, 255, 0.85);
}
.third-party__list {
  display: flex;
  justify-content: space-around;
  gap: 16px;
  padding: 0 12px;
  border-top: 1px dashed #e5e7eb;
  padding-top: 16px;
}
.third-party__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 4px;
  border-radius: 8px;
  transition: background 0.2s;
}
.third-party__item:hover {
  background: #f3f4f6;
}
.third-party__item.is-active {
  background: #eef2ff;
}
.third-party__icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  transition: transform 0.2s;
}
.third-party__item.is-active .third-party__icon {
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
}
.third-party__icon--wechat {
  background: #07c160;
}
.third-party__icon--alipay {
  background: #1677ff;
}
.third-party__icon--huawei {
  background: #c7000b;
}
.third-party__label {
  font-size: 13px;
  color: #374151;
}
.third-party__item.is-active .third-party__label {
  color: #4f46e5;
  font-weight: 600;
}
</style>
