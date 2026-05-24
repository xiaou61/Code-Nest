# 计划与学习小组

计划模块解决“我每天要做什么”，学习小组解决“一群人如何一起坚持”。这两个模块都围绕长期成长展开，但边界不同：个人计划强调自我执行和提醒，小组强调成员、任务、打卡、讨论、排行和统计。

## 功能入口

| 功能 | 用户端入口 | 后端模块 |
| --- | --- | --- |
| 个人计划 | `/plan` | `xiaou-plan` |
| 成长自动驾驶 | `/growth-autopilot`，当前重定向到 `/learning-cockpit` | `xiaou-plan`、`xiaou-ai` |
| 学习小组广场 | `/team` | `xiaou-team` |
| 创建小组 | `/team/create` | `xiaou-team` |
| 我的小组 | `/team/my` | `xiaou-team` |
| 小组详情 | `/team/:id` | `xiaou-team` |
| 编辑小组 | `/team/:id/edit` | `xiaou-team` |

## 推荐学习顺序

计划与学习小组看起来都和“坚持学习”有关，但读源码时要先分清边界：计划是个人任务，小组是多人协作。两条线分开看，最后再看成长自动驾驶如何把它们串进学习闭环。

1. 先看个人计划，理解计划状态、重复规则、今日任务和打卡。
2. 再看计划提醒，知道定时任务怎样生成提醒并通过通知中心发送。
3. 接着看成长自动驾驶，理解一周目标、任务状态和跨模块模板。
4. 然后看学习小组生命周期，从创建、加入、申请审核到角色权限。
5. 最后看小组任务、打卡、讨论和排行，理解多人学习数据如何沉淀。

推荐用两个账号验证小组功能：一个账号创建小组并担任组长，另一个账号走公开加入或申请加入，这样权限边界会比单账号测试清楚很多。

## 源码地图

| 层级 | 文件或目录 |
| --- | --- |
| 计划前端 | `vue3-user-front/src/views/plan/` |
| 计划 API | `vue3-user-front/src/api/plan.js` |
| 小组前端 | `vue3-user-front/src/views/team/` |
| 小组 API | `vue3-user-front/src/api/team.js` |
| 计划 Controller | `xiaou-plan/src/main/java/com/xiaou/plan/controller/user/` |
| 计划 Service | `xiaou-plan/src/main/java/com/xiaou/plan/service/impl/` |
| 小组 Controller | `xiaou-team/src/main/java/com/xiaou/team/controller/user/UserTeamController.java` |
| 小组 Service | `xiaou-team/src/main/java/com/xiaou/team/service/impl/` |

## 个人计划

接口域 `/user/plan`：

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/user/plan/create` | POST | 创建计划 |
| `/user/plan/update/{planId}` | PUT | 更新计划 |
| `/user/plan/{planId}` | DELETE | 删除计划 |
| `/user/plan/{planId}` | GET | 计划详情 |
| `/user/plan/list` | POST | 分页计划列表 |
| `/user/plan/{planId}/pause` | PUT | 暂停计划 |
| `/user/plan/{planId}/resume` | PUT | 恢复计划 |
| `/user/plan/today-tasks` | GET | 今日任务 |
| `/user/plan/checkin` | POST | 打卡 |
| `/user/plan/{planId}/checkin/list` | GET | 打卡记录 |
| `/user/plan/stats/overview` | GET | 统计概览 |

### PlanCreateRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `planName` | String | @NotBlank, @Size(max=100) | 计划名称 |
| `planDesc` | String | @Size(max=500) | 计划描述 |
| `planType` | Integer | @NotNull, @Min(1), @Max(5) | 计划类型：1-刷题 2-学习 3-阅读 4-运动 5-自定义 |
| `targetType` | Integer | @Min(1), @Max(3) | 目标类型：1-数量 2-时长 3-次数 |
| `targetValue` | Integer | @Min(1) | 目标值 |
| `targetUnit` | String | @Size(max=20) | 目标单位（道/小时/次） |
| `startDate` | LocalDate | @NotNull | 开始日期，格式 yyyy-MM-dd |
| `endDate` | LocalDate | — | 结束日期，NULL 表示长期 |
| `dailyStartTime` | LocalTime | — | 每日开始时间，格式 HH:mm |
| `dailyEndTime` | LocalTime | — | 每日截止时间，格式 HH:mm |
| `repeatType` | Integer | @Min(1), @Max(4) | 重复类型：1-每日 2-工作日 3-周末 4-自定义 |
| `repeatDays` | String | @Pattern(自定义重复日正则) | 自定义重复日，1-7 表示周一到周日 |
| `remindBefore` | Integer | @Min(0) | 提前提醒分钟数 |
| `remindDeadline` | Integer | @Min(0) | 截止提醒分钟数 |
| `remindEnabled` | Integer | @Min(0), @Max(1) | 是否启用提醒：0-否 1-是 |

更新计划复用 `PlanCreateRequest`，路径为 `PUT /user/plan/update/{planId}`。

### PlanListRequest 字段

| 字段 | 类型 | 校验 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `userId` | Long | @Positive | — | 用户 ID（内部自动填充，前端不需传） |
| `status` | Integer | @Min(0), @Max(4) | — | 按状态筛选：0-已删除 1-进行中 2-已暂停 3-已完成 4-已过期 |
| `planType` | Integer | @Min(1), @Max(5) | — | 按类型筛选 |
| `keyword` | String | @Size(max=100) | — | 搜索关键字 |
| `pageNum` | Integer | @Min(1) | 1 | 当前页码 |
| `pageSize` | Integer | @Min(1) | 10 | 每页数量 |

### PlanCheckinRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `planId` | Long | @NotNull, @Positive | 计划 ID |
| `completeValue` | Integer | @NotNull, @Min(1) | 完成数量（前端可用别名 `actualValue`） |
| `completeContent` | String | @Size(max=500) | 完成内容描述 |
| `remark` | String | @Size(max=500) | 心得备注 |

### 计划状态

| 值 | 枚举 | 含义 |
| --- | --- | --- |
| `0` | `DELETED` | 已删除 |
| `1` | `ACTIVE` | 进行中 |
| `2` | `PAUSED` | 已暂停 |
| `3` | `COMPLETED` | 已完成 |
| `4` | `EXPIRED` | 已过期 |

### 计划类型

| 值 | 枚举 | 含义 |
| --- | --- | --- |
| `1` | `CODING` | 刷题计划 |
| `2` | `STUDY` | 学习计划 |
| `3` | `READING` | 阅读计划 |
| `4` | `EXERCISE` | 运动计划 |
| `5` | `CUSTOM` | 自定义 |

### 重复规则

| 值 | 枚举 | 含义 |
| --- | --- | --- |
| `1` | `DAILY` | 每日 |
| `2` | `WORKDAY` | 工作日 |
| `3` | `WEEKEND` | 周末 |
| `4` | `CUSTOM` | 自定义星期列表 |

自定义重复日使用 `repeat_days` 保存，例如 `1,2,3,4,5` 表示周一到周五。

## 计划打卡流程

1. 用户创建计划，系统补默认值：目标类型默认数量、目标值默认 1、单位默认“次”、提醒提前 30 分钟、截止提醒 10 分钟。
2. 今日任务接口根据计划状态、日期范围和重复规则筛出今天需要做的计划。
3. 打卡时校验计划属于当前用户，并且状态是进行中。
4. 同一计划当天只允许打卡一次。
5. 根据重复规则计算新的连续打卡天数。
6. 插入 `plan_checkin_record`。
7. 更新 `user_plan.total_checkin_days`、`current_streak`、`max_streak`。
8. 返回本次打卡获得的积分数，目前服务里固定为 10。

容易忽略的点：`PlanServiceImpl` 当前把积分写进响应模型，但没有看到直接调用积分服务入账。若产品要求真实加积分，需要补业务联动并更新积分明细文档。

## 计划提醒

`PlanRemindScheduler` 有三个任务：

| 调度 | 说明 |
| --- | --- |
| 每天 00:00 | 为当天需要提醒的计划生成提醒任务 |
| 每分钟 | 扫描即将发送的提醒任务并发送通知 |
| 每周一 03:00 | 清理 7 天前的提醒任务 |

提醒类型：

| 值 | 含义 |
| --- | --- |
| `1` | 开始提醒 |
| `2` | 截止提醒 |

提醒会写入通知中心，`sourceModule = plan`，`sourceId = planId`。

## 成长自动驾驶

成长自动驾驶把 OJ、题库、闪卡、计划、模拟面试、积分等模块拆成一周任务包。

接口域 `/user/plan/autopilot`：

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/user/plan/autopilot/dashboard` | GET | 周计划仪表盘，可选 `weekStart` 参数 |
| `/user/plan/autopilot/generate` | POST | 生成周计划 |
| `/user/plan/autopilot/replan` | POST | 根据进度重排 |
| `/user/plan/autopilot/tasks/{taskId}/complete` | POST | 完成单个任务 |
| `/user/plan/autopilot/tasks/today/complete` | POST | 批量完成今日任务，可选 `weekStart` 参数 |
| `/user/plan/autopilot/tasks/{taskId}/postpone` | POST | 顺延任务 |

### GrowthAutopilotGenerateRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `targetRole` | String | @Size(max=50) | 目标岗位 |
| `weeklyHours` | Integer | @Min(1), @Max(40) | 每周投入时长（小时），默认 8 |
| `weekStart` | LocalDate | — | 周起始日期（周一），为空时使用当前周 |

周投入时长会被限制在 3 到 40 小时，默认 8 小时。任务状态：

| 状态 | 含义 |
| --- | --- |
| `todo` | 待完成 |
| `done` | 已完成 |
| `missed` | 已错过 |

事件类型：

| 类型 | 含义 |
| --- | --- |
| `generate` | 生成计划 |
| `replan` | 任务重排 |
| `complete` | 完成任务 |
| `complete_batch` | 批量完成 |
| `postpone` | 任务顺延 |

模块模板包括 `oj`、`interview`、`flashcard`、`plan`、`mock`、`points`。不同目标岗位会调整各模块权重，例如后端/算法更偏 OJ，前端更偏题库和闪卡。

## 学习小组

接口域 `/user/team` 覆盖小组生命周期、成员、任务、打卡、讨论、排行和统计。

### 小组生命周期接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/user/team/create` | POST | 创建小组 |
| `/user/team/{teamId}` | PUT | 更新小组 |
| `/user/team/{teamId}` | DELETE | 解散小组 |
| `/user/team/{teamId}` | GET | 小组详情（弱登录，未登录也可看） |
| `/user/team/list` | POST | 分页小组列表（弱登录） |
| `/user/team/my` | GET | 我的小组 |
| `/user/team/created` | GET | 我创建的小组 |
| `/user/team/recommend` | GET | 推荐小组（弱登录） |
| `/user/team/{teamId}/invite-code` | GET | 获取邀请码 |
| `/user/team/{teamId}/invite-code/refresh` | POST | 刷新邀请码 |
| `/user/team/by-code/{inviteCode}` | GET | 根据邀请码获取小组信息 |

### 成员管理接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/user/team/{teamId}/join` | POST | 申请加入小组 |
| `/user/team/join-by-code` | POST | 通过邀请码加入 |
| `/user/team/{teamId}/quit` | POST | 退出小组 |
| `/user/team/{teamId}/members` | GET | 成员列表 |
| `/user/team/{teamId}/applications` | GET | 申请列表 |
| `/user/team/applications/my` | GET | 我的申请记录 |
| `/user/team/{teamId}/application/{applicationId}/approve` | POST | 审批通过 |
| `/user/team/{teamId}/application/{applicationId}/reject` | POST | 审批拒绝，可选 `rejectReason` 参数 |
| `/user/team/application/{applicationId}/cancel` | POST | 取消申请 |
| `/user/team/{teamId}/member/{targetUserId}` | DELETE | 移除成员 |
| `/user/team/{teamId}/member/{targetUserId}/role` | PUT | 设置成员角色，`role` 参数 2-管理员 3-成员 |
| `/user/team/{teamId}/transfer` | PUT | 转让组长，`newLeaderId` 参数 |
| `/user/team/{teamId}/member/{targetUserId}/mute` | POST | 禁言成员，`minutes` 参数，最长 7 天 |
| `/user/team/{teamId}/member/{targetUserId}/mute` | DELETE | 解除禁言 |

### 任务管理接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/user/team/{teamId}/task` | POST | 创建打卡任务 |
| `/user/team/task/{taskId}` | PUT | 更新任务 |
| `/user/team/task/{taskId}` | DELETE | 删除任务 |
| `/user/team/task/{taskId}/status` | PUT | 启用/禁用任务，`status` 参数 0 或 1 |
| `/user/team/task/{taskId}` | GET | 任务详情（弱登录） |
| `/user/team/{teamId}/tasks` | GET | 小组任务列表，可选 `status` 筛选（弱登录） |
| `/user/team/{teamId}/tasks/today` | GET | 今日需要打卡的任务（弱登录） |

### 打卡接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/user/team/{teamId}/checkin` | POST | 打卡 |
| `/user/team/{teamId}/checkin/supplement` | POST | 补卡，`date` 参数格式 yyyy-MM-dd |
| `/user/team/checkin/{checkinId}` | DELETE | 删除打卡记录 |
| `/user/team/checkin/{checkinId}` | GET | 打卡详情（弱登录） |
| `/user/team/{teamId}/checkins` | GET | 打卡动态列表，支持 `taskId`、`page`、`pageSize` 参数（弱登录） |
| `/user/team/{teamId}/checkins/my` | GET | 我的打卡记录，支持 `startDate`、`endDate` 筛选 |
| `/user/team/{teamId}/checkin/calendar` | GET | 打卡日历数据，`year`、`month` 参数 |
| `/user/team/checkin/{checkinId}/like` | POST | 点赞打卡 |
| `/user/team/checkin/{checkinId}/like` | DELETE | 取消点赞 |
| `/user/team/{teamId}/checkin/streak` | GET | 连续打卡天数，可选 `taskId` |
| `/user/team/{teamId}/checkin/total` | GET | 总打卡天数 |

### 排行榜接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/user/team/{teamId}/rank/checkin` | GET | 打卡次数排行，`type` 参数 total/weekly/monthly，`limit` 参数 |
| `/user/team/{teamId}/rank/streak` | GET | 连续打卡排行，`limit` 参数 |
| `/user/team/{teamId}/rank/duration` | GET | 学习时长排行，`type` 和 `limit` 参数 |
| `/user/team/{teamId}/rank/contribution` | GET | 贡献值排行，`limit` 参数 |
| `/user/team/{teamId}/rank/my` | GET | 我的排名，`rankType` 参数 checkin/streak/duration/contribution |

### 讨论接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/user/team/{teamId}/discussion` | POST | 创建讨论 |
| `/user/team/discussion/{discussionId}` | PUT | 更新讨论 |
| `/user/team/discussion/{discussionId}` | DELETE | 删除讨论 |
| `/user/team/discussion/{discussionId}` | GET | 讨论详情（弱登录） |
| `/user/team/{teamId}/discussions` | GET | 讨论列表，支持 `category`、`keyword`、`page`、`pageSize`（弱登录） |
| `/user/team/discussion/{discussionId}/top` | PUT | 置顶/取消置顶，`isTop` 参数 0 或 1 |
| `/user/team/discussion/{discussionId}/essence` | PUT | 设为精华/取消精华，`isEssence` 参数 0 或 1 |
| `/user/team/discussion/{discussionId}/like` | POST | 点赞讨论 |
| `/user/team/discussion/{discussionId}/like` | DELETE | 取消点赞讨论 |

### 统计接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/user/team/{teamId}/stats` | GET | 小组统计概览（弱登录） |
| `/user/team/{teamId}/stats/weekly` | GET | 每周统计 |
| `/user/team/{teamId}/stats/monthly` | GET | 每月统计，`year`、`month` 参数 |
| `/user/team/{teamId}/stats/my` | GET | 个人统计 |

### TeamCreateRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `teamName` | String | @NotBlank, @Size(min=2, max=50) | 小组名称 |
| `teamDesc` | String | @Size(max=500) | 小组简介 |
| `teamAvatar` | String | @Size(max=255) | 小组头像 |
| `teamType` | Integer | @NotNull, @Min(1), @Max(3) | 类型：1-目标型 2-学习型 3-打卡型 |
| `tags` | String | @Size(max=100) | 标签，逗号分隔，最多 5 个 |
| `maxMembers` | Integer | @Min(2), @Max(50) | 最大成员数 |
| `joinType` | Integer | @Min(1), @Max(3) | 加入方式：1-公开 2-申请 3-邀请 |
| `goalTitle` | String | @Size(max=100) | 目标标题 |
| `goalDesc` | String | @Size(max=500) | 目标描述 |
| `goalStartDate` | LocalDate | — | 目标开始日期 |
| `goalEndDate` | LocalDate | — | 目标结束日期 |
| `dailyTarget` | Integer | @Min(1) | 每日目标量 |

