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
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock, Message, User } from '@element-plus/icons-vue'
import { userApi } from '../../api/user'
import SlideCaptcha from '../../components/SlideCaptcha.vue'
import {
  CAPTCHA_STATE_PREFIX, LOGIN_TAB_STORAGE_KEY, PHONE_STORAGE_KEY,
  SMS_COUNTDOWN_KEY_PREFIX,
} from '../../utils/login-state'

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
// 验证码 tab 独立滑块实例 (与账号 tab 的 captchaRef 分离, 避免互相重置)
const codeCaptchaRef = ref<InstanceType<typeof SlideCaptcha> | null>(null)

// ===== 站点配置: 注册开关 / 协议勾选强制性 / 品牌信息 (后台系统设置页维护) =====
const registerOpen = ref(true)
const loginAgreeRequired = ref(true)
const systemName = ref('JC RAG')
// 验证码位数: 由发送回执 codeLength 决定, 默认 4 位; 前端据此判断输入到第几位自动提交
const expectedCodeLen = ref(4)

// ===== 验证码登录: 手机号实时校验 =====
const phoneTouched = ref(false)
const phoneValid = computed(() => /^1[3-9]\d{9}$/.test(codeForm.phone))
const phoneError = computed(() => phoneTouched.value && codeForm.phone.length > 0 && !phoneValid.value)
// 验证码输入区默认隐藏, 滑块通过后平滑展开
const codeBlockVisible = ref(false)

// ===== 倒计时持久化: localStorage 存 deadline 时间戳, 页面刷新后继续读秒 (键名见 utils/login-state) =====
let countdownTimer: ReturnType<typeof setInterval> | null = null

const smsCountdownKey = () => SMS_COUNTDOWN_KEY_PREFIX + codeForm.phone

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

/** 读取 localStorage 中的 deadline, 刷新 codeCountdown; 归零时清理缓存 */
function tickCountdown() {
  let deadline = 0
  try {
    deadline = Number(localStorage.getItem(smsCountdownKey()) || 0)
  } catch (e) {
    deadline = 0
  }
  const remain = Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
  const prev = codeCountdown.value
  codeCountdown.value = remain
  if (remain <= 0) {
    stopCountdown()
    try {
      localStorage.removeItem(smsCountdownKey())
    } catch (e) {
      // 忽略隐私模式写入失败
    }
    // 倒计时自然归零 (prev>0 跳变): 自动重置滑块, 再次发送需重新滑动 (token 一次性消费语义)
    if (prev > 0) resetCodeCaptcha()
  }
}

/** 以后端返回的秒数写入 deadline 并启动读秒 (与后端 Redis 限流 TTL 对齐) */
function startCountdown(seconds: number) {
  const deadline = Date.now() + seconds * 1000
  try {
    localStorage.setItem(smsCountdownKey(), String(deadline))
  } catch (e) {
    // 忽略隐私模式写入失败
  }
  tickCountdown()
  stopCountdown()
  countdownTimer = setInterval(tickCountdown, 1000)
}

/** 进入页面/切换手机号时恢复倒计时 (刷新不丢) */
function restoreCountdown() {
  if (!codeForm.phone) {
    codeCountdown.value = 0
    return
  }
  tickCountdown()
  if (codeCountdown.value > 0 && !countdownTimer) {
    countdownTimer = setInterval(tickCountdown, 1000)
  }
}

/**
 * 重置验证码 tab 滑块 (清除通过态与持久化缓存)。
 * <p>重置时机仅限: 倒计时归零 / 发送失败 / 登录成功。
 * 发送成功不重置 (保持验证通过态, 与倒计时强绑定), tab 切换也不重置。
 */
function resetCodeCaptcha() {
  captchaPassed.value = false
  captchaToken.value = ''
  codeCaptchaRef.value?.reset()
  clearCaptchaState('code:' + codeForm.phone)
}

/**
 * 按当前手机号的短信倒计时同步滑块状态 (强绑定):
 * 倒计时进行中且存在持久化通过记录 → 恢复通过态与验证码输入区;
 * 否则 → 重置滑块并收起验证码输入区。
 * <p>手机号变化与页面刷新恢复时调用。
 */
