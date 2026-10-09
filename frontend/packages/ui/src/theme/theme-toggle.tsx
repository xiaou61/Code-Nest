import { cn } from 'cn'
import { Moon, Sun } from 'lucide-react'

import { useTheme } from './theme-context'

/**
 * 主题切换。纯展示组件：只调 useTheme，不认识平台也不认识存储。
 */
export function ThemeToggle({ className }: { className?: string }) {
  const { theme, toggleTheme } = useTheme()
  const next = theme === 'dark' ? '浅色' : '深色'

  return (
    <button
      type="button"
      onClick={toggleTheme}
      // 图标按钮没有可见文字，因此必须有可读的名字
      aria-label={`切换到${next}主题`}
      title={`切换到${next}主题`}
      data-testid="theme-toggle"
      data-theme={theme}
      className={cn(
        'inline-flex size-9 items-center justify-center rounded-md border border-border',
        'bg-card-solid text-foreground transition-colors',
        'hover:bg-accent hover:text-accent-foreground',
        'focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none',
        className,
      )}
    >
      {theme === 'dark' ? <Sun className="size-4" /> : <Moon className="size-4" />}
    </button>
  )
}
