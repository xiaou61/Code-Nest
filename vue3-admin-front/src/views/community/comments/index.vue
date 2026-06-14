<template>
  <CnPage class="community-comments-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="评论管理"
      description="管理社区帖子评论，支持按帖子、评论者、状态和时间筛选，快速查看评论详情。"
      eyebrow="Community Comments"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">社区管理</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 条评论</CnStatusTag>
        <CnStatusTag type="success">正常 {{ normalCountInPage }} 条</CnStatusTag>
        <CnStatusTag type="danger">删除 {{ deletedCountInPage }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="fetchComments">刷新</el-button>
      </template>
    </CnPageHeader>

    <div class="community-stat-grid">
      <CnStatCard title="评论总量" :value="pagination.total" description="当前筛选条件下的评论数量" tone="brand" />
      <CnStatCard title="正常评论" :value="normalCountInPage" description="当前页仍展示的评论" tone="success" />
      <CnStatCard title="已删评论" :value="deletedCountInPage" description="当前页已删除的评论" tone="danger" />
      <CnStatCard title="点赞总量" :value="likeCountInPage" description="当前页评论累计点赞数" tone="info" />
    </div>

    <CnSection title="筛选条件" description="按帖子 ID、评论者、状态和评论时间筛选评论。" divided>
      <CnFilterForm
        :model-value="searchForm"
        :fields="filterFields"
        :columns="4"
        :loading="loading"
        @update:model-value="handleSearchFormUpdate"
        @search="handleSearch"
        @reset="handleReset"
      />
    </CnSection>

    <CnSection title="评论列表" :description="`共 ${pagination.total} 条评论`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="commentList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="评论数据" description="删除评论会影响帖子互动内容展示，请确认详情后操作。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="info" size="sm">点赞 {{ likeCountInPage }} 次</CnStatusTag>
            </template>
          </CnToolbar>
        </template>

        <template #content="{ row }">
          <div class="comment-cell">
            <span v-if="row.parentId > 0" class="reply-hint">回复评论</span>
            <strong>{{ row.content || '-' }}</strong>
          </div>
        </template>

        <template #likeCount="{ row }">
          <CnStatusTag :type="Number(row.likeCount) > 0 ? 'success' : 'neutral'" size="sm">
            {{ row.likeCount || 0 }} 次
          </CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="row.status === 1 ? 'success' : 'danger'" size="sm">
            {{ row.status === 1 ? '正常' : '删除' }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="View" @click="handleView(row)">查看详情</el-button>
            <el-button type="danger" link size="small" :icon="Delete" :disabled="row.status === 2" @click="handleDelete(row)">
              删除
            </el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无评论"
            description="当前筛选条件下没有社区评论，可以重置筛选后再查看。"
            icon="CM"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="handleReset">重置筛选</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog title="评论详情" v-model="detailVisible" width="800px">
      <div v-if="currentComment" class="comment-detail">
        <div class="detail-header">
          <h4>评论信息</h4>
          <div class="meta-info">
            <span>评论者：{{ currentComment.authorName || '-' }}</span>
            <span>帖子ID：{{ currentComment.postId || '-' }}</span>
            <span v-if="Number(currentComment.parentId) > 0">父评论ID：{{ currentComment.parentId }}</span>
            <span>评论时间：{{ currentComment.createTime || '-' }}</span>
          </div>
        </div>
        <div class="detail-content">
          <h5>评论内容</h5>
          <div class="content-box">{{ currentComment.content || '-' }}</div>
          <div class="stats">
            <CnStatusTag type="success">点赞数：{{ currentComment.likeCount || 0 }}</CnStatusTag>
            <CnStatusTag :type="currentComment.status === 1 ? 'success' : 'danger'">
              状态：{{ currentComment.status === 1 ? '正常' : '删除' }}
            </CnStatusTag>
          </div>
        </div>
      </div>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh, View } from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'
import {
  CnDataTable,
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn } from '@/design-system'

interface CommentRecord {
  id: number
  postId?: number
  parentId?: number
  content: string
  authorName?: string
  likeCount?: number
  status: number
  createTime?: string
  [key: string]: unknown
}

interface SearchForm {
  postId: string
  authorName: string
  status: number | null
  timeRange: string[] | null
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '社区管理' }, { label: '评论管理' }]

