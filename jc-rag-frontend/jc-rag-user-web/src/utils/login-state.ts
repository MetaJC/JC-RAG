/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


/**
 * 登录页本地持久化状态: 键名常量 + 退出登录统一清理。
 * <p>localStorage: tab / 手机号 / 短信倒计时 (跨刷新恢复);
 * sessionStorage: 滑块通过态 / 会话就绪标记 (会话内恢复)。
 * <p>键名是 LoginView 与 ChatHomeView(退出清理) 的单一事实源, 禁止各处重复硬编码。
 */

/** 登录页当前 tab (account / code / third) */
export const LOGIN_TAB_STORAGE_KEY = 'jc_rag_login_tab'

/** 验证码登录手机号 */
export const PHONE_STORAGE_KEY = 'jc_rag_login_phone'

/** 短信发送倒计时 deadline 前缀, 完整键 = 前缀 + 手机号 */
export const SMS_COUNTDOWN_KEY_PREFIX = 'jc_rag_sms_cd_'

/** 滑块通过态 (token + deadline) 前缀, 完整键 = 前缀 + scope ('account:用户名' / 'code:手机号') */
export const CAPTCHA_STATE_PREFIX = 'jc_rag_captcha_state_'

/** 登录成功 → 会话 cookie 就绪标记, 供 ChatHomeView 判定"从登录来" */
export const SESSION_READY_KEY = 'jc_rag_session_ready'

/** 按前缀批量删除存储键 */
function removeByPrefix(storage: Storage, prefix: string) {
  const keys: string[] = []
  for (let i = 0; i < storage.length; i++) {
    const k = storage.key(i)
    if (k && k.startsWith(prefix)) keys.push(k)
  }
  keys.forEach(k => storage.removeItem(k))
}

/**
 * 清理登录页全部持久化状态 (退出登录时调用),
 * 保证退出后重新进入登录页是干净初始态: 滑块/倒计时/tab/手机号均不残留。
 */
export function clearLoginLocalState() {
  try {
    localStorage.removeItem(LOGIN_TAB_STORAGE_KEY)
    localStorage.removeItem(PHONE_STORAGE_KEY)
    removeByPrefix(localStorage, SMS_COUNTDOWN_KEY_PREFIX)
  } catch (e) {
    // 忽略隐私模式读写失败
  }
  try {
    sessionStorage.removeItem(SESSION_READY_KEY)
    removeByPrefix(sessionStorage, CAPTCHA_STATE_PREFIX)
  } catch (e) {
    // 忽略隐私模式读写失败
  }
}
