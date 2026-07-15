# API 导航

API 文档按“调用端 + 业务域”分类。先确认认证域，再进入对应接口分组；需要完整路由、示例和错误语义时使用参考索引。

## 调用前先确认

| 项 | 用户端 | 管理端 |
| --- | --- | --- |
| 路径前缀 | `/api/user/**` | `/api/admin/**` |
| 登录工具 | `StpUserUtil` | `StpAdminUtil` |
| 主要调用方 | 用户端 Web / Electron | 管理端 Web / Electron |
| 权限重点 | 登录态、资源归属 | 登录态、角色与 `@RequireAdmin` |

认证细节见 [API 认证指南](/guide/api-auth)，统一响应和错误码见 [响应体与错误码](/reference/response-errors)。

## 用户端 API

| 分组 | 内容 |
| --- | --- |
| [账号与系统](/api/account-system) | 注册登录、用户资料、系统公共接口 |
| [学习成长](/api/learning-growth) | 题库、计划、小组、闪卡、模拟面试和学习资产 |
| [OJ 判题](/api/oj) | 题目、提交、判题、赛事和排行榜 |
| [内容社区](/api/content-community) | 社区、动态、博客、代码工坊和评论互动 |
| [平台能力](/api/platform) | 文件、通知、聊天、积分和简历 |
| [工具与运营](/api/tools-operations) | 开发工具、摸鱼、版本和公共运营数据 |

## 管理端 API

| 分组 | 内容 |
| --- | --- |
| [积分与抽奖](/api/admin-points-lottery) | 规则、活动、奖品、风控和统计 |
| [内容社区](/api/admin-content-community) | 社区、动态、博客和内容审核 |
| [平台能力](/api/admin-platform) | 用户、文件、通知、敏感词和系统运营 |
| [AI Runtime](/api/admin-ai-runtime) | 模型配置、Prompt、RAG、Schema 和回归治理 |

## 常用参考

- [API 路由索引](/reference/api-routes)：按 Controller 和路径查询。
- [API 调用示例](/reference/api-examples)：查看请求头、分页和错误处理。
- [权限注解与角色边界](/reference/permission-boundaries)：判断接口属于公开、用户态还是管理态。
- [WebSocket 协议](/reference/websocket)：查看聊天室握手、票据和消息类型。
