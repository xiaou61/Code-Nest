<template>
  <CnPage class="daily-content-management-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="每日内容管理"
      description="维护每日推送内容，包括编程格言、技术小贴士、代码片段和历史上的今天。"
      eyebrow="Daily Content"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">总内容 {{ statistics?.totalContents || 0 }}</CnStatusTag>
        <CnStatusTag type="success">格言 {{ statistics?.quotes || 0 }}</CnStatusTag>
        <CnStatusTag type="warning">小贴士 {{ statistics?.tips || 0 }}</CnStatusTag>
        <CnStatusTag type="info">查看 {{ statistics?.totalViews || 0 }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="danger" :icon="Delete" :disabled="selectedContents.length === 0" @click="handleBatchDelete">
          批量删除 ({{ selectedContents.length }})
        </el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增内容</el-button>
      </template>
    </CnPageHeader>

    <div v-if="statistics" class="stats-grid">
      <CnStatCard title="总内容数" :value="statistics.totalContents || 0" description="每日内容库总量" tone="brand" />
      <CnStatCard title="编程格言" :value="statistics.quotes || 0" description="格言类内容数量" tone="success" />
      <CnStatCard title="技术小贴士" :value="statistics.tips || 0" description="提示类内容数量" tone="warning" />
      <CnStatCard title="代码片段" :value="statistics.codeSnippets || 0" description="代码片段内容数量" tone="info" />
      <CnStatCard title="历史上的今天" :value="statistics.histories || 0" description="历史节点内容数量" tone="danger" />
      <CnStatCard title="总查看数" :value="statistics.totalViews || 0" description="内容累计查看次数" tone="neutral" />
    </div>

    <CnSection title="筛选条件" description="按标题、类型、语言和难度筛选每日内容。" divided>
      <div class="filter-grid">
        <el-input
          v-model="searchForm.title"
          placeholder="请输入内容标题"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select v-model="searchForm.contentType" placeholder="内容类型" clearable @change="handleSearch">
          <el-option label="编程格言" :value="1" />
          <el-option label="技术小贴士" :value="2" />
          <el-option label="代码片段" :value="3" />
          <el-option label="历史上的今天" :value="4" />
        </el-select>

        <el-select v-model="searchForm.programmingLanguage" placeholder="编程语言" clearable @change="handleSearch">
          <el-option label="Java" value="Java" />
          <el-option label="Python" value="Python" />
          <el-option label="JavaScript" value="JavaScript" />
          <el-option label="C++" value="C++" />
          <el-option label="Go" value="Go" />
          <el-option label="Rust" value="Rust" />
        </el-select>

        <el-select v-model="searchForm.difficultyLevel" placeholder="难度等级" clearable @change="handleSearch">
          <el-option label="初级" :value="1" />
          <el-option label="中级" :value="2" />
          <el-option label="高级" :value="3" />
        </el-select>

        <div class="filter-actions">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" :icon="DataAnalysis" @click="loadPopularRanking">热门排行</el-button>
        </div>
      </div>
    </CnSection>

    <CnSection title="内容列表" :description="`当前展示 ${contentList.length} 条内容`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="contentList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        border
        empty-title="暂无内容"
        empty-description="当前筛选条件下没有每日内容。"
        empty-icon="DC"
        @selection-change="handleSelectionChange"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #title="{ row }">
          <div class="content-title">
            <el-icon class="type-icon" :class="`type-icon--${row.contentType || 'default'}`">
              <component :is="getContentTypeIcon(row.contentType)" />
            </el-icon>
            <span class="title-text">{{ row.title }}</span>
          </div>
        </template>

        <template #contentType="{ row }">
          <CnStatusTag :type="getContentTypeTone(row.contentType)" size="sm">
            {{ getContentTypeName(row.contentType) }}
          </CnStatusTag>
        </template>

        <template #programmingLanguage="{ row }">
          <CnStatusTag v-if="row.programmingLanguage" type="info" size="sm" subtle>
            {{ row.programmingLanguage }}
          </CnStatusTag>
          <span v-else class="muted-text">-</span>
        </template>

        <template #difficultyLevel="{ row }">
          <CnStatusTag v-if="row.difficultyLevel" :type="getDifficultyTone(row.difficultyLevel)" size="sm">
            {{ getDifficultyName(row.difficultyLevel) }}
          </CnStatusTag>
          <span v-else class="muted-text">-</span>
        </template>

        <template #contentStats="{ row }">
          <div class="content-stats">
            <span><el-icon><View /></el-icon>{{ row.viewCount || 0 }}</span>
            <span><el-icon><Star /></el-icon>{{ row.likeCount || 0 }}</span>
          </div>
        </template>

        <template #createTime="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>

        <template #status="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="info" link size="small" :icon="View" @click="handleView(row)">查看</el-button>
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1000px" :before-close="handleCloseDialog" destroy-on-close>
      <el-form ref="contentFormRef" :model="contentForm" :rules="contentRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="内容标题" prop="title">
              <el-input v-model="contentForm.title" placeholder="请输入内容标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内容类型" prop="contentType">
              <el-select v-model="contentForm.contentType" placeholder="请选择内容类型">
                <el-option label="编程格言" :value="1" />
                <el-option label="技术小贴士" :value="2" />
                <el-option label="代码片段" :value="3" />
                <el-option label="历史上的今天" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="作者">
              <el-input v-model="contentForm.author" placeholder="请输入作者（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="编程语言">
              <el-select v-model="contentForm.programmingLanguage" placeholder="选择编程语言（可选）" clearable>
                <el-option label="Java" value="Java" />
                <el-option label="Python" value="Python" />
                <el-option label="JavaScript" value="JavaScript" />
                <el-option label="C++" value="C++" />
                <el-option label="Go" value="Go" />
                <el-option label="Rust" value="Rust" />
                <el-option label="TypeScript" value="TypeScript" />
                <el-option label="C#" value="C#" />
                <el-option label="PHP" value="PHP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度等级">
              <el-select v-model="contentForm.difficultyLevel" placeholder="选择难度等级（可选）" clearable>
                <el-option label="初级" :value="1" />
                <el-option label="中级" :value="2" />
                <el-option label="高级" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="内容正文" prop="content">
          <el-input v-model="contentForm.content" type="textarea" :rows="8" placeholder="请输入内容正文，支持 Markdown 格式" />
        </el-form-item>

        <el-form-item label="标签">
          <el-input v-model="contentForm.tagsText" placeholder="请输入标签，用逗号分隔（可选）" />
        </el-form-item>

        <el-form-item label="来源链接">
          <el-input v-model="contentForm.sourceUrl" placeholder="请输入来源链接（可选）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="查看内容" width="800px" destroy-on-close>
      <div v-if="viewContent" class="content-view">
        <div class="content-header">
          <h3>{{ viewContent.title }}</h3>
          <div class="content-meta">
            <CnStatusTag :type="getContentTypeTone(viewContent.contentType)" size="sm">
              {{ getContentTypeName(viewContent.contentType) }}
            </CnStatusTag>
            <CnStatusTag v-if="viewContent.programmingLanguage" type="info" size="sm" subtle>
              {{ viewContent.programmingLanguage }}
            </CnStatusTag>
            <CnStatusTag v-if="viewContent.difficultyLevel" :type="getDifficultyTone(viewContent.difficultyLevel)" size="sm">
              {{ getDifficultyName(viewContent.difficultyLevel) }}
            </CnStatusTag>
          </div>
          <div class="view-stats">
            <span>作者：{{ viewContent.author || '无' }}</span>
            <span>查看：{{ viewContent.viewCount || 0 }}</span>
            <span>点赞：{{ viewContent.likeCount || 0 }}</span>
          </div>
        </div>

        <div class="content-body">
          <pre>{{ viewContent.content }}</pre>
        </div>

        <div v-if="viewContent.tags && viewContent.tags.length" class="content-tags">
          <strong>标签：</strong>
          <CnStatusTag v-for="tag in viewContent.tags" :key="tag" type="neutral" size="sm" subtle>
            {{ tag }}
          </CnStatusTag>
        </div>

        <div v-if="viewContent.sourceUrl" class="content-source">
          <strong>来源：</strong>
          <a :href="viewContent.sourceUrl" target="_blank">{{ viewContent.sourceUrl }}</a>
        </div>
      </div>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { DataAnalysis, Delete, Edit, Plus, Refresh, Search, Star, View } from '@element-plus/icons-vue'
import { dailyContentApi } from '@/api/moyu'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface DailyContent extends Record<string, unknown> {
  id: number | null
  title: string
  contentType?: number | null
  content?: string
  author?: string
  programmingLanguage?: string
  tags?: string[]
  tagsText?: string
  difficultyLevel?: number | null
  sourceUrl?: string
  status?: number
  viewCount?: number
  likeCount?: number
  createTime?: string
}

interface DailyContentStatistics {
  totalContents?: number
  quotes?: number
  tips?: number
  codeSnippets?: number
  histories?: number
  totalViews?: number
}

interface SearchForm {
  title: string | null
  contentType: number | null
  programmingLanguage: string | null
  difficultyLevel: number | null
}

interface ContentForm {
  id: number | null
  title: string | null
  contentType: number | null
  content: string | null
  author: string | null
  programmingLanguage: string | null
  tagsText: string | null
  difficultyLevel: number | null
  sourceUrl: string | null
  status: number
}

type SubmitContentData = Omit<ContentForm, 'tagsText'> & { tags?: string[] }

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '摸鱼工具' }, { label: '每日内容' }]

