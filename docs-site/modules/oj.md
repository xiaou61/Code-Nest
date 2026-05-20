# OJ 判题系统

OJ 模块提供题目、测试用例、代码提交、判题、题解、评论、赛事和排行能力。

## 主要入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/oj`、`/oj/problem/:id`、`/oj/my-submissions`、`/oj/contests`、`/oj/ranking`、`/oj/playground` |
| 管理端 | `/oj/problems`、`/oj/contests`、`/oj/tags` |
| 后端 | `xiaou-oj` |
| 沙箱 | `docker/go-judge` |

## 推荐学习顺序

OJ 的代码看起来多，但主线很清楚。建议按这条线读：

1. 先看 `OjProblemPublicController` 和 `OjProblemController`，理解题目、标签、测试用例从哪里来。
2. 再看 `OjSubmissionServiceImpl.submitCode`，理解提交记录什么时候写入。
3. 接着看 `JudgeService.judge`，把一次判题拆成加载题目、加载用例、编译、运行、比对、更新状态。
4. 然后看 `CodeRunnerService`、`GoJudgeClient` 和 `judge/strategy`，理解不同语言怎么进入 go-judge。
5. 最后看 `ContestRuleValidator` 和 `ContestRankingCalculator`，学习赛事校验和 ACM 排名。

## 源码地图

| 目标 | 文件 |
| --- | --- |
| 题目公开接口 | `xiaou-oj/src/main/java/com/xiaou/oj/controller/pub/OjProblemPublicController.java` |
| 提交和运行接口 | `xiaou-oj/src/main/java/com/xiaou/oj/controller/pub/OjSubmissionController.java` |
| 赛事公开接口 | `xiaou-oj/src/main/java/com/xiaou/oj/controller/pub/OjContestPublicController.java` |
| 管理题目 | `xiaou-oj/src/main/java/com/xiaou/oj/controller/admin/OjProblemController.java` |
| 提交服务 | `xiaou-oj/src/main/java/com/xiaou/oj/service/impl/OjSubmissionServiceImpl.java` |
| 判题服务 | `xiaou-oj/src/main/java/com/xiaou/oj/judge/JudgeService.java` |
| 沙箱客户端 | `xiaou-oj/src/main/java/com/xiaou/oj/judge/sandbox/GoJudgeClient.java` |
| 语言策略 | `xiaou-oj/src/main/java/com/xiaou/oj/judge/strategy` |
| 赛事校验 | `xiaou-oj/src/main/java/com/xiaou/oj/contest/ContestRuleValidator.java` |
| 赛事榜单 | `xiaou-oj/src/main/java/com/xiaou/oj/contest/ContestRankingCalculator.java` |

## 接口分组

| 接口域 | 能力 |
| --- | --- |
| `/oj/problems/list` | 用户题目列表 |
| `/oj/problems/{id}` | 题目详情 |
| `/oj/run` | Playground 自由运行 |
| `/oj/test` | 使用样例或题目样例自测 |
| `/oj/submit` | 正式提交并异步判题 |
| `/oj/submissions/**` | 提交详情、我的提交、题目提交 |
| `/oj/statistics/me` | 我的刷题统计 |
| `/oj/contests/**` | 赛事列表、报名、详情、榜单 |
| `/oj/problems/{problemId}/comments/**` | 题目评论、回复、点赞 |
| `/admin/oj/problems/**` | 题目、标签和题目列表管理 |
| `/admin/oj/test-cases/**` | 测试用例维护 |
| `/admin/oj/contests/**` | 赛事创建、题目关联和状态管理 |

## 核心流程

1. 管理员创建题目、标签、测试用例和赛事。
2. 用户查看题目并提交代码。
3. 后端保存提交记录并调用判题服务。
4. 判题服务编译、运行、比对输出并返回结果。
5. 后端更新提交状态、耗时、内存和得分。
6. 赛事模式下同步刷新榜单。

## 一次提交如何落库

正式提交入口是 `/oj/submit`。主流程可以按下面这张表排查：

| 步骤 | 代码位置 | 说明 |
| --- | --- | --- |
| 校验语言 | `JudgeLanguage.of` | 不支持的语言会在提交前被拒绝 |
| 校验题目 | `OjProblemMapper.selectById` | 题目不存在时不写提交 |
| 校验赛事 | `validateContestSubmit` | 赛事必须存在、状态为 `2`、用户已报名、题目属于赛事 |
| 写提交 | `oj_submission` | 初始状态是 `pending` |
| 增加提交数 | `OjProblemMapper.incrementSubmitCount` | 无论最后是否 AC，提交数都会增加 |
| 异步判题 | `judgeService.judge(submissionId)` | 后续状态更新不阻塞提交接口返回 |

