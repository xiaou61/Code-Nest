---
artifact: spec
status: active
scope: 两套 Vue3 前端应用
---

# 前端应用

Code-Nest 的前端由两套独立的 Vue3 + Vite 应用组成（管理端 `vue3-admin-front`、用户端 `vue3-user-front`），二者均为 SPA + Electron 双形态，共用两个 `file:` 协议链接的本地私有包（`@code-nest/api-contract`、`@code-nest/design-system`）。两套前端均通过 Vite 代理把 `/api` 转发到 `http://localhost:9999`（后端 context-path 已是 `/api`，故不做 rewrite）。统一返回体解包与错误分类集中在 `api-contract`，两端 `src/utils/request.js` 的拦截器实现几乎逐行同构。

## 总览

| 应用 | 定位 | 端口 | 关键入口 |
| --- | --- | --- | --- |
| `vue3-admin-front` | 运营/管理后台（社区、内容、OJ、敏感词、文件存储、SRE、AI 治理、系统配置） | 3000 | `src/main.js`、`src/router/index.js`、`src/utils/request.js`、`src/layout/index.vue`、`electron/main/index.ts` |
| `vue3-user-front` | C 端学习/求职平台（面试、OJ、模拟面试、闪卡、简历、社区、动态、博客、代码作品、积分、计划、团队、知识图谱） | 3001 | `src/main.js`、`src/router/index.js`、`src/utils/request.js`、`src/App.vue`、`src/config/navigation.js`、`electron/main/index.ts` |
| `code-nest-api-contract` | 共享包：返回体解包 + 错误分类 + 请求配置归一化 | — | `src/index.js`、`src/index.d.ts`、`tests/api-contract.test.js` |
| `code-nest-design-system` | 共享包：主题 token、Element Plus 组件封装、组合式函数 | — | `src/index.ts`（+ 32 个源文件） |

## vue3-admin-front（管理端）

**定位与边界**：仅服务管理端运营功能。路由默认强制登录（`to.meta?.requiresAuth !== false`），页面按业务域分片挂到统一 `Layout`。不承载任何 C 端用户页面。

**技术栈**（`vue3-admin-front/package.json`，version 2.5.8，`"type": "module"`，`main: out/main/index.js`）

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| vue / vue-router / pinia | ^3.4.0 / ^4.2.5 / ^2.1.7 | 框架三件套 |
| element-plus / @element-plus/icons-vue | ^2.4.4 / ^2.3.1 | UI 库（`main.js` 全量引入 + 全量 CSS） |
| `@code-nest/api-contract` / `@code-nest/design-system` | `file:../code-nest-api-contract` / `file:../code-nest-design-system` | 本地共享包，非 npm 发布 |
| axios | ^1.6.2 | HTTP 客户端 |
| echarts ^5.6.0、@antv/g6 ^4.8.24、d3 ^7.8.5 | — | 图表 / 图谱 |
| markdown-it ^14.1.0、highlight.js ^11.11.1、dompurify ^3.4.2 | — | Markdown 渲染与净化 |
| vue / vite ^5.0.8、@vitejs/plugin-vue ^4.5.2、sass ^1.69.5、eslint ^8.55.0 + eslint-plugin-vue ^9.19.2 | dev | 构建与 lint |
| electron ^28.0.0、electron-vite ^2.0.0、electron-builder ^24.9.1、electron-store ^8.1.0、@electron-toolkit/utils ^2.0.1 | dev | 桌面打包 |

**目录结构**：`src/{api(26 个文件), components(2), layout(1), router, stores, styles, utils(3), views(75 个文件/26 个目录)}`、`electron/{main/{index,store,tray,window}.ts, preload/index.ts, resources/}`、`tests/`（10 个 `*.test.js` + `helpers/router-source.js`）、`out/{main/index.js, preload/index.mjs}`。

**路由与页面清单**：`src/router/index.js`（60 行）只做组装，六片路由工厂由 `src/router/routes/*.js` 提供，顺序为 core → learning → community → operations → system → fallback。

