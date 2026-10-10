import { createKnowledgeAdminApi, type CategoryNode } from '@paideia/knowledge'
import { Card, CardContent, CardHeader, CardTitle, Empty, EmptyDescription, EmptyHeader, EmptyTitle, PageHeader } from '@paideia/ui'
import { useQuery } from '@tanstack/react-query'
import { ChevronRight, FileText, FolderTree, FilePen } from 'lucide-react'
import { Link } from 'react-router'

import { api } from '../api'

const admin = createKnowledgeAdminApi(api)

/**
 * 管理端首页：内容规模 + 管理入口。
 *
 * <p>统计值全部取真实接口（分类树与条目列表本就要为管理页加载），不额外加计数端点——
 * 三个数字不值得多一条后端查询。加载中显示 `…` 而不是 0：把"还不知道"画成 0 是在说谎。
 *
 * <p>没有的模块不摆假入口，只留一句说明，等对应需求进来再加。
 */
export function AdminHomePage() {
  const categories = useQuery({
    queryKey: ['admin-knowledge-categories'],
    queryFn: () => admin.categories(),
  })
  const entries = useQuery({
    queryKey: ['admin-knowledge-entries', 'all'],
    queryFn: () => admin.entries(),
  })

  const items = entries.data?.items ?? []
  const stats = [
    { label: '分类', icon: FolderTree, value: categories.data === undefined ? undefined : count(categories.data) },
    { label: '条目', icon: FileText, value: entries.data?.total },
    { label: '其中草稿', icon: FilePen, value: entries.data === undefined ? undefined : items.filter((item) => item.status === 'draft').length },
  ]

  return (
    <>
      <PageHeader title="总览" description="管理端入口与当前内容规模。" />

      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        {stats.map((stat) => (
          <Card key={stat.label}>
            <CardContent className="flex items-center justify-between gap-4">
              <div>
                <div className="text-muted-foreground text-xs">{stat.label}</div>
                <div className="mt-1 text-2xl font-semibold tabular-nums">
                  {stat.value === undefined ? '…' : stat.value}
                </div>
              </div>
              <span className="bg-muted text-muted-foreground grid size-9 shrink-0 place-items-center rounded-md">
                <stat.icon className="size-4" />
              </span>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>内容管理</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <Link
            to="/knowledge"
            data-testid="admin-knowledge-link"
            className="border-border hover:bg-muted/60 flex items-center justify-between gap-4 rounded-md border px-3 py-3 transition-colors"
          >
            <span className="min-w-0">
              <span className="block text-sm font-medium">知识库</span>
              <span className="text-muted-foreground mt-0.5 block text-xs">
                分类、条目与关系的录入；发布前可用预览确认学习者看到的样子。
              </span>
            </span>
            <ChevronRight className="text-muted-foreground size-4 shrink-0" />
          </Link>

          <Empty>
            <EmptyHeader>
              <EmptyTitle>其余管理功能还没有</EmptyTitle>
              <EmptyDescription>
                用户、课程与内容管理的入口会随对应需求加入，这里不提前摆假入口。
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        </CardContent>
      </Card>
    </>
  )
}

/** 分类树的节点总数（含各级子分类）。 */
function count(nodes: CategoryNode[]): number {
  return nodes.reduce((total, node) => total + 1 + count(node.children), 0)
}
