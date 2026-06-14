# 用户端首页高颜值改版 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在保持当前蓝白科技基调不变的前提下，把用户端首页升级为“内容更丰富、动效更高级、全量真实数据”的高端视觉首页。

**Architecture:** 采用“视觉层 + 数据层 + 动效层”三层拆分。视觉层重构首页模块布局；数据层统一真实接口拉取与降级策略（严禁 mock）；动效层统一入场、滚动、悬停、视差、数字增长和节奏控制，保证性能与观感平衡。

**Tech Stack:** Vue 3 + Element Plus + SCSS + requestAnimationFrame + IntersectionObserver + 现有用户端 API；缺口数据按需补后端聚合接口。

---

## 约束与底线

- 不改“蓝白科技感”主基调。
- 不允许任何 mock/硬编码演示数据。
- 动效遵循 `prefers-reduced-motion`，并控制在 60fps 目标内。
- 首屏可交互时间不能明显倒退（对比现网最多增加 10%）。

---

### Task 1: 信息架构重排（先把内容做“满”）

**Files:**
- Modify: `vue3-user-front/src/views/Home.vue`
- Modify: `vue3-user-front/src/styles/index.scss`

**Step 1: 首屏改为双列 Hero（品牌 + 动态数据面板）**
- 左侧：品牌主文案、核心 CTA。
- 右侧：实时数据卡（学习数、在线数、热门主题数、今日任务完成率）。

**Step 2: 在 Hero 下新增 4 个内容区块**
- `今日热门`：社区热门帖子 + 热门动态。
- `成长驾驶舱`：学习进度、打卡、积分摘要。
- `每日挑战`：OJ 每日一题 + AI 模拟面试入口。
- `版本播报`：最近版本更新时间线。

**Step 3: 保留现有“核心功能/快速入口/特色亮点/CTA”并降权到次级内容区**
- 通过层级、尺寸、留白，让新增内容优先被看到。

**Step 4: 本地验收**
- Run: `npm run build`（`vue3-user-front`）
- Expected: 打包成功，无报错。

---

### Task 2: 动效体系升级（高端感关键）

**Files:**
- Modify: `vue3-user-front/src/styles/motion.scss`
- Modify: `vue3-user-front/src/views/Home.vue`
- Create: `vue3-user-front/src/composables/useHomeMotion.js`

**Step 1: 增加入场编排（stagger timeline）**
- 首屏文字、按钮、数据卡、背景光斑按 60~100ms 级联入场。

**Step 2: 增加滚动触发动画**
- 每个 section 使用 `IntersectionObserver` 单次 reveal。
- 卡片使用“轻上浮 + 透明度 + 阴影呼吸”。

**Step 3: 增加交互动效**
- Hero 背景视差（鼠标轻微偏移）。
- 卡片 hover 倾斜（2~4 度以内）。
- 数字滚动增长（easeOutCubic）。

**Step 4: 动效降级策略**
- `prefers-reduced-motion: reduce` 下关闭复杂动画，仅保留淡入。

**Step 5: 本地验收**
- 手工检查：动效不抖动、不突兀；滚动不卡顿。

---

### Task 3: 真实数据编排（零 mock）

**Files:**
- Modify: `vue3-user-front/src/views/Home.vue`
- Create: `vue3-user-front/src/composables/useHomeData.js`
- Optional Create: `vue3-user-front/src/api/home.js`

**Step 1: 接入现有真实接口（先不补后端）**
- 学习总量：`interviewApi.getTotalLearned()`
- 图谱总量：`getPublishedKnowledgeMaps()`
- 在线人数：`getOnlineCount()`
- 社区热门：`communityApi.getHotPosts()`
- 热门动态：`getHotMoments()`
- 每日一题：`ojApi.getDailyProblem()`
- 模拟面试统计：`mockInterviewApi.getStats()`
- 打卡统计：`planApi.getStatsOverview()`
- 积分余额：`pointsApi.getPointsBalance()`
- 最新版本：`versionApi.getLatestVersions()`

**Step 2: 统一请求与错误兜底**
- 页面级 `Promise.allSettled`，单模块失败不影响全页。
- 失败模块显示“暂不可用”状态，不显示假数据。

**Step 3: 数据刷新策略**
- 首屏关键数据首次加载。
- 在线人数与热门模块可设置 60s 轻刷新（可选）。

**Step 4: 本地验收**
- 打开 Network 确认无 mock 请求、无本地伪数据来源。

---

### Task 4: 视觉质感升级（保持基调但更高级）

**Files:**
- Modify: `vue3-user-front/src/views/Home.vue`
- Modify: `vue3-user-front/src/styles/index.scss`

**Step 1: Typography 升级**
- 标题更强对比，正文更高可读性，减少“发灰”感。

**Step 2: 背景层次升级**
- 保持蓝白系，加入网格纹理/柔光层/径向渐变分层。

**Step 3: 卡片体系升级**
- 主卡片玻璃质感，次级卡片统一边框与阴影语义。

**Step 4: 微交互统一**
- 按钮、标签、图标 hover 全部统一节奏与曲线。

---

### Task 5: 后端补口预案（仅在数据不够时启用）

**Files:**
- Optional Create: `xiaou-user/src/main/java/com/xiaou/user/controller/UserHomeController.java`
- Optional Create: `xiaou-user/src/main/java/com/xiaou/user/service/UserHomeService.java`
- Optional Create: `xiaou-user/src/main/java/com/xiaou/user/service/impl/UserHomeServiceImpl.java`
- Optional Create: `xiaou-user/src/main/java/com/xiaou/user/dto/HomePortalResponse.java`

**Step 1: 定义聚合接口**
- `GET /user/home/portal`
- 聚合首页所需：热门内容、成长数据、挑战数据、版本播报。

**Step 2: 前端切单请求（可选）**
- 将多接口并发替换为单接口，降低首页瀑布请求。

**Step 3: 编译验收**
- Run: `mvn -pl xiaou-user -am -DskipTests compile`
- Expected: 编译通过。

---

### Task 6: 性能与可访问性收口

**Files:**
- Modify: `vue3-user-front/src/views/Home.vue`
- Modify: `vue3-user-front/src/styles/motion.scss`

**Step 1: 动效性能收口**
- 仅对 `transform/opacity` 做动画。
- 大块滤镜数量可控，防止低端机掉帧。

**Step 2: 可访问性**
- 关键区块 `aria-label` 完整。
- 主行动按钮焦点态明显可见。

**Step 3: 响应式一致性**
- 断点：`1200 / 992 / 768 / 480` 四档完整验证。

**Step 4: 验收**
- Run: `npm run build`（`vue3-user-front`）
- Expected: 构建成功，仅允许非阻塞 warning。

---

### Task 7: 最终验收清单（上线前）

**Checklist:**
- 首页内容密度明显提升，不再“空”。
- 动效层次明显增强，但不花哨。
- 无 mock 数据，无硬编码统计。
- 移动端首屏结构完整、交互顺滑。
- 视觉基调仍然是 Code Nest 当前蓝白科技风。

**回归点:**
- 路由跳转正确。
- 登录态相关模块正常。
- 接口异常时页面不白屏。

---

## 执行顺序建议（一次性推进）

1. Task 1（信息架构）
2. Task 3（真实数据）
3. Task 2（动效升级）
4. Task 4（视觉精修）
5. Task 6（性能与可访问性）
6. Task 7（验收）
7. Task 5（仅在接口能力不足时补后端）

---

## 里程碑（建议）

- Day 1: Task 1 + Task 3
- Day 2: Task 2 + Task 4
- Day 3: Task 6 + Task 7（必要时启动 Task 5）
