# Code-Nest 全量 API 测试报告

> 日期：2026-06-06  
> 测试对象：本地 Code-Nest 后端 `http://127.0.0.1:9999/api`  
> OpenAPI 来源：`output/api-test/openapi-live-local.json`  
> 测试工具：`output/api-test/tools/full_api_harness.py`  
> 最终运行目录：`output/api-test/run-live-local-full-audit-0606185757`

## 结论

- OpenAPI operation 数：`757`
- 最终失败数：`0`
- 最终结果：当前本地全量 757 个接口已完成回归，未发现剩余 HTTP 5xx、业务码 500 或传输超时失败。
- 补充结论：OpenAPI smoke 结果不等价于完整业务正确性；已开始按服务补充 correctness harness，对成功路径做字段/状态读回断言，对预期业务错误单独计数。

## 最终全量运行摘要

- Run ID：`0606185757`
- 开始时间：`2026-06-06T18:57:58+08:00`
- 结束时间：`2026-06-06T18:58:25+08:00`
- Base URL：`http://127.0.0.1:9999/api`
- 计划接口数：`757`
- 失败接口数：`0`

| 分类 | 数量 | 说明 |
| --- | ---: | --- |
| `PASS_SUCCESS` | 417 | HTTP/业务均成功 |
| `PASS_BUSINESS_ERROR` | 248 | 返回明确业务错误，非服务端 500 |
| `PASS_AUTH` | 57 | 认证/权限按预期拦截 |
| `PASS_VALIDATION` | 34 | 参数校验按预期拦截 |
| `PASS_HTTP_CLIENT_ERROR` | 1 | HTTP 4xx 客户端错误，非服务端故障 |

| HTTP 状态 | 数量 |
| --- | ---: |
| `200` | 733 |
| `400` | 23 |
| `403` | 1 |

| 鉴权模式 | 数量 |
| --- | ---: |
| `admin` | 304 |
| `user` | 299 |
| `none-safety` | 85 |
| `none` | 69 |

## Setup 状态

| 项 | 结果 |
| --- | --- |
| 管理员登录 | `PASS_SUCCESS`，已获取 token |
| 用户注册 | `PASS_SUCCESS` |
| 用户登录 | `PASS_SUCCESS`，已获取 token |
| 注册/登录验证码 | 均已获取 |
| 后端健康检查 | `UP`，数据库和 Redis 均为 `UP` |

测试过程中获取到的 token、验证码和密钥未写入报告。

## 本轮定位并修复的问题

- 敏感词管理多个接口默认 `Result.error(...)` 返回业务码 500，已改为参数错误、数据不存在或业务错误等语义化业务码。
- 知识图谱公开接口在图谱不存在或未发布时返回不稳定，已改为明确 `DATA_NOT_EXIST`。
- 摸鱼 Bug 随机接口把未登录异常吞进业务异常，已调整鉴权异常处理。
- 本地测试库缺少 `oj_problem_comment`、`oj_problem_comment_like`、`study_team_discussion_like`，已补表后完成回归。
- 测试 harness 无法稳定获取用户验证码，已补充本地日志读取路径。
- 热榜分类接口在全量测试中受外部热榜源超时影响，已改为优先返回本地枚举分类并写缓存；外部数据刷新仍保留独立降级。
- 学习小组匿名查询接口曾被全局 `/user/**` 鉴权拦截，导致邀请码查小组返回 `701 未提供Token`；已补充学习小组匿名查询白名单。
- 学习小组任务创建缺少 `creator_id` 写入，导致 leader 创建任务时数据库 `Field 'creator_id' doesn't have a default value`；已补齐实体赋值和 mapper insert 列。
- 后台 `/admin/**` 接口未进入统一管理端 Sa-Token 路由鉴权，主要依赖 `@RequireAdmin` AOP 兜底；已将 `/admin/**` 纳入 `StpAdminUtil.checkLogin()`。
- 管理员鉴权 AOP 曾吞掉业务方法抛出的 `IllegalArgumentException`，将真实参数错误包装成“权限验证失败”；已改为鉴权通过后再执行业务方法，让业务异常按原语义返回。
- 积分后台发放接口曾允许给不存在用户发积分并创建孤儿积分账户；已改为发放前校验用户存在，不存在时返回明确业务错误“用户不存在”。
- 抽奖风控链曾让黑名单用户立即抽奖先命中冷却限制，错误原因不明确；已调整为黑名单检查优先于积分、限流和冷却检查。
- 通知后台批量发送曾忽略请求 `type`，固定写入 `PERSONAL`，导致后台发送 `SYSTEM` 后用户按系统消息读回失败；已改为校验并透传消息类型。
- 通知后台批量发送曾允许给不存在用户发送消息；已改为逐个校验接收者用户存在，不存在时返回明确业务错误“用户不存在”。
- 通知后台全量列表曾无法按标题/时间过滤且对管理端全量查询语义不完整；已补齐 title/startTime/endTime 过滤并支持后台不传 userId 查询全部非删除消息。
- 通知后台删除公告曾复用用户删除条件 `receiver_id = userId`，公告 `receiver_id IS NULL` 时无法删除；已新增管理员按消息 ID 软删除，并同步清理该通知的已读记录。
- 博客配置读取曾因 `socialLinks` 从 JSON 字符串复制到 Map 时触发转换异常；已改为手动组装 DTO 并完成配置读回验证。
- 博客公开按分类列表曾复用用户文章列表查询，因 `user_id` 过滤导致公开分类页漏文章；已改为独立公开分类分页查询。
- 博客下架文章详情曾对非作者可见；已补齐草稿、下架、删除状态的非作者访问拦截。
- 博客删除文章曾只回滚博客文章总数，没有回滚分类文章数；已在用户删除和管理员删除路径同步扣减分类文章数。
- 摸鱼每日内容状态更新曾复用全字段更新 SQL，只传 `id/status` 时会把必填字段更新为 `null` 并导致“更新状态失败”；已新增专用状态更新 SQL，仅更新 `status/update_time`。
- 摸鱼程序员日历事件状态更新存在同类全字段更新风险；已同步新增专用状态更新 SQL，避免启停事件时污染业务字段。
- 普通用户 `/user/{userId}`、`/user/{userId}` 更新和 `/user/{userId}/password` 曾允许通过路径 ID 读取、更新或修改其他用户资料/密码；已补充当前登录用户与路径用户一致性校验，并让密码修改后当前 token 失效。
- 敏感词检测曾忽略词条自身 `level/action` 配置，只按命中数量和策略表计算风险与动作，导致后台配置高风险/拒绝词后检测仍可能允许通过；已在词库刷新时同步启用词元数据，并按命中词条最高风险等级和最严格动作计算检测结果。

