# 权限与安全边界

这页把 Code Nest 里分散在多个模块的安全边界放到一起，方便新增功能或排查问题时统一检查。

重点先记住一句：前端隐藏按钮不等于权限控制，敏感词不等于 XSS 防护，文件 URL 可访问不等于业务有权限。

如果你现在最想先搞清楚"项目里的权限到底在哪层真正生效"，建议先配合 [权限注解与角色边界索引](/reference/permission-boundaries) 一起看。那一页会把路径拦截、`@RequireAdmin`、当前用户、owner 归属和混合前缀模式拆开讲清楚。

## 安全边界总览

| 边界 | 主要风险 | 先看 |
| --- | --- | --- |
| 用户端和管理端登录域 | Token 混用、越权访问 | [鉴权与用户体系](/modules/auth) |
| 管理端接口权限 | 后台接口裸奔、普通用户调用后台接口 | [系统运营后台](/modules/system-ops) |
| 当前用户 ID | 前端传错或伪造 `userId` | [用户账户与个人中心](/modules/user-account) |
| 文件访问 | 私有文件被公开读取 | [文件存储](/modules/file-storage) |
| 内容渲染 | `v-html` 造成 XSS | [前端渲染安全](/reference/frontend-rendering-security) |
| 内容风控 | 敏感词漏拦或误拦 | [敏感词风控](/modules/sensitive) |
| WebSocket | 票据复用、伪造连接、消息失败态丢失 | [WebSocket 协议](/reference/websocket) |
| AI 输入输出 | Prompt、文档、模型响应进入日志或页面 | [AI Runtime](/modules/ai-runtime) |
| 部署 Origin | CORS 或 WebSocket Origin 放行错误 | [Docker 与服务部署](/operations/docker) |

## 登录域隔离

Code Nest 使用 Sa-Token 双登录域架构：

| 端 | 工具类 | loginType | Redis DB | Token 名称 |
| --- | --- | --- | --- | --- |
| 用户端 | `StpUserUtil` | `"user"` | `alone-redis`（DB 1） | `satoken:user` |
| 管理端 | `StpAdminUtil` | `"admin"` | `alone-redis`（DB 2） | `satoken:admin` |

**两套登录域完全隔离**：用户端 Token 不能调用管理端接口，反之亦然。这通过 `@RequireAdmin` 切面中调用 `StpAdminUtil.checkLogin()` 实现。

前端存储同样隔离：

| 端 | Token 存储 key | localStorage/Cookie |
| --- | --- | --- |
| 用户端 | 用户端自定义 key | `vue3-user-front` 的 Pinia store |
| 管理端 | 管理端自定义 key | `vue3-admin-front` 的 Pinia store |

常见错误与后果：

| 错误 | 后果 | 排查 |
| --- | --- | --- |
| 后端相信前端传入的 `userId` | 可能越权读取或修改别人的数据 | 检查是否使用 `StpUserUtil.getLoginIdAsLong()` |
| 只在前端隐藏按钮 | 用户仍可直接调用接口 | 检查后端接口是否加 `@RequireAdmin` |
| 新后台接口漏 `@RequireAdmin` | 管理接口可能被非管理员访问 | 新增 Controller 必须检查 |
| 注册页实时校验接口未放行 | 未登录状态下校验失败 | 检查是否加 `@SaIgnore` 或在配置中排除 |

### 源码落点：路径拦截规则

登录域隔离的核心实现在 `xiaou-common` 的 `SaTokenConfig.java`：

```java
// xiaou-common/src/main/java/com/xiaou/common/config/SaTokenConfig.java
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 管理端路由认证（路径是 /auth/**）
            SaRouter.match("/auth/**")
                .notMatch("/auth/login", "/auth/register", "/auth/refresh")
                .check(r -> StpAdminUtil.checkLogin());

            // 用户端路由认证（路径是 /user/**）
            SaRouter.match("/user/**")
                .notMatch("/user/auth/login", "/user/auth/register", "/user/auth/refresh")
                .notMatch("/user/auth/check-username", "/user/auth/check-email")
                .check(r -> StpUserUtil.checkLogin());

            // 验证码接口无需认证
            SaRouter.match("/captcha/**").stop();

            // Swagger 和 API 文档接口无需认证
            SaRouter.match("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").stop();

        })).addPathPatterns("/**")
          .excludePathPatterns("/error", "/favicon.ico");
    }
}
```

