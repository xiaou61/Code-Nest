# 常见二开场景

这页把“我想扩展 Code Nest”拆成几个最常见的二开场景。每个场景都给出源码落点、需要同步的文档和最低验证项。

如果你还没确定功能边界，先读 [功能开发流程](/guide/feature-development)。如果只是想找某个模块在哪里，先读 [模块总览](/modules/) 和 [源码地图](/reference/source-map)。

## 场景总览

| 场景 | 适合例子 | 主要入口 |
| --- | --- | --- |
| 新增用户端功能 | 新学习工具、新个人页面、新内容页 | 用户端路由、用户端 API、业务模块 |
| 新增后台管理页 | 审核列表、配置页、运营统计 | 管理端路由、后台接口、权限和日志 |
| 新增 AI 场景 | 新 Prompt、RAG 检索、结构化输出 | AI Runtime、Schema、回归 |
| 新增内容发布类型 | 新动态类型、新社区内容、新作品形态 | 内容矩阵、敏感词、通知、文件 |
| 新增积分规则 | 发帖奖励、任务奖励、消费扣减 | 积分账户、流水、幂等 → 详见 [幂等回滚与补偿](/reference/idempotency-rollbacks-compensation) |
| 新增文件使用场景 | 附件、封面、导出文件 | 文件存储、访问权限、URL 回流 |
| 新增 WebSocket 事件 | 输入中、在线态、实时通知 | ws-ticket、事件协议、前端状态 |
| 新增运维配置 | 环境变量、Nginx、监控指标 | Docker、监控、发布前验证 |

## 新增用户端功能

适合用户可见的新页面或新操作。

| 步骤 | 做什么 |
| --- | --- |
| 1 | 在 `vue3-user-front/src/router/index.js` 增加路由 |
| 2 | 在 `vue3-user-front/src/views` 新增页面或模块目录 |
| 3 | 在 `vue3-user-front/src/api` 增加接口封装 |
| 4 | 在对应 `xiaou-*` 模块增加用户端 Controller 和 Service |
| 5 | 如果需要表，补 SQL 脚本和 Mapper |
| 6 | 同步 [前端路由索引](/reference/frontend-routes)、[API 路由索引](/reference/api-routes) 和模块页 |

最低验证：

1. 用户端 `npm run build`。
2. 后端聚合构建。
3. 打开真实路由，验证登录态、空态、成功态、失败态。

## 新增后台管理页

适合运营管理、审核、配置、统计和高风险操作。

| 步骤 | 做什么 |
| --- | --- |
| 1 | 在 `vue3-admin-front/src/router/index.js` 增加路由 |
| 2 | 在 `vue3-admin-front/src/views` 新增页面 |
| 3 | 在 `vue3-admin-front/src/api` 增加后台接口封装 |
| 4 | 后端增加管理端 Controller，并加管理鉴权 |
| 5 | 高风险写操作考虑操作日志、二次确认和状态回滚 |
| 6 | 同步 [系统运营后台](/modules/system-ops)、前端路由索引和 API 路由索引 |

最低验证：

1. 管理端 `npm run build`。
2. 后端聚合构建。
3. 管理端登录后打开页面，验证列表、筛选、写操作和权限失败态。

## 新增 AI 场景

适合模拟面试、求职、SQL 优化、摘要、推荐、问答等模型能力。

| 步骤 | 做什么 |
| --- | --- |
| 1 | 定义场景名和输入输出边界 |
| 2 | 补 Prompt 模板 |
| 3 | 补结构化输出 Schema |
| 4 | 如需知识库，补 RAG query/profile |
| 5 | 如需多步骤流程，补 Graph 编排 |
| 6 | 补回归样例和治理说明 |
| 7 | 同步 [AI Runtime](/modules/ai-runtime) 和 [AI Schema 与治理](/reference/ai-schemas) |

最低验证：

1. AI 相关测试或回归用例。
2. 管理端 `/system/ai-config` 场景调试。
3. RAG sidecar 健康和召回结果。
4. 结构化输出字段完整，不只看 HTTP 200。

## 新增内容发布类型

适合社区、动态、博客、代码工坊、学习资产等内容域扩展。

| 步骤 | 做什么 |
| --- | --- |
| 1 | 明确内容归属：社区、动态、博客、代码工坊还是学习资产 |
| 2 | 设计主表、评论/标签/附件/统计表 |
| 3 | 接入敏感词风控 |
| 4 | 如有图片或附件，接入文件存储 |
| 5 | 如有互动，接入通知和计数 |
| 6 | 如有奖励或消费，接入积分 |
| 7 | 同步内容模块页和 [社区与内容矩阵](/modules/community-content) |

最低验证：

1. 发布成功。
2. 敏感词命中时符合预期。
3. 文件或图片可访问。
4. 管理端可审核或管理。
5. 用户端列表和详情回流正常。

## 新增积分规则

适合奖励、扣减、抽奖、付费 Fork、任务激励等。

| 步骤 | 做什么 |
| --- | --- |
| 1 | 确认积分类型和业务动作 |
| 2 | 判断是否需要幂等键，避免重复奖励 → 详见 [幂等回滚与补偿](/reference/idempotency-rollbacks-compensation) |
| 3 | 写入积分流水 |
| 4 | 更新用户余额 |
| 5 | 失败时保证事务回滚 |
| 6 | 同步 [积分与抽奖](/modules/points) |

最低验证：

1. 首次操作积分变化正确。
2. 重复操作不会重复奖励或扣减。
3. 积分明细能查到来源。
4. 余额和流水一致。

## 新增文件使用场景

