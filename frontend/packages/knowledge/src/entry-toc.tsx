import { useMemo } from 'react'

import { extractHeadings } from './heading-slug'

/**
 * 正文目录。
 *
 * <p><b>用按钮 + `scrollIntoView` 而不是 `href="#id"`</b>：本应用用 Hash 路由，
 * 地址栏的 hash 是**路由**——写成 `href="#常见误解"` 会被路由器当成"跳到名为
 * 常见误解的路由"，结果是跳到一个空白页而不是滚动到章节。这是 Hash 路由下很容易踩的一脚。
 *
 * <p>也因此不改地址栏：滚动是纯视图行为，不值得为它生成一个路由状态。
 */
export function EntryToc({ markdown }: { markdown: string }) {
  const headings = useMemo(() => extractHeadings(markdown), [markdown])

  if (headings.length < 2) {
    // 只有一个标题（通常就是开头那一句）时目录没有导航价值，不如不显示
    return null
  }

  return (
    <nav
      aria-label="正文目录"
      data-testid="entry-toc"
      className="border-border bg-muted/40 mb-8 rounded-md border p-4"
    >
      <div className="text-muted-foreground mb-2 text-xs font-medium">目录</div>
      <ul className="space-y-1">
        {headings.map((heading) => (
          <li key={heading.id} style={{ paddingLeft: `${(heading.level - 1) * 12}px` }}>
            <button
              type="button"
              onClick={() => {
                const target = document.getElementById(heading.id)
                target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
              }}
              className="text-muted-foreground hover:text-foreground text-left text-xs underline-offset-2 hover:underline"
            >
              {heading.text}
            </button>
          </li>
        ))}
      </ul>
    </nav>
  )
}
