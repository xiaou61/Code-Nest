import { ThemeToggle } from '@paideia/ui'
import { ChevronRight, LayoutDashboard, Library, PanelLeftClose, PanelLeftOpen } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link, useLocation } from 'react-router'

/**
 * 侧边菜单。分组只为表达层级，`to` 与路由表一一对应。
 *
 * <p>放在管理端而不是设计系统里：学习者端是内容优先的单列阅读界面，两侧的信息架构本就
 * 不同，把侧栏做成共享组件等于给阅读界面塞一套它不需要的导航。
 */
const MENU: { section: string | null; items: { to: string; label: string; icon: typeof Library }[] }[] = [
  { section: null, items: [{ to: '/', label: '总览', icon: LayoutDashboard }] },
  { section: '内容管理', items: [{ to: '/knowledge', label: '知识库', icon: Library }] },
]

/** 面包屑的末级：由路由表推出来的固定标题，不做逐级可点的通用面包屑（当前只有两级）。 */
const TITLES: Record<string, string> = { '/': '总览', '/knowledge': '知识库管理' }

/**
 * 管理端外壳：左侧菜单 + 顶栏（面包屑 / 操作区）+ 内容区。
 *
 * <p>与学习者端的 `AppShell` 是两种骨架：那边是顶部横条 + 单列窄内容，这边是后台常见
 * 的"深色边框侧栏 + 灰底内容 + 白卡片"。共用的是色板与组件，不是布局——两边的信息
 * 密度差一个量级，硬套同一个外壳会让这两边都不好用。
 *
 * <p>侧栏可收起（宽窄两档），收起后只留图标并用 `title` 兜住可读性：后台的表单页常需要
 * 横向空间，这是这个布局最该给的一个开关。
 */
export function AdminShell({ actions, children }: { actions?: ReactNode; children: ReactNode }) {
  const { pathname } = useLocation()
  const [collapsed, setCollapsed] = useState(false)
  const current = TITLES[pathname] ?? '管理端'

  return (
    <div className="flex min-h-screen">
      <aside
        className={`border-border bg-card sticky top-0 flex h-screen shrink-0 flex-col border-r transition-[width] duration-200 ${
          collapsed ? 'w-14' : 'w-52'
        }`}
      >
        <div className="border-border flex h-14 items-center gap-2.5 border-b px-3">
          <span className="bg-primary text-primary-foreground grid size-7 shrink-0 place-items-center rounded-md text-xs font-bold">
            P
          </span>
          {collapsed ? null : (
            <div className="min-w-0">
              <div className="truncate text-sm leading-tight font-semibold">Paideia</div>
              <div className="text-muted-foreground truncate text-[11px] leading-tight">管理端</div>
            </div>
          )}
        </div>

        <nav className="flex-1 space-y-4 overflow-y-auto p-2">
          {MENU.map((group, index) => (
            <div key={group.section ?? `group-${index}`} className="space-y-1">
              {group.section === null || collapsed ? null : (
                <div className="text-muted-foreground px-2 pt-1 text-[11px] font-medium">
                  {group.section}
                </div>
              )}
              {group.items.map((item) => {
                const active = pathname === item.to
                const Icon = item.icon
                return (
                  <Link
                    key={item.to}
                    to={item.to}
                    title={item.label}
                    aria-current={active ? 'page' : undefined}
                    data-active={active ? 'true' : undefined}
                    className={`flex h-8 items-center gap-2 rounded-md px-2 text-sm transition-colors ${
                      active
                        ? 'bg-muted text-foreground font-medium'
                        : 'text-muted-foreground hover:bg-muted/60 hover:text-foreground'
                    } ${collapsed ? 'justify-center' : ''}`}
                  >
                    <Icon className="size-4 shrink-0" />
                    {collapsed ? null : <span className="truncate">{item.label}</span>}
                  </Link>
                )
              })}
            </div>
          ))}
        </nav>

        <div className="border-border border-t p-2">
          <button
            type="button"
            onClick={() => setCollapsed((value) => !value)}
            title={collapsed ? '展开菜单' : '收起菜单'}
            aria-label={collapsed ? '展开菜单' : '收起菜单'}
            data-testid="admin-menu-toggle"
            className={`text-muted-foreground hover:bg-muted hover:text-foreground flex h-8 w-full items-center gap-2 rounded-md px-2 text-sm ${
              collapsed ? 'justify-center' : ''
            }`}
          >
            {collapsed ? <PanelLeftOpen className="size-4" /> : <PanelLeftClose className="size-4" />}
            {collapsed ? null : <span>收起菜单</span>}
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="border-border bg-background sticky top-0 z-10 flex h-14 shrink-0 items-center justify-between gap-4 border-b px-4">
          <nav aria-label="面包屑" className="flex min-w-0 items-center gap-1.5 text-sm">
            <span className="text-muted-foreground">首页</span>
            <ChevronRight className="text-muted-foreground size-3.5 shrink-0" />
            <span className="truncate font-medium">{current}</span>
          </nav>
          <div className="flex shrink-0 items-center gap-2">
            {actions}
            <ThemeToggle />
          </div>
        </header>

        <main className="bg-muted/60 flex-1 p-6">{children}</main>
      </div>
    </div>
  )
}
