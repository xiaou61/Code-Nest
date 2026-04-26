<template>
  <div class="create-team-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h1>{{ isEdit ? '编辑小组' : '创建小组' }}</h1>
    </div>

    <!-- 表单区域 -->
    <div class="form-card">
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <!-- 基本信息 -->
        <div class="form-section">
          <h3 class="section-title">📝 基本信息</h3>
          
          <el-form-item label="小组名称" prop="teamName">
            <el-input 
              v-model="form.teamName" 
              placeholder="给小组起个响亮的名字（2-20字符）"
              maxlength="20"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="小组简介" prop="teamDesc">
            <el-input 
              v-model="form.teamDesc" 
              type="textarea"
              :rows="3"
              placeholder="简单介绍一下小组的定位和目标（最多200字）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="小组头像">
            <div class="avatar-upload">
              <div class="avatar-preview">
                <img v-if="form.teamAvatar" :src="form.teamAvatar" />
                <span v-else class="avatar-text">{{ form.teamName?.charAt(0) || '组' }}</span>
              </div>
              <div class="avatar-actions">
                <el-upload
                  :auto-upload="false"
                  :show-file-list="false"
                  :before-upload="beforeAvatarUpload"
                  :on-change="handleAvatarChange"
                  accept="image/*"
                >
                  <el-button type="primary" :loading="avatarUploading">
                    <el-icon><Plus /></el-icon>
                    {{ avatarUploading ? '上传中...' : '上传头像' }}
                  </el-button>
                </el-upload>
                <span class="upload-tip">支持jpg、png、gif格式，不超过2MB</span>
              </div>
            </div>
          </el-form-item>
        </div>

        <!-- 小组设置 -->
        <div class="form-section">
          <h3 class="section-title">⚙️ 小组设置</h3>
          
          <el-form-item label="小组类型" prop="teamType">
            <div class="type-selector">
              <div 
                v-for="typeItem in typeOptions" 
                :key="typeItem.value"
                class="type-option"
                :class="{ active: form.teamType === typeItem.value }"
                @click="form.teamType = typeItem.value"
              >
                <span class="type-icon">{{ typeItem.icon }}</span>
                <span class="type-name">{{ typeItem.label }}</span>
                <span class="type-desc">{{ typeItem.desc }}</span>
              </div>
            </div>
          </el-form-item>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="最大成员数" prop="maxMembers">
                <el-slider 
                  v-model="form.maxMembers" 
                  :min="2" 
                  :max="50"
                  :marks="memberMarks"
                  show-input
                  :show-input-controls="false"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="加入方式" prop="joinType">
                <el-radio-group v-model="form.joinType">
                  <el-radio :label="1">
                    <span class="join-option">
                      <span>🔓 公开加入</span>
                      <span class="join-desc">任何人可直接加入</span>
                    </span>
                  </el-radio>
                  <el-radio :label="2">
                    <span class="join-option">
                      <span>📝 申请加入</span>
                      <span class="join-desc">需要组长审批</span>
                    </span>
                  </el-radio>
                  <el-radio :label="3">
                    <span class="join-option">
                      <span>🔒 邀请加入</span>
                      <span class="join-desc">仅限邀请码</span>
                    </span>
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="小组标签">
            <div class="tags-input">
              <el-tag 
                v-for="tag in tagList" 
                :key="tag"
                closable
                @close="removeTag(tag)"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
              <el-input 
                v-if="tagList.length < 5"
                v-model="tagInput"
                placeholder="输入标签后回车"
                style="width: 120px"
                @keyup.enter="addTag"
                maxlength="10"
              />
            </div>
            <div class="tags-hint">
              <span>推荐标签：</span>
              <span 
                v-for="tag in suggestedTags" 
                :key="tag" 
                class="suggested-tag"
                @click="addSuggestedTag(tag)"
              >
                {{ tag }}
              </span>
            </div>
          </el-form-item>
        </div>

        <!-- 小组目标（可选） -->
        <div class="form-section">
          <h3 class="section-title">
            🎯 小组目标
            <span class="optional-badge">选填</span>
          </h3>
          
          <el-form-item label="目标标题">
            <el-input 
              v-model="form.goalTitle" 
              placeholder="如：30天刷完LeetCode热题100"
              maxlength="100"
            />
          </el-form-item>

          <el-form-item label="目标描述">
            <el-input 
              v-model="form.goalDesc" 
              type="textarea"
              :rows="2"
              placeholder="详细描述小组的学习目标"
              maxlength="500"
            />
          </el-form-item>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="目标周期">
                <el-date-picker
                  v-model="goalDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="每日目标量">
                <el-input-number 
                  v-model="form.dailyTarget" 
                  :min="1" 
                  :max="999"
                  placeholder="如：3"
                  style="width: 100%"
                />
                <span class="unit-hint">道题/小时/页等</span>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 提交按钮 -->
        <div class="form-actions">
          <el-button size="large" @click="goBack">取消</el-button>
          <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '创建小组' }}
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import teamApi from '@/api/team'
import { uploadSingle } from '@/api/upload'

