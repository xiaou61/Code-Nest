<template>
  <div class="article-detail-container">
    <div v-if="article" class="article-content">
      <el-card>
        <!-- 文章头部 -->
        <div class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <el-avatar :size="40" :src="article.userAvatar" />
            <div class="meta-info">
              <div class="author">{{ article.userNickname }}</div>
              <div class="time">
                发布于 {{ article.publishTime }}
                <span v-if="article.updateTime !== article.publishTime"> · 更新于 {{ article.updateTime }}</span>
              </div>
            </div>
          </div>
        <div class="article-tags">
          <el-tag type="primary">{{ article.categoryName }}</el-tag>
          <el-tag v-for="tag in article.tags" :key="tag" type="info">{{ tag }}</el-tag>
          <el-tag v-if="article.isOriginal === 1" type="success">原创</el-tag>
        </div>

        <div class="transform-entry">
          <el-button type="success" plain @click="openTransformDialog">
            转为学习资产
          </el-button>
        </div>
      </div>

        <!-- 文章内容 -->
        <div class="article-body">
          <div class="markdown-body" v-html="renderMarkdown(article.content)"></div>
        </div>

        <!-- 操作按钮 -->
        <div v-if="article.canEdit" class="article-actions">
          <el-button type="primary" @click="editArticle">编辑文章</el-button>
          <el-button type="danger" @click="deleteArticle">删除文章</el-button>
        </div>
      </el-card>

      <!-- 相关文章 -->
      <el-card v-if="article.relatedArticles && article.relatedArticles.length > 0" class="mt-3">
        <template #header>
          <div class="card-header">相关文章</div>
        </template>
        <div class="related-articles">
          <div
            v-for="related in article.relatedArticles"
            :key="related.id"
            class="related-item"
            @click="viewRelated(related.id)"
          >
            {{ related.title }}
          </div>
        </div>
      </el-card>
    </div>

    <el-empty v-else description="文章不存在或已删除" />

    <TransformDialog
      v-model="transformDialogVisible"
      source-type="blog"
      :source-id="article?.id || Number(route.params.articleId)"
      :source-title="article?.title || ''"
      :default-tags="article?.tags || []"
      @success="handleTransformSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getArticleDetail, deleteArticle as deleteArticleApi } from '@/api/blog'
import TransformDialog from '@/components/learning-assets/TransformDialog.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const article = ref(null)
const transformDialogVisible = ref(false)

const loadArticle = async () => {
  try {
    const articleId = route.params.articleId
    article.value = await getArticleDetail(articleId)
  } catch (error) {
    ElMessage.error(error.message || '加载文章失败')
  }
}

const renderMarkdown = (content) => {
  // 简单的Markdown渲染，实际应该使用markdown-it等库
  if (!content) return ''
  return content
    .replace(/\n/g, '<br/>')
    .replace(/#{3}\s+(.*)/g, '<h3>$1</h3>')
    .replace(/#{2}\s+(.*)/g, '<h2>$1</h2>')
    .replace(/#{1}\s+(.*)/g, '<h1>$1</h1>')
}

const editArticle = () => {
  router.push(`/blog/editor/${article.value.id}`)
}

const deleteArticle = async () => {
  try {
    await ElMessageBox.confirm('确定要删除该文章吗？', '提示', {
      type: 'warning'
    })
    await deleteArticleApi(article.value.id)
    ElMessage.success('删除成功')
    router.push('/blog')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const viewRelated = (id) => {
  router.push(`/blog/${route.params.userId}/article/${id}`)
  loadArticle()
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

onMounted(() => {
  loadArticle()
})
</script>

<style scoped>
.article-detail-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.article-header {
  padding: 30px 0;
  border-bottom: 1px solid #eee;
}

.article-title {
  font-size: 32px;
  font-weight: bold;
  margin: 0 0 20px 0;
  line-height: 1.4;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.meta-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.author {
  font-weight: bold;
  color: #333;
}

.time {
  font-size: 14px;
  color: #999;
}

.article-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.transform-entry {
  margin-top: 20px;
}

.article-body {
  padding: 30px 0;
  line-height: 1.8;
  font-size: 16px;
}

.markdown-body {
  color: #333;
}

.article-actions {
  padding-top: 20px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 10px;
}

.mt-3 {
  margin-top: 20px;
}

.card-header {
  font-weight: bold;
}

.related-articles {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.related-item {
  padding: 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.related-item:hover {
  background: #f5f7fa;
  color: #409eff;
}
</style>

