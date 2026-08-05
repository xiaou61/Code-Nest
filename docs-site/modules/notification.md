# 通知中心

`xiaou-notification` 完整拥有通知发布、查询、模板、持久化、异步执行和 Web DTO。其他领域只通过 `NotificationPublisher` 发布消息，不依赖通知实体、Mapper 或内部服务。

## 模块接口

```text
业务模块
  → NotificationPublisher
      → NotificationPublisherImpl
          → NotificationService
              → NotificationMapper / NotificationUserReadRecordMapper
```

| 公开类型 | 用途 |
| --- | --- |
| `NotificationPublisher` | 同步发布单条通知、异步批量发布 |
| `NotificationCommand` | 与持久化无关的发布命令及 personal/system/community/interview/announcement 工厂 |

`NotificationCommand` 会归一化消息类型和优先级。调用方不需要知道发送者 ID、未读初始状态、时间字段或 Mapper 写入细节。

## 所有权

以下实现全部位于 `xiaou-notification`：

| 目录 | 内容 |
| --- | --- |
| `api/` | 跨模块发布接口和命令 |
| `controller/` | 用户端与管理端 HTTP 适配 |
| `service/` | 发布、用户读取、管理操作和模板逻辑 |
| `domain/` | 通知、模板、配置和阅读记录实体 |
| `mapper/` + `resources/mapper/` | MyBatis 接口与 SQL |
| `enums/` | 类型、来源、优先级和状态 |
| `config/` | 通知专用有界异步执行器 |
| `dto/` | 请求与响应契约 |

`xiaou-common` 不再保存任何通知实体、Mapper、缓存工具或静态发布入口。

## HTTP 契约

### 用户端 `/api/notification`

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/list` | 分页查询当前用户消息 |
| GET | `/unread-count` | 查询未读数 |
| GET | `/{id}` | 读取详情并按规则标记已读 |
| POST | `/mark-read` | 单条或批量标记已读 |
| POST | `/mark-all-read` | 全部标记已读 |
| POST | `/delete` | 删除当前用户消息 |

### 管理端 `/api/admin/notification`

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/announcement` | 发布公告 |
| POST | `/statistics` | 查询通知统计 |
| POST | `/list` | 查询全部消息 |
| POST | `/batch-send` | 批量发送个人消息 |
| POST | `/delete/{id}` | 管理员删除消息 |
| GET/POST | `/templates` | 查询或创建模板 |
| PUT/DELETE | `/templates/{id}` | 更新或删除模板 |

## Web DTO

Controller 不直接暴露持久化实体：

| DTO | 稳定字段 |
| --- | --- |
| `NotificationResponse` | `id`、内容/类型/优先级、来源、状态、`readTime`、`createdTime`、`updatedTime` |
| `NotificationTemplateRequest` | `code`、`name`、`titleTemplate`、`contentTemplate`、`isEnabled` |
| `NotificationTemplateResponse` | 请求字段加 `id`、`createdTime`、`updatedTime` |

双前端从 `@code-nest/api-contract` 导入对应 TypeScript 类型。时间字段统一为 `createdTime`，模板字段不再维护 `createTime`、`title` 或 `content` 等历史别名。

## 已读模型

个人通知与公告使用不同持久化语义：

- 个人通知通过通知主表的 `receiverId` 和 `status` 记录归属与已读状态。
- 公告的 `receiverId` 为空，每个用户的阅读状态写入 `notification_user_read_record`。
- 用户只能读取和删除自己的个人通知；公告详情可读，但已读状态按用户隔离。

## 异步策略

批量发布使用 `notificationExecutor`。执行器有固定核心/最大线程数、有界队列、CallerRuns 拒绝策略和关闭等待时间，避免通知突发流量无限创建线程或静默丢任务。

## 依赖规则

- 调用方只能导入 `com.xiaou.notification.api`。
- 通知 Controller 只能返回 DTO，不能把 `domain` 类型作为 Web 契约。
- 通知数据库表、Mapper 和枚举只由通知模块维护。
- 批量发送前通过 `xiaou-user-api` 校验接收用户，不反向依赖 `xiaou-user` 实现。

这些规则由 `scripts/check-architecture.py` 持续检查。

## 源码导航

| 路径 | 内容 |
| --- | --- |
| `xiaou-notification/src/main/java/com/xiaou/notification/api/` | 发布接口 |
| `xiaou-notification/src/main/java/com/xiaou/notification/controller/` | HTTP 入口 |
| `xiaou-notification/src/main/java/com/xiaou/notification/service/` | 通知用例与实现 |
| `xiaou-notification/src/main/java/com/xiaou/notification/dto/` | Web 请求/响应 DTO |
| `xiaou-notification/src/main/resources/mapper/` | SQL 映射 |

## 验证

```bash
mvn -pl xiaou-notification -am test
npm --prefix vue3-user-front run test:contracts
npm --prefix vue3-admin-front run test:contracts
python scripts/check-architecture.py
```
