<template>
  <CnPage class="oj-tags-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="标签管理"
      description="维护 OJ 题目标签，用于题目检索、分类筛选和运营编排。"
      eyebrow="OJ Tags"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">OJ 判题</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ tagList.length }} 个标签</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Plus" @click="showAddDialog">新建标签</el-button>
      </template>
    </CnPageHeader>

    <div class="oj-stat-grid">
      <CnStatCard title="标签总数" :value="tagList.length" description="当前可用于题目管理的标签数量" tone="brand" />
      <CnStatCard title="本页展示" :value="tagList.length" description="标签接口返回的全部记录" tone="success" />
      <CnStatCard title="命名规范" value="30" unit="字符内" description="标签名称限制长度" tone="info" />
      <CnStatCard title="后端能力" value="创建" description="当前接口支持新增标签" tone="warning" />
    </div>

    <CnSection title="标签列表" description="标签删除接口暂未暴露，删除操作保留原提示逻辑。" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tagList"
        :loading="loading"
        :pagination="null"
        row-key="id"
        empty-title="暂无标签"
        empty-description="当前还没有 OJ 标签，可以先创建常用算法或语言标签。"
      >
        <template #name="{ row }">
          <div class="tag-cell">
            <CnStatusTag type="success" size="sm"># {{ row.name }}</CnStatusTag>
          </div>
        </template>

        <template #actions="{ row }">
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="dialogVisible" title="新建标签" width="450px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" maxlength="30" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn } from '@/design-system'

interface OjTag {
  id: number
  name: string
  createTime?: string
  [key: string]: unknown
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: 'OJ 判题管理' }, { label: '标签管理' }]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const tagList = ref<OjTag[]>([])
const formRef = ref()

const form = reactive({ name: '' })
const rules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { min: 1, max: 30, message: '标签名称长度为1-30个字符', trigger: 'blur' }
  ]
}

const tableColumns: CnTableColumn<OjTag>[] = [
  { type: 'index', label: '#', width: 60 },
  { prop: 'name', label: '标签名称', minWidth: 220, slot: 'name', showOverflowTooltip: true },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 100, fixed: 'right', slot: 'actions' }
]

const fetchTags = async () => {
  loading.value = true
  try {
    tagList.value = (await ojApi.getTags()) || []
  } catch (error) {
    console.error('获取 OJ 标签失败:', error)
    ElMessage.error('获取标签列表失败')
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  form.name = ''
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    await ojApi.createTag(form)
    ElMessage.success('创建标签成功')
    dialogVisible.value = false
    fetchTags()
  } catch (error) {
    if (error) {
      console.error('创建 OJ 标签失败:', error)
    }
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: OjTag) => {
  try {
    await ElMessageBox.confirm(`确认删除标签「${row.name}」吗？`, '确认删除', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    ElMessage.info('如需删除标签，请联系开发者扩展后端接口')
  } catch {
    // cancelled
  }
}

onMounted(() => {
  fetchTags()
})
</script>

<style scoped>
.oj-tags-page {
  min-height: 100%;
}

.oj-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.tag-cell {
  display: flex;
  min-width: 0;
}

@media (max-width: 1180px) {
  .oj-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .oj-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
