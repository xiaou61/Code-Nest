<template>
  <CnPage class="interview-questions-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="题目管理"
      description="管理面试题目，支持 Markdown 参考答案、批量导入和批量删除。"
      eyebrow="Interview Questions"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">面试题库</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ total }} 道题</CnStatusTag>
        <CnStatusTag v-if="currentQuestionSetTitle" type="info">{{ currentQuestionSetTitle }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="danger" :icon="Delete" :disabled="selectedQuestions.length === 0" @click="handleBatchDelete">
          批量删除 ({{ selectedQuestions.length }})
        </el-button>
        <el-button type="success" :icon="Upload" @click="handleImport">MD导入</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增题目</el-button>
      </template>
    </CnPageHeader>

    <div class="interview-stat-grid">
      <CnStatCard title="题目总量" :value="total" description="当前筛选条件下的题目数量" tone="brand" />
      <CnStatCard title="当前页浏览" :value="viewCountInPage" description="当前页题目累计浏览量" tone="info" />
      <CnStatCard title="当前页收藏" :value="favoriteCountInPage" description="当前页题目累计收藏量" tone="success" />
      <CnStatCard title="已选题目" :value="selectedQuestions.length" description="可用于批量删除操作" tone="warning" />
    </div>

    <CnSection title="筛选条件" description="按题单、题目标题和关键词筛选面试题目。" divided>
      <CnFilterForm
        :model-value="queryForm"
        :fields="filterFields"
        :columns="3"
        :loading="loading"
        @update:model-value="handleFilterUpdate"
        @search="handleSearch"
        @reset="handleReset"
      />
    </CnSection>

    <CnSection title="题目列表" :description="`共 ${total} 道题目`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="questionList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @selection-change="handleSelectionChange"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="题目数据" description="预览可检查 Markdown 渲染效果，批量操作仅对已选题目生效。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ queryForm.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="warning" size="sm">已选 {{ selectedQuestions.length }} 道</CnStatusTag>
            </template>
            <el-button type="success" :icon="Upload" @click="handleImport">MD导入</el-button>
            <el-button type="primary" :icon="Plus" @click="handleAdd">新增题目</el-button>
          </CnToolbar>
        </template>

        <template #viewCount="{ row }">
          <CnStatusTag type="info" size="sm">{{ row.viewCount || 0 }}</CnStatusTag>
        </template>

        <template #favoriteCount="{ row }">
          <CnStatusTag type="success" size="sm">{{ row.favoriteCount || 0 }}</CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="info" link size="small" :icon="View" @click="handlePreview(row)">预览</el-button>
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="1200px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="question-form">
        <el-form-item label="题目标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入题目标题" maxlength="500" />
        </el-form-item>
        <el-form-item label="所属题单" prop="questionSetId">
          <el-select v-model="form.questionSetId" placeholder="请选择题单" class="full-width-control">
            <el-option
              v-for="questionSet in questionSetList"
              :key="questionSet.id"
              :label="questionSet.title"
              :value="questionSet.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" placeholder="请输入排序序号" />
        </el-form-item>
        <el-form-item label="参考答案" prop="answer">
          <el-input
            v-model="form.answer"
            type="textarea"
            :rows="12"
            placeholder="请输入参考答案，支持Markdown语法"
            class="full-width-control"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="题目预览" v-model="previewVisible" width="1000px" :close-on-click-modal="false">
      <div v-if="previewData" class="preview-content">
        <div class="preview-header">
          <h3>{{ previewData.title }}</h3>
          <CnStatusTag type="info">{{ previewData.questionSetTitle }}</CnStatusTag>
        </div>

        <div class="answer-section">
          <h3>参考答案</h3>
          <div class="markdown-content" v-html="renderMarkdown(previewData.answer)" />
        </div>
      </div>

      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      title="Markdown导入"
      v-model="importDialogVisible"
      width="900px"
      :close-on-click-modal="false"
      @close="resetImportForm"
    >
      <el-form ref="importFormRef" :model="importForm" :rules="importRules" label-width="100px">
        <el-form-item label="目标题单" prop="questionSetId">
          <el-select v-model="importForm.questionSetId" placeholder="请选择题单" class="full-width-control">
            <el-option
              v-for="questionSet in questionSetList"
              :key="questionSet.id"
              :label="questionSet.title"
              :value="questionSet.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="MD内容" prop="markdownContent">
          <el-input
            v-model="importForm.markdownContent"
            type="textarea"
            :rows="15"
            placeholder="请输入Markdown格式的题目内容，格式示例：&#10;&#10;## 什么是Java中的多态？&#10;&#10;多态是面向对象编程的一个重要特性，指同一个接口可以有不同的实现方式。&#10;&#10;在Java中，多态主要体现在：&#10;- 方法重载（编译时多态）&#10;- 方法重写（运行时多态）&#10;&#10;运行时多态的实现条件：&#10;1. 继承关系&#10;2. 方法重写&#10;3. 父类引用指向子类对象"
            class="full-width-control"
          />
        </el-form-item>
        <el-form-item>
          <el-alert title="格式说明" type="info" :closable="false">
            <template #default>
              <p>使用 ## 作为题目分割标识，## 后的内容作为题目标题。</p>
              <p>题目标题下的所有内容将作为参考答案。</p>
              <p>示例：## 什么是Java中的多态？ [换行] 多态是面向对象编程的一个重要特性...</p>
            </template>
          </el-alert>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImportSubmit">导入</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Upload, View } from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'
import { renderMarkdown as renderSafeMarkdown } from '@/utils/markdown'
import {
  CnDataTable,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn } from '@/design-system'

interface InterviewQuestionSet {
  id: number
  title: string
  [key: string]: unknown
}

interface InterviewQuestion {
  id: number
  questionSetId?: number | null
  title: string
  answer?: string
  questionSetTitle?: string
  sortOrder?: number
  viewCount?: number
  favoriteCount?: number
  createTime?: string
  [key: string]: unknown
}

const route = useRoute()
const breadcrumbs = computed<CnBreadcrumbItem[]>(() => {
  const items: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '面试题目管理' }, { label: '题目管理' }]
  if (currentQuestionSetTitle.value) {
    items.push({ label: currentQuestionSetTitle.value })
  }
  return items
})

