export type DropPosition = 'before' | 'after' | 'inner' | null

export interface DropPositionResult {
  position: DropPosition
  targetId: number | null
  level: number
  indentPx: number
  illegal: boolean
}

export function useDropPosition(deadZone = 8) {
  let lastTargetId: number | null = null
  let lastPosition: DropPosition = null

  function calculatePosition(target: HTMLElement, mouseY: number, level: number, insideAllowed: boolean): DropPositionResult {
    const rect = target.getBoundingClientRect()
    const height = rect.height || 1
    const mouseOffset = mouseY - rect.top

    const zoneTop = height * 0.25
    const zoneBottom = height * 0.75

    let position: DropPosition
    if (mouseOffset <= zoneTop) {
      position = 'before'
    } else if (mouseOffset >= zoneBottom) {
      position = 'after'
    } else {
      position = insideAllowed ? 'inner' : (mouseOffset < height / 2 ? 'before' : 'after')
    }

    const targetId = parseInt(target.getAttribute('data-row-key') || '0', 10) || null

    if (lastTargetId === targetId && lastPosition !== null) {
      if (lastPosition === 'before' && position === 'inner') {
        if (mouseOffset < zoneTop + deadZone) {
          position = 'before'
        }
      } else if (lastPosition === 'inner' && position === 'before') {
        if (mouseOffset > zoneTop - deadZone) {
          position = 'inner'
        }
      } else if (lastPosition === 'inner' && position === 'after') {
        if (mouseOffset < zoneBottom + deadZone) {
          position = 'inner'
        }
      } else if (lastPosition === 'after' && position === 'inner') {
        if (mouseOffset > zoneBottom - deadZone) {
          position = 'after'
        }
      }
    }

    lastTargetId = targetId
    lastPosition = position

    const finalLevel = position === 'inner' ? level + 1 : level
    const indentPx = finalLevel * 24

    return {
      position,
      targetId,
      level: finalLevel,
      indentPx,
      illegal: false,
    }
  }

  function reset() {
    lastTargetId = null
    lastPosition = null
  }

  return { calculatePosition, reset }
}