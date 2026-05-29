# 模块最小回归矩阵

这页专门回答一个很实际的问题：

**当你已经知道改的是哪个模块时，最低应该回归什么？**

它和几页相邻文档的分工不同：

- [全功能覆盖矩阵](/reference/feature-coverage)：回答“这个功能落在哪些入口、接口、表和文档上”。
- [测试与回归](/guide/testing-regression)：回答“按测试层级应该怎么选验证方式”。
- [发布前验证](/guide/release-verification)：回答“发版前按变更范围怎么组合验证”。
- 本页：回答“改完某个模块，最低别漏掉哪几项”。

## 怎么用这页

建议按下面顺序用：

1. 先确认这次改动主要落在哪个模块。
2. 直接找到对应行，把“最少回归组”逐项勾掉。
3. 至少补一条“失败态”验证，不要只测成功路径。
4. 再看“关键依赖”，确认哪些外部条件没起时不能写成“已验证”。
5. 最后按“常见文档同步点”把模块页、索引页和验证记录补齐。

如果一次改动横跨多个模块，不取最轻的一行，而是取这些模块的并集。

## 账号与平台基础

| 模块 | 最少回归组 | 至少一条失败态 | 关键依赖 | 常见文档同步点 |
| --- | --- | --- | --- | --- |
| [鉴权与用户体系](/modules/auth) | 用户端登录、管理端登录、鉴权接口、受保护页面回流 | Token 过期、权限不足、验证码错误、未登录访问受限页面 | Sa-Token、Redis、验证码能力 | 模块页、[前端路由索引](/reference/frontend-routes)、[API 路由索引](/reference/api-routes)、[响应体与错误码](/reference/response-errors) |
| [用户账户与个人中心](/modules/user-account) | 当前用户资料、资料编辑、头像上传、后台用户查询 | 昵称/资料校验失败、头像上传失败、禁用用户态 | 文件存储、用户表、登录态 | 模块页、[数据表索引](/reference/database-tables)、操作手册 |
| [系统运营后台](/modules/system-ops) | 后台登录、菜单可见、目标配置页打开、保存一项基础配置 | 无权限账号访问后台菜单、配置校验失败 | 管理端鉴权、系统配置、相关业务模块 | 模块页、[前端路由索引](/reference/frontend-routes)、[权限与安全边界](/guide/security-boundaries) |
| [仪表盘与日志](/modules/dashboard-logs) | 仪表盘打开、核心统计接口返回、登录日志/操作日志列表 | 空数据、筛选条件非法、日志详情缺字段 | 聚合查询、日志表、定时任务 | 模块页、[数据表索引](/reference/database-tables)、[问题定位流程](/operations/diagnosis-flow) |

## 学习成长

| 模块 | 最少回归组 | 至少一条失败态 | 关键依赖 | 常见文档同步点 |
| --- | --- | --- | --- | --- |
| [面试题库](/modules/interview) / [题库与成长闭环](/modules/interview-and-growth) | 题单列表、题目详情、学习记录写入、收藏或复习入口 | 题目不存在、未登录学习、掌握度更新异常 | 用户登录态、题库表、成长记录 | 两个模块页、[数据表索引](/reference/database-tables)、[核心链路教程](/manuals/core-workflows) |
| [模拟面试与求职作战台](/modules/mock-interview-job-battle) | 会话创建、问答推进、报告或阶段状态回写、后台会话查看 | AI 输出异常、阶段推进失败、无配置方向 | AI Runtime、Redis、会话表 | 模块页、[AI Schema 与治理](/reference/ai-schemas)、[验证记录与已知问题](/manuals/verified-scenarios) |
| [学习资产](/modules/learning-assets) | 资产记录生成、候选状态流转、审核或发布、目标模块回流 | 重复发布、审核拒绝、目标模块回流失败 | 学习资产表、审核流、目标业务模块 | 模块页、[数据表索引](/reference/database-tables)、操作手册、验证记录 |
| [闪卡](/modules/flashcard) | 卡组列表、学习一轮、学习统计、后台或公开读取 | 无权限卡组访问、学习记录写入失败 | 卡组表、学习算法、登录态 | 模块页、[前端路由索引](/reference/frontend-routes)、[数据表索引](/reference/database-tables) |
| [计划与学习小组](/modules/plan-team) | 计划创建、打卡、任务或讨论、小组列表回流 | 重复打卡、非法状态推进、无成员权限 | 计划表、小组表、提醒任务 | 模块页、[数据表索引](/reference/database-tables)、操作手册 |
| [知识图谱](/modules/knowledge) | 图谱列表、图谱详情、节点编辑或排序、发布回流 | 节点树异常、未发布图谱访问、排序冲突 | 图谱表、节点树构建、后台权限 | 模块页、[前端路由索引](/reference/frontend-routes)、[数据表索引](/reference/database-tables) |
| [SQL 优化工作台](/modules/sql-optimizer) | 输入 SQL、生成分析结果、历史记录或案例读取 | AI 结构化输出不完整、非法 SQL、分析超时 | AI Runtime、历史记录表、模型配置 | 模块页、[AI Schema 与治理](/reference/ai-schemas)、验证记录 |

