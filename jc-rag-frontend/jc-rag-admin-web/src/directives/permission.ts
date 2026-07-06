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