关键规则：

| 路径前缀 | 认证方式 | 放行子路径 |
| --- | --- | --- |
| `/auth/**` | `StpAdminUtil.checkLogin()` | `/auth/login`, `/auth/register`, `/auth/refresh` |
| `/user/**` | `StpUserUtil.checkLogin()` | `/user/auth/login`, `/user/auth/register`, `/user/auth/refresh`, `/user/auth/check-username`, `/user/auth/check-email` |
| `/captcha/**` | 无需认证 | — |
| `/v3/api-docs/**` | 无需认证 | — |

新增公开接口时，如果路径在 `/user/**` 或 `/auth/**` 下且不需要登录，必须在 `SaTokenConfig` 中增加 `.notMatch()` 规则，否则会被拦截返回 701。

## 管理端高风险操作

这些操作需要特别谨慎：

| 操作 | 建议 |
| --- | --- |
| 删除、强制删除、批量处理 | 二次确认、操作日志、可恢复或可追溯 |
| 配置 AI、RAG、存储、敏感词策略 | 管理权限、脱敏展示、避免大段 Prompt 入日志 |
| 发放积分、修改库存、发布版本 | 操作日志、幂等、失败回滚 |
| 审核内容、封禁用户、禁言踢人 | 状态记录、通知、可查询原因 |

新增后台页面时，至少验证：

1. 未登录访问被拒。
2. 非管理员访问被拒。
3. 成功操作写入业务表。
4. 失败操作不产生半状态。

### 源码落点：@RequireAdmin 切面

管理端权限的第二层保障是 `@RequireAdmin` 注解 + AOP 切面，定义在 `xiaou-common`：

```java
// xiaou-common/src/main/java/com/xiaou/common/annotation/RequireAdmin.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {
    boolean allowSuperAdmin() default true;
    String message() default "需要管理员权限";
}
```

```java
// xiaou-common/src/main/java/com/xiaou/common/aspect/AdminAuthAspect.java
@Aspect
@Component
public class AdminAuthAspect {
    @Pointcut("@annotation(com.xiaou.common.annotation.RequireAdmin)")
    public void adminAuthPointcut() {}

    @Around("adminAuthPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            StpAdminUtil.checkLogin();     // 第1层：管理员登录校验
            StpAdminUtil.checkRole("admin"); // 第2层：管理员角色校验
            Long adminId = StpAdminUtil.getLoginIdAsLong();
            return joinPoint.proceed();
        } catch (NotLoginException e) {
            throw new BusinessException("请先登录");
        } catch (NotRoleException e) {
            throw new BusinessException(requireAdmin.message());
        }
    }
}
```

执行链路：

```text
HTTP 请求 → SaTokenConfig 路径拦截 → @RequireAdmin 切面 → StpAdminUtil.checkLogin() + checkRole("admin") → 业务方法
```

**当前项目有 47 个 Controller 类使用了 `@RequireAdmin`**，覆盖了所有管理端写操作。新增管理端 Controller 时，每个写方法都必须加 `@RequireAdmin`。

### 源码落点：权限和角色实现

`StpInterfaceImpl` 决定了 `checkRole` 和 `checkPermission` 的结果：

```java
// xiaou-common/src/main/java/com/xiaou/common/satoken/StpInterfaceImpl.java
@Component
public class StpInterfaceImpl implements StpInterface {
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if ("admin".equals(loginType)) {
            return List.of("admin");  // 管理员角色
        } else if ("user".equals(loginType)) {
            return List.of("user");   // 普通用户角色
        }
        return List.of();
    }
}
```

当前实现是粗粒度：admin loginType 直接获得 `"admin"` 角色。如果后续需要细粒度 RBAC（如模块级权限控制），需要在这里接入数据库查询。

## 文件访问边界

文件模块有两个边界：

1. 上传、删除、列表、存在性检查等接口需要登录。
2. 匿名读取只应允许 `is_public = 1` 的文件。

业务模块接入文件时要判断：

| 场景 | 建议 |
| --- | --- |
| 头像、公开封面 | 可以公开读取 |
| 简历导出、私密附件 | 不要只靠静态 URL，优先通过业务接口校验权限 |
| 聊天图片 | 结合聊天室权限和消息归属判断 |
| 后台文件管理 | 删除和强制删除要谨慎，避免业务引用断裂 |

