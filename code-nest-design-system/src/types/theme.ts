export const CODE_NEST_THEME_NAMES = [
  'light',
  'dark',
  'professional-blue',
  'growth',
  'high-contrast'
] as const

export const CODE_NEST_THEME_MODES = ['system', ...CODE_NEST_THEME_NAMES] as const

export type CodeNestThemeName = (typeof CODE_NEST_THEME_NAMES)[number]
export type CodeNestThemeMode = (typeof CODE_NEST_THEME_MODES)[number]

export interface ThemePreference {
  mode: CodeNestThemeMode
  resolvedTheme: CodeNestThemeName
  followSystem: boolean
}

export interface CodeNestThemeOption {
  value: CodeNestThemeMode
  label: string
  description?: string
}

export function isCodeNestThemeName(value: unknown): value is CodeNestThemeName {
  return typeof value === 'string' && CODE_NEST_THEME_NAMES.includes(value as CodeNestThemeName)
}

export function isCodeNestThemeMode(value: unknown): value is CodeNestThemeMode {
  return typeof value === 'string' && CODE_NEST_THEME_MODES.includes(value as CodeNestThemeMode)
}
