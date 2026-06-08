<template>
  <CnPage class="codepen-detail" max-width="1240px" full-height v-loading="loading">
    <template v-if="penData">
      <CnPageHeader
        :title="penData.title"
        :description="penData.description || '查看作品预览、源码和社区互动。'"
        eyebrow="CODEPEN DETAIL"
        :breadcrumbs="[{ label: '首页', to: '/' }, { label: '代码广场', to: '/codepen' }, { label: penData.title }]"
      >
        <template #meta>
          <CnStatusTag :type="penData.isFree ? 'info' : 'warning'" size="sm" dot>
            {{ penData.isFree ? '免费作品' : `付费作品 ${penData.forkPrice || 0} 积分` }}
          </CnStatusTag>
          <CnStatusTag :type="penData.canViewCode ? 'success' : 'neutral'" size="sm" subtle>
            {{ penData.canViewCode ? '可查看源码' : '源码受限' }}
          </CnStatusTag>
          <CnStatusTag v-if="penData.category" type="brand" size="sm" subtle>
            {{ penData.category }}
          </CnStatusTag>
        </template>

        <template #actions>
          <el-button :icon="Back" @click="goBack">返回</el-button>
          <el-button type="warning" plain @click="openTransformDialog">转为学习资产</el-button>
          <el-button v-if="penData.canEdit" type="primary" :icon="Edit" @click="editPen">
            编辑
          </el-button>
          <el-button
            v-if="!penData.canEdit"
            type="success"
            :icon="CopyDocument"
            :loading="forking"
            @click="handleFork"
          >
            Fork
          </el-button>
          <el-button
            :type="penData.isLiked ? 'danger' : 'default'"
            :icon="penData.isLiked ? StarFilled : Star"
            @click="toggleLike"
          >
            {{ penData.likeCount || 0 }}
          </el-button>
          <el-button
            :type="penData.isCollected ? 'warning' : 'default'"
            @click="toggleCollect"
          >
            <el-icon :class="{ collected: penData.isCollected }">
              <Collection />
            </el-icon>
            {{ penData.collectCount || 0 }}
          </el-button>
          <el-button :icon="Share" @click="showShareDialog = true">分享</el-button>
        </template>
      </CnPageHeader>

      <section class="detail-stats" aria-label="作品数据">
        <CnStatCard
          title="浏览"
          :value="penData.viewCount || 0"
          unit="次"
          description="当前作品访问量"
          tone="brand"
          trend="flat"
          trend-text="Views"
        />
        <CnStatCard
          title="Fork"
          :value="penData.forkCount || 0"
          unit="次"
          description="被复用和二创次数"
          tone="success"
          trend="flat"
          trend-text="Fork"
        />
        <CnStatCard
          title="点赞"
          :value="penData.likeCount || 0"
          unit="个"
          description="社区认可反馈"
          tone="danger"
          trend="flat"
          trend-text="Like"
        />
        <CnStatCard
          title="评论"
          :value="penData.commentCount || 0"
          unit="条"
          description="讨论和建议数量"
          tone="info"
          trend="flat"
          trend-text="Talk"
        />
      </section>

      <CnSection class="author-section" title="作者与标签" description="作品发布者、发布时间和检索标签。" compact divided>
        <div class="pen-meta">
          <div class="author-info">
            <el-avatar :size="44" :src="penData.userAvatar">
              {{ penData.userNickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="author-details">
              <div class="author-name">{{ penData.userNickname || '匿名用户' }}</div>
              <div class="pen-time">{{ formatTime(penData.createTime) }}</div>
            </div>
          </div>

          <div class="pen-tags" v-if="penData.tags?.length">
            <CnStatusTag
              v-for="(tag, index) in penData.tags"
              :key="`${tag}-${index}`"
              type="info"
              size="sm"
              subtle
            >
              {{ tag }}
            </CnStatusTag>
          </div>
        </div>
      </CnSection>

      <CnSection
        class="preview-section"
        title="预览效果"
        description="在受限沙箱中运行作品代码，保持与原详情页一致的预览方式。"
        divided
      >
        <template #actions>
          <el-button text :icon="FullScreen" @click="fullscreenPreview = true">全屏预览</el-button>
        </template>

        <div class="preview-container">
          <iframe
            ref="previewFrame"
            class="preview-iframe"
            sandbox="allow-scripts allow-same-origin"
          ></iframe>
        </div>
      </CnSection>

      <CnSection
        v-if="penData.canViewCode"
        class="code-section"
        title="源代码"
        description="按 HTML、CSS、JavaScript 查看当前作品源码。"
        divided
      >
        <el-tabs v-model="activeTab">
          <el-tab-pane v-if="penData.htmlCode" label="HTML" name="html">
            <pre class="code-block"><code>{{ penData.htmlCode }}</code></pre>
          </el-tab-pane>
          <el-tab-pane v-if="penData.cssCode" label="CSS" name="css">
            <pre class="code-block"><code>{{ penData.cssCode }}</code></pre>
          </el-tab-pane>
          <el-tab-pane v-if="penData.jsCode" label="JavaScript" name="js">
            <pre class="code-block"><code>{{ penData.jsCode }}</code></pre>
          </el-tab-pane>
        </el-tabs>
      </CnSection>

      <CnSection
        v-else
        class="locked-section"
        title="源码权限"
        description="该作品源码受 Fork 权限保护。"
        divided
      >
        <div class="locked-tip">
          <el-icon :size="48"><Lock /></el-icon>
          <h3>{{ penData.codeViewMessage || '付费作品，Fork后可查看源码' }}</h3>
          <p>Fork价格：{{ penData.forkPrice || 0 }} 积分</p>
          <el-button type="primary" size="large" :loading="forking" @click="handleFork">
            立即Fork
          </el-button>
        </div>
      </CnSection>

      <CnSection
        class="comment-section"
        :title="`评论（${penData.commentCount || 0}）`"
        description="留下使用反馈、改进建议或实现讨论。"
        divided
      >
        <div class="comment-input">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="4"
            placeholder="说说你的看法..."
            maxlength="500"
            show-word-limit
          />
          <el-button type="primary" :loading="commenting" @click="submitComment">
            发表评论
          </el-button>
        </div>

        <div class="comment-list" v-loading="loadingComments">
          <div
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item"
          >
            <el-avatar :size="36">
              {{ comment.userNickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="comment-content">
              <div class="comment-header">
                <span class="comment-author">{{ comment.userNickname || '匿名用户' }}</span>
                <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
              </div>
              <div class="comment-text">{{ comment.content }}</div>
            </div>
          </div>

          <CnEmptyState
            v-if="!loadingComments && comments.length === 0"
            title="暂无评论"
            description="成为第一个给这个作品留言的人。"
            icon="CM"
            size="sm"
            surface="plain"
          />
        </div>
      </CnSection>
    </template>

    <CnEmptyState
      v-else-if="!loading"
      title="作品不存在"
      description="该作品可能已被删除，或当前链接不可访问。"
      icon="404"
      surface="panel"
    />

    <!-- 全屏预览 -->
    <el-dialog
      v-model="fullscreenPreview"
      title="全屏预览"
      fullscreen
    >
      <iframe
        ref="fullscreenFrame"
        class="fullscreen-iframe"
        sandbox="allow-scripts allow-same-origin"
      ></iframe>
    </el-dialog>

    <!-- 分享对话框 -->
    <el-dialog
      v-model="showShareDialog"
      title="分享作品"
      width="500px"
    >
      <div class="share-content">
        <el-input
          :value="shareUrl"
          readonly
        >
          <template #append>
            <el-button @click="copyShareUrl">复制链接</el-button>
          </template>
        </el-input>
      </div>
    </el-dialog>

    <TransformDialog
      v-model="transformDialogVisible"
      source-type="codepen"
      :source-id="sourceId"
      :source-title="penData?.title || ''"
      :default-tags="penData?.tags || []"
      @success="handleTransformSuccess"
    />
  </CnPage>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { codepenApi } from '@/api/codepen'
import { useUserStore } from '@/stores/user'
import TransformDialog from '@/components/learning-assets/TransformDialog.vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import {
  Back, Edit, CopyDocument, Share, Star, StarFilled, Collection, Lock, FullScreen
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

interface CodePenDetail {
  id: number | string
  title: string
  description?: string
  htmlCode?: string
  cssCode?: string
  jsCode?: string
  tags?: string[]
  category?: string
  isFree?: boolean | number
  forkPrice?: number
  canEdit?: boolean
  canViewCode?: boolean
  codeViewMessage?: string
  userAvatar?: string
  userNickname?: string
  createTime?: string | number | Date
  viewCount?: number
  forkCount?: number
  commentCount?: number
  isLiked?: boolean
  likeCount?: number
  isCollected?: boolean
  collectCount?: number
}

interface CodePenComment {
  id: number | string
  userNickname?: string
  createTime?: string | number | Date
  content: string
}

interface TransformRecord {
  recordId: number | string
}

interface ForkCheckResult {
  canFork: boolean
  message?: string
  isFree?: boolean
  forkPrice?: number
  authorName?: string
}

interface ForkResult {
  forkPrice?: number
  newPenId: number | string
}

// 页面数据
const loading = ref(true)
const penData = ref<CodePenDetail | null>(null)
const activeTab = ref('html')
const previewFrame = ref<HTMLIFrameElement | null>(null)
const fullscreenFrame = ref<HTMLIFrameElement | null>(null)
const fullscreenPreview = ref(false)
const showShareDialog = ref(false)
const transformDialogVisible = ref(false)

// 评论相关
const comments = ref<CodePenComment[]>([])
const commentContent = ref('')
const commenting = ref(false)
const loadingComments = ref(false)

// Fork相关
const forking = ref(false)

const routePenId = computed(() => {
  const id = route.params.id
  return Array.isArray(id) ? id[0] : id
})

const sourceId = computed(() => penData.value?.id || Number(routePenId.value))

// 分享链接
const shareUrl = computed(() => {
  return window.location.origin + `/codepen/${routePenId.value}`
})

// 返回
const goBack = () => {
  router.back()
}

// 编辑作品
const editPen = () => {
  if (!penData.value) return
  router.push(`/codepen/editor/${penData.value.id}`)
}

// Fork作品
const handleFork = async () => {
  if (!penData.value) return

  try {
    // 先检查价格和积分
    const checkResult = await codepenApi.checkForkPrice(penData.value.id) as ForkCheckResult
    
    if (!checkResult.canFork) {
      ElMessage.warning(checkResult.message || '积分不足')
      return
    }

    // 如果是付费作品，确认支付
    if (!checkResult.isFree) {
      await ElMessageBox.confirm(
        `Fork此作品需要支付 ${checkResult.forkPrice} 积分给作者 ${checkResult.authorName}，确认继续吗？`,
        '确认Fork',
        {
          confirmButtonText: '确认支付',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    }

    forking.value = true
    const result = await codepenApi.forkPen(penData.value.id) as ForkResult

    if ((result.forkPrice || 0) > 0) {
      ElMessage.success(`Fork成功！已支付 ${result.forkPrice} 积分`)
    } else {
      ElMessage.success('Fork成功！')
    }

    // 跳转到编辑器
    router.push(`/codepen/editor/${result.newPenId}`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Fork失败:', error)
    }
  } finally {
    forking.value = false
  }
}

// 点赞/取消点赞
const toggleLike = async () => {
  if (!penData.value) return

  try {
    if (penData.value.isLiked) {
      await codepenApi.unlikePen(penData.value.id)
      penData.value.isLiked = false
      penData.value.likeCount = Math.max(0, (penData.value.likeCount || 0) - 1)
    } else {
      await codepenApi.likePen(penData.value.id)
      penData.value.isLiked = true
      penData.value.likeCount = (penData.value.likeCount || 0) + 1
    }
  } catch (error) {
    console.error('点赞操作失败:', error)
  }
}

// 收藏/取消收藏
const toggleCollect = async () => {
  if (!penData.value) return

  try {
    if (penData.value.isCollected) {
      await codepenApi.uncollectPen(penData.value.id)
      penData.value.isCollected = false
      penData.value.collectCount = Math.max(0, (penData.value.collectCount || 0) - 1)
      ElMessage.success('已取消收藏')
    } else {
      await codepenApi.collectPen(penData.value.id)
      penData.value.isCollected = true
      penData.value.collectCount = (penData.value.collectCount || 0) + 1
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
  }
}

// 复制分享链接
const copyShareUrl = () => {
  navigator.clipboard.writeText(shareUrl.value)
  ElMessage.success('链接已复制')
}

const openTransformDialog = () => {
  if (!userStore.isLogin()) {
    ElMessage.warning('请先登录后再转化学习资产')
    router.push('/login')
    return
  }
  transformDialogVisible.value = true
}

const handleTransformSuccess = (record: TransformRecord) => {
  router.push(`/learning-assets?recordId=${record.recordId}`)
}

// 发表评论
const submitComment = async () => {
  if (!penData.value) return

  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    commenting.value = true
    await codepenApi.createComment({
      penId: penData.value.id,
      content: commentContent.value
    })
    
    ElMessage.success('评论成功')
    commentContent.value = ''
    loadComments()
    penData.value.commentCount = (penData.value.commentCount || 0) + 1
  } catch (error) {
    console.error('评论失败:', error)
  } finally {
    commenting.value = false
  }
}

// 加载评论
const loadComments = async () => {
  if (!penData.value) return

  try {
    loadingComments.value = true
    comments.value = await codepenApi.getComments(penData.value.id)
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    loadingComments.value = false
  }
}

// 运行代码
const runCode = () => {
  if (!penData.value || !penData.value.canViewCode) return

  const html = penData.value.htmlCode || ''
  const css = penData.value.cssCode || ''
  const js = penData.value.jsCode || ''
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

  if (fullscreenPreview.value && fullscreenFrame.value) {
    fullscreenFrame.value.srcdoc = content
  }
}

// 格式化时间
const formatTime = (time?: string | number | Date) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return Math.floor(diff / 60000) + '分钟前'
  } else if (diff < 86400000) {
    return Math.floor(diff / 3600000) + '小时前'
  } else if (diff < 604800000) {
    return Math.floor(diff / 86400000) + '天前'
  } else {
    return date.toLocaleDateString()
  }
}

// 加载作品数据
const loadPenData = async () => {
  try {
    loading.value = true
    const id = routePenId.value
    penData.value = await codepenApi.getPenDetail(id) as CodePenDetail
    
    // 增加浏览数
    codepenApi.incrementView(id)

    // 如果可以查看代码，运行预览
    if (penData.value.canViewCode) {
      runCode()
      // 设置默认激活的tab
      if (penData.value.htmlCode) {
        activeTab.value = 'html'
      } else if (penData.value.cssCode) {
        activeTab.value = 'css'
      } else if (penData.value.jsCode) {
        activeTab.value = 'js'
      }
    }

    // 加载评论
    loadComments()
  } catch (error) {
    console.error('加载作品失败:', error)
    ElMessage.error('加载作品失败')
  } finally {
    loading.value = false
  }
}

// 监听全屏预览打开
watch(fullscreenPreview, (val) => {
  if (val) {
    setTimeout(() => {
      runCode()
    }, 100)
  }
})

// 初始化
onMounted(() => {
  loadPenData()
})
</script>

<style scoped lang="scss">
.codepen-detail {
  min-height: calc(100vh - 68px);

  .pen-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: var(--cn-space-4);

    .author-info {
      display: flex;
      align-items: center;
      gap: var(--cn-space-3);

      .author-details {
        .author-name {
          font-size: 16px;
          font-weight: 650;
          color: var(--cn-color-text-primary);
        }

        .pen-time {
          font-size: 13px;
          color: var(--cn-color-text-tertiary);
          margin-top: var(--cn-space-1);
        }
      }
    }

    .pen-tags {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: var(--cn-space-2);
    }
  }

  .fullscreen-iframe {
    width: 100%;
    height: calc(100vh - 120px);
    border: none;
  }

  .share-content {
    padding: var(--cn-space-5) 0;
  }

  .collected {
    color: var(--cn-color-warning) !important;
    transform: scale(1.1);
  }
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.preview-container {
  width: 100%;
  height: min(560px, 64vh);
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: var(--cn-color-bg-surface);
}

.code-block {
  max-height: 520px;
  margin: 0;
  overflow: auto;
  padding: var(--cn-space-4);
  border: 1px solid color-mix(in srgb, var(--cn-slate-700) 72%, transparent);
  border-radius: var(--cn-radius-card);
  background: color-mix(in srgb, var(--cn-slate-900) 96%, var(--cn-color-bg-surface));
  color: var(--cn-slate-100);
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.65;
}

.locked-tip {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-3);
  padding: var(--cn-space-10) var(--cn-space-5);
  text-align: center;

  .el-icon {
    color: var(--cn-color-text-tertiary);
  }

  h3 {
    margin: 0;
    color: var(--cn-color-text-primary);
    font-size: 20px;
    font-weight: 650;
  }

  p {
    margin: 0 0 var(--cn-space-2);
    color: var(--cn-color-text-secondary);
    font-size: 15px;
  }
}

.comment-input {
  display: grid;
  gap: var(--cn-space-3);
  justify-items: start;
  margin-bottom: var(--cn-space-6);
}

.comment-list {
  min-height: 120px;
}

.comment-item {
  display: flex;
  gap: var(--cn-space-3);
  padding: var(--cn-space-4) 0;
  border-bottom: 1px solid var(--cn-color-border-subtle);

  &:last-child {
    border-bottom: none;
  }
}

.comment-content {
  min-width: 0;
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-2);
}

.comment-author {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 650;
}

.comment-time {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  white-space: nowrap;
}

.comment-text {
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

@media (max-width: 992px) {
  .detail-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .codepen-detail .pen-meta {
    align-items: flex-start;
    flex-direction: column;

    .pen-tags {
      justify-content: flex-start;
    }
  }
}

@media (max-width: 640px) {
  .detail-stats {
    grid-template-columns: 1fr;
  }

  .preview-container {
    height: 420px;
  }

  .comment-header {
    display: grid;
  }
}
</style>

