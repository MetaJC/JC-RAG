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


import type { Directive, DirectiveBinding } from 'vue'

import { useAuthStore } from '../stores/auth'

/**
 * v-permission 指令：基于 permCode 控制 DOM 显隐。
 * <p>用法：
 * <pre>
 *   v-permission="'sys:user:list'"          // 单个
 *   v-permission="['sys:user:edit','sys:user:delete']"  // 任一命中
 * </pre>
 * <p>不满足时移除 DOM 元素（display:none 仍占位；移除更彻底）。
 *
 * @author JC
 */
const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    apply(el, binding)
  },
  updated(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    apply(el, binding)
  },
}

function apply(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
  const authStore = useAuthStore()
  const ok = authStore.hasPerm(binding.value)
  if (!ok && el.parentNode) {
    el.parentNode.removeChild(el)
  }
}

export default permission