const loading = ref(false)
const detailVisible = ref(false)
const commentList = ref<CommentRecord[]>([])
const currentComment = ref<CommentRecord | null>(null)

const searchForm = reactive<SearchForm>({
  postId: '',
  authorName: '',
  status: null,
  timeRange: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const filterFields: CnFilterField[] = [
  { prop: 'postId', label: '帖子ID', type: 'input', placeholder: '请输入帖子ID' },
  { prop: 'authorName', label: '评论者', type: 'input', placeholder: '评论者用户名' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '正常', value: 1 },
      { label: '删除', value: 2 }
    ]
  },
  { prop: 'timeRange', label: '评论时间', type: 'daterange', placeholder: '开始日期' }
]

const tableColumns: CnTableColumn<CommentRecord>[] = [
  { prop: 'id', label: '评论ID', width: 100 },
  { prop: 'postId', label: '帖子ID', width: 100 },
  { prop: 'content', label: '评论内容', minWidth: 280, slot: 'content', showOverflowTooltip: true },
  { prop: 'authorName', label: '评论者', width: 120, showOverflowTooltip: true },
  { prop: 'likeCount', label: '点赞数', width: 100, slot: 'likeCount', align: 'center' },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'createTime', label: '评论时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 150, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.pageNum,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const normalCountInPage = computed(() => commentList.value.filter((item) => item.status === 1).length)
const deletedCountInPage = computed(() => commentList.value.filter((item) => item.status === 2).length)
const likeCountInPage = computed(() => commentList.value.reduce((sum, item) => sum + (Number(item.likeCount) || 0), 0))

onMounted(() => {
  fetchComments()
})

const fetchComments = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    }

    if (searchForm.timeRange && searchForm.timeRange.length === 2) {
      params.startTime = searchForm.timeRange[0]
      params.endTime = searchForm.timeRange[1]
    }
    delete params.timeRange

    if (params.postId) {
      params.postId = Number.parseInt(String(params.postId), 10) || null
    }

    const data = await communityApi.getCommentList(params)
    commentList.value = data?.records || []
    pagination.total = data?.total || 0
  } catch (error) {
    console.error('获取评论列表失败:', error)
    ElMessage.error('获取评论列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchComments()
}

const handleReset = () => {
  Object.assign(searchForm, {
    postId: '',
    authorName: '',
    status: null,
    timeRange: null
  })
  pagination.pageNum = 1
  fetchComments()
}

const handleSearchFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(searchForm, value)
}

const handleView = async (row: CommentRecord) => {
  try {
    currentComment.value = await communityApi.getCommentById(row.id)
    detailVisible.value = true
  } catch (error) {
    console.error('获取评论详情失败:', error)
    ElMessage.error('获取评论详情失败')
  }
}

const handleDelete = async (row: CommentRecord) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？删除后无法恢复。', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await communityApi.deleteComment(row.id)
    ElMessage.success('删除成功')
    await fetchComments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handlePageChange = (page: number) => {
  pagination.pageNum = page
  fetchComments()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchComments()
}
</script>

<style scoped>
.community-comments-page {
  min-height: 100%;
}

.community-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.comment-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.comment-cell strong {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reply-hint {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.table-actions,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.table-actions .el-button {
  margin-left: 0;
}

.comment-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-header {
  margin-bottom: var(--cn-space-5);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.detail-header h4,
.detail-content h5 {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-text-primary);
}

.meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-4);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.content-box {
  margin-bottom: var(--cn-space-4);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-primary);
  line-height: 1.7;
  white-space: pre-wrap;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

@media (max-width: 1180px) {
  .community-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .community-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
