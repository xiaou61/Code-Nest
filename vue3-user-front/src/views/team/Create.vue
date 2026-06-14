<template>
  <CnPage class="create-team-container" surface="transparent" max-width="1040px">
    <CnPageHeader
      :title="isEdit ? '编辑小组' : '创建小组'"
      :description="isEdit ? '更新小组资料、加入方式和阶段目标。' : '设置一个学习小组，让成员围绕目标一起打卡和讨论。'"
      eyebrow="Learning Team"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag :type="isEdit ? 'info' : 'brand'" size="sm">
          {{ isEdit ? '编辑模式' : '新建小组' }}
        </CnStatusTag>
        <CnStatusTag type="neutral" size="sm">
          {{ getTypeLabel(form.teamType) }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '创建小组' }}
        </el-button>
      </template>
    </CnPageHeader>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="team-form"
      @submit.prevent="handleSubmit"
    >
      <div class="form-layout">
        <main class="form-main">
          <CnSection title="基本信息" description="小组名称、简介和头像会展示在小组广场与详情页。" surface="panel" divided>
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
                  <img v-if="form.teamAvatar" :src="form.teamAvatar" alt="小组头像" />
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
                    <el-button type="primary" :icon="Plus" :loading="avatarUploading">
                      {{ avatarUploading ? '上传中...' : '上传头像' }}
                    </el-button>
                  </el-upload>
                  <span class="upload-tip">支持 jpg、png、gif 格式，不超过 2MB</span>
                </div>
              </div>
            </el-form-item>
          </CnSection>

          <CnSection title="小组设置" description="选择协作类型、容量和加入规则。" surface="panel" divided>
            <el-form-item label="小组类型" prop="teamType">
              <div class="type-selector">
                <button
                  v-for="typeItem in typeOptions"
                  :key="typeItem.value"
                  type="button"
                  class="type-option"
                  :class="{ active: form.teamType === typeItem.value }"
                  @click="form.teamType = typeItem.value"
                >
                  <span class="type-icon">{{ typeItem.icon }}</span>
                  <span class="type-name">{{ typeItem.label }}</span>
                  <span class="type-desc">{{ typeItem.desc }}</span>
                </button>
              </div>
            </el-form-item>

            <div class="settings-grid">
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

              <el-form-item label="加入方式" prop="joinType">
                <el-radio-group v-model="form.joinType" class="join-group">
                  <el-radio :label="1">
                    <span class="join-option">
                      <span>公开加入</span>
                      <span class="join-desc">任何人可直接加入</span>
                    </span>
                  </el-radio>
                  <el-radio :label="2">
                    <span class="join-option">
                      <span>申请加入</span>
                      <span class="join-desc">需要组长审批</span>
                    </span>
                  </el-radio>
                  <el-radio :label="3">
                    <span class="join-option">
                      <span>邀请加入</span>
                      <span class="join-desc">仅限邀请码</span>
                    </span>
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </div>

            <el-form-item label="小组标签">
              <div class="tags-input">
                <CnStatusTag
                  v-for="tag in tagList"
                  :key="tag"
                  type="info"
                  size="sm"
                  subtle
                  class="tag-chip"
                >
                  {{ tag }}
                  <button type="button" class="tag-remove" @click.stop="removeTag(tag)">×</button>
                </CnStatusTag>
                <el-input
                  v-if="tagList.length < 5"
                  v-model="tagInput"
                  placeholder="输入标签后回车"
                  class="tag-input"
                  maxlength="10"
                  @keyup.enter="addTag"
                />
              </div>
              <div class="tags-hint">
                <span>推荐标签：</span>
                <button
                  v-for="tag in suggestedTags"
                  :key="tag"
                  type="button"
                  class="suggested-tag"
                  @click="addSuggestedTag(tag)"
                >
                  {{ tag }}
                </button>
              </div>
            </el-form-item>
          </CnSection>

          <CnSection title="小组目标" description="可选。用于展示阶段目标、目标周期和每日目标量。" surface="panel" divided>
            <template #actions>
              <CnStatusTag type="neutral" size="sm">选填</CnStatusTag>
            </template>

            <el-form-item label="目标标题">
              <el-input v-model="form.goalTitle" placeholder="如：30天刷完LeetCode热题100" maxlength="100" />
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

            <div class="settings-grid">
              <el-form-item label="目标周期">
                <el-date-picker
                  v-model="goalDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  class="full-control"
                />
              </el-form-item>

              <el-form-item label="每日目标量">
                <el-input-number
                  v-model="form.dailyTarget"
                  :min="1"
                  :max="999"
                  placeholder="如：3"
                  class="full-control"
                />
                <span class="unit-hint">道题/小时/页等</span>
              </el-form-item>
            </div>
          </CnSection>
        </main>

        <aside class="form-side">
          <CnSection title="创建摘要" surface="panel" compact divided>
            <div class="summary-card">
              <div class="summary-avatar">
                <img v-if="form.teamAvatar" :src="form.teamAvatar" alt="小组头像预览" />
                <span v-else>{{ form.teamName?.charAt(0) || '组' }}</span>
              </div>
              <div class="summary-name">{{ form.teamName || '未命名小组' }}</div>
              <p class="summary-desc">{{ form.teamDesc || '暂无简介' }}</p>
              <div class="summary-tags">
                <CnStatusTag type="brand" size="sm">{{ getTypeLabel(form.teamType) }}</CnStatusTag>
                <CnStatusTag type="info" size="sm">最多 {{ form.maxMembers }} 人</CnStatusTag>
                <CnStatusTag type="neutral" size="sm">{{ getJoinTypeLabel(form.joinType) }}</CnStatusTag>
              </div>
            </div>
          </CnSection>

          <CnSection title="提交检查" surface="panel" compact divided>
            <div class="check-list">
              <div class="check-item">
                <span>名称</span>
                <CnStatusTag :type="form.teamName.length >= 2 ? 'success' : 'warning'" size="sm">
                  {{ form.teamName.length }}/20
                </CnStatusTag>
              </div>
              <div class="check-item">
                <span>类型</span>
                <CnStatusTag type="success" size="sm">{{ getTypeLabel(form.teamType) }}</CnStatusTag>
              </div>
              <div class="check-item">
                <span>成员容量</span>
                <CnStatusTag type="info" size="sm">{{ form.maxMembers }} 人</CnStatusTag>
              </div>
              <div class="check-item">
                <span>标签</span>
                <CnStatusTag :type="tagList.length <= 5 ? 'success' : 'danger'" size="sm">
                  {{ tagList.length }}/5
                </CnStatusTag>
              </div>
            </div>
          </CnSection>
        </aside>
      </div>

      <div class="form-actions">
        <el-button size="large" @click="goBack">取消</el-button>
        <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '创建小组' }}
        </el-button>
      </div>
    </el-form>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadProps, UploadUserFile } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import teamApi from '@/api/team'
