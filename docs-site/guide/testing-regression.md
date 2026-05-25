# 测试与回归

这页说明 Code Nest 里"改完之后怎么证明没坏"。它和 [发布前验证](/guide/release-verification) 的区别是：发布前验证告诉你发版前跑什么，本页告诉你日常开发时怎么选择测试层级、怎么补回归样例、怎么记录结果。

如果你已经明确知道这次主要改的是哪个模块，也可以先对照 [模块最小回归矩阵](/reference/module-regression-matrix)，快速拿到该模块最低不能漏掉的回归组。

如果你现在卡住的是"这个模块到底该补哪类失败态"，可以再配合 [异常路径与失败态索引](/reference/failure-paths) 一起看，补测时不要只跑成功路径。

如果你要确认"成功后用户端有没有真的看到结果"，可以顺手看 [事件、通知与回流索引](/reference/event-backflow-index)，把通知、统计、日志和 ACK 一起补进回归。

## 分层思路

| 层级 | 目的 | 适合场景 |
| --- | --- | --- |
| 编译构建 | 证明代码能编译、依赖能解析 | 每次后端或前端改动 |
| 单元测试 | 证明纯逻辑、状态机、规则计算没坏 | AI、OJ、积分、学习资产、SQL 优化 |
| 集成测试 | 证明 Controller、Service、Mapper、配置能协作 | 接口、数据库脚本、权限 |
| 页面烟测 | 证明真实页面能走通 | 用户端、管理端、上传、聊天 |
| 依赖联调 | 证明外部服务可用 | RAG、go-judge、Redis、MySQL、对象存储 |
| 回归记录 | 证明结果可追溯 | 版本合并、已知问题修复 |

不要把"构建通过"当成"业务可用"。构建只覆盖语法和依赖，不能覆盖业务规则、权限、外部依赖和页面状态。

## 后端基础命令

后端聚合构建：

```powershell
mvn -pl xiaou-application -am clean package -DskipTests
```

按模块跑测试：

```powershell
mvn -pl xiaou-ai -am test
```

```powershell
mvn -pl xiaou-oj -am test
```

```powershell
mvn -pl xiaou-mock-interview -am test
```

```powershell
mvn -pl xiaou-learning-asset -am test
```

```powershell
mvn -pl xiaou-sql-optimizer -am test
```

```powershell
mvn -pl xiaou-system -am test
```

如果只改了某个测试类，可以加 `-Dtest=类名` 缩小范围。

## 现有测试重点

| 模块 | 现有测试方向 | 测试类数量 |
| --- | --- | --- |
| `xiaou-ai` | Prompt、Schema、RAG、Graph、AI 回归、指标 | 35+ |
| `xiaou-oj` | 赛事规则、排名、提交、Controller、SQL 脚本 | 6 |
| `xiaou-mock-interview` | 模拟面试、求职作战台、Career Loop 状态机 | 5 |
| `xiaou-learning-asset` | 学习资产转化、发布、审核、统计 | 5 |
| `xiaou-sql-optimizer` | SQL 优化服务 | 1 |
| `xiaou-system` | AI 配置、回归运行状态 | 2 |
| `llamaindex-service` | Python sidecar 接口和服务逻辑 | — |
| `vue3-user-front` | 适配器类测试（home-data、oj-contest、career-loop） | 3 |

前端仓库当前没有统一 `npm test` 脚本。`vue3-user-front/tests` 下有适配器类测试文件，但日常最低验证仍应以 `npm run build` 和真实页面烟测为准。

## 实战：现有测试模式详解

### 状态机单元测试

项目中凡是涉及状态流转的功能，测试的第一优先级是"状态推进正确、回退被拒绝"。Career Loop 状态机是一个典型例子：

```java
// xiaou-mock-interview/src/test/java/.../CareerLoopStateMachineTest.java
class CareerLoopStateMachineTest {
    private final CareerLoopStateMachine machine = new CareerLoopStateMachine();

    @Test
    void shouldRejectBackwardTransition() {
        // 反向推进应抛出 BusinessException
        assertThrows(BusinessException.class, () ->
            machine.next(CareerLoopStageEnum.PLAN_READY, CareerLoopStageEnum.JD_PARSED));
    }

    @Test
    void shouldBeIdempotentForSameStage() {
        // 同阶段推进应幂等
        assertEquals(CareerLoopStageEnum.PLAN_READY,
            machine.next(CareerLoopStageEnum.PLAN_READY, CareerLoopStageEnum.PLAN_READY));
    }

    @Test
    void shouldAllowForwardTransition() {
        // 正向推进应成功
        assertEquals(CareerLoopStageEnum.REVIEWED,
            machine.next(CareerLoopStageEnum.INTERVIEW_DONE, CareerLoopStageEnum.REVIEWED));
    }
}
```