const router = useRouter()
const route = useRoute()

// 是否编辑模式
const isEdit = computed(() => route.name === 'TeamEdit')
const teamId = computed(() => route.params.id)

// 表单引用
const formRef = ref(null)
const submitting = ref(false)
const avatarUploading = ref(false)

// 表单数据
const form = ref({
  teamName: '',
  teamDesc: '',
  teamAvatar: '',
  teamType: 2, // 默认学习型
  maxMembers: 20,
  joinType: 1, // 默认公开
  tags: '',
  goalTitle: '',
  goalDesc: '',
  goalStartDate: '',
  goalEndDate: '',
  dailyTarget: null
})

// 标签相关
const tagList = ref([])
const tagInput = ref('')
const suggestedTags = ['Java', '前端', '算法', '秋招', 'Python', 'Go', '转行', '刷题', '面试', 'LeetCode']

// 目标日期范围
const goalDateRange = ref([])

// 监听日期范围变化
watch(goalDateRange, (val) => {
  if (val && val.length === 2) {
    form.value.goalStartDate = val[0]
    form.value.goalEndDate = val[1]
  } else {
    form.value.goalStartDate = ''
    form.value.goalEndDate = ''
  }
})

// 类型选项
const typeOptions = [
  { value: 1, label: '目标型', icon: '🎯', desc: '为特定目标组建' },
  { value: 2, label: '学习型', icon: '📖', desc: '长期学习交流' },
  { value: 3, label: '打卡型', icon: '✅', desc: '互相监督打卡' }
]

// 成员数标记
const memberMarks = {
  2: '2',
  10: '10',
  20: '20',
  30: '30',
  50: '50'
}

// 表单验证规则
const rules = {
  teamName: [
    { required: true, message: '请输入小组名称', trigger: 'blur' },
    { min: 2, max: 20, message: '名称长度在2-20个字符', trigger: 'blur' }
  ],
  teamType: [
    { required: true, message: '请选择小组类型', trigger: 'change' }
  ],
  maxMembers: [
    { required: true, message: '请设置最大成员数', trigger: 'change' }
  ],
  joinType: [
    { required: true, message: '请选择加入方式', trigger: 'change' }
  ]
}

// 页面初始化
onMounted(async () => {
  if (isEdit.value && teamId.value) {
    await loadTeamDetail()
  }
})

// 加载小组详情（编辑模式）
const loadTeamDetail = async () => {
  try {
    const response = await teamApi.getTeamDetail(teamId.value)
    if (response) {
      form.value = {
        teamName: response.teamName || '',
        teamDesc: response.teamDesc || '',
        teamAvatar: response.teamAvatar || '',
        teamType: response.teamType || 2,
        maxMembers: response.maxMembers || 20,
        joinType: response.joinType || 1,
        tags: response.tags || '',
        goalTitle: response.goalTitle || '',
        goalDesc: response.goalDesc || '',
        goalStartDate: response.goalStartDate || '',
        goalEndDate: response.goalEndDate || '',
        dailyTarget: response.dailyTarget || null
      }
      
      // 解析标签
      if (response.tags) {
        tagList.value = response.tags.split(',').filter(t => t.trim())
      }
      
      // 设置日期范围
      if (response.goalStartDate && response.goalEndDate) {
        goalDateRange.value = [response.goalStartDate, response.goalEndDate]
      }
    }
  } catch (error) {
    console.error('加载小组详情失败:', error)
    ElMessage.error('加载失败')
  }
}

// 添加标签
const addTag = () => {
  const tag = tagInput.value.trim()
  if (tag && !tagList.value.includes(tag) && tagList.value.length < 5) {
    tagList.value.push(tag)
    tagInput.value = ''
    form.value.tags = tagList.value.join(',')
  }
}

// 添加推荐标签
const addSuggestedTag = (tag) => {
  if (!tagList.value.includes(tag) && tagList.value.length < 5) {
    tagList.value.push(tag)
    form.value.tags = tagList.value.join(',')
  }
}

