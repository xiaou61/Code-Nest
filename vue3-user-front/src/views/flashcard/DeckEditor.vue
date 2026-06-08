<template>
  <CnPage class="deck-editor-page" max-width="1080px" full-height>
    <CnPageHeader
      :title="isEdit ? '编辑卡组' : '创建卡组'"
      :description="isEdit ? '更新卡组资料、封面和公开状态。' : '建立一个新的闪卡卡组，用于沉淀可复习的知识点。'"
      eyebrow="DECK EDITOR"
      :breadcrumbs="[
        { label: '闪卡记忆', to: '/flashcard' },
        { label: '我的卡组', to: '/flashcard/my' },
        { label: isEdit ? '编辑卡组' : '创建卡组' }
      ]"
    >
      <template #meta>
        <CnStatusTag :type="form.isPublic ? 'success' : 'info'" size="sm">
          {{ form.isPublic ? '公开卡组' : '私有卡组' }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="sm" subtle>
          {{ tagList.length || 0 }} 个标签
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '创建卡组' }}
        </el-button>
      </template>
    </CnPageHeader>

    <div class="editor-layout">
      <CnSection title="卡组资料" description="名称、描述、标签和可见性会影响卡组在个人中心与公开广场中的展示。" divided>
        <el-form
          ref="formRef"
          v-loading="loading"
          :model="form"
          :rules="rules"
          label-position="top"
          class="deck-form"
        >
          <el-form-item label="卡组名称" prop="name">
            <el-input
              v-model="form.name"
              placeholder="请输入卡组名称"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              placeholder="请输入卡组描述"
              :rows="4"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="封面图片">
            <div class="cover-upload">
              <div v-if="form.coverImage" class="cover-preview">
                <img :src="form.coverImage" alt="封面预览" @error="handleImageError" />
                <div class="cover-actions">
                  <el-button type="danger" size="small" :icon="Delete" @click="form.coverImage = ''">
                    删除封面
                  </el-button>
                </div>
              </div>
              <el-upload
                v-else
                class="cover-uploader"
                :auto-upload="false"
                :show-file-list="false"
                :before-upload="beforeCoverUpload"
                :on-change="handleCoverChange"
                accept="image/*"
              >
                <div class="upload-placeholder">
                  <el-icon class="upload-icon" :class="{ 'is-loading': coverUploading }">
                    <Loading v-if="coverUploading" />
                    <Plus v-else />
                  </el-icon>
                  <span class="upload-text">{{ coverUploading ? '上传中...' : '点击上传封面' }}</span>
                  <span class="upload-hint">JPG / PNG / GIF，不超过 2MB</span>
                </div>
              </el-upload>
            </div>
          </el-form-item>

          <el-form-item label="标签" prop="tags">
            <el-input v-model="form.tags" placeholder="多个标签用逗号分隔，如：Java,面试,基础" />
          </el-form-item>

          <el-form-item label="公开设置" prop="isPublic">
            <div class="visibility-row">
              <el-switch v-model="form.isPublic" active-text="公开" inactive-text="私有" />
              <span class="hint">公开的卡组可以被其他用户搜索和 Fork。</span>
            </div>
          </el-form-item>

          <el-form-item class="form-actions">
            <el-button @click="goBack">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              {{ isEdit ? '保存修改' : '创建卡组' }}
            </el-button>
          </el-form-item>
        </el-form>
      </CnSection>

      <CnSection title="展示预览" description="根据当前表单内容生成卡组在列表中的展示摘要。" divided>
        <div class="deck-preview-panel">
          <div class="preview-cover">
            <img v-if="form.coverImage" :src="form.coverImage" alt="卡组封面预览" @error="handleImageError" />
            <div v-else class="preview-cover-fallback">
              <el-icon><Collection /></el-icon>
            </div>
          </div>

          <div class="preview-copy">
            <h2>{{ form.name || '未命名卡组' }}</h2>
            <p>{{ form.description || '添加一句清晰描述，帮助自己和其他学习者快速理解这个卡组的用途。' }}</p>
          </div>

          <div v-if="tagList.length" class="preview-tags">
            <CnStatusTag v-for="tag in tagList" :key="tag" type="neutral" size="sm" subtle>
              {{ tag }}
            </CnStatusTag>
          </div>
          <CnEmptyState
            v-else
            title="暂无标签"
            description="建议用标签标记技术方向、难度或使用场景。"
            icon="TAG"
            size="sm"
            surface="transparent"
          />

          <div class="preview-foot">
            <CnStatusTag :type="form.isPublic ? 'success' : 'info'" size="sm">
              {{ form.isPublic ? '公开可搜索' : '仅自己可见' }}
            </CnStatusTag>
            <span>{{ form.name.length }}/100 字</span>
          </div>
        </div>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'
import { ArrowLeft, Collection, Delete, Loading, Plus } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import { flashcardApi } from '@/api/flashcard'
import { uploadSingle } from '@/api/upload'

interface DeckForm {
  name: string
  description: string
  coverImage: string
  tags: string
  isPublic: boolean
}

interface DeckDetail {
  name?: string
  description?: string
  coverImage?: string
  tags?: string
  isPublic?: boolean | number
}

type UploadResponse = string | {
  data?: {
    accessUrl?: string
  }
  accessUrl?: string
  url?: string
}

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const coverUploading = ref(false)

const isEdit = computed(() => Boolean(route.params.id))
const deckId = computed(() => route.params.id as string)

const form = reactive<DeckForm>({
  name: '',
  description: '',
  coverImage: '',
  tags: '',
  isPublic: false
})

