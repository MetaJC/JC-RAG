export function useAutoExpand(delay = 600) {
  let timer: ReturnType<typeof setTimeout> | null = null
  let pendingTargetId: number | null = null

  function scheduleExpand(targetId: number, hasChildren: boolean, expanded: boolean, onExpand: (id: number) => void) {
    if (!hasChildren || expanded) {
      clear()
      return
    }

    if (pendingTargetId === targetId) return

    clear()
    pendingTargetId = targetId

    timer = setTimeout(() => {
      if (pendingTargetId === targetId) {
        onExpand(targetId)
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