不要把 `/api/files/**` 可访问当作"业务有权限"。如果文件本身有业务归属，业务层仍要校验用户是否能看。

### 源码落点：文件上传安全校验

`FileStorageServiceImpl.uploadSingle()` 有三层安全检查：

```java
// xiaou-filestorage/src/main/java/.../impl/FileStorageServiceImpl.java
public FileUploadResult uploadSingle(MultipartFile file, String moduleName, String businessType) {
    // 1. 参数校验
    if (file == null || file.isEmpty()) {
        return FileUploadResult.failure("文件不能为空");
    }

    // 2. 文件类型白名单校验（通过 Tika 检测真实 MIME 类型）
    String contentType = detectContentType(file);
    String fileExtension = cn.hutool.core.io.FileUtil.extName(originalName);
    if (!fileSystemSettingService.isFileTypeAllowed(fileExtension)) {
        return FileUploadResult.failure("不支持的文件类型: " + fileExtension);
    }

    // 3. 文件大小校验
    if (fileSystemSettingService.isFileSizeExceeded(fileSize)) {
        return FileUploadResult.failure("文件大小超限");
    }

    // 新文件默认 isPublic = 0（私有）
    fileInfo.setIsPublic(0);
}
```

| 校验层 | 实现方式 | 配置位置 |
| --- | --- | --- |
| 文件类型白名单 | `isFileTypeAllowed(extension)` | `file_system_setting` 表 |
| 文件大小限制 | `isFileSizeExceeded(size)` | `file_system_setting` 表 + Spring `max-file-size` |
| 真实 MIME 检测 | Apache Tika `detectContentType()` | 硬编码在 Service |
| 默认私有 | `setIsPublic(0)` | 硬编码在 Service |

文件大小超限时，`GlobalExceptionHandler` 会捕获 `MaxUploadSizeExceededException` 并返回 HTTP 413 + 业务错误码。

## 前端渲染边界

只要使用 `v-html`，都要判断 HTML 从哪里来。

| 内容来源 | 安全做法 |
| --- | --- |
| Markdown | `renderMarkdown(content)` |
| 普通用户文本 | `escapeHtml(content)` 后再 `sanitizeHtml` |
| JSON、Diff、高亮片段 | 先转义，再插入有限标签 |
| AI/RAG 返回文本 | 先转义正文和关键词，再插入 `&lt;mark&gt;` |

敏感词检测不是 XSS 防护。敏感词解决的是内容合规，DOMPurify 和转义解决的是 HTML 注入。

### 源码落点：DOMPurify 净化配置

`vue3-user-front/src/utils/markdown.js` 是前端渲染安全的核心：

```javascript
// vue3-user-front/src/utils/markdown.js
import DOMPurify from 'dompurify'

const sanitizeOptions = {
  USE_PROFILES: { html: true },
  ADD_ATTR: ['target', 'rel'],
  FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed'],
  FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover', 'style']
}

export function sanitizeHtml(html) {
  if (!html || typeof html !== 'string') return ''
  return DOMPurify.sanitize(html, sanitizeOptions)
}

export function renderMarkdown(content) {
  if (!content || typeof content !== 'string') return ''
  try {
    return sanitizeHtml(md.render(content))  // Markdown → HTML → 净化
  } catch (error) {
    return sanitizeHtml(md.utils.escapeHtml(content).replace(/\n/g, '&lt;br&gt;'))
  }
}
```

净化规则详解：

| 规则 | 值 | 作用 |
| --- | --- | --- |
| `FORBID_TAGS` | `script, style, iframe, object, embed` | 阻止脚本注入和外部资源嵌入 |
| `FORBID_ATTR` | `onerror, onload, onclick, onmouseover, style` | 阻止事件处理器和内联样式注入 |
| `ADD_ATTR` | `target, rel` | 允许链接打开新标签 |
| `USE_PROFILES` | `{ html: true }` | 允许安全 HTML 标签 |

当前使用 `sanitizeHtml` 的页面有 8 个：DiscussionList、lottery、notification、bug-store、DailyContent、FlashCard、DeckDetail，以及所有通过 `renderMarkdown` 间接调用的页面。

新增页面如果需要渲染用户提交的 HTML，**必须**通过 `renderMarkdown()` 或 `sanitizeHtml()`，不要直接 `v-html="rawContent"`。

## 内容风控边界

