<template>
  <div class="codepen-detail" v-loading="loading">
    <div class="detail-container" v-if="penData">
      <!-- 顶部操作栏 -->
      <div class="detail-header">
        <div class="header-left">
          <el-button icon="Back" @click="goBack">返回</el-button>
          <h1 class="pen-title">{{ penData.title }}</h1>
          
          <!-- 付费标识 -->
          <el-tag v-if="!penData.isFree" type="warning" size="large">
            <el-icon><Lock /></el-icon>
            付费作品 {{ penData.forkPrice }} 积分
          </el-tag>
        </div>

        <div class="header-right">
          <el-button
            type="warning"
            plain
            @click="openTransformDialog"
          >
            转为学习资产
          </el-button>

          <!-- 编辑按钮（仅作者可见） -->
          <el-button 
            v-if="penData.canEdit"
            type="primary"
            icon="Edit"
            @click="editPen"
          >
            编辑
          </el-button>

          <!-- Fork按钮 -->
          <el-button
            v-if="!penData.canEdit"
            type="success"
            icon="CopyDocument"
            @click="handleFork"
            :loading="forking"
          >
            Fork
          </el-button>

          <!-- 互动按钮 -->
          <el-button
            :type="penData.isLiked ? 'danger' : 'default'"
            :icon="penData.isLiked ? StarFilled : Star"
            @click="toggleLike"
          >
            {{ penData.likeCount }}
          </el-button>

          <el-button
            :type="penData.isCollected ? 'warning' : 'default'"
            @click="toggleCollect"
          >
            <el-icon :class="{ 'collected': penData.isCollected }">
              <Collection />
            </el-icon>
            {{ penData.collectCount }}
          </el-button>

          <el-button icon="Share" @click="showShareDialog = true">分享</el-button>
        </div>
      </div>

      <!-- 作品信息 -->
      <div class="pen-meta">
        <div class="author-info">
          <el-avatar :size="40" :src="penData.userAvatar">
            {{ penData.userNickname?.charAt(0) }}
          </el-avatar>
          <div class="author-details">
            <div class="author-name">{{ penData.userNickname }}</div>
            <div class="pen-time">{{ formatTime(penData.createTime) }}</div>
          </div>
        </div>

        <div class="pen-stats">
          <span class="stat-item">
            <el-icon><View /></el-icon>
            {{ penData.viewCount }} 浏览
          </span>
          <span class="stat-item">
            <el-icon><CopyDocument /></el-icon>
            {{ penData.forkCount }} Fork
          </span>
          <span class="stat-item">
            <el-icon><ChatDotRound /></el-icon>
            {{ penData.commentCount }} 评论
          </span>
        </div>
      </div>

      <!-- 描述和标签 -->
      <div class="pen-info" v-if="penData.description || penData.tags?.length > 0">
        <p class="pen-description" v-if="penData.description">
          {{ penData.description }}
        </p>
        <div class="pen-tags" v-if="penData.tags?.length > 0">
          <el-tag
            v-for="(tag, index) in penData.tags"
            :key="index"
            type="info"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>

      <!-- 预览和代码 -->
      <div class="pen-content">
        <!-- 预览区 -->
        <div class="preview-section">
          <div class="section-header">
            <h2>预览效果</h2>
            <el-button
              text
              icon="FullScreen"
              @click="fullscreenPreview = true"
            >
              全屏预览
            </el-button>
          </div>
          <div class="preview-container">
            <iframe
              ref="previewFrame"
              class="preview-iframe"
              sandbox="allow-scripts allow-same-origin"
            ></iframe>
          </div>
        </div>

        <!-- 代码区 -->
        <div class="code-section" v-if="penData.canViewCode">
          <div class="section-header">
            <h2>源代码</h2>
          </div>

          <el-tabs v-model="activeTab">
            <el-tab-pane label="HTML" name="html" v-if="penData.htmlCode">
              <pre class="code-block"><code>{{ penData.htmlCode }}</code></pre>
            </el-tab-pane>
            <el-tab-pane label="CSS" name="css" v-if="penData.cssCode">
              <pre class="code-block"><code>{{ penData.cssCode }}</code></pre>
            </el-tab-pane>
            <el-tab-pane label="JavaScript" name="js" v-if="penData.jsCode">
              <pre class="code-block"><code>{{ penData.jsCode }}</code></pre>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 付费作品提示 -->
        <div class="code-section locked-section" v-else>
          <div class="locked-tip">
            <el-icon :size="48"><Lock /></el-icon>
            <h3>{{ penData.codeViewMessage || '付费作品，Fork后可查看源码' }}</h3>
            <p>Fork价格：{{ penData.forkPrice }} 积分</p>
            <el-button
              type="primary"
              size="large"
              @click="handleFork"
              :loading="forking"
            >
              立即Fork
            </el-button>
          </div>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="comment-section">
        <h2 class="section-title">评论 ({{ penData.commentCount }})</h2>
        
        <!-- 发表评论 -->
        <div class="comment-input">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="4"
            placeholder="说说你的看法..."
            maxlength="500"
            show-word-limit
          />
          <el-button
            type="primary"
            @click="submitComment"
            :loading="commenting"
            style="margin-top: 10px"
          >
            发表评论
          </el-button>
        </div>

        <!-- 评论列表 -->
        <div class="comment-list" v-loading="loadingComments">
          <div
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item"
          >
            <el-avatar :size="36">
              {{ comment.userNickname?.charAt(0) }}
            </el-avatar>
            <div class="comment-content">
              <div class="comment-header">
                <span class="comment-author">{{ comment.userNickname }}</span>
                <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
              </div>
              <div class="comment-text">{{ comment.content }}</div>
            </div>
          </div>

          <el-empty
            v-if="!loadingComments && comments.length === 0"
            description="暂无评论"
          />
        </div>
      </div>
    </div>

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
      :source-id="penData?.id || Number(route.params.id)"
      :source-title="penData?.title || ''"
      :default-tags="penData?.tags || []"
      @success="handleTransformSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { codepenApi } from '@/api/codepen'
