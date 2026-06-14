<template>
  <CnPage class="interview-question-sets-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="题单管理"
      description="管理官方面试题单，支持分类归属、发布状态和题目入口维护。"
      eyebrow="Interview Question Sets"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">面试题库</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ total }} 个题单</CnStatusTag>
        <CnStatusTag type="success">已发布 {{ publishedCountInPage }} 个</CnStatusTag>
        <CnStatusTag type="warning">草稿 {{ draftCountInPage }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增题单</el-button>
      </template>
    </CnPageHeader>

    <div class="interview-stat-grid">
      <CnStatCard title="题单总量" :value="total" description="当前筛选条件下的题单数量" tone="brand" />
      <CnStatCard title="当前页题目" :value="questionCountInPage" description="当前页题单关联题目总数" tone="info" />
      <CnStatCard title="浏览总量" :value="viewCountInPage" description="当前页题单累计浏览量" tone="success" />
      <CnStatCard title="收藏总量" :value="favoriteCountInPage" description="当前页题单累计收藏量" tone="warning" />
    </div>

    <CnSection title="筛选条件" description="按题单标题、分类、状态和创建类型筛选面试题单。" divided>
      <CnFilterForm
        :model-value="queryForm"
        :fields="filterFields"
        :columns="4"
        :loading="loading"
        @update:model-value="handleFilterUpdate"
        @search="handleSearch"
        @reset="handleReset"
      />
    </CnSection>

    <CnSection title="题单列表" :description="`共 ${total} 个题单`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="questionSetList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="题单数据" description="题单状态会影响用户端可见性，题目入口用于维护题单下的问题。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ queryForm.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="info" size="sm">题目 {{ questionCountInPage }} 道</CnStatusTag>
            </template>
            <el-button type="primary" :icon="Plus" @click="handleAdd">新增题单</el-button>
          </CnToolbar>
        </template>

        <template #type="{ row }">
          <CnStatusTag :type="row.type === 1 ? 'success' : 'info'" size="sm">
            {{ row.type === 1 ? '官方' : '用户' }}
          </CnStatusTag>
        </template>

        <template #questionCount="{ row }">
          <CnStatusTag type="info" size="sm">{{ row.questionCount || 0 }}</CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getStatusTone(row.status)" size="sm">{{ getStatusText(row.status) }}</CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="info" link size="small" :icon="View" @click="handleViewQuestions(row)">题目</el-button>
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="700px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="题单标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入题单标题" maxlength="200" />
        </el-form-item>
        <el-form-item label="题单描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入题单描述"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" class="full-width-control">
            <el-option
              v-for="category in enabledCategories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">官方题单</el-radio>
            <el-radio :label="2">用户题单</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type === 2" label="可见性" prop="visibility">
          <el-radio-group v-model="form.visibility">
            <el-radio :label="1">公开</el-radio>
            <el-radio :label="2">私有</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">草稿</el-radio>
            <el-radio :label="1">发布</el-radio>
            <el-radio :label="2">下线</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, View } from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'
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
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface InterviewCategory {
  id: number
  name: string
  status: number
  [key: string]: unknown
}

interface InterviewQuestionSet {
  id: number
  title: string
  description?: string
  categoryId?: number | null
  categoryName?: string
  type?: number
  visibility?: number
  questionCount?: number
  viewCount?: number
  favoriteCount?: number
  status: number
  creatorName?: string
  createTime?: string
  [key: string]: unknown
}

const router = useRouter()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '面试题目管理' }, { label: '题单管理' }]

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const questionSetList = ref<InterviewQuestionSet[]>([])
const categoryList = ref<InterviewCategory[]>([])
const total = ref(0)
const formRef = ref()

const queryForm = reactive({
  title: '',
  categoryId: null as number | null,
  type: null as number | null,
  status: null as number | null,
  pageNum: 1,
  pageSize: 10
})

const form = reactive({
  id: null as number | null,
  title: '',
  description: '',
  categoryId: null as number | null,
  type: 1,
  visibility: 1,
  status: 1
})

const rules = {
  title: [
    { required: true, message: '请输入题单标题', trigger: 'blur' },
    { max: 200, message: '题单标题长度不能超过200字符', trigger: 'blur' }
  ],
  description: [{ max: 2000, message: '题单描述长度不能超过2000字符', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const categoryOptions = computed(() => categoryList.value.map((item) => ({ label: item.name, value: item.id })))
const enabledCategories = computed(() => categoryList.value.filter((item) => item.status === 1))

const filterFields = computed<CnFilterField[]>(() => [
  { prop: 'title', label: '题单标题', type: 'input', placeholder: '请输入题单标题' },
  { prop: 'categoryId', label: '分类', type: 'select', placeholder: '请选择分类', options: categoryOptions.value },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '草稿', value: 0 },
      { label: '发布', value: 1 },
      { label: '下线', value: 2 }
    ]
  },
  {
    prop: 'type',
    label: '类型',
    type: 'select',
    placeholder: '请选择类型',
    options: [
      { label: '官方', value: 1 },
      { label: '用户创建', value: 2 }
    ]
  }
])