更新小组复用 `TeamCreateRequest`，路径为 `PUT /user/team/{teamId}`。

### TeamListRequest 字段

| 字段 | 类型 | 校验 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `pageNum` | Integer | @Min(1) | 1 | 当前页码 |
| `pageSize` | Integer | @Min(1), @Max(100) | 10 | 每页条数 |
| `teamType` | Integer | @Min(1), @Max(3) | — | 按类型筛选 |
| `tag` | String | @Size(max=20) | — | 标签筛选 |
| `keyword` | String | @Size(max=50) | — | 关键字搜索 |
| `sortBy` | String | @Pattern(hot\|new\|active) | hot | 排序方式 |
| `userId` | Long | — | — | 内部使用 |
| `joinable` | Boolean | — | — | 只查询可加入的小组 |

### JoinRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `teamId` | Long | @Positive | 小组 ID |
| `applyReason` | String | @Size(max=200) | 申请理由 |
| `inviteCode` | String | @Size(max=32) | 邀请码（通过邀请码加入时使用） |

### CheckinRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `taskId` | Long | @NotNull, @Positive | 任务 ID |
| `completeValue` | Integer | @Min(1) | 完成数量 |
| `content` | String | @Size(max=1000) | 打卡内容 |
| `images` | List\<String\> | — | 图片列表，最多 9 张 |
| `duration` | Integer | @Min(0) | 学习时长（分钟） |
| `relatedLink` | String | @Size(max=255) | 相关代码/笔记链接 |

### TaskCreateRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `taskName` | String | @NotBlank, @Size(max=100) | 任务名称 |
| `taskDesc` | String | @Size(max=500) | 任务描述 |
| `taskType` | Integer | @NotNull, @Min(1), @Max(4) | 类型：1-刷题 2-学习时长 3-阅读 4-自定义 |
| `targetValue` | Integer | @Min(1) | 目标数量 |
| `targetUnit` | String | @Size(max=20) | 目标单位 |
| `repeatType` | Integer | @Min(1), @Max(3) | 重复：1-每日 2-工作日 3-自定义 |
| `repeatDays` | String | @Size(max=32) | 自定义重复日（如 1,2,3,4,5） |
| `requireContent` | Integer | @Min(0), @Max(1) | 是否必须附带内容 |
| `requireImage` | Integer | @Min(0), @Max(1) | 是否必须附带图片 |
| `startDate` | LocalDate | — | 开始日期 |
| `endDate` | LocalDate | — | 结束日期 |

### DiscussionCreateRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `category` | Integer | @Min(1), @Max(5) | 分类：1-公告 2-问答 3-笔记 4-经验 5-闲聊 |
| `title` | String | @NotBlank, @Size(max=100) | 标题 |
| `content` | String | @NotBlank, @Size(max=5000) | 内容 |
| `images` | List\<String\> | — | 图片列表 |
| `isTop` | Integer | @Min(0), @Max(1) | 是否置顶 |
| `isEssence` | Integer | @Min(0), @Max(1) | 是否精华 |

### 小组枚举

| 枚举 | 值 | 含义 |
| --- | --- | --- |
| `TeamType` | `1` | 目标型 |
| `TeamType` | `2` | 学习型 |
| `TeamType` | `3` | 打卡型 |
| `JoinType` | `1` | 公开加入 |
| `JoinType` | `2` | 申请加入 |
| `JoinType` | `3` | 邀请加入 |
| `TeamStatus` | `0` | 已解散 |
| `TeamStatus` | `1` | 正常 |
| `TeamStatus` | `2` | 已满员 |
| `MemberRole` | `1` | 组长 |
| `MemberRole` | `2` | 管理员 |
| `MemberRole` | `3` | 成员 |
| `MemberStatus` | `0` | 已退出 |
| `MemberStatus` | `1` | 正常 |
| `MemberStatus` | `2` | 禁言中 |
| `ApplicationStatus` | `0` | 待审核 |
| `ApplicationStatus` | `1` | 已通过 |
| `ApplicationStatus` | `2` | 已拒绝 |
| `ApplicationStatus` | `3` | 已取消 |

### 小组创建规则

1. 小组名称不能为空，长度 2 到 50 个字符。
2. 小组类型不能为空。
3. 每个用户最多创建 3 个小组。
4. 创建者自动成为组长。
5. 默认加入方式是申请加入。
6. 小组初始状态是正常，当前人数从 1 开始。

### 加入规则

| 加入方式 | 行为 |
| --- | --- |
| 公开加入 | 直接成为成员 |
| 申请加入 | 写入申请记录，等待组长或管理员审核 |
| 邀请加入 | 普通申请会被拒绝，需要邀请码加入 |

限制：

- 每个用户最多加入 20 个小组。
- 小组已解散或满员时不能加入。
- 已经是正常成员时不能重复加入。
- 同一小组存在待审核申请时不能重复申请。

## 小组权限

| 操作 | 权限 |
| --- | --- |
| 修改小组信息 | 组长 |
| 解散小组 | 组长，且成员不超过 10 人 |
| 获取邀请码 | 小组成员 |
| 刷新邀请码 | 组长或管理员 |
| 审核申请 | 组长或管理员 |
| 移除成员 | 组长或管理员，管理员不能移除管理员，不能移除组长 |
| 设置角色 | 组长，且不能修改自己 |
| 转让组长 | 当前组长 |
| 禁言/解除禁言 | 组长或管理员，不能禁言组长，管理员不能禁言管理员 |
| 退出小组 | 普通成员或管理员，组长必须先转让 |

禁言最长 7 天。

## 小组任务、打卡和排行

任务类型：

| 值 | 含义 |
| --- | --- |
| `1` | 刷题 |
| `2` | 学习时长 |
| `3` | 阅读 |
| `4` | 自定义 |

打卡规则：

1. 用户必须是小组正常成员。
2. 同一用户、同一任务、同一天不能重复打卡。
3. 任务可以要求填写内容或上传图片。
4. 补卡只能补今天到过去 7 天内的日期。
5. 打卡删除只允许本人操作，并且创建后不超过 24 小时。
6. 打卡成功会更新成员累计打卡、贡献等统计字段。

排行类型：

| 排行 | 口径 |
| --- | --- |
| 打卡榜 | 周、月或总打卡次数 |
| 连续榜 | 连续打卡天数 |
| 时长榜 | 周、月或总学习时长 |
| 贡献榜 | 成员贡献值 |

## 小组讨论

讨论分类：

| 值 | 含义 |
| --- | --- |
| `1` | 公告 |
| `2` | 问题求助 |
| `3` | 学习笔记 |
| `4` | 经验分享 |
| `5` | 闲聊灌水 |

讨论能力包括创建、更新、删除、详情、列表、置顶、精华、点赞和取消点赞。置顶和精华应该视为运营动作，建议只允许有管理权限的成员操作。

## 核心数据表

| 表 | 说明 |
| --- | --- |
| `user_plan` | 个人计划主表 |
| `plan_checkin_record` | 个人计划打卡记录 |
| `plan_remind_task` | 计划提醒任务 |
| `growth_autopilot_goal` | 成长自动驾驶周目标 |
| `growth_autopilot_task` | 周目标下的任务 |
| `growth_autopilot_event` | 生成、重排、完成、顺延事件 |
| `learning_cockpit_rank_snapshot` | 学习驾驶舱排名快照 |
| `study_team` | 小组主表 |
| `study_team_member` | 成员和角色 |
| `study_team_application` | 加入申请 |
| `study_team_task` | 小组任务 |
| `study_team_checkin` | 小组打卡 |
| `study_team_checkin_like` | 打卡点赞 |
| `study_team_discussion` | 小组讨论 |
| `study_team_daily_stats` | 小组每日统计 |

## 验证清单

