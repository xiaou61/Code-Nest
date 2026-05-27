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

---

## OJ 判题系统模块深度拆解

> 以下内容基于 `xiaou-oj` 全部源码逐行拆解，覆盖 6 个 ServiceImpl、1 个 JudgeService、1 个 CodeRunnerService、1 个 GoJudgeClient、6 个 JudgeStrategy、2 个 Contest 组件、4 个 Controller、9 个 Domain、3 个 Enum、10 个 Mapper。

### 一、提交服务深度分析

**源码**：`OjSubmissionServiceImpl.java`（139 行）

```
submitCode(userId, request):
  1. JudgeLanguage.of(request.language) → 不支持则抛 IllegalArgumentException
  2. problemMapper.selectById(request.problemId) → null → throw BusinessException("题目不存在")
  3. if (request.contestId != null) → validateContestSubmit(contestId, userId, problemId)
  4. 构建 OjSubmission → status = PENDING
  5. submissionMapper.insert(submission)
  6. problemMapper.increaseSubmitCount(request.problemId)  ← 无论判题结果，提交数+1
  7. judgeService.judge(submission.getId())  ← @Async 异步
  8. return submission.getId()

getSubmissionById(id):
  1. submissionMapper.selectById(id)
  2. if status == "accepted":
     → submissionMapper.isFirstAccepted(userId, problemId, id)
     → firstAc → problem.getDifficulty → getAcPoints(difficulty) → setPointsEarned
  3. 返回提交记录

getStatistics(userId):
  → submissionMapper.countByUserId(userId)
  → submissionMapper.countAcceptedProblemsByUserId(userId)
  → submissionMapper.countAttemptedProblemsByUserId(userId)
  → submissionMapper.countAcceptedByDifficulty(userId, "easy/medium/hard")

validateContestSubmit(contestId, userId, problemId):
  1. contestMapper.selectById(contestId) → null → throw
  2. ContestRuleValidator.checkCanSubmit(contest, now) → status==2 且 now 在窗口内
  3. contestParticipantMapper.existsByContestIdAndUserId → 未报名 → throw
  4. contestProblemMapper.existsProblemInContest → 题目不属于赛事 → throw
```

**关键发现 1**：`increaseSubmitCount` 在步骤 6 被调用，发生在判题结果返回之前。如果判题结果为 `system_error`（如用例缺失），提交数已经 +1 但该提交实际未执行任何用例，导致题目提交数虚高。

**关键发现 2**：`getSubmissionById` 中 `isFirstAccepted` 的查询条件是"没有比当前提交 ID 更早的 accepted"，这意味着如果同一用户有两条 accepted 提交，第二条的 `isFirstAccepted` 会返回 `false`，逻辑正确。但 `pointsEarned` 只是回填到返回对象上，不会真正发放积分——积分发放只在 `JudgeService.judge` 的异步流程中完成。

**关键发现 3**：`JudgeLanguage.of` 在步骤 1 抛出 `IllegalArgumentException` 而非 `BusinessException`。`GlobalExceptionHandler` 不会对 `IllegalArgumentException` 返回友好错误码，会落入兜底的 `INTERNAL_ERROR`，用户看到的错误信息可能不够清晰。

### 二、判题服务深度分析

**源码**：`JudgeService.java`（231 行）

