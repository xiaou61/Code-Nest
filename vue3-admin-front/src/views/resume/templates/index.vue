<template>
  <CnPage class="resume-template-admin" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="简历模板管理"
      description="维护模板基础信息、分类、经验层级、标签和启用状态。"
      eyebrow="Resume Templates"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">简历中心</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 个模板</CnStatusTag>
        <CnStatusTag type="success">启用 {{ enabledCountInPage }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新建模板</el-button>
      </template>
    </CnPageHeader>

    <div class="resume-stat-grid">
      <CnStatCard title="模板总量" :value="pagination.total" description="当前筛选条件下的模板数量" tone="brand" />
      <CnStatCard title="当前页启用" :value="enabledCountInPage" description="当前页可用模板数量" tone="success" />
      <CnStatCard title="当前页下载" :value="downloadCountInPage" description="当前页模板累计下载量" tone="info" />
      <CnStatCard title="平均评分" :value="avgRatingInPage" description="当前页模板平均评分" tone="warning" />
    </div>

    <CnSection title="筛选条件" description="按模板名称、标签、分类和状态筛选简历模板。" divided>
      <CnFilterForm
        :model-value="searchForm"
        :fields="filterFields"
        :columns="3"
        :loading="loading"
        @update:model-value="handleFilterUpdate"
        @search="handleSearch"
        @reset="handleReset"
      />
    </CnSection>

    <CnSection title="模板列表" :description="`共 ${pagination.total} 个模板`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="templateList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @page-change="handlePageChange"
        @page-size-change="handleSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="模板数据" description="启用状态会影响用户端模板选择，预览可检查封面和详情信息。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.size }} 条</CnStatusTag>
              <CnStatusTag type="info" size="sm">下载 {{ downloadCountInPage }} 次</CnStatusTag>
            </template>
            <el-button type="primary" :icon="Plus" @click="handleAdd">新建模板</el-button>
          </CnToolbar>
        </template>

        <template #experienceLevel="{ row }">
          <CnStatusTag type="info" size="sm">{{ formatExperience(row.experienceLevel) }}</CnStatusTag>
        </template>

        <template #rating="{ row }">
          <el-rate v-model="row.rating" disabled show-score text-color="var(--cn-color-warning)" />
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="row.status === 1 ? 'success' : 'neutral'" size="sm">
            {{ row.status === 1 ? '启用' : '下线' }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="View" @click="handlePreview(row)">预览</el-button>
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑模板' : '新建模板'" width="640px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="经验层级" prop="experienceLevel">
          <el-select v-model="form.experienceLevel">
            <el-option v-for="item in experienceOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面地址" prop="coverUrl">
          <el-input v-model="form.coverUrl" placeholder="建议使用COS链接" />
        </el-form-item>
        <el-form-item label="效果预览" prop="previewUrl">
          <el-input v-model="form.previewUrl" placeholder="可填写高清图/在线预览链接" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-select v-model="form.tags" multiple filterable allow-create default-first-option placeholder="输入标签后回车" />
        </el-form-item>
        <el-form-item label="技术栈">
          <el-input v-model="form.techStack" placeholder="如：Vue3,TS,Node.js" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">下线</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="模板简介">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="previewVisible" title="模板概览" size="40%">
      <div v-if="currentTemplate">
        <el-image
          :src="currentTemplate.previewUrl || currentTemplate.coverUrl"
          fit="cover"
          class="template-preview-image"
          :preview-src-list="[currentTemplate.previewUrl || currentTemplate.coverUrl]"
        />
        <el-descriptions :column="1" border class="template-descriptions">
          <el-descriptions-item label="模板名称">{{ currentTemplate.name }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ currentTemplate.category || '通用' }}</el-descriptions-item>
          <el-descriptions-item label="经验层级">{{ formatExperience(currentTemplate.experienceLevel) }}</el-descriptions-item>
          <el-descriptions-item label="标签">
            <CnStatusTag v-for="tag in parseTags(currentTemplate.tags)" :key="tag" type="info" size="sm" class="tag-gap">
              {{ tag }}
            </CnStatusTag>
          </el-descriptions-item>
          <el-descriptions-item label="技术栈">{{ currentTemplate.techStack || '未配置' }}</el-descriptions-item>
          <el-descriptions-item label="模板说明">{{ currentTemplate.description || '暂无描述' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, View } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'
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

interface ResumeTemplate {
  id: number | string
  name: string
  category?: string
  description?: string
  coverUrl?: string
  previewUrl?: string
  tags?: string
  techStack?: string
  experienceLevel?: number
  rating?: number
  downloadCount?: number
  status?: number
  updateTime?: string
  [key: string]: unknown
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '简历中心' }, { label: '模板管理' }]

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const currentTemplate = ref<ResumeTemplate | null>(null)
const templateList = ref<ResumeTemplate[]>([])
const formRef = ref()

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const searchForm = reactive({
  keyword: '',
  category: '',
  status: '' as number | ''
})

const categoryOptions = [
  { label: '前端', value: '前端' },
  { label: '后端', value: '后端' },
  { label: '全栈', value: '全栈' },
  { label: '算法', value: '算法' },
  { label: '移动端', value: '移动端' },
  { label: '测试', value: '测试' }
]

