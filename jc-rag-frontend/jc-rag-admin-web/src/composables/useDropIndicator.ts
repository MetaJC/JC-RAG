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


import type { DropPosition } from './useDropPosition'

export interface IndicatorConfig {
  position: DropPosition
  rowRect: DOMRect
  lineLeft: number
  lineWidth: number
  boxLeft: number
  boxWidth: number
  boxHeight: number
}

export function useDropIndicator() {
  let indicatorLayer: HTMLElement | null = null
  let lineEl: HTMLElement | null = null
  let boxEl: HTMLElement | null = null

  function init(parent: HTMLElement) {
    cleanup()

    indicatorLayer = document.createElement('div')
    indicatorLayer.className = 'jc-dept-indicator-layer'
    if (window.getComputedStyle(parent).position === 'static') {
      parent.style.position = 'relative'
    }
    parent.appendChild(indicatorLayer)

    lineEl = document.createElement('div')
    lineEl.className = 'jc-dept-indicator-line'
    indicatorLayer.appendChild(lineEl)

    boxEl = document.createElement('div')
    boxEl.className = 'jc-dept-indicator-box'
    indicatorLayer.appendChild(boxEl)
  }

  function update(config: IndicatorConfig) {
    if (!indicatorLayer || !lineEl || !boxEl) return

    const { position, rowRect, lineLeft, lineWidth, boxLeft, boxWidth, boxHeight } = config
    const layerRect = indicatorLayer.getBoundingClientRect()

    if (position === 'before' || position === 'after') {
      const top = (position === 'before' ? rowRect.top : rowRect.bottom - 2) - layerRect.top
      lineEl.style.opacity = '1'
      boxEl.style.opacity = '0'
      lineEl.style.transform = `translate3d(${lineLeft - layerRect.left}px, ${top}px, 0)`
      lineEl.style.width = `${lineWidth}px`
      return
    }

    if (position === 'inner') {
      const top = rowRect.top - layerRect.top + Math.max((rowRect.height - boxHeight) / 2, 0)
      lineEl.style.opacity = '0'
      boxEl.style.opacity = '1'
      boxEl.style.transform = `translate3d(${boxLeft - layerRect.left}px, ${top}px, 0)`
      boxEl.style.width = `${boxWidth}px`
      boxEl.style.height = `${boxHeight}px`
      return
    }

    hide()
  }

  function hide() {
    if (!lineEl || !boxEl) return
    lineEl.style.opacity = '0'
    boxEl.style.opacity = '0'
  }

  function cleanup() {
    if (indicatorLayer && indicatorLayer.parentElement) {
      indicatorLayer.parentElement.removeChild(indicatorLayer)
    }
    indicatorLayer = null
    lineEl = null
    boxEl = null
  }

  return { init, update, hide, cleanup }
}