function syncCodeCaptchaWithCountdown() {
  if (codeForm.phone && phoneValid.value && codeCountdown.value > 0) {
    const token = loadCaptchaState('code:' + codeForm.phone)
    if (token) {
      captchaPassed.value = true
      captchaToken.value = token
      codeBlockVisible.value = true
      nextTick(() => codeCaptchaRef.value?.restorePassed())
      return
    }
  }
  resetCodeCaptcha()
  codeBlockVisible.value = false
}

// ===== 登录页状态持久化: tab / 手机号刷新不丢 (localStorage, 键名见 utils/login-state) =====

// ===== 滑块验证通过态持久化: sessionStorage 存 token + deadline, 与后端 token 5 分钟有效期对齐, 刷新不丢 =====
const CAPTCHA_STATE_TTL_MS = 5 * 60 * 1000

/** 保存滑块通过 token (scopeKey 形如 'account:用户名' / 'code:手机号') */
function saveCaptchaState(scopeKey: string, token: string) {
  try {
    sessionStorage.setItem(CAPTCHA_STATE_PREFIX + scopeKey,
      JSON.stringify({ token, deadline: Date.now() + CAPTCHA_STATE_TTL_MS }))
  } catch (e) {
    // 忽略隐私模式写入失败
  }
}

/** 读取滑块通过 token; 过期自动清理并返回空串 */
function loadCaptchaState(scopeKey: string): string {
  try {
    const raw = sessionStorage.getItem(CAPTCHA_STATE_PREFIX + scopeKey)
    if (!raw) return ''
    const state = JSON.parse(raw) as { token?: string; deadline?: number }
    if (!state.token || !state.deadline || Date.now() > state.deadline) {
      sessionStorage.removeItem(CAPTCHA_STATE_PREFIX + scopeKey)
      return ''
    }
    return state.token
  } catch (e) {
    return ''
  }
}

function clearCaptchaState(scopeKey: string) {
  try {
    sessionStorage.removeItem(CAPTCHA_STATE_PREFIX + scopeKey)
  } catch (e) {
    // 忽略隐私模式写入失败
  }
}

// 连续失败计数: 后端告知是否需要滑动验证
const failCount = ref(0)
const needCaptcha = ref(false)

/**
 * 查询后端登录失败次数, 决定是否展示滑块验证, 并恢复本会话已有的通过状态。
 * 失败次数由后端持久化: 达阈值保持出现, 仅登录成功清零。
 */
async function fetchCaptchaStatus(username: string) {
  if (!username) {
    failCount.value = 0
    needCaptcha.value = false
    return
  }
  try {
    const res = await userApi.captchaStatus(username) as { needCaptcha?: boolean; failCount?: number }
    failCount.value = res?.failCount || 0
    needCaptcha.value = !!res?.needCaptcha
  } catch (e) {
    console.warn('[LoginView] captchaStatus 查询失败:', (e as any)?.message)
    return
  }
  if (needCaptcha.value) {
    // 达阈值: 刷新后从会话存储恢复通过态, 避免重复滑动
    const token = loadCaptchaState('account:' + username)
    if (token) {
      captchaPassed.value = true
      captchaToken.value = token
      nextTick(() => captchaRef.value?.restorePassed())
    } else {
      captchaPassed.value = false
      captchaToken.value = ''
    }
  } else {
    captchaPassed.value = false
    captchaToken.value = ''
  }
}

// 协议勾选 (账号密码 tab)
const accountAgree = ref(false)

// 手机号变化: 实时标记 touched + 持久化 + 恢复倒计时 + 按新手机号的倒计时同步滑块状态
watch(() => codeForm.phone, () => {
  phoneTouched.value = true
  try {
    localStorage.setItem(PHONE_STORAGE_KEY, codeForm.phone)
  } catch (e) {
    // 忽略隐私模式写入失败
  }
  // 验证码与旧手机号绑定, 切换后清空防止对新手机号误触发自动登录
  codeForm.code = ''
  restoreCountdown()
  syncCodeCaptchaWithCountdown()
})