| 分片 | 一级路径（meta.title） | 子页面 |
| --- | --- | --- |
| core | `/login`（免登录）、`/design-system/components`（免登录）、`/` → `/dashboard`、`/user`、`/profile` | 仪表板、用户管理、个人中心/编辑资料/修改密码 |
| learning | `/interview`、`/mock-interview`、`/oj`、`/resume`、`/knowledge`、`/learning-assets` | 题目分类/题单/题目；面试会话/方向配置；题目与赛事增删改、标签；模板/数据总览/健康巡检；图谱管理/编辑；学习资产审核台/统计 |
| community | `/community`、`/moments`、`/chat`、`/notification`、`/codepen`、`/moyu`、`/blog` | 分类/标签/帖子/评论/用户；动态/评论/统计；消息/在线用户；通知；作品/模板/标签/统计；日历事件/每日内容/统计/Bug 商店；文章/分类/标签 |
| operations | `/logs`、`/sre`、`/sensitive`、`/filestorage`、`/points` | 登录/操作日志；事故工作台；词库/白名单/策略/统计/来源/版本/配置；存储配置/文件管理/迁移/系统设置；积分概览/排行/明细/发放/抽奖管理 |
| system | `/system` | AI 配置与观测、版本管理、AI 质量治理 |
| fallback | `/:pathMatch(.*)*` → 404 | `views/error/404.vue` |

全部业务页使用 `() => import(...)` 懒加载；每个路由带 `meta.title`（用于 `document.title` 与菜单文案）。

**接口层**：`src/api/` 26 个业务模块（agentChat、aiConfig、aiGovernance、auth、blog、chat、codepen、community、dashboard、filestorage、interview、knowledge、learningAssets、log、lotteryAdmin、mockInterview、moment、moyu、notification、oj、points、resume、sensitive、sre、user、version），导出对象式 API 集合（如 `authApi`）。`src/api/auth.js` 命中 `/auth/login`、`/auth/logout`、`/auth/refresh`、`/auth/info`、`/auth/login-logs`、`/auth/profile`、`/auth/password`，即管理端走 `/auth/**` 与 `/admin/**` 分区。`utils/request-options.js` 仅 5 行，从 `@code-nest/api-contract` 直接 re-export，无本地实现。

**状态管理**：`pinia` + setup store。`stores/user.js`（262 行）：`token` 初值 `Cookies.get('token')`，另有 `tokenExpireTime`、`isTokenExpiringSoon`（30 分钟内）、`login/logout/getUserInfo/refreshToken/setToken/changePassword/checkAndRefreshToken`，用 `window` 自定义事件 `authStateChange` 做跨 tab 登出同步。`stores/theme.ts` 复用 design-system 的 `useTheme`。用户信息持久化在 `localStorage.userInfo` + `localStorage.tokenExpireTime`。

**构建与打包**：`vite.config.js` 端口 3000（`open: true`），alias `@`→`src`、`@/design-system`→`node_modules/@code-nest/design-system/src`，`preserveSymlinks: true`，`server.fs.allow` 额外放行 `../code-nest-design-system`；代理 `/api` → `http://localhost:9999`（无 rewrite，无 `ws`）。构建产物 `dist/`，`manualChunks` 按 vendor-graph/charts/markdown/element/router/state/vue/utils 拆分，`chunkSizeWarningLimit: 1200`。Electron 用 `electron.vite.config.ts`（main=`electron/main/index.ts`，preload=`electron/preload/index.ts`，renderer 复用同样 alias 与代理，端口同为 3000）；`electron-builder.yml` → `appId: com.codenest.admin`、`productName: Code-Nest 管理端`、`win.executableName: CodeNest-Admin`、`output: release/${version}`、NSIS x64（`artifactName: ${productName}-Setup-${version}.${ext}`），另有 mac dmg、linux AppImage/deb。

**约束与注意事项**：`tests/route-slices.test.js` 以文本方式断言 `src/router/index.js` 少于 120 行、六个分片文件存在、fallback 位于 system 之后——改动路由装配方式会直接使契约测试失败。`electron/main/index.ts` 全仓唯一实例锁 + 托盘最小化（`store.get('minimizeToTray', true)`），IPC 通道固定为 `window:*`、`shell:openExternal`、`app:getVersion`、`settings:get|set`。

## vue3-user-front（用户端）

**定位与边界**：C 端全部业务页面（面试、OJ、模拟面试、简历、社区、动态、博客、代码作品、闪卡、积分、计划、团队、知识图谱、摸鱼工具、开发者工具、版本历史、引导页）。路由登录校验是**选择性开启**：仅 `to.meta.requiresAuth` 为真才拦截。

