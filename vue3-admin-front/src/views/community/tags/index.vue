<template>
  <CnPage class="community-tags-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="标签管理"
      description="维护社区帖子标签、使用统计和展示排序，提升内容检索与聚合效率。"
      eyebrow="Community Tags"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">社区管理</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ total }} 个标签</CnStatusTag>
        <CnStatusTag type="success">启用 {{ enabledCountInPage }} 个</CnStatusTag>
        <CnStatusTag type="info">使用 {{ useCountInPage }} 次</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="fetchTagList">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="showAddDialog">新建标签</el-button>
      </template>
    </CnPageHeader>

    <div class="community-stat-grid">
      <CnStatCard title="标签总量" :value="total" description="当前筛选条件下的标签数量" tone="brand" />
      <CnStatCard title="启用标签" :value="enabledCountInPage" description="当前页可用于帖子聚合的标签" tone="success" />
      <CnStatCard title="已使用标签" :value="usedTagCountInPage" description="当前页已有帖子使用的标签数量" tone="warning" />
      <CnStatCard title="累计使用" :value="useCountInPage" description="当前页标签累计使用次数" tone="info" />
    </div>

    <CnSection title="筛选条件" description="按标签名称和启用状态快速定位标签。" divided>
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

    <CnSection title="标签列表" :description="`共 ${total} 个标签`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tagList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="标签数据" description="使用次数大于 0 的标签不能直接删除，请先完成内容清理或迁移。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pageParams.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="info" size="sm">使用 {{ useCountInPage }} 次</CnStatusTag>
            </template>

            <el-button type="primary" :icon="Plus" @click="showAddDialog">新建标签</el-button>
          </CnToolbar>
        </template>

        <template #name="{ row }">
          <div class="tag-cell">
            <CnStatusTag type="success" size="sm"># {{ row.name || '-' }}</CnStatusTag>
            <span>ID {{ row.id }}</span>
          </div>
        </template>

        <template #description="{ row }">
          <span class="muted-text">{{ row.description || '暂无描述' }}</span>
        </template>

        <template #useCount="{ row }">
          <CnStatusTag :type="Number(row.useCount) > 0 ? 'info' : 'neutral'" size="sm">
            {{ row.useCount || 0 }} 次
          </CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="row.status === 1 ? 'success' : 'danger'" size="sm">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="showEditDialog(row)">编辑</el-button>
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
              :disabled="Number(row.useCount) > 0"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无标签"
            description="当前筛选条件下没有社区标签，可以重置筛选或新建标签。"
            icon="TG"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="handleReset">重置筛选</el-button>
              <el-button type="primary" @click="showAddDialog">新建标签</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="标签描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入标签描述"
            :rows="3"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <div class="sort-field">
            <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
            <span>数字越小越靠前</span>
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
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

interface TagRecord {
  id: number
  name: string
  description?: string
  useCount?: number
  sortOrder?: number
  status: number
  createTime?: string
  [key: string]: unknown
}

interface SearchForm {
  keyword: string
  status: number | null
}

interface TagForm {
  id: number | null
  name: string
  description: string
  sortOrder: number
  status: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '社区管理' }, { label: '标签管理' }]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const tagList = ref<TagRecord[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()

const searchForm = reactive<SearchForm>({
  keyword: '',
  status: null
})

const pageParams = reactive({
  pageNum: 1,
  pageSize: 10
})

const form = reactive<TagForm>({
  id: null,
  name: '',
  description: '',
  sortOrder: 0,
  status: 1
})

const rules: FormRules<TagForm> = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { min: 2, max: 20, message: '标签名称长度为2-20个字符', trigger: 'blur' }
  ],
  description: [{ max: 200, message: '标签描述最多200个字符', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const filterFields: CnFilterField[] = [
  { prop: 'keyword', label: '标签名称', type: 'input', placeholder: '请输入标签名称' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '全部',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  }
]

const tableColumns: CnTableColumn<TagRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '标签名称', minWidth: 150, slot: 'name', showOverflowTooltip: true },
  { prop: 'description', label: '标签描述', minWidth: 220, slot: 'description', showOverflowTooltip: true },
  { prop: 'useCount', label: '使用次数', width: 120, align: 'center', slot: 'useCount' },
  { prop: 'sortOrder', label: '排序', width: 100, align: 'right' },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 190, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pageParams.pageNum,
  pageSize: pageParams.pageSize,
  total: total.value,
  pageSizes: [10, 20, 50, 100]
}))

const enabledCountInPage = computed(() => tagList.value.filter((item) => item.status === 1).length)
const usedTagCountInPage = computed(() => tagList.value.filter((item) => Number(item.useCount) > 0).length)
const useCountInPage = computed(() => tagList.value.reduce((sum, item) => sum + (Number(item.useCount) || 0), 0))

onMounted(() => {
  fetchTagList()
})

const fetchTagList = async () => {
  loading.value = true
  try {
    const response = await communityApi.getTagList({
      ...pageParams,
      ...searchForm
    })
    tagList.value = response?.records || []
    total.value = response?.total || 0
  } catch (error) {
    console.error('获取标签列表失败:', error)
    ElMessage.error('获取标签列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageParams.pageNum = 1
  fetchTagList()
}

const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    status: null
  })
  pageParams.pageNum = 1
  fetchTagList()
}

const handleSearchFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(searchForm, value)
}

const handleSizeChange = (size: number) => {
  pageParams.pageSize = size
  pageParams.pageNum = 1
  fetchTagList()
}

const handleCurrentChange = (page: number) => {
  pageParams.pageNum = page
  fetchTagList()
}

const showAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '新建标签'
  resetForm()
  dialogVisible.value = true
}

const showEditDialog = (row: TagRecord) => {
  isEdit.value = true
  dialogTitle.value = '编辑标签'
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    description: row.description || '',
    sortOrder: Number(row.sortOrder) || 0,
    status: Number(row.status) === 0 ? 0 : 1
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    name: '',
    description: '',
    sortOrder: 0,
    status: 1
  })
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value && form.id !== null) {
      await communityApi.updateTag(form.id, { ...form })
      ElMessage.success('更新标签成功')
    } else {
      await communityApi.createTag({ ...form })
      ElMessage.success('创建标签成功')
    }

    dialogVisible.value = false
    fetchTagList()
  } catch (error) {
    console.error(isEdit.value ? '更新标签失败:' : '创建标签失败:', error)
    ElMessage.error(isEdit.value ? '更新标签失败' : '创建标签失败')
  } finally {
    submitting.value = false
  }
}

const toggleStatus = async (row: TagRecord) => {
  try {
    await ElMessageBox.confirm(`确认${row.status === 1 ? '禁用' : '启用'}标签「${row.name}」吗？`, '确认操作', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await communityApi.toggleTagStatus(row.id)
    ElMessage.success('操作成功')
    fetchTagList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('切换标签状态失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const handleDelete = async (row: TagRecord) => {
  if (Number(row.useCount) > 0) {
    ElMessage.warning('该标签已被使用，无法删除')
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除标签「${row.name}」吗？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await communityApi.deleteTag(row.id)
    ElMessage.success('删除成功')

    if (tagList.value.length === 1 && pageParams.pageNum > 1) {
      pageParams.pageNum -= 1
    }

    fetchTagList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除标签失败:', error)
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped>
.community-tags-page {
  min-height: 100%;
}

.community-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.tag-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.tag-cell span,
.muted-text,
.sort-field span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.sort-field {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-3);
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
