# 响应体与错误码

后端统一响应体位于 `xiaou-common/src/main/java/com/xiaou/common/core/domain/Result.java`，错误码定义位于 `ResultCode.java`，异常收敛位于 `GlobalExceptionHandler.java`。

如果你已经看到失败表现，但不确定它对应的是"权限、参数、状态机、外部依赖"哪一层，继续看 [异常路径与失败态索引](/reference/failure-paths) 会更快。

如果你看到的是"接口成功，但后续通知、统计、ACK 或日志没有跟上"，那通常不只是错误码问题，也可以继续看 [事件、通知与回流索引](/reference/event-backflow-index)。

## 统一响应体

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1710000000000
}
```

| 字段 | 说明 |
| --- | --- |
| `code` | 业务状态码，成功为 `200` |
| `message` | 面向前端提示的消息 |
| `data` | 响应数据，可能为对象、数组、分页对象或 `null` |
| `timestamp` | 后端生成响应时的毫秒时间戳 |

## 分页响应

分页对象位于 `PageResult.java`。

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "total": 100,
  "totalPages": 10,
  "records": [],
  "hasNext": true,
  "hasPrevious": false
}
```

## ResultCode 完整枚举

`ResultCode.java` 定义了全站所有业务状态码，共 21 个枚举值：

### 成功码

| 枚举名 | code | message | 说明 |
| --- | --- | --- | --- |
| `SUCCESS` | 200 | 操作成功 | 所有成功响应统一使用 |

### 客户端错误码（4xx 语义）

| 枚举名 | code | message | 说明 |
| --- | --- | --- | --- |
| `BAD_REQUEST` | 400 | 请求参数错误 | 通用参数错误兜底 |
| `UNAUTHORIZED` | 401 | 未授权访问 | 标准 401 语义，实际登录态多用 701/702 |
| `FORBIDDEN` | 403 | 访问被禁止 | 标准 403 语义 |
| `NOT_FOUND` | 404 | 资源不存在 | 路由或资源不存在 |
| `METHOD_NOT_ALLOWED` | 405 | 请求方法不支持 | HTTP method 不匹配 |
| `REQUEST_TIMEOUT` | 408 | 请求超时 | 客户端请求超时 |
| `CONFLICT` | 409 | 数据冲突 | 唯一键、版本冲突 |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | 不支持的媒体类型 | Content-Type 不支持 |
| `TOO_MANY_REQUESTS` | 429 | 请求过于频繁 | 频控和限流 |

### 服务端错误码（5xx 语义）

| 枚举名 | code | message | 说明 |
| --- | --- | --- | --- |
| `ERROR` | 500 | 系统内部错误 | 未捕获异常兜底 |
| `SERVICE_UNAVAILABLE` | 503 | 服务不可用 | 外部依赖或服务不可用 |

### 业务错误码（6xx 语义）

| 枚举名 | code | message | 说明 |
| --- | --- | --- | --- |
| `BUSINESS_ERROR` | 600 | 业务处理失败 | `BusinessException` 默认错误码 |
| `PARAM_VALIDATE_ERROR` | 601 | 参数校验失败 | Bean Validation、绑定失败 |
| `DATA_NOT_EXIST` | 602 | 数据不存在 | 查询、更新、删除目标不存在 |
| `DATA_ALREADY_EXIST` | 603 | 数据已存在 | 用户名、邮箱、版本号等重复 |
| `OPERATION_NOT_ALLOWED` | 604 | 操作不被允许 | 状态不满足或无权操作 |

### 认证授权错误码（7xx 语义）

| 枚举名 | code | message | 说明 |
| --- | --- | --- | --- |
| `TOKEN_INVALID` | 701 | Token无效 | 未提供 Token、Token 无效、无法识别 |
| `TOKEN_EXPIRED` | 702 | Token已过期 | 过期、被顶替、被踢下线 |
| `PERMISSION_DENIED` | 703 | 权限不足 | Sa-Token 权限或角色不足 |
| `ACCOUNT_DISABLED` | 704 | 账户已被禁用 | 账号封禁或服务禁用 |
| `LOGIN_FAILED` | 705 | 登录失败 | 用户名或密码错误 |

### 文件错误码（8xx 语义）

| 枚举名 | code | message | 说明 |
| --- | --- | --- | --- |
| `FILE_UPLOAD_ERROR` | 801 | 文件上传失败 | 上传链路异常 |
| `FILE_DOWNLOAD_ERROR` | 802 | 文件下载失败 | 下载链路异常 |
| `FILE_NOT_EXIST` | 803 | 文件不存在 | 文件记录或对象不存在 |
| `FILE_TYPE_ERROR` | 804 | 文件类型不支持 | MIME 或后缀不在允许范围 |
| `FILE_SIZE_EXCEEDED` | 805 | 文件大小超出限制 | 超过上传大小限制 |