**技术栈**（`vue3-user-front/package.json`，version 2.5.8，`main: out/main/index.js`）：与管理端同版本的核心依赖（vue ^3.4.0、vue-router ^4.2.5、pinia ^2.1.7、element-plus ^2.4.4、axios ^1.6.2、两个 `file:` 共享包、echarts 除外），差异项：**无 echarts/d3**，增加 `monaco-editor ^0.52.0` + `@monaco-editor/loader ^1.4.0`（代码编辑器/Playground），devDependencies 增加 `unplugin-vue-components ^0.27.5` 与 `@playwright/test ^1.61.1`。`main.js` 按需只引入 `ElLoading` / message / message-box 的 css，而非全量 CSS。

**目录结构**：`src/{api(30), components(4), config(1), router, stores(5), styles(4), utils(14), views(115 个文件/30 个目录 + Home.vue/HomeRevamp.vue/Profile.vue)}`、`electron/`（与管理端同名同构）、`tests/`（17 个 `*.test.js` + `e2e/mobile-navigation.spec.js` + `helpers/router-source.js`）、`playwright.config.js`。

**路由与页面清单**：同样拆六片（core、learning、career、community、productivity、fallback），但导出的是常量数组（`coreRoutes`、`learningRoutes`…）而非工厂函数。

| 分片 | 一级路径 | 子页面（代表） |
| --- | --- | --- |
| core | `/`(HomeRevamp)、`/login`、`/register`（同一 `auth/Auth.vue`）、`/onboarding`、`/design-system/components` | 引导页、设计系统验收页 |
| learning | `/interview`(6)、`/oj`(9)、`/learning-cockpit`、`/growth-capabilities`、`/learning-assets`、`/knowledge`(2)、`/flashcard`(9) | 抽题/题单/题目/收藏/复习；赛事/题目/提交/统计/Playground/排行；闪卡 Index/Deck/Study/My/Editor |
| career | `/mock-interview`(5)、`/job-battle`、`/job-match-engine`、`/career-loop`、`/sql-optimizer/workbench`、`/growth-autopilot`(重定向)、`/resume`(5) | 模拟面试 Index/Config/Interview/Report/History；简历列表/模板中心/编辑器 |
| community | `/community`(6)、`/notification`、`/moments`(3)、`/chat`、`/blog`(5) | 帖子详情/收藏/我的帖/发帖/用户主页；动态/用户主页/我的收藏；博客编辑/主页/文章详情 |
| productivity | `/profile`、`/points`、`/plan`、`/team`(5)、`/lottery`、`/version-history`、`/dev-tools`(4)、`/moyu-tools`(5) | 团队广场/创建/我的/详情/编辑；JSON/文本对比/翻译工具；热榜/薪资计算/开发者日历/每日内容 |
| fallback | `/:pathMatch(.*)*` → 404 | — |

导航数据另有集中配置 `src/config/navigation.js`：一级 Tab 为 `/`（今天）、`/learning-cockpit`（学习）、`/career-loop`（求职）、`/community`（社区）+「练习与工具」「创作」「更多」，并额外维护「常用场景 / 学习与训练 / 创作与输出 / 更多与协作」分组。

**接口层**：`src/api/` 30 个模块，含学习域增量文件 `careerLoop、growthAutopilot、growthCoach、jobBattle、learningCockpit、sqlOptimizer`，以及 `captcha`、`upload`、`flashcard`、`plan`、`team`、`home`。抽样 `src/api/auth.js` 走 `/user/auth/{login,register,refresh,logout,check-username,check-email}`，与用户端 `/user/**` 分区一致。`utils/` 除 `request.js`/`request-options.js` 外有 12 个业务适配层（`home-data-adapter`、`career-loop-adapter`、`oj-contest-adapter`、`interview-navigation`、`monacoCompletions`、`home-motion`、`reveal-motion`、`cache`、`timeUtil` 等）。

**状态管理**：`stores/{user.js, theme.ts, community.js, interview.js, oj.js}`。`stores/user.js` 仅 86 行：`token` 初值 `Cookies.get('user_token')`，`setToken` 同时写 Cookie（7 天）与 `localStorage.user_token`，用户信息存 `localStorage.user_info`；`logout()` **只清本地、不调用后端**；无 refresh/过期时间/跨 tab 广播。`main.js` 在 `router.isReady()` 后预加载面试 store。