import { uploadSingle } from '@/api/upload'

interface TeamForm {
  teamName: string
  teamDesc: string
  teamAvatar: string
  teamType: number
  maxMembers: number
  joinType: number
  tags: string
  goalTitle: string
  goalDesc: string
  goalStartDate: string
  goalEndDate: string
  dailyTarget: number | null
}

interface TeamDetailResponse extends Partial<TeamForm> {}

interface TypeOption {
  value: number
  label: string
  icon: string
  desc: string
}

type GoalDateRange = [string, string] | []

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => route.name === 'TeamEdit')
const teamId = computed(() => getFirstRouteParam(route.params.id))

const breadcrumbs = [
  { label: '小组广场', to: '/team' },
  { label: '小组表单' }
]

const formRef = ref<FormInstance>()
const submitting = ref(false)
const avatarUploading = ref(false)

const form = ref<TeamForm>({
  teamName: '',
  teamDesc: '',
  teamAvatar: '',
  teamType: 2,
  maxMembers: 20,
  joinType: 1,
  tags: '',
  goalTitle: '',
  goalDesc: '',
  goalStartDate: '',
  goalEndDate: '',
  dailyTarget: null
})

const tagList = ref<string[]>([])
const tagInput = ref('')
const suggestedTags = ['Java', '前端', '算法', '秋招', 'Python', 'Go', '转行', '刷题', '面试', 'LeetCode']
const goalDateRange = ref<GoalDateRange>([])

const typeOptions: TypeOption[] = [
  { value: 1, label: '目标型', icon: '🎯', desc: '为特定目标组建' },
  { value: 2, label: '学习型', icon: '📖', desc: '长期学习交流' },
  { value: 3, label: '打卡型', icon: '✅', desc: '互相监督打卡' }
]

const memberMarks = {
  2: '2',
  10: '10',
  20: '20',
  30: '30',
  50: '50'
}

const rules: FormRules<TeamForm> = {
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

watch(goalDateRange, (val) => {
  if (val && val.length === 2) {
    form.value.goalStartDate = val[0]
    form.value.goalEndDate = val[1]
  } else {
    form.value.goalStartDate = ''
    form.value.goalEndDate = ''
  }
})

const getFirstRouteParam = (param: unknown) => {
  return Array.isArray(param) ? String(param[0] || '') : String(param || '')
}

const getTypeLabel = (type: number) => {
  return typeOptions.find(item => item.value === type)?.label || '学习型'
}

const getJoinTypeLabel = (type: number) => {
  const joinTypeMap: Record<number, string> = {
    1: '公开加入',
    2: '申请加入',
    3: '邀请加入'
  }
  return joinTypeMap[type] || '公开加入'
}

const loadTeamDetail = async () => {
  try {
    const response = await teamApi.getTeamDetail(teamId.value) as TeamDetailResponse
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

      if (response.tags) {
        tagList.value = response.tags.split(',').map(tag => tag.trim()).filter(Boolean)
      }

      if (response.goalStartDate && response.goalEndDate) {
        goalDateRange.value = [response.goalStartDate, response.goalEndDate]
      }
    }
  } catch (error) {
    console.error('加载小组详情失败:', error)
    ElMessage.error('加载失败')
  }
}

