# OJ 判题系统

OJ 模块提供题目、测试用例、代码提交、判题、题解、评论、赛事和排行能力。

## 主要入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/oj`、`/oj/problem/:id`、`/oj/my-submissions`、`/oj/contests`、`/oj/ranking`、`/oj/playground` |
| 管理端 | `/oj/problems`、`/oj/contests`、`/oj/tags` |
| 后端 | `xiaou-oj` |
| 沙箱 | `docker/go-judge` |

## 核心流程

1. 管理员创建题目、标签、测试用例和赛事。
2. 用户查看题目并提交代码。
3. 后端保存提交记录并调用判题服务。
4. 判题服务编译、运行、比对输出并返回结果。
5. 后端更新提交状态、耗时、内存和得分。
6. 赛事模式下同步刷新榜单。

## 判题状态机

提交状态定义在 `xiaou-oj/src/main/java/com/xiaou/oj/enums/SubmissionStatus.java`。

| 状态 | 含义 | 进入条件 |
| --- | --- | --- |
| `pending` | 等待判题 | `/oj/submit` 保存提交记录后写入 |
| `judging` | 判题中 | `JudgeService.judge` 异步任务开始执行 |
| `accepted` | 通过 | 所有测试用例输出比对通过 |
| `wrong_answer` | 答案错误 | 运行成功但输出与期望不一致 |
| `time_limit_exceeded` | 超时 | go-judge 返回 `Time Limit Exceeded` |
| `memory_limit_exceeded` | 超内存 | go-judge 返回 `Memory Limit Exceeded` |
| `runtime_error` | 运行错误 | go-judge 非 Accepted 或退出码非 0 |
| `compile_error` | 编译错误 | 编译阶段失败或退出码非 0 |
| `system_error` | 系统错误 | 测试用例缺失、沙箱异常或判题异常 |

正常提交链路是 `pending -> judging -> accepted/failed`。如果赛事提交带 `contestId`，`OjSubmissionServiceImpl.validateContestSubmit` 会先校验赛事存在、状态为 `2`、当前时间在比赛窗口内、用户已报名、题目属于该赛事。

## go-judge 调用边界

沙箱客户端在 `xiaou-oj/src/main/java/com/xiaou/oj/judge/sandbox/GoJudgeClient.java`，配置在 `OjJudgeProperties`：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `oj.judge.goJudgeUrl` | `http://localhost:5050` | go-judge REST 地址 |
| `oj.judge.maxCompileTime` | `10000` ms | 编译阶段 CPU 时间 |
| `oj.judge.defaultTimeLimit` | `2000` ms | 题目未设置时的默认运行时间 |
| `oj.judge.defaultMemoryLimit` | `256` MB | 题目未设置时的默认内存 |

执行时会把源码放入 `copyIn`，编译型语言使用 `copyOutCached` 缓存编译产物，运行结束后通过 `/file/{fileId}` 清理缓存文件。支持语言策略放在 `judge/strategy`，当前包含 C、C++、Go、Java、JavaScript 和 Python。

## 三种运行入口

| 入口 | Controller | 特点 |
| --- | --- | --- |
| `/oj/run` | `OjSubmissionController.runCode` | Playground 自由运行，不绑定题目 |
| `/oj/test` | `OjSubmissionController.selfTest` | 使用样例用例或题目样例输入输出，同步返回每个用例结果 |
| `/oj/submit` | `OjSubmissionController.submitCode` | 写入 `oj_submission` 并异步判题 |

自测输出只比较 `strip()` 后的文本；正式判题同样忽略首尾空白，但不忽略行内空格差异。

## 积分联动

`JudgeService` 在首次 AC 时会：

1. 判断该用户该题是否已有 accepted 提交。
2. 更新题目 AC 数。
3. 调用 `PointsService.grantSystemPoints` 发放积分。

积分规则目前按难度写在代码里：easy 或其他 100，medium 200，hard 500。积分发放失败只记录日志，不回滚提交状态。

## 题目管理

管理端支持题目列表、创建、编辑和测试用例维护。题目通常包含：

- 标题和描述。
- 难度和标签。
- 输入输出说明。
- 样例。
- 测试用例。
- 时间和内存限制。

## 赛事能力

OJ 赛事覆盖赛事创建、题目关联、报名、提交限制、榜单统计和赛后评分预估。

赛事状态在保存请求中标注为：`0` 草稿、`1` 即将开始、`2` 进行中、`3` 已结束。当前提交校验只允许状态 `2` 且当前时间在 `startTime` 和 `endTime` 之间。

后续文档还需要继续补齐 ACM 罚时、冻结榜、赛后补题和题目可见性策略。

## 安全边界

- 用户代码不能在主应用进程直接执行。
- 判题服务应限制 CPU、内存、文件系统和网络能力。
- 测试用例输出比对应处理空白符、超时、编译错误和运行错误。
- 正式判题和 Playground 自测可以采用不同响应策略。
- go-judge 不可用时会落到 `system_error` 或 Playground 的 `error`，本地验收要先确认 `docker/go-judge` 服务可访问。