关键测试维度：

| 维度 | 测试什么 | 用什么断言 |
| --- | --- | --- |
| 正向推进 | 状态从 A 到 B 成功 | `assertEquals(expected, actual)` |
| 反向推进 | 状态从 A 回退到 B 被拒 | `assertThrows(BusinessException.class, ...)` |
| 幂等 | 同阶段推进不报错 | `assertEquals(same, same)` |

适用模块：Career Loop、OJ 赛事状态、学习资产审核流程、版本发布流程、文件迁移流程。

### 规则校验单元测试

OJ 赛事规则校验是另一个典型模式——纯逻辑、不依赖数据库、用构造的输入测试边界：

```java
// xiaou-oj/src/test/java/.../ContestRuleValidatorTest.java
class ContestRuleValidatorTest {
    @Test
    void shouldRejectSubmitWhenContestNotRunning() {
        OjContest contest = new OjContest()
            .setStatus(1)  // 赛事还没开始
            .setStartTime(LocalDateTime.now().plusHours(1))
            .setEndTime(LocalDateTime.now().plusHours(3));
        assertThrows(BusinessException.class,
            () -> ContestRuleValidator.checkCanSubmit(contest, LocalDateTime.now()));
    }

    @Test
    void shouldRejectSubmitWhenOutOfContestWindow() {
        LocalDateTime now = LocalDateTime.now();
        OjContest contest = new OjContest()
            .setStatus(2)  // 赛事进行中
            .setStartTime(now.minusHours(2))
            .setEndTime(now.minusHours(1));  // 但已超时
        assertThrows(BusinessException.class,
            () -> ContestRuleValidator.checkCanSubmit(contest, now));
    }

    @Test
    void shouldAllowSubmitWhenContestRunningAndInWindow() {
        LocalDateTime now = LocalDateTime.now();
        OjContest contest = new OjContest()
            .setStatus(2)
            .setStartTime(now.minusMinutes(30))
            .setEndTime(now.plusMinutes(30));
        assertDoesNotThrow(() -> ContestRuleValidator.checkCanSubmit(contest, now));
    }
}
```

这种模式的核心思路：**构造最小输入 → 测试每个边界 → 不依赖 Spring 容器**。

新增二开功能时，如果你的 Service 有状态校验、唯一性校验或时间窗口校验，优先用这种模式补单元测试。

### 跨模块集成测试（Mockito）

学习资产发布涉及多模块协作（flashcard、knowledge、notification），测试使用 Mockito 模拟外部依赖：

```java
// xiaou-learning-asset/src/test/java/.../LearningAssetPublishServiceTest.java
@ExtendWith(MockitoExtension.class)
class LearningAssetPublishServiceTest {
    @Mock private LearningAssetRecordMapper recordMapper;
    @Mock private LearningAssetCandidateMapper candidateMapper;
    @Mock private FlashcardDeckService flashcardDeckService;
    @Mock private FlashcardService flashcardService;
    @Mock private NotificationService notificationService;
    // ... 更多 @Mock

    @InjectMocks
    private LearningAssetPublishServiceImpl publishService;

    @Test
    void publishShouldCreateDeckAndSubmitReviewCandidates() {
        // 1. 构造测试数据
        LearningAssetRecord record = new LearningAssetRecord()
            .setId(101L).setUserId(1001L)
            .setStatus(LearningAssetRecordStatus.PENDING_CONFIRM.name());

        // 2. 设置 Mock 返回
        when(recordMapper.selectByUserIdAndId(1001L, 101L)).thenReturn(record);
        when(candidateMapper.selectByRecordId(101L)).thenReturn(List.of(...));
        when(flashcardDeckService.createDeck(any(), eq(1001L))).thenReturn(301L);

        // 3. 执行
        LearningAssetPublishResponse response = publishService.publish(1001L, 101L, List.of(201L, 202L));

        // 4. 断言结果
        assertEquals(1, response.getPublishedCount());
        assertEquals(301L, response.getFlashcardDeckId());

        // 5. 验证副作用（通知已发送）
        verify(notificationService).sendNotification(argThat(n ->
            n.getReceiverId().equals(1001L) && "学习资产已发布".equals(n.getTitle())));
    }
}
```

这种模式的要点：

| 要点 | 说明 |
| --- | --- |
| `@Mock` | 模拟 Mapper 和外部 Service，不启动真实容器 |
| `@InjectMocks` | 自动把 Mock 注入被测 Service |
| `when(...).thenReturn(...)` | 控制依赖返回值，隔离测试 |
| `verify(...)` | 验证副作用（通知、日志、状态更新） |
| `argThat(...)` | 精确验证通知内容 |

