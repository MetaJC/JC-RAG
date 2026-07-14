export type DropPosition = 'before' | 'after' | 'inner' | null

export interface DropPositionResult {
  position: DropPosition
  targetId: number | null
  level: number
  illegal: boolean
}

export function useDropPosition(deadZone = 8) {
  let lastTargetId: number | null = null
  let lastPosition: DropPosition = null

  function calculatePosition(target: HTMLElement, mouseY: number, level: number, insideAllowed = true): DropPositionResult {
    const rect = target.getBoundingClientRect()
    const height = rect.height || 1
    const mouseOffset = mouseY - rect.top

    const topZoneEnd = height * 0.25
    const bottomZoneStart = height * 0.75
    const middleLine = height / 2

    let position: DropPosition
    if (mouseOffset <= topZoneEnd) {
      position = 'before'
    } else if (mouseOffset >= bottomZoneStart) {
      position = 'after'
    } else {
      position = insideAllowed ? 'inner' : mouseOffset < middleLine ? 'before' : 'after'
    }

    const match = target.className.match(/dept-row-(\d+)/)
    const targetId = match ? Number(match[1]) : null

    if (lastTargetId === targetId && lastPosition !== null) {
      if (lastPosition === 'before' && position === 'inner') {
        if (mouseOffset <= topZoneEnd + deadZone) {
          position = 'before'
        }
      } else if (lastPosition === 'inner' && position === 'before') {
        if (mouseOffset >= topZoneEnd - deadZone) {
          position = 'inner'
        }
      } else if (lastPosition === 'inner' && position === 'after') {
        if (mouseOffset <= bottomZoneStart + deadZone) {
          position = 'inner'
        }
      } else if (lastPosition === 'after' && position === 'inner') {
        if (mouseOffset >= bottomZoneStart - deadZone) {
          position = 'after'
        }
      }
    }

    lastTargetId = targetId
    lastPosition = position

    const finalLevel = position === 'inner' ? level + 1 : level

    return {
      position,
      targetId,
      level: finalLevel,
      illegal: false,
    }
  }

  function reset() {
    lastTargetId = null
    lastPosition = null
  }

  return { calculatePosition, reset }
}