const loading = ref(false)
const contentList = ref<DailyContent[]>([])
const selectedContents = ref<DailyContent[]>([])
const statistics = ref<DailyContentStatistics | null>(null)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const viewContent = ref<DailyContent | null>(null)
const submitLoading = ref(false)
const isEdit = ref(false)
const contentFormRef = ref<FormInstance>()

const searchForm = reactive<SearchForm>({
  title: '',
  contentType: null,
  programmingLanguage: '',
  difficultyLevel: null
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

const contentForm = reactive<ContentForm>({
  id: null,
  title: '',
  contentType: null,
  content: '',
  author: '',
  programmingLanguage: '',
  tagsText: '',
  difficultyLevel: null,
  sourceUrl: '',
  status: 1
})

const contentRules: FormRules<ContentForm> = {
  title: [{ required: true, message: '请输入内容标题', trigger: 'blur' }],
  contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容正文', trigger: 'blur' }]
}

const tableColumns: CnTableColumn<DailyContent>[] = [
  { type: 'selection', width: 55 },
  { prop: 'title', label: '标题', minWidth: 220, slot: 'title', showOverflowTooltip: true },
  { prop: 'contentType', label: '内容类型', width: 120, align: 'center', slot: 'contentType' },
  { prop: 'programmingLanguage', label: '编程语言', width: 120, align: 'center', slot: 'programmingLanguage' },
  { prop: 'difficultyLevel', label: '难度等级', width: 110, align: 'center', slot: 'difficultyLevel' },
  { prop: 'author', label: '作者', width: 120, showOverflowTooltip: true },
  { label: '统计', width: 120, align: 'center', slot: 'contentStats' },
  { prop: 'createTime', label: '创建时间', width: 170, align: 'center', slot: 'createTime' },
  { prop: 'status', label: '状态', width: 100, align: 'center', slot: 'status' },
  { label: '操作', width: 200, fixed: 'right', align: 'center', slot: 'actions' }
]

const dialogTitle = computed(() => (isEdit.value ? '编辑内容' : '新增内容'))
const tablePagination = computed<CnPagination>(() => ({
  page: pagination.currentPage,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const getContentTypeName = (type?: number | null) => {
  const typeMap: Record<number, string> = {
    1: '编程格言',
    2: '技术小贴士',
    3: '代码片段',
    4: '历史上的今天'
  }
  return type ? typeMap[type] || '未知' : '未知'
}

const getContentTypeTone = (type?: number | null): CnTone => {
  const tagMap: Record<number, CnTone> = {
    1: 'success',
    2: 'warning',
    3: 'brand',
    4: 'info'
  }
  return type ? tagMap[type] || 'neutral' : 'neutral'
}

const getContentTypeIcon = (type?: number | null) => {
  const iconMap: Record<number, string> = {
    1: 'Star',
    2: 'InfoFilled',
    3: 'EditPen',
    4: 'Calendar'
  }
  return type ? iconMap[type] || 'Document' : 'Document'
}

const getDifficultyName = (level?: number | null) => {
  const levelMap: Record<number, string> = {
    1: '初级',
    2: '中级',
    3: '高级'
  }
  return level ? levelMap[level] || '未知' : '未知'
}

const getDifficultyTone = (level?: number | null): CnTone => {
  const tagMap: Record<number, CnTone> = {
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return level ? tagMap[level] || 'neutral' : 'neutral'
}

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const loadContentList = async () => {
  try {
    loading.value = true
    const data = await dailyContentApi.getContentList({ ...searchForm, limit: pagination.pageSize })
    contentList.value = data || []
    pagination.total = contentList.value.length
  } catch (error) {
    console.error('加载内容列表失败:', error)
    ElMessage.error('加载内容列表失败')
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const data = await dailyContentApi.getContentStatistics()
    statistics.value = data
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

const loadPopularRanking = async () => {
  try {
    loading.value = true
    const data = await dailyContentApi.getPopularRanking({ limit: 50 })
    contentList.value = data || []
    pagination.total = contentList.value.length
    ElMessage.success('已加载热门内容排行')
  } catch (error) {
    console.error('加载热门排行失败:', error)
    ElMessage.error('加载热门排行失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadContentList()
}

const handleReset = () => {
  Object.assign(searchForm, {
    title: null,
    contentType: null,
    programmingLanguage: null,
    difficultyLevel: null
  })
  handleSearch()
}

const handleSelectionChange = (selection: unknown[]) => {
  selectedContents.value = selection as DailyContent[]
}

const handleStatusChange = async (row: DailyContent) => {
  try {
    await dailyContentApi.updateContentStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
    loadStatistics()
  } catch (error) {
    console.error('状态更新失败:', error)
    ElMessage.error('状态更新失败')
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleView = async (row: DailyContent) => {
  try {
    const data = await dailyContentApi.getContentById(row.id)
    viewContent.value = data
    viewDialogVisible.value = true
  } catch (error) {
    console.error('加载内容详情失败:', error)
    ElMessage.error('加载内容详情失败')
  }
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = async (row: DailyContent) => {
  try {
    isEdit.value = true
    const contentData = await dailyContentApi.getContentById(row.id)

    Object.keys(contentForm).forEach((key) => {
      const formKey = key as keyof ContentForm
      if (formKey === 'tagsText') {
        contentForm[formKey] = contentData.tags ? contentData.tags.join(',') : ''
      } else {
        contentForm[formKey] = contentData[formKey] ?? getDefaultFormValue(formKey)
      }
    })

    dialogVisible.value = true
  } catch (error) {
    console.error('加载内容详情失败:', error)
    ElMessage.error('加载内容详情失败')
  }
}

const handleDelete = async (row: DailyContent) => {
  try {
    await ElMessageBox.confirm(`确定要删除内容 "${row.title}" 吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await dailyContentApi.deleteContent(row.id)
    ElMessage.success('删除成功')
    loadContentList()
    loadStatistics()
  } catch (error) {
    if (error === 'cancel') return
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const handleBatchDelete = async () => {
  if (!selectedContents.value.length) {
    ElMessage.warning('请选择要删除的内容')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedContents.value.length} 个内容吗？`, '确认批量删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const ids = selectedContents.value.map((item) => item.id)
    await dailyContentApi.batchDeleteContents(ids)
    ElMessage.success('批量删除成功')
    selectedContents.value = []
    loadContentList()
    loadStatistics()
  } catch (error) {
    if (error === 'cancel') return
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败')
  }
}

const handleSubmit = async () => {
  if (!contentFormRef.value) return

  try {
    await contentFormRef.value.validate()
    submitLoading.value = true

    const formData: SubmitContentData = { ...contentForm }
    if (contentForm.tagsText) {
      formData.tags = contentForm.tagsText
        .split(',')
        .map((tag) => tag.trim())
        .filter(Boolean)
    }
    delete (formData as Partial<ContentForm>).tagsText

    if (isEdit.value) {
      await dailyContentApi.updateContent(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await dailyContentApi.createContent(formData)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadContentList()
    loadStatistics()
  } catch (error: unknown) {
    if (!(error instanceof Error) || error.name !== 'ValidationError') {
      console.error('提交失败:', error)
      ElMessage.error('提交失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const getDefaultFormValue = (key: keyof ContentForm): ContentForm[keyof ContentForm] => {
  if (key === 'status') return 1
  if (key === 'id' || key === 'contentType' || key === 'difficultyLevel') return null
  return ''
}

const resetForm = () => {
  Object.keys(contentForm).forEach((key) => {
    const formKey = key as keyof ContentForm
    contentForm[formKey] = getDefaultFormValue(formKey)
  })
}

const handleCloseDialog = (done: () => void) => {
  resetForm()
  done()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadContentList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadContentList()
}

onMounted(() => {
  loadContentList()
  loadStatistics()
})
</script>

<style scoped>
.daily-content-management-page {
  min-height: 100%;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1.3fr) repeat(3, minmax(150px, 1fr)) auto;
  gap: var(--cn-space-3);
  align-items: center;
}

.filter-actions,
.table-actions,
.content-title,
.content-meta,
.content-tags,
.view-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.content-title {
  flex-wrap: nowrap;
  min-width: 0;
}

.type-icon {
  flex-shrink: 0;
}

.type-icon--1 {
  color: var(--cn-color-success);
}

.type-icon--2 {
  color: var(--cn-color-warning);
}

.type-icon--3 {
  color: var(--cn-color-brand-primary);
}

.type-icon--4 {
  color: var(--cn-color-info);
}

.type-icon--default {
  color: var(--cn-color-text-tertiary);
}

.title-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-stats {
  display: grid;
  gap: var(--cn-space-1);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.content-stats span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--cn-space-1);
}

.muted-text {
  color: var(--cn-color-text-tertiary);
}

.dialog-footer {
  text-align: right;
}

.content-view {
  max-height: 60vh;
  overflow-y: auto;
}

.content-header {
  margin-bottom: var(--cn-space-4);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.content-header h3 {
  margin: 0 0 var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-size: 20px;
}

.view-stats {
  margin-top: var(--cn-space-3);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.content-body {
  margin-bottom: var(--cn-space-4);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.content-body pre {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-mono);
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.content-tags {
  margin-bottom: var(--cn-space-4);
}

.content-source a {
  color: var(--cn-color-brand-primary);
  text-decoration: none;
}

.content-source a:hover {
  text-decoration: underline;
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .stats-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
