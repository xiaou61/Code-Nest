import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

import { THEME_KEY, isTheme, type Theme, type ThemeStorage } from './keys'

interface ThemeContextValue {
  theme: Theme
  setTheme: (theme: Theme) => void
  toggleTheme: () => void
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

function applyTheme(theme: Theme): void {
  document.documentElement.classList.toggle('dark', theme === 'dark')
}

/**
 * 初始主题直接从 <html> 上已有的类读出来。
 *
 * 这里**刻意不重算一遍**（不读存储、不看系统偏好）：首屏脚本已经算过一次并落了类，
 * 视图层再算一次就可能得出不同结论 —— 那是"首屏颜色对、点一下又跳回去"这类
 * 只在部分环境下复现的 bug。唯一事实来源是 DOM 的当前状态。
 */
function readAppliedTheme(): Theme {
  if (typeof document === 'undefined') {
    return 'light'
  }
  return document.documentElement.classList.contains('dark') ? 'dark' : 'light'
}

const DARK_QUERY = '(prefers-color-scheme: dark)'

export function ThemeProvider({
  storage,
  children,
}: {
  /** 应用把 `platform.cache` 传进来；不传则主题只在本次会话有效。 */
  storage?: ThemeStorage
  children: ReactNode
}) {
  const [theme, setThemeState] = useState<Theme>(readAppliedTheme)

  const setTheme = useCallback(
    (next: Theme) => {
      applyTheme(next)
      setThemeState(next)
      storage?.set(THEME_KEY, next)
    },
    [storage],
  )

  /**
   * 用户还没有显式选择过时跟随系统换色；一旦选择过就不再跟随，
   * 否则用户在系统设置里改了深色，会把他自己挑的主题覆盖掉。
   */
  useEffect(() => {
    if (isTheme(storage?.get(THEME_KEY))) {
      return
    }
    const query = globalThis.matchMedia?.(DARK_QUERY)
    if (query === undefined) {
      return
    }
    const onChange = (event: MediaQueryListEvent): void => {
      // 挂载之后用户可能已经显式选过主题。这个 effect 的依赖是 [storage]，setTheme 不会让它
      // 重跑，监听器会一直留着——不加这层判断，用户挑好的主题会被随后的系统换色覆盖。
      if (isTheme(storage?.get(THEME_KEY))) {
        return
      }
      const next: Theme = event.matches ? 'dark' : 'light'
      applyTheme(next)
      setThemeState(next)
    }
    query.addEventListener('change', onChange)
    return () => query.removeEventListener('change', onChange)
  }, [storage])

  const value = useMemo<ThemeContextValue>(
    () => ({
      theme,
      setTheme,
      toggleTheme: () => setTheme(theme === 'dark' ? 'light' : 'dark'),
    }),
    [theme, setTheme],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const value = useContext(ThemeContext)
  if (value === null) {
    throw new Error('缺少 ThemeProvider：主题必须通过 useTheme() 获取')
  }
  return value
}