```
judge(submissionId):  @Async
  try:
    1. submissionMapper.selectById(submissionId) → null → log + return
    2. submission.setStatus("judging") → updateById
    3. problemMapper.selectById(problemId)
    4. testCaseMapper.selectByProblemId(problemId)
    5. testCases.isEmpty() → updateSubmission(SYSTEM_ERROR, "没有测试用例") + return

    // 编译阶段
    6. strategy.getCompileArgs() != null:
       goJudgeClient.run(compileArgs, sourceFiles, null, compileCpuLimit, memoryLimit, null, compiledFileNames)
       → !accepted || exitStatus!=0 → updateSubmission(COMPILE_ERROR) + return
       → compiledFileIds = result.getFileIds()

    // 运行阶段 - 逐个用例
    7. for each testCase:
       goJudgeClient.run(runArgs, runFiles, compiledFileIds, stdin, cpuLimit, memoryLimit, null, null)
       → TLE → updateSubmission(TIME_LIMIT_EXCEEDED) + return
       → MLE → updateSubmission(MEMORY_LIMIT_EXCEEDED) + return
       → !accepted || exitStatus!=0 → updateSubmission(RUNTIME_ERROR) + return
       → compareOutput(stdout, expectedOutput) → !match → updateSubmission(WRONG_ANSWER) + return
       → passCount++

    // 全部通过
    8. firstAc = !submissionMapper.existsAccepted(userId, problemId)
    9. updateSubmission(ACCEPTED, maxTime, maxMemory, passCount, totalCount)
    10. if (firstAc):
        problemMapper.increaseAcceptedCount(problemId)
        → pointsService.grantSystemPoints(userId, points, OJ_AC, "首次通过题目「xxx」")
        → catch: log.error (积分发放失败不回滚)

  finally:
    if (compiledFileIds != null):
      compiledFileIds.values().forEach(goJudgeClient::deleteFile)

  catch Throwable:
    updateSubmission(SYSTEM_ERROR, "系统错误: " + t.getMessage())
```

**关键发现 1**：步骤 8 和 9 存在**竞态窗口**。`existsAccepted` 查询和 `updateSubmission(ACCEPTED)` 更新之间没有同步锁。如果同一用户对同一题几乎同时有两个提交都通过了全部用例，两个异步线程可能都读到 `existsAccepted=false`，导致：
- AC 数被 +1 两次
- 积分被发放两次

这是典型的 check-then-act 竞态问题。

**关键发现 2**：`compareOutput` 方法使用 `strip()` 忽略首尾空白。但 `strip()` 是 Java 11+ 方法，会移除 Unicode 空白字符，行为比 `trim()` 更强。如果题面期望输出包含首尾的特殊空白字符（如不间断空格 `\u00A0`），会被错误地 trim 掉，导致误判通过。

**关键发现 3**：步骤 7 中的用例执行是**串行**的——逐个用例调用 go-judge。如果一道题有 100 个用例，每个用例最长 2 秒，最坏情况需要 200 秒才能判完一道提交。没有用例级别的超时退出机制——即使已经发现 TLE/WA 也会继续跑完所有用例（实际上会提前 return，但 TLE/MLE/WA/RTE 之后的用例不会被跳过，因为该分支已经 return）。

**关键发现 4**：`errorMessage` 截断到 4000 字符。如果 go-judge 返回的超长错误信息被截断，运维排查时可能丢失关键信息。

**关键发现 5**：`@Async` 注解没有指定线程池，使用 Spring 默认的 `SimpleAsyncTaskExecutor`。这意味着每次判题都创建新线程，没有上界限制。如果短时间内有大量提交，可能耗尽线程资源。

### 三、GoJudgeClient 通信协议深度分析

**源码**：`GoJudgeClient.java`（249 行）

```
run(args, files, cachedFileIn, stdin, cpuLimit, memoryLimit, copyOut, copyOutCached):
  构建 cmd:
    args: 命令参数
    env: PATH=/usr/bin:/bin:/usr/local/bin:/usr/lib/jvm/default-java/bin
    cpuLimit: 纳秒
    memoryLimit: 字节
    procLimit: 50  ← 硬编码进程数限制
    files: [stdin(content), stdout(name, max=10240), stderr(name, max=10240)]
    copyIn: {源码文件(content), 缓存文件(fileId)}
    copyOut: 需要拷出的文件
    copyOutCached: 需要拷出并缓存的文件

  HTTP POST → {goJudgeUrl}/run
  解析响应:
    status → "Accepted"/"Time Limit Exceeded"/"Memory Limit Exceeded" 等
    exitStatus → 退出码
    time → ns → ms
    memory → bytes → KB
    files → stdout/stderr 内容
    fileIds → copyOutCached 返回的缓存文件 ID
    error → 错误信息

deleteFile(fileId):
  HTTP DELETE → {goJudgeUrl}/file/{fileId}
  catch: log.warn (删除失败不阻断)
```