## GlobalExceptionHandler 完整映射

`GlobalExceptionHandler.java` 是全站唯一的异常收敛点，共处理 18 种异常类型：

### Sa-Token 认证授权异常

| 异常类型 | HTTP 状态 | 业务 code | 详细 message | 触发条件 |
| --- | --- | --- | --- | --- |
| `NotLoginException` | 200 | 701 或 702 | 根据子类型不同 | Sa-Token 登录态失效 |
| `NotPermissionException` | 200 | 703 | "权限不足，无法访问" | `@SaCheckPermission` 校验失败 |
| `NotRoleException` | 200 | 703 | "角色权限不足，无法访问" | `@SaCheckRole` 校验失败 |
| `DisableServiceException` | 200 | 704 | "账号已被封禁，请联系管理员" | Sa-Token 服务封禁 |

#### NotLoginException 子类型映射

| 子类型常量 | 业务 code | 返回 message | 场景 |
| --- | --- | --- | --- |
| `NOT_TOKEN` | 701 | "未提供Token，请先登录" | 请求头无 Token |
| `INVALID_TOKEN` | 701 | "Token无效，请重新登录" | Token 格式或签名无效 |
| `TOKEN_TIMEOUT` | 702 | "Token已过期，请重新登录" | Token 超过有效期 |
| `BE_REPLACED` | 702 | "Token已被顶替，请重新登录" | 同一账号在另一设备登录 |
| `KICK_OUT` | 702 | "已被踢下线，请重新登录" | 管理员强制踢下线 |
| 其他 | 701 | "登录状态已失效，请重新登录" | 未知 NotLoginException 类型 |

### 业务异常

| 异常类型 | HTTP 状态 | 业务 code | 返回 message | 触发条件 |
| --- | --- | --- | --- | --- |
| `BusinessException` | 200 | 异常携带的 code（默认 600） | 异常携带的 message | 业务逻辑主动抛出 |

### 参数校验异常

| 异常类型 | HTTP 状态 | 业务 code | 返回 message | 触发条件 |
| --- | --- | --- | --- | --- |
| `MethodArgumentNotValidException` | 400 | 601 | 第一个 FieldError 的 defaultMessage | `@Valid` + `@RequestBody` 校验失败 |
| `BindException` | 400 | 601 | 第一个 FieldError 的 defaultMessage | 表单绑定校验失败 |
| `ConstraintViolationException` | 400 | 601 | 第一个 ConstraintViolation 的 message | `@Validated` + 路径参数/请求参数校验失败 |
| `IllegalArgumentException` | 400 | 400 | 异常原始 message | 代码主动抛出非法参数 |

### Spring MVC 框架异常

| 异常类型 | HTTP 状态 | 业务 code | 返回 message | 触发条件 |
| --- | --- | --- | --- | --- |
| `HttpRequestMethodNotSupportedException` | 405 | 405 | "请求方法'{method}'不支持" | GET/POST 等方法不匹配 |
| `HttpMediaTypeNotSupportedException` | 415 | 415 | "不支持的媒体类型" | Content-Type 不在支持列表 |
| `HttpMediaTypeNotAcceptableException` | 406 | 400 | "不支持的响应媒体类型" | Accept 头无法满足 |
| `HttpMessageNotReadableException` | 400 | 400 | "请求体格式错误或缺少必要内容" | JSON 解析失败、缺少请求体 |
| `MissingPathVariableException` | 400 | 400 | "缺少必要的路径变量[{name}]" | 路径变量缺失 |
| `MissingServletRequestParameterException` | 400 | 400 | "缺少必要的请求参数[{name}]" | `@RequestParam` 参数缺失 |
| `MissingRequestHeaderException` | 400 | 400 | "缺少必要的请求头[{name}]" | `@RequestHeader` 头缺失 |
| `MethodArgumentTypeMismatchException` | 400 | 400 | "参数类型不匹配,参数[{name}]需要[{type}]类型" | 参数类型转换失败 |

### 文件上传异常

| 异常类型 | HTTP 状态 | 业务 code | 返回 message | 触发条件 |
| --- | --- | --- | --- | --- |
| `MaxUploadSizeExceededException` | 413 | 805 | "文件大小超出限制" | 上传文件超过 `spring.servlet.multipart.max-file-size` |

### 路由和兜底异常

| 异常类型 | HTTP 状态 | 业务 code | 返回 message | 触发条件 |
| --- | --- | --- | --- | --- |
| `NoHandlerFoundException` | 404 | 404 | "请求地址'{uri}'不存在" | 路由不存在 |
| `Exception` | 500 | 500 | "系统内部错误，请联系管理员" | 所有未捕获异常兜底 |