// 用户名变化: 防抖查询后端失败次数, 刷新滑块展示状态
let usernameQueryTimer: ReturnType<typeof setTimeout> | null = null
watch(() => accountForm.username, () => {
  if (usernameQueryTimer) clearTimeout(usernameQueryTimer)
  usernameQueryTimer = setTimeout(() => fetchCaptchaStatus(accountForm.username), 400)
})

// 验证码输入到后端下发的位数时自动登录; 协议强制开启时需已勾选才触发
watch(() => codeForm.code, (val) => {
  if (!val || val.length < expectedCodeLen.value) return
  if (!phoneValid.value || loading.value) return
  if (loginAgreeRequired.value && !codeForm.agree) return
  onCodeLogin()
})

/** 手机号无效时遮罩拦截滑块, 点击提示 */
function onCaptchaGuardClick() {
  if (!phoneValid.value) {
    ElMessage.warning('请输入正确的手机号')
  }
}

function onCaptchaChange(data: { passed: boolean; captchaToken?: string }) {
  captchaPassed.value = data.passed
  captchaToken.value = data.captchaToken || ''
  if (data.passed) {
    saveCaptchaState('account:' + accountForm.username, data.captchaToken || '')
  } else {
    clearCaptchaState('account:' + accountForm.username)
  }
}

/** 验证码 tab 滑块通过: 展开验证码输入区, 首次自动发送短信 */
function onCodeCaptchaChange(data: { passed: boolean; captchaToken?: string }) {
  if (!data.passed) {
    captchaPassed.value = false
    captchaToken.value = ''
    clearCaptchaState('code:' + codeForm.phone)
    return
  }
  if (!phoneValid.value) {
    // 兜底: 正常路径已被 captcha-guard 遮罩拦截
    ElMessage.warning('请输入正确的手机号')
    codeCaptchaRef.value?.reset()
    return
  }
  captchaPassed.value = true
  captchaToken.value = data.captchaToken || ''
  saveCaptchaState('code:' + codeForm.phone, captchaToken.value)
  codeBlockVisible.value = true
  // 首次通过自动获取验证码; 倒计时中说明已发过, 仅展示不重复发送
  if (codeCountdown.value === 0 && !codeLoading.value) {
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
  if (loginAgreeRequired.value && !accountAgree.value) {
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
      clearCaptchaState('account:' + accountForm.username)
      return
    }
    if (res && typeof res === 'object') {
      if (typeof res.failCount === 'number') failCount.value = res.failCount
      if (typeof res.needCaptcha === 'boolean') needCaptcha.value = res.needCaptcha
    }
    ElMessage.success('登录成功')
    // 登录成功: 后端已清零失败次数, 同步重置滑块并清除通过态缓存, 保证退出后再进登录页是干净初始态
    captchaPassed.value = false
    captchaToken.value = ''
    captchaRef.value?.reset()
    clearCaptchaState('account:' + accountForm.username)
    console.log('[Login] success, verifying session before navigation, pathname:', window.location.pathname)
    // 会话就绪验证合并到下方轮询循环统一执行 (此前此处先验一次、finally 后再轮询, 重复 2 次 currentUser)
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
    clearCaptchaState('account:' + accountForm.username)
    return
  } finally {
    loading.value = false
  }
  // 主动验证 session cookie 已生效: 调用 currentUser, 拿不到 user.authenticated=true 就等
  // 解决"已登录浏览器残留旧 cookie"导致的 ChatHomeView 首次 401
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
  await router.push('/chat').catch(err => console.error('[LoginView] router.push error:', err))
}

