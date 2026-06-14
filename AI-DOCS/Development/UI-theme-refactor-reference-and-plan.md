# Code-Nest Design System 与主题化重构技术方案

> 状态：正式执行前技术设计稿  
> 日期：2026-05-29  
> 分支：`v2.3.0`  
> 适用范围：`vue3-admin-front`、`vue3-user-front`  
> 推荐路线：TypeScript 组件库优先，现有业务页面渐进迁移  

## 0. 结论先行

本次 UI 重构不建议做成“套模板”或“一次性重写所有页面”。推荐采用下面这条路线：

```text
保留现有业务页面和接口
  -> 新增 TypeScript Design System / UI Kit
  -> 建立主题 token 与 Element Plus 映射
  -> 先改 3-4 个试点页面
  -> 沉淀组件 API
  -> 按模块逐步迁移管理端和用户端
```

核心判断：

- TypeScript 是正确方向，尤其适合组件库、主题系统、表格/表单 schema、组合式函数。
- 不建议一开始把两套前端所有页面全部改成 TypeScript，这会把 UI 重构扩大成前端重写。
- 不建议直接 fork 开源后台模板，因为 Code-Nest 已经有很厚的业务页面、路由、接口、状态和后端约定。
- 推荐先在两端各自建立同构的 `src/design-system`，等组件稳定后再抽成真正的 workspace package。
- 组件库第一阶段不要替代 Element Plus 的基础组件，而是封装 Code-Nest 高频业务模式。

最终目标是形成 Code-Nest 自己的 UI 系统，而不是把项目塞进别人的模板。

## 1. 项目现状

### 1.1 前端项目

| 项目 | 路径 | 当前技术栈 | 入口命令 | 端口 |
| --- | --- | --- | --- | --- |
| 管理端 | `vue3-admin-front` | Vue3 + Vite + JavaScript + Element Plus + Pinia + SCSS | `npm run dev` | `3000` |
| 用户端 | `vue3-user-front` | Vue3 + Vite + JavaScript + Element Plus + Pinia + SCSS | `npm run dev2` | `3001` |

两端都已经存在：

- `tsconfig.json`
- `tsconfig.node.json`
- `vite.config.js`
- `src/styles/index.scss`
- `src/router/index.js`
- `src/stores`

这说明项目已经具备 TypeScript 增量接入条件。即使当前业务主要是 `.js` 和普通 Vue SFC，也可以从新增 `.ts`、`<script setup lang="ts">` 组件开始。

### 1.2 页面规模

当前粗略统计：

| 项目 | Vue 文件数 | views 下 Vue 文件数 | components 下 Vue 文件数 | store 文件数 |
| --- | ---: | ---: | ---: | ---: |
| 管理端 | 约 76 | 约 73 | 约 1 | 约 1 |
| 用户端 | 约 117 | 约 112 | 约 4 | 约 4 |

这个规模已经不适合靠全局 SCSS 和页面局部样式继续堆。需要建立可复用组件和主题 token。

### 1.3 已有优势

- 两端都使用 Element Plus，基础 UI 组件统一。
- 两端都已经有 `--cn-*` CSS variables。
- 用户端已有命令面板、全局导航、移动端抽屉等产品化体验基础。
- 管理端已有固定 layout、侧边栏菜单、菜单搜索等后台体验基础。
- Vite + Vue3 本身支持增量 TypeScript，不需要一次性迁移。

### 1.4 当前痛点

| 痛点 | 表现 | 影响 |
| --- | --- | --- |
| 样式分散 | 全局 SCSS、页面 scoped CSS、硬编码颜色并存 | 主题切换难、风格不一致 |
| token 未分层 | `--cn-*` 已有但职责不清 | 后续变量会越来越难维护 |
| 页面级重复 | 筛选区、表格区、统计卡、空状态反复手写 | 改版成本高 |
| 管理端组件少 | 大量 view 直接写业务 UI | 批量迁移需要先抽模式 |
| 两端体验不统一 | 管理端侧边栏，用户端顶栏和命令面板 | 品牌一致性不足 |
| 类型不足 | 组件 props、table columns、form schema 缺少约束 | 组件库长期维护风险高 |

## 2. 外部参考结论

调研了以下 GitHub 项目：

