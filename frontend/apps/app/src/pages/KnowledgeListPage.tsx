import { createKnowledgeApi } from '@paideia/knowledge'
import {
  Alert,
  AlertDescription,
  AlertTitle,
  AppShell,
  Badge,
  Button,
  Card,
  CardContent,
  Input,
  PageHeader,
  Spinner,
} from '@paideia/ui'
import { useQuery } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'

import { api } from '../api'
import { AccountBar } from '../components/AccountBar'
import { CategoryTree } from '../components/CategoryTree'

/** 客户端只包一层路径拼装；令牌与 401 刷新都在 api 里。 */
const knowledge = createKnowledgeApi(api)

/**
 * 知识库列表：左侧分类树、右侧条目列表，顶部检索。
 *
 * <p><b>检索是"回车/点按钮"而不是输入即搜</b>：中文输入法在拼字过程中会连续触发
 * change 事件，按字数防抖会把"并发"这种词的中间态（拼音或半截字）也发出去，
 * 既浪费请求也容易看到一闪而过的空结果。提交式检索没有这个问题，代价是需要用户按一下回车。
 */
export function KnowledgeListPage() {
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [draft, setDraft] = useState('')
  const [keyword, setKeyword] = useState('')

  const categories = useQuery({
    queryKey: ['knowledge-categories'],
    queryFn: () => knowledge.categories(),
  })

  const entries = useQuery({
    queryKey: ['knowledge-entries', categoryId, keyword],
    queryFn: () => knowledge.entries({ categoryId, q: keyword, size: 50 }),
  })

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    setKeyword(draft.trim())
  }

  return (
    <AppShell title="Paideia" subtitle="知识库" actions={<AccountBar />}>
      <PageHeader title="知识库" description="按分类浏览或按关键词检索，打开条目阅读。" />

      <form onSubmit={onSubmit} className="mb-8 flex items-center gap-2">
        <Input
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="搜索标题或正文（中文可用）"
          aria-label="搜索知识条目"
          data-testid="knowledge-search"
          className="max-w-md"
        />
        <Button type="submit" variant="outline" data-testid="knowledge-search-submit">
          搜索
        </Button>
        {keyword === '' ? null : (
          <Button
            type="button"
            variant="ghost"
            data-testid="knowledge-search-clear"
            onClick={() => {
              setDraft('')
              setKeyword('')
            }}
          >
            清除
          </Button>
        )}
      </form>

      <div className="grid gap-8 md:grid-cols-[16rem_1fr]">
        <aside>
          {categories.isPending ? (
            <p className="text-muted-foreground flex items-center gap-2 text-sm" data-testid="category-state">
              <Spinner />
              加载分类…
            </p>
          ) : null}
          {categories.isError ? (
            <Alert variant="destructive" data-testid="category-state">
              <AlertTitle>分类加载失败</AlertTitle>
              <AlertDescription>{(categories.error as Error).message}</AlertDescription>
            </Alert>
          ) : null}
          {categories.isSuccess ? (
            <CategoryTree nodes={categories.data} selectedId={categoryId} onSelect={setCategoryId} />
          ) : null}
        </aside>

        <section>
          {entries.isPending ? (
            <p className="text-muted-foreground flex items-center gap-2 text-sm" data-testid="entry-state">
              <Spinner />
              加载条目…
            </p>
          ) : null}
          {entries.isError ? (
            <Alert variant="destructive" data-testid="entry-state">
              <AlertTitle>条目加载失败</AlertTitle>
              <AlertDescription>{(entries.error as Error).message}</AlertDescription>
            </Alert>
          ) : null}
          {entries.isSuccess && entries.data.items.length === 0 ? (
            <p className="text-muted-foreground text-sm" data-testid="entry-empty">
              没有符合条件的条目
            </p>
          ) : null}
          {entries.isSuccess && entries.data.items.length > 0 ? (
            <div className="space-y-3" data-testid="entry-list">
              <p className="text-muted-foreground text-xs" data-testid="entry-total">
                共 {entries.data.total} 条
              </p>
              {entries.data.items.map((item) => (
                <Card key={item.id}>
                  <CardContent className="pt-6">
                    <Link
                      to={`/knowledge/entries/${item.id}`}
                      className="text-base font-medium underline-offset-4 hover:underline"
                      data-testid={`entry-link-${item.id}`}
                    >
                      {item.title}
                    </Link>
                    <div className="mt-2 flex items-center gap-2">
                      {item.categoryName === null ? null : (
                        <Badge variant="outline">{item.categoryName}</Badge>
                      )}
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          ) : null}
        </section>
      </div>
    </AppShell>
  )
}