**关键发现 1**：`procLimit` 硬编码为 50。这限制了用户代码可创建的子进程数。但不同题目的需求可能不同——有些并发题需要更多进程。当前不可配置。

**关键发现 2**：stdout 和 stderr 的最大收集量硬编码为 10240 字节（10KB）。如果用户程序输出超过 10KB，go-judge 会截断。这意味着输出大量数据的程序可能被判 WA（因为实际输出被截断后和期望不匹配）。10KB 对于某些需要输出大型结果的题目可能太小。

**关键发现 3**：`env` 硬编码了 PATH，但没有包含用户可能需要的其他环境变量（如 `HOME`、`LANG`、`PYTHONPATH` 等）。某些需要特定环境变量的程序可能无法正常运行。

**关键发现 4**：`deleteFile` 失败只 warn 不阻断。如果 go-judge 服务重启，所有缓存文件引用会失效，但应用侧不知道——下次判题引用 fileId 会失败，回退到 system_error。

### 四、策略模式语言支持深度分析

**源码**：`JudgeStrategy` 接口 + 6 个实现类

| 语言 | 源文件名 | 编译命令 | 运行命令 | 编译缓存 |
| --- | --- | --- | --- | --- |
| Java | `Main.java` | `/usr/bin/javac Main.java` | `/usr/bin/java Main` | `Main.class` |
| Python | `main.py` | null（无需编译） | `/usr/bin/python3 main.py` | 无 |
| C++ | `main.cpp` | `/usr/bin/g++ -o main main.cpp -O2` | `main` | `main` |
| C | `main.c` | `/usr/bin/gcc -o main main.c -O2` | `main` | `main` |
| Go | `main.go` | `/usr/bin/go build -o main main.go` | `main` | `main` |
| JavaScript | `main.js` | null（无需编译） | `/usr/bin/node main.js` | 无 |

**关键发现 1**：Java 策略强制源文件名为 `Main.java`，类名必须是 `Main`。如果用户提交的代码使用其他类名（如 `Solution`），编译会直接失败。这个约束没有在前端或 API 层提前提示用户。

**关键发现 2**：`JudgeLanguage` 枚举中的 `needCompile()` 方法返回 `JAVA || CPP || C || GO`，但 `JudgeStrategy` 接口的 `getCompiledFileNames()` 默认返回 `null`。Python 和 JavaScript 策略不需要覆盖此方法——逻辑一致。但 `JavaScriptJudgeStrategy` 没有在列表中（上面未展示），需要确认是否已实现。

**关键发现 3**：C/C++ 编译命令使用 `-O2` 优化。这是标准竞赛实践，但 `-O2` 可能导致某些未定义行为（如整数溢出、空指针解引用）的代码产生不同结果。调试构建和优化构建的行为差异可能导致本地能跑但 OJ 上 WA。

**关键发现 4**：Go 策略的编译命令使用 `go build`，默认会启用模块模式。如果用户的代码依赖外部模块，编译会失败。当前没有 `GOFLAGS` 或 `GO111MODULE` 环境变量配置。

### 五、赛事规则校验与 ACM 榜单深度分析

**源码**：`ContestRuleValidator.java`（32 行）+ `ContestRankingCalculator.java`（115 行）

```
ContestRuleValidator.checkCanSubmit(contest, now):
  contest == null || contest.status != 2 → "赛事未开始"
  startTime/endTime == null → "赛事时间配置不完整"
  now < startTime || now > endTime → "不在赛事提交时间窗口内"

ContestRankingCalculator.calculate(contest, submissions, participants):
  1. 初始化 UserStat（含 problemStats: HashMap<Long, ProblemStat>）
  2. 按 createTime + id 排序所有提交
  3. 逐条处理:
     → 已解决 → 跳过
     → accepted → solved=true, solvedCount++, penalty += acMinutes + wrongBeforeAc*20
     → wrong_answer → wrongBeforeAc++
  4. 构建 ContestRankingItem 列表
  5. 排序: solvedCount DESC → penalty ASC → lastAcTime ASC → userId ASC
  6. 赋值排名 (rank = i+1)
```