async function sendCode() {
  if (!phoneValid.value) {
    ElMessage.warning('请输入正确的手机号'); return
  }
  if (!captchaPassed.value || !captchaToken.value) {
    ElMessage.warning('请先完成滑动验证'); return
  }
  codeLoading.value = true
  try {
    const res = await userApi.sendSmsCode({ phone: codeForm.phone, captchaToken: captchaToken.value }) as
      { sent?: boolean; waitSeconds?: number; intervalSeconds?: number; codeLength?: number }
    if (res && res.sent === false) {
      // 命中后端限流: 用后端剩余秒数同步倒计时, 保证前后端一致
      startCountdown(res.waitSeconds || 60)
      ElMessage.info(`发送过于频繁, 请 ${codeCountdown.value}s 后再试`)
    } else {
      // 后端回执告知验证码位数, 前端据此判断输入到第几位自动登录
      if (typeof res?.codeLength === 'number' && res.codeLength > 0) {
        expectedCodeLen.value = res.codeLength
      }
      startCountdown(res?.intervalSeconds || 60)
      ElMessage.success('验证码已发送')
    }
  } catch (e: any) {
    console.error('[LoginView] sendSmsCode failed:', e?.message)
    // http 拦截器已透传后端业务 message (如"请先完成滑动验证")
    ElMessage.error(e?.message || '验证码发送失败, 请稍后重试')
    // 发送失败: 后端可能已消费 token 且未启动倒计时, 重置滑块要求重新滑动
    resetCodeCaptcha()
  } finally {
    codeLoading.value = false
    // 发送成功/限流不重置滑块: 保持验证通过态与倒计时强绑定,
    // 仅倒计时归零 (tickCountdown) 或登录成功 (onCodeLogin) 时才重置
  }
}

async function onSendCode() {
  sendCode()
}

/** 验证码登录: 未注册手机号由后端自动注册, 登录/注册一体 */
async function onCodeLogin() {
  if (!codeForm.phone || !phoneValid.value) {
    ElMessage.warning('请输入正确的手机号'); return
  }
  if (!codeForm.code) {
    ElMessage.warning('请输入验证码'); return
  }
  if (loginAgreeRequired.value && !codeForm.agree) {
    ElMessage.warning('请先勾选并同意《用户协议》与《隐私政策》'); return
  }
  loading.value = true
  try {
    const res = await userApi.smsLogin({ phone: codeForm.phone, code: codeForm.code }) as
      { success?: boolean; message?: string }
    if (res && res.success === false) {
      ElMessage.error(res.message || '验证码错误或已过期')
      return
    }
    // 自动注册时后端返回 "已自动注册并登录"
    ElMessage.success(res?.message || '登录成功')
    // 登录成功: 重置滑块并清除通过态缓存, 保证退出后再进登录页是干净初始态
    resetCodeCaptcha()
    codeForm.code = ''
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败, 请重试')
    return
  } finally {
    loading.value = false
  }
  // 与账号登录一致: 轮询确认 session cookie 已生效再跳转, 避免 ChatHomeView 首次 401
  let verified = false
  for (let i = 0; i < 10; i++) {
    try {
      const u = await userApi.currentUser()
      if (u && u.authenticated) {
        verified = true
        break
      }
    } catch (e) {
      console.warn('[LoginView] sms session verify attempt', i, 'failed')
    }
    await new Promise(resolve => setTimeout(resolve, 200))
  }
  if (!verified) {
    ElMessage.error('登录态验证失败, 请重试')
    return
  }
  await router.push('/chat').catch(err => console.error('[LoginView] router.push error:', err))
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
  try {
    localStorage.setItem(LOGIN_TAB_STORAGE_KEY, tab)
  } catch (e) {
    // 忽略隐私模式写入失败
  }
  if (tab === 'third') drawQrCode()
})
onMounted(async () => {
  // 站点配置: 控制注册入口显隐与协议复选框强制性 (失败不阻断, 用默认值)
  userApi.siteConfig().then((cfg: { registerOpen?: boolean; loginAgreeRequired?: boolean; systemName?: string }) => {
    registerOpen.value = cfg?.registerOpen !== false
    loginAgreeRequired.value = cfg?.loginAgreeRequired !== false
    if (cfg?.systemName) systemName.value = cfg.systemName
  }).catch(() => { /* 配置加载失败按默认值: 注册开放/协议必选 */ })
  // 刷新恢复: 当前 tab → 手机号 → 倒计时 → 滑块通过态
  try {
    const savedTab = localStorage.getItem(LOGIN_TAB_STORAGE_KEY)
    if (savedTab === 'account' || savedTab === 'code' || savedTab === 'third') {
      activeTab.value = savedTab
    }
  } catch (e) {
    // 忽略隐私模式读取失败
  }
  try {
    const savedPhone = localStorage.getItem(PHONE_STORAGE_KEY)
    if (savedPhone) codeForm.phone = savedPhone
  } catch (e) {
    // 忽略隐私模式读取失败
  }
  // 等手机号 watch 先执行完 (恢复倒计时), 再同步滑块通过态, 避免被 watch 重置
  await nextTick()
  restoreCountdown()
  // 账号 tab: 查询后端失败次数决定是否展示滑块, 有通过态则恢复
  fetchCaptchaStatus(accountForm.username)
  // 验证码 tab: 滑块状态与倒计时强绑定, 仅倒计时进行中才恢复通过态
  syncCodeCaptchaWithCountdown()
  if (activeTab.value === 'third') drawQrCode()
})
onBeforeUnmount(() => {
  stopCountdown()
  if (usernameQueryTimer) clearTimeout(usernameQueryTimer)
})
// 切换 tab 不重置滑块: tab 面板不销毁, 视觉状态自然保留;
// 滑块通过态与短信倒计时强绑定, 仅登录成功 / 倒计时到期才重置
</script>

