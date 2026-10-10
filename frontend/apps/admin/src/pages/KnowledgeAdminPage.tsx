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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Spinner,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@paideia/ui'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, RotateCcw, TriangleAlert, Upload } from 'lucide-react'
import { useMemo, useState, type FormEvent } from 'react'

import { api } from '../api'
import { API_BASE_URL } from '../env'

const admin = createKnowledgeAdminApi(api)

/** 每页条数。列表用前端分页：接口一次给一页（size=200），条目量到千级再改成服务端分页。 */
const PAGE_SIZE = 10

type StatusFilter = 'all' | 'draft' | 'published'

const STATUS_FILTER_ITEMS = [
  { value: 'all', label: '全部状态' },
  { value: 'draft', label: '仅草稿' },
  { value: 'published', label: '仅已发布' },
]

const ENTRY_STATUS_ITEMS = [
  { value: 'draft', label: '草稿' },
  { value: 'published', label: '已发布' },
]

/** 关系类型的可选项：与后端 `Relation` 的常量一一对应。 */
const RELATION_TYPES = [
  { value: 'prerequisite', label: '前置（本条目是它的前置）' },
  { value: 'related', label: '相关' },
]

interface PendingDelete {
  kind: 'entry' | 'category'
  id: number
  name: string
}

/**
 * 知识库管理区：后台式的两块表格。
 *
 * <p>分类与条目各是一张卡片：列表用 `<Table>` 而不是卡片堆，工具栏在卡片头上，删除走
 * 确认弹窗，分页在卡片脚。这三样是"一眼看出这是后台"的东西，也是条目变多之后唯一还
 * 能用的形态。
 *
 * <p><b>草稿预览复用学习者端的 `MarkdownBody`</b>，不另做一套渲染：预览的全部价值就是
 * "看到的就是学习者看到的"，另写一个渲染器等于让预览说谎。
 */
