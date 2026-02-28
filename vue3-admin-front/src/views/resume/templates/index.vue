<template>
  <div class="resume-template-admin">
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div>
          <h2>简历模板管理</h2>
          <p>维护模板的基础信息、分类及启用状态</p>
        </div>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新建模板
        </el-button>
      </div>
    </el-card>

    <el-card class="search-card" shadow="never">
      <el-row :gutter="16" class="search-row">
        <el-col :xs="24" :md="8">
          <el-input
            v-model="searchForm.keyword"
            placeholder="模板名称 / 标签"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :xs="24" :md="6">
          <el-select v-model="searchForm.category" placeholder="分类" clearable @change="handleSearch">
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-col>
        <el-col :xs="24" :md="6">
          <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
            <el-option label="启用" :value="1" />
            <el-option label="下线" :value="0" />
          </el-select>
        </el-col>
        <el-col :xs="24" :md="4" class="search-actions">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="templateList"
        v-loading="loading"
        border
        row-key="id"
      >
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="name" label="模板名称" min-width="160" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column label="经验" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ formatExperience(row.experienceLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="140">
          <template #default="{ row }">
            <el-rate v-model="row.rating" disabled show-score text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" fixed="right" width="240">
          <template #default="{ row }">
            <el-button text type="primary" @click="handlePreview(row)">预览</el-button>
            <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              text
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="pagination.total > pagination.size">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑模板' : '新建模板'"
      width="640px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="经验层级" prop="experienceLevel">
          <el-select v-model="form.experienceLevel">
            <el-option
              v-for="item in experienceOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="封面地址" prop="coverUrl">
          <el-input v-model="form.coverUrl" placeholder="建议使用COS链接" />
        </el-form-item>
        <el-form-item label="效果预览" prop="previewUrl">
          <el-input v-model="form.previewUrl" placeholder="可填写高清图/在线预览链接" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入标签后回车"
          />
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
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="previewVisible"
      title="模板概览"
      size="40%"
    >
      <div v-if="currentTemplate">
        <el-image
          :src="currentTemplate.previewUrl || currentTemplate.coverUrl"
          fit="cover"
          style="width: 100%; height: 240px; border-radius: 12px"
          :preview-src-list="[currentTemplate.previewUrl || currentTemplate.coverUrl]"
        />
        <el-descriptions :column="1" border style="margin-top: 16px">
          <el-descriptions-item label="模板名称">{{ currentTemplate.name }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ currentTemplate.category || '通用' }}</el-descriptions-item>
          <el-descriptions-item label="经验层级">{{ formatExperience(currentTemplate.experienceLevel) }}</el-descriptions-item>
          <el-descriptions-item label="标签">
            <el-tag
              v-for="tag in parseTags(currentTemplate.tags)"
              :key="tag"
              size="small"
              class="tag-gap"
            >
              {{ tag }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="技术栈">{{ currentTemplate.techStack || '未配置' }}</el-descriptions-item>
          <el-descriptions-item label="模板说明">{{ currentTemplate.description || '暂无描述' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const currentTemplate = ref(null)
const templateList = ref([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const searchForm = reactive({
  keyword: '',
  category: '',
  status: ''
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
  id: null,
  name: '',
  category: '',
  description: '',
  coverUrl: '',
  previewUrl: '',
  tags: [],
  techStack: '',
  experienceLevel: 1,
  status: 1
})

const form = reactive(defaultForm())
const formRef = ref(null)

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  experienceLevel: [{ required: true, message: '请选择经验层级', trigger: 'change' }]
}

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
  } catch (error) {
    ElMessage.error('加载模板失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchTemplates()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.category = ''
  searchForm.status = ''
  pagination.page = 1
  fetchTemplates()
}

const handlePageChange = (page) => {
  pagination.page = page
  fetchTemplates()
}

const handleAdd = () => {
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

const handleEdit = (row) => {
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

const handlePreview = (row) => {
  currentTemplate.value = row
  previewVisible.value = true
}

const handleDelete = async (row) => {
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
  } catch (error) {
    ElMessage.error('保存失败，请检查表单')
  } finally {
    submitLoading.value = false
  }
}

const parseTags = (tags) => {
  if (!tags) return []
  return tags.split(',').map(tag => tag.trim()).filter(Boolean)
}

const formatExperience = (level) => {
  const map = {
    1: '应届 / 实习',
    2: '初级',
    3: '中级',
    4: '高级',
    5: '专家'
  }
  return map[level] || '通用'
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  Object.assign(form, defaultForm())
}

onMounted(() => {
  fetchTemplates()
})
</script>

<style scoped>
.resume-template-admin {
  display: grid;
  gap: 14px;
  padding: 2px 0 8px;
  min-height: calc(100vh - 130px);
}

.header-card {
  margin-bottom: 0;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-content h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--cn-text-primary);
  font-weight: 600;
}

.header-content p {
  margin: 0;
  color: var(--cn-text-secondary);
  font-size: 14px;
}

.search-card {
  margin-bottom: 0;
}

.search-row :deep(.el-select),
.search-row :deep(.el-input) {
  width: 100%;
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  align-items: center;
}

.table-card :deep(.el-card__body) {
  padding-top: 8px;
}

.table-card :deep(.el-rate) {
  --el-rate-fill-color: #f2a93b;
}

.table-card :deep(.el-drawer__body) {
  background: #f8fbff;
}

.pagination {
  margin-top: 14px;
  text-align: right;
}

.tag-gap {
  margin-right: 6px;
  margin-bottom: 6px;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .search-actions {
    justify-content: flex-start;
  }
}
</style>