## 关键验证记录

| 验证项 | 结果 |
| --- | --- |
| `mvn -pl xiaou-application -am -DskipTests package` | 通过，生成可运行 fat jar |
| Redis | `PONG` |
| `/api/actuator/health` | `UP` |
| 原始 17 个失败接口定向回归 | `output/api-test/run-live-local-targeted-0606180255`，0 失败 |
| 团队讨论缺表失败定向回归 | `output/api-test/run-live-local-targeted-team-0606180552`，0 失败 |
| 热榜分类最终定向回归 | `output/api-test/run-live-local-targeted-hot-topic-fixed-0606185144`，0 失败 |
| 最终 757 全量回归 | `output/api-test/run-live-local-full-audit-0606185757`，0 失败 |
| 博客服务级正确性回归 | `output/api-test/run-service-blog-correctness-0607131300`，54 步，0 失败，`PASS=46`，`PASS_EXPECTED_ERROR=8` |
| 学习小组服务级正确性回归 | `output/api-test/run-service-team-correctness-0607141700`，64 步，0 失败，`PASS=63`，`PASS_EXPECTED_ERROR=1` |
| 后台 AI 配置服务级正确性回归 | `output/api-test/run-service-ai-config-correctness-0607131500`，17 步，0 失败，`PASS=12`，`PASS_EXPECTED_ERROR=5` |
| 学习资产服务级正确性回归 | `output/api-test/run-service-learning-asset-correctness-0607131600`，27 步，0 失败，`PASS=24`，`PASS_EXPECTED_ERROR=3` |
| 积分与抽奖服务级正确性回归 | `output/api-test/run-service-points-lottery-correctness-0607141500`，45 步，0 失败，`PASS=38`，`PASS_EXPECTED_ERROR=7` |
| 通知服务级正确性回归 | `output/api-test/run-service-notification-correctness-0607141600`，48 步，0 失败，`PASS=42`，`PASS_EXPECTED_ERROR=6` |
| 摸鱼工具服务级正确性回归 | `output/api-test/run-service-moyu-correctness-0607144100`，69 步，0 失败，`PASS=59`，`PASS_EXPECTED_ERROR=10` |
| 用户/Auth/Admin User 服务级正确性回归 | `output/api-test/run-service-user-correctness-0607210855`，57 步，0 失败，`PASS=37`，`PASS_EXPECTED_ERROR=20` |
| 敏感词服务级正确性回归 | `output/api-test/run-service-sensitive-correctness-0607212337`，65 步，0 失败，`PASS=55`，`PASS_EXPECTED_ERROR=10` |

## 服务级正确性补充