const loading = ref(false)
const submitLoading = ref(false)
const importLoading = ref(false)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const importDialogVisible = ref(false)
const questionList = ref<InterviewQuestion[]>([])
const questionSetList = ref<InterviewQuestionSet[]>([])
const selectedQuestions = ref<InterviewQuestion[]>([])
const total = ref(0)
const formRef = ref()
const importFormRef = ref()
const previewData = ref<InterviewQuestion | null>(null)
const currentQuestionSetTitle = ref('')

const queryForm = reactive({
  questionSetId: null as number | null,
  title: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

const form = reactive({
  id: null as number | null,
  questionSetId: null as number | null,
  title: '',
  answer: '',
  sortOrder: 0
})

const importForm = reactive({
  questionSetId: null as number | null,
  markdownContent: ''
})

const rules = {
  title: [
    { required: true, message: '请输入题目标题', trigger: 'blur' },
    { max: 500, message: '题目标题长度不能超过500字符', trigger: 'blur' }
  ],
  questionSetId: [{ required: true, message: '请选择题单', trigger: 'change' }]
}

const importRules = {
  questionSetId: [{ required: true, message: '请选择题单', trigger: 'change' }],
  markdownContent: [{ required: true, message: '请输入Markdown内容', trigger: 'blur' }]
}

const questionSetOptions = computed(() => questionSetList.value.map((item) => ({ label: item.title, value: item.id })))

const filterFields = computed<CnFilterField[]>(() => [
  { prop: 'questionSetId', label: '所属题单', type: 'select', placeholder: '请选择题单', options: questionSetOptions.value },
  { prop: 'title', label: '题目标题', type: 'input', placeholder: '请输入题目标题' },
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '关键词搜索（标题、答案）' }
])

const tableColumns: CnTableColumn<InterviewQuestion>[] = [
  { type: 'selection', width: 55 },
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'title', label: '题目标题', minWidth: 280, showOverflowTooltip: true },
  { prop: 'questionSetTitle', label: '所属题单', width: 180, showOverflowTooltip: true },
  { prop: 'sortOrder', label: '排序', width: 80, align: 'center' },
  { prop: 'viewCount', label: '浏览量', width: 100, align: 'center', slot: 'viewCount' },
  { prop: 'favoriteCount', label: '收藏量', width: 100, align: 'center', slot: 'favoriteCount' },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 170, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: queryForm.pageNum,
  pageSize: queryForm.pageSize,
  total: total.value,
  pageSizes: [10, 20, 50, 100]
}))

