import { computed, ref } from 'vue'
import type { CodeNestThemeMode, CodeNestThemeName, CodeNestThemeOption } from '../types/theme'
import { isCodeNestThemeMode } from '../types/theme'

export const CODE_NEST_THEME_STORAGE_KEY = 'code-nest-theme'

const themeMode = ref<CodeNestThemeMode>('system')
const resolvedTheme = ref<CodeNestThemeName>('light')
let systemThemeListenerReady = false

export const codeNestThemeOptions: CodeNestThemeOption[] = [
  {
    value: 'system',
    label: '跟随系统',
    description: '自动匹配系统亮色或暗色'
  },
  {
    value: 'light',
    label: '亮色',
    description: '明亮清爽的默认主题'
  },
  {
    value: 'dark',
    label: '暗色',
    description: '低亮度工作环境'
  },
  {
    value: 'professional-blue',
    label: '专业蓝',
    description: '适合管理后台的信息密集主题'
  },
  {
    value: 'growth',
    label: '成长绿',
    description: '适合学习成长场景的轻盈主题'
  },
  {
    value: 'high-contrast',
    label: '高对比',
    description: '强化文字、边框和焦点识别'
  }
]

function getSystemTheme(): CodeNestThemeName {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'light'
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function resolveTheme(mode: CodeNestThemeMode): CodeNestThemeName {
  return mode === 'system' ? getSystemTheme() : mode
}

function syncDocumentTheme(mode: CodeNestThemeMode, theme: CodeNestThemeName) {
  if (typeof document === 'undefined') {
    return
  }

  const root = document.documentElement
  root.dataset.theme = theme
  root.dataset.themeMode = mode
  root.style.colorScheme = theme === 'dark' ? 'dark' : 'light'
}

function readStoredTheme(): CodeNestThemeMode {
  if (typeof window === 'undefined') {
    return 'system'
  }

  const storedTheme = window.localStorage.getItem(CODE_NEST_THEME_STORAGE_KEY)
  return isCodeNestThemeMode(storedTheme) ? storedTheme : 'system'
}

function writeStoredTheme(mode: CodeNestThemeMode) {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(CODE_NEST_THEME_STORAGE_KEY, mode)
}

function applyTheme(mode: CodeNestThemeMode, options: { persist?: boolean } = {}) {
  themeMode.value = mode
  resolvedTheme.value = resolveTheme(mode)
  syncDocumentTheme(mode, resolvedTheme.value)

  if (options.persist !== false) {
    writeStoredTheme(mode)
  }
}

function registerSystemThemeListener() {
  if (
    systemThemeListenerReady ||
    typeof window === 'undefined' ||
    typeof window.matchMedia !== 'function'
  ) {
    return
  }

  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  const handleSystemThemeChange = () => {
    if (themeMode.value === 'system') {
      applyTheme('system', { persist: false })
    }
  }

  if (typeof mediaQuery.addEventListener === 'function') {
    mediaQuery.addEventListener('change', handleSystemThemeChange)
  } else {
    mediaQuery.addListener(handleSystemThemeChange)
  }

  systemThemeListenerReady = true
}

function initializeTheme() {
  applyTheme(readStoredTheme(), { persist: false })
  registerSystemThemeListener()
}

function setTheme(mode: CodeNestThemeMode) {
  if (!isCodeNestThemeMode(mode)) {
    return
  }

  applyTheme(mode)
}

function toggleDark() {
  setTheme(resolvedTheme.value === 'dark' ? 'light' : 'dark')
}

const isDark = computed(() => resolvedTheme.value === 'dark')
const followSystem = computed(() => themeMode.value === 'system')

export function useTheme() {
  return {
    themeMode,
    resolvedTheme,
    isDark,
    followSystem,
    options: codeNestThemeOptions,
    initializeTheme,
    setTheme,
    toggleDark
  }
}
