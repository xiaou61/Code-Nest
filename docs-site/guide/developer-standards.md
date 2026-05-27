# 开发者规范

本文档定义 Code Nest 项目的代码规范、Git 规范和 Review 规范。

## 代码规范

### Java 规范

#### 命名规范

| 类型 | 规范 | 示例 |
| --- | --- | --- |
| 类名 | 大驼峰 | `UserService`, `OrderController` |
| 方法名 | 小驼峰 | `getUserById`, `createOrder` |
| 变量名 | 小驼峰 | `userName`, `orderList` |
| 常量 | 全大写下划线 | `MAX_PAGE_SIZE`, `DEFAULT_PASSWORD` |
| 包名 | 全小写 | `com.xiaou.user.service` |

#### 代码格式

```java
// 类注释
/**
 * 用户服务实现类
 *
 * @author xiaou
 * @since 1.0.0
 */
@Service
public class UserServiceImpl implements UserService {

    // 字段注入
    @Autowired
    private UserMapper userMapper;

    // 方法注释
    /**
     * 根据ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Override
    public User getUserById(Long userId) {
        // 业务逻辑
        return userMapper.selectById(userId);
    }
}
```

#### 异常处理

```java
// 业务异常
throw new BusinessException(ResultCode.USER_NOT_FOUND);

// 带消息的异常
throw new BusinessException("用户不存在");

// 不要捕获通用异常
try {
    // 业务逻辑
} catch (BusinessException e) {
    throw e; // 重新抛出业务异常
} catch (Exception e) {
    log.error("系统异常", e);
    throw new BusinessException(ResultCode.INTERNAL_ERROR);
}
```

### Vue 规范

#### 组件命名

```vue
<!-- 文件名：UserProfile.vue -->
<template>
  <div class="user-profile">
    <!-- 模板内容 -->
  </div>
</template>

<script setup>
// 组件逻辑
</script>

<style scoped>
/* 样式 */
</style>
```

#### 命名规范

| 类型 | 规范 | 示例 |
| --- | --- | --- |
| 组件文件 | 大驼峰 | `UserProfile.vue`, `OrderList.vue` |
| 变量 | 小驼峰 | `userName`, `orderList` |
| 方法 | 小驼峰 | `getUserById`, `handleSubmit` |
| CSS 类 | 短横线 | `.user-profile`, `.order-item` |

### SQL 规范

#### 命名规范

| 类型 | 规范 | 示例 |
| --- | --- | --- |
| 表名 | 小写下划线 | `user_info`, `order_detail` |
| 字段名 | 小写下划线 | `user_name`, `create_time` |
| 主键 | `id` | `id BIGINT AUTO_INCREMENT` |
| 外键 | `{table}_id` | `user_id`, `order_id` |

#### SQL 格式

```sql
-- 关键字大写
SELECT
    id,
    user_name,
    create_time
FROM
    user_info
WHERE
    status = 1
    AND deleted = 0
ORDER BY
    create_time DESC
LIMIT 10;
```

## Git 规范

### 分支管理

| 分支 | 用途 | 命名格式 |
| --- | --- | --- |
| `main` | 生产环境 | - |
| `develop` | 开发环境 | - |
| `feature/*` | 功能开发 | `feature/user-login` |
| `fix/*` | Bug 修复 | `fix/login-error` |
| `release/*` | 版本发布 | `release/v2.2.0` |

### 提交规范

使用 Conventional Commits 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Type 类型

| 类型 | 说明 | 示例 |
| --- | --- | --- |
| `feat` | 新功能 | `feat(user): 添加用户注册功能` |
| `fix` | Bug 修复 | `fix(auth): 修复 Token 过期问题` |
| `docs` | 文档更新 | `docs(api): 更新接口文档` |
| `style` | 代码格式 | `style: 格式化代码` |
| `refactor` | 重构 | `refactor(service): 重构用户服务` |
| `test` | 测试 | `test(user): 添加用户单元测试` |
| `chore` | 构建/工具 | `chore: 更新依赖版本` |

#### 示例

```bash
# 简单提交
git commit -m "feat(user): 添加用户注册功能"

# 带详细说明
git commit -m "feat(user): 添加用户注册功能

- 支持用户名、邮箱注册
- 添加验证码校验
- 密码强度验证

Closes #123"
```

### 代码审查

#### 审查要点

- [ ] 代码符合命名规范
- [ ] 有适当的注释
- [ ] 异常处理完善
- [ ] 没有硬编码
- [ ] 有单元测试
- [ ] 没有安全漏洞

#### 审查流程

1. 提交 Pull Request
2. 填写变更说明
3. 指定审查者
4. 审查通过后合并

## Review 规范

### Pull Request 模板

```markdown
## 变更说明

简要描述本次变更的内容。

## 变更类型

- [ ] 新功能
- [ ] Bug 修复
- [ ] 文档更新
- [ ] 重构
- [ ] 其他

## 测试情况

- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 手动测试通过

## 截图（如适用）

添加相关截图。

## 关联 Issue

Closes #123
```

### Review 检查清单

#### 代码质量

- [ ] 代码简洁易读
- [ ] 没有重复代码
- [ ] 命名清晰准确
- [ ] 注释充分适当

#### 功能正确

- [ ] 功能符合需求
- [ ] 边界情况处理
- [ ] 异常情况处理
- [ ] 性能满足要求

#### 安全性

- [ ] 输入验证
- [ ] SQL 注入防护
- [ ] XSS 防护
- [ ] 权限控制

## 文档规范

### 代码注释

```java
/**
 * 用户服务接口
 *
 * 提供用户相关的业务操作，包括：
 * - 用户注册、登录、退出
 * - 用户信息查询、修改
 * - 用户状态管理
 *
 * @author xiaou
 * @since 1.0.0
 * @see UserServiceImpl
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param request 注册请求，包含用户名、邮箱、密码
     * @return 注册成功的用户信息
     * @throws BusinessException 用户名或邮箱已存在时抛出
     */
    User register(RegisterRequest request);
}
```

### 接口文档

使用 Swagger 注解：

```java
@Operation(summary = "用户登录", description = "用户名/邮箱 + 密码登录")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "登录成功"),
    @ApiResponse(responseCode = "701", description = "用户名或密码错误"),
    @ApiResponse(responseCode = "704", description = "账号已被禁用")
})
@PostMapping("/login")
public Result<LoginResponse> login(@RequestBody LoginRequest request) {
    // 实现
}
```

## 工具配置

### IDE 配置

#### IntelliJ IDEA

1. 安装 Lombok 插件
2. 安装 SonarLint 插件
3. 配置代码模板

#### VS Code

1. 安装 ESLint 插件
2. 安装 Prettier 插件
3. 安装 Vetur 插件

### 代码格式化

#### Java

使用 `google-java-format`：

```bash
# 格式化单个文件
java -jar google-java-format.jar --replace MyClass.java

# 格式化整个项目
find . -name "*.java" -exec java -jar google-java-format.jar --replace {} \;
```

#### Vue

使用 Prettier：

```bash
# 格式化
npm run format

# 检查
npm run lint
```

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [功能开发流程](/guide/feature-development) | 开发流程指南 |
| [测试与回归](/guide/testing-regression) | 测试规范 |
| [文档维护规范](/guide/documentation-maintenance) | 文档规范 |
| [Git Log 版更新记录](/guide/git-log-release-notes) | Git 规范 |