适合头像、封面、聊天图片、简历导出、附件等。

| 步骤 | 做什么 |
| --- | --- |
| 1 | 前端使用统一上传接口 |
| 2 | 后端通过文件存储服务落库和返回 URL |
| 3 | 业务表只保存文件 ID、URL 或业务关联，不重新实现存储 |
| 4 | 判断文件是否公开，设置 `is_public` |
| 5 | 删除业务数据时考虑文件引用关系 |
| 6 | 同步 [文件存储](/modules/file-storage) |

最低验证：

1. 上传成功。
2. 重复上传或大文件限制符合预期。
3. `/api/files/**` 可访问。
4. 私有文件不会绕过业务权限。

## 新增 WebSocket 事件

适合聊天室扩展实时状态、输入提示、在线事件或实时通知。

| 步骤 | 做什么 |
| --- | --- |
| 1 | 明确事件类型、方向和 payload |
| 2 | 服务端校验登录态和 ws-ticket |
| 3 | 前端处理 ACK、失败态和重连 |
| 4 | 如需落库，先保证消息 ID 和 tempId 映射 |
| 5 | 更新 [WebSocket 协议](/reference/websocket) |

最低验证：

1. 正常连接。
2. 事件发送和接收成功。
3. 失败事件能回填到对应本地状态。
4. 刷新或重连后状态不乱。

## 新增运维配置

适合新增环境变量、Nginx 代理、监控指标、Docker Compose 服务。

| 步骤 | 做什么 |
| --- | --- |
| 1 | 确认配置项属于后端、前端、AI sidecar、OJ 还是 Nginx |
| 2 | 提供本地默认值和生产建议 |
| 3 | 不在文档中写真实密钥 |
| 4 | 如果影响部署，更新 Docker 或 Nginx 文档 |
| 5 | 如果影响验证，更新 [发布前验证](/guide/release-verification) |

最低验证：

1. 本地配置可启动。
2. Docker 或 Nginx 示例能说明用途。
3. 配置缺失时有明确错误或降级说明。

## 代码模式速查

以下是每种场景中最常用的代码模式，可以直接复制后替换业务名。

### Controller 注解模式

```java
// 用户端 Controller — StpUserUtil 鉴权
@RestController
@RequestMapping("/user/your-module")
public class YourUserController {
    @PostMapping("/create")
    public Result<Long> create(@RequestBody YourCreateRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();  // 用户端身份
        return Result.success(yourService.create(userId, request));
    }
}

// 管理端 Controller — @RequireAdmin + @Log
@RestController
@RequestMapping("/admin/your-module")
public class YourAdminController {
    @RequireAdmin
    @Log(module = "你的模块", type = Log.OperationType.INSERT, description = "创建XX")
    @PostMapping("/create")
    public Result<Long> create(@Validated @RequestBody YourCreateRequest request) {
        return Result.success(yourService.create(request));
    }
}
```

### Service 异常模式

```java
@Service
@RequiredArgsConstructor
public class YourServiceImpl implements YourService {
    private final YourMapper yourMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(YourCreateRequest request) {
        // 1. 唯一性校验
        if (yourMapper.selectByName(request.getName()) != null) {
            throw new BusinessException("名称已存在");
        }

        // 2. 转换实体
        YourEntity entity = new YourEntity();
        BeanUtil.copyProperties(request, entity);

        // 3. 设置创建人
        entity.setCreatedBy(StpAdminUtil.getLoginIdAsLong());

        // 4. 插入
        int result = yourMapper.insert(entity);
        if (result <= 0) {
            throw new BusinessException("创建失败");
        }

        return entity.getId();
    }
}
```

### 统一返回体

```java
// 成功
return Result.success(data);
return Result.success();

// 失败
throw new BusinessException("业务错误消息");
throw new BusinessException(ResultCode.DATA_NOT_EXIST);  // 响应码详见 → [响应与错误码](/reference/response-errors)
return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), "参数错误");  // → [响应与错误码](/reference/response-errors)
```

### 分页查询模式

```java
@Override
public PageResult<YourResponse> getList(YourQueryRequest request) {
    return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
        List<YourEntity> list = yourMapper.selectByPage(request);
        return list.stream().map(this::convertToResponse).collect(Collectors.toList());
    });
}
```

### 状态流转模式

```java
// 状态校验 + 更新
public boolean publish(Long id) {
    YourEntity entity = yourMapper.selectById(id);
    if (entity == null) {
        throw new BusinessException("记录不存在");
    }
    if (entity.getStatus() != 0) {  // 只有草稿可以发布
        throw new BusinessException("只有草稿状态可以发布");
    }
    return yourMapper.updateStatus(id, 1, StpAdminUtil.getLoginIdAsLong()) > 0;
}
```

## 二开提交前自检

| 检查项 | 预期 |
| --- | --- |
| 入口 | 用户端或管理端入口已同步 |
| API | Controller 前缀、请求体、响应体清楚 |
| 数据 | 新表、新字段、状态和软删除口径清楚 |
| 横切能力 | 鉴权、文件、通知、积分、敏感词、日志已判断是否需要 |
| 安全 | `v-html`、文件公开、接口放行、管理权限已检查 |
| 验证 | 构建、接口、页面、失败态至少跑过最低集合 |
| 文档 | 模块页、索引、验证记录和路线图已同步 |


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [功能开发流程](/guide/feature-development) | 开发流程 |
| [测试与回归](/guide/testing-regression) | 测试策略 |
| [API 路由索引](/reference/api-routes) | 接口清单 |
| [模块总览](/modules/) | 各模块功能 |
