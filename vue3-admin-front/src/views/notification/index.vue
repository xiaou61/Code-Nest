<template>
  <div class="notification-management">
    <!-- 页面头部 -->
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div class="title-section">
          <h2>通知管理</h2>
          <p>管理系统通知，包括公告发布、消息统计、批量发送等功能</p>
        </div>
      </div>
    </el-card>

    <!-- 功能标签页 -->
    <el-card shadow="never">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 消息统计 -->
        <el-tab-pane label="消息统计" name="statistics">
          <div class="statistics-section">
            <!-- 统计查询表单 -->
            <el-row class="mb-4">
              <el-col :span="24">
                <el-form :model="statisticsForm" inline>
                  <el-form-item label="统计类型：">
                    <el-select v-model="statisticsForm.type" placeholder="选择类型" clearable>
                      <el-option label="个人消息" value="PERSONAL" />
                      <el-option label="系统公告" value="ANNOUNCEMENT" />
                      <el-option label="社区互动" value="COMMUNITY_INTERACTION" />
                      <el-option label="系统通知" value="SYSTEM" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="时间范围：">
                    <el-date-picker
                      v-model="statisticsForm.dateRange"
                      type="daterange"
                      range-separator="至"
                      start-placeholder="开始日期"
                      end-placeholder="结束日期"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                    />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="getStatisticsData">查询统计</el-button>
                  </el-form-item>
                </el-form>
              </el-col>
            </el-row>

            <!-- 统计图表 -->
            <el-row :gutter="20" v-if="statisticsData">
              <el-col :span="6">
                <el-card>
                  <div class="stat-item">
                    <div class="stat-value">{{ statisticsData.todayTotal }}</div>
                    <div class="stat-label">今日发送</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card>
                  <div class="stat-item">
                    <div class="stat-value">{{ statisticsData.monthTotal }}</div>
                    <div class="stat-label">本月发送</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card>
                  <div class="stat-item">
                    <div class="stat-value">{{ statisticsData.unreadTotal }}</div>
                    <div class="stat-label">未读消息</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card>
                  <div class="stat-item">
                    <div class="stat-value">{{ getTotalMessages() }}</div>
                    <div class="stat-label">总消息数</div>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <!-- 分类统计图表 -->
            <el-row :gutter="20" v-if="statisticsData" class="mt-4">
              <el-col :span="6">
                <el-card>
                  <div class="stat-item">
                    <div class="stat-value">{{ statisticsData.announcementCount }}</div>
                    <div class="stat-label">系统公告</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card>
                  <div class="stat-item">
                    <div class="stat-value">{{ statisticsData.personalCount }}</div>
                    <div class="stat-label">个人消息</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card>
                  <div class="stat-item">
                    <div class="stat-value">{{ statisticsData.communityCount }}</div>
                    <div class="stat-label">社区互动</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card>
                  <div class="stat-item">
                    <div class="stat-value">{{ statisticsData.systemCount }}</div>
                    <div class="stat-label">系统通知</div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- 系统公告 -->
        <el-tab-pane label="系统公告" name="announcement">
          <div class="announcement-section">
            <el-row>
              <el-col :span="24">
                <el-form :model="announcementForm" :rules="announcementRules" ref="announcementFormRef" label-width="120px">
                  <el-form-item label="公告标题" prop="title">
                    <el-input
                      v-model="announcementForm.title"
                      placeholder="请输入公告标题"
                      maxlength="200"
                      show-word-limit
                    />
                  </el-form-item>
                  <el-form-item label="公告内容" prop="content">
                    <el-input
                      v-model="announcementForm.content"
                      type="textarea"
                      :rows="8"
                      placeholder="请输入公告内容"
                    />
                  </el-form-item>
                  <el-form-item label="优先级" prop="priority">
                    <el-select v-model="announcementForm.priority">
                      <el-option label="低" value="LOW" />
                      <el-option label="中" value="MEDIUM" />
                      <el-option label="高" value="HIGH" />
                    </el-select>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="publishAnnouncement" :loading="announcementLoading">
                      发布公告
                    </el-button>
                    <el-button @click="resetAnnouncementForm">重置</el-button>
                  </el-form-item>
                </el-form>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- 消息列表 -->
        <el-tab-pane label="消息列表" name="messages">
          <div class="messages-section">
            <!-- 搜索表单 -->
            <el-row class="mb-4">
              <el-col :span="24">
                <el-form :model="messageSearchForm" inline>
                  <el-form-item label="标题：">
                    <el-input
                      v-model="messageSearchForm.title"
                      placeholder="输入标题关键词"
                      clearable
                    />
                  </el-form-item>
                  <el-form-item label="状态：">
                    <el-select v-model="messageSearchForm.status" placeholder="选择状态" clearable>
                      <el-option label="未读" value="UNREAD" />
                      <el-option label="已读" value="READ" />
                      <el-option label="已删除" value="DELETED" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="类型：">
                    <el-select v-model="messageSearchForm.type" placeholder="选择类型" clearable>
                      <el-option label="个人消息" value="PERSONAL" />
                      <el-option label="系统公告" value="ANNOUNCEMENT" />
                      <el-option label="社区互动" value="COMMUNITY_INTERACTION" />
                      <el-option label="系统通知" value="SYSTEM" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="时间：">
                    <el-date-picker
                      v-model="messageSearchForm.dateRange"
                      type="daterange"
                      range-separator="至"
                      start-placeholder="开始日期"
                      end-placeholder="结束日期"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                    />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="searchMessages">查询</el-button>
                    <el-button @click="resetMessageSearch">重置</el-button>
                  </el-form-item>
                </el-form>
              </el-col>
            </el-row>

            <!-- 消息表格 -->
            <el-table :data="messageList" v-loading="messageLoading" border>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="title" label="标题" show-overflow-tooltip />
              <el-table-column prop="type" label="类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="getTypeTagType(row.type)">
                    {{ getTypeText(row.type) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)">
                    {{ getStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="priority" label="优先级" width="100">
                <template #default="{ row }">
                  <el-tag :type="getPriorityTagType(row.priority)">
                    {{ getPriorityText(row.priority) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="receiverName" label="接收者" width="120" />
              <el-table-column prop="createTime" label="创建时间" width="180" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button
                    type="danger"
                    size="small"
                    @click="deleteMessageById(row.id)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="messagePagination.pageNum"
                v-model:page-size="messagePagination.pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="messagePagination.total"
                background
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="searchMessages"
                @current-change="searchMessages"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 批量发送 -->
        <el-tab-pane label="批量发送" name="batchSend">
          <div class="batch-send-section">
            <el-form :model="batchSendForm" :rules="batchSendRules" ref="batchSendFormRef" label-width="120px">
              <el-form-item label="接收者" prop="receiverIds">
                <el-input
                  v-model="receiverIdsText"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入接收者用户ID，多个ID用逗号分隔，如：1,2,3"
                />
              </el-form-item>
              <el-form-item label="消息标题" prop="title">
                <el-input
                  v-model="batchSendForm.title"
                  placeholder="请输入消息标题"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="消息内容" prop="content">
                <el-input
                  v-model="batchSendForm.content"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入消息内容"
                />
              </el-form-item>
              <el-form-item label="消息类型" prop="type">
                <el-select v-model="batchSendForm.type">
                  <el-option label="个人消息" value="PERSONAL" />
                  <el-option label="系统通知" value="SYSTEM" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="sendBatchMessage" :loading="batchSendLoading">
                  批量发送
                </el-button>
                <el-button @click="resetBatchSendForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 模板管理 -->
        <el-tab-pane label="模板管理" name="templates">
          <div class="templates-section">
            <div class="mb-4">
              <el-button type="primary" @click="showCreateTemplateDialog">创建模板</el-button>
            </div>

            <!-- 模板列表 -->
            <el-table :data="templateList" v-loading="templateLoading" border>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="name" label="模板名称" />
              <el-table-column prop="type" label="类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="getTypeTagType(row.type)">
                    {{ getTypeText(row.type) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="标题模板" show-overflow-tooltip />
              <el-table-column prop="content" label="内容模板" show-overflow-tooltip />
              <el-table-column prop="createTime" label="创建时间" width="180" />
              <el-table-column label="操作" width="160" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="editTemplate(row)">编辑</el-button>
                  <el-button type="danger" size="small" @click="deleteTemplateById(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 模板创建/编辑对话框 -->
    <el-dialog
      v-model="templateDialogVisible"
      :title="templateDialogTitle"
      width="600px"
    >
      <el-form :model="templateForm" :rules="templateRules" ref="templateFormRef" label-width="120px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="消息类型" prop="type">
          <el-select v-model="templateForm.type" style="width: 100%">
            <el-option label="个人消息" value="PERSONAL" />
            <el-option label="系统公告" value="ANNOUNCEMENT" />
            <el-option label="社区互动" value="COMMUNITY_INTERACTION" />
            <el-option label="系统通知" value="SYSTEM" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题模板" prop="title">
          <el-input v-model="templateForm.title" placeholder="请输入标题模板" />
        </el-form-item>
        <el-form-item label="内容模板" prop="content">
          <el-input
            v-model="templateForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入内容模板"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="templateDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveTemplate" :loading="templateSaveLoading">
            {{ isEditMode ? '更新' : '创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  publishAnnouncement as apiPublishAnnouncement,
  getStatistics,
  getAllMessages,
  batchSendMessage,
  deleteMessage,
  getTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate
} from '@/api/notification'

// 活动标签页
const activeTab = ref('statistics')

// 统计相关
const statisticsForm = reactive({
  type: '',
  dateRange: []
})

const statisticsData = ref(null)

// 公告相关
const announcementForm = reactive({
  title: '',
  content: '',
  priority: 'LOW'
})

const announcementRules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 200, message: '标题长度不能超过200字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' }
  ]
}

const announcementFormRef = ref()
const announcementLoading = ref(false)

// 消息列表相关
const messageSearchForm = reactive({
  title: '',
  status: '',
  type: '',
  dateRange: [],
  pageNum: 1,
  pageSize: 10
})

const messageList = ref([])
const messageLoading = ref(false)
const messagePagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 批量发送相关
const batchSendForm = reactive({
  receiverIds: [],
  title: '',
  content: '',
  type: 'PERSONAL'
})

const receiverIdsText = ref('')

const batchSendRules = {
  title: [
    { required: true, message: '请输入消息标题', trigger: 'blur' },
    { max: 200, message: '标题长度不能超过200字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入消息内容', trigger: 'blur' }
  ]
}

const batchSendFormRef = ref()
const batchSendLoading = ref(false)

// 模板管理相关
const templateList = ref([])
const templateLoading = ref(false)
const templateDialogVisible = ref(false)
const templateDialogTitle = ref('')
const isEditMode = ref(false)
const templateSaveLoading = ref(false)

const templateForm = reactive({
  id: null,
  name: '',
  type: 'PERSONAL',
  title: '',
  content: ''
})

const templateRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择消息类型', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入标题模板', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容模板', trigger: 'blur' }
  ]
}

const templateFormRef = ref()

// 监听接收者ID文本变化
watch(receiverIdsText, (val) => {
  if (val) {
    batchSendForm.receiverIds = val.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id))
  } else {
    batchSendForm.receiverIds = []
  }
})

// 获取统计数据
const getStatisticsData = async () => {
  try {
    const params = {
      type: statisticsForm.type
    }

    if (statisticsForm.dateRange && statisticsForm.dateRange.length === 2) {
      params.startTime = statisticsForm.dateRange[0]
      params.endTime = statisticsForm.dateRange[1]
    }

    const response = await getStatistics(params)
    statisticsData.value = response
    ElMessage.success('统计数据获取成功')
  } catch (error) {
    ElMessage.error('获取统计数据失败：' + error.message)
  }
}

// 发布公告
const publishAnnouncement = async () => {
  if (!announcementFormRef.value) return

  const valid = await announcementFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    announcementLoading.value = true
    await apiPublishAnnouncement(announcementForm)
    ElMessage.success('公告发布成功')
    resetAnnouncementForm()
  } catch (error) {
    ElMessage.error('发布公告失败：' + error.message)
  } finally {
    announcementLoading.value = false
  }
}