const dialogTitle = computed(() => (form.id ? '编辑题目' : '新增题目'))
const viewCountInPage = computed(() => questionList.value.reduce((sum, item) => sum + (Number(item.viewCount) || 0), 0))
const favoriteCountInPage = computed(() => questionList.value.reduce((sum, item) => sum + (Number(item.favoriteCount) || 0), 0))

const renderMarkdown = (text?: string) => renderSafeMarkdown(text || '')

const fetchQuestions = async () => {
  loading.value = true
  try {
    const data = await interviewApi.getQuestions(queryForm)
    questionList.value = data?.records || []
    total.value = data?.total || 0
  } catch (error) {
    console.error('获取题目列表失败:', error)
    ElMessage.error('获取题目列表失败')
  } finally {
    loading.value = false
  }
}

const fetchQuestionSets = async () => {
  try {
    const data = await interviewApi.getQuestionSets({ pageNum: 1, pageSize: 100 })
    questionSetList.value = data?.records || []
  } catch (error) {
    console.error('获取题单列表失败:', error)
    ElMessage.error('获取题单列表失败')
  }
}

const handleFilterUpdate = (value: Record<string, unknown>) => {
  Object.assign(queryForm, value)
}

const handleSearch = () => {
  queryForm.pageNum = 1
  fetchQuestions()
}

const handleReset = () => {
  Object.assign(queryForm, {
    questionSetId: null,
    title: '',
    keyword: '',
    pageNum: 1
  })
  currentQuestionSetTitle.value = ''
  fetchQuestions()
}

const handleSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.pageNum = 1
  fetchQuestions()
}

const handleCurrentChange = (page: number) => {
  queryForm.pageNum = page
  fetchQuestions()
}

const handleSelectionChange = (selection: unknown[]) => {
  selectedQuestions.value = selection as InterviewQuestion[]
}

const handleAdd = () => {
  resetForm()
  if (queryForm.questionSetId) {
    form.questionSetId = queryForm.questionSetId
  }
  dialogVisible.value = true
}

const handleEdit = (row: InterviewQuestion) => {
  form.id = row.id
  form.questionSetId = row.questionSetId || null
  form.title = row.title
  form.answer = row.answer || ''
  form.sortOrder = row.sortOrder || 0
  dialogVisible.value = true
}

const handlePreview = (row: InterviewQuestion) => {
  previewData.value = row
  previewVisible.value = true
}

