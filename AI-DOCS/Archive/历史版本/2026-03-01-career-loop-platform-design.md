# 求职闭环中台（V1）设计文档

## 1. 背景与目标

当前项目已经具备 `Job Battle`、`计划打卡`、`AI模拟面试`、`简历`、`通知` 等独立能力，但用户仍需跨页面自行串联流程。  
本设计目标是在不推翻现有模块的前提下，新增“求职闭环中台”，把核心链路统一成一条可追踪、可推进、可提醒的成长主线。

V1 目标范围：
- 打通 `JD解析 -> 简历匹配 -> 计划生成 -> 计划执行 -> 面试完成 -> 复盘完成`。
- 提供统一的阶段状态、时间线、动作清单、风险提示。
- 支持自动同步和手动补偿同步。

不在 V1 范围：
- Offer 投递跟踪系统
- 复杂推荐算法（先用规则）
- 多角色协作与导师体系

## 2. 方案对比与选型

### 方案 A：中心编排式（选用）
- 描述：新增中台服务管理状态机与跨模块同步，其他模块保留现有边界。
- 优点：一致性好，可观测性高，后续扩展新阶段成本低。
- 缺点：需要新增表结构和编排逻辑。

### 方案 B：事件总线式
- 描述：各模块发事件，中台只做订阅聚合。
- 优点：解耦强。
- 缺点：排障复杂、最终一致性成本高，不适合当前快速落地。

### 方案 C：前端聚合式
- 描述：不新增后端中台，前端拼装多接口数据。
- 优点：初期开发快。
- 缺点：状态不可控，流程容易“看起来连通，实际上不一致”。

结论：V1 采用 **方案 A（中心编排式）**。

## 3. 架构设计

### 3.1 模块边界

- 新增中台域：建议放在 `xiaou-mock-interview`（同求职场景，低迁移成本）。
- 既有模块保持职责不变：
  - `job-battle` 负责计划生成与复盘内容
  - `xiaou-plan` 负责计划执行/打卡
  - `mock-interview` 负责面试过程与报告
  - `resume` 保持简历域能力
  - `notification` 负责提醒触达

### 3.2 核心职责

中台只做三件事：
1. 阶段状态机管理（可追踪）
2. 跨模块聚合快照（可展示）
3. 动作清单编排（可执行）

## 4. 数据模型

### 4.1 career_loop_session（主会话）
- `id`
- `user_id`
- `target_role`
- `target_company_type`
- `current_stage`
- `health_score`（0-100）
- `status`（active/completed/archived）
- `create_time` / `update_time`

### 4.2 career_loop_stage_log（阶段日志）
- `id`
- `session_id`
- `from_stage`
- `to_stage`
- `trigger_source`（job_battle/plan/mock_interview/manual）
- `trigger_ref_id`（外部记录ID）
- `note`
- `create_time`

### 4.3 career_loop_action（动作清单）
- `id`
- `session_id`
- `stage`
- `action_type`（study/mock/review/resume）
- `title`
- `description`
- `priority`（P0/P1/P2）
- `status`（todo/doing/done）
- `due_date`
- `source`（rule/ai/manual）
- `create_time` / `update_time`

### 4.4 career_loop_snapshot（快照）
- `id`
- `session_id`
- `plan_progress`
- `mock_count`
- `latest_mock_score`
- `review_count`
- `resume_updated_at`
- `risk_flags_json`
- `next_suggestion_json`
- `create_time` / `update_time`

## 5. 状态机设计

阶段枚举（V1）：
- `INIT`
- `JD_PARSED`
- `RESUME_MATCHED`
- `PLAN_READY`
- `PLAN_EXECUTING`
- `INTERVIEW_DONE`
- `REVIEWED`

关键推进规则（V1）：
- Job Battle 完成 JD 解析 -> `JD_PARSED`
- Job Battle 完成简历匹配 -> `RESUME_MATCHED`
- Job Battle 成功生成计划 -> `PLAN_READY`
- 计划打卡进度达到阈值（如 >= 20%）-> `PLAN_EXECUTING`
- 完成至少一次模拟面试并生成报告 -> `INTERVIEW_DONE`
- 完成复盘总结 -> `REVIEWED`

约束：
- 只允许向前推进，不允许直接回退（回退通过新会话或人工干预日志处理）。
- 同阶段重复事件要幂等。

## 6. API 设计（用户端）

### 6.1 启动会话
- `POST /user/career-loop/start`
- 入参：`targetRole`、`targetCompanyType`（可选）
- 出参：当前会话信息

### 6.2 当前状态
- `GET /user/career-loop/current`
- 出参：`session` + `snapshot` + `nextActions`

### 6.3 时间线
- `GET /user/career-loop/timeline`
- 出参：阶段推进日志列表

### 6.4 动作清单
- `GET /user/career-loop/actions`
- `POST /user/career-loop/actions/{id}/done`

### 6.5 手动同步
- `POST /user/career-loop/sync`
- 用于补偿：拉取现有模块最新状态并重算阶段/快照

### 6.6 事件上报（内部）
- `POST /user/career-loop/event`
- 供 job-battle / plan / mock-interview 在关键节点调用（也可服务内直连）

## 7. 前端交互设计

新增页面：`/career-loop`

页面结构：
1. 顶部总览卡：当前阶段、健康分、近7天推进情况
2. 阶段时间线：展示从 INIT 到 REVIEWED 的推进轨迹
3. 动作清单：可勾选完成、可一键跳转到对应模块页面
4. 风险区：展示当前卡点（例如“打卡进度不足”“模拟面试次数偏低”）

现有页面最小接入：
- `job-battle` 步骤3/4增加“同步到闭环”“查看闭环进度”入口
- 模拟面试报告页增加“写入闭环记录”入口（默认自动）

## 8. 错误处理与一致性

- 事件写入使用幂等键：`userId + source + refId + stage`
- 同步失败记录错误日志并返回“待同步”状态，不阻断主流程
- 增加定时补偿任务（可选）：扫描未同步事件并重试
- 所有跨模块调用失败时，仅降级当前步骤，不清空已有快照

## 9. 安全与权限

- 仅用户本人可访问自己的闭环会话
- 管理端后续可加只读观测接口（V1 可不做）
- 所有接口走现有 Sa-Token 用户态鉴权

## 10. 指标与验收

核心指标：
- `阶段转化率`（每阶段进入人数/上一阶段人数）
- `阶段平均停留时长`
- `动作完成率`
- `从计划生成到复盘完成的周期`

验收标准：
- 用户能在单页看到完整闭环状态与时间线
- Job Battle 生成计划后，闭环状态可自动推进
- 计划打卡与模拟面试完成后，动作清单和风险提示自动更新
- 任一模块异常不导致闭环页白屏

## 11. 实施策略

- Phase 1：状态机 + 主接口 + 闭环页骨架
- Phase 2：接入计划/面试事件自动同步
- Phase 3：健康分、风险规则、通知提醒