内容型模块通常要接入敏感词：

| 模块 | 常见字段 |
| --- | --- |
| 社区 | 标题、正文、评论 |
| 动态 | 正文、评论 |
| 博客 | 标题、摘要、正文 |
| 聊天 | 文本消息、公告 |
| 学习资产 | 标题、摘要、内容 |

新增内容发布能力时至少验证：

1. 普通文本通过。
2. 命中低风险词时按策略替换或记录。
3. 命中高风险词时禁止发布。
4. 白名单能按模块生效。
5. 前端展示的是处理后的文本还是原始文本要写清楚。

### 源码落点：敏感词检测流程

`SensitiveCheckServiceImpl.checkText()` 的完整流程：

```java
// xiaou-sensitive/src/main/java/.../impl/SensitiveCheckServiceImpl.java
public SensitiveCheckResponse checkText(SensitiveCheckRequest request) {
    String text = request.getText().trim();

    // 1. 文本长度保护（超过 10000 字符截断）
    if (text.length() > MAX_TEXT_LENGTH) {
        text = text.substring(0, MAX_TEXT_LENGTH);
    }

    // 2. 文本预处理（变形词检测：同音字、形似字、拆字、间隔符）
    String preprocessedText = textPreprocessor.preprocess(text, true, true, true, true);

    // 3. 敏感词引擎检测（DFA 算法）
    List<String> hitWords = sensitiveEngine.findSensitiveWords(text);
    List<String> variantHitWords = sensitiveEngine.findSensitiveWords(preprocessedText);

    // 4. 白名单过滤（按模块生效）
    for (String word : hitWords) {
        if (!whitelistService.isInWhitelist(word, module)) {
            filteredHitWords.add(word);
        }
    }

    // 5. 风险等级计算 → 策略匹配 → 动作决定
    int riskLevel = calculateRiskLevel(hitWords.size());
    //   1个命中 → 低风险(1) → replace
    //   2-3个命中 → 中风险(2) → replace
    //   4+个命中 → 高风险(3) → reject

    // 6. 文本处理
    //   replace → sensitiveEngine.replaceSensitiveWords(text, "***")
    //   reject  → 返回空文本，allowed = false

    // 7. 异步记录日志和统计
    logSensitiveDetectionAsync(request, hitWords, action);
    recordStatisticsAsync(request, hitWords, strategy);
}
```

业务模块接入方式：

```java
// 方式1：检测是否包含敏感词（返回 boolean）
boolean hasSensitive = sensitiveCheckService.containsSensitiveWords(title, "community");

// 方式2：替换敏感词后返回处理文本
String processed = sensitiveCheckService.replaceSensitiveWords(content, "blog");

// 方式3：完整检测（返回命中词、风险等级、动作、是否允许）
SensitiveCheckRequest request = new SensitiveCheckRequest();
request.setText(content);
request.setModule("moment");
request.setUserId(userId);
SensitiveCheckResponse response = sensitiveCheckService.checkText(request);
if (!response.getAllowed()) {
    throw new BusinessException("内容包含敏感词，禁止发布");
}
```

## WebSocket 边界

聊天室连接不能直接带普通 Token 建连。当前流程是：

1. 用户端登录。
2. 调用 `POST /user/chat/ws-ticket`。
3. 使用一次性 `ticket` 连接 `/ws/chat?ticket=...`。
4. 服务端消费票据，写入 session attributes。

新增 WebSocket 事件时要检查：

| 检查项 | 预期 |
| --- | --- |
| 连接认证 | 必须先拿 ws-ticket |
| 票据复用 | 复用应失败 |
| 消息校验 | 空消息、超长消息、非法图片 URL 被拒 |
| 失败态 | 前端能按 tempId 标记失败 |
| 重连 | 不重复发送已成功消息 |

### 源码落点：ws-ticket 生成与消费