**关键发现 1**：`ContestRuleValidator` 只检查状态 `2`（进行中），不检查状态 `1`（即将开始）。这意味着"即将开始"的赛事不能提交——即使当前时间在赛事窗口内。这是合理的，但错误提示"赛事未开始"可能让用户困惑，因为赛事可能已经在日历上但还没切换到进行中状态。

**关键发现 2**：`ContestRankingCalculator` 中 `wrongBeforeAc` 只计算 `wrong_answer` 类型的提交。`time_limit_exceeded`、`memory_limit_exceeded`、`runtime_error` 等失败类型不计入罚时。这与标准 ACM 规则不同——标准规则下所有非 AC 提交（包括 TLE、RTE）都计入罚时。

**关键发现 3**：榜单计算是**内存全量计算**——每次请求榜单都从数据库加载所有提交和参与者，然后在内存中重新计算。如果赛事有数千参与者和数万提交，每次请求榜单的数据库查询和计算开销较大。没有缓存机制。

### 六、赛事评分预估深度分析

**源码**：`OjContestRankingServiceImpl.java`（109 行）

```
enrichRatingEstimates(ranking):
  对每条榜单项:
    rankScore = round((total - rank + 1) * 100 / total)
    solveScore = round(solvedCount * 100 / maxSolvedCount)
    speedScore = solvedCount <= 0 → 0 : max(0, 100 - min(100, penalty/10))
    performanceScore = round(solveScore*0.55 + rankScore*0.3 + speedScore*0.15)

    rankDelta = round((total + 1 - 2*rank) * 60 / total)
    solveDelta = solvedCount <= 0 → -20 : min(40, solvedCount*10)
    penaltyDelta = solvedCount <= 0 → 0 : -min(25, penalty/120)
    ratingChange = clamp(rankDelta + solveDelta + penaltyDelta, -80, 120)

    ratingAfter = 1500 + ratingChange
```

**关键发现 1**：所有参赛者的初始评分硬编码为 1500。这意味着第一次参赛所有人起点相同。但真正的 ELO 系统需要跟踪历史评分——当前没有 `user_contest_rating` 表，每次都从 1500 开始计算。

**关键发现 2**：评分变化公式 `ratingChange = rankDelta + solveDelta + penaltyDelta` 是简单的线性公式，不是 ELO 或 TrueSkill。参数（`-80` 到 `120`）硬编码在源码中。

**关键发现 3**：`penalty` 单位是分钟，`penalty/10` 用于 speedScore 计算，`penalty/120` 用于 penaltyDelta 计算。两个除数不同——一个是"每 10 分钟扣 1 分速度分"，另一个是"每 120 分钟扣 1 分评分"。没有注释解释为什么选择这两个特定数值。

### 七、评论系统深度分析

**源码**：`OjProblemCommentServiceImpl.java`（227 行）

```
getComments(problemId, request):
  → 查总数 countByProblemId
  → 分页查一级评论 selectByProblemId
  → 每条一级评论 → convertToResponseWithReplies:
    → convertToResponse (设 isLiked)
    → 加载最多 2 条回复 selectRepliesByCommentId(limit=2)
    → hasMoreReplies = replyCount > 2

createComment(problemId, request):
  → StpUserUtil.checkLogin()
  → SaTokenUserUtil.getCurrentUserUsername("用户" + currentUserId)
  → parentId = 0  ← 一级评论

replyComment(commentId, request):
  → StpUserUtil.checkLogin()
  → 检查父评论存在且 status==1
  → topCommentId = parentId==0 ? commentId : parentComment.parentId
  → replyToUserName = 先取"用户"+replyToUserId, 再尝试查 authorName
  → insert reply → updateReplyCount(topCommentId, +1)

likeComment(commentId):
  → 检查评论存在
  → 检查未点赞过
  → insert like → updateLikeCount(+1)

unlikeComment(commentId):
  → 检查评论存在
  → 检查已点赞
  → delete like → updateLikeCount(-1)
```

**关键发现 1**：`likeComment` 和 `unlikeComment` 方法中，先查询评论是否存在，再检查点赞状态。两个操作之间没有同步——并发场景下，两个请求可能同时通过"未点赞"检查，都尝试插入点赞记录。数据库唯一索引会阻止第二次插入，但抛出的异常未被捕获，会变成 500 错误而非友好提示。

