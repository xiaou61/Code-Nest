<template>
  <CnPage class="design-system-demo" surface="transparent" max-width="1180px">
    <CnPageHeader
      title="Design System 组件验收"
      description="展示页面骨架、状态组件与 Phase 4 数据组件。该页面只验证组件状态，不绑定业务接口。"
      eyebrow="Phase 4"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">TypeScript SFC</CnStatusTag>
        <CnStatusTag type="success">Token 驱动</CnStatusTag>
        <CnStatusTag type="neutral">无业务依赖</CnStatusTag>
      </template>

      <template #actions>
        <CnThemeSwitch show-label />
        <el-button type="primary" @click="themeDrawerVisible = true">
          <el-icon><Setting /></el-icon>
          主题面板
        </el-button>
      </template>
    </CnPageHeader>

    <CnThemeDrawer v-model="themeDrawerVisible" show-advanced-themes />

    <CnSection title="导航组件" description="CnSidebar 承载后台侧栏，CnTopNav 承载产品端顶部导航与移动抽屉。" divided>
      <div class="demo-top-nav-preview">
        <CnTopNav
          v-model:mobile-open="demoTopNavMobileOpen"
          :primary-items="demoTopNavItems"
          :dropdowns="demoTopNavDropdowns"
          :mobile-sections="demoTopNavMobileSections"
          active-path="/"
          active-full-path="/"
          :user="demoTopNavUser"
          :user-actions="demoTopNavUserActions"
          :unread-count="3"
          :fixed="false"
          :show-theme-switch="false"
          @brand-click="handleDemoNavAction"
          @search="handleDemoNavAction"
          @notification="handleDemoNavAction"
          @navigate="handleDemoNavAction"
          @user-action="handleDemoNavAction"
        />
      </div>

      <div class="demo-sidebar-preview">
        <CnSidebar
          :items="demoSidebarItems"
          active-path="/"
          :searchable="false"
          brand="Code-Nest"
        />

        <div class="demo-sidebar-notes">
          <CnStatusTag type="brand">递归菜单</CnStatusTag>
          <CnStatusTag type="success">Element Plus Router</CnStatusTag>
          <CnStatusTag type="neutral">Token 样式</CnStatusTag>
        </div>
      </div>
    </CnSection>

    <CnSection title="页面骨架" description="CnPage、CnPageHeader、CnSection 负责统一页面层级、间距和操作区。" divided>
      <div class="demo-layout-grid">
        <CnSection title="紧凑分区" description="适合筛选栏、工具条和低高度配置区域。" compact surface="plain">
          <div class="demo-inline">
            <CnStatusTag type="info">compact</CnStatusTag>
            <CnStatusTag type="brand" subtle>plain surface</CnStatusTag>
          </div>
        </CnSection>

        <CnSection title="透明分区" description="用于不需要额外边框的内容组合。" surface="transparent">
          <div class="demo-inline">
            <CnStatusTag type="success">transparent</CnStatusTag>
            <CnStatusTag type="neutral">responsive</CnStatusTag>
          </div>
        </CnSection>
      </div>
    </CnSection>

    <CnSection title="统计卡" description="覆盖指标、趋势、语义色和 loading 状态。" divided>
      <div class="demo-stat-grid">
        <CnStatCard
          v-for="item in stats"
          :key="item.title"
          :title="item.title"
          :value="item.value"
          :unit="item.unit"
          :description="item.description"
          :trend="item.trend"
          :trend-text="item.trendText"
          :tone="item.tone"
          :loading="item.loading"
        />
      </div>
    </CnSection>

    <CnSection title="状态标签" description="标签用文本和圆点共同表达状态，避免只依赖颜色。" divided>
      <div class="demo-tag-grid">
        <CnStatusTag v-for="item in statuses" :key="item.type" :type="item.type">
          {{ item.label }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="lg">大尺寸标签</CnStatusTag>
        <CnStatusTag type="neutral" size="sm" :dot="false">无圆点</CnStatusTag>
        <CnStatusTag type="warning" subtle>弱背景</CnStatusTag>
      </div>
    </CnSection>

    <CnSection title="数据组件" description="CnToolbar、CnFilterForm、CnDataTable 组合成标准列表页结构。" divided>
      <div class="demo-data-stack">
        <CnFilterForm
          v-model="demoFilters"
          :fields="demoFilterFields"
          :columns="3"
          @search="handleDemoSearch"
          @reset="handleDemoReset"
        />

        <CnDataTable
          :columns="demoColumns"
          :data="demoRows"
          :pagination="demoPagination"
          row-key="id"
          @page-change="handleDemoPageChange"
          @page-size-change="handleDemoPageSizeChange"
        >
          <template #toolbar>
            <CnToolbar title="审核列表" description="工具栏承载批量动作、状态摘要和辅助操作。" align="center">
              <template #meta>
                <CnStatusTag type="brand" size="sm">3 条记录</CnStatusTag>
                <CnStatusTag type="warning" size="sm">1 条待处理</CnStatusTag>
              </template>

              <el-button size="small">导出</el-button>
              <el-button type="primary" size="small">新增</el-button>
            </CnToolbar>
          </template>

          <template #status="{ row }">
            <CnStatusTag :type="getDemoStatusTone(row.status)" size="sm">
              {{ row.status }}
            </CnStatusTag>
          </template>

          <template #owner="{ row }">
            <div class="demo-owner-cell">
              <strong>{{ row.owner }}</strong>
              <span>{{ row.email }}</span>
            </div>
          </template>

          <template #actions>
            <div class="demo-table-actions">
              <el-button size="small" type="primary">查看</el-button>
              <el-button size="small">分配</el-button>
            </div>
          </template>
        </CnDataTable>
      </div>
    </CnSection>

    <CnSection title="空状态" description="列表、表格、审核流和搜索结果页都可以复用。" divided>
      <div class="demo-empty-grid">
        <CnEmptyState
          title="暂无待处理数据"
          description="当前筛选条件下没有需要处理的记录，可以调整筛选项或稍后刷新。"
          icon="DS"
        >
          <template #actions>
            <el-button type="primary">刷新数据</el-button>
            <el-button>重置筛选</el-button>
          </template>
        </CnEmptyState>

        <CnEmptyState
          title="轻量空状态"
          description="用于面板内部的小范围空内容。"
          icon="CN"
          size="sm"
          surface="transparent"
        />
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Document, Odometer, Setting, User } from '@element-plus/icons-vue'
import {
  CnDataTable,
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnSidebar,
  CnStatCard,
  CnStatusTag,
  CnThemeDrawer,
  CnTopNav,
  CnToolbar,
  CnThemeSwitch
} from '@/design-system'
import type {
  CnBreadcrumbItem,
  CnFilterField,
  CnPagination,
  CnSidebarItem,
  CnTableColumn,
  CnTone,
  CnTopNavDropdown,
  CnTopNavItem,
  CnTopNavMobileSection,
  CnTopNavUserAction,
  CnTrend
} from '@/design-system'