export function KnowledgeAdminPage() {
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all')
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(1)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const [creatingCategory, setCreatingCategory] = useState(false)
  const [pendingDelete, setPendingDelete] = useState<PendingDelete | null>(null)
  const [error, setError] = useState<string | null>(null)

  const categories = useQuery({
    queryKey: ['admin-knowledge-categories'],
    queryFn: () => admin.categories(),
  })
  const entries = useQuery({
    queryKey: ['admin-knowledge-entries', statusFilter],
    queryFn: () => admin.entries(statusFilter === 'all' ? undefined : statusFilter),
  })

  const flat = useMemo(() => flatten(categories.data ?? []), [categories.data])

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['admin-knowledge-categories'] })
    await queryClient.invalidateQueries({ queryKey: ['admin-knowledge-entries'] })
    await queryClient.invalidateQueries({ queryKey: ['admin-knowledge-entry'] })
  }

  const report = (cause: unknown) => setError(cause instanceof Error ? cause.message : String(cause))

  const removeEntry = useMutation({
    mutationFn: (id: number) => admin.deleteEntry(id),
    onSuccess: async () => {
      setPendingDelete(null)
      await invalidate()
    },
    onError: report,
  })
  const removeCategory = useMutation({
    mutationFn: (id: number) => admin.deleteCategory(id),
    onSuccess: async () => {
      setPendingDelete(null)
      await invalidate()
    },
    onError: report,
  })

  const all = entries.data?.items ?? []
  const needle = keyword.trim().toLowerCase()
  const filtered = needle === '' ? all : all.filter((item) => item.title.toLowerCase().includes(needle))
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  // 过滤后条数变少时把页码夹回范围内：夹在渲染里算，就不必再写一个"筛选变化时重置页码"的副作用
  const current = Math.min(page, totalPages)
  const rows = filtered.slice((current - 1) * PAGE_SIZE, current * PAGE_SIZE)

  return (
    <>
      <PageHeader
        title="知识库管理"
        description="分类、条目与关系的录入；发布前可用预览确认学习者看到的样子。"
        actions={
          <Button data-testid="admin-entry-new" onClick={() => setCreating(true)}>
            <Plus className="size-4" />
            新建条目
          </Button>
        }
      />

      {error === null ? null : (
        <Alert variant="destructive" className="mb-6" data-testid="admin-knowledge-error">
          <AlertTitle>操作失败</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
          <Button variant="ghost" size="sm" className="mt-2" onClick={() => setError(null)}>
            知道了
          </Button>
        </Alert>
      )}

      <div className="mb-6">
        <Card>
          <CardHeader className="border-border border-b">
            <CardTitle>分类</CardTitle>
            <div className="text-muted-foreground text-xs">层级树；删除前必须先移走它的子分类与条目。</div>
          </CardHeader>
          <CardContent className="px-0">
            <div className="px-(--card-spacing) pb-3">
              <Button
                variant="outline"
                size="sm"
                data-testid="admin-category-new"
                onClick={() => setCreatingCategory(true)}
              >
                <Plus className="size-4" />
                新建分类
              </Button>
            </div>

            {categories.isPending ? <Loading label="加载分类…" /> : null}

            <Table data-testid="admin-category-list">
              <TableHeader>
                <TableRow>
                  <TableHead className="w-20 pl-(--card-spacing)">ID</TableHead>
                  <TableHead>名称</TableHead>
                  <TableHead className="w-40">上级</TableHead>
                  <TableHead className="w-56">标识</TableHead>
                  <TableHead className="w-24 pr-(--card-spacing) text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {flat.length === 0 && !categories.isPending ? (
                  <TableRow>
                    <TableCell colSpan={5} className="text-muted-foreground h-20 text-center" data-testid="admin-category-empty">
                      还没有分类
                    </TableCell>
                  </TableRow>
                ) : null}
                {flat.map((category) => (
                  <TableRow key={category.id}>
                    <TableCell className="text-muted-foreground pl-(--card-spacing) tabular-nums">
                      {category.id}
                    </TableCell>
                    <TableCell style={{ paddingLeft: `${16 + category.depth * 18}px` }}>
                      {category.name}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{category.parentName ?? '—'}</TableCell>
                    <TableCell className="text-muted-foreground font-mono text-xs">{category.slug}</TableCell>
                    <TableCell className="pr-(--card-spacing) text-right">
                      <Button
                        variant="ghost"
                        size="xs"
                        data-testid={`admin-category-delete-${category.id}`}
                        onClick={() => setPendingDelete({ kind: 'category', id: category.id, name: category.name })}
                      >
                        删除
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="border-border border-b">
          <CardTitle>条目</CardTitle>
          <div className="text-muted-foreground text-xs">草稿不会出现在学习者端；发布后学习者即可浏览。</div>
        </CardHeader>
        <CardContent className="px-0">
          <div className="flex flex-wrap items-center gap-2 px-(--card-spacing) pb-3">
            <Select
              items={STATUS_FILTER_ITEMS}
              value={statusFilter}
              onValueChange={(value) => setStatusFilter(value as StatusFilter)}
            >
              <SelectTrigger size="sm" className="w-32" data-testid="admin-entry-status-filter">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {STATUS_FILTER_ITEMS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Input
              className="h-7 w-56"
              placeholder="按标题筛选"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              data-testid="admin-entry-keyword"
            />

            <Button
              variant="ghost"
              size="sm"
              disabled={keyword === '' && statusFilter === 'all'}
              onClick={() => {
                setKeyword('')
                setStatusFilter('all')
              }}
              data-testid="admin-entry-reset"
            >
              <RotateCcw className="size-4" />
              重置
            </Button>
          </div>

          {entries.isPending ? <Loading label="加载条目…" /> : null}

          {entries.isError ? (
            <div className="px-(--card-spacing) pb-3">
              <Alert variant="destructive">
                <AlertTitle>条目加载失败</AlertTitle>
                <AlertDescription>{(entries.error as Error).message}</AlertDescription>
              </Alert>
            </div>
          ) : null}

          <Table data-testid="admin-entry-list">
            <TableHeader>
              <TableRow>
                <TableHead className="w-20 pl-(--card-spacing)">ID</TableHead>
                <TableHead>标题</TableHead>
                <TableHead className="w-40">分类</TableHead>
                <TableHead className="w-24">状态</TableHead>
                <TableHead className="w-40">更新时间</TableHead>
                <TableHead className="w-32 pr-(--card-spacing) text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.length === 0 && !entries.isPending ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-muted-foreground h-24 text-center" data-testid="admin-entry-empty">
                    {all.length === 0 ? '还没有条目' : '没有符合条件的条目'}
                  </TableCell>
                </TableRow>
              ) : null}
              {rows.map((entry) => (
                <TableRow key={entry.id} data-testid={`admin-entry-row-${entry.id}`}>
                  <TableCell className="text-muted-foreground pl-(--card-spacing) tabular-nums">
                    {entry.id}
                  </TableCell>
                  <TableCell className="max-w-0 truncate font-medium" title={entry.title}>
                    {entry.title}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {entry.categoryName ?? '未分类'}
                  </TableCell>
                  <TableCell>
                    <Badge variant={entry.status === 'published' ? 'outline' : 'secondary'}>
                      {entry.status === 'published' ? '已发布' : '草稿'}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground text-xs tabular-nums">
                    {formatTime(entry.updatedAt)}
                  </TableCell>
                  <TableCell className="pr-(--card-spacing) text-right">
                    <Button
                      variant="ghost"
                      size="xs"
                      data-testid={`admin-entry-edit-${entry.id}`}
                      onClick={() => setEditingId(entry.id)}
                    >
                      编辑
                    </Button>
                    <Button
                      variant="ghost"
                      size="xs"
                      className="text-destructive hover:text-destructive"
                      data-testid={`admin-entry-delete-${entry.id}`}
                      onClick={() => setPendingDelete({ kind: 'entry', id: entry.id, name: entry.title })}
                    >
                      删除
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <Pagination
            total={filtered.length}
            current={current}
            totalPages={totalPages}
            onChange={setPage}
          />
        </CardContent>
      </Card>

      {categories.isSuccess ? (
        <EntryEditor
          open={creating || editingId !== null}
          entryId={editingId}
          categories={flat}
          entries={all}
          onClose={() => {
            setCreating(false)
            setEditingId(null)
          }}
          onSaved={invalidate}
          onError={report}
        />
      ) : null}

      <CreateCategoryDialog
        open={creatingCategory}
        categories={flat}
        onClose={() => setCreatingCategory(false)}
        onCreated={invalidate}
        onError={report}
      />

      <ConfirmDeleteDialog
        pending={pendingDelete}
        busy={removeEntry.isPending || removeCategory.isPending}
        onCancel={() => setPendingDelete(null)}
        onConfirm={() => {
          if (pendingDelete === null) {
            return
          }
          if (pendingDelete.kind === 'entry') {
            removeEntry.mutate(pendingDelete.id)
          } else {
            removeCategory.mutate(pendingDelete.id)
          }
        }}
      />
    </>
  )
}

function Loading({ label }: { label: string }) {
  return (
    <p className="text-muted-foreground flex items-center gap-2 px-(--card-spacing) pb-3 text-sm">
      <Spinner />
      {label}
    </p>
  )
}

function Pagination({
  total,
  current,
  totalPages,
  onChange,
}: {
  total: number
  current: number
  totalPages: number
  onChange: (page: number) => void
}) {
  return (
    <div className="border-border text-muted-foreground flex items-center justify-between gap-4 border-t px-(--card-spacing) py-3 text-xs">
      <span data-testid="admin-entry-total">共 {total} 条</span>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="xs"
          disabled={current <= 1}
          onClick={() => onChange(current - 1)}
          data-testid="admin-entry-prev"
        >
          上一页
        </Button>
        <span className="tabular-nums">
          {current} / {totalPages}
        </span>
        <Button
          variant="outline"
          size="xs"
          disabled={current >= totalPages}
          onClick={() => onChange(current + 1)}
          data-testid="admin-entry-next"
        >
          下一页
        </Button>
      </div>
    </div>
  )
}

function ConfirmDeleteDialog({
  pending,
  busy,
  onCancel,
  onConfirm,
}: {
  pending: PendingDelete | null
  busy: boolean
  onCancel: () => void
  onConfirm: () => void
}) {
  if (pending === null) {
    return null
  }
  return (
    <Dialog open onOpenChange={() => onCancel()}>
      <DialogContent className="sm:max-w-md" data-testid="admin-delete-dialog">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <TriangleAlert className="text-destructive size-4" />
            确认删除
          </DialogTitle>
        </DialogHeader>
        <p className="text-muted-foreground text-sm">
          将删除{pending.kind === 'entry' ? '条目' : '分类'}「{pending.name}」，此操作不可撤销。
          {pending.kind === 'category' ? '该分类下还有子分类或条目时会被拒绝。' : ''}
        </p>
        <DialogFooter>
          <Button variant="ghost" onClick={onCancel} data-testid="admin-delete-cancel">
            取消
          </Button>
          <Button variant="destructive" disabled={busy} onClick={onConfirm} data-testid="admin-delete-confirm">
            删除
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

interface FlatCategory {
  id: number
  name: string
  slug: string
  depth: number
  /** 上级分类名：表格里显式列出来，光靠缩进说不清"缩进到哪一层算谁的孩子"。 */
  parentName: string | null
}

/** 后端时间戳是 UTC 的 ISO 串，列表里按浏览器本地时区显示——后台是给人看的钟点，不是给机器的。 */
function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

function flatten(nodes: CategoryNode[], depth = 0, parentName: string | null = null): FlatCategory[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, slug: node.slug, depth, parentName },
    ...flatten(node.children, depth + 1, node.name),
  ])
}

/** 分类下拉的选项：`null` 是"未分类/根分类"，用 `null` 而不是空串——空串是一个合法值，当哨兵会混淆。 */
function categoryOptions(flat: FlatCategory[], noneLabel: string) {
  return [
    { value: null as number | null, label: noneLabel },
    ...flat.map((category) => ({
      value: category.id as number | null,
      label: `${'\u3000'.repeat(category.depth)}${category.name}`,
    })),
  ]
}

function CreateCategoryDialog({
  open,
  categories,
  onClose,
  onCreated,
  onError,
}: {
  open: boolean
  categories: FlatCategory[]
  onClose: () => void
  onCreated: () => Promise<void>
  onError: (cause: unknown) => void
}) {
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [parentId, setParentId] = useState<number | null>(null)

  const options = categoryOptions(categories, '（作为根分类）')

  const create = useMutation({
    mutationFn: () => admin.createCategory({ parentId, name: name.trim(), slug: slug.trim() }),
    onSuccess: async () => {
      setName('')
      setSlug('')
      setParentId(null)
      await onCreated()
      onClose()
    },
    onError,
  })

  if (!open) {
    return null
  }

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    create.mutate()
  }

  return (
    <Dialog open onOpenChange={() => onClose()}>
      <DialogContent data-testid="admin-category-editor">
        <DialogHeader>
          <DialogTitle>新建分类</DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <Label htmlFor="admin-category-parent">上级分类</Label>
            <Select
              items={options}
              value={parentId}
              onValueChange={(value) => setParentId(value)}
            >
              <SelectTrigger id="admin-category-parent" className="mt-1 w-full" data-testid="admin-category-parent">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {options.map((option) => (
                  <SelectItem key={option.value ?? 'none'} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label htmlFor="admin-category-name">名称</Label>
            <Input
              id="admin-category-name"
              className="mt-1"
              value={name}
              onChange={(event) => setName(event.target.value)}
              data-testid="admin-category-name"
            />
          </div>
          <div>
            <Label htmlFor="admin-category-slug">标识（小写字母、数字、连字符）</Label>
            <Input
              id="admin-category-slug"
              className="mt-1"
              value={slug}
              onChange={(event) => setSlug(event.target.value)}
              data-testid="admin-category-slug"
            />
          </div>
          <DialogFooter>
            <Button type="button" variant="ghost" onClick={onClose}>
              取消
            </Button>
            <Button
              type="submit"
              disabled={name.trim() === '' || slug.trim() === '' || create.isPending}
              data-testid="admin-category-create"
            >
              确定
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function EntryEditor({
  open,
  entryId,
  categories,
  entries,
  onClose,
  onSaved,
  onError,
}: {
  open: boolean
  entryId: number | null
  categories: FlatCategory[]
  entries: AdminEntrySummary[]
  onClose: () => void
  onSaved: () => Promise<void>
  onError: (cause: unknown) => void
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
      entries={entries}
      onClose={onClose}
      onSaved={onSaved}
      onError={onError}
    />
  )
}

function EntryForm({
  detail,
  categories,
  entries,
  onClose,
  onSaved,
  onError,
}: {
  detail: AdminEntryDetail | null
  categories: FlatCategory[]
  entries: AdminEntrySummary[]
  onClose: () => void
  onSaved: () => Promise<void>
  onError: (cause: unknown) => void
}) {
  const [title, setTitle] = useState(detail?.title ?? '')
  const [body, setBody] = useState(detail?.body ?? '')
  const [categoryId, setCategoryId] = useState<number | null>(detail?.categoryId ?? null)
  const [status, setStatus] = useState<'draft' | 'published'>(
    detail?.status === 'published' ? 'published' : 'draft',
  )
  const [preview, setPreview] = useState(false)
  const [relationTarget, setRelationTarget] = useState<number | null>(null)
  const [relationType, setRelationType] = useState('prerequisite')

  const categoryItems = categoryOptions(categories, '（未分类）')
  // 关系目标不能是自己（后端也会拒，但下拉里就不该出现这个选项）
  const targetItems = entries
    .filter((entry) => entry.id !== detail?.id)
    .map((entry) => ({ value: entry.id as number | null, label: `#${entry.id} ${entry.title}` }))

  const save = useMutation({
    mutationFn: async () => {
      const input: EntryInput = { title: title.trim(), body, categoryId, status }
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
    mutationFn: () => admin.createRelation(detail?.id as number, relationTarget as number, relationType),
    onSuccess: async () => {
      setRelationTarget(null)
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
          <DialogTitle>{detail === null ? '新建条目' : `编辑条目 #${detail.id}`}</DialogTitle>
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
              <Select items={categoryItems} value={categoryId} onValueChange={(value) => setCategoryId(value)}>
                <SelectTrigger id="admin-entry-category" className="mt-1 w-56" data-testid="admin-entry-category">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {categoryItems.map((option) => (
                    <SelectItem key={option.value ?? 'none'} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label htmlFor="admin-entry-status">状态</Label>
              <Select
                items={ENTRY_STATUS_ITEMS}
                value={status}
                onValueChange={(value) => setStatus(value as 'draft' | 'published')}
              >
                <SelectTrigger id="admin-entry-status" className="mt-1 w-32" data-testid="admin-entry-status">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ENTRY_STATUS_ITEMS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
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
              {/* 原生 <input type="file"> 会渲染出英文的"Choose File"，这是这个弹窗里最扎眼的
                  一块：用 label 当按钮、把 input 藏起来，外观与其它按钮一致。 */}
              <label className="border-border bg-background hover:bg-muted inline-flex h-7 cursor-pointer items-center gap-1 rounded-[min(var(--radius-md),12px)] border px-2.5 text-[0.8rem] font-medium">
                <Upload className="size-3.5" />
                上传附件
                <input
                  type="file"
                  className="sr-only"
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
              </label>
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
                  <li key={relation.id} className="flex items-center gap-2 text-sm">
                    <span>
                      {relation.relationType === 'prerequisite' ? '前置' : '相关'}：
                      {relation.fromEntryId === detail.id
                        ? `本条目 → 条目 ${relation.toEntryId}`
                        : `条目 ${relation.fromEntryId} → 本条目`}
                    </span>
                    <Button
                      variant="ghost"
                      size="xs"
                      className="text-destructive hover:text-destructive"
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
                <Label htmlFor="admin-relation-target">目标条目</Label>
                <Select
                  items={targetItems}
                  value={relationTarget}
                  onValueChange={(value) => setRelationTarget(value)}
                >
                  <SelectTrigger
                    id="admin-relation-target"
                    className="mt-1 w-72"
                    data-testid="admin-relation-target"
                  >
                    <SelectValue placeholder="选择目标条目" />
                  </SelectTrigger>
                  <SelectContent>
                    {targetItems.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="admin-relation-type">类型</Label>
                <Select
                  items={RELATION_TYPES}
                  value={relationType}
                  onValueChange={(value) => setRelationType(value ?? 'prerequisite')}
                >
                  <SelectTrigger id="admin-relation-type" className="mt-1 w-52" data-testid="admin-relation-type">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {RELATION_TYPES.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <Button
                type="button"
                variant="outline"
                disabled={relationTarget === null || addRelation.isPending}
                onClick={() => addRelation.mutate()}
                data-testid="admin-relation-add"
              >
                建立关系
              </Button>
            </div>
            {targetItems.length === 0 ? (
              <p className="text-muted-foreground mt-2 text-xs">还没有其它条目可以关联。</p>
            ) : null}
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