- 创建个人计划后，今天任务能按重复规则出现。
- 同一计划同一天重复打卡会被拒绝。
- 暂停计划后不能打卡，恢复后可以继续。
- 计划提醒任务能在 00:00 生成，并在扫描任务中发送通知。
- 生成成长自动驾驶计划后，任务分布到本周日期。
- 顺延任务只能顺延 `todo` 状态任务。
- 创建小组后，创建者自动成为组长。
- 公开加入、申请加入、邀请码加入三种路径都能走通。
- 组长退出前必须转让组长。
- 补卡只能补 7 天内，删除打卡只能在 24 小时内。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 今日任务为空 | 重复规则或日期范围不包含今天 | 检查 `repeat_type`、`repeat_days`、`start_date`、`end_date` |
| 打卡提示今日不需要 | 自定义星期列表没有今天 | 使用 1 到 7 表示周一到周日 |
| 计划积分没有真实到账 | 当前计划服务只返回积分值 | 如需入账，补积分服务调用 |
| 小组不能解散 | 成员超过 10 人 | 先移除成员或走后台治理流程 |
| 邀请码加入失败 | 小组不存在、已满、已解散或邀请码无效 | 查 `study_team.invite_code` 和小组状态 |
| 排行数据不准 | 打卡统计字段未同步或历史数据缺失 | 对照 `study_team_checkin` 回填成员统计 |

---

# 学习小组深度拆解

> 以下内容基于 `xiaou-team` 全量源码（7 个 ServiceImpl、9 个 Domain、8 个枚举、7 个 Mapper XML、1 个 820 行 Controller）逐行拆解，覆盖状态机、权限矩阵、业务规则、数据流、异常路径和常见坑。

## 小组生命周期状态机

小组有三种状态，由 `TeamStatus` 枚举驱动：

```
  创建 ──→ NORMAL(1)
              │
              ├── 成员数 >= maxMembers ──→ FULL(2)
              │       │
              │       └── 成员数 < maxMembers ──→ NORMAL(1)
              │
              └── 组长解散 ──→ DISSOLVED(0)  [终态，不可逆]
```

### 状态转换触发点

| 当前状态 | 触发条件 | 目标状态 | 实现位置 |
| --- | --- | --- | --- |
| NORMAL | `updateMemberCount(teamId, +1)` 后 `current_members >= max_members` | FULL | `StudyTeamMapper.xml:131-138` 的 CASE 语句 |
| FULL | `updateMemberCount(teamId, -1)` 后 `current_members < max_members` | NORMAL | 同上 |
| NORMAL | 组长调用 `dissolveTeam` 且成员 <= 10 | DISSOLVED | `StudyTeamServiceImpl:191` |

**关键细节**：`updateMemberCount` 是一条 SQL 原子操作，同时更新 `current_members` 和 `status`，用 CASE 表达式判断是否满员。这意味着成员数和满员状态始终一致，不会出现"成员数已满但状态还是 NORMAL"的中间态。

**终态约束**：`selectById` 查询条件是 `WHERE id = #{id} AND status != 0`，已解散的小组对所有查询不可见，相当于软删除。

## 小组创建规则（源码级）

`StudyTeamServiceImpl.createTeam()` 的完整校验链：

1. `teamName` 非空且长度 2-50
2. `teamType` 非空
3. `teamMapper.countByCreatorId(userId) >= 3` → 抛出"每个用户最多创建3个小组"
4. 构建 `StudyTeam` 实体，默认值：
   - `maxMembers`：未传则默认 20
   - `joinType`：未传则默认 APPLY(2)
   - `inviteCode`：`RandomUtil.randomString(8).toUpperCase()` — 8 位大写字母
   - `currentMembers`：1（创建者自己）
   - `totalCheckins/totalDiscussions/activeDays`：全部 0
   - `status`：NORMAL(1)
5. 插入 `study_team`
6. 插入 `study_team_member`，创建者为 LEADER(1)，统计字段全部初始化为 0

**注意**：文档之前写"每个用户最多创建 5 个小组"，源码实际是 `MAX_CREATE_TEAMS = 3`。同样，"每个用户最多加入 20 个小组"应为 `MAX_JOIN_TEAMS = 10`。

## 成员角色与权限矩阵

### 角色层级

```
LEADER(1) > ADMIN(2) > MEMBER(3)
```

### 完整权限矩阵

| 操作 | LEADER | ADMIN | MEMBER | 非成员 | 源码位置 |
| --- | :---: | :---: | :---: | :---: | --- |
| 修改小组信息 | Y | - | - | - | `StudyTeamServiceImpl:138` |
| 解散小组 | Y(<=10人) | - | - | - | `StudyTeamServiceImpl:182` |
| 获取邀请码 | Y | Y | Y | - | `StudyTeamServiceImpl:320` |
| 刷新邀请码 | Y | Y | - | - | `StudyTeamServiceImpl:338` |
| 审核申请 | Y | Y | - | - | `TeamMemberServiceImpl:470` |
| 移除成员 | Y | Y(不能移除ADMIN/LEADER) | - | - | `TeamMemberServiceImpl:267-295` |
| 设置角色 | Y(不能改自己) | - | - | - | `TeamMemberServiceImpl:299-326` |
| 转让组长 | Y | - | - | - | `TeamMemberServiceImpl:330-349` |
| 禁言/解除禁言 | Y | Y(不能禁言ADMIN/LEADER) | - | - | `TeamMemberServiceImpl:353-405` |
| 退出小组 | - | Y | Y | - | `TeamMemberServiceImpl:143-159` |
| 创建/管理任务 | Y | Y | - | - | `TeamTaskServiceImpl:213-224` |
| 打卡 | Y | Y | Y | - | `TeamCheckinServiceImpl:59` |
| 创建讨论 | Y | Y | Y(非禁言) | - | `TeamDiscussionServiceImpl:52-69` |
| 发公告 | Y | Y | - | - | `TeamDiscussionServiceImpl:65-69` |
| 置顶/精华 | Y | Y | - | - | `TeamDiscussionServiceImpl:292-303` |
| 查看小组详情 | Y | Y | Y | Y(弱登录) | `UserTeamController:86` |

### 权限校验的两种模式

1. **角色码比较**：`member.getRole() != MemberRole.LEADER.getCode()` — 用于"只有组长可以"的场景
2. **成员资格 + 角色范围**：先检查 `member != null && member.getStatus() == 1`，再检查角色 — 用于"组长或管理员可以"的场景

### 管理员之间的互斥规则

- 管理员**不能**移除其他管理员
- 管理员**不能**禁言其他管理员
- 管理员**不能**设置角色（只有组长可以）
- 组长**不能**修改自己的角色

这些互斥规则防止管理员之间互相踢人/禁言的"宫斗"场景。

## 加入方式与审批流

### 三种 joinType 的完整流程

```
joinType = PUBLIC(1):
  用户 ──→ applyJoin() ──→ 检测到 PUBLIC ──→ addMember() 直接加入
  （跳过申请，无需审核）

joinType = APPLY(2):
  用户 ──→ applyJoin() ──→ 检测到 APPLY ──→ 创建 Application(status=PENDING)
       ──→ 组长/管理员 approveApplication() ──→ addMember() ──→ Application(status=APPROVED)
       或 rejectApplication() ──→ Application(status=REJECTED)

joinType = INVITE(3):
  用户 ──→ applyJoin() ──→ 检测到 INVITE ──→ 抛出"该小组仅限邀请加入"
  用户 ──→ joinByInviteCode(inviteCode) ──→ 查找小组 ──→ addMember() 直接加入
```

### 申请状态机

```
PENDING(0) ──→ APPROVED(1)  [审核通过]
    │
    ├──→ REJECTED(2)  [审核拒绝]
    │
    └──→ CANCELLED(3)  [申请人取消]
```

- `cancel` SQL 有条件：`WHERE id = #{id} AND user_id = #{userId} AND status = 0`，只有 PENDING 状态且本人才能取消
- 已处理的申请不能再次操作（`approveApplication` 检查 `status == PENDING`）

### addMember 内部逻辑

`addMember()` 是所有加入路径的最终汇聚点：

1. 检查小组存在且未解散
2. 检查未满员
3. 检查是否已是成员：
   - 已是 NORMAL 成员 → 抛出"您已经是该小组成员"
   - 已是 QUIT 成员 → **重新激活**（更新 role=MEMBER, status=NORMAL, joinTime=now），而不是插入新记录
   - 不存在 → 插入新成员记录
4. `teamMapper.updateMemberCount(teamId, 1)` — 原子更新成员数和满员状态

**重新加入的坑**：退出后再加入，角色会重置为 MEMBER(3)，之前的统计字段（totalCheckins 等）不会清零，因为 `update` 只改了 role/status/joinTime/joinReason。

## 打卡与补卡业务规则

