import { onBeforeUnmount } from 'vue'

function supportsReducedMotion() {
  return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export function useHomeMotion(visibleSections) {
  let sectionObserver = null
  let heroElement = null
  let parallaxFrame = 0
  let counterFrame = 0
  let removeMouseMove = null
  let removeMouseLeave = null

  const setAllVisible = () => {
    Object.keys(visibleSections.value).forEach((key) => {
      visibleSections.value[key] = true
    })
  }

  const observeSections = () => {
    if (supportsReducedMotion() || !('IntersectionObserver' in window)) {
      setAllVisible()
      return
    }

    sectionObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) {
            return
          }
          const sectionName = entry.target.dataset.section
          if (sectionName && Object.prototype.hasOwnProperty.call(visibleSections.value, sectionName)) {
            visibleSections.value[sectionName] = true
          }
          sectionObserver.unobserve(entry.target)
        })
      },
      { threshold: 0.16, rootMargin: '0px 0px -10% 0px' }
    )

    document.querySelectorAll('.section-reveal').forEach((element) => {
      sectionObserver.observe(element)
    })
  }

  const bindHeroParallax = (selector = '.hero-section') => {
    if (supportsReducedMotion()) {
      return
    }

    heroElement = document.querySelector(selector)
    if (!heroElement) {
      return
    }

    const pointer = { targetX: 0, targetY: 0, currentX: 0, currentY: 0 }
    const maxOffset = 16

    const update = () => {
      pointer.currentX += (pointer.targetX - pointer.currentX) * 0.12
      pointer.currentY += (pointer.targetY - pointer.currentY) * 0.12

      heroElement.style.setProperty('--hero-shift-x', `${pointer.currentX.toFixed(2)}px`)
      heroElement.style.setProperty('--hero-shift-y', `${pointer.currentY.toFixed(2)}px`)

      parallaxFrame = window.requestAnimationFrame(update)
    }

    const onMouseMove = (event) => {
      const rect = heroElement.getBoundingClientRect()
      const centerX = rect.left + rect.width / 2
      const centerY = rect.top + rect.height / 2
      const ratioX = (event.clientX - centerX) / (rect.width / 2)
      const ratioY = (event.clientY - centerY) / (rect.height / 2)

      pointer.targetX = Math.max(-1, Math.min(1, ratioX)) * maxOffset
      pointer.targetY = Math.max(-1, Math.min(1, ratioY)) * maxOffset
    }

    const onMouseLeave = () => {
      pointer.targetX = 0
      pointer.targetY = 0
    }

    heroElement.addEventListener('mousemove', onMouseMove)
    heroElement.addEventListener('mouseleave', onMouseLeave)
    removeMouseMove = () => heroElement?.removeEventListener('mousemove', onMouseMove)
    removeMouseLeave = () => heroElement?.removeEventListener('mouseleave', onMouseLeave)

    parallaxFrame = window.requestAnimationFrame(update)
  }

  const animateNumberMap = (targetMap, onFrame, duration = 1400) => {
    if (counterFrame) {
      window.cancelAnimationFrame(counterFrame)
      counterFrame = 0
    }

    if (supportsReducedMotion()) {
      onFrame({ ...targetMap })
      return
    }

    const start = window.performance.now()
    const beginMap = {}
    Object.keys(targetMap).forEach((key) => {
      beginMap[key] = 0
    })

    const tick = (now) => {
      const progress = Math.min((now - start) / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3)
      const current = {}

      Object.keys(targetMap).forEach((key) => {
        const endValue = Number(targetMap[key]) || 0
        current[key] = Math.round(beginMap[key] + (endValue - beginMap[key]) * eased)
      })

      onFrame(current)
      if (progress < 1) {
        counterFrame = window.requestAnimationFrame(tick)
      }
    }

    counterFrame = window.requestAnimationFrame(tick)
  }

  const cleanup = () => {
    if (sectionObserver) {
      sectionObserver.disconnect()
      sectionObserver = null
    }

    if (parallaxFrame) {
      window.cancelAnimationFrame(parallaxFrame)
      parallaxFrame = 0
    }

    if (counterFrame) {
      window.cancelAnimationFrame(counterFrame)
      counterFrame = 0
    }

    if (removeMouseMove) {
      removeMouseMove()
      removeMouseMove = null
    }

    if (removeMouseLeave) {
      removeMouseLeave()
      removeMouseLeave = null
    }
  }

  onBeforeUnmount(() => {
    cleanup()
  })

  return {
    observeSections,
    bindHeroParallax,
    animateNumberMap,
    cleanup
  }
}
