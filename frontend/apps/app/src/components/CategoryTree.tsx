import type { CategoryNode } from '@paideia/knowledge'
import { Input } from '@paideia/ui'
import { useState } from 'react'

/**
 * 分类树导航：逐级展开／折叠 + 按名称筛选。
 *
 * <p>后端一次返回整棵树（分类数量是几十级），筛选与折叠都是纯前端状态——不为一个
 * 几十节点的树加一个查询参数，也不值得为它加一个接口。
 *
 * <p>筛选只**收窄显示范围**，不会自动展开：命中的节点若在被折叠的分支里，用户仍然
 * 看不到。所以筛选生效时强制展开全部命中分支（见 {@link filterTree} 保留 children
 * 后 render 时忽略 collapsed）。
 */
export function CategoryTree({
  nodes,
  selectedId,
  onSelect,
}: {
  nodes: CategoryNode[]
  selectedId: number | null
  onSelect: (id: number | null) => void
}) {
  const [query, setQuery] = useState('')
  const [collapsed, setCollapsed] = useState<readonly number[]>([])

  const visible = filterTree(nodes, query.trim())
  const filtering = query.trim() !== ''

  const toggle = (id: number) => {
    setCollapsed((current) =>
      current.includes(id) ? current.filter((value) => value !== id) : [...current, id],
    )
  }

  const renderNodes = (list: CategoryNode[], depth: number) =>
    list.map((node) => {
      const hasChildren = node.children.length > 0
      const isCollapsed = filtering ? false : collapsed.includes(node.id)
      return (
        <li key={node.id}>
          <div className="flex items-center gap-1" style={{ paddingLeft: `${depth * 12}px` }}>
            {hasChildren ? (
              <button
                type="button"
                aria-label={isCollapsed ? '展开' : '折叠'}
                aria-expanded={!isCollapsed}
                onClick={() => toggle(node.id)}
                className="text-muted-foreground hover:text-foreground w-4 shrink-0 text-xs"
              >
                {isCollapsed ? '▸' : '▾'}
              </button>
            ) : (
              <span className="w-4 shrink-0" />
            )}
            <button
              type="button"
              data-testid={`category-${node.slug}`}
              onClick={() => onSelect(node.id)}
              className={
                selectedId === node.id
                  ? 'text-foreground text-left text-sm font-medium'
                  : 'text-muted-foreground hover:text-foreground text-left text-sm'
              }
            >
              {node.name}
            </button>
          </div>
          {hasChildren && !isCollapsed ? (
            <ul className="mt-1 space-y-1">{renderNodes(node.children, depth + 1)}</ul>
          ) : null}
        </li>
      )
    })

  return (
    <div data-testid="category-tree">
      <Input
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="筛选分类"
        aria-label="筛选分类"
        data-testid="category-filter"
        className="mb-3 h-8"
      />
      <ul className="space-y-1">
        <li>
          <button
            type="button"
            data-testid="category-all"
            onClick={() => onSelect(null)}
            className={
              selectedId === null
                ? 'text-foreground text-sm font-medium'
                : 'text-muted-foreground hover:text-foreground text-sm'
            }
          >
            全部
          </button>
        </li>
        {renderNodes(visible, 0)}
      </ul>
      {filtering && visible.length === 0 ? (
        <p className="text-muted-foreground mt-2 text-xs">没有匹配的分类</p>
      ) : null}
    </div>
  )
}

/**
 * 按名称筛选：**保留命中节点的整条祖先链**。
 *
 * <p>只匹配自身会让"Java 并发"这种子分类在搜索时凭空消失（它没有匹配的子节点），
 * 匹配到子分类却丢掉父节点又会让层级看着不对。
 */
function filterTree(nodes: CategoryNode[], keyword: string): CategoryNode[] {
  if (keyword === '') {
    return nodes
  }
  const needle = keyword.toLowerCase()
  return nodes.flatMap((node) => {
    const children = filterTree(node.children, keyword)
    if (node.name.toLowerCase().includes(needle) || children.length > 0) {
      return [{ ...node, children }]
    }
    return []
  })
}