适用场景：凡是涉及跨模块调用（通知、积分、文件、AI）的 Service，都适合用 Mockito 集成测试。

### AI 回归评测框架

AI 场景使用自建的评测框架（`AiSceneRegressionEvalTest`），核心是"场景 + 输入 + 期望输出 + 断言":

```text
AiSceneRegressionEvalTest
  → 遍历每个 EvalScenario（community_summary, interview_generate_questions, ...）
    → 每个 scenario 从 JSON fixture 加载测试用例
      → 执行场景，获取实际输出
        → 对比期望值（字段存在性、类型、值范围）
```

AI 回归的完整命令：

```powershell
mvn -pl xiaou-ai -am "-Dtest=AiSceneRegressionEvalTest,InterviewGraphTest,JobBattleGraphTest,SqlOptimizeGraphTest,AiSqlOptimizeServiceImplTest,AiPromptSpecTest,AiStructuredOutputValidatorTest,LlamaIndexClientTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

改这些内容时要跑 AI 回归：

| 变更 | 额外检查 |
| --- | --- |
| Prompt | 场景输出是否仍满足预期 |
| Schema | 必填字段、枚举、数组结构是否稳定 |
| RAG Profile | sidecar 健康、召回内容和空召回兜底 |
| Graph | 节点顺序、失败分支、结构化输出 |
| 模型配置 | 管理端 Runtime、Prompt 调试、回归记录 |

如果新增 AI 场景，要同步 [AI Runtime](/modules/ai-runtime)、[AI Schema 与治理](/reference/ai-schemas) 和回归样例。

## 补测试：新增模块时怎么写

如果你正在新增一个 `xiaou-*` 模块，以下是建议的测试层级和模式：

### 第 1 层：状态机/规则校验（纯逻辑）

如果你的模块有状态流转或规则校验，优先写这种测试：

```java
// 状态机测试模板
class YourStateMachineTest {
    private final YourStateMachine machine = new YourStateMachine();

    @Test
    void shouldAllowForwardTransition() {
        assertEquals(YourStatus.PUBLISHED,
            machine.next(YourStatus.DRAFT, YourStatus.PUBLISHED));
    }

    @Test
    void shouldRejectBackwardTransition() {
        assertThrows(BusinessException.class, () ->
            machine.next(YourStatus.PUBLISHED, YourStatus.DRAFT));
    }

    @Test
    void shouldRejectInvalidTransition() {
        assertThrows(BusinessException.class, () ->
            machine.next(YourStatus.DRAFT, YourStatus.DELETED));
    }
}
```

### 第 2 层：Service 集成测试（Mockito）

如果你的 Service 调用了 Mapper、外部 Service 或需要通知，用 Mockito：

```java
@ExtendWith(MockitoExtension.class)
class YourServiceTest {
    @Mock private YourMapper yourMapper;
    @Mock private NotificationService notificationService;
    @InjectMocks private YourServiceImpl yourService;

    @Test
    void createShouldCheckUniqueAndNotify() {
        when(yourMapper.selectByName("test")).thenReturn(null);
        when(yourMapper.insert(any())).thenReturn(1);

        Long id = yourService.create(buildCreateRequest());

        assertNotNull(id);
        verify(notificationService).sendNotification(any());
    }

    @Test
    void createShouldRejectDuplicateName() {
        when(yourMapper.selectByName("test")).thenReturn(existingEntity);
        assertThrows(BusinessException.class, () -> yourService.create(buildCreateRequest()));
    }
}
```

### 第 3 层：SQL Schema 测试

如果新增了表或字段，补 Schema 验证测试确保 SQL 脚本和 domain 一致：

```java
// 参考 xiaou-learning-asset/src/test/java/.../LearningAssetSchemaSqlTest.java
```

### 测试目录结构

```text
xiaou-your-module/src/test/java/com/xiaou/your/
├── statemachine/     ← 状态机测试（纯逻辑）
│   └── YourStateMachineTest.java
├── service/          ← Service 集成测试（Mockito）
│   └── YourServiceImplTest.java
├── controller/       ← Controller 层测试（可选）
│   └── YourControllerWebTest.java
└── sql/              ← Schema SQL 验证
    └── YourSchemaSqlTest.java
