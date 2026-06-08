import { defineStore } from 'pinia'
import { computed } from 'vue'
import { useTheme } from '@/design-system/composables/useTheme'
import type { ThemePreference } from '@/design-system/types/theme'

export const useThemeStore = defineStore('theme', () => {
  const theme = useTheme()

  const preference = computed<ThemePreference>(() => ({
    mode: theme.themeMode.value,
    resolvedTheme: theme.resolvedTheme.value,
    followSystem: theme.followSystem.value
  }))

  return {
    themeMode: theme.themeMode,
    resolvedTheme: theme.resolvedTheme,
    isDark: theme.isDark,
    followSystem: theme.followSystem,
    options: theme.options,
    preference,
    initializeTheme: theme.initializeTheme,
    setTheme: theme.setTheme,
    toggleDark: theme.toggleDark
  }
})
