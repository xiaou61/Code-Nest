import {
  MarkdownBody,
  createKnowledgeAdminApi,
  type AdminEntryDetail,
  type AdminEntrySummary,
  type CategoryNode,
  type EntryInput,
} from '@paideia/knowledge'
import {
  Alert,
  AlertDescription,
  AlertTitle,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Input,
  Label,
  PageHeader,
  Section,
  Spinner,
} from '@paideia/ui'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'

import { api } from '../api'
import { API_BASE_URL } from '../env'

const admin = createKnowledgeAdminApi(api)

/** 状态筛选：空值表示全部。 */
type StatusFilter = '' | 'draft' | 'published'

/**
 * 知识库管理区。
 *
 * <p>三块按"内容组织"的自然顺序排：先分类（放东西的架子）、再条目（东西）、最后关系
 * （东西之间的连线）。关系放在条目编辑器里编，因为它总是相对于某一条条目而言的。
 *
 * <p><b>草稿预览复用学习者端的 `MarkdownBody`</b>，不另做一套渲染：预览的全部价值就是
 * "看到的就是学习者看到的"，另写一个渲染器等于让预览说谎。
 */
export function KnowledgeAdminPage() {
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const categories = useQuery({
    queryKey: ['admin-knowledge-categories'],
    queryFn: () => admin.categories(),
  })
  const entries = useQuery({
    queryKey: ['admin-knowledge-entries', statusFilter],
    queryFn: () => admin.entries(statusFilter === '' ? undefined : statusFilter),
  })

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['admin-knowledge-categories'] })
    await queryClient.invalidateQueries({ queryKey: ['admin-knowledge-entries'] })
    await queryClient.invalidateQueries({ queryKey: ['admin-knowledge-entry'] })
  }

  const report = (error: unknown) => setError(error instanceof Error ? error.message : String(error))
  const clearError = () => setError(null)

  const removeEntry = useMutation({
    mutationFn: (id: number) => admin.deleteEntry(id),
    onSuccess: invalidate,
    onError: report,
  })
  const removeCategory = useMutation({
    mutationFn: (id: number) => admin.deleteCategory(id),
    onSuccess: invalidate,
    onError: report,
  })

  return (
    <>
      <PageHeader
        title="知识库管理"
        description="分类、条目与关系的录入；发布前可用预览确认学习者看到的样子。"
      />

      {error === null ? null : (
        <Alert variant="destructive" className="mb-6" data-testid="admin-knowledge-error">
          <AlertTitle>操作失败</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
          <Button variant="ghost" size="sm" className="mt-2" onClick={clearError}>
            知道了
          </Button>
        </Alert>
      )}

      <CategorySection
        nodes={categories.data ?? []}
        loading={categories.isPending}
        onDelete={(id) => removeCategory.mutate(id)}
      />

      <Section title="条目" description="草稿不会出现在学习者端；发布后学习者即可浏览。">
        <div className="mb-4 flex flex-wrap items-center gap-2">
          <Button data-testid="admin-entry-new" onClick={() => setCreating(true)}>
            新建条目
          </Button>
          <select
            className="border-input bg-background h-8 rounded-md border px-2 text-sm"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
            aria-label="按状态筛选"
            data-testid="admin-entry-status-filter"
          >
            <option value="">全部状态</option>
            <option value="draft">仅草稿</option>
            <option value="published">仅已发布</option>
          </select>
        </div>

        {entries.isPending ? (
          <p className="text-muted-foreground flex items-center gap-2 text-sm">
            <Spinner />
            加载条目…
          </p>
        ) : null}
        {entries.isError ? (
          <Alert variant="destructive">
            <AlertTitle>条目加载失败</AlertTitle>
            <AlertDescription>{(entries.error as Error).message}</AlertDescription>
          </Alert>
        ) : null}
        {entries.isSuccess && entries.data.items.length === 0 ? (
          <p className="text-muted-foreground text-sm" data-testid="admin-entry-empty">
            还没有条目
          </p>
        ) : null}
        {entries.isSuccess && entries.data.items.length > 0 ? (
          <ul className="space-y-2" data-testid="admin-entry-list">
            {entries.data.items.map((item) => (
              <EntryRow
                key={item.id}
                entry={item}
                onEdit={() => setEditingId(item.id)}
                onDelete={() => removeEntry.mutate(item.id)}
              />
            ))}
          </ul>
        ) : null}
      </Section>

      {categories.isSuccess ? (
        <EntryEditor
          open={creating || editingId !== null}
          entryId={editingId}
          categories={flatten(categories.data)}
          onClose={() => {
            setCreating(false)
            setEditingId(null)
          }}
          onSaved={invalidate}
          onError={report}
        />
      ) : null}
    </>
  )
}

