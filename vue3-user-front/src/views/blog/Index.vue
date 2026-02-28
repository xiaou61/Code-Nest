<template>
  <div class="blog-container">
    <!-- 博客头部 -->
    <el-card v-if="blogConfig" class="blog-header">
      <div class="header-content">
        <el-avatar :size="80" :src="blogConfig.blogAvatar" />
        <div class="header-info">
          <h2>{{ blogConfig.blogName }}</h2>
          <p class="description">{{ blogConfig.blogDescription }}</p>
          <div class="stats">
            <span>文章：{{ blogConfig.totalArticles }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 未开通提示 -->
    <el-card v-else class="open-blog-card">
      <el-empty description="您还未开通博客">
        <el-button type="primary" @click="handleOpenBlog">开通博客（消耗50积分）</el-button>
      </el-empty>
    </el-card>

    <!-- 文章列表 -->
    <el-card v-if="blogConfig" class="article-list mt-3">
      <div class="list-header">
        <h3>我的文章</h3>
        <div class="header-actions">
          <el-button type="success" @click="viewMyBlogHome">查看博客主页</el-button>
          <el-button type="primary" @click="handleCreateArticle">写文章</el-button>
        </div>
      </div>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="已发布" name="published">
          <div v-if="articleList.length > 0" class="articles">
            <div v-for="article in articleList" :key="article.id" class="article-item">
              <div class="article-content">
                <h4 class="article-title" @click="viewArticle(article.id)">
                  <el-tag v-if="article.isTop === 1" type="warning" size="small">置顶</el-tag>
                  {{ article.title }}
                </h4>
                <p class="article-summary">{{ article.summary }}</p>
                <div class="article-meta">
                  <span>{{ article.categoryName }}</span>
                  <span>{{ article.publishTime }}</span>
                </div>
              </div>
              <div class="article-actions">
                <el-button link type="primary" @click="editArticle(article.id)">编辑</el-button>
                <el-button link type="danger" @click="deleteArticle(article.id)">删除</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无文章" />
        </el-tab-pane>

        <el-tab-pane label="草稿箱" name="draft">
          <div v-if="draftList.length > 0" class="articles">
            <div v-for="draft in draftList" :key="draft.id" class="article-item">
              <div class="article-content">
                <h4 class="article-title">{{ draft.title }}</h4>
                <p class="article-summary">{{ draft.summary }}</p>
                <div class="article-meta">
                  <span>{{ draft.createTime }}</span>
                </div>
              </div>
              <div class="article-actions">
                <el-button link type="primary" @click="editArticle(draft.id)">继续编辑</el-button>
                <el-button link type="danger" @click="deleteArticle(draft.id)">删除</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无草稿" />
        </el-tab-pane>
      </el-tabs>

      <div v-if="total > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="getList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  checkBlogStatus,
  getBlogConfig,
  openBlog,
  getMyArticleList,
  getMyDraftList,
  deleteArticle as deleteArticleApi
} from '@/api/blog'

const router = useRouter()
const blogConfig = ref(null)
const articleList = ref([])
const draftList = ref([])
const activeTab = ref('published')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadBlogStatus = async () => {
  try {
    const res = await checkBlogStatus()
    if (res.isOpened) {
      // 使用当前用户信息获取博客配置
      const userStore = useUserStore()
      const userId = userStore.userInfo?.id
      if (userId) {
        const configRes = await getBlogConfig(userId)
        blogConfig.value = configRes
        getList()
      }
    }
  } catch (error) {
    console.error('加载博客状态失败', error)
  }
}

const handleOpenBlog = async () => {
  try {
    await ElMessageBox.confirm('开通博客需要消耗50积分，确定要开通吗？', '提示', {
      type: 'warning'
    })
    await openBlog()
    ElMessage.success('博客开通成功')
    loadBlogStatus()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '开通失败')
    }
  }
}

const getList = async () => {
  try {
    if (activeTab.value === 'published') {
      const res = await getMyArticleList({
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
      articleList.value = res.records || []
      total.value = res.total || 0
    } else {
      const res = await getMyDraftList(pageNum.value, pageSize.value)
      draftList.value = res.records || []
      total.value = res.total || 0
    }
  } catch (error) {
    ElMessage.error(error.message || '获取列表失败')
  }
}

const handleTabChange = () => {
  pageNum.value = 1
  getList()
}

const handleCreateArticle = () => {
  router.push('/blog/editor')
}

const viewMyBlogHome = () => {
  const userStore = useUserStore()
  const userId = userStore.userInfo?.id
  if (userId) {
    router.push(`/blog/${userId}`)
  }
}

const editArticle = (id) => {
  router.push(`/blog/editor/${id}`)
}

const viewArticle = (id) => {
  const userStore = useUserStore()
  const userId = userStore.userInfo?.id
  router.push(`/blog/${userId}/article/${id}`)
}

const deleteArticle = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该文章吗？', '提示', {
      type: 'warning'
    })
    await deleteArticleApi(id)
    ElMessage.success('删除成功')
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadBlogStatus()
})
</script>

<style scoped>
.blog-container {
  max-width: 1220px;
  margin: 0 auto;
  padding: 10px 12px 20px;
  display: grid;
  gap: 14px;
}

.blog-header {
  margin-bottom: 0;
  border: 1px solid #d8e5f8;
  border-radius: 16px;
  overflow: hidden;
}

.header-content {
  display: flex;
  gap: 18px;
  align-items: center;
  padding: 4px 0;
}

.header-info {
  flex: 1;
}

.header-info h2 {
  margin: 0 0 8px;
  color: var(--cn-text-primary);
  font-size: 24px;
  font-weight: 600;
}

.description {
  color: var(--cn-text-secondary);
  margin: 8px 0;
  line-height: 1.6;
}

.stats span {
  display: inline-flex;
  align-items: center;
  margin-right: 12px;
  padding: 2px 12px;
  border-radius: 999px;
  background: #edf4ff;
  color: #1f6feb;
  font-size: 13px;
}

.open-blog-card {
  text-align: center;
  padding: 36px;
  border: 1px dashed #cddcf2;
  border-radius: 16px;
  background: linear-gradient(180deg, #fbfdff 0%, #f5f9ff 100%);
}

.mt-3 {
  margin-top: 0;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  gap: 10px;
}

.list-header h3 {
  margin: 0;
  font-size: 20px;
  color: var(--cn-text-primary);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.articles {
  min-height: 280px;
}

.article-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 18px;
  border-bottom: 1px solid #e8eef8;
  border-radius: 12px;
  transition: var(--cn-transition);
}

.article-item:last-child {
  border-bottom: none;
}

.article-item:hover {
  background: #f6faff;
}

.article-content {
  flex: 1;
  min-width: 0;
}

.article-title {
  margin: 0 0 10px;
  cursor: pointer;
  color: var(--cn-text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.45;
}

.article-title:hover {
  color: var(--cn-primary);
}

.article-summary {
  color: var(--cn-text-secondary);
  margin: 8px 0 10px;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  color: var(--cn-text-tertiary);
  font-size: 13px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.article-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}

.article-list {
  border: 1px solid #d8e5f8;
  border-radius: 16px;
}

.article-list :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.article-list :deep(.el-tabs__item) {
  height: 40px;
}

.article-list :deep(.el-button.is-link) {
  font-weight: 500;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

@media (max-width: 900px) {
  .header-content {
    align-items: flex-start;
  }

  .list-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .blog-container {
    padding: 8px 4px 16px;
    gap: 12px;
  }

  .header-content {
    flex-direction: column;
  }

  .article-item {
    flex-direction: column;
    padding: 14px;
  }

  .article-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>