## OJ 与高风险判题链路

| 模块 | 最少回归组 | 至少一条失败态 | 关键依赖 | 常见文档同步点 |
| --- | --- | --- | --- | --- |
| [OJ 判题系统](/modules/oj) | 题面展示、运行或自测、正式提交、提交详情、赛事或排行至少一项 | 编译错误、测试用例失败、赛事时间不合法、go-judge 不可用 | go-judge、编译器镜像、OJ 表、登录态 | 模块页、[数据表索引](/reference/database-tables)、[核心链路教程](/manuals/core-workflows)、验证记录 |

## 内容与创作

| 模块 | 最少回归组 | 至少一条失败态 | 关键依赖 | 常见文档同步点 |
| --- | --- | --- | --- | --- |
| [社区帖子](/modules/community) | 列表、详情、发帖、评论或点赞、后台可见 | 敏感词拦截、未登录发帖、被删除内容访问 | 敏感词、登录态、社区表 | 模块页、[前端渲染安全](/reference/frontend-rendering-security)、操作手册 |
| [动态广场](/modules/moments) | 动态列表、发布、互动计数、后台统计或评论查看 | 发布频率限制、敏感词失败、空态列表 | 敏感词、Redis 浏览去重、动态表 | 模块页、[数据表索引](/reference/database-tables)、验证记录 |
| [博客](/modules/blog) | 博客列表、详情、编辑器保存、发布、后台文章管理 | 草稿发布失败、分类标签异常、未开通博客访问 | 博客表、分类标签、登录态 | 模块页、[前端渲染安全](/reference/frontend-rendering-security)、操作手册 |
| [代码工坊](/modules/codepen) | 作品列表、编辑器保存、发布、作品详情 | 私有作品越权访问、模板读取失败、付费 Fork 失败 | 作品表、模板表、登录态 | 模块页、[前端路由索引](/reference/frontend-routes)、验证记录 |
| [简历系统](/modules/resume) | 模板列表、简历编辑、预览或导出、后台模板管理 | 模板缺字段、导出失败、分享统计异常 | 文件导出、模板表、登录态 | 模块页、[数据表索引](/reference/database-tables)、操作手册 |

## 平台能力

| 模块 | 最少回归组 | 至少一条失败态 | 关键依赖 | 常见文档同步点 |
| --- | --- | --- | --- | --- |
| [文件存储](/modules/file-storage) | 上传、返回 URL 访问、业务页面回显、后台文件列表 | 文件类型非法、大小超限、访问 URL 失效 | 本地或对象存储、文件表、鉴权 | 模块页、[数据表索引](/reference/database-tables)、操作手册 |
| [通知中心](/modules/notification) | 未读数、通知列表、详情、已读动作 | 公告/个人通知混读、重复已读、越权读取 | 通知表、用户态、发送链路 | 模块页、[API 路由索引](/reference/api-routes)、验证记录 |
| [IM 聊天室](/modules/chat) | 获取 ws-ticket、建立 WebSocket、发送消息、ACK、历史消息 | 鉴权失败、禁言、断线重连失败、发送失败回执 | WebSocket、Redis、聊天表、登录态 | 模块页、[WebSocket 协议](/reference/websocket)、操作手册、验证记录 |
| [积分与抽奖](/modules/points) | 积分余额、积分流水、抽奖、后台发放或奖品管理 | 库存不足、重复领取、风控限制 | 积分表、抽奖库存、风控规则 | 模块页、[数据表索引](/reference/database-tables)、验证记录 |
| [敏感词风控](/modules/sensitive) | 词库查询、命中检测、后台新增或导入、业务调用结果 | 白名单误杀、导入预览失败、未命中该命中的词 | AC 自动机、词库表、业务接入方 | 模块页、[前端渲染安全](/reference/frontend-rendering-security)、验证记录 |
| [AI Runtime](/modules/ai-runtime) | Catalog 读取、单场景调试、回归执行、治理页打开 | Schema 不匹配、Prompt 变量缺失、RAG 空召回、模型配置错误 | AI 模型、Redis、回归样例、RAG sidecar | 模块页、[AI Schema 与治理](/reference/ai-schemas)、[测试与回归](/guide/testing-regression)、验证记录 |

