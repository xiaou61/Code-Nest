<template>
  <CnPage class="community-posts-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="帖子管理"
      description="管理社区帖子内容、置顶状态和下架删除操作，保障社区内容秩序。"
      eyebrow="Community Posts"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">社区管理</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 篇帖子</CnStatusTag>
        <CnStatusTag type="success">正常 {{ normalCountInPage }} 篇</CnStatusTag>
        <CnStatusTag type="warning">置顶 {{ topCountInPage }} 篇</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="fetchPosts">刷新</el-button>
      </template>
    </CnPageHeader>

    <div class="community-stat-grid">
      <CnStatCard title="帖子总量" :value="pagination.total" description="当前筛选条件下的帖子数量" tone="brand" />
      <CnStatCard title="正常帖子" :value="normalCountInPage" description="当前页可正常展示的帖子" tone="success" />
      <CnStatCard title="下架帖子" :value="disabledCountInPage" description="当前页已被运营下架的帖子" tone="danger" />
      <CnStatCard title="互动总量" :value="engagementCountInPage" description="当前页浏览、点赞、评论、收藏累计" tone="info" />
    </div>

    <CnSection title="筛选条件" description="按标题内容、作者、分类、状态和发布时间定位帖子。" divided>
      <CnFilterForm
        :model-value="searchForm"
        :fields="filterFields"
        :columns="4"
        :loading="loading"
        @update:model-value="handleSearchFormUpdate"
        @search="handleSearch"
        @reset="handleReset"
      >
        <template #categoryId="{ value, setValue }">
          <el-select :model-value="value" placeholder="选择分类" clearable @update:model-value="setValue">
            <el-option
              v-for="category in categoryList"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </template>
      </CnFilterForm>
    </CnSection>

    <CnSection title="帖子列表" :description="`共 ${pagination.total} 篇帖子`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="postList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="帖子数据" description="置顶、下架和删除会直接影响用户端社区内容展示。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="warning" size="sm">置顶 {{ topCountInPage }} 篇</CnStatusTag>
            </template>
          </CnToolbar>
        </template>

        <template #title="{ row }">
          <div class="post-title-cell">
            <div class="post-title-line">
              <CnStatusTag v-if="row.isTop === 1" type="danger" size="sm">置顶</CnStatusTag>
              <strong>{{ row.title || '-' }}</strong>
            </div>
            <span>ID {{ row.id }}</span>
          </div>
        </template>

        <template #categoryName="{ row }">
          <CnStatusTag v-if="row.categoryName" type="info" size="sm">{{ row.categoryName }}</CnStatusTag>
          <span v-else class="muted-text">-</span>
        </template>

        <template #metrics="{ row }">
          <div class="metric-stack">
            <span>浏览 {{ row.viewCount || 0 }}</span>
            <span>赞 {{ row.likeCount || 0 }}</span>
            <span>评 {{ row.commentCount || 0 }}</span>
            <span>藏 {{ row.collectCount || 0 }}</span>
          </div>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getPostStatusTone(row.status)" size="sm">
            {{ getPostStatusText(row.status) }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="View" @click="handleView(row)">查看</el-button>
            <el-button v-if="row.status === 1 && row.isTop === 0" type="warning" link size="small" :icon="Top" @click="handleTop(row)">
              置顶
            </el-button>
            <el-button v-if="row.isTop === 1" type="info" link size="small" :icon="Bottom" @click="handleCancelTop(row)">
              取消置顶
            </el-button>
            <el-button v-if="row.status === 1" type="warning" link size="small" :icon="Lock" @click="handleDisable(row)">
              下架
            </el-button>
            <el-button type="danger" link size="small" :icon="Delete" :disabled="row.status === 3" @click="handleDelete(row)">
              删除
            </el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无帖子"
            description="当前筛选条件下没有社区帖子，可以重置筛选后再查看。"
            icon="PT"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="handleReset">重置筛选</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog title="帖子详情" v-model="detailVisible" width="800px">
      <div v-if="currentPost" class="post-detail">
        <div class="detail-header">
          <h3>{{ currentPost.title }}</h3>
          <div class="meta-info">
            <span>作者：{{ currentPost.authorName || '-' }}</span>
            <span v-if="currentPost.categoryName">分类：{{ currentPost.categoryName }}</span>
            <span>创建时间：{{ currentPost.createTime || '-' }}</span>
          </div>
        </div>
        <div class="detail-content markdown-content" v-html="formatContent(currentPost.content)"></div>
      </div>
    </el-dialog>

    <el-dialog title="置顶帖子" v-model="topDialogVisible" width="400px">
      <el-form :model="topForm" label-width="100px">
        <el-form-item label="置顶时长" required>
          <div class="duration-field">
            <el-input-number v-model="topForm.duration" :min="1" :max="8760" />
            <span>小时</span>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="topDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleTopSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bottom, Delete, Lock, Refresh, Top, View } from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'
import { renderMarkdown } from '@/utils/markdown'
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
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface CategoryOption {
  id: number
  name: string
}

interface PostRecord {
  id: number
  title: string
  content?: string
  categoryId?: number | null
  categoryName?: string
  authorName?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  collectCount?: number
  status: number
  isTop?: number
  createTime?: string
  [key: string]: unknown
}

interface SearchForm {
  keyword: string
  authorName: string
  categoryId: number | null
  status: number | null
  timeRange: string[] | null
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '社区管理' }, { label: '帖子管理' }]

const loading = ref(false)
const submitLoading = ref(false)
const detailVisible = ref(false)
const topDialogVisible = ref(false)
const postList = ref<PostRecord[]>([])
const categoryList = ref<CategoryOption[]>([])
const currentPost = ref<PostRecord | null>(null)