**构建与打包**：`vite.config.js` 端口 3001，别名与 fs.allow 同管理端；代理 `/api` → `http://localhost:9999` 且多一个 **`ws: true`**（聊天/通知 WebSocket 需要）。额外启用 `unplugin-vue-components`（`dts: false`，`ElementPlusResolver({ importStyle: 'css' })`），并用 `exclude: /node_modules[\\/](?!@code-nest[\\/]design-system[\\/])/` 把共享包保留在转换范围内以自动导入其中的 Element Plus 标签。`manualChunks` 比管理端多一个 `vendor-monaco`、无 charts 分支。Electron 配置（`electron.vite.config.ts`、`electron-builder.yml`）与管理端同构，差异：`appId: com.codenest.user`、`productName: Code-Nest`、`win.executableName: CodeNest`、artifactName `Code-Nest-Setup-${version}.${ext}`。`electron/main/window.ts` 默认窗口 1400×900、最小 1200×800，`contextIsolation: true` / `nodeIntegration: false` / `sandbox: false`，dev 用 `ELECTRON_RENDERER_URL`、prod 用 `loadFile('../renderer/index.html')`，窗口尺寸持久化到 electron-store 的 `windowBounds`。

**约束与注意事项**：`tests/` 契约测试按源码文本断言（`route-slices`、`navigation-routes`、`no-debug-console` 等），重命名文件或新增 `console.log` 会破坏测试；`tests/e2e/mobile-navigation.spec.js` 依赖 `playwright.config.js`，仓库无 node_modules 时不可运行（未验证）。

## code-nest-api-contract 与 code-nest-design-system

**是共享包，不是独立部署产物**：两端 `package.json` 的 `dependencies` 都写了 `"@code-nest/api-contract": "file:../code-nest-api-contract"` 与 `"@code-nest/design-system": "file:../code-nest-design-system"`；两者自身都是 `"private": true`、`files: ["src"]`，无发布脚本、无 build 产物、无 dist，因此只能同仓本地链接使用。仓库根目录**没有** `package.json`/workspaces（已确认 `Test-Path F:\Code-Nest\package.json` 为 False）。

**api-contract（4 个文件）**：`src/index.js`（166 行，唯一实现）、`src/index.d.ts`（类型）、`tests/api-contract.test.js`（7 个 `node:test` 用例）、`package.json`（`exports` 提供 `types`/`import`，`scripts.test = node --test tests/*.test.js`）。导出 `ApiError`、`isApiResponse`、`isSuccessfulHttpStatus`、`unwrapApiResponse`、`classifyApiFailure`、`getApiErrorMessage`、`normalizeQueryRequestConfig`、`normalizeBodyRequestConfig`。核心语义：`unwrapApiResponse` 只在 `code === 200` 且 HTTP 2xx 时返回 `body.data`，否则抛出带 `kind/code/httpStatus` 的 `ApiError`；`classifyApiFailure` 把 HTTP 状态、业务码、`ECONNABORTED`/`ERR_NETWORK` 归一为 `authentication | authorization | validation | not-found | conflict | rate-limit | timeout | unavailable | server | business | unknown`。`normalizeQueryRequestConfig`/`normalizeBodyRequestConfig` 用白名单键判断入参到底是 axios 配置还是查询/参数体。使用方式：两端 `utils/request.js` 的响应拦截器调用 `unwrapApiResponse` / `classifyApiFailure`；两端 `utils/request-options.js` 做纯 re-export；`views/notification/index.vue` 与部分页面直接 import `getApiErrorMessage` 及 `ApiId`/`PageResult`/`NotificationRecord` 等类型。