### 打卡校验链（doCheckin 方法）

```
1. 任务存在且未删除         → taskMapper.selectById(taskId)
2. 任务属于该小组           → task.getTeamId().equals(teamId)
3. 用户是小组正常成员       → memberMapper.selectByTeamIdAndUserId(teamId, userId)
4. 该任务今日未打卡         → checkinMapper.selectUserTodayCheckin(userId, taskId, date)
5. 如 requireContent=1     → content 不能为空
6. 如 requireImage=1       → images 不能为空
```

### 补卡时间窗口

```java
// TeamCheckinServiceImpl:67-69
LocalDate today = LocalDate.now();
if (date.isAfter(today) || date.isBefore(today.minusDays(7))) {
    throw new BusinessException("只能补最近7天内的打卡");
}
```

- 补卡日期不能是未来
- 补卡日期不能超过 7 天前
- 补卡记录 `isSupplement = 1`，与正常打卡区分

### 打卡删除规则

```java
// TeamCheckinServiceImpl:169-171
if (Duration.between(checkin.getCreateTime(), LocalDateTime.now()).toHours() > 24) {
    throw new BusinessException("只能删除24小时内的打卡记录");
}
```

- 只能删除自己的打卡
- 只能删除 24 小时内的打卡
- 删除后级联更新：成员统计、小组打卡数、活跃天数、每日统计

### 连续打卡天数算法（递归 CTE）

`countStreakDays` 使用 MySQL 8 递归 CTE：

```sql
WITH RECURSIVE streak AS (
    -- 锚点：从 today 开始，如果有打卡则 day_count=1
    SELECT checkin_date, 1 as day_count
    FROM study_team_checkin
    WHERE user_id = #{userId} AND team_id = #{teamId}
      AND checkin_date = #{today} AND is_deleted = 0

    UNION ALL

    -- 递归：找前一天是否有打卡，有则 day_count+1
    SELECT c.checkin_date, s.day_count + 1
    FROM study_team_checkin c
    JOIN streak s ON c.checkin_date = DATE_SUB(s.checkin_date, INTERVAL 1 DAY)
    WHERE c.user_id = #{userId} AND c.team_id = #{teamId}
      AND c.is_deleted = 0
)
SELECT COALESCE(MAX(day_count), 0) FROM streak
```

**关键点**：
- 如果今天没打卡，递归锚点为空集，结果为 0
- 连续天数从今天往回数，断一天就停
- `taskId` 可选，传了则按任务算，不传则按小组整体算

### 最长连续打卡天数算法（窗口函数）

`countMaxStreakDays` 使用 `ROW_NUMBER() OVER` + 分组法：

```sql
SELECT COALESCE(MAX(streak_len), 0)
FROM (
    SELECT COUNT(1) AS streak_len
    FROM (
        SELECT DISTINCT checkin_date,
               DATE_SUB(checkin_date, INTERVAL ROW_NUMBER() OVER (ORDER BY checkin_date) DAY) AS grp
        FROM study_team_checkin
        WHERE user_id = #{userId} AND team_id = #{teamId} AND is_deleted = 0
    ) streak_groups
    GROUP BY grp
) streak_summary
```

原理：连续日期减去行号后得到相同的 `grp` 值，按 `grp` 分组计数即为每段连续天数，取最大值。

## 今日任务筛选逻辑

`selectTodayTasks` 的 SQL 条件：

```sql
WHERE team_id = #{teamId}
  AND is_deleted = 0
  AND status = 1                          -- 任务启用
  AND (start_date IS NULL OR start_date <= #{today})   -- 已开始
  AND (end_date IS NULL OR end_date >= #{today})       -- 未结束
  AND (
      repeat_type = 1                     -- 每日
      OR (repeat_type = 2 AND #{dayOfWeek} BETWEEN 1 AND 5)   -- 工作日
      OR (repeat_type = 3 AND FIND_IN_SET(#{dayOfWeek}, repeat_days) > 0)  -- 自定义
  )
```

- `dayOfWeek` 使用 Java 的 `DayOfWeek.getValue()`：MONDAY=1 ... SUNDAY=7
- `repeat_days` 存储格式如 `"1,2,3,4,5"`，用 `FIND_IN_SET` 匹配
- `start_date`/`end_date` 为 NULL 表示不限

## 排行榜口径与计算

### 四种排行维度

| 排行 | 排序字段 | 时间范围 | 数据来源 |
| --- | --- | --- | --- |
| 打卡榜 | `totalCheckins` | total/weekly/monthly | `countUserCheckinsByDateRange` 或 `member.totalCheckins` |
| 连续榜 | `streakDays` | 无（当前连续） | `countStreakDays` 递归 CTE |
| 时长榜 | `totalDuration` | total/weekly/monthly | `sumUserDurationByDateRange` |
| 贡献榜 | `contribution` | 无（累计） | `member.contributionPoints` |

### 周/月日期范围计算

```java
// TeamRankServiceImpl
case "weekly":
    startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    break;
case "monthly":
    startDate = today.withDayOfMonth(1);
    break;
default: // total
    startDate = null;  // 不限开始日期
    break;
```

- weekly：从本周一开始
- monthly：从本月1号开始
- total：不限开始日期，打卡榜直接用 `member.totalCheckins`，时长榜用 `sumUserDurationByDateRange(teamId, userIds, null, today)`

### 排名计算流程

1. `memberMapper.selectActiveByTeamId(teamId)` — 获取所有活跃成员
2. 按维度计算每个成员的值
3. 按值降序排序（`Comparator.reverseOrder()`）
4. `fillRankInfo()` — 批量查用户信息，填充排名序号、角色名、是否当前用户
5. 如果 `limit` 不为 null，截断到 limit 条

**贡献值来源**：`contributionPoints` 字段在 `StudyTeamMember` 上，但当前源码中没有看到增加贡献值的服务调用。打卡、讨论等操作都没有增加 `contributionPoints`。贡献榜可能全部为 0，需要后续补业务联动。

## 讨论与互动规则

### 讨论创建校验链

```
1. 用户是小组正常成员
2. 用户未被禁言（muteEndTime > now → 抛出"您已被禁言"）
3. 如 category=ANNOUNCEMENT(1) → 只有 LEADER/ADMIN 可以发
```

### 讨论删除权限

```java
boolean canDelete = discussion.getUserId().equals(userId);  // 作者可以删
if (!canDelete) {
    // LEADER/ADMIN 也可以删
    if (member != null && member.getStatus() == 1
        && (member.getRole() == LEADER || member.getRole() == ADMIN)) {
        canDelete = true;
    }
}
```

### 置顶与精华

- 置顶：`updateTopStatus` 设置 `is_pinned` 字段
- 精华：`updateEssenceStatus` 设置 `is_essence` 字段
- 讨论列表排序：`ORDER BY is_pinned DESC, create_time DESC` — 置顶的永远在最前

### 讨论点赞（非幂等）

```java
// TeamDiscussionServiceImpl:226-228
// 简化实现：直接增加点赞数
discussionMapper.updateLikeCount(discussionId, 1);
```

**重要坑**：讨论点赞没有去重表（不像打卡点赞有 `study_team_checkin_like`），同一个用户可以无限点赞，每次都 `+1`。这是简化实现，生产环境需要补幂等校验。

SQL 用 `GREATEST(like_count + #{delta}, 0)` 防止点赞数变成负数。

### 打卡点赞（幂等）

打卡点赞有 `study_team_checkin_like` 表做去重：

```java
Integer liked = checkinMapper.checkUserLiked(checkinId, userId);
if (liked != null && liked > 0) {
    throw new BusinessException("您已点赞过");
}
```

取消点赞时，先 `likeMapper.delete(checkinId, userId)`，只有 `deleted > 0` 才 `updateLikeCount(checkinId, -1)`。

## 每日统计刷新机制

`refreshDailyStats()` 在打卡和讨论操作后都会被调用：

```java
private void refreshDailyStats(Long teamId, LocalDate date) {
    // 1. 查活跃成员数
    Integer memberCount = memberMapper.countActiveMembers(teamId);
    // 2. 查当日打卡人数（DISTINCT user_id）
    Integer checkinCount = dailyStatsMapper.countCheckinsByDate(teamId, date);
    // 3. 计算打卡率 = checkinCount * 100.0 / memberCount
    // 4. 查当日讨论数
    Integer discussionCount = discussionMapper.countDiscussionsByDate(teamId, date);
    // 5. 保留之前的 newMemberCount
    // 6. INSERT ... ON DUPLICATE KEY UPDATE
}
```

