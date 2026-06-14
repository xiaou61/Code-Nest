import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

export interface UseCnBreakpointsOptions {
  mobile?: number
  tablet?: number
  desktop?: number
  immediate?: boolean
}

export function useCnBreakpoints(options: UseCnBreakpointsOptions = {}) {
  const breakpoints = reactive({
    mobile: options.mobile ?? 768,
    tablet: options.tablet ?? 1024,
    desktop: options.desktop ?? 1280
  })

  const width = ref(getViewportWidth())

  const isMobile = computed(() => width.value > 0 && width.value <= breakpoints.mobile)
  const isTablet = computed(() => width.value > breakpoints.mobile && width.value <= breakpoints.tablet)
  const isDesktop = computed(() => width.value > breakpoints.tablet)
  const isWide = computed(() => width.value >= breakpoints.desktop)

  function update() {
    width.value = getViewportWidth()
  }

  onMounted(() => {
    if (options.immediate !== false) {
      update()
    }

    window.addEventListener('resize', update, { passive: true })
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', update)
  })

  return {
    width,
    breakpoints,
    isMobile,
    isTablet,
    isDesktop,
    isWide,
    update
  }
}

function getViewportWidth() {
  return typeof window === 'undefined' ? 0 : window.innerWidth
}
