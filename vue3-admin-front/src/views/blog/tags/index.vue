<template>
  <CnPage class="blog-tags-page" surface="transparent" max-width="1120px">
    <CnPageHeader
      title="博客标签"
      description="维护用户文章标签，支持低频标签删除和相似标签合并。"
      eyebrow="Blog Tags"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">标签 {{ total }} 个</CnStatusTag>
        <CnStatusTag type="success">本页使用 {{ usageCount }} 次</CnStatusTag>
      </template>
      <template #actions>
        <el-button :icon="Refresh" @click="getList">刷新</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="标签列表" :description="`共 ${total} 条标签记录`" divided>
      <el-table :data="tagList" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="tagName" label="标签名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="useCount" label="使用次数" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="热度" width="110">
          <template #default="{ row }">
            <CnStatusTag :type="Number(row.useCount) > 10 ? 'success' : 'neutral'" size="sm">
              {{ Number(row.useCount) > 10 ? '高频' : '普通' }}
            </CnStatusTag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" size="small" @click="handleMerge(row)">合并</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <CnEmptyState
            title="暂无标签"
            description="用户发布文章并添加标签后，标签数据会显示在这里。"
            icon="BT"
            surface="transparent"
          />
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

    <el-dialog v-model="mergeDialogVisible" title="合并标签" width="500px">
      <div class="merge-dialog">
        <CnSection surface="plain" compact class="merge-summary">
          <div class="merge-summary__content">
            <CnStatusTag type="warning" size="sm" subtle>不可恢复</CnStatusTag>
            <span>合并后，源标签将被删除，使用源标签的文章将改为使用目标标签。</span>
          </div>
        </CnSection>

        <el-form label-width="120px">
          <el-form-item label="源标签">
            <el-input :value="currentTag?.tagName" disabled />
          </el-form-item>
          <el-form-item label="目标标签">
            <el-select v-model="targetTagId" placeholder="请选择目标标签" filterable>
              <el-option
                v-for="tag in tagList.filter((item) => item.id !== currentTag?.id)"
                :key="tag.id"
                :label="`${tag.tagName} (${tag.useCount || 0}次)`"
                :value="tag.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="mergeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmMerge">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { deleteTag, getTagList, mergeTags } from '@/api/blog'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem } from '@/design-system'

interface TagRecord {
  id: number
  tagName: string
  useCount?: number
  createTime?: string
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '博客管理' }, { label: '标签管理' }]

const tagList = ref<TagRecord[]>([])
const total = ref(0)
const mergeDialogVisible = ref(false)
const currentTag = ref<TagRecord | null>(null)
const targetTagId = ref<number | null>(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20
})

const usageCount = computed(() => tagList.value.reduce((sum, item) => sum + (Number(item.useCount) || 0), 0))

const getErrorMessage = (error: unknown, fallback: string) => {
  return error instanceof Error ? error.message || fallback : fallback
}

const getList = async () => {
  try {
    const res = await getTagList(queryParams)
    tagList.value = Array.isArray(res?.records) ? res.records : []
    total.value = Number(res?.total) || 0
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '获取标签列表失败'))
  }
}

const handleMerge = (row: TagRecord) => {
  currentTag.value = row
  targetTagId.value = null
  mergeDialogVisible.value = true
}

const confirmMerge = async () => {
  if (!currentTag.value) return
  if (!targetTagId.value) {
    ElMessage.warning('请选择目标标签')
    return
  }

  try {
    await ElMessageBox.confirm('确定要合并这两个标签吗？此操作不可恢复', '提示', { type: 'warning' })
    await mergeTags(currentTag.value.id, targetTagId.value)
    ElMessage.success('合并成功')
    mergeDialogVisible.value = false
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error, '合并失败'))
    }
  }
}

const handleDelete = async (row: TagRecord) => {
  try {
    await ElMessageBox.confirm('确定要删除该标签吗？', '提示', { type: 'warning' })
    await deleteTag(row.id)
    ElMessage.success('删除成功')
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error, '删除失败'))
    }
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.blog-tags-page {
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

.merge-dialog {
  display: grid;
  gap: var(--cn-space-5);
}

.merge-summary {
  border-color: color-mix(in srgb, var(--cn-color-warning) 24%, var(--cn-color-border));
}

.merge-summary__content {
  display: grid;
  gap: var(--cn-space-2);
}

.merge-summary__content > span {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
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