```java
// xiaou-chat/src/main/java/.../service/ChatWebSocketTicketService.java
@Service
public class ChatWebSocketTicketService {
    private static final int TICKET_BYTE_LENGTH = 32;  // 32字节 = 256位随机
    private static final long TICKET_TTL_SECONDS = 60L; // 60秒过期
    private static final int MAX_TICKET_LENGTH = 128;

    public String createTicket(Long userId) {
        byte[] randomBytes = new byte[TICKET_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);  // SecureRandom 密码学安全
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        redisUtil.set(buildKey(ticket), String.valueOf(userId), TICKET_TTL_SECONDS);
        return ticket;
    }

    public Long consumeTicket(String ticket) {
        if (!isValidTicket(ticket)) return null;  // 格式校验
        String key = buildKey(ticket);
        Object cachedUserId = redisUtil.get(key);
        redisUtil.del(key);  // 一次性消费，复用返回 null
        return cachedUserId != null ? Long.parseLong(cachedUserId.toString()) : null;
    }

    private boolean isValidTicket(String ticket) {
        return ticket != null
            && !ticket.isBlank()
            && ticket.length() <= MAX_TICKET_LENGTH
            && ticket.matches("^[A-Za-z0-9_-]+$");  // 只允许 URL 安全字符
    }
}
```

### 源码落点：WebSocket 握手拦截

```java
// xiaou-chat/src/main/java/.../websocket/SaTokenWebSocketInterceptor.java
@Component
public class SaTokenWebSocketInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(..., Map<String, Object> attributes) {
        // 从 URL 查询参数取 ticket
        String ticket = UriComponentsBuilder.fromUri(servletRequest.getURI())
            .build().getQueryParams().getFirst("ticket");

        // 消费票据（一次性）
        Long userId = chatWebSocketTicketService.consumeTicket(ticket);
        if (userId != null) {
            attributes.put("userId", userId);       // 写入 session attributes
            attributes.put("username", getUsername(userId));
            return true;  // 握手通过
        }
        return false;  // 握手拒绝
    }
}
```

### 源码落点：WebSocket Origin 白名单

```java
// xiaou-chat/src/main/java/.../config/WebSocketConfig.java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Value("${xiaou.cors.allowed-origin-patterns:...}")
    private String allowedOriginPatterns;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(saTokenWebSocketInterceptor)
                .setAllowedOriginPatterns(parseAllowedOriginPatterns());
                // ↑ Origin 白名单与 CORS 共用同一配置
    }
}
```

安全参数汇总：

| 参数 | 值 | 作用 |
| --- | --- | --- |
| 票据随机长度 | 32 字节（256 位） | 防止暴力猜测 |
| 票据编码 | Base64 URL-safe | 兼容 WebSocket URL 参数 |
| 票据 TTL | 60 秒 | 窗口期短，减少被盗用风险 |
| 票据消费 | 一次性（Redis DEL） | 防止重放攻击 |
| 票据格式校验 | `^[A-Za-z0-9_-]+$`，最大 128 字符 | 防止注入 |
| Origin 白名单 | 与 CORS 共用 | 防止跨域 WebSocket 劫持 |

## 异常处理与错误码

安全校验失败后，异常由 `GlobalExceptionHandler` 统一处理，**所有 Sa-Token 异常返回 HTTP 200 + 业务错误码**，前端在响应拦截器中统一处理：

```java
// xiaou-common/src/main/java/.../exception/GlobalExceptionHandler.java

// 未登录 → HTTP 200 + code 701/702
@ExceptionHandler(NotLoginException.class)
@ResponseStatus(HttpStatus.OK)  // 注意：不是 401
public Result<Void> handleNotLoginException(NotLoginException e, ...) {
    switch (e.getType()) {
        case NotLoginException.NOT_TOKEN:     → code 701 "未提供Token"
        case NotLoginException.INVALID_TOKEN:  → code 701 "Token无效"
        case NotLoginException.TOKEN_TIMEOUT:  → code 702 "Token已过期"
        case NotLoginException.BE_REPLACED:    → code 702 "Token已被顶替"
        case NotLoginException.KICK_OUT:       → code 702 "已被踢下线"
    }
}

// 权限不足 → HTTP 200 + code 703
@ExceptionHandler(NotPermissionException.class)
@ResponseStatus(HttpStatus.OK)
public Result<Void> handleNotPermissionException(...) {
    return Result.error(703, "权限不足，无法访问");
}

// 账号封禁 → HTTP 200 + code 704
@ExceptionHandler(DisableServiceException.class)
@ResponseStatus(HttpStatus.OK)
public Result<Void> handleDisableServiceException(...) {
    return Result.error(704, "账号已被封禁，请联系管理员");
}

// 文件大小超限 → HTTP 413
@ExceptionHandler(MaxUploadSizeExceededException.class)
@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
public Result<Void> handleMaxUploadSizeExceeded(...) { ... }
```

