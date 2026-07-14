export function useAutoScroll(edgeThreshold = 50, maxSpeed = 20) {
  let rafId: number | null = null
  let scrollContainer: HTMLElement | null = null
  let targetMouseY = 0
  let onScrollFrame: (() => void) | null = null

  function start(container: HTMLElement, onFrame?: () => void) {
    stop()
    scrollContainer = container
    onScrollFrame = onFrame ?? null
    animate()
  }

  function update(mouseY: number) {
    targetMouseY = mouseY
  }

  function animate() {
    if (!scrollContainer) return

    const rect = scrollContainer.getBoundingClientRect()
    const containerTop = rect.top
    const containerBottom = rect.bottom

    const distanceToTop = targetMouseY - containerTop
    const distanceToBottom = containerBottom - targetMouseY

    let scrollSpeed = 0

    if (distanceToTop < edgeThreshold) {
      const ratio = 1 - distanceToTop / edgeThreshold
      scrollSpeed = -ratio * maxSpeed
    } else if (distanceToBottom < edgeThreshold) {
      const ratio = 1 - distanceToBottom / edgeThreshold
      scrollSpeed = ratio * maxSpeed
    }

    if (scrollSpeed !== 0) {
      scrollContainer.scrollTop += scrollSpeed
      onScrollFrame?.()
    }

    rafId = requestAnimationFrame(animate)
  }

  function stop() {
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = null
    }
    onScrollFrame = null
    scrollContainer = null
  }

  return { start, update, stop }
}
