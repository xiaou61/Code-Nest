# 问题定位流程

这页用于处理“项目哪里不对，但还不知道该看哪一页”的情况。它不是错误码大全，而是一条排查路线：先缩小范围，再找证据，最后回到对应模块页处理。

如果你已经知道自己遇到的是哪类失败，例如 `701/703`、文件 `403`、WebSocket ACK 失败、OJ 卡 `pending`、AI 结构化输出不完整，也可以先看 [异常路径与失败态索引](/reference/failure-paths)。如果只是看到“页面不对、接口失败、线上变慢”，先按本页走。

## 先做三件事

遇到问题时先不要急着改代码，先收集最小证据。

| 顺序 | 动作 | 目的 |
| --- | --- | --- |
| 1 | 确认影响范围 | 是用户端、管理端、后端接口、AI、OJ、文件、WebSocket，还是部署层 |
| 2 | 找到第一次失败点 | 浏览器 Network、后端日志、管理端日志、数据库记录、监控曲线 |
| 3 | 对照最近变更 | 最近是否发版、改配置、改代理、改 SQL、换模型或重启依赖 |

一个好用的判断：如果浏览器 Network 没发请求，先看前端；请求发了但状态异常，先看接口和鉴权；接口返回成功但页面不对，先看数据口径和前端渲染；所有接口都慢，先看监控和外部依赖。

## 现象到入口

| 现象 | 第一入口 | 第二入口 |
| --- | --- | --- |
| 页面打不开或路由 404 | [前端路由索引](/reference/frontend-routes) | [前端应用](/architecture/frontend-apps) |
| 接口 401、701、702 | [鉴权与用户体系](/modules/auth) | [响应体与错误码](/reference/response-errors) |
| 管理端无权限或菜单异常 | [系统运营后台](/modules/system-ops) | [仪表盘与日志](/modules/dashboard-logs) |
| 接口 5xx 或后端异常 | [监控与观测](/operations/monitoring) | 对应模块页的“常见坑” |
| 页面数据为空但接口成功 | [API 路由索引](/reference/api-routes) | [数据表索引](/reference/database-tables) |
| 文件上传成功但访问失败 | [文件存储](/modules/file-storage) | [常见问题排查](/operations/troubleshooting) |
| 聊天连接或消息异常 | [WebSocket 协议](/reference/websocket) | [IM 聊天室](/modules/chat) |
| AI 返回结构不稳定 | [AI Runtime](/modules/ai-runtime) | [AI Schema 与治理](/reference/ai-schemas) |
| OJ 提交一直等待或失败 | [OJ 判题系统](/modules/oj) | [常见问题排查](/operations/troubleshooting) |
| 部署后跨域或代理异常 | [Docker 与服务部署](/operations/docker) | [本地开发](/guide/local-dev) |
| 不知道某个名词是什么 | [术语表](/reference/glossary) | [源码地图](/reference/source-map) |

## 前端问题怎么缩小范围

先看浏览器 DevTools：

| 检查点 | 怎么判断 |
| --- | --- |
| Console | 是否有 JS 报错、组件渲染报错、资源加载失败 |
| Network | 请求是否发出，URL 是否带 `/api`，状态码是什么 |
| Response | 后端是否返回统一响应体，`code` 和 `message` 是什么 |
| Preview | 数据结构是否符合页面预期 |
| Application | Token 是否存在，用户端和管理端 Token 是否混用 |

常见分叉：

1. 请求没有发出：查路由、组件生命周期、按钮事件、前端 API 封装。
2. 请求 URL 错：查 Vite 代理、环境变量、`/api` 上下文。
3. 请求 401/701/702：查登录域和 Token。
4. 请求 200 但页面空：查字段名、分页结构、空态文案和筛选条件。
5. 使用 `v-html` 的内容不显示：查 [前端渲染安全](/reference/frontend-rendering-security) 和净化后的 HTML。

## 后端问题怎么缩小范围

后端排查优先定位到“哪个 Controller、哪个 Service、哪张表”。

| 检查点 | 先看 |
| --- | --- |
| 接口前缀 | [API 路由索引](/reference/api-routes) |
| 模块目录 | [源码地图](/reference/source-map) |
| 统一错误 | [响应体与错误码](/reference/response-errors) |
| 核心表 | [数据表索引](/reference/database-tables) |
| 业务规则 | 对应模块页的核心流程和常见坑 |

