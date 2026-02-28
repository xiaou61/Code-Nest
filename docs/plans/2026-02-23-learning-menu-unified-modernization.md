# 学习菜单统一现代化改版 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将“学习”下拉菜单下的 8 个核心入口页统一为同一套现代化视觉语言，并提升动态交互层次与整体一致性。

**Architecture:** 采用“统一主题基座 + 页面分批改造”的方式推进。先在全局样式层定义学习域的颜色、卡片、动效语义，再分两批接入页面，最后做构建与交互回归。通过可复用类名与最小侵入改动，保证速度和稳定性。

**Tech Stack:** Vue 3 + Element Plus + SCSS + CSS Variables + IntersectionObserver（轻量 reveal）+ 现有路由结构。

---

## 约束与目标

- 保持 Code Nest 当前蓝白科技基调，不改品牌方向。
- 学习模块 8 个入口页视觉统一，避免每页一套色板和阴影。
- 增强动态效果（入场、hover、轻视差、reveal），但遵守 `prefers-reduced-motion`。
- 不引入 mock，不改接口语义，不改变核心业务流程。

---

### Task 1: 统一视觉与动效基座

**Files:**
- Create: `vue3-user-front/src/styles/learning-theme.scss`
- Modify: `vue3-user-front/src/styles/index.scss`
- Modify: `vue3-user-front/src/styles/motion.scss`

**Step 1: 新建学习域主题层**
- 定义学习模块专用变量：背景层、卡片层、强调色、描边、阴影、玻璃效果。
- 定义统一容器类：`cn-learn-shell / cn-learn-hero / cn-learn-panel / cn-learn-grid`。

**Step 2: 新增统一动效语义**
- 定义 reveal 动画类：`cn-learn-reveal` 与 `is-visible`。
- 定义 hover 动效类：`cn-learn-float`、`cn-learn-tilt`、`cn-learn-shine`。

**Step 3: 可访问性与降级**
- 在 `prefers-reduced-motion: reduce` 下关闭复杂动效。

---

### Task 2: 升级学习下拉菜单（全局导航）

**Files:**
- Modify: `vue3-user-front/src/App.vue`

**Step 1: 重构学习菜单项元数据**
- 将学习菜单提取为数组配置，统一图标、描述和分组。

**Step 2: 升级下拉菜单视觉**
- 增加分组标题、图标容器、描述文本、活跃态与 hover 光效。
- 菜单弹层改为更现代的玻璃卡片风格，与学习域主题对齐。

**Step 3: 增加轻交互**
- 下拉项 hover 轻上浮与背景流光。
- 当前路由项高亮。

---

### Task 3: 第一批页面统一（题库 / 在线判题 / 闪卡）

**Files:**
- Modify: `vue3-user-front/src/views/interview/Index.vue`
- Modify: `vue3-user-front/src/views/oj/Index.vue`
- Modify: `vue3-user-front/src/views/flashcard/Index.vue`

**Step 1: 注入统一页面壳**
- 页面根节点接入 `cn-learn-shell`。
- 添加统一 Hero 区（标题、描述、快捷动作）。

**Step 2: 统一卡片系统**
- 侧栏卡、列表卡、统计卡切换到统一 panel 语义类。
- 统一圆角、描边、阴影、间距和标题层级。

**Step 3: 注入动效**
- 首屏卡片 stagger 入场。
- 列表卡 hover 浮起 + 边框 glow。

---

### Task 4: 第二批页面统一（AI 面试 / 图谱 / 打卡 / 小组 / 练习场）

**Files:**
- Modify: `vue3-user-front/src/views/mock-interview/Index.vue`
- Modify: `vue3-user-front/src/views/knowledge/Index.vue`
- Modify: `vue3-user-front/src/views/plan/Index.vue`
- Modify: `vue3-user-front/src/views/team/Index.vue`
- Modify: `vue3-user-front/src/views/oj/Playground.vue`

**Step 1: 对齐统一外层布局**
- 统一 page header 视觉和间距系统。
- 保留各页业务结构，仅重做视觉表达。

**Step 2: 统一色彩与层次**
- 替换离散紫色/灰色方案，统一到学习主题色阶。
- 统一按钮、输入框、标签的视觉语义。

**Step 3: 场景化动效补充**
- 图谱卡片、方向卡片、小组卡片加入 reveal + hover。
- 练习场保留暗色编辑器内核，但重做工具栏与 IO 面板层次。

---

### Task 5: 验证与回归

**Files:**
- Modify: `vue3-user-front/src/views/interview/Index.vue`（若需修复）
- Modify: `vue3-user-front/src/views/oj/Index.vue`（若需修复）
- Modify: `vue3-user-front/src/views/flashcard/Index.vue`（若需修复）
- Modify: `vue3-user-front/src/views/mock-interview/Index.vue`（若需修复）
- Modify: `vue3-user-front/src/views/knowledge/Index.vue`（若需修复）
- Modify: `vue3-user-front/src/views/plan/Index.vue`（若需修复）
- Modify: `vue3-user-front/src/views/team/Index.vue`（若需修复）
- Modify: `vue3-user-front/src/views/oj/Playground.vue`（若需修复）

**Step 1: 构建验证**
- Run: `npm run build`（`vue3-user-front`）
- Expected: 构建成功，无阻塞错误。

**Step 2: 手工回归**
- 学习菜单 8 个入口都可正常跳转。
- 动效在桌面端流畅，移动端无布局断裂。
- `prefers-reduced-motion` 下无强制复杂动画。

---

## 执行顺序

1. Task 1（主题与动效基座）
2. Task 2（学习下拉菜单）
3. Task 3（第一批页面）
4. Task 4（第二批页面）
5. Task 5（验证回归）

---

## 分批交付策略

- 批次 A（先交付）：Task 1 + Task 2 + Task 3
- 批次 B（继续交付）：Task 4 + Task 5