function EntryRow({
  entry,
  onEdit,
  onDelete,
}: {
  entry: AdminEntrySummary
  onEdit: () => void
  onDelete: () => void
}) {
  return (
    <li>
      <Card>
        <CardContent className="flex items-center justify-between gap-4 pt-6">
          <div className="min-w-0">
            <div className="truncate text-sm font-medium">{entry.title}</div>
            <div className="mt-1 flex items-center gap-2">
              <Badge variant={entry.status === 'published' ? 'outline' : 'secondary'}>
                {entry.status === 'published' ? '已发布' : '草稿'}
              </Badge>
              {entry.categoryName === null ? null : (
                <span className="text-muted-foreground text-xs">{entry.categoryName}</span>
              )}
            </div>
          </div>
          <div className="flex shrink-0 gap-2">
            <Button variant="outline" size="sm" data-testid={`admin-entry-edit-${entry.id}`} onClick={onEdit}>
              编辑
            </Button>
            <Button variant="ghost" size="sm" data-testid={`admin-entry-delete-${entry.id}`} onClick={onDelete}>
              删除
            </Button>
          </div>
        </CardContent>
      </Card>
    </li>
  )
}

interface FlatCategory {
  id: number
  name: string
  depth: number
}

function flatten(nodes: CategoryNode[], depth = 0): FlatCategory[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, depth },
    ...flatten(node.children, depth + 1),
  ])
}

