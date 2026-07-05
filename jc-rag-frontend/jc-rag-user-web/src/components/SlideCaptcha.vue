<template>
  <div class="slide-captcha" :class="{ 'is-pass': passed, 'is-drag': dragging, 'is-check': checking, 'is-fail': failed }">
    <div ref="trackEl" class="slide-captcha__track">
      <!-- 已拖动区域: GPU transform 跟随, 0 延迟 -->
      <div
        class="slide-captcha__progress"
        :class="{ 'is-show': dragging || passed }"
        :style="progressStyle"
      ></div>

      <!-- 流光层: 单条银色光带, 验证通过后隐藏 -->
      <div class="slide-captcha__stream">
        <span class="slide-captcha__beam"></span>
      </div>

      <!-- 提示文字 -->
      <div class="slide-captcha__text">
        <template v-if="failed">验证失败, 请重试</template>
        <template v-else-if="checking">验证中...</template>
        <template v-else-if="passed">验证通过</template>
        <template v-else>{{ hintText }}</template>
      </div>

      <!-- 拖动滑块 -->
      <div
        ref="knobEl"
        class="slide-captcha__knob"
        :class="{ 'is-active': dragging, 'is-pass': passed, 'is-fail': failed }"
        :style="{ left: knobX + 'px' }"
        draggable="false"
        @mousedown="onDragStart"
        @touchstart.prevent="onDragStart"
        @pointerdown="onDragStart"
        @dragstart.prevent
        @selectstart.prevent
      >
        <span class="slide-captcha__knob-grip" aria-hidden="true">
          <span></span><span></span><span></span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 滑动验证码 (用户端)
 *  - hintText: 提示文案
 *  - @change: { passed, captchaToken, challengeId }
 *
 * 修复点: 改为 mouse + touch + document 级监听 (与管理端一致),
 * 避免 pointer + setPointerCapture 在嵌套容器下失效导致无法拖动.
 */
const props = withDefaults(defineProps<{
  hintText?: string
}>(), {
  hintText: '按住滑块, 拖到最右边',
})

const emit = defineEmits<{
  (e: 'change', data: { passed: boolean; captchaToken?: string; challengeId?: string }): void
}>()

const trackEl = ref<HTMLElement | null>(null)
const trackWidth = ref(0)
const blockSize = 44

const knobX = ref(0)
const dragging = ref(false)
const passed = ref(false)
const failed = ref(false)
const checking = ref(false)

let challengeId = ''
let startClientX = 0
let startKnobX = 0
let dragStartTime = 0

const apiBase = (import.meta.env?.VITE_API_BASE as string) || '/api'

/** GPU 合成层: scaleX 跟随滑块, 0 延迟, 0 reflow */
const progressStyle = computed(() => {
  const max = Math.max(1, trackWidth.value - blockSize)
  const ratio = Math.min(1, Math.max(0, knobX.value / max))
  return { transform: `scaleX(${ratio})` }
})

/**
 * fetch 拿到的 json 是 R 包装 { code, message, data: {...} } 形态, 这里解包。
 * 兜底: 老接口或非 R 形态 (裸 Map) 时原样透传。
 */
function unwrap(json: any): any {
  if (json && typeof json === 'object' && 'code' in json && 'data' in json) {
    return json.data
  }
  return json
}

async function fetchChallenge() {
  try {
    const res = await fetch(`${apiBase}/auth/captcha/challenge`, { credentials: 'include' })
    const json = await res.json()
    const data = unwrap(json) ?? {}
    if (data.challengeId) {
      challengeId = data.challengeId
    }
  } catch (e) {
    console.warn('[captcha] 获取 challenge 失败, 继续允许本地校验', e)
  }
}

function updateTrackWidth() {
  if (!trackEl.value) return
  const newWidth = trackEl.value.offsetWidth
  if (newWidth > 0 && newWidth !== trackWidth.value) {
    trackWidth.value = newWidth
  }
}