interface DemoStat {
  title: string
  value: string | number
  unit?: string
  description: string
  trend: CnTrend
  trendText: string
  tone: CnTone
  loading?: boolean
}

const breadcrumbs: CnBreadcrumbItem[] = [
  { label: 'Code Nest' },
  { label: 'Design System' },
  { label: 'Components' }
]

const themeDrawerVisible = ref(false)

const demoSidebarItems: CnSidebarItem[] = [
  { label: '首页', path: '/', icon: Odometer },
  { label: '个人中心', path: '/profile', icon: User },
  {
    label: '学习内容',
    index: '/learning',
    icon: Document,
    children: [
      { label: '学习资产', path: '/learning-assets', icon: Document },
      { label: '面试题库', path: '/interview', icon: Document }
    ]
  }
]

const demoTopNavMobileOpen = ref(false)

const demoTopNavItems: CnTopNavItem[] = [
  { label: '首页', path: '/', icon: Odometer },
  { label: '社区', path: '/community', icon: User, matchPrefixes: ['/community'] }
]

const demoTopNavDropdowns: CnTopNavDropdown[] = [
  {
    key: 'learning',
    label: '学习',
    icon: Document,
    groups: [
      {
        title: '成长',
        items: [
          { label: '学习资产', path: '/learning-assets', desc: '沉淀题单、笔记与目标', icon: Document },
          { label: '面试题库', path: '/interview', desc: '按题单推进面试复习', icon: Document }
        ]
      }
    ]
  }
]

const demoTopNavMobileSections: CnTopNavMobileSection[] = [
  {
    key: 'content',
    title: '内容',
    items: demoTopNavDropdowns.flatMap((dropdown) =>
      dropdown.groups ? dropdown.groups.flatMap((group) => group.items) : dropdown.items || []
    )
  }
]

const demoTopNavUser = {
  username: 'Code Nest'
}

const demoTopNavUserActions: CnTopNavUserAction[] = [
  { command: 'profile', label: '个人中心', icon: User },
  { command: 'logout', label: '退出登录', icon: User, divided: true }
]

const handleDemoNavAction = () => {}

