<template>
  <CnPage class="blog-categories-page" surface="transparent" max-width="1180px">
    <CnPageHeader
      title="博客分类"
      description="维护博客文章分类、排序和启用状态。"
      eyebrow="Blog Categories"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">分类 {{ total }} 个</CnStatusTag>
        <CnStatusTag type="success">启用 {{ enabledCount }} 个</CnStatusTag>
      </template>
      <template #actions>
        <el-button :icon="Refresh" @click="getList">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增分类</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="分类列表" :description="`共 ${total} 条分类记录`" divided>
      <el-table :data="categoryList" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="categoryName" label="分类名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="categoryDescription" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column prop="articleCount" label="文章数" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <CnStatusTag :type="row.status === 1 ? 'success' : 'danger'" size="sm">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </CnStatusTag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <CnEmptyState
            title="暂无分类"
            description="新增分类后，用户博客文章可以归入对应主题。"
            icon="BC"
            surface="transparent"
          >
            <template #actions>
              <el-button type="primary" @click="handleAdd">新增分类</el-button>
            </template>
          </CnEmptyState>
        </template>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </CnSection>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="分类名称">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类图标">
          <el-input v-model="form.categoryIcon" placeholder="请输入分类图标" />
        </el-form-item>
        <el-form-item label="分类描述">
          <el-input v-model="form.categoryDescription" type="textarea" placeholder="请输入分类描述" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { createCategory, deleteCategory, getCategoryList, updateCategory } from '@/api/blog'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem } from '@/design-system'

interface CategoryRecord {
  id: number | null
  categoryName: string
  categoryIcon: string
  categoryDescription: string
  sortOrder: number
  articleCount?: number
  status: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '博客管理' }, { label: '分类管理' }]

const categoryList = ref<CategoryRecord[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20
})

const form = reactive<CategoryRecord>({
  id: null,
  categoryName: '',
  categoryIcon: '',
  categoryDescription: '',
  sortOrder: 0,
  status: 1
})

const enabledCount = computed(() => categoryList.value.filter((item) => item.status === 1).length)

const getErrorMessage = (error: unknown, fallback: string) => {
  return error instanceof Error ? error.message || fallback : fallback
}

const getList = async () => {
  try {
    const res = await getCategoryList(queryParams)
    categoryList.value = Array.isArray(res?.records) ? res.records : []
    total.value = Number(res?.total) || 0
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '获取分类列表失败'))
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增分类'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: CategoryRecord) => {
  dialogTitle.value = '编辑分类'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (form.id) {
      await updateCategory(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createCategory(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    getList()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '操作失败'))
  }
}

const handleDelete = async (row: CategoryRecord) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm('确定要删除该分类吗？', '提示', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error, '删除失败'))
    }
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    categoryName: '',
    categoryIcon: '',
    categoryDescription: '',
    sortOrder: 0,
    status: 1
  })
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.blog-categories-page {
  min-height: 100%;
}

.table-actions,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--cn-space-4);
}

.dialog-footer {
  justify-content: flex-end;
}

@media (max-width: 680px) {
  .pagination-container,
  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