**关键发现 2**：评论的 `authorName` 在创建时冗余写入。如果用户后续修改昵称，评论中的 `authorName` 不会更新。这是典型的反规范化设计——读取更快但一致性弱。

**关键发现 3**：`replyComment` 中步骤 `replyToUserName = "用户" + replyToUserId` 是 fallback，然后尝试查 `authorName`。但查询条件是 `selectById(commentId)` 而非 `selectById(request.getReplyToUserId())`——这意味着取的是**被回复评论的作者名**而非**被回复用户的真实用户名**。如果被回复评论的 `authorName` 为 null（数据异常），则 fallback 到硬编码值。

**关键发现 4**：`updateLikeCount` 和 `updateReplyCount` 使用 `+=1` / `-=1` SQL 语句。这是并发安全的（数据库行级锁），但如果 `likeCount` 或 `replyCount` 因为数据异常变成负数，`unlikeComment` 的 `-1` 操作会继续减少。

### 八、题目服务深度分析

**源码**：`OjProblemServiceImpl.java`（110 行）

```
createProblem(problem, tagIds):
  → acceptedCount=0, submitCount=0, status=0(default hidden)
  → insert(problem)
  → 逐条 insertProblemTag(problemId, tagId)

updateProblem(id, problem, tagIds):
  → updateById(problem)
  → deleteProblemTags(id) → 逐条 insertProblemTag

deleteProblem(id):
  → deleteById(id)  ← 物理删除！
  → deleteProblemTags(id)
  → testCaseMapper.deleteByProblemId(id)  ← 连带删除测试用例

getDailyProblem():
  → countPublic → offset = abs(LocalDate.now().hashCode()) % count
  → selectPublicByOffset(offset)
```

**关键发现 1**：`deleteProblem` 是**物理删除**——直接 `DELETE FROM oj_problem WHERE id=?`。这意味着删除题目后，所有关联的提交记录的 `problemId` 变成悬空引用。如果用户查看历史提交详情，关联的题目标题将无法显示（`problemTitle` 为 null）。

**关键发现 2**：`getDailyProblem` 使用 `LocalDate.now().hashCode()` 的绝对值对公开题目总数取模。`hashCode()` 在 Java 中对 `LocalDate` 是确定性的（基于 epochDay），所以同一天内结果确定。但这是"伪随机"——不同 JVM 版本的 `LocalDate.hashCode()` 实现可能不同。如果集群部署不同 JVM 版本，不同节点可能选出不同的每日一题。

**关键发现 3**：`createProblem` 中标签关联是逐条插入的（for 循环 + `insertProblemTag`），而非批量插入。如果标签数量多（如 10+），数据库交互次数较多。

### 九、深度发现与坑点

#### 9.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | 提交数在判题前 +1 | `OjSubmissionServiceImpl.submitCode:72` | system_error 时提交数虚高 |
| BUG-2 | 首次 AC 存在竞态 | `JudgeService.judge:158-161` | 同题多提交同时 AC 会重复发放积分 |
| BUG-3 | JudgeLanguage.of 抛 IllegalArgumentException | `OjSubmissionServiceImpl.submitCode:49` | 用户看到 500 而非友好错误 |
| BUG-4 | 物理删除题目 | `OjProblemServiceImpl.deleteProblem:68` | 关联提交的 problemId 悬空 |
| BUG-5 | 点赞并发无保护 | `OjProblemCommentServiceImpl.likeComment:140-143` | 并发点赞导致 500 而非友好提示 |
| BUG-6 | WA 罚时不包含 TLE/MLE/RTE | `ContestRankingCalculator` | 与标准 ACM 罚时规则不一致 |
| BUG-7 | 每日一题 hashCode 跨 JVM 不稳定 | `OjProblemServiceImpl.getDailyProblem:100` | 不同节点可能选出不同题目 |
| BUG-8 | 评分系统无持久化 | `OjContestRankingServiceImpl` | 每次参赛都从 1500 起算 |