<template>
  <div class="login-wrapper">
    <el-card class="login-card" shadow="never">
      <h2>用户登录</h2>
      <p class="login-desc">{{ systemName }} 智能问答系统</p>

      <el-tabs v-model="activeTab" class="login-tabs">
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
            <transition name="code-block">
              <el-form-item v-if="needCaptcha" label="安全验证">
                <SlideCaptcha ref="captchaRef" @change="onCaptchaChange" />
                <div class="captcha-hint">连续登录失败 {{ failCount }} 次, 请完成滑动验证</div>
              </el-form-item>
            </transition>
            <el-button type="primary" :loading="loading" class="login-btn" @click="onAccountLogin">登录</el-button>
            <div v-if="loginAgreeRequired" class="agreement-row">
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
              <el-input
                v-model="codeForm.phone"
                placeholder="请输入手机号"
                maxlength="11"
                :class="{ 'phone-invalid': phoneError }"
                @blur="phoneTouched = true"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Iphone /></el-icon>
                </template>
              </el-input>
              <div v-if="phoneError" class="phone-error-msg">请输入正确的手机号</div>
            </el-form-item>
            <el-form-item label="安全验证">
              <div class="captcha-wrap">
                <SlideCaptcha ref="codeCaptchaRef" @change="onCodeCaptchaChange" />
                <!-- 手机号格式不正确时拦截滑块拖动 -->
                <div
                  v-if="!phoneValid"
                  class="captcha-guard"
                  @click="onCaptchaGuardClick"
                  @mousedown.prevent
                  @touchstart.prevent="onCaptchaGuardClick"
                ></div>
              </div>
              <div class="captcha-hint">完成滑动验证后将自动发送短信验证码</div>
            </el-form-item>
            <transition name="code-block">
              <div v-if="codeBlockVisible" class="code-block">
                <el-form-item label="验证码">
                  <el-input
                    v-model="codeForm.code"
                    :placeholder="`请输入 ${expectedCodeLen} 位短信验证码`"
                    :maxlength="expectedCodeLen"
                  >
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
              </div>
            </transition>
            <el-button type="primary" :loading="loading" class="login-btn" @click="onCodeLogin">{{ registerOpen ? '登录 / 注册' : '登录' }}</el-button>
            <div class="agreement-row">
              <el-checkbox v-if="loginAgreeRequired" v-model="codeForm.agree">
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

      <div v-if="registerOpen" class="login-links">
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
/* ===== 验证码登录: 手机号校验 / 滑块门禁 / 验证码区展开动效 ===== */
.phone-invalid :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #f56c6c inset !important;
}
.phone-error-msg {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.4;
}
.captcha-wrap {
  position: relative;
  width: 100%;
}
.captcha-guard {
  position: absolute;
  inset: 0;
  z-index: 6;
  cursor: not-allowed;
  border-radius: 22px;
  background: rgba(248, 250, 252, 0.45);
}
.code-block {
  overflow: hidden;
}
.code-block-enter-active,
.code-block-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease, max-height 0.35s ease;
  overflow: hidden;
}
.code-block-enter-from,
.code-block-leave-to {
  opacity: 0;
  transform: translateY(-8px);
  max-height: 0;
}
.code-block-enter-to,
.code-block-leave-from {
  opacity: 1;
  max-height: 180px;
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
