<template>
  <div class="daily-content-management">
    <!-- 页面头部 -->
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div class="title-section">
          <h2>每日内容管理</h2>
          <p>管理每日推送的内容，包括编程格言、技术小贴士、代码片段、历史上的今天等</p>
        </div>
        <div class="action-section">
          <el-button 
            type="danger" 
            :disabled="selectedContents.length === 0"
            @click="handleBatchDelete"
          >
            <el-icon><Delete /></el-icon>
            批量删除 ({{ selectedContents.length }})
          </el-button>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增内容
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计信息 -->
    <el-card class="stats-card" shadow="never" v-if="statistics">
      <el-row :gutter="16">
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-number primary">{{ statistics.totalContents || 0 }}</div>
            <div class="stat-label">总内容数</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-number success">{{ statistics.quotes || 0 }}</div>
            <div class="stat-label">编程格言</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-number warning">{{ statistics.tips || 0 }}</div>
            <div class="stat-label">技术小贴士</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-number info">{{ statistics.codeSnippets || 0 }}</div>
            <div class="stat-label">代码片段</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-number danger">{{ statistics.histories || 0 }}</div>
            <div class="stat-label">历史上的今天</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-number popular">{{ statistics.totalViews || 0 }}</div>
            <div class="stat-label">总查看数</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 搜索和操作区域 -->
    <el-card class="search-card" shadow="never">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input 
            v-model="searchForm.title" 
            placeholder="请输入内容标题" 
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.contentType" placeholder="内容类型" clearable @change="handleSearch">
            <el-option label="编程格言" :value="1" />
            <el-option label="技术小贴士" :value="2" />
            <el-option label="代码片段" :value="3" />
            <el-option label="历史上的今天" :value="4" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.programmingLanguage" placeholder="编程语言" clearable @change="handleSearch">
            <el-option label="Java" value="Java" />
            <el-option label="Python" value="Python" />
            <el-option label="JavaScript" value="JavaScript" />
            <el-option label="C++" value="C++" />
            <el-option label="Go" value="Go" />
            <el-option label="Rust" value="Rust" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.difficultyLevel" placeholder="难度等级" clearable @change="handleSearch">
            <el-option label="初级" :value="1" />
            <el-option label="中级" :value="2" />
            <el-option label="高级" :value="3" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
          <el-button type="success" @click="loadPopularRanking">
            <el-icon><DataAnalysis /></el-icon>
            热门排行
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 内容表格 -->
    <el-card class="table-card" shadow="never">
      <el-table 
        :data="contentList" 
        v-loading="loading"
        @selection-change="handleSelectionChange"
        stripe
        border
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="title" label="标题" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="content-title">
              <el-icon :color="getContentTypeColor(row.contentType)">
                <component :is="getContentTypeIcon(row.contentType)" />
              </el-icon>
              <span style="margin-left: 8px;">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="contentType" label="内容类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getContentTypeTag(row.contentType)">
              {{ getContentTypeName(row.contentType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="programmingLanguage" label="编程语言" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.programmingLanguage" type="info" size="small">
              {{ row.programmingLanguage }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="difficultyLevel" label="难度等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.difficultyLevel" :type="getDifficultyTag(row.difficultyLevel)" size="small">
              {{ getDifficultyName(row.difficultyLevel) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
        <el-table-column label="统计" width="120" align="center">
          <template #default="{ row }">
            <div class="content-stats">
              <span class="stat-item">
                <el-icon><View /></el-icon>
                {{ row.viewCount || 0 }}
              </span>
              <span class="stat-item">
                <el-icon><Star /></el-icon>
                {{ row.likeCount || 0 }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="info" size="small" @click="handleView(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button type="primary" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="1000px"
      :before-close="handleCloseDialog"
      destroy-on-close
    >
      <el-form
        ref="contentFormRef"
        :model="contentForm"
        :rules="contentRules"
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="内容标题" prop="title">
              <el-input v-model="contentForm.title" placeholder="请输入内容标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内容类型" prop="contentType">
              <el-select v-model="contentForm.contentType" placeholder="请选择内容类型">
                <el-option label="编程格言" :value="1" />
                <el-option label="技术小贴士" :value="2" />
                <el-option label="代码片段" :value="3" />
                <el-option label="历史上的今天" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="作者">
              <el-input v-model="contentForm.author" placeholder="请输入作者（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="编程语言">
              <el-select v-model="contentForm.programmingLanguage" placeholder="选择编程语言（可选）" clearable>
                <el-option label="Java" value="Java" />
                <el-option label="Python" value="Python" />
                <el-option label="JavaScript" value="JavaScript" />
                <el-option label="C++" value="C++" />
                <el-option label="Go" value="Go" />
                <el-option label="Rust" value="Rust" />
                <el-option label="TypeScript" value="TypeScript" />
                <el-option label="C#" value="C#" />
                <el-option label="PHP" value="PHP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度等级">
              <el-select v-model="contentForm.difficultyLevel" placeholder="选择难度等级（可选）" clearable>
                <el-option label="初级" :value="1" />
                <el-option label="中级" :value="2" />
                <el-option label="高级" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="内容正文" prop="content">
          <el-input
            v-model="contentForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入内容正文，支持Markdown格式"
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-input
            v-model="contentForm.tagsText"
            placeholder="请输入标签，用逗号分隔（可选）"
          />
        </el-form-item>

        <el-form-item label="来源链接">
          <el-input v-model="contentForm.sourceUrl" placeholder="请输入来源链接（可选）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看对话框 -->
    <el-dialog
      title="查看内容"
      v-model="viewDialogVisible"
      width="800px"
      destroy-on-close
    >
      <div v-if="viewContent" class="content-view">
        <div class="content-header">
          <h3>{{ viewContent.title }}</h3>
          <div class="content-meta">
            <el-tag :type="getContentTypeTag(viewContent.contentType)">
              {{ getContentTypeName(viewContent.contentType) }}
            </el-tag>
            <el-tag v-if="viewContent.programmingLanguage" type="info" style="margin-left: 8px;">
              {{ viewContent.programmingLanguage }}
            </el-tag>
            <el-tag v-if="viewContent.difficultyLevel" :type="getDifficultyTag(viewContent.difficultyLevel)" style="margin-left: 8px;">
              {{ getDifficultyName(viewContent.difficultyLevel) }}
            </el-tag>
          </div>
          <div class="content-stats" style="margin-top: 8px;">
            <span>作者：{{ viewContent.author || '无' }}</span>
            <span style="margin-left: 16px;">查看：{{ viewContent.viewCount || 0 }}</span>
            <span style="margin-left: 16px;">点赞：{{ viewContent.likeCount || 0 }}</span>
          </div>
        </div>
        <div class="content-body">
          <pre>{{ viewContent.content }}</pre>
        </div>
        <div v-if="viewContent.tags && viewContent.tags.length" class="content-tags">
          <strong>标签：</strong>
          <el-tag v-for="tag in viewContent.tags" :key="tag" size="small" style="margin-right: 8px;">
            {{ tag }}
          </el-tag>
        </div>
        <div v-if="viewContent.sourceUrl" class="content-source">
          <strong>来源：</strong>
          <a :href="viewContent.sourceUrl" target="_blank">{{ viewContent.sourceUrl }}</a>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit, Search, Refresh, View, Star, DataAnalysis } from '@element-plus/icons-vue'
import { dailyContentApi } from '@/api/moyu'

// 响应式数据
const loading = ref(false)
const contentList = ref([])
const selectedContents = ref([])
const statistics = ref(null)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const viewContent = ref(null)
const submitLoading = ref(false)
const isEdit = ref(false)
const contentFormRef = ref()

// 搜索表单
const searchForm = reactive({
  title: '',
  contentType: null,
  programmingLanguage: '',
  difficultyLevel: null
})

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 内容表单
const contentForm = reactive({
  id: null,
  title: '',
  contentType: null,
  content: '',
  author: '',
  programmingLanguage: '',
  tagsText: '',
  difficultyLevel: null,
  sourceUrl: '',
  status: 1
})

// 表单验证规则
const contentRules = {
  title: [
    { required: true, message: '请输入内容标题', trigger: 'blur' }
  ],
  contentType: [
    { required: true, message: '请选择内容类型', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入内容正文', trigger: 'blur' }
  ]
}

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑内容' : '新增内容')

// 获取内容类型名称
const getContentTypeName = (type) => {
  const typeMap = {
    1: '编程格言',
    2: '技术小贴士',
    3: '代码片段',
    4: '历史上的今天'
  }
  return typeMap[type] || '未知'
}

// 获取内容类型标签
const getContentTypeTag = (type) => {
  const tagMap = {
    1: 'success',
    2: 'warning',
    3: 'primary',
    4: 'info'
  }
  return tagMap[type] || 'info'
}

// 获取内容类型颜色
const getContentTypeColor = (type) => {
  const colorMap = {
    1: '#67c23a',
    2: '#e6a23c',
    3: '#409eff',
    4: '#909399'
  }
  return colorMap[type] || '#909399'
}

// 获取内容类型图标
const getContentTypeIcon = (type) => {
  const iconMap = {
    1: 'Star',
    2: 'InfoFilled',
    3: 'EditPen',
    4: 'Calendar'
  }
  return iconMap[type] || 'Document'
}

// 获取难度等级名称
const getDifficultyName = (level) => {
  const levelMap = {
    1: '初级',
    2: '中级', 
    3: '高级'
  }
  return levelMap[level] || '未知'
}

// 获取难度等级标签
const getDifficultyTag = (level) => {
  const tagMap = {
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return tagMap[level] || 'info'
}

// 格式化时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 加载内容列表
const loadContentList = async () => {
  try {
    loading.value = true
    const data = await dailyContentApi.getContentList({ ...searchForm, limit: pagination.pageSize })
    contentList.value = data || []
    pagination.total = contentList.value.length
  } catch (error) {
    console.error('加载内容列表失败:', error)
    ElMessage.error('加载内容列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    const data = await dailyContentApi.getContentStatistics()
    statistics.value = data
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

// 加载热门排行
const loadPopularRanking = async () => {
  try {
    loading.value = true
    const data = await dailyContentApi.getPopularRanking({ limit: 50 })
    contentList.value = data || []
    pagination.total = contentList.value.length
    ElMessage.success('已加载热门内容排行')
  } catch (error) {
    console.error('加载热门排行失败:', error)
    ElMessage.error('加载热门排行失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.currentPage = 1
  loadContentList()
}

// 重置搜索
const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = null
  })
  handleSearch()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedContents.value = selection
}

// 状态变更
const handleStatusChange = async (row) => {
  try {
    await dailyContentApi.updateContentStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
    loadStatistics()
  } catch (error) {
    console.error('状态更新失败:', error)
    ElMessage.error('状态更新失败')
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

// 查看内容
const handleView = async (row) => {
  try {
    const data = await dailyContentApi.getContentById(row.id)
    viewContent.value = data
    viewDialogVisible.value = true
  } catch (error) {
    console.error('加载内容详情失败:', error)
    ElMessage.error('加载内容详情失败')
  }
}

// 新增内容
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑内容
const handleEdit = async (row) => {
  try {
    isEdit.value = true
    const contentData = await dailyContentApi.getContentById(row.id)
    
    Object.keys(contentForm).forEach(key => {
      if (key === 'tagsText') {
        contentForm[key] = contentData.tags ? contentData.tags.join(',') : ''
      } else {
        contentForm[key] = contentData[key] || ''
      }
    })
    
    dialogVisible.value = true
  } catch (error) {
    console.error('加载内容详情失败:', error)
    ElMessage.error('加载内容详情失败')
  }
}

// 删除内容
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除内容 "${row.title}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await dailyContentApi.deleteContent(row.id)
    ElMessage.success('删除成功')
    loadContentList()
    loadStatistics()
  } catch (error) {
    if (error === 'cancel') return
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedContents.value.length} 个内容吗？`,
      '确认批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    const ids = selectedContents.value.map(item => item.id)
    await dailyContentApi.batchDeleteContents(ids)
    ElMessage.success('批量删除成功')
    selectedContents.value = []
    loadContentList()
    loadStatistics()
  } catch (error) {
    if (error === 'cancel') return
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!contentFormRef.value) return
  
  try {
    await contentFormRef.value.validate()
    submitLoading.value = true
    
    const formData = { ...contentForm }
    // 处理标签
    if (formData.tagsText) {
      formData.tags = formData.tagsText.split(',').map(tag => tag.trim()).filter(tag => tag)
    }
    delete formData.tagsText
    
    if (isEdit.value) {
      await dailyContentApi.updateContent(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await dailyContentApi.createContent(formData)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadContentList()
    loadStatistics()
  } catch (error) {
    if (error.name !== 'ValidationError') {
      console.error('提交失败:', error)
      ElMessage.error('提交失败')
    }
  } finally {
    submitLoading.value = false
  }
}

// 重置表单
const resetForm = () => {
  Object.keys(contentForm).forEach(key => {
    if (key === 'status') {
      contentForm[key] = 1
    } else {
      contentForm[key] = null
    }
  })
}

// 关闭对话框
const handleCloseDialog = (done) => {
  resetForm()
  done()
}

// 分页处理
const handleSizeChange = (size) => {
  pagination.pageSize = size
  loadContentList()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  loadContentList()
}

// 初始化
onMounted(() => {
  loadContentList()
  loadStatistics()
})
</script>

<style scoped>
.daily-content-management {
  padding: 0;
}

.header-card {
  margin-bottom: 16px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title-section h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.title-section p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.action-section {
  display: flex;
  gap: 12px;
}

.stats-card {
  margin-bottom: 16px;
}

.stat-item {
  text-align: center;
  padding: 20px;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-number.primary { color: #409eff; }
.stat-number.success { color: #67c23a; }
.stat-number.warning { color: #e6a23c; }
.stat-number.info { color: #909399; }
.stat-number.danger { color: #f56c6c; }
.stat-number.popular { color: #722ed1; }

.stat-label {
  color: #909399;
  font-size: 14px;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.content-title {
  display: flex;
  align-items: center;
}

.content-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.content-stats .stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
  padding: 0;
  text-align: left;
}

.pagination-container {
  display: flex;
  justify-content: right;
  margin-top: 20px;
}

.content-view {
  max-height: 60vh;
  overflow-y: auto;
}

.content-header {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.content-header h3 {
  margin: 0 0 8px 0;
  color: #303133;
}

.content-meta {
  margin-bottom: 8px;
}

.content-body {
  margin-bottom: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 4px;
}

.content-body pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  line-height: 1.6;
}

.content-tags {
  margin-bottom: 16px;
}

.content-source a {
  color: #409eff;
  text-decoration: none;
}

.content-source a:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 16px;
  }
  
  .action-section {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
