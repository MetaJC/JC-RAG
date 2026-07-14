export function useAutoExpand(delay = 700) {
  let timer: ReturnType<typeof setTimeout> | null = null
  let pendingTargetId: number | null = null

  function scheduleExpand(targetId: number, enabled: boolean, onExpand: () => void) {
    if (!enabled) {
      clear()
      return
    }

    if (pendingTargetId === targetId) return

    clear()
    pendingTargetId = targetId

    timer = setTimeout(() => {
      if (pendingTargetId === targetId) {
        onExpand()
      }
      clear()
    }, delay)
  }

  function clear() {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    pendingTargetId = null
  }

  return { scheduleExpand, clear }
}