// 移除标签
const removeTag = (tag) => {
  tagList.value = tagList.value.filter(t => t !== tag)
  form.value.tags = tagList.value.join(',')
}

// 头像上传前校验
const beforeAvatarUpload = (rawFile) => {
  const isValidType = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'].includes(rawFile.type)
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isValidType) {
    ElMessage.error('头像只能是 JPG、PNG、GIF 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

// 头像文件选择变化
const handleAvatarChange = async (uploadFile) => {
  if (!uploadFile.raw) return
  
  // 校验文件
  if (!beforeAvatarUpload(uploadFile.raw)) return
  
  try {
    avatarUploading.value = true
    const response = await uploadSingle(uploadFile.raw, 'team', 'avatar')
    
    // 更新头像显示
    form.value.teamAvatar = response.data?.accessUrl || response.accessUrl || response.url || response
    ElMessage.success('头像上传成功')
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败，请重试')
  } finally {
    avatarUploading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    submitting.value = true
    
    const submitData = { ...form.value }
    
    if (isEdit.value) {
      await teamApi.updateTeam(teamId.value, submitData)
      ElMessage.success('修改成功')
      router.push(`/team/${teamId.value}`)
    } else {
      await teamApi.createTeam(submitData)
      ElMessage.success('创建成功')
      // 返回小组广场页面
      router.push('/team')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败:', error)
    }
  } finally {
    submitting.value = false
  }
}

// 返回
const goBack = () => {
  router.back()
}
</script>

<style lang="scss" scoped>
.create-team-container {
  padding: 24px 32px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
  
  @media (max-width: 768px) {
    padding: 16px;
  }
}

// 页面头部
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  
  h1 {
    font-size: 20px;
    margin: 0;
    color: #333;
  }
}

// 表单卡片
.form-card {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

// 表单分区
.form-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eee;
  
  &:last-of-type {
    border-bottom: none;
  }
  
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin: 0 0 20px 0;
    display: flex;
    align-items: center;
    gap: 8px;
    
    .optional-badge {
      font-size: 12px;
      font-weight: normal;
      color: #999;
      background: #f5f7fa;
      padding: 2px 8px;
      border-radius: 4px;
    }
  }
}

// 头像上传
.avatar-upload {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  
  .avatar-preview {
    width: 80px;
    height: 80px;
    border-radius: 12px;
    overflow: hidden;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    flex-shrink: 0;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .avatar-text {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      font-size: 32px;
      font-weight: bold;
      color: white;
    }
  }
  
  .avatar-actions {
    display: flex;
    flex-direction: column;
    gap: 8px;
    
    .upload-tip {
      font-size: 12px;
      color: #999;
    }
  }
}

// 类型选择器
.type-selector {
  display: flex;
  gap: 16px;
  
  @media (max-width: 600px) {
    flex-direction: column;
  }
  
  .type-option {
    flex: 1;
    padding: 16px;
    border: 2px solid #eee;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s;
    text-align: center;
    
    &:hover {
      border-color: #409eff;
    }
    
    &.active {
      border-color: #409eff;
      background: #ecf5ff;
    }
    
    .type-icon {
      display: block;
      font-size: 32px;
      margin-bottom: 8px;
    }
    
    .type-name {
      display: block;
      font-size: 15px;
      font-weight: 600;
      color: #333;
      margin-bottom: 4px;
    }
    
    .type-desc {
      display: block;
      font-size: 12px;
      color: #999;
    }
  }
}

// 加入方式
:deep(.el-radio-group) {
  display: flex;
  flex-direction: column;
  gap: 12px;
  
  .el-radio {
    height: auto;
    
    .el-radio__label {
      padding-left: 8px;
    }
  }
}

.join-option {
  display: flex;
  flex-direction: column;
  
  .join-desc {
    font-size: 12px;
    color: #999;
    margin-top: 2px;
  }
}

// 标签输入
.tags-input {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  
  .tag-item {
    margin: 0;
  }
}

.tags-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #999;
  
  .suggested-tag {
    display: inline-block;
    padding: 2px 8px;
    margin: 0 4px;
    background: #f5f7fa;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover {
      background: #ecf5ff;
      color: #409eff;
    }
  }
}

// 单位提示
.unit-hint {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

// 表单操作
.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 24px;
  
  .el-button {
    min-width: 120px;
  }
}

// 滑块样式
:deep(.el-slider) {
  .el-slider__marks-text {
    font-size: 12px;
  }
}
</style>