const rules: FormRules<DeckForm> = {
  name: [
    { required: true, message: '请输入卡组名称', trigger: 'blur' },
    { max: 100, message: '名称不能超过100个字符', trigger: 'blur' }
  ],
  description: [
    { max: 500, message: '描述不能超过500个字符', trigger: 'blur' }
  ]
}

const tagList = computed(() => {
  return form.tags
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
})

const loadDeckDetail = async () => {
  if (!isEdit.value) return

  loading.value = true
  try {
    const data = (await flashcardApi.getDeckById(deckId.value)) as DeckDetail
    Object.assign(form, {
      name: data.name || '',
      description: data.description || '',
      coverImage: data.coverImage || '',
      tags: data.tags || '',
      isPublic: Boolean(data.isPublic)
    })
  } catch (error) {
    console.error('加载卡组失败:', error)
    ElMessage.error('加载卡组失败')
    router.push('/flashcard/my')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await flashcardApi.updateDeck({
        id: Number(deckId.value),
        ...form
      })
      ElMessage.success('卡组更新成功')
      router.push(`/flashcard/deck/${deckId.value}`)
      return
    }

    const newDeckId = await flashcardApi.createDeck({ ...form })
    ElMessage.success('卡组创建成功')
    router.push(`/flashcard/deck/${newDeckId}`)
  } catch (error) {
    const message = error instanceof Error ? error.message : '保存失败'
    console.error('保存失败:', error)
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  if (isEdit.value) {
    router.push(`/flashcard/deck/${deckId.value}`)
    return
  }
  router.push('/flashcard/my')
}

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement | null
  if (target) {
    target.style.display = 'none'
  }
}

const beforeCoverUpload = (rawFile: UploadRawFile) => {
  const isValidType = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'].includes(rawFile.type)
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isValidType) {
    ElMessage.error('封面只能是 JPG、PNG、GIF 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('封面大小不能超过 2MB!')
    return false
  }
  return true
}

const resolveUploadUrl = (response: UploadResponse) => {
  if (typeof response === 'string') return response
  return response.data?.accessUrl || response.accessUrl || response.url || ''
}

const handleCoverChange = async (uploadFile: UploadFile) => {
  if (!uploadFile.raw) return
  if (!beforeCoverUpload(uploadFile.raw)) return

  try {
    coverUploading.value = true
    const response = (await uploadSingle(uploadFile.raw, 'flashcard', 'cover')) as UploadResponse
    const url = resolveUploadUrl(response)
    if (!url) {
      ElMessage.error('封面上传失败，请重试')
      return
    }
    form.coverImage = url
    ElMessage.success('封面上传成功')
  } catch (error) {
    console.error('封面上传失败:', error)
    ElMessage.error('封面上传失败，请重试')
  } finally {
    coverUploading.value = false
  }
}

onMounted(() => {
  loadDeckDetail()
})
</script>

<style scoped>
.deck-editor-page {
  min-height: calc(100vh - 68px);
}

.editor-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: var(--cn-space-5);
  align-items: start;
}

.deck-form {
  max-width: 680px;
}

.cover-upload {
  width: 100%;
}

.cover-preview,
.cover-uploader :deep(.el-upload),
.preview-cover {
  width: min(100%, 320px);
  aspect-ratio: 16 / 10;
  overflow: hidden;
  border-radius: var(--cn-radius-panel);
}

.cover-preview {
  position: relative;
  border: 1px solid var(--cn-color-border);
  background: var(--cn-color-bg-surface-muted);
}

.cover-preview img,
.preview-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-actions {
  position: absolute;
  right: var(--cn-space-3);
  bottom: var(--cn-space-3);
  opacity: 0;
  transition: opacity var(--cn-motion-fast) var(--cn-ease-out);
}

.cover-preview:hover .cover-actions {
  opacity: 1;
}

.cover-uploader :deep(.el-upload) {
  border: 1px dashed var(--cn-color-border);
  background: var(--cn-color-bg-surface-muted);
  cursor: pointer;
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out);
}

.cover-uploader :deep(.el-upload:hover) {
  border-color: var(--cn-color-brand-primary);
  background: var(--cn-color-brand-soft);
}

.upload-placeholder {
  display: grid;
  place-items: center;
  align-content: center;
  gap: var(--cn-space-2);
  width: 100%;
  height: 100%;
  color: var(--cn-color-text-secondary);
  text-align: center;
}

.upload-icon {
  color: var(--cn-color-brand-primary);
  font-size: 32px;
}

.upload-icon.is-loading {
  animation: spin 1s linear infinite;
}

.upload-text {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 700;
}

.upload-hint,
.hint {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.visibility-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-3);
}

.form-actions {
  margin-top: var(--cn-space-6);
  margin-bottom: 0;
}

.form-actions :deep(.el-form-item__content) {
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

.deck-preview-panel {
  display: grid;
  gap: var(--cn-space-4);
}

.preview-cover {
  width: 100%;
  border: 1px solid var(--cn-color-border);
  background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 82%, var(--cn-color-brand-soft));
}

.preview-cover-fallback {
  display: grid;
  place-items: center;
  height: 100%;
  color: var(--cn-color-brand-primary);
  font-size: 44px;
}

.preview-copy {
  display: grid;
  gap: var(--cn-space-2);
}

.preview-copy h2 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 20px;
  font-weight: 780;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.preview-copy p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.preview-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding-top: var(--cn-space-3);
  border-top: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 980px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }

  .deck-form {
    max-width: none;
  }
}

@media (max-width: 560px) {
  .form-actions :deep(.el-button),
  .visibility-row :deep(.el-switch) {
    width: 100%;
  }
}
</style>