#### 9.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | @Async 无线程池限制 | 使用默认 SimpleAsyncTaskExecutor，无上界 |
| RISK-2 | 榜单全量计算无缓存 | 每次请求都查全部提交+参与者+重算 |
| RISK-3 | go-judge stdout 截断 10KB | 大输出题目可能被误判 WA |
| RISK-4 | 评论 authorName 冗余不更新 | 用户改昵称后评论中名字不同步 |
| RISK-5 | Java 强制 Main 类名 | 无提前提示，编译错误体验差 |
| RISK-6 | procLimit 硬编码 50 | 不可按题目配置进程数限制 |
| RISK-7 | 编译缓存文件引用失效 | go-judge 重启后 fileId 全部失效 |

#### 9.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 策略模式支持 6 种语言 | JudgeStrategy 接口统一，新增语言只需添加实现类 |
| H-2 | copyOutCached 编译缓存 | 编译产物缓存到 go-judge，多次运行只需编译一次 |
| H-3 | 三种运行模式分离 | Playground / 自测 / 正式提交，各司其职 |
| H-4 | ACM 排名 4 级排序 | solvedCount → penalty → lastAcTime → userId |
| H-5 | 赛事评分预估公式 | 融合排名、解题、速度三个维度加权 |
| H-6 | 评论两级结构 + 懒加载 | 一级评论预载 2 条回复，hasMoreReplies 控制展开 |
| H-7 | 点赞幂等设计 | 先查再插再更新计数，取消时反向操作 |
| H-8 | compareOutput 统一比较 | strip() 忽略首尾空白，JudgeService 和 CodeRunnerService 共用逻辑 |
| H-9 | go-judge 安全沙箱 | CPU/内存/进程数/文件系统全隔离 |
| H-10 | 积分发放容错 | catch 异常只记日志不回滚 AC 状态 |

#### 9.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 提交主流程 | `OjSubmissionServiceImpl.java` — submitCode + validateContestSubmit |
| 判题全链路 | `JudgeService.java` — @Async + 编译 + 逐用例运行 + 比对 + AC 积分 |
| 沙箱通信 | `GoJudgeClient.java` — REST /run + /file/{id} + 请求体构建 |
| Playground | `CodeRunnerService.java` — run + selfTest 双模式 |
| 语言策略 | `judge/strategy/*.java` — 6 种语言编译/运行命令 |
| 赛事校验 | `ContestRuleValidator.java` — checkCanSubmit 状态+时间窗口 |
| ACM 榜单 | `ContestRankingCalculator.java` — UserStat + ProblemStat + 4 级排序 |
| 评分预估 | `OjContestRankingServiceImpl.java` — performanceScore + ratingChange |
| 题目 CRUD | `OjProblemServiceImpl.java` — 创建/更新/删除/每日一题 |
| 评论系统 | `OjProblemCommentServiceImpl.java` — 两级评论 + 点赞 + 回复 |
| 测试用例 | `OjTestCaseServiceImpl.java` — 排序号自增 + isSample 默认值 |
| 题解管理 | `OjSolutionServiceImpl.java` — 排序号自增 + 按 problemId 查询 |
| 判题配置 | `OjJudgeProperties.java` + `OjConfig.java` — goJudgeUrl + RestTemplate |
| 提交状态枚举 | `SubmissionStatus.java` — 9 种状态 + of() 默认 SYSTEM_ERROR |
| 语言枚举 | `JudgeLanguage.java` — 6 种语言 + needCompile() |

## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | OJ 模块依赖公共底座的统一响应、并发工具和异常处理 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 提交代码、参加赛事需要用户登录态 |
| [积分与抽奖](/modules/points) | 强依赖 | 首次 AC 题目会触发积分发放（PointsType.OJ_AC） |
| [用户账户与个人中心](/modules/user-account) | 被依赖 | 用户刷题统计、排行榜依赖用户信息 |
| [社区帖子](/modules/community) | 间接关联 | OJ 题解和评论系统与社区模块类似 |
| [Docker 与服务部署](/operations/docker) | 参考 | go-judge 沙箱部署配置 |
| [前端渲染安全](/reference/frontend-rendering-security) | 参考 | 代码展示和题解内容安全规范 |