错误码速查：

| 错误码 | 含义 | 前端处理 |
| --- | --- | --- |
| 701 | Token 无效/缺失 | 跳转登录页 |
| 702 | Token 过期/被顶替 | 尝试刷新 Token，失败则跳转登录 |
| 703 | 权限不足 | 提示无权限 |
| 704 | 账号封禁 | 提示联系管理员 |

## CORS 与部署安全边界

### 源码落点：CORS 配置

```java
// xiaou-common/src/main/java/com/xiaou/common/config/CorsConfig.java
@Configuration
public class CorsConfig {
    @Value("${xiaou.cors.allowed-origin-patterns:http://localhost:3000,...}")
    private String allowedOriginPatterns;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        Arrays.stream(allowedOriginPatterns.split(","))
            .map(String::trim)
            .forEach(config::addAllowedOriginPattern);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        // ...
    }
}
```

生产部署时要检查：

| 项 | 建议 |
| --- | --- |
| CORS | 只写前端页面 Origin，例如 `https://www.example.com` |
| WebSocket Origin | 与 HTTP CORS 使用同一白名单 |
| Nginx `/api` | 转发到后端 `/api` 上下文 |
| 文件目录 | Docker volume 或 Nginx alias 不应暴露私有目录 |
| go-judge | 独立部署，限制 CPU/内存，不与主后端共享高权限容器 |
| 密钥 | JWT、Sa-Token、AI Key、RAG Key 不进仓库 |

### CORS 配置方式

本地开发默认值已覆盖常见端口（3000/3001/5175），生产环境通过 YAML 配置：

```yaml
# application-prod.yml
xiaou:
  cors:
    allowed-origin-patterns: https://www.example.com,https://admin.example.com
```

**不要**在生产配置中使用 `*` 通配符，因为 `allowCredentials=true` 与 `*` 不兼容，且会带来 CSRF 风险。

## 新功能安全自检

| 检查项 | 预期 |
| --- | --- |
| 登录态 | 用户端和管理端登录域清楚 |
| 接口权限 | 管理端写接口有 `@RequireAdmin` 或等价控制 |
| 当前用户 | 后端从 Token 取用户 ID，不信任前端传入 |
| 文件 | 明确公开/私有，业务归属权限不丢 |
| 内容 | 敏感词和 XSS 分别处理 |
| WebSocket | ws-ticket、消息校验、失败态完整 |
| AI | Prompt、Schema、RAG、日志脱敏和输出净化明确 |
| 部署 | CORS、Origin、Nginx、密钥、外部依赖配置明确 |

### 自检代码模式

新增用户端接口时，确认当前用户取法：

```java
// ✅ 正确：从 Token 取
Long userId = StpUserUtil.getLoginIdAsLong();

// ❌ 错误：信任前端传入
@PostMapping("/create")
public Result<Long> create(@RequestBody CreateRequest request) {
    // request.getUserId() 可能被伪造！
    return Result.success(service.create(request.getUserId(), request));
}
```

新增管理端接口时，确认权限注解：

```java
// ✅ 正确：每个写方法加 @RequireAdmin
@RequireAdmin
@Log(module = "XX管理", type = Log.OperationType.INSERT, description = "创建XX")
@PostMapping("/create")
public Result<Long> create(@Validated @RequestBody CreateRequest request) {
    return Result.success(service.create(request));
}

// ❌ 错误：漏掉 @RequireAdmin
@PostMapping("/create")  // 任何登录用户都能调用！
public Result<Long> create(@RequestBody CreateRequest request) { ... }
```

新增公开接口时，确认路径放行：

```java
// 如果新接口在 /user/** 下且不需要登录，必须在 SaTokenConfig 中放行
SaRouter.match("/user/**")
    .notMatch("/user/auth/login", "/user/auth/register", "/user/auth/refresh")
    .notMatch("/user/auth/check-username", "/user/auth/check-email")
    .notMatch("/user/your-public-endpoint")  // ← 新增放行
    .check(r -> StpUserUtil.checkLogin());
```

新增页面渲染用户内容时，确认净化：

```vue
<!-- ✅ 正确：通过 renderMarkdown 净化 -->
<div v-html="renderMarkdown(content)"></div>

<!-- ❌ 错误：直接渲染原始内容 -->
<div v-html="content"></div>
```