| 服务/模块 | Harness | 验证重点 | 结果 |
| --- | --- | --- | --- |
| 博客用户端/管理端 | `output/api-test/tools/blog_correctness_harness.py` | 开通博客、配置更新读回、草稿/发布、公开列表/分类/标签/后台列表、置顶/下架/恢复/删除、分类/博客计数回滚、权限和参数错误 | 通过 |
| 学习小组用户端 | `output/api-test/tools/team_correctness_harness.py` | 注册/登录、创建/更新/读回小组、邀请码解析、任务创建/更新/启停/今日任务、申请审批、成员角色/禁言、打卡、讨论、排行、统计、清理 | 通过 |
| 管理后台 AI 配置 | `output/api-test/tools/ai_config_correctness_harness.py` | 管理鉴权、runtime 配置摘要脱敏、schema/regression 目录一致性、metrics 清空读回、RAG 健康降级、参数错误语义、AI 配置测试缺 key 的 `available=false` 结果 | 通过 |
| 学习资产用户端/管理端 | `output/api-test/tools/learning_asset_correctness_harness.py` | 用户注册/登录、积分打卡、开通博客、创建博客草稿、博客转学习资产、候选编辑/丢弃/确认、发布到闪卡和练习计划、知识节点进审核、管理端详情/驳回、用户端状态读回、统计读回、匿名/未确认发布/空驳回原因错误 | 通过 |
| 积分与抽奖用户端/管理端 | `output/api-test/tools/points_lottery_correctness_harness.py` | 用户积分初始化、每日打卡、重复打卡错误、积分明细/日历/统计读回、后台单发/批量发放、缺失用户错误、奖品配置、抽奖余额变化、抽奖记录/监控读回、冷却和黑名单风控错误 | 通过 |
| 通知用户端/管理端 | `output/api-test/tools/notification_correctness_harness.py` | 管理员批量发送个人/系统消息、用户端列表/详情/未读数/已读/删除读回、公告多用户已读隔离、后台列表和统计读回、模板创建/更新/重复 code 错误、空接收者和缺失用户错误 | 通过 |
| 摸鱼工具用户端/管理端 | `output/api-test/tools/moyu_correctness_harness.py` | Bug 商店创建/列表/详情/更新/随机/删除，每日内容创建/列表/详情/更新/启停/浏览/点赞/收藏/统计/删除，程序员日历事件创建/月份/日期/收藏/偏好/更新/启停/统计/删除，薪资配置/工作状态机，热榜分类与降级结构 | 通过 |
| 用户/Auth/Admin User | `output/api-test/tools/user_correctness_harness.py` | 管理员登录，验证码生成/校验/消费，用户注册、唯一性检查、重复和参数错误，用户名/邮箱登录，资料和 token 刷新，资料更新读回，密码修改、旧 token 失效、旧密码拒绝，普通用户跨用户读取/更新/改密拒绝，后台用户列表/统计/创建/详情/更新/禁用/启用/重置密码/批量删除/删除后登录拒绝 | 通过 |
| 敏感词用户端/管理端 | `output/api-test/tools/sensitive_correctness_harness.py` | 后台鉴权，敏感词新增/重复/详情/更新/缺失错误/删除和引擎刷新，公开单条/批量检测命中与不命中，词条高风险拒绝动作生效，白名单过滤/禁用读回，同音字/形似字新增更新读回和错误分支，策略列表/详情/更新/恢复/公开查询，统计/版本/来源安全读回和清理 | 通过 |

## 测试口径

- 每个 OpenAPI operation 至少生成一条 HTTP 结果记录。
- 高风险写操作使用安全路径测试认证、校验或不存在数据分支，避免破坏数据。
- `PASS_BUSINESS_ERROR` 表示接口返回明确业务错误，且不是业务码 500。
- `PASS_AUTH` 表示未授权或权限不足被正确拦截。
- `PASS_VALIDATION` 表示参数校验被正确拦截。
- 服务级 correctness harness 不把业务错误混作成功路径；成功路径必须断言业务码、关键字段和必要的状态读回。

## 残余风险

- 这是 OpenAPI 驱动的 HTTP full-coverage smoke 测试，不等价于完整业务场景 E2E。
- 高风险写接口没有执行真实破坏性批处理，只验证安全分支不 5xx/不超时。
- 外部热榜源当前不可达，系统已降级可用；外部源恢复后建议再做真实内容验收。
- 目前只完成博客、学习小组、后台 AI 配置、学习资产、积分与抽奖、通知、摸鱼工具、用户/Auth/Admin User、敏感词九个服务级 correctness harness；不能据此宣称所有服务业务正确。
- 后台 AI 配置 correctness 尚未把真实模型连通性写入报告，密钥未落盘；如需验证真实模型调用，应通过环境变量短时注入并确认结果文件只包含脱敏信息。