const searchForm = reactive<SearchForm>({
  keyword: '',
  authorName: '',
  categoryId: null,
  status: null,
  timeRange: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const topForm = reactive({
  duration: 24
})

const filterFields: CnFilterField[] = [
  { prop: 'keyword', label: '帖子关键词', type: 'input', placeholder: '请输入标题或内容关键词' },
  { prop: 'authorName', label: '作者', type: 'input', placeholder: '作者用户名' },
  { prop: 'categoryId', label: '分类', type: 'custom', slot: 'categoryId' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '正常', value: 1 },
      { label: '下架', value: 2 },
      { label: '删除', value: 3 }
    ]
  },
  { prop: 'timeRange', label: '发布时间', type: 'daterange', placeholder: '开始日期' }
]

const tableColumns: CnTableColumn<PostRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'title', label: '帖子标题', minWidth: 220, slot: 'title', showOverflowTooltip: true },
  { prop: 'categoryName', label: '分类', width: 120, slot: 'categoryName' },
  { prop: 'authorName', label: '作者', width: 120, showOverflowTooltip: true },
  { label: '数据统计', width: 190, slot: 'metrics', align: 'center' },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 240, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.pageNum,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const normalCountInPage = computed(() => postList.value.filter((item) => item.status === 1).length)
const disabledCountInPage = computed(() => postList.value.filter((item) => item.status === 2).length)
const topCountInPage = computed(() => postList.value.filter((item) => item.isTop === 1).length)
const engagementCountInPage = computed(() =>
  postList.value.reduce(
    (sum, item) =>
      sum +
      (Number(item.viewCount) || 0) +
      (Number(item.likeCount) || 0) +
      (Number(item.commentCount) || 0) +
      (Number(item.collectCount) || 0),
    0
  )
)

onMounted(async () => {
  await loadCategories()
  fetchPosts()
})

const fetchPosts = async () => {
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

    const data = await communityApi.getPostList(params)
    postList.value = data?.records || []
    pagination.total = data?.total || 0
  } catch (error) {
    console.error('获取帖子列表失败:', error)
    ElMessage.error('获取帖子列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchPosts()
}

const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    authorName: '',
    categoryId: null,
    status: null,
    timeRange: null
  })
  pagination.pageNum = 1
  fetchPosts()
}

const handleSearchFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(searchForm, value)
}

const handleView = async (row: PostRecord) => {
  try {
    currentPost.value = await communityApi.getPostById(row.id)
    detailVisible.value = true
  } catch (error) {
    console.error('获取帖子详情失败:', error)
    ElMessage.error('获取帖子详情失败')
  }
}

const handleTop = (row: PostRecord) => {
  currentPost.value = row
  topForm.duration = 24
  topDialogVisible.value = true
}

const handleTopSubmit = async () => {
  if (!currentPost.value) return

  submitLoading.value = true
  try {
    await communityApi.topPost(currentPost.value.id, { duration: topForm.duration })
    ElMessage.success('置顶成功')
    topDialogVisible.value = false
    await fetchPosts()
  } catch (error) {
    console.error('置顶失败:', error)
    ElMessage.error('置顶失败')
  } finally {
    submitLoading.value = false
  }
}

const handleCancelTop = async (row: PostRecord) => {
  try {
    await ElMessageBox.confirm(`确定要取消置顶帖子 "${row.title}" 吗？`, '取消置顶确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await communityApi.cancelTop(row.id)
    ElMessage.success('取消置顶成功')
    await fetchPosts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消置顶失败:', error)
      ElMessage.error('取消置顶失败')
    }
  }
}

const handleDisable = async (row: PostRecord) => {
  try {
    await ElMessageBox.confirm(`确定要下架帖子 "${row.title}" 吗？`, '下架确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await communityApi.disablePost(row.id)
    ElMessage.success('下架成功')
    await fetchPosts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('下架失败:', error)
      ElMessage.error('下架失败')
    }
  }
}

const handleDelete = async (row: PostRecord) => {
  try {
    await ElMessageBox.confirm(`确定要删除帖子 "${row.title}" 吗？删除后无法恢复。`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await communityApi.deletePost(row.id)
    ElMessage.success('删除成功')
    await fetchPosts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handlePageChange = (page: number) => {
  pagination.pageNum = page
  fetchPosts()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchPosts()
}

const formatContent = (content: string | undefined) => {
  if (!content) return ''
  return renderMarkdown(content)
}

const loadCategories = async () => {
  try {
    const data = await communityApi.getEnabledCategories()
    categoryList.value = data || []
  } catch (error) {
    console.error('加载分类列表失败:', error)
  }
}

const getPostStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    1: '正常',
    2: '下架',
    3: '删除'
  }
  return statusMap[status] || '未知'
}

const getPostStatusTone = (status: number): CnTone => {
  const toneMap: Record<number, CnTone> = {
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return toneMap[status] || 'neutral'
}
</script>

<style scoped>
.community-posts-page {
  min-height: 100%;
}

.community-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.post-title-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.post-title-line {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: var(--cn-space-2);
}

.post-title-line strong {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-title-cell span,
.muted-text,
.duration-field span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.metric-stack {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.table-actions,
.dialog-footer,
.duration-field {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.duration-field {
  align-items: center;
}

.table-actions .el-button {
  margin-left: 0;
}

.dialog-footer {
  justify-content: flex-end;
}

.post-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-header {
  margin-bottom: var(--cn-space-5);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.detail-header h3 {
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

.detail-content {
  color: var(--cn-color-text-primary);
  line-height: 1.7;
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

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
