<template>
  <CnPage class="codepen-tags-page" surface="transparent" max-width="1180px">
    <CnPageHeader
      title="CodePen 标签"
      description="维护作品标签、使用次数和标签合并，保持作品检索维度干净可控。"
      eyebrow="CodePen Tags"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">标签 {{ tableData.length }} 个</CnStatusTag>
        <CnStatusTag type="success">已使用 {{ usedTagCount }} 个</CnStatusTag>
        <CnStatusTag type="neutral">空闲 {{ unusedTagCount }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新建标签</el-button>
      </template>
    </CnPageHeader>

    <div class="codepen-stat-grid">
      <CnStatCard title="标签总数" :value="tableData.length" description="当前系统作品标签数量" tone="brand" />
      <CnStatCard title="已使用标签" :value="usedTagCount" description="至少关联 1 个作品的标签" tone="success" />
      <CnStatCard title="空闲标签" :value="unusedTagCount" description="可直接删除或等待作品使用" tone="warning" />
      <CnStatCard title="总使用次数" :value="totalUseCount" description="标签被作品引用的累计次数" tone="info" />
    </div>

    <CnSection title="标签列表" description="支持新建、编辑、删除未使用标签，以及把源标签合并到目标标签。" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="null"
        row-key="id"
        empty-title="暂无标签"
        empty-description="还没有作品标签，可以先创建一个常用分类标签。"
        empty-icon="CT"
      >
        <template #tagName="{ row }">
          <CnStatusTag type="brand" size="sm"># {{ row.tagName }}</CnStatusTag>
        </template>

        <template #tagDescription="{ row }">
          <span class="description-text">{{ row.tagDescription || '-' }}</span>
        </template>

        <template #useCount="{ row }">
          <CnStatusTag :type="row.useCount > 0 ? 'success' : 'neutral'" size="sm" subtle>
            {{ row.useCount || 0 }} 次
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button link type="primary" size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" :icon="Connection" @click="handleMerge(row)">合并</el-button>
            <el-button link type="danger" size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="标签名称" required>
          <el-input v-model="formData.tagName" placeholder="请输入标签名称" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="标签描述">
          <el-input
            v-model="formData.tagDescription"
            type="textarea"
            :rows="3"
            placeholder="请输入标签描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="mergeDialogVisible" title="合并标签" width="500px" :close-on-click-modal="false">
      <el-alert title="合并说明" type="info" :closable="false" class="merge-alert">
        <p>将源标签的所有作品合并到目标标签，然后删除源标签。</p>
      </el-alert>

      <el-form :model="mergeFormData" label-width="100px">
        <el-form-item label="源标签">
          <el-input :value="mergeFormData.sourceTag" readonly />
        </el-form-item>

        <el-form-item label="目标标签" required>
          <el-select v-model="mergeFormData.targetId" placeholder="请选择目标标签" filterable class="full-width">
            <el-option v-for="tag in targetTags" :key="tag.id" :label="tag.tagName" :value="tag.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="mergeDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="merging" @click="handleMergeSubmit">确定合并</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { codepenApi } from '@/api/codepen'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn } from '@/design-system'

interface CodePenTag extends Record<string, unknown> {
  id: number | null
  tagName: string
  tagDescription?: string
  useCount?: number
  createTime?: string
}

interface TagForm {
  id: number | null
  tagName: string
  tagDescription: string
}

interface MergeForm {
  sourceId: number | null
  sourceTag: string
  targetId: number | null
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: 'CodePen 管理' }, { label: '标签管理' }]

const tableData = ref<CodePenTag[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建标签')
const submitting = ref(false)
const mergeDialogVisible = ref(false)
const merging = ref(false)

const formData = ref<TagForm>({
  id: null,
  tagName: '',
  tagDescription: ''
})

const mergeFormData = ref<MergeForm>({
  sourceId: null,
  sourceTag: '',
  targetId: null
})

const tableColumns: CnTableColumn<CodePenTag>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'tagName', label: '标签名称', minWidth: 180, slot: 'tagName' },
  { prop: 'tagDescription', label: '标签描述', minWidth: 280, slot: 'tagDescription', showOverflowTooltip: true },
  { prop: 'useCount', label: '使用次数', width: 120, slot: 'useCount', sortable: true },
  { prop: 'createTime', label: '创建时间', width: 170, showOverflowTooltip: true },
  { label: '操作', width: 220, fixed: 'right', slot: 'actions' }
]

const targetTags = computed(() => {
  return tableData.value.filter((tag) => tag.id !== mergeFormData.value.sourceId)
})

const usedTagCount = computed(() => tableData.value.filter((tag) => Number(tag.useCount) > 0).length)
const unusedTagCount = computed(() => tableData.value.length - usedTagCount.value)
const totalUseCount = computed(() => tableData.value.reduce((sum, tag) => sum + (Number(tag.useCount) || 0), 0))

const loadData = async () => {
  try {
    loading.value = true
    const result = await codepenApi.getTagList()
    tableData.value = Array.isArray(result) ? result : []
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新建标签'
  formData.value = {
    id: null,
    tagName: '',
    tagDescription: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row: CodePenTag) => {
  dialogTitle.value = '编辑标签'
  formData.value = {
    id: row.id,
    tagName: row.tagName,
    tagDescription: row.tagDescription || ''
  }
  dialogVisible.value = true
}

const handleMerge = (row: CodePenTag) => {
  mergeFormData.value = {
    sourceId: row.id,
    sourceTag: row.tagName,
    targetId: null
  }
  mergeDialogVisible.value = true
}

const handleDelete = async (row: CodePenTag) => {
  if (Number(row.useCount) > 0) {
    ElMessage.warning('该标签正在使用中，无法删除。请先合并到其他标签或等待作品更新。')
    return
  }

  try {
    await ElMessageBox.confirm(`确定删除标签"${row.tagName}"吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })

    if (!row.id) return
    await codepenApi.deleteTag(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSubmit = async () => {
  if (!formData.value.tagName) {
    ElMessage.warning('请输入标签名称')
    return
  }

  try {
    submitting.value = true

    if (formData.value.id) {
      await codepenApi.updateTag(formData.value)
      ElMessage.success('更新成功')
    } else {
      await codepenApi.createTag(formData.value)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleMergeSubmit = async () => {
  if (!mergeFormData.value.targetId) {
    ElMessage.warning('请选择目标标签')
    return
  }

  try {
    await ElMessageBox.confirm(`确定将标签"${mergeFormData.value.sourceTag}"合并到目标标签吗？此操作不可恢复。`, '确认合并', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    merging.value = true
    await codepenApi.mergeTags({
      sourceId: mergeFormData.value.sourceId,
      targetId: mergeFormData.value.targetId
    })

    ElMessage.success('合并成功')
    mergeDialogVisible.value = false
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('合并失败:', error)
    }
  } finally {
    merging.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.codepen-tags-page {
  min-height: 100%;
}

.codepen-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.description-text {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.table-actions,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.dialog-footer {
  justify-content: flex-end;
}

.merge-alert {
  margin-bottom: var(--cn-space-5);
}

.merge-alert p {
  margin: 0;
}

.full-width {
  width: 100%;
}

@media (max-width: 1180px) {
  .codepen-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .codepen-stat-grid {
    grid-template-columns: 1fr;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