function CategorySection({
  nodes,
  loading,
  onDelete,
}: {
  nodes: CategoryNode[]
  loading: boolean
  onDelete: (id: number) => void
}) {
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [parentId, setParentId] = useState<string>('')
  const [error, setError] = useState<string | null>(null)

  const flat = flatten(nodes)

  const create = useMutation({
    mutationFn: () =>
      admin.createCategory({
        parentId: parentId === '' ? null : Number(parentId),
        name: name.trim(),
        slug: slug.trim(),
      }),
    onSuccess: async () => {
      setName('')
      setSlug('')
      setError(null)
      await queryClient.invalidateQueries({ queryKey: ['admin-knowledge-categories'] })
    },
    onError: (cause) => setError(cause instanceof Error ? cause.message : String(cause)),
  })

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    create.mutate()
  }

  return (
    <Section title="分类" description="层级树。删除前必须先移走它的子分类与条目。">
      {loading ? (
        <p className="text-muted-foreground flex items-center gap-2 text-sm">
          <Spinner />
          加载分类…
        </p>
      ) : null}

      {flat.length === 0 && !loading ? (
        <p className="text-muted-foreground text-sm" data-testid="admin-category-empty">
          还没有分类
        </p>
      ) : null}

      <ul className="mb-6 space-y-1" data-testid="admin-category-list">
        {flat.map((category) => (
          <li
            key={category.id}
            className="flex items-center justify-between gap-4"
            style={{ paddingLeft: `${category.depth * 16}px` }}
          >
            <span className="text-sm">{category.name}</span>
            <Button
              variant="ghost"
              size="sm"
              data-testid={`admin-category-delete-${category.id}`}
              onClick={() => onDelete(category.id)}
            >
              删除
            </Button>
          </li>
        ))}
      </ul>

      {error === null ? null : (
        <p className="text-destructive mb-3 text-sm" data-testid="admin-category-error">
          {error}
        </p>
      )}

      <form onSubmit={onSubmit} className="flex flex-wrap items-end gap-2">
        <div>
          <Label htmlFor="admin-category-parent">上级分类</Label>
          <select
            id="admin-category-parent"
            className="border-input bg-background mt-1 block h-8 rounded-md border px-2 text-sm"
            value={parentId}
            onChange={(event) => setParentId(event.target.value)}
            data-testid="admin-category-parent"
          >
            <option value="">（作为根分类）</option>
            {flat.map((category) => (
              <option key={category.id} value={category.id}>
                {'　'.repeat(category.depth)}
                {category.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <Label htmlFor="admin-category-name">名称</Label>
          <Input
            id="admin-category-name"
            className="mt-1 h-8"
            value={name}
            onChange={(event) => setName(event.target.value)}
            data-testid="admin-category-name"
          />
        </div>
        <div>
          <Label htmlFor="admin-category-slug">标识（小写字母、数字、连字符）</Label>
          <Input
            id="admin-category-slug"
            className="mt-1 h-8"
            value={slug}
            onChange={(event) => setSlug(event.target.value)}
            data-testid="admin-category-slug"
          />
        </div>
        <Button
          type="submit"
          variant="outline"
          disabled={name.trim() === '' || slug.trim() === '' || create.isPending}
          data-testid="admin-category-create"
        >
          新建分类
        </Button>
      </form>
    </Section>
  )
}

/** 关系类型的可选项：与后端 `Relation` 的常量一一对应。 */
const RELATION_TYPES = [
  { value: 'prerequisite', label: '前置（本条目是它的前置）' },
  { value: 'related', label: '相关' },
]

function EntryEditor({
  open,
  entryId,
  categories,
  onClose,
  onSaved,
  onError,
}: {
  open: boolean
  entryId: number | null
  categories: FlatCategory[]
  onClose: () => void
  onSaved: () => Promise<void>
  onError: (error: unknown) => void
}) {
  const detail = useQuery({
    queryKey: ['admin-knowledge-entry', entryId],
    queryFn: () => admin.entry(entryId as number),
    enabled: open && entryId !== null,
  })

  if (!open) {
    return null
  }
  if (entryId !== null && detail.isPending) {
    return (
      <Dialog open onOpenChange={() => onClose()}>
        <DialogContent>
          <DialogTitle>加载条目…</DialogTitle>
          <p className="text-muted-foreground flex items-center gap-2 text-sm">
            <Spinner />
            正在读取
          </p>
        </DialogContent>
      </Dialog>
    )
  }

  return (
    <EntryForm
      // 换条目时用 key 强制重建，免得把上一条的内容留在表单里
      key={entryId ?? 'new'}
      detail={entryId === null ? null : (detail.data ?? null)}
      categories={categories}
      onClose={onClose}
      onSaved={onSaved}
      onError={onError}
    />
  )
}

function EntryForm({
  detail,
  categories,
  onClose,
  onSaved,
  onError,
}: {
  detail: AdminEntryDetail | null
  categories: FlatCategory[]
  onClose: () => void
  onSaved: () => Promise<void>
  onError: (error: unknown) => void
}) {
  const [title, setTitle] = useState(detail?.title ?? '')
  const [body, setBody] = useState(detail?.body ?? '')
  const [categoryId, setCategoryId] = useState(detail?.categoryId == null ? '' : String(detail.categoryId))
  const [status, setStatus] = useState<'draft' | 'published'>(
    detail?.status === 'published' ? 'published' : 'draft',
  )
  const [preview, setPreview] = useState(false)
  const [relationTarget, setRelationTarget] = useState('')
  const [relationType, setRelationType] = useState('prerequisite')

  const save = useMutation({
    mutationFn: async () => {
      const input: EntryInput = {
        title: title.trim(),
        body,
        categoryId: categoryId === '' ? null : Number(categoryId),
        status,
      }
      if (detail === null) {
        return admin.createEntry(input)
      }
      await admin.updateEntry(detail.id, input)
      return detail.id
    },
    onSuccess: async () => {
      await onSaved()
      onClose()
    },
    onError,
  })

  const addRelation = useMutation({
    mutationFn: () =>
      admin.createRelation(Number(detail?.id), Number(relationTarget), relationType),
    onSuccess: async () => {
      setRelationTarget('')
      await onSaved()
    },
    onError,
  })

  const removeRelation = useMutation({
    mutationFn: (id: number) => admin.deleteRelation(id),
    onSuccess: onSaved,
    onError,
  })

  const upload = useMutation({
    mutationFn: (file: File) => admin.uploadFile(file),
    onSuccess: (uploaded) => {
      // 上传与保存是两件事：附件本身不依赖条目存在，所以先把引用插进正文，再随保存一起提交。
      // 图注取原文件名——正文里的 alt 直接决定图注与可访问名称，留空等于浪费一次表达机会。
      setBody((current) => {
        const separator = current === '' || current.endsWith('\n') ? '' : '\n'
        return `${current}${separator}\n![${uploaded.originalName}](${uploaded.url})\n`
      })
    },
    onError,
  })

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    save.mutate()
  }

  return (
    <Dialog open onOpenChange={() => onClose()}>
      <DialogContent className="max-h-[88vh] overflow-y-auto sm:max-w-3xl" data-testid="admin-entry-editor">
        <DialogHeader>
          <DialogTitle>{detail === null ? '新建条目' : '编辑条目'}</DialogTitle>
        </DialogHeader>

        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <Label htmlFor="admin-entry-title">标题</Label>
            <Input
              id="admin-entry-title"
              className="mt-1"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              data-testid="admin-entry-title"
            />
          </div>

          <div className="flex flex-wrap gap-4">
            <div>
              <Label htmlFor="admin-entry-category">分类</Label>
              <select
                id="admin-entry-category"
                className="border-input bg-background mt-1 block h-9 rounded-md border px-2 text-sm"
                value={categoryId}
                onChange={(event) => setCategoryId(event.target.value)}
                data-testid="admin-entry-category"
              >
                <option value="">（未分类）</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {'　'.repeat(category.depth)}
                    {category.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <Label htmlFor="admin-entry-status">状态</Label>
              <select
                id="admin-entry-status"
                className="border-input bg-background mt-1 block h-9 rounded-md border px-2 text-sm"
                value={status}
                onChange={(event) => setStatus(event.target.value as 'draft' | 'published')}
                data-testid="admin-entry-status"
              >
                <option value="draft">草稿</option>
                <option value="published">已发布</option>
              </select>
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between">
              <Label htmlFor="admin-entry-body">正文（markdown）</Label>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                data-testid="admin-entry-preview-toggle"
                onClick={() => setPreview((current) => !current)}
              >
                {preview ? '回到编辑' : '预览'}
              </Button>
            </div>
            {preview ? (
              // 预览与学习者端用同一个渲染组件：这样"预览"才等于"学习者看到的"
              <div className="border-border mt-1 rounded-md border p-4" data-testid="admin-entry-preview">
                <MarkdownBody markdown={body} mediaBaseUrl={API_BASE_URL} />
              </div>
            ) : (
              <textarea
                id="admin-entry-body"
                className="border-input bg-background mt-1 block h-64 w-full rounded-md border p-3 font-mono text-xs"
                value={body}
                onChange={(event) => setBody(event.target.value)}
                data-testid="admin-entry-body"
              />
            )}

            <div className="mt-2">
              <input
                type="file"
                accept="image/png,image/jpeg,image/gif,image/webp,application/pdf"
                aria-label="上传附件"
                data-testid="admin-entry-upload"
                onChange={(event) => {
                  const file = event.target.files?.[0]
                  // 清空 input 的值，否则连续选同一个文件不会触发 change
                  event.target.value = ''
                  if (file !== undefined) {
                    upload.mutate(file)
                  }
                }}
              />
              <p className="text-muted-foreground mt-1 text-xs">
                {upload.isPending
                  ? '上传中…'
                  : '允许 png / jpeg / gif / webp / pdf，单个不超过 10 MB；上传后会把引用插到正文末尾。'}
              </p>
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="ghost" onClick={onClose}>
              取消
            </Button>
            <Button
              type="submit"
              disabled={title.trim() === '' || save.isPending}
              data-testid="admin-entry-save"
            >
              {status === 'published' ? '保存并发布' : '保存草稿'}
            </Button>
          </DialogFooter>
        </form>

        {detail === null ? null : (
          <div className="border-border mt-2 border-t pt-4" data-testid="admin-entry-relations">
            <h3 className="mb-2 text-sm font-medium">关系</h3>
            {detail.relations.length === 0 ? (
              <p className="text-muted-foreground text-sm">还没有关系</p>
            ) : (
              <ul className="mb-3 space-y-1">
                {detail.relations.map((relation) => (
                  <li key={relation.id} className="flex items-center justify-between gap-4 text-sm">
                    <span>
                      {relation.relationType === 'prerequisite' ? '前置' : '相关'}：
                      {relation.fromEntryId === detail.id
                        ? `本条目 → 条目 ${relation.toEntryId}`
                        : `条目 ${relation.fromEntryId} → 本条目`}
                    </span>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => removeRelation.mutate(relation.id)}
                      data-testid={`admin-relation-delete-${relation.id}`}
                    >
                      删除
                    </Button>
                  </li>
                ))}
              </ul>
            )}

            <div className="flex flex-wrap items-end gap-2">
              <div>
                <Label htmlFor="admin-relation-target">目标条目 id</Label>
                <Input
                  id="admin-relation-target"
                  className="mt-1 h-9 w-32"
                  value={relationTarget}
                  onChange={(event) => setRelationTarget(event.target.value)}
                  data-testid="admin-relation-target"
                />
              </div>
              <div>
                <Label htmlFor="admin-relation-type">类型</Label>
                <select
                  id="admin-relation-type"
                  className="border-input bg-background mt-1 block h-9 rounded-md border px-2 text-sm"
                  value={relationType}
                  onChange={(event) => setRelationType(event.target.value)}
                  data-testid="admin-relation-type"
                >
                  {RELATION_TYPES.map((type) => (
                    <option key={type.value} value={type.value}>
                      {type.label}
                    </option>
                  ))}
                </select>
              </div>
              <Button
                type="button"
                variant="outline"
                disabled={relationTarget.trim() === '' || addRelation.isPending}
                onClick={() => addRelation.mutate()}
                data-testid="admin-relation-add"
              >
                建立关系
              </Button>
            </div>
            <p className="text-muted-foreground mt-2 text-xs">
              目标条目用 id 指定（上一步列表里能看到）。关系要先保存条目才能建立。
            </p>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
