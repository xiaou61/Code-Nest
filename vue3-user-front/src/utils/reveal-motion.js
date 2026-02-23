import { nextTick, onBeforeUnmount, onMounted } from 'vue'

export function useRevealMotion(selector = '.cn-learn-reveal') {
  let observer = null

  const disconnect = () => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  }

  const initObserver = () => {
    disconnect()

    const nodes = Array.from(document.querySelectorAll(selector))
    if (!nodes.length) return

    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reducedMotion) {
      nodes.forEach(node => node.classList.add('is-visible'))
      return
    }

    observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return
        entry.target.classList.add('is-visible')
        observer?.unobserve(entry.target)
      })
    }, {
      threshold: 0.2,
      rootMargin: '0px 0px -8% 0px'
    })

    nodes.forEach((node, index) => {
      if (!node.style.getPropertyValue('--reveal-delay')) {
        node.style.setProperty('--reveal-delay', `${Math.min(index * 70, 420)}ms`)
      }
      observer.observe(node)
    })
  }

  onMounted(async () => {
    await nextTick()
    initObserver()
  })

  onBeforeUnmount(() => {
    disconnect()
  })

  return {
    refreshReveal: initObserver
  }
}
