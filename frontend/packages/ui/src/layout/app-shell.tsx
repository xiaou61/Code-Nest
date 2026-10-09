import type { ReactNode } from 'react'

import { cn } from 'cn'

import { ThemeToggle } from '../theme/theme-toggle'

/**
 * 应用外壳：页头 / 内容区 / 页脚三个插槽。
 *
 * <p>刻意只做布局，不含路由、不含角色判断、不读后端 —— 三个应用（学习者端、管理端、
 * 展览页）的导航与权限各不相同，那些属于各自应用；共用的只是"页面长什么样"。
 *
 * <p>页头是一条实底 + 细分隔线的横条（不是玻璃浮层）：这套观感靠一条极细的线划分层次。
 * 页面的主标题由各页面自己用 text-2xl 那一档写，这里的标题只是应用标识，所以不是 heading。
 */
export function AppShell({
  title,
  subtitle,
  actions,
  children,
  className,
}: {
  title: string
  subtitle?: ReactNode
  /** 页头右侧的操作区，通常放 ThemeToggle 或应用自己的入口。 */
  actions?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <div className={cn('flex min-h-screen flex-col', className)}>
      <header className="border-border bg-background sticky top-0 z-10 flex items-center justify-between gap-4 border-b px-6 py-2.5">
        <div className="min-w-0">
          <div className="truncate text-sm font-semibold">{title}</div>
          {subtitle === undefined ? null : (
            <div className="text-muted-foreground truncate text-xs">{subtitle}</div>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-2">
          {actions}
          <ThemeToggle />
        </div>
      </header>

      <main className="mx-auto w-full max-w-4xl flex-1 px-6 py-10">{children}</main>

      <footer className="border-border text-muted-foreground border-t px-6 py-5 text-center text-xs">
        Paideia · 千人千面的 AI 个性化学习平台
      </footer>
    </div>
  )
}

/**
 * 页面的主标题。放在外壳的内容区里，用字号阶梯里最大的一档 + 负字距，
 * 让层次由字号建立而不是由色块建立。
 */
export function PageHeader({
  title,
  description,
  actions,
}: {
  title: string
  description?: ReactNode
  actions?: ReactNode
}) {
  return (
    <div className="mb-10 flex items-start justify-between gap-6">
      <div className="min-w-0">
        <h1 className="text-2xl">{title}</h1>
        {description === undefined ? null : (
          <p className="text-muted-foreground mt-2 text-sm">{description}</p>
        )}
      </div>
      {actions === undefined ? null : <div className="flex shrink-0 items-center gap-2">{actions}</div>}
    </div>
  )
}

/** 页面里的一节：标题 + 说明 + 内容，展览页与各应用页面共用。 */
export function Section({
  title,
  description,
  children,
  className,
}: {
  title: string
  description?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <section className={cn('mb-12', className)}>
      <div className="mb-4">
        <h2 className="text-base font-semibold tracking-tight">{title}</h2>
        {description === undefined ? null : (
          <p className="text-muted-foreground mt-1 text-sm">{description}</p>
        )}
      </div>
      {children}
    </section>
  )
}