const experienceOptions = [
  { label: '应届 / 实习', value: 1 },
  { label: '初级工程师', value: 2 },
  { label: '中级工程师', value: 3 },
  { label: '高级工程师', value: 4 },
  { label: '专家 / 管理', value: 5 }
]

const defaultForm = () => ({
  id: null as number | string | null,
  name: '',
  category: '',
  description: '',
  coverUrl: '',
  previewUrl: '',
  tags: [] as string[],
  techStack: '',
  experienceLevel: 1,
  status: 1
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  experienceLevel: [{ required: true, message: '请选择经验层级', trigger: 'change' }]
}

const filterFields: CnFilterField[] = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '模板名称 / 标签' },
  { prop: 'category', label: '分类', type: 'select', placeholder: '请选择分类', options: categoryOptions },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '启用', value: 1 },
      { label: '下线', value: 0 }
    ]
  }
]

const tableColumns: CnTableColumn<ResumeTemplate>[] = [
  { type: 'index', label: '#', width: 60 },
  { prop: 'name', label: '模板名称', minWidth: 170, showOverflowTooltip: true },
  { prop: 'category', label: '分类', width: 120, showOverflowTooltip: true },
  { prop: 'experienceLevel', label: '经验', width: 120, align: 'center', slot: 'experienceLevel' },
  { prop: 'rating', label: '评分', width: 150, slot: 'rating' },
  { prop: 'downloadCount', label: '下载', width: 100, align: 'center' },
  { prop: 'status', label: '状态', width: 110, align: 'center', slot: 'status' },
  { prop: 'updateTime', label: '更新时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 170, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.page,
  pageSize: pagination.size,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const enabledCountInPage = computed(() => templateList.value.filter((item) => item.status === 1).length)
const downloadCountInPage = computed(() => templateList.value.reduce((sum, item) => sum + (Number(item.downloadCount) || 0), 0))
const avgRatingInPage = computed(() => {
  if (!templateList.value.length) return '0.0'
  const total = templateList.value.reduce((sum, item) => sum + (Number(item.rating) || 0), 0)
  return (total / templateList.value.length).toFixed(1)
})

const fetchTemplates = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword || undefined,
      category: searchForm.category || undefined,
      status: searchForm.status !== '' ? searchForm.status : undefined
    }
    const result = await resumeApi.getTemplates(params)
    templateList.value = result?.records || []
    pagination.total = result?.total || templateList.value.length
  } catch {
    ElMessage.error('加载模板失败')
  } finally {
    loading.value = false
  }
}

const handleFilterUpdate = (value: Record<string, unknown>) => {
  Object.assign(searchForm, value)
}

const handleSearch = () => {
  pagination.page = 1
  fetchTemplates()
}

const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    category: '',
    status: ''
  })
  pagination.page = 1
  fetchTemplates()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  fetchTemplates()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  fetchTemplates()
}

const handleAdd = () => {
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

const handleEdit = (row: ResumeTemplate) => {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    category: row.category,
    description: row.description,
    coverUrl: row.coverUrl,
    previewUrl: row.previewUrl,
    tags: parseTags(row.tags),
    techStack: row.techStack,
    experienceLevel: row.experienceLevel,
    status: row.status
  })
  dialogVisible.value = true
}

const handlePreview = (row: ResumeTemplate) => {
  currentTemplate.value = row
  previewVisible.value = true
}

const handleDelete = async (row: ResumeTemplate) => {
  try {
    await ElMessageBox.confirm(`确认删除模板《${row.name}》吗？`, '提示', { type: 'warning' })
    await resumeApi.deleteTemplate(row.id)
    ElMessage.success('删除成功')
    fetchTemplates()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    const payload = {
      name: form.name,
      category: form.category,
      description: form.description,
      coverUrl: form.coverUrl,
      previewUrl: form.previewUrl,
      tags: (form.tags || []).join(','),
      techStack: form.techStack,
      experienceLevel: form.experienceLevel,
      status: form.status
    }
    if (form.id) {
      await resumeApi.updateTemplate(form.id, payload)
      ElMessage.success('更新成功')
    } else {
      await resumeApi.createTemplate(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchTemplates()
  } catch {
    ElMessage.error('保存失败，请检查表单')
  } finally {
    submitLoading.value = false
  }
}

const parseTags = (tags?: string) => {
  if (!tags) return []
  return tags.split(',').map((tag) => tag.trim()).filter(Boolean)
}

const formatExperience = (level?: number) => {
  const map: Record<number, string> = {
    1: '应届 / 实习',
    2: '初级',
    3: '中级',
    4: '高级',
    5: '专家'
  }
  return map[Number(level)] || '通用'
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, defaultForm())
}

onMounted(() => {
  fetchTemplates()
})
</script>

<style scoped>
.resume-template-admin {
  min-height: 100%;
}

.resume-stat-grid {
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

.template-preview-image {
  width: 100%;
  height: 240px;
  border-radius: var(--cn-radius-card);
}

.template-descriptions {
  margin-top: var(--cn-space-4);
}

.tag-gap {
  margin-right: var(--cn-space-2);
  margin-bottom: var(--cn-space-2);
}

@media (max-width: 1180px) {
  .resume-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .resume-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
