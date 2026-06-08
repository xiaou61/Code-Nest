<template>
  <CnPage class="interview-categories-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="分类管理"
      description="管理面试题分类，支持分类排序、状态控制和题单数量约束。"
      eyebrow="Interview Categories"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">面试题库</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ categoryList.length }} 个分类</CnStatusTag>
        <CnStatusTag type="success">启用 {{ enabledCount }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增分类</el-button>
      </template>
    </CnPageHeader>

    <div class="interview-stat-grid">
      <CnStatCard title="分类总数" :value="categoryList.length" description="当前筛选条件下的分类数量" tone="brand" />
      <CnStatCard title="启用分类" :value="enabledCount" description="可用于题单归类的分类" tone="success" />
      <CnStatCard title="禁用分类" :value="disabledCount" description="当前不可选用的分类" tone="warning" />
      <CnStatCard title="题单总量" :value="questionSetCount" description="当前分类关联题单总数" tone="info" />
    </div>

    <CnSection title="筛选条件" description="按分类名称和状态筛选面试题分类。" divided>
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

    <CnSection title="分类列表" :description="`共 ${categoryList.length} 个分类`" divided>
      <CnDataTable :columns="tableColumns" :data="categoryList" :loading="loading" :pagination="null" row-key="id">
        <template #questionSetCount="{ row }">
          <CnStatusTag type="info" size="sm">{{ row.questionSetCount || 0 }}</CnStatusTag>
        </template>

        <template #status="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" :icon="Delete" :disabled="row.questionSetCount > 0" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="分类描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入分类描述" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="排序序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" placeholder="请输入排序序号" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'
import {
  CnDataTable,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnBreadcrumbItem, CnFilterField, CnTableColumn } from '@/design-system'

interface InterviewCategory {
  id: number
  name: string
  description?: string
  sortOrder?: number
  questionSetCount?: number
  status: number
  createTime?: string
  [key: string]: unknown
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '面试题目管理' }, { label: '分类管理' }]

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const categoryList = ref<InterviewCategory[]>([])
const formRef = ref()

const searchForm = reactive({
  name: '',
  status: null as number | null
})

const form = reactive({
  id: null as number | null,
  name: '',
  description: '',
  sortOrder: 0,
  status: 1
})

const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 100, message: '分类名称长度不能超过100字符', trigger: 'blur' }
  ],
  description: [{ max: 500, message: '分类描述长度不能超过500字符', trigger: 'blur' }],
  sortOrder: [{ type: 'number', message: '排序序号必须为数字', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const filterFields: CnFilterField[] = [
  { prop: 'name', label: '分类名称', type: 'input', placeholder: '请输入分类名称' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  }
]

const tableColumns: CnTableColumn<InterviewCategory>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '分类名称', minWidth: 150, showOverflowTooltip: true },
  { prop: 'description', label: '分类描述', minWidth: 220, showOverflowTooltip: true },
  { prop: 'sortOrder', label: '排序', width: 90, align: 'center' },
  { prop: 'questionSetCount', label: '题单数量', width: 110, align: 'center', slot: 'questionSetCount' },
  { prop: 'status', label: '状态', width: 100, align: 'center', slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 140, fixed: 'right', slot: 'actions' }
]

const dialogTitle = computed(() => (form.id ? '编辑分类' : '新增分类'))
const enabledCount = computed(() => categoryList.value.filter((item) => item.status === 1).length)
const disabledCount = computed(() => categoryList.value.filter((item) => item.status === 0).length)
const questionSetCount = computed(() => categoryList.value.reduce((sum, item) => sum + (Number(item.questionSetCount) || 0), 0))

const fetchCategories = async () => {
  loading.value = true
  try {
    const data = await interviewApi.getAllCategories()
    let filteredData: InterviewCategory[] = data || []
    if (searchForm.name) {
      filteredData = filteredData.filter((item) => item.name.includes(searchForm.name))
    }
    if (searchForm.status !== null) {
      filteredData = filteredData.filter((item) => item.status === searchForm.status)
    }
    categoryList.value = filteredData.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  } catch (error) {
    console.error('获取分类列表失败:', error)
    ElMessage.error('获取分类列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilterUpdate = (value: Record<string, unknown>) => {
  Object.assign(searchForm, value)
}

const handleSearch = () => {
  fetchCategories()
}

const handleReset = () => {
  Object.assign(searchForm, { name: '', status: null })
  fetchCategories()
}

const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: InterviewCategory) => {
  form.id = row.id
  form.name = row.name
  form.description = row.description || ''
  form.sortOrder = row.sortOrder || 0
  form.status = row.status
  dialogVisible.value = true
}

const handleDelete = async (row: InterviewCategory) => {
  if ((row.questionSetCount || 0) > 0) {
    ElMessage.warning('该分类下还有题单，不能删除')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除分类 "${row.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await interviewApi.deleteCategory(row.id)
    ElMessage.success('删除成功')
    await fetchCategories()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除分类失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleStatusChange = async (row: InterviewCategory) => {
  try {
    await interviewApi.updateCategory(row.id, {
      name: row.name,
      description: row.description,
      sortOrder: row.sortOrder,
      status: row.status
    })
    ElMessage.success('状态更新成功')
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('状态更新失败')
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleSubmit = async () => {
  const formElement = formRef.value
  if (!formElement) return

  try {
    await formElement.validate()
    submitLoading.value = true
    const data = {
      name: form.name,
      description: form.description,
      sortOrder: form.sortOrder,
      status: form.status
    }
    if (form.id) {
      await interviewApi.updateCategory(form.id, data)
      ElMessage.success('编辑成功')
    } else {
      await interviewApi.createCategory(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await fetchCategories()
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
    name: '',
    description: '',
    sortOrder: 0,
    status: 1
  })
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.interview-categories-page {
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
