<template>
  <CnPage class="community-categories-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="分类管理"
      description="维护社区内容分类、排序和启用状态，帮助帖子按主题稳定沉淀。"
      eyebrow="Community Categories"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">社区管理</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 个分类</CnStatusTag>
        <CnStatusTag type="success">启用 {{ enabledCountInPage }} 个</CnStatusTag>
        <CnStatusTag type="info">帖子 {{ postCountInPage }} 篇</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="fetchCategoryList">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">创建分类</el-button>
      </template>
    </CnPageHeader>

    <div class="community-stat-grid">
      <CnStatCard title="分类总量" :value="pagination.total" description="当前筛选条件下的社区分类数量" tone="brand" />
      <CnStatCard title="启用分类" :value="enabledCountInPage" description="当前页可用于发帖选择的分类" tone="success" />
      <CnStatCard title="有帖分类" :value="usedCategoryCountInPage" description="当前页已产生内容的分类数量" tone="warning" />
      <CnStatCard title="帖子总数" :value="postCountInPage" description="当前页分类下累计帖子数量" tone="info" />
    </div>

    <CnSection title="筛选条件" description="按分类名称、描述关键词和状态快速定位分类。" divided>
      <CnFilterForm
        :model-value="searchForm"
        :fields="filterFields"
        :columns="4"
        :loading="loading"
        @update:model-value="handleSearchFormUpdate"
        @search="handleSearch"
        @reset="resetSearch"
      />
    </CnSection>

    <CnSection title="分类列表" :description="`共 ${pagination.total} 个分类`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="categoryList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="分类数据" description="分类排序和启用状态会影响用户发帖时的内容归类入口。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="info" size="sm">帖子 {{ postCountInPage }} 篇</CnStatusTag>
            </template>

            <el-button type="primary" :icon="Plus" @click="handleAdd">创建分类</el-button>
          </CnToolbar>
        </template>

        <template #name="{ row }">
          <div class="category-cell">
            <strong>{{ row.name || '-' }}</strong>
            <span>ID {{ row.id }}</span>
          </div>
        </template>

        <template #description="{ row }">
          <span class="muted-text">{{ row.description || '暂无描述' }}</span>
        </template>

        <template #postCount="{ row }">
          <CnStatusTag :type="Number(row.postCount) > 0 ? 'info' : 'neutral'" size="sm">
            {{ row.postCount || 0 }} 篇
          </CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="row.status === 1 ? 'success' : 'danger'" size="sm">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              :icon="Delete"
              :disabled="Number(row.postCount) > 0"
              @click="deleteCategory(row)"
            >
              删除
            </el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无分类"
            description="当前筛选条件下没有社区分类，可以重置筛选或创建新分类。"
            icon="CT"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="resetSearch">重置筛选</el-button>
              <el-button type="primary" @click="handleAdd">创建分类</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="categoryForm" :rules="formRules" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="分类描述" prop="description">
          <el-input
            v-model="categoryForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分类描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="排序顺序" prop="sortOrder">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" :max="999" class="full-width-control" />
        </el-form-item>
        <el-form-item v-if="editingCategoryId !== null" label="状态" prop="status">
          <el-radio-group v-model="categoryForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">
            {{ editingCategoryId !== null ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
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

interface CategoryRecord {
  id: number
  name: string
  description?: string
  sortOrder?: number
  postCount?: number
  status: number
  createTime?: string
  creatorName?: string
  [key: string]: unknown
}

interface SearchForm {
  keyword: string
  status: number | null
}

interface CategoryForm {
  name: string
  description: string
  sortOrder: number
  status: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '社区管理' }, { label: '分类管理' }]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('创建分类')
const editingCategoryId = ref<number | null>(null)
const categoryList = ref<CategoryRecord[]>([])
const formRef = ref<FormInstance>()

const searchForm = reactive<SearchForm>({
  keyword: '',
  status: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const categoryForm = reactive<CategoryForm>({
  name: '',
  description: '',
  sortOrder: 0,
  status: 1
})

const formRules: FormRules<CategoryForm> = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 50, message: '分类名称长度不能超过50个字符', trigger: 'blur' }
  ],
  description: [{ max: 200, message: '分类描述长度不能超过200个字符', trigger: 'blur' }],
  sortOrder: [
    { required: true, message: '请输入排序顺序', trigger: 'blur' },
    { type: 'number', min: 0, max: 999, message: '排序顺序范围为0-999', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const filterFields: CnFilterField[] = [
  { prop: 'keyword', label: '分类关键词', type: 'input', placeholder: '搜索分类名称或描述' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '状态筛选',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  }
]

const tableColumns: CnTableColumn<CategoryRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '分类名称', minWidth: 150, slot: 'name', showOverflowTooltip: true },
  { prop: 'description', label: '描述', minWidth: 220, slot: 'description', showOverflowTooltip: true },
  { prop: 'sortOrder', label: '排序', width: 90, align: 'right' },
  { prop: 'postCount', label: '帖子数', width: 110, slot: 'postCount', align: 'center' },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { prop: 'creatorName', label: '创建者', width: 120, showOverflowTooltip: true },
  { label: '操作', width: 190, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.pageNum,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const enabledCountInPage = computed(() => categoryList.value.filter((item) => item.status === 1).length)
const usedCategoryCountInPage = computed(() => categoryList.value.filter((item) => Number(item.postCount) > 0).length)
const postCountInPage = computed(() => categoryList.value.reduce((sum, item) => sum + (Number(item.postCount) || 0), 0))

onMounted(() => {
  fetchCategoryList()
})

const fetchCategoryList = async () => {
  loading.value = true
  try {
    const response = await communityApi.getCategoryList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    categoryList.value = response?.records || []
    pagination.total = response?.total || 0
  } catch (error) {
    console.error('获取分类列表失败:', error)
    ElMessage.error('获取分类列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchCategoryList()
}

const resetSearch = () => {
  Object.assign(searchForm, {
    keyword: '',
    status: null
  })
  pagination.pageNum = 1
  fetchCategoryList()
}

const handleSearchFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(searchForm, value)
}

const handlePageChange = (page: number) => {
  pagination.pageNum = page
  fetchCategoryList()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchCategoryList()
}

const handleAdd = () => {
  dialogTitle.value = '创建分类'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: CategoryRecord) => {
  dialogTitle.value = '编辑分类'
  editingCategoryId.value = row.id
  Object.assign(categoryForm, {
    name: row.name || '',
    description: row.description || '',
    sortOrder: Number(row.sortOrder) || 0,
    status: Number(row.status) === 0 ? 0 : 1
  })
  dialogVisible.value = true
}

const toggleStatus = async (row: CategoryRecord) => {
  try {
    await ElMessageBox.confirm(`确定要${row.status === 1 ? '禁用' : '启用'}分类 "${row.name}" 吗？`, '状态切换确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await communityApi.toggleCategoryStatus(row.id)
    ElMessage.success(`${row.status === 1 ? '禁用' : '启用'}成功`)
    fetchCategoryList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('状态切换失败:', error)
      ElMessage.error('状态切换失败')
    }
  }
}

const deleteCategory = async (row: CategoryRecord) => {
  try {
    await ElMessageBox.confirm(`确定要删除分类 "${row.name}" 吗？该操作不可撤销。`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await communityApi.deleteCategory(row.id)
    ElMessage.success('删除成功')
    fetchCategoryList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除分类失败:', error)
      ElMessage.error('删除分类失败')
    }
  }
}

const submitForm = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editingCategoryId.value !== null) {
      await communityApi.updateCategory(editingCategoryId.value, { ...categoryForm })
      ElMessage.success('更新成功')
    } else {
      await communityApi.createCategory({ ...categoryForm })
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    fetchCategoryList()
  } catch (error) {
    console.error(editingCategoryId.value !== null ? '更新分类失败:' : '创建分类失败:', error)
    ElMessage.error(editingCategoryId.value !== null ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  editingCategoryId.value = null
  Object.assign(categoryForm, {
    name: '',
    description: '',
    sortOrder: 0,
    status: 1
  })
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.community-categories-page {
  min-height: 100%;
}

.community-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.category-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.category-cell strong {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-cell span,
.muted-text {
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

.dialog-footer {
  justify-content: flex-end;
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