## 轻工具与版本

| 模块 | 最少回归组 | 至少一条失败态 | 关键依赖 | 常见文档同步点 |
| --- | --- | --- | --- | --- |
| [开发者工具](/modules/dev-tools) | 至少一项核心工具输入输出、复制或下载动作、路由打开 | 非法输入、空输入、超长输入 | 前端本地逻辑、浏览器 API | 模块页、[前端路由索引](/reference/frontend-routes)、操作手册 |
| [摸鱼工具](/modules/moyu) | 至少一个工具页、一个数据列表或计算器、后台配置或统计 | 空数据、缓存过期、未登录限制或非法输入 | Redis、工具表、定时任务 | 模块页、[数据表索引](/reference/database-tables)、验证记录 |
| [版本历史](/modules/version-history) | 版本列表、详情、后台新增或编辑、状态流转 | 重复版本号、未发布版本公开可见、排序错误 | 版本表、后台权限 | 模块页、[数据表索引](/reference/database-tables)、[v2.2.1 文档计划](/roadmap/v2.2.1-docs-plan)、[按 Git Log 重写版本更新记录](/guide/git-log-release-notes) |

## 快速断点设置

按模块快速设置断点的参考位置：

| 模块 | Java 断点推荐 | 前端断点推荐 |
| --- | --- | --- |
| 鉴权与用户体系 | \`AuthController.login()\`、\`AdminAuthAspect.checkLogin()\` | \`stores/user.js login()\` |
| 用户账户 | \`UserController.getUserInfo()\` | \`views/profile/\` 组件 mounted |
| 系统运营后台 | \`AuthController.adminLogin()\` | \`layout/\` 侧边栏渲染 |
| 面试题库 | \`InterviewUserController.list()\` | \`views/interview/\` |
| 模拟面试与求职 | \`MockInterviewUserController.createSession()\` | \`views/mock-interview/\` |
| OJ 判题 | \`OjUserController.submit()\`、\`OjJudgeServiceImpl.judge()\` | \`views/oj/\` |
| 社区帖子 | \`CommunityUserController.createPost()\` | \`views/community/\` |
| IM 聊天 | \`ChatUserController.getWsTicket()\` | \`views/chat/\` WebSocket 连接 |
| 积分与抽奖 | \`LotteryServiceImpl.draw()\` | \`views/lottery/\` |
| 敏感词风控 | \`SensitiveCheckService.check()\` | 无前端断点（后端拦截） |
| AI Runtime | \`AiInterviewServiceImpl.evaluate()\` | \`views/mock-interview/\` |

> **提示**：后端断点在 IntelliJ IDEA 中设置。前端断点在浏览器 DevTools Sources 面板中设置，或使用 \`debugger\` 语句。

## 回归执行脚本参考

### 后端快速接口验证

```bash
# 检查后端健康
curl ${BACKEND_HOST}/api/actuator/health

# 检查认证
curl -X POST ${BACKEND_HOST}/api/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# 检查面试题库列表
curl ${BACKEND_HOST}/api/user/interview/question-sets/list \
  -H "Authorization: Bearer YOUR_TOKEN"

# 检查社区帖子列表
curl ${BACKEND_HOST}/api/user/community/posts/list \
  -H "Authorization: Bearer YOUR_TOKEN"
```

> `BACKEND_HOST` 默认为 `http://localhost:9999`，可在 `application.yml` 中修改。

### 前端快速页面验证

```bash
# 用户端首页加载（默认 http://localhost:3001）
curl ${USER_FRONT_HOST}/

# 管理端首页加载（默认 http://localhost:3000）
curl ${ADMIN_FRONT_HOST}/

# 检查 Vite 代理连通性
curl ${USER_FRONT_HOST}/api/actuator/health
curl ${ADMIN_FRONT_HOST}/api/actuator/health
```

> `USER_FRONT_HOST` 默认 `http://localhost:3001`，`ADMIN_FRONT_HOST` 默认 `http://localhost:3000`。

## 最后一个判断原则

如果你改完后只能说“我 build 过了”，大多数时候还不够。

更可靠的口径通常至少要能说清：

```text
受影响模块：
最少回归组：
已跑成功路径：
已跑失败态：
未验证项：
未验证原因：
```

这样别人接你的结论时，知道哪些是真的验证过，哪些只是这次还没覆盖。


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [测试与回归](/guide/testing-regression) | 测试策略 |
| [发布前验证](/guide/release-verification) | 发布检查清单 |
| [全功能覆盖矩阵](/reference/feature-coverage) | 功能覆盖 |
| [模块状态机与生命周期索引](/reference/module-state-machines) | 状态管理 |