这意味着排查“提交后一直等待”时，第一眼应该看 `oj_submission.status` 是否还停在 `pending`。如果停在 `pending`，通常是异步判题没有启动或判题线程异常；如果进入 `judging` 后不变，再看 go-judge 和用例执行。

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

题目描述和题解说明会在用户端以 Markdown 展示，当前前端通过 `@/utils/markdown` 的 `renderMarkdown` 统一渲染并净化。新增题面字段或题解富文本时，不要直接 `v-html="rawContent"`，应按 [前端渲染安全](/reference/frontend-rendering-security) 的 Markdown 链路处理。

## 赛事能力

OJ 赛事覆盖赛事创建、题目关联、报名、提交限制、榜单统计和赛后评分预估。

赛事状态在保存请求中标注为：`0` 草稿、`1` 即将开始、`2` 进行中、`3` 已结束。当前提交校验只允许状态 `2` 且当前时间在 `startTime` 和 `endTime` 之间。

ACM 排名在 `ContestRankingCalculator` 中计算，规则是：

| 规则 | 说明 |
| --- | --- |
| 解题数优先 | `acceptedCount` 越高排名越靠前 |
| 罚时越少越靠前 | 每题首次 AC 时间加上 AC 前错误提交罚时 |
| 错题罚时 | AC 前每次错误提交增加 20 分钟 |
| 最后 AC 时间 | 解题数和罚时相同后，用最后 AC 时间辅助排序 |
| 稳定排序 | 最后用 `userId` 保证排序稳定 |

当前文档和源码可确认的赛事能力是报名、提交校验、ACM 排名和赛后评分预估。冻结榜、赛后补题和更细的题目可见性策略需要在新增源码实现后再补，不应该在现有文档里写成已上线能力。

## 核心数据表

| 表 | 作用 | 排查时重点看 |
| --- | --- | --- |
| `oj_problem` | 题目主表 | 难度、状态、时间/内存限制、提交数和 AC 数 |
| `oj_test_case` | 测试用例 | 是否有用例、输入输出是否和题面一致 |
| `oj_submission` | 用户提交 | `status`、`time_used`、`memory_used`、`error_message` |
| `oj_problem_tag` | 标签 | 标签是否可用于题目筛选 |
| `oj_problem_tag_relation` | 题目标签关系 | 题目列表筛选异常时检查 |
| `oj_solution` | 题解 | 用户题解展示和后台维护 |
| `oj_contest` | 赛事主表 | 状态、开始时间、结束时间 |
| `oj_contest_problem` | 赛事题目关系 | 提交赛事题时必须存在 |
| `oj_contest_participant` | 报名记录 | 用户未报名时不能提交赛事题 |

## 常见坑

| 问题 | 常见原因 | 排查方式 |
| --- | --- | --- |
| 提交一直 `pending` | 异步判题没有执行或线程异常 | 看应用日志和 `JudgeService.judge` 是否进入 |
| 提交变成 `system_error` | 测试用例缺失、go-judge 不可用或判题异常 | 先查题目用例，再查 `http://localhost:5050` |
| 自测通过但正式提交 WA | 自测只覆盖样例，正式判题覆盖隐藏用例 | 比对 `oj_test_case`，不要只看样例 |
| 输出看起来一样仍 WA | 当前只忽略首尾空白，不忽略行内空格 | 检查多余空格、换行和格式化输出 |
| 首次 AC 没有积分 | 积分发放失败只记日志，不回滚 AC | 查 `user_points_detail` 和判题日志 |
| 赛事题提交失败 | 状态不是 `2`、时间窗口不对、未报名或题目未关联 | 查 `oj_contest`、`oj_contest_participant`、`oj_contest_problem` |

## 安全边界

- 用户代码不能在主应用进程直接执行。
- 判题服务应限制 CPU、内存、文件系统和网络能力。
- 测试用例输出比对应处理空白符、超时、编译错误和运行错误。
- 正式判题和 Playground 自测可以采用不同响应策略。
- go-judge 不可用时会落到 `system_error` 或 Playground 的 `error`，本地验收要先确认 `docker/go-judge` 服务可访问。
- 题面、题解和评论展示涉及 Markdown 或用户文本时，必须走统一渲染或转义链路。

## 验证清单

| 场景 | 预期 |
| --- | --- |
| Playground 运行 Python/JavaScript | 同步返回输出、耗时和状态 |
| 题目自测样例 | 每个用例返回 `accepted` 或错误状态 |
| 正式提交正确代码 | `pending -> judging -> accepted`，题目 AC 数增加 |
| 正式提交错误代码 | 返回 `wrong_answer`，提交数增加但 AC 数不增加 |
| 编译错误代码 | 返回 `compile_error` 并保存错误信息 |
| 无测试用例题目 | 返回 `system_error`，提示测试用例缺失 |
| 赛事未报名提交 | 提交前被拒绝 |
| 赛事榜单 | 按 AC 数、罚时和最后 AC 时间排序 |