- 使用 `ON DUPLICATE KEY UPDATE` 实现幂等写入
- 打卡率是实时计算的，不是增量更新
- `newMemberCount` 在加入成员时没有看到更新逻辑，可能始终为 0

## 活跃天数维护

```java
// 打卡时
StudyTeamDailyStats existingStats = dailyStatsMapper.selectByTeamIdAndDate(teamId, date);
if (existingStats == null || existingStats.getCheckinCount() == null
    || existingStats.getCheckinCount() == 0) {
    teamMapper.incrementActiveDays(teamId);  // 当天第一次有人打卡 → 活跃天数+1
}

// 删除打卡时
if (checkinMapper.selectCheckinUserIds(checkin.getTeamId(), null,
    checkin.getCheckinDate()).isEmpty()) {
    teamMapper.decrementActiveDays(checkin.getTeamId());  // 当天再无打卡 → 活跃天数-1
}
```

活跃天数的定义：**当天至少有一个人打卡**。注意这是小组级别的，不是个人级别的。

## 7 日打卡率计算

```java
// StudyTeamServiceImpl:373-386
LocalDate startDate = endDate.minusDays(6);  // 含今天共7天
List<TeamValueStat> stats = checkinMapper.countRecentCheckinUsersByTeamIds(
    Collections.singletonList(teamId), startDate, endDate);
int totalActiveUsers = stats.get(0).getValue();  // COUNT(DISTINCT CONCAT(checkin_date, ':', user_id))
return Math.min(100, totalActiveUsers * 100 / (memberCount * 7));
```

- 分母：`memberCount * 7`（7 天内每人每天应打卡的总人次）
- 分子：7 天内实际有打卡的人天次（不同日期+不同用户去重）
- 结果上限 100%

## 数据模型关系图

```
study_team (1) ──→ (N) study_team_member
    │                      │
    │                      ├── role: LEADER/ADMIN/MEMBER
    │                      └── status: QUIT/NORMAL/MUTED
    │
    ├──→ (N) study_team_task
    │         ├── taskType: CODING/DURATION/READING/CUSTOM
    │         ├── repeatType: DAILY/WORKDAY/CUSTOM
    │         └── status: 0已结束/1进行中
    │
    ├──→ (N) study_team_checkin ──→ (N) study_team_checkin_like
    │         ├── isSupplement: 0正常/1补卡
    │         └── isDeleted: 软删除
    │
    ├──→ (N) study_team_discussion
    │         ├── category: ANNOUNCEMENT/QUESTION/NOTE/EXPERIENCE/CHAT
    │         ├── isPinned: 置顶
    │         └── isEssence: 精华
    │
    ├──→ (N) study_team_application
    │         └── status: PENDING/APPROVED/REJECTED/CANCELLED
    │
    └──→ (N) study_team_daily_stats
              └── UNIQUE(team_id, stat_date)
```

## 弱登录接口

以下接口不强制登录，未登录用户也能访问（Controller 中用 `StpUserUtil.isLogin()` 判断）：

| 接口 | 说明 |
| --- | --- |
| `GET /{teamId}` | 小组详情 |
| `POST /list` | 小组广场列表 |
| `GET /recommend` | 推荐小组 |
| `GET /task/{taskId}` | 任务详情 |
| `GET /{teamId}/tasks` | 任务列表 |
| `GET /{teamId}/tasks/today` | 今日任务 |
| `GET /checkin/{checkinId}` | 打卡详情 |
| `GET /{teamId}/checkins` | 打卡动态 |
| `GET /{teamId}/discussions` | 讨论列表 |
| `GET /discussion/{discussionId}` | 讨论详情 |
| `GET /{teamId}/stats` | 统计概览 |
| `GET /{teamId}/rank/*` | 所有排行榜 |

未登录时 `userId = null`，影响的是：不显示"是否已点赞""是否是作者""我的角色"等个性化字段，但公共数据正常返回。

## 异常路径汇总

| 场景 | 异常消息 | 源码位置 |
| --- | --- | --- |
| 创建小组超限 | 每个用户最多创建3个小组 | `StudyTeamServiceImpl:79` |
| 加入小组超限 | 每个用户最多加入10个小组 | `TeamMemberServiceImpl:77` |
| 小组已解散 | 小组已解散 / 小组不存在或已解散 | `TeamMemberServiceImpl:62/424` |
| 小组已满员 | 小组已满员 | `TeamMemberServiceImpl:65` |
| 已是成员 | 您已经是该小组成员 | `TeamMemberServiceImpl:71/434` |
| 重复申请 | 您已提交过申请，请等待审核 | `TeamMemberServiceImpl:83` |
| 邀请加入走申请 | 该小组仅限邀请加入 | `TeamMemberServiceImpl:93` |
| 组长退出 | 组长不能直接退出，请先转让组长 | `TeamMemberServiceImpl:151` |
| 移除组长 | 不能移除组长 | `TeamMemberServiceImpl:282` |
| 管理员互移 | 管理员不能移除其他管理员 | `TeamMemberServiceImpl:287` |
| 设置自己角色 | 不能修改自己的角色 | `TeamMemberServiceImpl:308` |
| 禁言组长 | 不能禁言组长 | `TeamMemberServiceImpl:365` |
| 管理员互禁 | 管理员不能禁言其他管理员 | `TeamMemberServiceImpl:369` |
| 禁言超7天 | 禁言时长最长7天 | `TeamMemberServiceImpl:374` |
| 重复打卡 | 该任务今日已打卡 | `TeamCheckinServiceImpl:99` |
| 补卡超7天 | 只能补最近7天内的打卡 | `TeamCheckinServiceImpl:69` |
| 删除超24h | 只能删除24小时内的打卡记录 | `TeamCheckinServiceImpl:171` |
| 删除他人打卡 | 只能删除自己的打卡记录 | `TeamCheckinServiceImpl:166` |
| 禁言中发讨论 | 您已被禁言，无法发布讨论 | `TeamDiscussionServiceImpl:61` |
| 成员发公告 | 只有组长或管理员可以发布公告 | `TeamDiscussionServiceImpl:67` |
| 重复点赞打卡 | 您已点赞过 | `TeamCheckinServiceImpl:252` |
| 解散超10人 | 小组成员超过10人，不可直接解散 | `StudyTeamServiceImpl:188` |

## 深度常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 创建小组提示超限 | 源码 `MAX_CREATE_TEAMS = 3`，不是 5 | 更新文档和前端提示文案 |
| 加入小组提示超限 | 源码 `MAX_JOIN_TEAMS = 10`，不是 20 | 更新文档和前端提示文案 |
| 讨论点赞可以无限刷 | 没有去重表，简化实现直接 +1 | 补 `study_team_discussion_like` 表和幂等校验 |
| 贡献值始终为 0 | 没有服务调用增加 `contributionPoints` | 补打卡/讨论/获赞时的贡献值累加逻辑 |
| `newMemberCount` 始终为 0 | `refreshDailyStats` 保留旧值但无写入入口 | 在 `addMember` 中补 `newMemberCount` 更新 |
| 退出后重新加入角色重置 | `addMember` 对 QUIT 成员设 `role=MEMBER` | 如需保留原角色，改 `addMember` 逻辑 |
| 连续打卡今天没打则为 0 | 递归 CTE 锚点从 today 开始 | 这是正确行为，"连续"必须包含今天 |
| 打卡删除后活跃天数可能不准 | 删除当天最后一条打卡时 `decrementActiveDays` | 依赖 `selectCheckinUserIds` 实时判断，逻辑正确但需注意并发 |
| 小组列表排序 | `sortBy=hot` 按 `current_members DESC, total_checkins DESC` | "热度"实际是"人数+打卡数"，不是真正的活跃度 |
| 邀请码无唯一性保证 | `RandomUtil.randomString(8).toUpperCase()` 可能碰撞 | 8 位大写字母 = 26^8 ≈ 2088 亿种，碰撞概率极低，但建议加唯一索引 |
| 成员排序 | `ORDER BY role ASC, join_time ASC` | role=1(组长)排最前，role=3(成员)排最后，同角色按加入时间 |
| `updateMemberCount` 并发安全 | 一条 SQL 原子操作，安全 | 但 `addMember` 先查后插，高并发下可能超额加入，建议加分布式锁或乐观锁 |

---

# 个人计划深度拆解

