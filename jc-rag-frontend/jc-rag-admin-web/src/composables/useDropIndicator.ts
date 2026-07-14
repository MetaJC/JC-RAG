import type { DropPosition } from './useDropPosition'

export interface IndicatorConfig {
  position: DropPosition
  targetRect: DOMRect
  indentPx: number
  illegal: boolean
}

export function useDropIndicator() {
  let indicatorLayer: HTMLElement | null = null
  let lineEl: HTMLElement | null = null
  let boxEl: HTMLElement | null = null

  function init(parent: HTMLElement) {
    cleanup()

    indicatorLayer = document.createElement('div')
    indicatorLayer.className = 'jc-dept-indicator-layer'
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

    const { position, targetRect, indentPx, illegal } = config

    lineEl.style.display = position === 'before' || position === 'after' ? 'block' : 'none'
    boxEl.style.display = position === 'inner' ? 'block' : 'none'

    if (illegal) {
      lineEl.classList.add('jc-dept-indicator-illegal')
      boxEl.classList.add('jc-dept-indicator-illegal')
    } else {
      lineEl.classList.remove('jc-dept-indicator-illegal')
      boxEl.classList.remove('jc-dept-indicator-illegal')
    }

    const layerRect = indicatorLayer.getBoundingClientRect()
    const scrollLeft = indicatorLayer.parentElement?.scrollLeft || 0

    if (position === 'before' || position === 'after') {
      const top = (position === 'before' ? targetRect.top : targetRect.bottom - 2) - layerRect.top
      lineEl.style.transform = `translate(${indentPx - scrollLeft}px, ${top}px)`
      lineEl.style.width = `${targetRect.width - indentPx - 24}px`
    } else if (position === 'inner') {
      const top = targetRect.top - layerRect.top
      boxEl.style.transform = `translate(${indentPx - 24 - scrollLeft}px, ${top}px)`
      boxEl.style.width = `${targetRect.width - indentPx + 24}px`
      boxEl.style.height = `${targetRect.height}px`
    }
  }

  function hide() {
    if (!lineEl || !boxEl) return
    lineEl.style.display = 'none'
    boxEl.style.display = 'none'
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