```

## OJ 回归

OJ 至少要覆盖三层：

| 层级 | 验证 |
| --- | --- |
| 规则 | 赛事时间、报名、语言、排名、罚时 |
| 后端 | 题目、测试用例、提交、题解、赛事接口 |
| 外部依赖 | go-judge 可访问，编译器可用 |

常用命令：

```powershell
mvn -pl xiaou-oj -am test
```

页面烟测：

1. 打开 `/oj/problem/:id`。
2. 点运行或自测。
3. 点正式提交。
4. 查看提交详情和状态。

如果 go-judge 未启动，只能说"页面和接口构建已验证"，不能说判题链路已验证。

## 前端回归

用户端构建：

```powershell
cd vue3-user-front
npm run build
```

管理端构建：

```powershell
cd vue3-admin-front
npm run build
```

前端页面烟测至少看：

| 场景 | 检查 |
| --- | --- |
| 新页面 | 路由可打开，刷新不 404 |
| 新接口 | Network URL、状态码、响应结构正确 |
| 登录态 | 未登录、Token 过期、权限不足都有处理 |
| 表单 | 必填、非法输入、提交成功、提交失败 |
| 列表 | 空态、分页、筛选、重置 |
| 上传 | 成功、失败、URL 回显、权限 |
| `v-html` | 走 `renderMarkdown` 或 `escapeHtml + sanitizeHtml` |

前端 `lint` 脚本当前带 `--fix`，会直接改文件。它适合本地整理风格，不适合作为只读 CI 检查口径。

## 文档回归

文档站构建：

```powershell
cd docs-site
npm run build
```

当前 `.github/workflows/docs-site.yml` 会在 `docs-site/**` 或 workflow 变更时执行：

1. Node 20。
2. `npm ci`。
3. `npm run build`。
4. 上传 `docs-site/.vitepress/dist`。

文档改动至少检查：

| 检查 | 说明 |
| --- | --- |
| 内部链接 | VitePress 构建能发现大多数断链 |
| 标题层级 | 页面目录是否清楚 |
| 路线图 | 新增大页或新阶段入口要更新当前文档计划 |
| 导航 | 新页面要接入 sidebar 或相关索引 |
| 证据 | 命令、路径、接口不要凭记忆写 |

## 安全回归检查

安全相关的测试不只看"接口返回 200"，还要看：

| 测试场景 | 验证点 | 怎么测 |
| --- | --- | --- |
| 管理端未登录 | 返回 701/702 | 用户端 Token 调管理端接口 |
| 非管理员调管理接口 | 返回 703 | 普通用户 Token 调 `@RequireAdmin` 接口 |
| 前端传 userId | 被忽略 | 发送伪造 userId，确认后端从 Token 取 |
| 私有文件访问 | 返回拒绝或空 | 未登录或无权限用户访问 `is_public=0` 文件 |
| XSS 渲染 | 被净化 | 在内容中插入 `&lt;script&gt;`，确认前端 `sanitizeHtml` 过滤 |
| ws-ticket 复用 | 连接失败 | 用同一 ticket 连两次 WebSocket |
| 文件类型限制 | 返回拒绝 | 上传 `exe` 或 `sh` 文件 |

更多安全边界细节见 [权限与安全边界](/guide/security-boundaries) 和 [权限注解与角色边界索引](/reference/permission-boundaries)。

## 结果怎么记录

验证结果建议写成下面这种格式：

```text
验证范围：用户端聊天 ws-ticket 和消息发送
已执行：用户端 build、后端 package、登录后进入 /chat、发送文本消息
通过项：ws-ticket 获取成功，WebSocket CONNECT 成功，消息 ACK 成功
未验证：多实例广播、禁言到期恢复
依赖状态：Redis 和 MySQL 已启动，未启动监控
```

适合沉淀到：

1. [验证记录与已知问题](/manuals/verified-scenarios)。
2. 对应模块页"验证清单"。
3. PR 描述或版本记录。

如果你已经开始以模块 owner 视角维护某个模块，建议继续配合 [模块值守与回归手册](/guide/module-owner-playbook) 把这些验证沉淀成固定回归组和周度值守节奏。

## 选择测试的速查表

| 改了什么 | 至少跑什么 |
| --- | --- |
| 只改文档 | `docs-site npm run build` |
| 后端普通业务 | `mvn -pl xiaou-application -am clean package -DskipTests` + 相关接口 |
| AI Runtime | AI 回归命令 + 管理端场景调试 |
| OJ | `mvn -pl xiaou-oj -am test` + go-judge 联调 |
| 模拟面试/求职 | `mvn -pl xiaou-mock-interview -am test` + 页面流程 |
| 学习资产 | `mvn -pl xiaou-learning-asset -am test` + 审核/发布流程 |
| 用户端页面 | 用户端 build + 真实路由 |
| 管理端页面 | 管理端 build + 真实后台路由 |
| 文件上传 | 后端构建 + 上传 + `/api/files/**` 访问 |
| WebSocket | 后端构建 + 用户端构建 + ws-ticket + ACK/失败态 |
| 安全边界 | 未登录、非管理员、私有文件、XSS、票据复用 |
