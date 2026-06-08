<template>
  <CnPage class="mock-interview-directions-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="面试方向配置"
      description="维护模拟面试可用方向、排序、状态和题库分类映射。"
      eyebrow="Mock Interview Directions"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">模拟面试</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ tableData.length }} 个方向</CnStatusTag>
        <CnStatusTag type="success">启用 {{ enabledCount }} 个</CnStatusTag>
        <CnStatusTag type="warning">停用 {{ disabledCount }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增方向</el-button>
      </template>
    </CnPageHeader>

    <div class="mock-stat-grid">
      <CnStatCard title="方向总数" :value="tableData.length" description="模拟面试可选方向配置总数" tone="brand" />
      <CnStatCard title="启用方向" :value="enabledCount" description="用户端当前可选择的方向" tone="success" />
      <CnStatCard title="停用方向" :value="disabledCount" description="暂不对用户开放的方向" tone="warning" />
      <CnStatCard title="分类映射" :value="mappedCategoryCount" description="已填写分类 ID 映射的方向" tone="info" />
    </div>

    <CnSection title="方向列表" :description="`共 ${tableData.length} 个方向配置`" divided>
      <CnDataTable :columns="tableColumns" :data="tableData" :loading="loading" :pagination="null" row-key="id">
        <template #toolbar>
          <CnToolbar title="方向配置" description="方向代码创建后不可编辑，状态切换会影响用户端入口。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">启用 {{ enabledCount }} 个</CnStatusTag>
              <CnStatusTag type="info" size="sm">分类映射 {{ mappedCategoryCount }} 个</CnStatusTag>
            </template>
            <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增方向</el-button>
          </CnToolbar>
        </template>

        <template #directionName="{ row }">
          <div class="direction-name">
            <span class="direction-icon">{{ row.icon || '-' }}</span>
            <span>{{ row.directionName }}</span>
          </div>
        </template>

        <template #status="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :loading="statusLoadingIds.includes(row.id)"
            @change="(value) => handleToggleStatus(row, Boolean(value))"
          />
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="方向名称" prop="directionName">
          <el-input v-model="formData.directionName" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="方向代码" prop="directionCode">
          <el-input v-model="formData.directionCode" :disabled="dialogMode === 'edit'" placeholder="如：java / frontend" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="formData.icon" placeholder="如：Java / FE" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="分类ID" prop="categoryIds">
          <el-input v-model="formData.categoryIds" placeholder="多个ID逗号分隔，如：1,2,3" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" controls-position="right" class="sort-order-control" />
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
import mockInterviewAdminApi from '@/api/mockInterview'
import {
  CnDataTable,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn } from '@/design-system'

interface DirectionRecord {
  id: number | string
  directionName: string
  directionCode: string
  icon?: string
  description?: string
  categoryIds?: string
  sortOrder?: number
  status?: number
  createTime?: string
  [key: string]: unknown
}

interface DirectionForm {
  id: number | string | null
  directionName: string
  directionCode: string
  icon: string
  description: string
  categoryIds: string
  sortOrder: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '模拟面试' }, { label: '方向配置' }]

const loading = ref(false)
const submitLoading = ref(false)
const statusLoadingIds = ref<Array<number | string>>([])
const tableData = ref<DirectionRecord[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formRef = ref()

const defaultForm = (): DirectionForm => ({
  id: null,
  directionName: '',
  directionCode: '',
  icon: '',
  description: '',
  categoryIds: '',
  sortOrder: 0
})

const formData = reactive<DirectionForm>(defaultForm())

const formRules = {
  directionName: [{ required: true, message: '请输入方向名称', trigger: 'blur' }],
  directionCode: [{ required: true, message: '请输入方向代码', trigger: 'blur' }]
}

const tableColumns: CnTableColumn<DirectionRecord>[] = [
  { prop: 'id', label: 'ID', minWidth: 80 },
  { prop: 'directionName', label: '方向名称', minWidth: 160, slot: 'directionName', showOverflowTooltip: true },
  { prop: 'directionCode', label: '方向代码', minWidth: 140, showOverflowTooltip: true },
  { prop: 'description', label: '描述', minWidth: 260, showOverflowTooltip: true },
  { prop: 'categoryIds', label: '分类ID', minWidth: 180, showOverflowTooltip: true },
  { prop: 'sortOrder', label: '排序', minWidth: 90, align: 'center' },
  { prop: 'status', label: '状态', minWidth: 100, align: 'center', slot: 'status' },
  { prop: 'createTime', label: '创建时间', minWidth: 170, showOverflowTooltip: true },
  { label: '操作', width: 130, fixed: 'right', slot: 'actions' }
]

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增方向' : '编辑方向'))
const enabledCount = computed(() => tableData.value.filter((item) => item.status === 1).length)
const disabledCount = computed(() => tableData.value.filter((item) => item.status !== 1).length)
const mappedCategoryCount = computed(() => tableData.value.filter((item) => String(item.categoryIds || '').trim()).length)

const resetForm = () => {
  const next = defaultForm()
  Object.keys(next).forEach((key) => {
    formData[key as keyof DirectionForm] = next[key as keyof DirectionForm] as never
  })
}

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await mockInterviewAdminApi.getDirections()
  } catch {
    ElMessage.error('加载方向列表失败')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row: DirectionRecord) => {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(formData, {
    id: row.id,
    directionName: row.directionName,
    directionCode: row.directionCode,
    icon: row.icon || '',
    description: row.description || '',
    categoryIds: row.categoryIds || '',
    sortOrder: row.sortOrder ?? 0
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true
    const payload = {
      directionName: formData.directionName,
      directionCode: formData.directionCode,
      icon: formData.icon,
      description: formData.description,
      categoryIds: formData.categoryIds,
      sortOrder: formData.sortOrder
    }
    if (dialogMode.value === 'create') {
      await mockInterviewAdminApi.createDirection(payload)
      ElMessage.success('新增成功')
    } else {
      await mockInterviewAdminApi.updateDirection(formData.id, payload)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    if (error?.name !== 'ValidationError') {
      ElMessage.error(error?.message || '提交失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: DirectionRecord) => {
  try {
    await ElMessageBox.confirm(`确认删除方向「${row.directionName}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await mockInterviewAdminApi.deleteDirection(row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const handleToggleStatus = async (row: DirectionRecord, value: boolean) => {
  statusLoadingIds.value.push(row.id)
  try {
    await mockInterviewAdminApi.updateDirectionStatus(row.id, value ? 1 : 0)
    row.status = value ? 1 : 0
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error(error?.message || '状态更新失败')
  } finally {
    statusLoadingIds.value = statusLoadingIds.value.filter((id) => id !== row.id)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.mock-interview-directions-page {
  min-height: 100%;
}

.mock-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.direction-name,
.table-actions {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
}

.direction-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 6px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.table-actions {
  flex-wrap: wrap;
}

.table-actions .el-button {
  margin-left: 0;
}

@media (max-width: 1180px) {
  .mock-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .mock-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