// 重置公告表单
const resetAnnouncementForm = () => {
  if (announcementFormRef.value) {
    announcementFormRef.value.resetFields()
  }
  Object.assign(announcementForm, {
    title: '',
    content: '',
    priority: 'LOW'
  })
}

// 搜索消息
const searchMessages = async () => {
  try {
    messageLoading.value = true

    const params = {
      ...messageSearchForm,
      pageNum: messagePagination.pageNum,
      pageSize: messagePagination.pageSize
    }

    if (messageSearchForm.dateRange && messageSearchForm.dateRange.length === 2) {
      params.startTime = messageSearchForm.dateRange[0]
      params.endTime = messageSearchForm.dateRange[1]
    }

    const response = await getAllMessages(params)
    messageList.value = response.records
    messagePagination.total = response.total
  } catch (error) {
    ElMessage.error('获取消息列表失败：' + error.message)
  } finally {
    messageLoading.value = false
  }
}

// 重置消息搜索
const resetMessageSearch = () => {
  Object.assign(messageSearchForm, {
    title: '',
    status: '',
    type: '',
    dateRange: []
  })
  messagePagination.pageNum = 1
  searchMessages()
}

// 删除消息
const deleteMessageById = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteMessage(id)
    ElMessage.success('删除成功')
    searchMessages()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error('删除失败：' + error.message)
  }
}