function init() {
  knobX.value = 0
  passed.value = false
  failed.value = false
  checking.value = false
  challengeId = ''
  
  // 立即尝试获取尺寸
  updateTrackWidth()
  
  // 通过 nextTick 再次尝试，确保 DOM 已渲染
  Promise.resolve().then(() => {
    updateTrackWidth()
    if (!challengeId) {
      fetchChallenge()
    }
  })
}

function onDragStart(e: MouseEvent | TouchEvent | PointerEvent) {
  if (passed.value || checking.value) return
  
  // 确保 trackWidth 已正确初始化
  updateTrackWidth()
  if (trackWidth.value <= blockSize) {
    console.warn('[captcha] trackWidth 未正确初始化, 无法拖动')
    return
  }
  
  dragging.value = true
  failed.value = false
  const clientX = 'touches' in e ? e.touches[0]!.clientX :
                  'pointerType' in e ? e.clientX : e.clientX
  startClientX = clientX
  startKnobX = knobX.value
  dragStartTime = performance.now()
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('touchmove', onDragMove, { passive: false })
  document.addEventListener('pointermove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  document.addEventListener('touchend', onDragEnd)
  document.addEventListener('pointerup', onDragEnd)
  document.addEventListener('touchcancel', onDragEnd)
  document.addEventListener('pointercancel', onDragEnd)
}

function onDragMove(e: MouseEvent | TouchEvent | PointerEvent) {
  if (!dragging.value) return
  if ('touches' in e) e.preventDefault()
  const clientX = 'touches' in e ? e.touches[0]!.clientX :
                  'pointerType' in e ? e.clientX : e.clientX
  const delta = clientX - startClientX
  const max = Math.max(0, trackWidth.value - blockSize)
  knobX.value = Math.max(0, Math.min(max, startKnobX + delta))
}

async function onDragEnd() {
  if (!dragging.value) return
  dragging.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('touchmove', onDragMove)
  document.removeEventListener('pointermove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('touchend', onDragEnd)
  document.removeEventListener('pointerup', onDragEnd)
  document.removeEventListener('touchcancel', onDragEnd)
  document.removeEventListener('pointercancel', onDragEnd)

  const durationMs = Math.round(performance.now() - dragStartTime)
  const max = Math.max(0, trackWidth.value - blockSize)
  const reachThreshold = max * 0.98

  if (knobX.value < reachThreshold) {
    knobX.value = 0
    failed.value = true
    emit('change', { passed: false })
    setTimeout(() => { if (!passed.value) failed.value = false }, 1500)
    return
  }

  knobX.value = max
  checking.value = true
  try {
    if (!challengeId) {
      await fetchChallenge()
      if (!challengeId) throw new Error('challengeId 获取失败')
    }
    const res = await fetch(`${apiBase}/auth/captcha/check`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ challengeId, durationMs, trackSize: trackWidth.value }),
    })
    const json = await res.json().catch(() => ({}))
    if (!res.ok) throw new Error(`HTTP ${res.status} ${json?.message || ''}`)
    const data = unwrap(json) ?? {}
    if (!data.success) throw new Error(data.message || '校验失败')
    passed.value = true
    checking.value = false
    emit('change', { passed: true, captchaToken: data.captchaToken, challengeId })
  } catch (err: any) {
    console.error('[captcha] 校验失败', err)
    knobX.value = 0
    failed.value = true
    checking.value = false
    challengeId = ''  // 清除过期的 challengeId，下次拖动时重新获取
    emit('change', { passed: false })
    setTimeout(() => { if (!passed.value) failed.value = false }, 2000)
  }
}

onMounted(init)
onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('touchmove', onDragMove)
  document.removeEventListener('pointermove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('touchend', onDragEnd)
  document.removeEventListener('pointerup', onDragEnd)
  document.removeEventListener('touchcancel', onDragEnd)
  document.removeEventListener('pointercancel', onDragEnd)
})

const reset = () => init()
defineExpose({ reset })
</script>