> 以下内容基于 `xiaou-plan` 全量源码（PlanServiceImpl 516 行、GrowthAutopilotServiceImpl 739 行、PlanRemindScheduler 202 行、7 个 Domain、3 个枚举、7 个 Mapper XML）逐行拆解。

## 计划生命周期状态机

```
创建 ──→ ACTIVE(1) ──→ PAUSED(2) ──→ ACTIVE(1)  [可逆循环]
    │                      │
    │                      └──→ COMPLETED(3) [用户手动标记]
    │
    ├──→ EXPIRED(4)  [endDate 已过且未完成]
    │
    └──→ DELETED(0)  [软删除，selectById 自动过滤 status!=0]
```

### 状态转换规则

| 操作 | 前置状态 | 目标状态 | 校验条件 | 源码位置 |
| --- | --- | --- | --- | --- |
| 暂停 | ACTIVE | PAUSED | `plan.getStatus() == 1` | `PlanServiceImpl:183` |
| 恢复 | PAUSED | ACTIVE | `plan.getStatus() == 2` | `PlanServiceImpl:199` |
| 删除 | 任意 | DELETED(0) | `WHERE id AND user_id`（只能删自己的） | `UserPlanMapper.xml:135-138` |
| 打卡 | ACTIVE | ACTIVE（不变） | `plan.getStatus() == 1` 否则抛"计划未在进行中" | `PlanServiceImpl:284` |

**注意**：没有从 ACTIVE → COMPLETED 或 ACTIVE → EXPIRED 的自动转换。源码中 `PlanStatus.COMPLETED(3)` 和 `PlanStatus.EXPIRED(4)` 是枚举值但当前没有任何 ServiceImpl 代码触发这些状态。这意味着计划要么进行中、要么暂停、要么删除，不会自动过期或完成。

## 打卡校验链（PlanServiceImpl:276-333）

```
1. 计划存在且属于当前用户     → planMapper.selectByUserIdAndId
2. 计划状态为 ACTIVE         → plan.getStatus() != 1 → 抛"计划未在进行中"
3. 今日未重复打卡            → checkinRecordMapper.selectByPlanIdAndDate(planId, today)
4. 今天需要打卡              → shouldCheckinToday(plan, today)
5. 计算新连续天数            → calculateNewStreak(plan, today)
6. 创建打卡记录              → pointsEarned = CHECKIN_POINTS(10)
7. 更新计划统计              → totalCheckinDays+1, currentStreak, maxStreak
```

### 连续打卡天数算法

```java
// PlanServiceImpl:409-435
private int calculateNewStreak(UserPlan plan, LocalDate today) {
    PlanCheckinRecord lastRecord = checkinRecordMapper.selectLatestByPlanId(plan.getId());
    if (lastRecord == null) return 1;  // 首次打卡

    LocalDate lastCheckinDate = lastRecord.getCheckinDate();
    LocalDate checkDate = today.minusDays(1);

    // 从昨天往前回溯
    while (!checkDate.isBefore(lastCheckinDate)) {
        if (shouldCheckinToday(plan, checkDate)) {
            // 这天需要打卡
            if (checkDate.equals(lastCheckinDate)) {
                return plan.getCurrentStreak() + 1;  // 连续！
            } else {
                return 1;  // 中间断了
            }
        }
        checkDate = checkDate.minusDays(1);  // 不需要打卡的日子跳过
    }
    return plan.getCurrentStreak() + 1;  // 两次打卡间没有需要打卡的日子
}
```

**关键细节**：连续天数的计算考虑了**重复规则**。如果计划是"工作日打卡"，周末不算断开。举例：周五打卡 → 周一打卡，中间的周六周日 `shouldCheckinToday` 返回 false，所以周一打卡连续天数 = 周五的连续天数 + 1。

**与小组的区别**：小组打卡连续天数用 MySQL 递归 CTE，个人计划连续天数用 Java 代码逐日回溯。两种算法本质逻辑相同，但实现路径不同。

## 重复规则与今日任务筛选

### shouldCheckinToday 逻辑（PlanServiceImpl:382-404）

```java
switch (repeatType) {
    case 1: return true;  // 每日
    case 2: return dayOfWeek != SATURDAY && dayOfWeek != SUNDAY;  // 工作日
    case 3: return dayOfWeek == SATURDAY || dayOfWeek == SUNDAY;  // 周末
    case 4:  // 自定义
        return Arrays.stream(plan.getRepeatDays().split(","))
            .anyMatch(d -> d.equals(String.valueOf(todayValue)));
}
```

### 今日任务 SQL（UserPlanMapper.xml:104-112）

```sql
WHERE user_id = #{userId}
  AND status = 1                    -- 进行中
  AND start_date <= #{today}        -- 已开始
  AND (end_date IS NULL OR end_date >= #{today})  -- 未结束
ORDER BY daily_start_time ASC, create_time DESC
```

- SQL 只筛进行中的计划，**重复规则**由 Java 层 `shouldCheckinToday` 过滤
- 排序按每日开始时间升序，早的任务先出现

### 任务状态计算（PlanServiceImpl:227-235）

```
taskStatus = 0（待完成）：默认
taskStatus = 1（已完成）：checkinRecordMapper.selectByPlanIdAndDate 有记录
taskStatus = 2（已过期）：now.isAfter(plan.getDailyEndTime) 且未打卡
```

## 提醒调度器（三个定时任务）

### 1. generateDailyRemindTasks — 每天 00:00

```java
@Scheduled(cron = "0 0 0 * * ?")
```

流程：
1. `planMapper.selectPlansForRemind(today)` — 查所有 `status=1, remind_enabled=1, start_date<=today, end_date>=today` 的计划
2. 检查是否已生成（`remindTaskMapper.countByPlanIdAndDate` 去重）
3. 生成两种提醒任务：
   - **开始提醒**（remindType=1）：`dailyStartTime - remindBefore 分钟`
   - **截止提醒**（remindType=2）：`dailyEndTime - remindDeadline 分钟`
4. 只生成 `remindTime > now` 的任务（已过时间的跳过）
5. `remindTaskMapper.batchInsert(tasksToInsert)` — 批量插入

**默认值**：`remindBefore=30分钟`，`remindDeadline=10分钟`（PlanServiceImpl:78-79）

### 2. scanAndSendRemindTasks — 每分钟

```java
@Scheduled(cron = "0 * * * * ?")
```

流程：
1. `remindTaskMapper.selectPendingTasks(now, now.plusMinutes(1))` — 查待发送且时间窗口在当前到 1 分钟后的任务
2. 对每个任务 `sendRemind(task)`：
   - 构建通知内容（含计划名、开始/截止时间、目标值）
   - `notificationService.sendNotification(notification)` — 发送到通知中心
   - `remindTaskMapper.updateStatus(taskId, 1, now)` — 标记已发送
3. 异常处理：计划不存在时标记为"已取消"(status=2)

### 3. cleanOldRemindTasks — 每周一 03:00

```java
@Scheduled(cron = "0 0 3 ? * MON")
```

- 删除 7 天前的提醒任务（`remindTaskMapper.deleteOldTasks(beforeDate)`）

### 提醒任务状态机

```
PENDING(0) ──→ SENT(1)  [发送成功]
    │
    └──→ CANCELLED(2)  [计划不存在]
```

## 成长自动驾驶

### 周计划生成流程（GrowthAutopilotServiceImpl:62-105）

1. `normalizeWeekStart(weekStart)` — 对齐到本周一
2. 如果已有 goal → 先 `deleteByGoalId` 删除旧任务，再重新生成
3. `buildWeeklyTasks(goal, today)` — 基于 6 个模块模板生成任务
4. `refreshGoalMetrics(goalId)` — 刷新目标统计
5. `writeEvent(goalId, userId, "generate", ...)` — 写事件日志

### 6 个模块模板与权重分配

| 模块 | key | 默认权重 | 路由 | 最低分钟 |
| --- | --- | --- | --- | --- |
| OJ 冲刺 | `oj` | 26% | `/oj` | 80 |
| 题库学习 | `interview` | 22% | `/interview` | 70 |
| 闪卡记忆 | `flashcard` | 16% | `/flashcard/study` | 50 |
| 计划打卡 | `plan` | 14% | `/plan` | 45 |
| 模拟面试 | `mock` | 14% | `/mock-interview/config` | 50 |
| 积分打卡 | `points` | 8% | `/points` | 35 |

**岗位定制权重**：