const stats: DemoStat[] = [
  {
    title: '活跃用户',
    value: '12,486',
    unit: '人',
    description: '较昨日保持增长',
    trend: 'up',
    trendText: '+8.2%',
    tone: 'brand'
  },
  {
    title: '审核通过率',
    value: '96.4',
    unit: '%',
    description: '近 7 日平均值',
    trend: 'flat',
    trendText: '稳定',
    tone: 'success'
  },
  {
    title: '风险事件',
    value: 18,
    unit: '条',
    description: '需在今日闭环',
    trend: 'down',
    trendText: '-12%',
    tone: 'danger'
  },
  {
    title: '数据加载',
    value: '--',
    description: '骨架屏状态',
    trend: 'flat',
    trendText: '',
    tone: 'neutral',
    loading: true
  }
]

const statuses: Array<{ type: CnTone; label: string }> = [
  { type: 'brand', label: '进行中' },
  { type: 'success', label: '已完成' },
  { type: 'warning', label: '待确认' },
  { type: 'danger', label: '异常' },
  { type: 'info', label: '信息' },
  { type: 'neutral', label: '草稿' }
]

interface DemoRow {
  id: number
  title: string
  owner: string
  email: string
  status: '已通过' | '待处理' | '已退回'
  updatedAt: string
}

const demoFilters = ref<Record<string, unknown>>({
  keyword: '',
  type: '',
  status: ''
})

const demoFilterFields: CnFilterField[] = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '搜索标题或负责人' },
  {
    prop: 'type',
    label: '类型',
    type: 'select',
    options: [
      { label: '文章', value: 'article' },
      { label: '题目', value: 'problem' },
      { label: '评论', value: 'comment' }
    ]
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '已通过', value: 'approved' },
      { label: '待处理', value: 'pending' },
      { label: '已退回', value: 'rejected' }
    ]
  }
]

const demoRows: DemoRow[] = [
  {
    id: 1001,
    title: '动态规划专题题单',
    owner: '林北辰',
    email: 'lin@example.com',
    status: '已通过',
    updatedAt: '2026-05-28 10:24'
  },
  {
    id: 1002,
    title: '面试复盘内容审核',
    owner: '许知夏',
    email: 'xu@example.com',
    status: '待处理',
    updatedAt: '2026-05-29 16:10'
  },
  {
    id: 1003,
    title: '低质量评论拦截',
    owner: '周野',
    email: 'zhou@example.com',
    status: '已退回',
    updatedAt: '2026-05-30 09:18'
  }
]

const demoColumns: CnTableColumn<DemoRow>[] = [
  { type: 'selection', width: 48 },
  { prop: 'id', label: 'ID', width: 92 },
  { prop: 'title', label: '内容标题', minWidth: 180, showOverflowTooltip: true },
  { prop: 'owner', label: '负责人', minWidth: 170, slot: 'owner' },
  { prop: 'status', label: '状态', width: 110, slot: 'status' },
  { prop: 'updatedAt', label: '更新时间', width: 170 },
  { label: '操作', width: 150, fixed: 'right', slot: 'actions' }
]

const demoPagination = ref<CnPagination>({
  page: 1,
  pageSize: 10,
  total: 36,
  background: true
})

const handleDemoSearch = () => {}

const handleDemoReset = () => {
  demoFilters.value = {
    keyword: '',
    type: '',
    status: ''
  }
}

const handleDemoPageChange = (page: number) => {
  demoPagination.value.page = page
}

const handleDemoPageSizeChange = (size: number) => {
  demoPagination.value.pageSize = size
  demoPagination.value.page = 1
}

const getDemoStatusTone = (status: DemoRow['status']): CnTone => {
  const toneMap: Record<DemoRow['status'], CnTone> = {
    已通过: 'success',
    待处理: 'warning',
    已退回: 'danger'
  }
  return toneMap[status]
}
</script>

<style scoped>
.design-system-demo {
  min-height: 100%;
}

.demo-layout-grid,
.demo-empty-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.demo-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.demo-tag-grid,
.demo-inline {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.demo-data-stack {
  display: grid;
  gap: var(--cn-space-4);
}

.demo-top-nav-preview {
  overflow: hidden;
  margin-bottom: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
}

.demo-sidebar-preview {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: var(--cn-space-4);
  align-items: start;
}

.demo-sidebar-preview :deep(.cn-sidebar) {
  height: 360px;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
}

.demo-sidebar-notes {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  align-content: flex-start;
}

.demo-owner-cell {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.demo-owner-cell strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
  font-weight: 650;
}

.demo-owner-cell span {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.demo-table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.demo-table-actions .el-button {
  margin-left: 0;
}

@media (max-width: 1024px) {
  .demo-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .demo-layout-grid,
  .demo-empty-grid,
  .demo-stat-grid,
  .demo-sidebar-preview {
    grid-template-columns: 1fr;
  }
}
</style>