const tableColumns: CnTableColumn<InterviewQuestionSet>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'title', label: '题单标题', minWidth: 200, showOverflowTooltip: true },
  { prop: 'description', label: '描述', minWidth: 240, showOverflowTooltip: true },
  { prop: 'categoryName', label: '分类', width: 120, showOverflowTooltip: true },
  { prop: 'type', label: '类型', width: 100, align: 'center', slot: 'type' },
  { prop: 'questionCount', label: '题目数', width: 100, align: 'center', slot: 'questionCount' },
  { prop: 'viewCount', label: '浏览量', width: 100, align: 'center' },
  { prop: 'favoriteCount', label: '收藏量', width: 100, align: 'center' },
  { prop: 'status', label: '状态', width: 100, align: 'center', slot: 'status' },
  { prop: 'creatorName', label: '创建者', width: 120, showOverflowTooltip: true },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 190, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: queryForm.pageNum,
  pageSize: queryForm.pageSize,
  total: total.value,
  pageSizes: [10, 20, 50, 100]
}))

const dialogTitle = computed(() => (form.id ? '编辑题单' : '新增题单'))
const publishedCountInPage = computed(() => questionSetList.value.filter((item) => item.status === 1).length)
const draftCountInPage = computed(() => questionSetList.value.filter((item) => item.status === 0).length)
const questionCountInPage = computed(() => questionSetList.value.reduce((sum, item) => sum + (Number(item.questionCount) || 0), 0))
const viewCountInPage = computed(() => questionSetList.value.reduce((sum, item) => sum + (Number(item.viewCount) || 0), 0))
const favoriteCountInPage = computed(() => questionSetList.value.reduce((sum, item) => sum + (Number(item.favoriteCount) || 0), 0))

const getStatusText = (status: number) => ({ 0: '草稿', 1: '发布', 2: '下线' })[status] || '未知'
const getStatusTone = (status: number): CnTone => ({ 0: 'warning', 1: 'success', 2: 'danger' })[status] as CnTone || 'neutral'

const fetchQuestionSets = async () => {
  loading.value = true
  try {
    const data = await interviewApi.getQuestionSets(queryForm)
    questionSetList.value = data?.records || []
    total.value = data?.total || 0
  } catch (error) {
    console.error('获取题单列表失败:', error)
    ElMessage.error('获取题单列表失败')
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const data = await interviewApi.getAllCategories()
    categoryList.value = data || []
  } catch (error) {
    console.error('获取分类列表失败:', error)
    ElMessage.error('获取分类列表失败')
  }
}

const handleFilterUpdate = (value: Record<string, unknown>) => {
  Object.assign(queryForm, value)
}

const handleSearch = () => {
  queryForm.pageNum = 1
  fetchQuestionSets()
}

const handleReset = () => {
  Object.assign(queryForm, {
    title: '',
    categoryId: null,
    type: null,
    status: null,
    pageNum: 1
  })
  fetchQuestionSets()
}

const handleSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.pageNum = 1
  fetchQuestionSets()
}

const handleCurrentChange = (page: number) => {
  queryForm.pageNum = page
  fetchQuestionSets()
}

const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: InterviewQuestionSet) => {
  form.id = row.id
  form.title = row.title
  form.description = row.description || ''
  form.categoryId = row.categoryId || null
  form.type = row.type || 1
  form.visibility = row.visibility || 1
  form.status = row.status
  dialogVisible.value = true
}

const handleDelete = async (row: InterviewQuestionSet) => {
  try {
    await ElMessageBox.confirm(`确定要删除题单 "${row.title}" 吗？删除后将无法恢复！`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await interviewApi.deleteQuestionSet(row.id)
    ElMessage.success('删除成功')
    await fetchQuestionSets()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除题单失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleViewQuestions = (row: InterviewQuestionSet) => {
  router.push(`/interview/questions?questionSetId=${row.id}&title=${encodeURIComponent(row.title)}`)
}

const handleSubmit = async () => {
  const formElement = formRef.value
  if (!formElement) return

  try {
    await formElement.validate()
    submitLoading.value = true
    const data = {
      title: form.title,
      description: form.description,
      categoryId: form.categoryId,
      type: form.type,
      visibility: form.type === 2 ? form.visibility : 1,
      status: form.status
    }
    if (form.id) {
      await interviewApi.updateQuestionSet(form.id, data)
      ElMessage.success('编辑成功')
    } else {
      await interviewApi.createQuestionSet(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await fetchQuestionSets()
  } catch (error) {
    if (error?.name !== 'ValidationError') {
      console.error('提交失败:', error)
      ElMessage.error('操作失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    id: null,
    title: '',
    description: '',
    categoryId: null,
    type: 1,
    visibility: 1,
    status: 1
  })
}

onMounted(() => {
  fetchCategories()
  fetchQuestionSets()
})
</script>

<style scoped>
.interview-question-sets-page {
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
