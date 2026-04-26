<template>
  <div class="template-management">
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>系统模板列表</span>
          <el-button type="primary" icon="Plus" @click="handleAdd">
            新建模板
          </el-button>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        
        <el-table-column prop="title" label="模板名称" width="250" />

        <el-table-column prop="description" label="模板描述" min-width="300">
          <template #default="{ row }">
            <span class="description-text">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="代码" width="150">
          <template #default="{ row }">
            <div class="code-badges">
              <el-tag v-if="row.htmlCode" size="small">HTML</el-tag>
              <el-tag v-if="row.cssCode" size="small" type="success">CSS</el-tag>
              <el-tag v-if="row.jsCode" size="small" type="warning">JS</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="160" />

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              icon="View"
              @click="handleView(row)"
            >
              查看
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              icon="Edit"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑模板对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="80%"
      :close-on-click-modal="false"
      fullscreen
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input
            v-model="formData.title"
            placeholder="请输入模板名称"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="模板描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模板描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="HTML代码">
          <textarea
            v-model="formData.htmlCode"
            class="code-textarea"
            placeholder="请输入HTML代码..."
            spellcheck="false"
          ></textarea>
        </el-form-item>

        <el-form-item label="CSS代码">
          <textarea
            v-model="formData.cssCode"
            class="code-textarea"
            placeholder="请输入CSS代码..."
            spellcheck="false"
          ></textarea>
        </el-form-item>

        <el-form-item label="JavaScript">
          <textarea
            v-model="formData.jsCode"
            class="code-textarea"
            placeholder="请输入JavaScript代码..."
            spellcheck="false"
          ></textarea>
        </el-form-item>

        <el-form-item label="预览">
          <div class="preview-container">
            <iframe
              ref="previewFrame"
              class="preview-iframe"
              sandbox="allow-scripts allow-same-origin"
            ></iframe>
          </div>
          <el-button @click="runPreview" style="margin-top: 10px">
            刷新预览
          </el-button>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="runPreview">预览</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看模板对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="查看模板"
      width="80%"
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="模板名称">
          {{ viewData.title }}
        </el-descriptions-item>
        <el-descriptions-item label="模板描述">
          {{ viewData.description || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ viewData.createTime }}
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="activeTab" style="margin-top: 20px">
        <el-tab-pane label="HTML" name="html" v-if="viewData.htmlCode">
          <pre class="code-block">{{ viewData.htmlCode }}</pre>
        </el-tab-pane>
        <el-tab-pane label="CSS" name="css" v-if="viewData.cssCode">
          <pre class="code-block">{{ viewData.cssCode }}</pre>
        </el-tab-pane>
        <el-tab-pane label="JavaScript" name="js" v-if="viewData.jsCode">
          <pre class="code-block">{{ viewData.jsCode }}</pre>
        </el-tab-pane>
        <el-tab-pane label="预览" name="preview">
          <div class="preview-container">
            <iframe
              ref="viewPreviewFrame"
              class="preview-iframe"
              sandbox="allow-scripts allow-same-origin"
            ></iframe>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { codepenApi } from '@/api/codepen'

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新建模板')
const submitting = ref(false)
const previewFrame = ref(null)

// 表单数据
const formData = ref({
  id: null,
  title: '',
  description: '',
  htmlCode: '',
  cssCode: '',
  jsCode: ''
})

// 查看对话框
const viewDialogVisible = ref(false)
const viewData = ref({})
const activeTab = ref('html')
const viewPreviewFrame = ref(null)

// 加载数据
const loadData = async () => {
  try {
    loading.value = true
    const result = await codepenApi.getTemplateList()
    tableData.value = result || []
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 新建
const handleAdd = () => {
  dialogTitle.value = '新建模板'
  formData.value = {
    id: null,
    title: '',
    description: '',
    htmlCode: '',
    cssCode: '',
    jsCode: ''
  }
  dialogVisible.value = true
  setTimeout(() => runPreview(), 100)
}

// 编辑
const handleEdit = (row) => {
  dialogTitle.value = '编辑模板'
  formData.value = {
    id: row.id,
    title: row.title,
    description: row.description,
    htmlCode: row.htmlCode || '',
    cssCode: row.cssCode || '',
    jsCode: row.jsCode || ''
  }
  dialogVisible.value = true
  setTimeout(() => runPreview(), 100)
}

// 查看
const handleView = (row) => {
  viewData.value = { ...row }
  viewDialogVisible.value = true
  activeTab.value = row.htmlCode ? 'html' : (row.cssCode ? 'css' : 'js')
  setTimeout(() => runViewPreview(), 100)
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除模板"${row.title}"吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }
    )

    await codepenApi.deleteTemplate(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 提交
const handleSubmit = async () => {
  if (!formData.value.title) {
    ElMessage.warning('请输入模板名称')
    return
  }

  if (!formData.value.htmlCode && !formData.value.cssCode && !formData.value.jsCode) {
    ElMessage.warning('请至少输入一种代码')
    return
  }

  try {
    submitting.value = true
    
    if (formData.value.id) {
      await codepenApi.updateTemplate(formData.value)
      ElMessage.success('更新成功')
    } else {
      await codepenApi.createTemplate(formData.value)
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

// 运行预览
const runPreview = () => {
  const html = formData.value.htmlCode || ''
  const css = formData.value.cssCode || ''
  const js = formData.value.jsCode || ''
  const scriptCloseTag = '</scr' + 'ipt>'

  const content = `
    <!DOCTYPE html>
    <html>
      <head>
        <style>${css}</style>
      </head>
      <body>
        ${html}
        <script>${js}${scriptCloseTag}
      </body>
    </html>
  `

  if (previewFrame.value) {
    previewFrame.value.srcdoc = content
  }
}

// 运行查看预览
const runViewPreview = () => {
  const html = viewData.value.htmlCode || ''
  const css = viewData.value.cssCode || ''
  const js = viewData.value.jsCode || ''
  const scriptCloseTag = '</scr' + 'ipt>'

  const content = `
    <!DOCTYPE html>
    <html>
      <head>
        <style>${css}</style>
      </head>
      <body>
        ${html}
        <script>${js}${scriptCloseTag}
      </body>
    </html>
  `

  if (viewPreviewFrame.value) {
    viewPreviewFrame.value.srcdoc = content
  }
}

// 监听预览标签页切换
watch(activeTab, (val) => {
  if (val === 'preview') {
    setTimeout(() => runViewPreview(), 100)
  }
})

// 初始化
onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.template-management {
  .table-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .description-text {
      color: #666;
      font-size: 14px;
    }

    .code-badges {
      display: flex;
      gap: 6px;
    }
  }

  .code-textarea {
    width: 100%;
    min-height: 200px;
    padding: 15px;
    background: #1e1e1e;
    color: #d4d4d4;
    border: 1px solid #ddd;
    border-radius: 4px;
    resize: vertical;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 14px;
    line-height: 1.6;

    &:focus {
      outline: none;
      border-color: #409eff;
    }

    &::placeholder {
      color: #666;
    }
  }

  .preview-container {
    width: 100%;
    height: 400px;
    border: 1px solid #ddd;
    border-radius: 4px;
    overflow: hidden;

    .preview-iframe {
      width: 100%;
      height: 100%;
      border: none;
    }
  }

  .code-block {
    background: #f5f5f5;
    padding: 15px;
    border-radius: 4px;
    overflow-x: auto;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 14px;
    line-height: 1.6;
    color: #333;
    margin: 0;
  }
}
</style>