const syncTagsToForm = () => {
  form.value.tags = tagList.value.join(',')
}

const addTag = () => {
  const tag = tagInput.value.trim()
  if (tag && !tagList.value.includes(tag) && tagList.value.length < 5) {
    tagList.value.push(tag)
    tagInput.value = ''
    syncTagsToForm()
  }
}

const addSuggestedTag = (tag: string) => {
  if (!tagList.value.includes(tag) && tagList.value.length < 5) {
    tagList.value.push(tag)
    syncTagsToForm()
  }
}

const removeTag = (tag: string) => {
  tagList.value = tagList.value.filter(item => item !== tag)
  syncTagsToForm()
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (rawFile) => {
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

const handleAvatarChange = async (uploadFile: UploadUserFile) => {
  if (!uploadFile.raw) return

  if (!beforeAvatarUpload(uploadFile.raw)) return

  try {
    avatarUploading.value = true
    const response = await uploadSingle(uploadFile.raw, 'team', 'avatar')
    form.value.teamAvatar = response.data?.accessUrl || response.accessUrl || response.url || response
    ElMessage.success('头像上传成功')
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败，请重试')
  } finally {
    avatarUploading.value = false
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitting.value = true

    const submitData = { ...form.value }

    if (isEdit.value) {
      await teamApi.updateTeam(teamId.value, submitData)
      ElMessage.success('修改成功')
      router.push(`/team/${teamId.value}`)
    } else {
      await teamApi.createTeam(submitData)
      ElMessage.success('创建成功')
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

const goBack = () => {
  router.back()
}

onMounted(async () => {
  if (isEdit.value && teamId.value) {
    await loadTeamDetail()
  }
})
</script>

<style scoped>
.create-team-container {
  min-height: calc(100vh - 68px);
}

.team-form {
  min-width: 0;
}

.form-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  align-items: start;
  gap: var(--cn-space-5);
}

.form-main {
  display: grid;
  gap: var(--cn-space-5);
  min-width: 0;
}

.form-side {
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
  position: sticky;
  top: 84px;
}

.avatar-upload {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-4);
}

.avatar-preview,
.summary-avatar {
  overflow: hidden;
  border-radius: var(--cn-radius-card);
  background: color-mix(in srgb, var(--cn-color-brand-primary) 74%, var(--cn-color-info));
  color: white;
  flex-shrink: 0;
}

.avatar-preview {
  width: 80px;
  height: 80px;
}

.avatar-preview img,
.summary-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text,
.summary-avatar span {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 800;
}

.avatar-actions {
  display: grid;
  gap: var(--cn-space-2);
}

.upload-tip,
.unit-hint,
.join-desc,
.type-desc {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.type-selector {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.type-option {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-2);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  padding: var(--cn-space-4);
  text-align: center;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    color var(--cn-motion-fast) var(--cn-ease-out);
}

.type-option:hover,
.type-option.active {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.type-icon {
  font-size: 28px;
  line-height: 1;
}

.type-name {
  color: var(--cn-color-text-primary);
  font-weight: 700;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-5);
}

.join-group {
  display: grid;
  gap: var(--cn-space-2);
}

.join-option {
  display: grid;
  gap: 2px;
}

.tags-input {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.tag-chip {
  gap: var(--cn-space-1);
}

.tag-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-left: 2px;
  border: 0;
  border-radius: var(--cn-radius-pill);
  background: transparent;
  color: inherit;
  cursor: pointer;
  font: inherit;
  padding: 0;
}

.tag-remove:hover {
  background: color-mix(in srgb, currentColor 12%, transparent);
}

.tag-input {
  width: 140px;
}

.tags-hint {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.suggested-tag {
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  font: inherit;
  padding: 3px 9px;
}

.suggested-tag:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 30%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.full-control {
  width: 100%;
}

.summary-card {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-3);
  text-align: center;
}

.summary-avatar {
  width: 72px;
  height: 72px;
}

.summary-name {
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 750;
}

.summary-desc {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.summary-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--cn-space-2);
}

.check-list {
  display: grid;
  gap: var(--cn-space-3);
}

.check-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-text-secondary);
  padding-bottom: var(--cn-space-3);
}

.check-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: var(--cn-space-3);
  padding-top: var(--cn-space-5);
}

.form-actions .el-button {
  min-width: 120px;
}

@media (max-width: 980px) {
  .form-layout {
    grid-template-columns: 1fr;
  }

  .form-side {
    position: static;
  }
}

@media (max-width: 768px) {
  .type-selector,
  .settings-grid {
    grid-template-columns: 1fr;
  }

  .avatar-upload {
    flex-direction: column;
  }

  .form-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