**design-system（32 个文件）**：`src/index.ts` 导出 3 个组合式函数（`useTheme`、`useCnBreakpoints`、`useCnTable`）、16 个组件（app：CnPage/CnPageHeader/CnSection；data：CnDataTable/CnEmptyState/CnFilterForm/CnStatCard/CnStatusTag/CnToolbar；navigation：CnCommandPalette/CnSidebar/CnSidebarItem/CnTopNav；theme：CnThemeDrawer/CnThemeSwitch）、类型 `types/components.ts`+`types/theme.ts`，以及纯 CSS 资产 `tokens/{primitive,motion,semantic,component}.css`、`themes/{light,dark,professional-blue,growth,high-contrast}.css`、`plugins/element-plus.css`。`package.json` 的 `exports` 同时开放 `"."`（→ `./src/index.ts`）与 `"./*"`（→ `./src/*`，供按文件深引），并声明 `peerDependencies`：vue ^3.4.0、vue-router ^4.2.5、element-plus ^2.4.4、@element-plus/icons-vue ^2.3.1（版本必须与两端一致），**无任何 scripts**。使用方式有三种，均因 `preserveSymlinks: true` + `@/design-system` 别名而落在 `node_modules/@code-nest/design-system/src`：① `import { CnPage, ... } from '@/design-system'`（两端 views 大面积使用，管理端出现 167 处、用户端 154 处匹配）；② 深层引用 `@/design-system/composables/useTheme`、`@/design-system/types/theme`（两端 `main.js`、`stores/theme.ts`）；③ SCSS 侧 `@use '@/design-system/tokens/*.css'` 与 `@/design-system/themes/*.css`（两端 `styles/index.scss` 前 10 行逐条对应）。两端均保留一个 `views/design-system/Components.vue` 验收页，路由 `/design-system/components`（`requiresAuth: false`）。

## 共享与差异

**共享模式**：① 请求层——两端 `src/utils/request.js` 结构几乎逐行同构（`axios.create({ baseURL: '/api', timeout: 60000, Content-Type: application/json;charset=UTF-8 })`、NProgress 开关、`Bearer ${token}` 头、`unwrapApiResponse` + `classifyApiFailure`、`isHandlingTokenExpired` 防重复弹窗、`ElMessageBox.alert(message, '登录过期')` 后跳 `/login`、`get/post/put/patch/delete` 五方法封装）；② 路由分片 + 懒加载 + `meta.title`；③ `stores/theme.ts` 与 `main.js` 的 `initializeTheme()`；④ `electron/` 目录文件集合完全一致；⑤ 依赖版本区间基本一致。

**差异清单**：

| 维度 | 管理端 | 用户端 | 实现位置 |
| --- | --- | --- | --- |
| token 存储键 | `Cookies.get('token')` / `Cookies.set('token', …, {expires: cookieExpireDays})` | `Cookies.get('user_token')` + `localStorage.user_token` | `src/stores/user.js` 两侧 |
| 用户信息键 | `localStorage.userInfo` + `tokenExpireTime` | `localStorage.user_info` | 同上 |
| 登录判定 | `isLoggedIn = !!token` | `isLoggedIn = !!token && !!userInfo` | 同上 |
| 路由守卫 | 默认必须登录：`to.meta?.requiresAuth !== false` | 白名单式：`if (to.meta.requiresAuth)` | `src/router/index.js` 两侧 |
| 守卫重定向 | 带 `query.redirect = to.fullPath` | 不带 redirect | 同上 |
| Token 刷新 | `refreshToken()`、`checkAndRefreshToken()`、`isTokenExpiringSoon`（30 分钟） | 无任何刷新逻辑，仅注释说明依赖 Sa-Token 自动续签 | `stores/user.js` / `utils/request.js` |
| 登出 | `logout()` 先调 `/auth/logout` 再清本地，并广播 `authStateChange` 跨 tab 同步 | `logout()` 只清本地，无广播 | 同上 |
| 接口前缀 | `/auth/**` + 业务管理接口 | `/user/**` + 公共 `/oj`、`/community`、`/version` | `src/api/auth.js` 两侧 |
| 代理 | 无 `ws` | `ws: true` | `vite.config.js` |
| Element Plus 引入 | `main.js` 全量 + 全量 CSS | `unplugin-vue-components` 按需 + 单组件样式 | `main.js` / `vite.config.js` |
| 构建分块 | 含 `vendor-charts`（echarts/d3） | 含 `vendor-monaco` | `vite.config.js` |
| 桌面产物 | `Code-Nest 管理端` / `CodeNest-Admin` / `com.codenest.admin` | `Code-Nest` / `CodeNest` / `com.codenest.user` | `electron-builder.yml` |

