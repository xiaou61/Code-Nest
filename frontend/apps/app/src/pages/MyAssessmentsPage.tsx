import { createKnowledgeApi } from '@paideia/knowledge'
import type { SelfAssessmentLevel } from '@paideia/knowledge'
import { Alert, AlertDescription, AlertTitle, AppShell, Badge, PageHeader, Spinner } from '@paideia/ui'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'

import { api } from '../api'
import { AccountBar } from '../components/AccountBar'

const knowledge = createKnowledgeApi(api)

/**
 * 我标记过的条目。
 *
 * <p>这是本项目第一个"关于我自己"的页面：内容来自学习者自己的标记，而不是公共内容库。
 * 它的价值不是好看，而是证明"个人状态"这条数据线是通的——掌握度、学习路径将来都长在这上面。
 */
export function MyAssessmentsPage() {
  const [level, setLevel] = useState<'' | SelfAssessmentLevel>('')

  const mine = useQuery({
    queryKey: ['my-assessments', level],
    queryFn: () => knowledge.myAssessments(level === '' ? undefined : level),
  })

  return (
    <AppShell title="Paideia" subtitle="我的标记" actions={<AccountBar />}>
      <PageHeader
        title="我标记过的内容"
        description="标记只是你自己的记录，用来挑出还需要再看一遍的内容。"
      />

      <div className="mb-6 flex flex-wrap items-center gap-2 text-sm">
        <label htmlFor="my-assessment-filter" className="text-muted-foreground text-xs">
          只看
        </label>
        <select
          id="my-assessment-filter"
          className="border-input bg-background h-8 rounded-md border px-2 text-sm"
          value={level}
          onChange={(event) => setLevel(event.target.value as '' | SelfAssessmentLevel)}
          data-testid="my-assessment-filter"
        >
          <option value="">全部</option>
          <option value="understood">懂了</option>
          <option value="unsure">还不懂</option>
        </select>
        <Link to="/knowledge" className="text-muted-foreground ml-auto underline-offset-4 hover:underline">
          ← 回到知识库
        </Link>
      </div>

      {mine.isPending ? (
        <p className="text-muted-foreground flex items-center gap-2 text-sm" data-testid="my-assessment-state">
          <Spinner />
          加载中…
        </p>
      ) : null}
      {mine.isError ? (
        <Alert variant="destructive" data-testid="my-assessment-state">
          <AlertTitle>加载失败</AlertTitle>
          <AlertDescription>{(mine.error as Error).message}</AlertDescription>
        </Alert>
      ) : null}
      {mine.isSuccess && mine.data.length === 0 ? (
        <p className="text-muted-foreground text-sm" data-testid="my-assessment-empty">
          还没有标记过任何内容
        </p>
      ) : null}
      {mine.isSuccess && mine.data.length > 0 ? (
        <ul className="space-y-2" data-testid="my-assessment-list">
          {mine.data.map((item) => (
            <li key={item.entryId} className="flex items-center gap-3">
              <Badge variant={item.level === 'unsure' ? 'secondary' : 'outline'}>
                {item.level === 'unsure' ? '还不懂' : '懂了'}
              </Badge>
              <Link
                to={`/knowledge/entries/${item.entryId}`}
                className="text-sm underline-offset-4 hover:underline"
              >
                {item.title}
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </AppShell>
  )
}
