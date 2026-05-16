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

源码位置：

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

接口域：

| 接口 | 说明 |
| --- | --- |
| `/user/plan/create` | 创建计划 |
| `/user/plan/update/{planId}` | 更新计划 |
| `/user/plan/delete/{planId}` | 删除计划 |
| `/user/plan/{planId}` | 计划详情 |
| `/user/plan/list` | 计划列表 |
| `/user/plan/pause/{planId}` | 暂停计划 |
| `/user/plan/resume/{planId}` | 恢复计划 |
| `/user/plan/today-tasks` | 今日任务 |
| `/user/plan/checkin` | 打卡 |
| `/user/plan/checkin-records/{planId}` | 打卡记录 |
| `/user/plan/stats/overview` | 统计概览 |

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

接口域：

| 接口 | 说明 |
| --- | --- |
| `/user/plan/autopilot/dashboard` | 周计划仪表盘 |
| `/user/plan/autopilot/generate` | 生成周计划 |
| `/user/plan/autopilot/replan` | 根据进度重排 |
| `/user/plan/autopilot/task/{taskId}/complete` | 完成单个任务 |
| `/user/plan/autopilot/tasks/today/complete` | 批量完成今日任务 |
| `/user/plan/autopilot/task/{taskId}/postpone` | 顺延任务 |

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
3. 每个用户最多创建 5 个小组。
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