| 项目 | 链接 | 主要参考价值 |
| --- | --- | --- |
| vue-pure-admin | [pure-admin/vue-pure-admin](https://github.com/pure-admin/vue-pure-admin) | Element Plus 后台、暗色模式、移动端适配、成熟布局 |
| vue-element-plus-admin | [kailong321200875/vue-element-plus-admin](https://github.com/kailong321200875/vue-element-plus-admin) | 主题配置、权限路由、二次封装组件 |
| vue3-element-admin | [youlaitech/vue3-element-admin](https://github.com/youlaitech/vue3-element-admin) | 简洁后台、暗黑模式、多布局、权限体系 |
| SoybeanAdmin ElementPlus | [soybeanjs/soybean-admin-element-plus](https://github.com/soybeanjs/soybean-admin-element-plus) | 主题系统分层、工程规范、组件系统边界 |
| Vue Vben Admin | [vbenjs/vue-vben-admin](https://github.com/vbenjs/vue-vben-admin) | 大型工程分层、packages、权限/主题模块化 |
| Art Design Pro | [Daymychen/art-design-pro](https://github.com/Daymychen/art-design-pro) | 视觉完成度、交互动效、表格/表单抽象 |
| Vuestic Admin | [epicmaxco/vuestic-admin](https://github.com/epicmaxco/vuestic-admin) | 可访问性和 dashboard 组织方式 |

### 2.1 不能直接照搬的原因

这些项目大多已经全面 TypeScript 化，有些引入 Tailwind、UnoCSS、Monorepo、自动路由、Shadcn UI 或 Naive UI。Code-Nest 当前已有大量业务页面，如果直接 fork 或强行迁移，会同时改动：

- UI 框架
- 样式体系
- 路由体系
- 状态管理习惯
- 请求封装
- 菜单和权限
- 构建方式
- 页面目录

这会让“UI 重构”变成“前端系统重建”，风险过大。

### 2.2 应该吸收的东西

| 来源 | 吸收内容 | Code-Nest 落地方式 |
| --- | --- | --- |
| vue-pure-admin | 后台 layout、暗色适配、Element Plus 主题 | 管理端 layout 和 theme token |
| vue-element-plus-admin | 主题配置面板、动态菜单、二次封装 | `CnThemeDrawer`、`CnSidebar`、`CnDataTable` |
| vue3-element-admin | 简洁后台信息架构 | 管理端页面模板规范 |
| SoybeanAdmin | token 分层、主题配置、规范化目录 | `design-system/tokens`、`themes`、`components` |
| Vben Admin | packages 思想、长期工程边界 | 后续抽 `packages/code-nest-ui` |
| Art Design Pro | 精致视觉、动效、表单/表格组合式函数 | 用户端工作台、`useCnTable`、`useCnForm` |

## 3. 最终技术路线

### 3.1 推荐路线

```text
阶段 1：两端本地 design-system
  - 在 admin/user 两端分别建立同构目录
  - 用 TypeScript 写新增组件和 composables
  - CSS variables 管主题
  - Element Plus 继续作为底层组件库

阶段 2：试点页面
  - 管理端 Dashboard
  - 管理端用户管理
  - 用户端首页或学习工作台
  - 用户端 OJ/面试列表

阶段 3：抽共享包
  - 当两端组件 API 稳定后，再抽 `packages/code-nest-ui`
  - 避免一开始因为 package/workspace 配置阻塞业务迁移

阶段 4：批量迁移
  - 管理端按模块迁移
  - 用户端按场景迁移
```

### 3.2 为什么组件库必须用 TypeScript

组件库和普通页面不同，组件库是约束别人怎么写代码的地方。TypeScript 对下面这些点非常关键：

- `props` 类型：防止组件被错误使用。
- `emits` 类型：保证事件和 payload 稳定。
- `CnDataTable` columns 类型：避免字段名、formatter、slot 配置混乱。
- `CnFilterForm` schema 类型：避免表单项配置不可控。
- 主题枚举类型：避免传不存在的主题名。
- composable 返回类型：让页面使用时有补全和约束。
- 未来抽 package 时自动生成类型声明。

建议新组件统一使用：

```vue
<script setup lang="ts">
</script>
```

建议新增工具函数统一使用：

```text
src/design-system/**/*.ts
```

现有 `.js` 页面先不强制改，迁移到新组件时再逐步 TS 化。

### 3.3 为什么不先做全项目 TS 迁移

当前两端页面数接近 190 个。全量 TS 迁移会涉及：

- 路由 meta 类型
- Pinia store 类型
- API response 类型
- 表单 model 类型
- 业务实体类型
- 第三方组件类型
- 大量隐式 any
- 旧代码潜在类型错误

如果和 UI 重构同时做，风险会叠加。正确做法是：

```text
新设计系统必须 TS
新组件必须 TS
新 composable 必须 TS
被迁移页面优先 TS
未迁移旧页面暂时保持 JS
```

这样既能拿到 TS 的长期收益，又不会让第一阶段失控。

## 4. 目标架构

### 4.1 第一阶段目录结构

管理端：

```text
vue3-admin-front/
  src/
    design-system/
      index.ts
      components/
        app/
        data/
        feedback/
        navigation/
        theme/
      composables/
      plugins/
      styles/
      themes/
      tokens/
      types/
    styles/
    layout/
    router/
    stores/
    views/
```

用户端：

```text
vue3-user-front/
  src/
    design-system/
      index.ts
      components/
        app/
        data/
        feedback/
        navigation/
        theme/
      composables/
      plugins/
      styles/
      themes/
      tokens/
      types/
    styles/
    config/
    router/
    stores/
    views/
```

### 4.2 后续共享包结构

等第一阶段稳定后，再演进成：

```text
packages/
  code-nest-ui/
    package.json
    tsconfig.json
    vite.config.ts
    src/
      index.ts
      components/
      composables/
      styles/
      themes/
      tokens/
      types/

vue3-admin-front/
  src/
    design-system/        # 只保留项目侧适配，或完全移除

vue3-user-front/
  src/
    design-system/        # 只保留项目侧适配，或完全移除
```

不建议第一天就上 `packages/`，因为当前仓库根目录是 Maven 多模块项目，不是 Node monorepo。过早加入 workspace 会引入 npm workspace/pnpm workspace、构建路径、IDE 识别、依赖提升等额外问题。

## 5. 主题系统设计

### 5.1 主题目标

首期支持：

| 主题 | 用途 |
| --- | --- |
| `light` | 默认亮色主题 |
| `dark` | 暗色主题 |
| `professional-blue` | 管理端专业蓝主题 |

第二批可选：

| 主题 | 用途 |
| --- | --- |
| `growth` | 用户端成长平台主题 |
| `high-contrast` | 高对比可访问性主题 |

### 5.2 Token 分层

Token 分三层：

```text
Primitive tokens
  -> 原始色阶、尺寸、字体、阴影

Semantic tokens
  -> 页面背景、文本、边框、品牌色、状态色

Component tokens
  -> card、button、table、dialog、nav、form
```

建议文件：

```text
src/design-system/tokens/
  primitive.css
  semantic.css
  component.css
  motion.css

src/design-system/themes/
  light.css
  dark.css
  professional-blue.css

src/design-system/plugins/
  element-plus.css
```

### 5.3 Token 命名规范

建议统一使用：

```css
--cn-color-bg-page
--cn-color-bg-surface
--cn-color-bg-elevated
--cn-color-text-primary
--cn-color-text-secondary
--cn-color-text-tertiary
--cn-color-border
--cn-color-border-subtle
--cn-color-brand-primary
--cn-color-brand-hover
--cn-color-brand-soft
--cn-color-success
--cn-color-warning
--cn-color-danger
--cn-radius-control
--cn-radius-card
--cn-radius-panel
--cn-shadow-card
--cn-shadow-popover
--cn-motion-fast
--cn-motion-base
--cn-motion-slow
--cn-ease-out
```

避免继续扩散旧式命名：

```css
--cn-primary
--cn-bg-page
--cn-surface-1
```

旧变量可以先保留兼容，但新增代码必须使用新 token。

### 5.4 Theme CSS 示例

```css
/* themes/light.css */
:root,
[data-theme='light'] {
  --cn-color-bg-page: #f6f8fb;
  --cn-color-bg-surface: #ffffff;
  --cn-color-bg-elevated: #ffffff;
  --cn-color-text-primary: #172033;
  --cn-color-text-secondary: #536179;
  --cn-color-text-tertiary: #7b879a;
  --cn-color-border: #d8e0ec;
  --cn-color-border-subtle: #e7ecf4;
  --cn-color-brand-primary: #2563eb;
  --cn-color-brand-hover: #1d4ed8;
  --cn-color-brand-soft: #eaf1ff;
}
```

```css
/* themes/dark.css */
[data-theme='dark'] {
  --cn-color-bg-page: #0f172a;
  --cn-color-bg-surface: #111827;
  --cn-color-bg-elevated: #1f2937;
  --cn-color-text-primary: #e5e7eb;
  --cn-color-text-secondary: #b6c2d2;
  --cn-color-text-tertiary: #8b98aa;
  --cn-color-border: #334155;
  --cn-color-border-subtle: #263244;
  --cn-color-brand-primary: #60a5fa;
  --cn-color-brand-hover: #93c5fd;
  --cn-color-brand-soft: rgba(96, 165, 250, 0.16);
}
```

### 5.5 Element Plus 映射

Element Plus 不做二次 fork，只通过 CSS variables 接入：

```css
:root,
[data-theme] {
  --el-color-primary: var(--cn-color-brand-primary);
  --el-color-primary-dark-2: var(--cn-color-brand-hover);
  --el-bg-color: var(--cn-color-bg-surface);
  --el-bg-color-page: var(--cn-color-bg-page);
  --el-text-color-primary: var(--cn-color-text-primary);
  --el-text-color-regular: var(--cn-color-text-secondary);
  --el-border-color: var(--cn-color-border);
  --el-border-color-light: var(--cn-color-border-subtle);
  --el-border-radius-base: var(--cn-radius-control);
}
```

复杂组件继续单独覆盖：

```text
plugins/element-plus/
  button.css
  input.css
  table.css
  dialog.css
  drawer.css
  menu.css
  pagination.css
  tag.css
```

### 5.6 主题状态管理

新增：

```text
src/design-system/types/theme.ts
src/design-system/composables/useTheme.ts
src/stores/theme.ts
```

类型草案：

```ts
export type CodeNestThemeName =
  | 'light'
  | 'dark'
  | 'professional-blue'
  | 'growth'
  | 'high-contrast'

export type CodeNestThemeMode = CodeNestThemeName | 'system'

export interface ThemePreference {
  mode: CodeNestThemeMode
  resolvedTheme: CodeNestThemeName
  followSystem: boolean
}
```

行为要求：

- 默认读取 `localStorage['code-nest-theme']`。
- 如果没有本地配置，默认 `system`。
- `system` 根据 `prefers-color-scheme` 解析为 `light` 或 `dark`。
- 每次切换时写入 `document.documentElement.dataset.theme`。
- 切换主题不刷新页面。
- 后续图表、Monaco、G6 单独订阅主题变化。

## 6. 组件库设计

### 6.1 组件库边界

第一阶段不做完整基础 UI 组件库，不重新实现：

- Button
- Input
- Select
- DatePicker
- Dialog
- Table 原子能力

这些继续使用 Element Plus。

第一阶段优先做 Code-Nest 高频业务组件：

- 页面容器
- 页面头部
- 筛选表单
- 表格容器
- 工具栏
- 统计卡
- 状态标签
- 空状态
- 主题切换
- 命令面板

### 6.2 首批组件清单

```text
components/app/
  CnPage.vue
  CnPageHeader.vue
  CnSection.vue

components/data/
  CnDataTable.vue
  CnFilterForm.vue
  CnToolbar.vue
  CnStatCard.vue
  CnStatusTag.vue
  CnEmptyState.vue

components/navigation/
  CnCommandPalette.vue
  CnSidebar.vue
  CnTopNav.vue

components/theme/
  CnThemeSwitch.vue
  CnThemeDrawer.vue

components/feedback/
  CnResultState.vue
```

### 6.3 CnPage

职责：

- 统一页面最大宽度、padding、背景、响应式间距。
- 提供 `dense`、`fullHeight`、`surface` 等布局模式。

API 草案：

```ts
export interface CnPageProps {
  title?: string
  description?: string
  dense?: boolean
  fullHeight?: boolean
  surface?: 'transparent' | 'plain' | 'panel'
}
```

用法：

```vue
<CnPage surface="transparent">
  <CnPageHeader title="用户管理" description="管理用户、状态和权限" />
  <CnSection>
    ...
  </CnSection>
</CnPage>
```

### 6.4 CnPageHeader

职责：

- 统一页面标题、描述、面包屑、右侧操作。
- 替代散落页面中的 `.page-header`、`.header-card`。

API 草案：

```ts
export interface CnPageHeaderProps {
  title: string
  description?: string
  eyebrow?: string
  breadcrumbs?: Array<{ label: string; to?: string }>
  compact?: boolean
}
```

Slots：

```text
default
actions
meta
```

### 6.5 CnFilterForm

职责：

- 统一查询区样式。
- 根据 schema 渲染常用筛选项。
- 支持自定义 slot。

类型草案：

```ts
export type CnFilterFieldType =
  | 'input'
  | 'select'
  | 'date'
  | 'daterange'
  | 'switch'
  | 'custom'

export interface CnFilterOption {
  label: string
  value: string | number | boolean
}

export interface CnFilterField {
  prop: string
  label: string
  type: CnFilterFieldType
  placeholder?: string
  options?: CnFilterOption[]
  span?: number
  clearable?: boolean
}
```

Emits：

```ts
{
  search: []
  reset: []
  'update:modelValue': [value: Record<string, unknown>]
}
```

### 6.6 CnDataTable

职责：

- 统一表格外壳、loading、empty、pagination、toolbar slot。
- 先不替代 Element Plus Table 的所有能力。
- 支持 columns schema + slot。

类型草案：

```ts
export interface CnTableColumn<T = Record<string, unknown>> {
  prop?: keyof T | string
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: boolean | 'left' | 'right'
  slot?: string
  formatter?: (row: T, column: CnTableColumn<T>, value: unknown, index: number) => string
}

export interface CnPagination {
  page: number
  pageSize: number
  total: number
}
```

用法：

```vue
<CnDataTable
  :columns="columns"
  :data="users"
  :loading="loading"
  :pagination="pagination"
  @page-change="handlePageChange"
>
  <template #status="{ row }">
    <CnStatusTag :type="row.enabled ? 'success' : 'danger'">
      {{ row.enabled ? '正常' : '禁用' }}
    </CnStatusTag>
  </template>
</CnDataTable>
```

### 6.7 CnStatCard

职责：

- 统一首页、Dashboard、统计分析页里的指标卡。
- 支持 icon、trend、loading、主题色。

API 草案：

```ts
export interface CnStatCardProps {
  title: string
  value: string | number
  unit?: string
  description?: string
  trend?: 'up' | 'down' | 'flat'
  trendText?: string
  tone?: 'brand' | 'success' | 'warning' | 'danger' | 'neutral'
  loading?: boolean
}
```

### 6.8 CnThemeSwitch / CnThemeDrawer

职责：

- `CnThemeSwitch`：轻量切换 light/dark/system。
- `CnThemeDrawer`：完整主题配置面板，后续支持品牌色、圆角、密度。

首期功能：

- 亮色
- 暗色
- 跟随系统
- 专业蓝
- localStorage 持久化

## 7. Composables 设计

### 7.1 useTheme

职责：

- 读取主题状态。
- 设置主题。
- 监听系统主题变化。
- 同步 `data-theme`。

```ts
export function useTheme() {
  return {
    themeMode,
    resolvedTheme,
    setTheme,
    toggleDark,
    isDark
  }
}
```

### 7.2 useCnTable

职责：

- 管理列表 loading、pagination、query、reload。
- 兼容现有 API 调用方式。

```ts
export interface UseCnTableOptions<T, Q> {
  query: Q
  pageSize?: number
  fetcher: (params: Q & { pageNum: number; pageSize: number }) => Promise<{
    records?: T[]
    list?: T[]
    total: number
  }>
}
```

### 7.3 useCnBreakpoints

职责：

- 统一移动端断点。
- 避免页面里反复写 `window.innerWidth`。

```ts
export function useCnBreakpoints() {
  return {
    isMobile,
    isTablet,
    isDesktop
  }
}
```

## 8. 样式组织规范

### 8.1 文件拆分

当前 `src/styles/index.scss` 职责过多。建议拆分：

```text
src/styles/
  index.scss              # 只负责 import
  reset.scss
  base.scss
  utilities.scss
  markdown.scss
  motion.scss

src/design-system/
  tokens/
  themes/
  plugins/
  styles/
```

`index.scss` 最终只保留：

```scss
@use '@/design-system/tokens/primitive.css';
@use '@/design-system/tokens/semantic.css';
@use '@/design-system/tokens/component.css';
@use '@/design-system/tokens/motion.css';
@use '@/design-system/themes/light.css';
@use '@/design-system/themes/dark.css';
@use '@/design-system/themes/professional-blue.css';
@use '@/design-system/plugins/element-plus.css';
@use './reset.scss';
@use './base.scss';
@use './utilities.scss';
```

### 8.2 禁止规则

新增代码中尽量禁止：

- 页面里直接写大量 hex 色值。
- 重复定义 `.page-header`、`.search-card`、`.table-card`。
- 用 scoped CSS 覆盖 Element Plus 深层结构，除非组件内部确实需要。
- 用渐变和阴影制造不必要的“卡片套卡片”。
- 用户端和管理端复制两份不同命名的相同组件。

### 8.3 允许规则

允许：

- 页面保留少量局部 layout 样式。
- 特殊业务模块自定义视觉，但必须基于 token。
- 试点阶段 admin/user 两端暂时复制 design-system，后续再抽包。

## 9. 视觉方向

### 9.1 管理端

关键词：

- 专业
- 克制
- 信息密度高
- 表格和筛选效率优先
- 状态清晰
- 视觉装饰少

参考：

- vue-pure-admin
- vue3-element-admin
- vue-element-plus-admin

建议：

- 左侧导航保留，但重新组件化。
- 页面容器减少卡片嵌套。
- 表格、筛选、工具栏统一。
- 可提供密度设置：舒适 / 默认 / 紧凑。

### 9.2 用户端

关键词：

- 产品化
- 成长平台感
- 工作台感
- 轻动效
- 清晰的内容层级
- 移动端可用

参考：

- Art Design Pro
- SoybeanAdmin
- 当前 Code-Nest 用户端已有导航和命令面板

建议：

- 顶部导航和命令面板保留，但主题化。
- 首页、学习、面试、OJ 做成高完成度试点。
- 用户端允许比管理端更强的视觉层次，但仍要控制渐变和装饰。

## 10. 实施计划

### 10.1 Phase 0：准备和约束

目标：

- 确认技术路线。
- 建立分支和回滚点。
- 确认两端启动正常。

任务：

- 确认当前分支：`v2.3.0`。
- 确认后端、管理端、用户端能启动。
- 新增文档和任务清单。
- 暂不改业务逻辑。

验收：

- `git status` 只包含本次计划文档或预期文件。
- 本地服务可启动。

### 10.2 Phase 1：TypeScript Design System 地基

目标：

- 在 admin/user 两端建立同构 `src/design-system`。
- 新增 TS 组件和 composables 的基础能力。
- 建立主题 token 和 Element Plus 映射。

任务：

- 新增 `src/design-system/index.ts`。
- 新增 `types/theme.ts`。
- 新增 `composables/useTheme.ts`。
- 新增 `tokens`、`themes`、`plugins/element-plus`。
- 新增 `stores/theme.ts` 或 `stores/theme.js`。推荐 `.ts`。
- main.js 引入新的 theme CSS。
- 保留旧变量兼容层。

验收：

- 管理端和用户端都能编译。
- 切换 `data-theme` 后 Element Plus 基础组件跟随变化。
- 不迁移业务页面。

预计：2-4 天。

### 10.3 Phase 2：基础组件

目标：

- 先完成 6 个高频组件。

首批组件：

```text
CnPage
CnPageHeader
CnSection
CnStatCard
CnStatusTag
CnEmptyState
```

任务：

- 每个组件使用 `<script setup lang="ts">`。
- 每个组件提供 props 类型。
- 每个组件只依赖 token，不写硬编码主题色。
- 组件内部避免强绑定具体业务。

验收：

- Story/demo 页面或试点页面可展示全部组件状态。
- light/dark/professional-blue 下视觉可用。

预计：2-4 天。

### 10.4 Phase 3：试点页面

目标：

- 验证组件库是否能支撑真实页面。

建议试点：

| 项目 | 页面 | 目的 |
| --- | --- | --- |
| 管理端 | Dashboard | 验证统计卡、布局、主题 |
| 管理端 | 用户管理 | 验证筛选、表格、操作、弹窗 |
| 用户端 | 首页或学习驾驶舱 | 验证产品化视觉 |
| 用户端 | OJ 或面试列表 | 验证列表、卡片、状态 |

任务：

- 将试点页面迁移到新组件。
- 试点页面中尽量消除局部硬编码颜色。
- 如果组件 API 不顺手，先改组件 API，不要让页面妥协。

验收：

- 试点页面在 3000/3001 可访问。
- 主题切换无明显错色。
- 移动端 375px 不破版。
- 页面代码比迁移前更短或更清晰。

预计：4-8 天。

### 10.5 Phase 4：数据组件

目标：

- 建立 `CnFilterForm`、`CnDataTable`、`CnToolbar`。

任务：

- 设计 table columns 类型。
- 设计 filter schema 类型。
- 支持 slot 扩展。
- 不一次性覆盖所有复杂场景。
- 先满足用户管理、日志、列表页的常见需求。

验收：

- 管理端至少 3 个列表页复用 `CnDataTable`。
- 筛选区、表格区、分页样式统一。

预计：5-10 天。

### 10.6 Phase 5：批量迁移管理端

优先级：

1. Dashboard、用户管理、日志管理。
2. 系统管理、通知、敏感词。
3. 社区、朋友圈、聊天室。
4. OJ、面试、模拟面试。
5. 简历、知识图谱、学习资产。
6. 积分、抽奖、摸鱼、文件存储、CodePen。

迁移原则：

- 每次迁移一个模块。
- 每个模块完成后做一次视觉检查。
- 不在批量迁移中随意改变接口和业务逻辑。

预计：2-4 周。

### 10.7 Phase 6：批量迁移用户端

优先级：

1. 全局导航、命令面板、登录注册、个人中心。
2. 首页、学习驾驶舱、学习资产。
3. 面试、模拟面试、OJ。
4. 社区、朋友圈、博客、团队。
5. 闪卡、积分、抽奖。
6. 工具类、摸鱼、CodePen。

预计：3-6 周。

### 10.8 Phase 7：抽共享包

触发条件：

- 两端都有至少 10 个页面使用 design-system。
- 组件 API 连续两轮迁移没有大改。
- 主题 token 稳定。

目标结构：

```text
packages/code-nest-ui
```

注意：

- 这一步可以晚一点做。
- 不要为了“看起来高级”提前抽包。

## 11. 验收标准

### 11.1 技术验收

- 两端 `npm run build` 通过。
- 新增组件全部使用 TypeScript。
- 新增 composables 全部使用 TypeScript。
- 主题切换不刷新页面。
- `data-theme` 正确写入 HTML。
- Element Plus 基础组件跟随主题。
- 旧页面不因为主题系统引入而破版。

### 11.2 视觉验收

- 管理端信息密度更稳定，表格/筛选/工具栏统一。
- 用户端更有产品感，但不花哨。
- light/dark/professional-blue 三个主题可用。
- 375px、768px、1440px 三档宽度可用。
- 文本不溢出按钮、卡片、标签。
- 弹窗、抽屉、表格在小屏可滚动。

### 11.3 可维护性验收

- 新页面优先使用 `CnPage`、`CnPageHeader`、`CnSection`。
- 列表页优先使用 `CnFilterForm`、`CnDataTable`。
- 新增颜色必须先进 token。
- 新增组件必须导出类型。
- 页面局部样式不能重复定义通用模式。

## 12. 风险与回滚

| 风险 | 表现 | 应对 |
| --- | --- | --- |
| TS 接入导致旧页面报错 | `.vue` 类型检查变严格 | 只新增 TS 文件，不强制改旧 JS 页面 |
| 主题变量影响旧样式 | 页面颜色错乱 | 保留旧 `--cn-*` 兼容层 |
| 组件 API 过早定死 | 后续页面不好用 | 先试点，组件 API 允许快速调整 |
| 一次迁移太多页面 | 回归困难 | 每个模块单独提交 |
| 暗色主题适配不完整 | 图表/编辑器/Markdown 错色 | 先标记风险组件，分批接入 |
| 共享包抽得太早 | workspace 配置拖慢进度 | 等组件稳定后再抽 |

回滚策略：

- Phase 1 只增加设计系统，不替换页面，风险低。
- Phase 2/3 每个试点页面单独提交。
- 若新组件路线不合适，可保留 token，只回滚页面迁移。
- 不改后端接口和数据库。

## 13. 新对话实施提示词

如果后续新开一个对话开始实现，可以直接贴下面这段：

```text
我们要在 Code-Nest 的 v2.3.0 分支实现前端 UI 组件化与主题系统重构。

请先阅读：
AI-DOCS/Development/UI-theme-refactor-reference-and-plan.md

当前项目：
- 管理端：vue3-admin-front，Vue3 + Vite + JS + Element Plus + Pinia + SCSS，端口 3000
- 用户端：vue3-user-front，Vue3 + Vite + JS + Element Plus + Pinia + SCSS，端口 3001
- 两端已有 tsconfig.json，可以增量接入 TypeScript

执行路线：
1. 不直接 fork 开源模板。
2. 不全量重写现有页面。
3. 新增 design-system，新增组件和 composables 必须使用 TypeScript。
4. 保留 Element Plus 作为底层组件库。
5. 用 CSS variables 建立 light/dark/professional-blue 主题。
6. 先在 admin/user 两端分别建立同构 design-system，稳定后再考虑抽 packages/code-nest-ui。
7. 第一批组件：CnPage、CnPageHeader、CnSection、CnStatCard、CnStatusTag、CnEmptyState、CnThemeSwitch。
8. 第一批试点页面：管理端 Dashboard、管理端用户管理、用户端首页或学习工作台、用户端 OJ/面试列表。

请从 Phase 1 开始：建立 TypeScript Design System 地基和主题系统，不要先迁移业务页面。
```

## 14. 推荐第一批文件清单

管理端首批：

```text
vue3-admin-front/src/design-system/index.ts
vue3-admin-front/src/design-system/types/theme.ts
vue3-admin-front/src/design-system/composables/useTheme.ts
vue3-admin-front/src/design-system/tokens/primitive.css
vue3-admin-front/src/design-system/tokens/semantic.css
vue3-admin-front/src/design-system/tokens/component.css
vue3-admin-front/src/design-system/tokens/motion.css
vue3-admin-front/src/design-system/themes/light.css
vue3-admin-front/src/design-system/themes/dark.css
vue3-admin-front/src/design-system/themes/professional-blue.css
vue3-admin-front/src/design-system/plugins/element-plus.css
vue3-admin-front/src/design-system/components/theme/CnThemeSwitch.vue
vue3-admin-front/src/stores/theme.ts
```

用户端首批同构：

```text
vue3-user-front/src/design-system/index.ts
vue3-user-front/src/design-system/types/theme.ts
vue3-user-front/src/design-system/composables/useTheme.ts
vue3-user-front/src/design-system/tokens/primitive.css
vue3-user-front/src/design-system/tokens/semantic.css
vue3-user-front/src/design-system/tokens/component.css
vue3-user-front/src/design-system/tokens/motion.css
vue3-user-front/src/design-system/themes/light.css
vue3-user-front/src/design-system/themes/dark.css
vue3-user-front/src/design-system/themes/professional-blue.css
vue3-user-front/src/design-system/plugins/element-plus.css
vue3-user-front/src/design-system/components/theme/CnThemeSwitch.vue
vue3-user-front/src/stores/theme.ts
```

## 15. 最终建议

这次重构应该被当成 Code-Nest 前端工程能力升级，而不只是 UI 美化。

我建议最终拍板为：

```text
TypeScript Design System 优先
Element Plus 继续作为底层
CSS variables 做主题
SCSS 保留但拆分职责
旧页面渐进迁移
组件稳定后再抽共享 package
```

这条路线的好处是：

- 有长期组件库沉淀。
- 有 TypeScript 的类型收益。
- 不会立刻打爆现有业务页面。
- 能逐步交付看得见的 UI 改进。
- 后续新功能可以直接使用 Code-Nest 自己的组件系统。

## 16. 来源

- [pure-admin/vue-pure-admin](https://github.com/pure-admin/vue-pure-admin)
- [kailong321200875/vue-element-plus-admin](https://github.com/kailong321200875/vue-element-plus-admin)
- [youlaitech/vue3-element-admin](https://github.com/youlaitech/vue3-element-admin)
- [soybeanjs/soybean-admin-element-plus](https://github.com/soybeanjs/soybean-admin-element-plus)
- [soybeanjs/soybean-admin](https://github.com/soybeanjs/soybean-admin)
- [vbenjs/vue-vben-admin](https://github.com/vbenjs/vue-vben-admin)
- [Daymychen/art-design-pro](https://github.com/Daymychen/art-design-pro)
- [epicmaxco/vuestic-admin](https://github.com/epicmaxco/vuestic-admin)
