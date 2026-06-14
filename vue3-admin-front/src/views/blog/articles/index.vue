<template>
  <CnPage class="blog-articles-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="文章管理"
      description="审核和维护用户博客文章，支持状态筛选、置顶、下架与删除。"
      eyebrow="Blog Articles"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">文章 {{ total }} 篇</CnStatusTag>
        <CnStatusTag type="success">本页发布 {{ publishedCount }} 篇</CnStatusTag>
        <CnStatusTag type="warning">置顶 {{ topCount }} 篇</CnStatusTag>
      </template>
      <template #actions>
        <el-button :icon="Refresh" @click="getList">刷新</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="筛选条件" description="按用户、分类、状态和关键词定位文章。" divided>
      <el-form :inline="true" :model="queryParams" class="filter-form">
        <el-form-item label="用户ID">
          <el-input v-model="queryParams.userId" placeholder="请输入用户ID" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable>
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.categoryName"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下架" :value="2" />
            <el-option label="已删除" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="标题/内容搜索" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </CnSection>

    <CnSection title="文章列表" :description="`共 ${total} 条文章记录`" divided>
      <el-table :data="articleList" border stripe>
        <el-table-column prop="id" label="文章ID" width="80" />
        <el-table-column prop="userNickname" label="作者" width="120" show-overflow-tooltip />
        <el-table-column prop="title" label="文章标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <CnStatusTag :type="getStatusTone(row.status)" size="sm">
              {{ getStatusText(row.status) }}
            </CnStatusTag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="90">
          <template #default="{ row }">
            <CnStatusTag v-if="row.isTop === 1" type="warning" size="sm">置顶</CnStatusTag>
            <CnStatusTag v-else type="neutral" size="sm" subtle>普通</CnStatusTag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="row.isTop === 0" link type="primary" size="small" @click="handleTop(row)">
                置顶
              </el-button>
              <el-button v-else link type="warning" size="small" @click="handleCancelTop(row)">
                取消置顶
              </el-button>
              <el-button v-if="row.status === 1" link type="warning" size="small" @click="handleUpdateStatus(row, 2)">
                下架
              </el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <CnEmptyState
            title="暂无文章"
            description="当前筛选条件下没有匹配文章，可以重置筛选后再查看。"
            icon="BA"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="resetQuery">重置筛选</el-button>
            </template>
          </CnEmptyState>
        </template>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </CnSection>

    <el-dialog v-model="topDialogVisible" title="设置置顶" width="400px">
      <el-form :model="topForm" label-width="90px">
        <el-form-item label="置顶天数">
          <el-input-number v-model="topForm.duration" :min="1" :max="365" placeholder="请输入天数" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="topDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmTop">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { cancelTop, deleteArticle, getArticleList, getCategoryList, topArticle, updateArticleStatus } from '@/api/blog'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTone } from '@/design-system'

interface BlogArticle {
  id: number
  userNickname?: string
  title?: string
  categoryName?: string
  status?: number
  isTop?: number
  publishTime?: string
}

interface BlogCategory {
  id: number
  categoryName: string
}

interface ArticleQuery {
  userId: number | null
  categoryId: number | null
  status: number | null
  keyword: string
  startTime: string
  endTime: string
  pageNum: number
  pageSize: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '博客管理' }, { label: '文章管理' }]

const articleList = ref<BlogArticle[]>([])
const categories = ref<BlogCategory[]>([])
const total = ref(0)
const topDialogVisible = ref(false)
const currentRow = ref<BlogArticle | null>(null)

const queryParams = reactive<ArticleQuery>({
  userId: null,
  categoryId: null,
  status: null,
  keyword: '',
  startTime: '',
  endTime: '',
  pageNum: 1,
  pageSize: 20
})

const topForm = reactive({
  duration: 7
})

const publishedCount = computed(() => articleList.value.filter((item) => item.status === 1).length)
const topCount = computed(() => articleList.value.filter((item) => item.isTop === 1).length)

const getErrorMessage = (error: unknown, fallback: string) => {
  return error instanceof Error ? error.message || fallback : fallback
}

const getList = async () => {
  try {
    const res = await getArticleList(queryParams)
    articleList.value = Array.isArray(res?.records) ? res.records : []
    total.value = Number(res?.total) || 0
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '获取文章列表失败'))
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategoryList({ pageNum: 1, pageSize: 100 })
    categories.value = Array.isArray(res?.records) ? res.records : []
  } catch (error) {
    console.error('加载分类失败', error)
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  Object.assign(queryParams, {
    userId: null,
    categoryId: null,
    status: null,
    keyword: '',
    startTime: '',
    endTime: '',
    pageNum: 1,
    pageSize: 20
  })
  getList()
}

const handleTop = (row: BlogArticle) => {
  currentRow.value = row
  topDialogVisible.value = true
}

const confirmTop = async () => {
  if (!currentRow.value) return
  try {
    await topArticle(currentRow.value.id, topForm.duration)
    ElMessage.success('置顶成功')
    topDialogVisible.value = false
    getList()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '置顶失败'))
  }
}

const handleCancelTop = async (row: BlogArticle) => {
  try {
    await ElMessageBox.confirm('确定要取消置顶吗？', '提示', { type: 'warning' })
    await cancelTop(row.id)
    ElMessage.success('取消置顶成功')
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error, '取消置顶失败'))
    }
  }
}

const handleUpdateStatus = async (row: BlogArticle, status: number) => {
  try {
    const action = status === 2 ? '下架' : '恢复'
    await ElMessageBox.confirm(`确定要${action}该文章吗？`, '提示', { type: 'warning' })
    await updateArticleStatus(row.id, status)
    ElMessage.success(`${action}成功`)
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error, '操作失败'))
    }
  }
}

const handleDelete = async (row: BlogArticle) => {
  try {
    await ElMessageBox.confirm('确定要删除该文章吗？', '提示', { type: 'warning' })
    await deleteArticle(row.id)
    ElMessage.success('删除成功')
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error, '删除失败'))
    }
  }
}

const getStatusText = (status?: number) => {
  const map: Record<number, string> = {
    0: '草稿',
    1: '已发布',
    2: '已下架',
    3: '已删除'
  }
  return typeof status === 'number' ? map[status] || '未知' : '未知'
}

const getStatusTone = (status?: number): CnTone => {
  const map: Record<number, CnTone> = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return typeof status === 'number' ? map[status] || 'neutral' : 'neutral'
}

onMounted(() => {
  getList()
  loadCategories()
})
</script>

<style scoped>
.blog-articles-page {
  min-height: 100%;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.filter-form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: var(--cn-space-2);
}

.filter-form :deep(.el-input),
.filter-form :deep(.el-select) {
  width: 200px;
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
  .filter-form,
  .filter-form :deep(.el-form-item),
  .filter-form :deep(.el-input),
  .filter-form :deep(.el-select) {
    width: 100%;
  }

  .pagination-container,
  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
