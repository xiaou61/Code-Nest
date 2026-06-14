<template>
  <CnPage class="codepen-square" max-width="1320px" full-height>
    <CnPageHeader
      title="代码广场"
      description="浏览、搜索和复用社区里的前端代码作品，把可运行示例沉淀成自己的技术资产。"
      eyebrow="CODEPEN"
      :breadcrumbs="[{ label: '首页', to: '/' }, { label: '代码广场' }]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ total }} 个作品</CnStatusTag>
        <CnStatusTag v-if="selectedTag" type="info" size="sm" subtle>{{ selectedTag }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" icon="Plus" @click="createNewPen">创建作品</el-button>
      </template>
    </CnPageHeader>

    <section class="codepen-stats" aria-label="代码广场概览">
      <CnStatCard
        title="当前结果"
        :value="total"
        unit="个"
        description="匹配当前搜索和筛选条件"
        tone="brand"
        trend="flat"
        trend-text="广场"
      />
      <CnStatCard
        title="编辑推荐"
        :value="recommendPens.length"
        unit="个"
        description="首页推荐作品数量"
        tone="success"
        trend="flat"
        trend-text="推荐"
      />
      <CnStatCard
        title="热门标签"
        :value="hotTags.length"
        unit="个"
        description="可快速切换的标签"
        tone="info"
        trend="flat"
        trend-text="标签"
      />
      <CnStatCard
        title="筛选类型"
        :value="filterTypeLabel"
        description="当前作品费用筛选"
        tone="neutral"
        trend="flat"
        trend-text="筛选"
      />
    </section>

    <CnSection title="搜索与筛选" description="按标题、描述、分类、费用类型或热门标签过滤作品。" divided>
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索作品标题或描述..."
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button icon="Search" @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <div class="filter-bar">
        <div class="filter-item">
          <span class="filter-label">分类：</span>
          <el-radio-group v-model="filterCategory" @change="handleFilter">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="动画">动画特效</el-radio-button>
            <el-radio-button label="组件">组件库</el-radio-button>
            <el-radio-button label="游戏">游戏</el-radio-button>
            <el-radio-button label="工具">工具</el-radio-button>
          </el-radio-group>
        </div>

        <div class="filter-item">
          <span class="filter-label">类型：</span>
          <el-radio-group v-model="filterType" @change="handleFilter">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="1">免费</el-radio-button>
            <el-radio-button :label="0">付费</el-radio-button>
          </el-radio-group>
        </div>

        <div class="filter-item">
          <span class="filter-label">排序：</span>
          <el-select v-model="sortBy" class="sort-select" @change="handleFilter">
            <el-option label="最新发布" value="latest" />
            <el-option label="最热门" value="hot" />
            <el-option label="最多点赞" value="most_liked" />
            <el-option label="最多浏览" value="most_viewed" />
          </el-select>
        </div>
      </div>

      <div class="hot-tags" v-if="hotTags.length > 0">
        <span class="tags-label">热门标签：</span>
        <CnStatusTag
          v-for="tag in hotTags"
          :key="tag.tagName"
          class="tag-option"
          :type="selectedTag === tag.tagName ? 'brand' : 'info'"
          size="sm"
          :subtle="selectedTag !== tag.tagName"
          @click="selectTag(tag.tagName)"
        >
          {{ tag.tagName }} ({{ tag.useCount }})
        </CnStatusTag>
      </div>
    </CnSection>

    <CnSection
      v-if="recommendPens.length > 0 && !searchKeyword"
      class="recommend-section"
      title="编辑推荐"
      description="近期更适合作为灵感入口的作品。"
      divided
    >
      <template #actions>
        <CnStatusTag type="success" size="sm">{{ recommendPens.length }} 个推荐</CnStatusTag>
      </template>
      <div class="pen-grid">
        <PenCard
          v-for="pen in recommendPens"
          :key="pen.id"
          :pen="pen"
          @click="viewPen(pen.id)"
        />
      </div>
    </CnSection>

    <CnSection
      class="pen-list-section"
      :title="listTitle"
      :description="selectedTag ? `当前标签：${selectedTag}` : '按当前条件展示公开作品。'"
      divided
    >
      <div class="pen-grid" v-loading="loading">
        <PenCard
          v-for="pen in penList"
          :key="pen.id"
          :pen="pen"
          @click="viewPen(pen.id)"
        />
      </div>

      <CnEmptyState
        v-if="!loading && penList.length === 0"
        title="暂无作品"
        description="换一个关键词或筛选条件再试。"
        icon="CP"
        size="sm"
        surface="plain"
      />

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[12, 24, 36, 48]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadPenList"
          @size-change="loadPenList"
        />
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { codepenApi } from '@/api/codepen'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import PenCard from './components/PenCard.vue'

const router = useRouter()

// 搜索和筛选
const searchKeyword = ref('')
const filterCategory = ref('')
const filterType = ref(null)
const sortBy = ref('latest')
const selectedTag = ref('')
const hotTags = ref([])

// 列表数据
const loading = ref(false)
const penList = ref([])
const recommendPens = ref([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

const filterTypeLabel = computed(() => {
  if (filterType.value === 1) {
    return '免费'
  }
  if (filterType.value === 0) {
    return '付费'
  }
  return '全部'
})

const listTitle = computed(() => {
  if (searchKeyword.value) {
    return `搜索结果（${total.value}）`
  }
  if (filterCategory.value) {
    return `${filterCategory.value}作品`
  }
  return '全部作品'
})

// 创建新作品
const createNewPen = () => {
  router.push('/codepen/editor')
}

// 查看作品
const viewPen = (id) => {
  router.push(`/codepen/${id}`)
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  loadPenList()
}

// 筛选
const handleFilter = () => {
  pageNum.value = 1
  selectedTag.value = ''
  loadPenList()
}

// 选择标签
const selectTag = (tagName) => {
  if (selectedTag.value === tagName) {
    selectedTag.value = ''
  } else {
    selectedTag.value = tagName
    searchKeyword.value = ''
    filterCategory.value = ''
  }
  pageNum.value = 1
  loadPenList()
}

// 加载作品列表
const loadPenList = async () => {
  try {
    loading.value = true
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sortBy: sortBy.value
    }

    // 搜索关键词
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }

    // 分类筛选
    if (filterCategory.value) {
      params.category = filterCategory.value
    }

    // 类型筛选
    if (filterType.value !== null) {
      params.isFree = filterType.value
    }

    // 标签筛选
    if (selectedTag.value) {
      params.tags = [selectedTag.value]
    }

    const result = await codepenApi.getPenList(params)
    penList.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    console.error('加载作品列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载推荐作品
const loadRecommendPens = async () => {
  try {
    recommendPens.value = await codepenApi.getRecommendPens()
  } catch (error) {
    console.error('加载推荐作品失败:', error)
  }
}

// 加载热门标签
const loadHotTags = async () => {
  try {
    hotTags.value = await codepenApi.getHotTags()
  } catch (error) {
    console.error('加载热门标签失败:', error)
  }
}

// 初始化
onMounted(() => {
  loadPenList()
  loadRecommendPens()
  loadHotTags()
})
</script>

<style scoped lang="scss">
.codepen-square {
  min-height: calc(100vh - 68px);
}

.codepen-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
  margin-bottom: var(--cn-space-4);
}

.search-bar {
  max-width: 680px;
  margin-bottom: var(--cn-space-4);
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-4);
  margin-bottom: var(--cn-space-4);
}

.filter-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
}

.filter-label,
.tags-label {
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  white-space: nowrap;
}

.sort-select {
  width: 150px;
}

.hot-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  padding-top: var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.tag-option {
  cursor: pointer;
}

.recommend-section,
.pen-list-section {
  margin-top: var(--cn-space-4);
}

.pen-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--cn-space-4);
  min-height: 180px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--cn-space-5);
  overflow-x: auto;
}

@media (max-width: 992px) {
  .codepen-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .codepen-stats,
  .pen-grid {
    grid-template-columns: 1fr;
  }

  .filter-item {
    display: grid;
    width: 100%;
  }

  .sort-select {
    width: 100%;
  }

  .pagination {
    justify-content: flex-start;
  }
}
</style>