**704 的特殊处理（两端一致）**：`api-contract.classifyErrorKind` 把 703/704 归为 `authorization`（既非 `authentication`），但两端 `handleApiError` 显式判断 `error.code === 704` 后调用 `handleLogout()` 强制登出；701/702 走 `authentication` 分支弹「登录过期」对话框。即 704（账号禁用）是"静默登出 + 提示文案"，703（权限不足）仅 `ElMessage.error` 提示不登出。

## 跨模块观察

**共性模式**：业务域分片路由 + 单一请求出口 + 全局错误分类 + 设计系统组件层，四层边界清楚；契约测试以源码文本为断言对象，属于"文档即测试"的低成本护栏。

**重复代码**：`request.js` 两端约 170 行近乎复制（仅注释与 store 取值不同）；`electron/` 五个文件两端同名同构（仅 productName/appId 等标识不同）；`views/design-system/Components.vue`、`styles/`、`utils/request-options.js`、`utils/markdown.js`、`components/MindMap.vue` 亦为双份。这些重复是"两套独立 Vite 工程"的直接结果，尚无 monorepo 抽象。

**潜在风险**：① `electron-builder.yml` 的 `output: release/${version}` 与 `release/manifest.json` 中 `admin/user` 采用 `dist/` 目录部署的两条产物流并行存在，同名 `release/` 目录下目前仅有 `manifest.json`，实际安装包未见于仓库，构建与发布路径是否冲突**未确认**；② 用户端 `out/` 只有 `main/index.js` 与 `preload/index.mjs`，renderer 未产出，`npm run build:electron` 未在本次会话执行；③ design-system 与 api-contract 是 `file:` 链接且 `private: true`，`preserveSymlinks: true` + `fs.allow` 是它们可用的前提，任何 CI 若改为 `npm ci --ignore-scripts` 之外的安装方式或删掉别名都会立刻断链；④ `api-contract` 已被 4 处非 request.js 页面直接引用（如两端 `views/notification/index.vue`），改动其导出会波及业务页；⑤ 用户端 `logout()` 不通知后端，服务端会话（Sa-Token）可能残留。

**与 `.agent/rules/always.md` 的不一致**：

| 项 | always.md | 实际 |
| --- | --- | --- |
| 业务码范围 | 只列 200/701/702/703/704 | `api-contract` 还处理 400/404/408/409/429/500/502/503/504、601/602/603/604/705/803，且测试断言 705 为 `business`（登录失败不触发过期流程） |
| 版本同步清单 | `VERSION`、根 `pom.xml`、两端 `package.json` | `release/manifest.json` 的 `projections.packageJson` 还包含 `code-nest-design-system/package.json`、`code-nest-api-contract/package.json`、`docs-site/package.json`；实际四个前端相关 package.json 均为 2.5.8，与之一致，但规则未覆盖两个共享包 |
| 共享包 | 未提及 | `code-nest-api-contract`、`code-nest-design-system` 是两端 `file:` 依赖，属项目事实，规则缺失 |
| npm 脚本 | 只提 `dev`/`dev2`/`build`/`lint` | 两端另有 `dev:electron`、`build:electron`、`test:contracts`、`preview`、`pack`、`dist`、`dist:win`；用户端另有 `dev2:electron`、`test:e2e` |
| 生成物禁用 | 禁止手改 `dist/`、`out/`、`target/`、`node_modules/` | 仓库内确实存在 `vue3-admin-front/out/` 与 `vue3-user-front/out/`（各含 main+preload 构建产物），本次未修改 |

**未确认项汇总**：① 各 `views/**` 页面内具体业务行为（未逐文件读取，清单仅依据路由与目录名）；② 是否存在可运行的 Electron 安装包/`dist/` 产物（`release/` 下仅 `manifest.json`）；③ 两端 `node_modules` 当前是否存在，`npm run lint` / `npm run build` 能否通过；④ `electron/preload/index.ts` 暴露的具体 API 白名单（未读）；⑤ `test:e2e`（Playwright）与 `playwright.config.js` 的实际可运行性；⑥ design-system 的 token 变量名与 `useTheme` 持久化键；⑦ 管理端 `layout/index.vue` 的侧边栏菜单数据是否完全来自路由 `meta`（仅见其导入 `CnSidebar`）；⑧ 除 `auth.js` 外其余 54 个 `src/api/*.js` 的端点常量是否与后端 `/admin/**`、`/user/**` 分区完全对齐（仅抽样两端 `auth.js`）。
