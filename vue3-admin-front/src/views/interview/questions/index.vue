<template>
  <div class="question-management">
    <!-- 页面头部 -->
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div class="title-section">
          <h2>题目管理</h2>
          <p>管理面试题目，支持Markdown编辑、批量导入和批量操作</p>
          <div v-if="currentQuestionSetTitle" class="breadcrumb">
            <el-tag type="info">
              <el-icon><Collection /></el-icon>
              {{ currentQuestionSetTitle }}
            </el-tag>
          </div>
        </div>
        <div class="action-section">
          <el-button
            type="danger"
            :disabled="selectedQuestions.length === 0"
            @click="handleBatchDelete"
          >
            <el-icon><Delete /></el-icon>
            批量删除 ({{ selectedQuestions.length }})
          </el-button>
          <el-button type="success" @click="handleImport">
            <el-icon><Upload /></el-icon>
            MD导入
          </el-button>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增题目
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 搜索和过滤区 -->
    <el-card class="search-card" shadow="never">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-select v-model="queryForm.questionSetId" placeholder="请选择题单" clearable @change="handleSearch">
            <el-option
              v-for="questionSet in questionSetList"
              :key="questionSet.id"
              :label="questionSet.title"
              :value="questionSet.id"
            />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-input
            v-model="queryForm.title"
            placeholder="请输入题目标题"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-input
            v-model="queryForm.keyword"
            placeholder="关键词搜索（标题、答案）"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
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
        </el-col>
      </el-row>
    </el-card>

    <!-- 题目表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="questionList"
        style="width: 100%"
        :row-key="row => row.id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="题目标题" min-width="300" show-overflow-tooltip />
        <el-table-column prop="questionSetTitle" label="所属题单" width="180" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="viewCount" label="浏览量" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ row.viewCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="favoriteCount" label="收藏量" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="success">{{ row.favoriteCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="info" size="small" @click="handlePreview(row)">
              <el-icon><View /></el-icon>
              预览
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

      <!-- 分页组件 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="1200px"
      @close="resetForm"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="max-height: 70vh; overflow-y: auto;"
      >
        <el-form-item label="题目标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入题目标题" maxlength="500" />
        </el-form-item>
        <el-form-item label="所属题单" prop="questionSetId">
          <el-select v-model="form.questionSetId" placeholder="请选择题单" style="width: 100%">
            <el-option
              v-for="questionSet in questionSetList"
              :key="questionSet.id"
              :label="questionSet.title"
              :value="questionSet.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序序号" prop="sortOrder">
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            :max="9999"
            placeholder="请输入排序序号"
          />
        </el-form-item>

        <el-form-item label="参考答案" prop="answer">
          <el-input
            v-model="form.answer"
            type="textarea"
            :rows="12"
            placeholder="请输入参考答案，支持Markdown语法"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog
      title="题目预览"
      v-model="previewVisible"
      width="1000px"
      :close-on-click-modal="false"
    >
      <div v-if="previewData" class="preview-content">
        <div class="preview-header">
          <h3>{{ previewData.title }}</h3>
          <el-tag type="info">{{ previewData.questionSetTitle }}</el-tag>
        </div>

        <div class="answer-section">
          <h3>参考答案</h3>
          <div class="markdown-content" v-html="renderMarkdown(previewData.answer)"></div>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="previewVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- MD导入对话框 -->
    <el-dialog
      title="Markdown导入"
      v-model="importDialogVisible"
      width="900px"
      @close="resetImportForm"
      :close-on-click-modal="false"
    >
      <el-form ref="importFormRef" :model="importForm" :rules="importRules" label-width="100px">
        <el-form-item label="目标题单" prop="questionSetId">
          <el-select v-model="importForm.questionSetId" placeholder="请选择题单" style="width: 100%">
            <el-option
              v-for="questionSet in questionSetList"
              :key="questionSet.id"
              :label="questionSet.title"
              :value="questionSet.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="MD内容" prop="markdownContent">
                     <el-input
             v-model="importForm.markdownContent"
             type="textarea"
             :rows="15"
             placeholder="请输入Markdown格式的题目内容，格式示例：&#10;&#10;## 什么是Java中的多态？&#10;&#10;多态是面向对象编程的一个重要特性，指同一个接口可以有不同的实现方式。&#10;&#10;在Java中，多态主要体现在：&#10;- 方法重载（编译时多态）&#10;- 方法重写（运行时多态）&#10;&#10;运行时多态的实现条件：&#10;1. 继承关系&#10;2. 方法重写&#10;3. 父类引用指向子类对象"
             style="width: 100%"
           />
        </el-form-item>
        <el-form-item>
                     <el-alert
             title="格式说明"
             type="info"
             :closable="false"
           >
             <template #default>
               <p>使用 ## 作为题目分割标识，## 后的内容作为题目标题</p>
               <p>题目标题下的所有内容将作为参考答案</p>
               <p>示例：## 什么是Java中的多态？ [换行] 多态是面向对象编程的一个重要特性...</p>
             </template>
           </el-alert>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleImportSubmit" :loading="importLoading">
            导入
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Refresh, Edit, Delete, View, Collection, Upload
} from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'

const route = useRoute()

// 响应式数据
const loading = ref(false)
const submitLoading = ref(false)
const importLoading = ref(false)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const importDialogVisible = ref(false)
const questionList = ref([])
const questionSetList = ref([])
const selectedQuestions = ref([])
const total = ref(0)
const formRef = ref(null)
const importFormRef = ref(null)
const previewData = ref(null)
const currentQuestionSetTitle = ref('')

// 查询表单
const queryForm = reactive({
  questionSetId: null,
  title: '',
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

// 编辑表单
const form = reactive({
  id: null,
  questionSetId: null,
  title: '',
  answer: '',
  sortOrder: 0
})

// 导入表单
const importForm = reactive({
  questionSetId: null,
  markdownContent: ''
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入题目标题', trigger: 'blur' },
    { max: 500, message: '题目标题长度不能超过500字符', trigger: 'blur' }
  ],
  questionSetId: [
    { required: true, message: '请选择题单', trigger: 'change' }
  ]
}

// 导入验证规则
const importRules = {
  questionSetId: [
    { required: true, message: '请选择题单', trigger: 'change' }
  ],
  markdownContent: [
    { required: true, message: '请输入Markdown内容', trigger: 'blur' }
  ]
}

// 计算对话框标题
const dialogTitle = computed(() => {
  return form.id ? '编辑题目' : '新增题目'
})

// 简单的Markdown渲染（基本格式支持）
const renderMarkdown = (text) => {
  if (!text) return ''

  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
    .replace(/^### (.*$)/gm, '<h3>$1</h3>')
    .replace(/^## (.*$)/gm, '<h2>$1</h2>')
    .replace(/^# (.*$)/gm, '<h1>$1</h1>')
}

// 获取题目列表
const fetchQuestions = async () => {
  loading.value = true
  try {
    const data = await interviewApi.getQuestions(queryForm)
    questionList.value = data?.records || []
    total.value = data?.total || 0
  } catch (error) {
    console.error('获取题目列表失败:', error)
    ElMessage.error('获取题目列表失败')
  } finally {
    loading.value = false
  }
}

// 获取题单列表
const fetchQuestionSets = async () => {
  try {
    const data = await interviewApi.getQuestionSets({ pageNum: 1, pageSize: 100 })
    questionSetList.value = data?.records || []
  } catch (error) {
    console.error('获取题单列表失败:', error)
    ElMessage.error('获取题单列表失败')
  }
}

// 搜索
const handleSearch = () => {
  queryForm.pageNum = 1
  fetchQuestions()
}

// 重置搜索
const handleReset = () => {
  queryForm.questionSetId = null
  queryForm.title = ''
  queryForm.keyword = ''
  queryForm.pageNum = 1
  currentQuestionSetTitle.value = ''
  fetchQuestions()
}

// 分页大小改变
const handleSizeChange = (size) => {
  queryForm.pageSize = size
  queryForm.pageNum = 1
  fetchQuestions()
}

// 当前页改变
const handleCurrentChange = (page) => {
  queryForm.pageNum = page
  fetchQuestions()
}

// 选择改变
const handleSelectionChange = (selection) => {
  selectedQuestions.value = selection
}

// 新增题目
const handleAdd = () => {
  resetForm()
  if (queryForm.questionSetId) {
    form.questionSetId = queryForm.questionSetId
  }
  dialogVisible.value = true
}

// 编辑题目
const handleEdit = (row) => {
  form.id = row.id
  form.questionSetId = row.questionSetId
  form.title = row.title
  form.answer = row.answer || ''
  form.sortOrder = row.sortOrder || 0
  dialogVisible.value = true
}

// 预览题目
const handlePreview = (row) => {
  previewData.value = row
  previewVisible.value = true
}

// 删除题目
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除题目 "${row.title}" 吗？删除后将无法恢复！`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await interviewApi.deleteQuestion(row.id)
    ElMessage.success('删除成功')
    await fetchQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除题目失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedQuestions.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedQuestions.value.length} 道题目吗？删除后将无法恢复！`,
      '批量删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const ids = selectedQuestions.value.map(item => item.id)
    await interviewApi.batchDeleteQuestions(ids)
    ElMessage.success('批量删除成功')
    selectedQuestions.value = []
    await fetchQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  const formElement = formRef.value
  if (!formElement) return

  try {
    await formElement.validate()
    submitLoading.value = true

    const data = {
      questionSetId: form.questionSetId,
      title: form.title,
      answer: form.answer,
      sortOrder: form.sortOrder
    }

    if (form.id) {
      // 编辑
      await interviewApi.updateQuestion(form.id, data)
      ElMessage.success('编辑成功')
    } else {
      // 新增
      await interviewApi.createQuestion(data)
      ElMessage.success('新增成功')
    }

    dialogVisible.value = false
    await fetchQuestions()
  } catch (error) {
    if (error?.name !== 'ValidationError') {
      console.error('提交失败:', error)
      ElMessage.error('操作失败')
    }
  } finally {
    submitLoading.value = false
  }
}

// 导入功能
const handleImport = () => {
  importForm.questionSetId = queryForm.questionSetId || null
  importForm.markdownContent = ''
  importDialogVisible.value = true
}

// 提交导入
const handleImportSubmit = async () => {
  const formElement = importFormRef.value
  if (!formElement) return

  try {
    await formElement.validate()
    importLoading.value = true

    const data = await interviewApi.importMarkdownQuestions({
      questionSetId: importForm.questionSetId,
      markdownContent: importForm.markdownContent
    })

    ElMessage.success(`导入成功，共导入 ${data} 道题目`)
    importDialogVisible.value = false
    await fetchQuestions()
  } catch (error) {
    if (error?.name !== 'ValidationError') {
      console.error('导入失败:', error)
      ElMessage.error('导入失败')
    }
  } finally {
    importLoading.value = false
  }
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  form.id = null
  form.questionSetId = null
  form.title = ''
  form.answer = ''
  form.sortOrder = 0
}

// 重置导入表单
const resetImportForm = () => {
  if (importFormRef.value) {
    importFormRef.value.resetFields()
  }
  importForm.questionSetId = null
  importForm.markdownContent = ''
}

// 监听路由参数变化
watch(() => route.query, (newQuery) => {
  if (newQuery.questionSetId) {
    queryForm.questionSetId = Number(newQuery.questionSetId)
    currentQuestionSetTitle.value = decodeURIComponent(newQuery.title || '')
  }
  fetchQuestions()
}, { immediate: true })

// 监听题单选择变化
watch(() => queryForm.questionSetId, (newValue) => {
  if (newValue) {
    const questionSet = questionSetList.value.find(item => item.id === newValue)
    currentQuestionSetTitle.value = questionSet ? questionSet.title : ''
  } else {
    currentQuestionSetTitle.value = ''
  }
})

// 页面挂载
onMounted(() => {
  fetchQuestionSets()
  // fetchQuestions() 会在 watch 中自动调用
})
</script>

<style scoped>
.question-management {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.header-card {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
}

.title-section p {
  margin: 0 0 10px 0;
  color: #909399;
  font-size: 14px;
}

.breadcrumb {
  margin-top: 5px;
}

.action-section {
  display: flex;
  gap: 10px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.preview-content {
  max-height: 60vh;
  overflow-y: auto;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.preview-header h3 {
  margin: 0;
  color: #303133;
}

.answer-section {
  margin-top: 20px;
}

.answer-section h3 {
  margin: 0 0 15px 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.markdown-content {
  padding: 20px;
  background-color: #fafafa;
  border-radius: 4px;
  line-height: 1.8;
}

.markdown-content :deep(h1) {
  font-size: 24px;
  color: #303133;
  margin: 20px 0 15px 0;
}

.markdown-content :deep(h2) {
  font-size: 20px;
  color: #303133;
  margin: 18px 0 12px 0;
}

.markdown-content :deep(h3) {
  font-size: 16px;
  color: #303133;
  margin: 15px 0 10px 0;
}

.markdown-content :deep(strong) {
  color: #409eff;
  font-weight: bold;
}

.markdown-content :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  color: #e83e8c;
}

.dialog-footer {
  text-align: right;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .action-section {
    align-self: stretch;
    justify-content: flex-end;
  }
}
</style>