// 批量发送消息
const sendBatchMessage = async () => {
  if (!batchSendFormRef.value) return

  const valid = await batchSendFormRef.value.validate().catch(() => false)
  if (!valid) return

  if (batchSendForm.receiverIds.length === 0) {
    ElMessage.error('请输入有效的接收者ID')
    return
  }

  try {
    batchSendLoading.value = true
    await batchSendMessage(batchSendForm)
    ElMessage.success('批量发送成功')
    resetBatchSendForm()
  } catch (error) {
    ElMessage.error('批量发送失败：' + error.message)
  } finally {
    batchSendLoading.value = false
  }
}

// 重置批量发送表单
const resetBatchSendForm = () => {
  if (batchSendFormRef.value) {
    batchSendFormRef.value.resetFields()
  }
  Object.assign(batchSendForm, {
    receiverIds: [],
    title: '',
    content: '',
    type: 'PERSONAL'
  })
  receiverIdsText.value = ''
}

// 获取模板列表
const getTemplateList = async () => {
  try {
    templateLoading.value = true
    const response = await getTemplates()
    templateList.value = response
  } catch (error) {
    ElMessage.error('获取模板列表失败：' + error.message)
  } finally {
    templateLoading.value = false
  }
}

// 显示创建模板对话框
const showCreateTemplateDialog = () => {
  isEditMode.value = false
  templateDialogTitle.value = '创建模板'
  resetTemplateForm()
  templateDialogVisible.value = true
}