import { useUserStore } from '@/stores/user'
import TransformDialog from '@/components/learning-assets/TransformDialog.vue'
import {
  Back, Edit, CopyDocument, Share, View, ChatDotRound,
  Star, StarFilled, Collection, Lock, FullScreen
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 页面数据
const loading = ref(true)
const penData = ref(null)
const activeTab = ref('html')
const previewFrame = ref(null)
const fullscreenFrame = ref(null)
const fullscreenPreview = ref(false)
const showShareDialog = ref(false)
const transformDialogVisible = ref(false)

// 评论相关
const comments = ref([])
const commentContent = ref('')
const commenting = ref(false)
const loadingComments = ref(false)

// Fork相关
const forking = ref(false)

// 分享链接
const shareUrl = computed(() => {
  return window.location.origin + `/codepen/${route.params.id}`
})

// 返回
const goBack = () => {
  router.back()
}

// 编辑作品
const editPen = () => {
  router.push(`/codepen/editor/${penData.value.id}`)
}

// Fork作品
const handleFork = async () => {
  try {
    // 先检查价格和积分
    const checkResult = await codepenApi.checkForkPrice(penData.value.id)
    
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
    const result = await codepenApi.forkPen(penData.value.id)

    if (result.forkPrice > 0) {
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
  try {
    if (penData.value.isLiked) {
      await codepenApi.unlikePen(penData.value.id)
      penData.value.isLiked = false
      penData.value.likeCount--
    } else {
      await codepenApi.likePen(penData.value.id)
      penData.value.isLiked = true
      penData.value.likeCount++
    }
  } catch (error) {
    console.error('点赞操作失败:', error)
  }
}

// 收藏/取消收藏
const toggleCollect = async () => {
  try {
    if (penData.value.isCollected) {
      await codepenApi.uncollectPen(penData.value.id)
      penData.value.isCollected = false
      penData.value.collectCount--
      ElMessage.success('已取消收藏')
    } else {
      await codepenApi.collectPen(penData.value.id)
      penData.value.isCollected = true
      penData.value.collectCount++
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

const handleTransformSuccess = (record) => {
  router.push(`/learning-assets?recordId=${record.recordId}`)
}

// 发表评论
const submitComment = async () => {
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
    penData.value.commentCount++
  } catch (error) {
    console.error('评论失败:', error)
  } finally {
    commenting.value = false
  }
}

// 加载评论
const loadComments = async () => {
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

  const content = `
    <!DOCTYPE html>
    <html>
      <head>
        <style>${css}</style>
      </head>
      <body>
        ${html}
        <script>${js}<\/script>
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
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

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
    const id = route.params.id
    penData.value = await codepenApi.getPenDetail(id)
    
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
  min-height: 100vh;
  background: #f5f5f5;

  .detail-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
  }

  .detail-header {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

    .header-left {
      display: flex;
      align-items: center;
      gap: 15px;
      flex: 1;

      .pen-title {
        font-size: 24px;
        font-weight: 600;
        color: #333;
        margin: 0;
      }
    }

    .header-right {
      display: flex;
      gap: 10px;
    }
  }

  .pen-meta {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

    .author-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .author-details {
        .author-name {
          font-size: 16px;
          font-weight: 600;
          color: #333;
        }

        .pen-time {
          font-size: 13px;
          color: #999;
          margin-top: 4px;
        }
      }
    }

    .pen-stats {
      display: flex;
      gap: 20px;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 14px;
        color: #666;

        .el-icon {
          font-size: 18px;
        }
      }
    }
  }

  .pen-info {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 20px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

    .pen-description {
      font-size: 15px;
      line-height: 1.6;
      color: #666;
      margin: 0 0 15px 0;
    }

    .pen-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
  }

  .pen-content {
    .preview-section {
      background: #fff;
      padding: 20px;
      border-radius: 8px;
      margin-bottom: 20px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;

        h2 {
          font-size: 20px;
          font-weight: 600;
          color: #333;
          margin: 0;
        }
      }

      .preview-container {
        width: 100%;
        height: 500px;
        border: 1px solid #e0e0e0;
        border-radius: 4px;
        overflow: hidden;

        .preview-iframe {
          width: 100%;
          height: 100%;
          border: none;
        }
      }
    }

    .code-section {
      background: #fff;
      padding: 20px;
      border-radius: 8px;
      margin-bottom: 20px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

      .section-header {
        margin-bottom: 15px;

        h2 {
          font-size: 20px;
          font-weight: 600;
          color: #333;
          margin: 0;
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

    .locked-section {
      .locked-tip {
        text-align: center;
        padding: 60px 20px;

        .el-icon {
          color: #999;
          margin-bottom: 20px;
        }

        h3 {
          font-size: 20px;
          color: #666;
          margin: 0 0 10px 0;
        }

        p {
          font-size: 16px;
          color: #999;
          margin: 0 0 30px 0;
        }
      }
    }
  }

  .comment-section {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

    .section-title {
      font-size: 20px;
      font-weight: 600;
      color: #333;
      margin: 0 0 20px 0;
    }

    .comment-input {
      margin-bottom: 30px;
    }

    .comment-list {
      .comment-item {
        display: flex;
        gap: 12px;
        padding: 15px 0;
        border-bottom: 1px solid #f0f0f0;

        &:last-child {
          border-bottom: none;
        }

        .comment-content {
          flex: 1;

          .comment-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;

            .comment-author {
              font-size: 14px;
              font-weight: 600;
              color: #333;
            }

            .comment-time {
              font-size: 12px;
              color: #999;
            }
          }

          .comment-text {
            font-size: 14px;
            line-height: 1.6;
            color: #666;
          }
        }
      }
    }
  }

  .fullscreen-iframe {
    width: 100%;
    height: calc(100vh - 120px);
    border: none;
  }

  .share-content {
    padding: 20px 0;
  }

  // 收藏图标样式
  .collected {
    color: #e6a23c !important;
    transform: scale(1.1);
  }
}
</style>