const handleDelete = async (row: InterviewQuestion) => {
  try {
    await ElMessageBox.confirm(`确定要删除题目 "${row.title}" 吗？删除后将无法恢复！`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await interviewApi.deleteQuestion(row.id)
    ElMessage.success('删除成功')
    await fetchQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除题目失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedQuestions.value.length === 0) return

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedQuestions.value.length} 道题目吗？删除后将无法恢复！`, '批量删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const ids = selectedQuestions.value.map((item) => item.id)
    await interviewApi.batchDeleteQuestions(ids)
    ElMessage.success('批量删除成功')
    selectedQuestions.value = []
    await fetchQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

const handleSubmit = async () => {
  const formElement = formRef.value
  if (!formElement) return

  try {
    await formElement.validate()
    submitLoading.value = true
    const data = {
      questionSetId: form.questionSetId,
      title: form.title,
      answer: form.answer,
      sortOrder: form.sortOrder
    }
    if (form.id) {
      await interviewApi.updateQuestion(form.id, data)
      ElMessage.success('编辑成功')
    } else {
      await interviewApi.createQuestion(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await fetchQuestions()
  } catch (error) {
    if (error?.name !== 'ValidationError') {
      console.error('提交失败:', error)
      ElMessage.error('操作失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const handleImport = () => {
  importForm.questionSetId = queryForm.questionSetId || null
  importForm.markdownContent = ''
  importDialogVisible.value = true
}

const handleImportSubmit = async () => {
  const formElement = importFormRef.value
  if (!formElement) return

  try {
    await formElement.validate()
    importLoading.value = true
    const data = await interviewApi.importMarkdownQuestions({
      questionSetId: importForm.questionSetId,
      markdownContent: importForm.markdownContent
    })
    ElMessage.success(`导入成功，共导入 ${data} 道题目`)
    importDialogVisible.value = false
    await fetchQuestions()
  } catch (error) {
    if (error?.name !== 'ValidationError') {
      console.error('导入失败:', error)
      ElMessage.error('导入失败')
    }
  } finally {
    importLoading.value = false
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    id: null,
    questionSetId: null,
    title: '',
    answer: '',
    sortOrder: 0
  })
}

const resetImportForm = () => {
  importFormRef.value?.resetFields()
  Object.assign(importForm, {
    questionSetId: null,
    markdownContent: ''
  })
}

watch(
  () => route.query,
  (newQuery) => {
    if (newQuery.questionSetId) {
      queryForm.questionSetId = Number(newQuery.questionSetId)
      currentQuestionSetTitle.value = decodeURIComponent(String(newQuery.title || ''))
    }
    fetchQuestions()
  },
  { immediate: true }
)

watch(
  () => queryForm.questionSetId,
  (newValue) => {
    if (newValue) {
      const questionSet = questionSetList.value.find((item) => item.id === newValue)
      currentQuestionSetTitle.value = questionSet ? questionSet.title : ''
    } else {
      currentQuestionSetTitle.value = ''
    }
  }
)

onMounted(() => {
  fetchQuestionSets()
})
</script>

<style scoped>
.interview-questions-page {
  min-height: 100%;
}

.interview-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.table-actions .el-button {
  margin-left: 0;
}

.question-form {
  max-height: 70vh;
  overflow-y: auto;
}

.preview-content {
  max-height: 60vh;
  overflow-y: auto;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--cn-space-5);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.preview-header h3,
.answer-section h3 {
  margin: 0;
  color: var(--cn-color-text-primary);
}

.answer-section {
  margin-top: var(--cn-space-5);
}

.answer-section h3 {
  margin-bottom: var(--cn-space-4);
  font-size: 18px;
  font-weight: 650;
}

.markdown-content {
  padding: var(--cn-space-5);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  line-height: 1.8;
}

.markdown-content :deep(h1) {
  margin: 20px 0 15px;
  color: var(--cn-color-text-primary);
  font-size: 24px;
}

.markdown-content :deep(h2) {
  margin: 18px 0 12px;
  color: var(--cn-color-text-primary);
  font-size: 20px;
}

.markdown-content :deep(h3) {
  margin: 15px 0 10px;
  color: var(--cn-color-text-primary);
  font-size: 16px;
}

.markdown-content :deep(strong) {
  color: var(--cn-color-primary);
  font-weight: 700;
}

.markdown-content :deep(code) {
  padding: 2px 4px;
  border-radius: 3px;
  background: var(--cn-color-bg-surface);
  color: var(--cn-color-danger);
  font-family: "Courier New", monospace;
}

@media (max-width: 1180px) {
  .interview-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .interview-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
