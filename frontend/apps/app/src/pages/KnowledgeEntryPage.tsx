import {
  EntryToc,
  KnowledgeNeighborhood,
  MarkdownBody,
  SelfAssessmentButtons,
  createKnowledgeApi,
} from '@paideia/knowledge'
import type { EntryRef, SelfAssessmentLevel } from '@paideia/knowledge'
import {
  Alert,
  AlertDescription,
  AlertTitle,
  AppShell,
  Badge,
  PageHeader,
  Spinner,
} from '@paideia/ui'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'

import { api } from '../api'
import { AccountBar } from '../components/AccountBar'
import { API_BASE_URL } from '../env'

const knowledge = createKnowledgeApi(api)

/**
 * 条目详情：正文目录 + md 渲染 + 上一篇／下一篇 + 反向链接。
 *
 * <p>三块阅读辅助都是"从当前条目往外看"，因此都放在正文之后，不打断阅读流：
 * 目录在正文之前（它本来就是给你跳的），相邻与反向链接在正文之后。
 */
export function KnowledgeEntryPage() {
  const params = useParams()
  const entryId = Number(params['id'])
  const valid = Number.isFinite(entryId)
  const queryClient = useQueryClient()

  const entry = useQuery({
    queryKey: ['knowledge-entry', entryId],
    queryFn: () => knowledge.entry(entryId),
    enabled: valid,
  })

  // 标记与取消共用一次 mutation：对用户来说是"改我的标记"，不是两个动作
  const assess = useMutation({
    mutationFn: (next: SelfAssessmentLevel | null) =>
      next === null ? knowledge.clearSelfAssessment(entryId) : knowledge.markSelfAssessment(entryId, next),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['knowledge-entry', entryId] })
      await queryClient.invalidateQueries({ queryKey: ['my-assessments'] })
    },
  })

  return (
    <AppShell title="Paideia" subtitle="知识库" actions={<AccountBar />}>
      <p className="mb-6 text-sm">
        <Link to="/knowledge" className="text-muted-foreground underline-offset-4 hover:underline">
          ← 返回知识库
        </Link>
      </p>

      {!valid ? (
        <Alert variant="destructive" data-testid="entry-state">
          <AlertTitle>地址里的条目编号不合法</AlertTitle>
        </Alert>
      ) : null}

      {valid && entry.isPending ? (
        <p className="text-muted-foreground flex items-center gap-2 text-sm" data-testid="entry-state">
          <Spinner />
          加载条目…
        </p>
      ) : null}

      {valid && entry.isError ? (
        <Alert variant="destructive" data-testid="entry-state">
          <AlertTitle>打不开这条内容</AlertTitle>
          <AlertDescription>{(entry.error as Error).message}</AlertDescription>
        </Alert>
      ) : null}

      {entry.isSuccess ? (
        <article>
          <PageHeader
            title={entry.data.title}
            description={
              entry.data.categoryName === null ? null : (
                <Badge variant="outline">{entry.data.categoryName}</Badge>
              )
            }
          />

          <EntryToc markdown={entry.data.body} />
          <MarkdownBody markdown={entry.data.body} mediaBaseUrl={API_BASE_URL} />

          <div className="mt-8">
            <SelfAssessmentButtons
              current={entry.data.selfAssessment}
              disabled={assess.isPending}
              onChange={(next) => assess.mutate(next)}
            />
          </div>

          <nav className="border-border mt-12 grid gap-4 border-t pt-6 sm:grid-cols-2">
            <div data-testid="entry-previous">
              <div className="text-muted-foreground mb-1 text-xs">上一篇</div>
              {entry.data.previous === null ? (
                <span className="text-muted-foreground text-sm">没有了</span>
              ) : (
                <Link
                  to={`/knowledge/entries/${entry.data.previous.id}`}
                  className="text-sm underline-offset-4 hover:underline"
                >
                  {entry.data.previous.title}
                </Link>
              )}
            </div>
            <div data-testid="entry-next" className="sm:text-right">
              <div className="text-muted-foreground mb-1 text-xs">下一篇</div>
              {entry.data.next === null ? (
                <span className="text-muted-foreground text-sm">没有了</span>
              ) : (
                <Link
                  to={`/knowledge/entries/${entry.data.next.id}`}
                  className="text-sm underline-offset-4 hover:underline"
                >
                  {entry.data.next.title}
                </Link>
              )}
            </div>
          </nav>

          <KnowledgeNeighborhood entry={entry.data} />

          <section className="mt-8 grid gap-6 sm:grid-cols-3">
            <RelationList title="需要先看" items={entry.data.prerequisites} testId="entry-prerequisites" />
            <RelationList title="相关内容" items={entry.data.related} testId="entry-related" />
            <RelationList title="看完这篇再看" items={entry.data.dependents} testId="entry-dependents" />
          </section>
        </article>
      ) : null}
    </AppShell>
  )
}

/** 一组关系链接。三组结构相同，只有标题与来源不同。 */
function RelationList({
  title,
  items,
  testId,
}: {
  title: string
  items: EntryRef[]
  testId: string
}) {
  return (
    <div data-testid={testId}>
      <h2 className="mb-2 text-sm font-medium">{title}</h2>
      {items.length === 0 ? (
        <p className="text-muted-foreground text-sm">暂时没有</p>
      ) : (
        <ul className="space-y-1">
          {items.map((item) => (
            <li key={item.id}>
              <Link
                to={`/knowledge/entries/${item.id}`}
                className="text-sm underline-offset-4 hover:underline"
              >
                {item.title}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