| 岗位关键词 | OJ | 题库 | 闪卡 | 计划 | 模面 | 积分 |
| --- | --- | --- | --- | --- | --- | --- |
| 算法/后端/java | 33% | 24% | 14% | 11% | 12% | 6% |
| 前端/react/vue | 20% | 27% | 20% | 13% | 14% | 6% |
| 测试/qa | 18% | 28% | 18% | 16% | 14% | 6% |

### 任务生成算法（buildWeeklyTasks）

```
weeklyMinutes = weeklyHours * 60（默认 8 小时 = 480 分钟）
startDate = max(weekStart, today)  -- 如果周中间才生成，从今天开始

每个模块：
  moduleMinutes = max(minMinutes, weeklyMinutes * weight)
  taskCount = clamp(moduleMinutes / 95, 1, 4)  -- 1 到 4 个任务
  minutePerTask = clamp(moduleMinutes / taskCount, 25, 150)
  taskScore = clamp(minutePerTask / 24, 1, 8)

  任务日期分配：(moduleIndex + i) % availableDays.size()
  -- 模块0的任务0在第一天，模块1的任务0在第二天，模块0的任务1在第三天...
```

### 任务状态机

```
todo ──→ done  [markDone / markDoneByDate]
todo ──→ missed  [markMissedBeforeDate — replan 时标记过去的 todo]
todo ──→ postponed  [postponeTask — taskDate+1 天]
todo ──→ DELETED  [deleteTodoFromDate — replan 时删除从今天起的 todo]
```

### 顺延规则（postponeTask）

```java
// GrowthAutopilotServiceImpl:153-184
if (!"todo".equalsIgnoreCase(task.getStatus())) {
    throw new BusinessException("仅待完成任务支持顺延");
}
LocalDate nextDate = task.getTaskDate().plusDays(1);
if (nextDate.isAfter(goal.getWeekEnd())) {
    throw new BusinessException("已是本周最后一天任务，建议执行一键重排");
}
```

- 只有 todo 状态的任务能顺延
- 顺延 1 天，不能超过周日
- done 和 missed 不能顺延

### 一键重排逻辑（replan）

```java
// GrowthAutopilotServiceImpl:188-219
1. markMissedBeforeDate(goalId, userId, today) — 把今天之前的 todo 改为 missed
2. deleteTodoFromDate(goalId, userId, today) — 删除今天及之后的 todo（物理删除）
3. 计算剩余分数 = totalScoreTarget - completedScore
4. buildReplanTasks(goal, currentTasks, availableDays, remainingScore) — 按最低完成率模块优先排任务
5. refreshGoalMetrics + writeEvent("replan")
```

**重排策略**：按各模块完成率从低到高排序，优先补完成率最低的模块。

### 风险等级计算

```java
// GrowthAutopilotServiceImpl:620-628
if (overdueTasks >= 4 || (daysLeft <= 2 && completionRate < 60)) return "high";
if (overdueTasks >= 2 || completionRate < 50) return "medium";
return "low";
```

- high：逾期 >= 4 或只剩 2 天但完成率 < 60%
- medium：逾期 >= 2 或完成率 < 50%
- low：其余

### 每周投入时长约束

```java
DEFAULT_WEEKLY_HOURS = 8;
MIN_WEEKLY_HOURS = 3;
MAX_WEEKLY_HOURS = 40;
```

不传则默认 8 小时，传入值 clamp 到 3-40。

## 计划积分

打卡积分固定为 `CHECKIN_POINTS = 10`（`PlanServiceImpl:47`）。写入 `plan_checkin_record.points_earned` 字段，但**没有调用积分服务入账**。`PlanServiceImpl:370` 的 `getStatsOverview` 中 `avgCompletionRate`、`weekCheckinCount`、`monthCheckinCount` 都是简化处理（返回 0.0 或 0），需要后续补 SQL 聚合。

## 数据模型关系图

```
user_plan (1) ──→ (N) plan_checkin_record
    │                    ├── isSupplement: 0正常/1补卡
    │                    └── pointsEarned: 固定10分
    │
    ├──→ (N) plan_remind_task
    │         ├── remindType: 1开始/2截止
    │         └── status: 0待发送/1已发送/2已取消
    │
    └──→ 状态: DELETED(0)/ACTIVE(1)/PAUSED(2)/COMPLETED(3)/EXPIRED(4)

growth_autopilot_goal (1) ──→ (N) growth_autopilot_task
    │                              ├── status: todo/done/missed
    │                              ├── source: auto/replan
    │                              └── priority: P1/P2/P3
    │
    └──→ (N) growth_autopilot_event
              └── eventType: generate/replan/complete/complete_batch/postpone
```

## 异常路径汇总

| 场景 | 异常消息 | 源码位置 |
| --- | --- | --- |
| 计划名空 | 计划名称不能为空 | `PlanServiceImpl:54` |
| 类型空 | 计划类型不能为空 | `PlanServiceImpl:57` |
| 开始日期空 | 开始日期不能为空 | `PlanServiceImpl:60` |
| 计划不存在 | 计划不存在 | `PlanServiceImpl:99/159/181/196/282/340` |
| 非ACTIVE暂停 | 只有进行中的计划可以暂停 | `PlanServiceImpl:184` |
| 非PAUSED恢复 | 只有已暂停的计划可以恢复 | `PlanServiceImpl:200` |
| 非ACTIVE打卡 | 计划未在进行中，无法打卡 | `PlanServiceImpl:285` |
| 重复打卡 | 今日已打卡，请勿重复操作 | `PlanServiceImpl:291` |
| 今天不需打卡 | 今日不需要打卡 | `PlanServiceImpl:297` |
| 任务ID空 | 任务ID不能为空 | `GrowthAutopilotServiceImpl:111/155` |
| 任务不存在 | 任务不存在 | `GrowthAutopilotServiceImpl:114/158` |
| 周计划不存在 | 周计划不存在 / 当前周尚未生成自动驾驶计划 | `GrowthAutopilotServiceImpl:119/136/193` |
| 非todo顺延 | 仅待完成任务支持顺延 | `GrowthAutopilotServiceImpl:162` |
| 超周尾顺延 | 已是本周最后一天任务，建议执行一键重排 | `GrowthAutopilotServiceImpl:174` |
| 顺延失败 | 任务顺延失败，请稍后重试 | `GrowthAutopilotServiceImpl:178` |
| 今天不在周内 | 今天不在当前周计划范围内 | `GrowthAutopilotServiceImpl:141` |

## 深度常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| COMPLETED/EXPIRED 状态永远不出现 | 源码没有自动过期和完成逻辑 | 补定时任务扫描 endDate 已过的计划设 EXPIRED，totalCheckinDays 达标的设 COMPLETED |
| 打卡积分不入账 | 只写入 `points_earned` 字段，没有调用 `xiaou-points` 服务 | 补积分服务调用 |
| `getStatsOverview` 周月统计为 0 | `avgCompletionRate`/`weekCheckinCount`/`monthCheckinCount` 简化返回 0 | 补 SQL 聚合查询 |
| 连续天数考虑重复规则 | `calculateNewStreak` 用 `shouldCheckinToday` 回溯 | 工作日计划周末断开不计，周末计划工作日断开不计 |
| `planType=99` 归一化 | `normalizePlanTypeCode` 把 99 转为 5(CUSTOM) | 前端可能传 99，需要统一 |
| `repeatType=3` 的歧义 | 前端传 `repeatType=3` + `repeatDays` 不为空 → 归一化为 CUSTOM(4) | RepeatType.WEEKEND(3) 和 CUSTOM(4) 之间有归一化逻辑，前端需注意 |
| 今日任务列表排序 | SQL 按 `daily_start_time ASC`，没有考虑 `daily_end_time` 过期 | 已过截止时间的任务显示为"已过期"状态，但仍在列表中 |
| 删除计划是软删除 | `update status=0`，不是物理删除 | 打卡记录不会被级联删除，查询时 `status!=0` 自动过滤 |
| 重排时物理删除 todo | `deleteTodoFromDate` 是 DELETE 不是 UPDATE | 已删除的任务不可恢复，与计划的软删除策略不一致 |
| 每周投入时长 clamp | 传入 50 → 实际存储 40（MAX_WEEKLY_HOURS） | 前端应做预校验，避免用户困惑 |
| 提醒去重不完善 | `countByPlanIdAndDate` 只按 planId+date 去重 | 同一计划同一天只生成一组开始+截止提醒，如果手动刷新可能重复 |