// 编辑模板
const editTemplate = (template) => {
  isEditMode.value = true
  templateDialogTitle.value = '编辑模板'
  Object.assign(templateForm, template)
  templateDialogVisible.value = true
}

// 保存模板
const saveTemplate = async () => {
  if (!templateFormRef.value) return

  const valid = await templateFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    templateSaveLoading.value = true

    if (isEditMode.value) {
      await updateTemplate(templateForm.id, templateForm)
      ElMessage.success('模板更新成功')
    } else {
      await createTemplate(templateForm)
      ElMessage.success('模板创建成功')
    }

    templateDialogVisible.value = false
    getTemplateList()
  } catch (error) {
    ElMessage.error('保存模板失败：' + error.message)
  } finally {
    templateSaveLoading.value = false
  }
}

// 删除模板
const deleteTemplateById = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个模板吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteTemplate(id)
    ElMessage.success('删除成功')
    getTemplateList()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error('删除失败：' + error.message)
  }
}

// 重置模板表单
const resetTemplateForm = () => {
  if (templateFormRef.value) {
    templateFormRef.value.resetFields()
  }
  Object.assign(templateForm, {
    id: null,
    name: '',
    type: 'PERSONAL',
    title: '',
    content: ''
  })
}

// 工具方法
const getTypeText = (type) => {
  const typeMap = {
    'PERSONAL': '个人消息',
    'ANNOUNCEMENT': '系统公告',
    'COMMUNITY_INTERACTION': '社区互动',
    'SYSTEM': '系统通知'
  }
  return typeMap[type] || type
}

const getTypeTagType = (type) => {
  const typeMap = {
    'PERSONAL': '',
    'ANNOUNCEMENT': 'success',
    'COMMUNITY_INTERACTION': 'warning',
    'SYSTEM': 'info'
  }
  return typeMap[type] || ''
}

const getStatusText = (status) => {
  const statusMap = {
    'UNREAD': '未读',
    'READ': '已读',
    'DELETED': '已删除'
  }
  return statusMap[status] || status
}

const getStatusTagType = (status) => {
  const statusMap = {
    'UNREAD': 'warning',
    'read': 'success',
    'DELETED': 'danger'
  }
  return statusMap[status] || ''
}

const getPriorityText = (priority) => {
  const priorityMap = {
    'LOW': '低',
    'MEDIUM': '中',
    'HIGH': '高'
  }
  return priorityMap[priority] || priority
}

const getPriorityTagType = (priority) => {
  const priorityMap = {
    'LOW': 'info',
    'MEDIUM': 'warning',
    'HIGH': 'danger'
  }
  return priorityMap[priority] || ''
}

const getTotalMessages = () => {
  if (!statisticsData.value) return 0
  const { announcementCount, personalCount, communityCount, systemCount } = statisticsData.value
  return (announcementCount || 0) + (personalCount || 0) + (communityCount || 0) + (systemCount || 0)
}

// 初始化
onMounted(() => {
  getStatisticsData()
  searchMessages()
  getTemplateList()
})
</script>

<style scoped>
.notification-management {
  padding: 20px;
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
  font-size: 20px;
  font-weight: 600;
}

.title-section p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.statistics-section,
.announcement-section,
.messages-section,
.batch-send-section,
.templates-section {
  padding: 20px 0;
}

.stat-item {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.mb-4 {
  margin-bottom: 16px;
}

.mt-4 {
  margin-top: 16px;
}

.dialog-footer {
  text-align: right;
}
</style>