## 文件接口认证和读取错误

文件接口有一部分直接返回基础 HTTP 语义码作为业务 `code`，下载流接口则会直接用 HTTP 状态表达失败。前端不要只按"文件错误码 8xx"判断文件模块失败。

| 表现 | 场景 | 常见接口 |
| --- | --- | --- |
| 业务 `code = 401` | 未登录就上传、批量上传、删除、查询列表或检查存在性 | `POST /file/upload/single`、`DELETE /file/{id}`、`GET /file/list` |
| 业务 `code = 403` 或 HTTP `403` | 读取私有文件但当前请求没有用户端或管理端登录态 | `GET /file/info/{id}`、`GET /file/url/{id}`、`POST /file/urls`、`GET /file/download/{id}` |
| 业务 `code = 803` 或 HTTP `404` | 文件记录不存在或 `status != 1` | `GET /file/info/{id}`、`GET /file/url/{id}`、`GET /file/download/{id}` |

## WebSocket 业务错误

聊天室 WebSocket 的 `ERROR` 事件不走统一 HTTP 响应体，而是把错误放在事件 `data` 里。前端会优先根据 `tempId` 找到本地乐观消息，并把它标记为失败。

```json
{
  "type": "ERROR",
  "data": {
    "code": "RATE_LIMITED",
    "message": "发送太快了，请稍后再试",
    "tempId": "temp_1710000000000_xxx",
    "retryAfterSeconds": 10
  }
}
```

| code | 场景 | 前端处理 |
| --- | --- | --- |
| `RATE_LIMITED` | 用户短时间发送消息超过 `xiaou.chat.rate-limit.message-limit` | 标记对应 `tempId` 消息失败，提示稍后再试 |
| `MESSAGE_REJECTED` | 消息为空、超长、图片 URL 非法、用户被禁言等 | 标记对应 `tempId` 消息失败，展示后端原因 |
| 无 code | JSON 解析失败或未知 WebSocket 处理异常 | 展示通用错误，必要时重连 |

## 异常处理策略

| 异常 | HTTP 状态 | 业务 code |
| --- | --- | --- |
| `BusinessException` | `200` | 异常内携带 code，默认 `600` |
| `NotLoginException` | `200` | `701` 或 `702` |
| `NotPermissionException`、`NotRoleException` | `200` | `703` |
| `DisableServiceException` | `200` | `704` |
| 参数校验和绑定异常 | `400` | `400` 或 `601` |
| 方法不支持 | `405` | `405` |
| 媒体类型不支持 | `415` | `415` |
| 文件大小超限 | `413` | `805` |
| 路由不存在 | `404` | `404` |
| 未捕获异常 | `500` | `500` |

### 设计要点

1. **Sa-Token 异常统一返回 HTTP 200**：所有 `NotLoginException`、`NotPermissionException`、`NotRoleException`、`DisableServiceException` 都返回 HTTP 200 + 业务状态码。这样前端只需在响应拦截器里处理 `code`，不需要同时处理 HTTP 状态码和业务状态码两套逻辑。
2. **参数校验异常优先返回字段级 message**：`MethodArgumentNotValidException` 和 `BindException` 会取 `FieldError.getDefaultMessage()`，而不是笼统的"参数校验失败"。
3. **未捕获异常不暴露堆栈**：`Exception` 兜底只返回"系统内部错误，请联系管理员"，堆栈信息只写在服务端日志里。

## 前端处理建议

1. 响应拦截器先判断 `code === 200`。
2. `701`、`702` 统一清理登录态并跳转登录页。
3. `703` 显示权限不足，不应重复重试。
4. `601` 优先展示后端返回的字段校验消息。
5. 文件接口同时检查 HTTP 状态和业务 code。

## 源码导航

| 想了解 | 读什么 |
| --- | --- |
| 统一响应包装 | `xiaou-common/src/main/java/com/xiaou/common/core/domain/Result.java` |
| 响应码枚举 | `xiaou-common/src/main/java/com/xiaou/common/core/domain/ResultCode.java` |
| 分页请求基类 | `xiaou-common/src/main/java/com/xiaou/common/core/domain/PageRequest.java` |
| 分页响应包装 | `xiaou-common/src/main/java/com/xiaou/common/core/domain/PageResult.java` |
| 业务异常 | `xiaou-common/src/main/java/com/xiaou/common/exception/BusinessException.java` |
| 全局异常拦截 | `xiaou-common/src/main/java/com/xiaou/common/exception/GlobalExceptionHandler.java` |
| AI 异常族 | `xiaou-common/src/main/java/com/xiaou/common/exception/ai/*.java` |