<style scoped>
.slide-captcha {
  width: 100%;
  user-select: none;
  -webkit-user-select: none;
}
.slide-captcha__track {
  position: relative;
  width: 100%;
  height: 44px;
  border-radius: 22px;
  overflow: hidden;
  background: linear-gradient(90deg, #f8fafc 0%, #f1f5f9 50%, #f8fafc 100%);
  touch-action: none;
  border: 1px solid #cbd5e1;
}
.slide-captcha__stream {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 1;
}
.slide-captcha__beam {
  position: absolute;
  top: 0;
  left: -55%;
  width: 55%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(203, 213, 225, 0) 6%,
    rgba(226, 232, 240, 0.25) 20%,
    rgba(241, 245, 249, 0.5) 38%,
    rgba(255, 255, 255, 0.6) 50%,
    rgba(241, 245, 249, 0.5) 62%,
    rgba(226, 232, 240, 0.25) 80%,
    rgba(203, 213, 225, 0) 94%,
    transparent 100%
  );
  filter: blur(3px);
  animation: slide-stream 2.5s linear infinite;
  will-change: transform;
}
@keyframes slide-stream {
  0%   { transform: translateX(0); }
  100% { transform: translateX(355%); }
}

/* 已拖动区域: GPU transform 跟随, 0 延迟 */
.slide-captcha__progress {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, #ecfdf5 0%, #d1fae5 100%);
  pointer-events: none;
  z-index: 0;
  transform: scaleX(0);
  transform-origin: left center;
  will-change: transform;
  opacity: 0;
  transition: opacity 0.2s ease;
}
.slide-captcha__progress.is-show { opacity: 1; }

/* 通过后: 纯色浅绿, 隐藏流光, 关闭所有动效 */
.slide-captcha.is-pass .slide-captcha__track {
  background: #d1fae5;
  border-color: #10b981;
}
.slide-captcha.is-pass .slide-captcha__stream { display: none; }
.slide-captcha.is-pass .slide-captcha__progress {
  background: #d1fae5;
  opacity: 1;
  transform: scaleX(1);
}

/* 失败: 红色态保留流光提示, 1.5s 后自动消失 */
.slide-captcha.is-fail .slide-captcha__track {
  background: #fee2e2;
  border-color: #f87171;
}
.slide-captcha.is-fail .slide-captcha__stream { display: none; }

/* 提示文字 */
.slide-captcha__text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #475569;
  font-weight: 500;
  letter-spacing: 0.5px;
  pointer-events: none;
  z-index: 2;
}
.slide-captcha.is-pass .slide-captcha__text { color: #047857; font-weight: 600; }
.slide-captcha.is-fail .slide-captcha__text { color: #b91c1c; font-weight: 600; }

/* 滑块 */
.slide-captcha__knob {
  position: absolute;
  top: 0;
  left: 0;
  width: 44px;
  height: 44px;
  background: linear-gradient(180deg, #ffffff 0%, #e2e8f0 100%);
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.8);
  z-index: 5;
  border: 1px solid #cbd5e1;
  touch-action: none;
  user-select: none;
  -webkit-user-select: none;
  -webkit-user-drag: none;
  transition: box-shadow 0.15s, background 0.2s, border-color 0.2s;
}
.slide-captcha__knob:hover {
  background: linear-gradient(180deg, #ffffff 0%, #f1f5f9 100%);
}
.slide-captcha__knob.is-active {
  cursor: grabbing;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}
.slide-captcha__knob.is-pass {
  background: linear-gradient(180deg, #10b981 0%, #047857 100%);
  border-color: #047857;
}
.slide-captcha__knob.is-fail {
  background: linear-gradient(180deg, #fca5a5 0%, #dc2626 100%);
  border-color: #b91c1c;
}
.slide-captcha__knob-grip {
  display: inline-flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.slide-captcha__knob-grip span {
  display: block;
  width: 14px;
  height: 2px;
  background: #475569;
  border-radius: 1px;
  transition: background 0.2s;
}
.slide-captcha__knob.is-pass .slide-captcha__knob-grip span { background: #fff; }
.slide-captcha__knob.is-fail .slide-captcha__knob-grip span { background: #fff; }
</style>