建议顺序：

1. 用接口前缀定位 Controller。
2. 从 Controller 跳到 Service，不要只看前端字段。
3. 找事务边界、状态更新、权限判断和外部调用。
4. 查对应表的记录是否真实变化。
5. 如果涉及 Redis、文件、AI、go-judge，再查外部依赖状态。

## 鉴权问题怎么判断

鉴权问题最容易被误判成“接口坏了”。先确认三个问题：

| 问题 | 判断 |
| --- | --- |
| 是用户端还是管理端 | 用户端和管理端是两套登录域 |
| Token 是否正确 | 管理端 Token 不能调用用户端登录域，反过来也不行 |
| 接口是否需要放行 | 登录、验证码、公开查询可以放行，写操作通常不能放行 |

管理端登录失败先查 `sys_login_log`。用户端登录、注册、资料问题先查 [用户账户与个人中心](/modules/user-account)。

## 数据问题怎么判断

接口成功但数据“不符合预期”时，不要只改前端展示。先看数据来源。

| 问题 | 检查 |
| --- | --- |
| 列表缺数据 | 筛选条件、分页参数、状态字段、逻辑删除 |
| 详情缺字段 | VO 映射、关联查询、前端字段名 |
| 统计不准 | 主表更新、计数缓存、异步任务、Redis 计数 |
| 写入成功但列表不更新 | 缓存 TTL、写后是否清缓存、前端是否重新拉取 |
| 删除后还能看到 | 软删除字段、缓存、前端本地状态 |

如果模块里有“热度、排行、统计、未读数、积分余额”，要同时看主表和统计表，必要时看 Redis。

## 外部依赖问题怎么判断

这些能力依赖外部服务，排查时要先确认依赖可用：

| 能力 | 依赖 | 健康判断 |
| --- | --- | --- |
| AI/RAG | OpenAI Compatible API、RAG sidecar、Redis | 管理端 AI 配置、RAG health、回归用例 |
| OJ | go-judge、编译器、测试用例 | go-judge 地址、提交状态、编译/运行错误 |
| 文件 | 本地目录、对象存储、Nginx alias、Docker volume | 文件记录、物理文件、URL 访问 |
| 聊天 | WebSocket 代理、Redis 在线态 | ws-ticket、Upgrade、在线 key |
| 监控 | Prometheus、Grafana、Actuator | `/api/actuator/prometheus`、Prometheus target |

外部依赖不可用时，文档和验证记录里要写清楚“未验证原因”，不要把主流程构建通过等同于链路已验证。

## 记录排查结论

排查完以后，建议按这个格式沉淀：

| 字段 | 写法 |
| --- | --- |
| 现象 | 用户端聊天发送图片后页面显示失败 |
| 影响范围 | 用户端 `/chat`，文件访问 URL，聊天消息展示 |
| 根因 | 返回 URL 是 `/files/**`，部署只代理了 `/api/files/**` |
| 修复 | 统一文件访问前缀，更新 Nginx 和前端展示 |
| 验证 | 上传图片、消息发送、刷新页面后图片仍可访问 |
| 文档 | 更新文件存储、WebSocket、验证记录 |

适合沉淀的位置：

1. 模块页“常见坑”。
2. [常见问题排查](/operations/troubleshooting)。
3. [验证记录与已知问题](/manuals/verified-scenarios)。
4. 如果是发布级问题，同步 [版本历史](/modules/version-history)。

## 最小排查清单

| 检查项 | 预期 |
| --- | --- |
| 能定位端 | 用户端、管理端、后端、依赖、部署层至少能归一类 |
| 能定位请求 | 有具体 URL、状态码、响应体或 WebSocket 事件 |
| 能定位模块 | 能找到模块页、Controller、Service 和核心表 |
| 能定位数据 | 能说清数据是否落库、是否进缓存、是否被前端过滤 |
| 能定位依赖 | AI、RAG、go-judge、Redis、MySQL、文件目录状态明确 |
| 能沉淀结论 | 已验证项和未验证原因写清楚 |
