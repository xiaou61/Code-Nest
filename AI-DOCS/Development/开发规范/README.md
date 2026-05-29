# 开发规范

## 目录说明

本目录存放项目开发规范文档，包括编码规范、Git规范、代码审查规范等。

## 1. 编码规范

### 1.1 Java 编码规范

#### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `UserService`, `OrderController` |
| 方法名 | camelCase | `getUserById()`, `createOrder()` |
| 变量名 | camelCase | `userId`, `orderList` |
| 常量名 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |
| 包名 | 小写 | `com.xiaou.user.service` |

#### 代码格式

- **缩进**: 4个空格
- **行长度**: 不超过120个字符
- **空行**: 方法之间空一行，逻辑块之间空一行
- **括号**: 左括号不换行，右括号独占一行

#### 注释规范

```java
/**
 * 用户服务类
 * 
 * @author xiaou
 * @since 1.0.0
 */
public class UserService {
    
    /**
     * 根据ID获取用户
     * 
     * @param userId 用户ID
     * @return 用户对象
     * @throws UserNotFoundException 用户不存在时抛出
     */
    public User getUserById(Long userId) {
        // 实现逻辑
    }
}
```

#### 异常处理

```java
// 1. 使用自定义异常
throw new BusinessException(ErrorCode.USER_NOT_FOUND);

// 2. 捕获异常时记录日志
try {
    // 业务逻辑
} catch (Exception e) {
    log.error("操作失败", e);
    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
}

// 3. 不要捕获通用异常
// 错误示例
try {
    // 业务逻辑
} catch (Exception e) {
    // 不要这样做
}
```

### 1.2 前端编码规范

#### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 组件名 | PascalCase | `UserProfile.vue`, `OrderList.vue` |
| 文件名 | kebab-case | `user-profile.vue`, `order-list.vue` |
| 变量名 | camelCase | `userName`, `orderList` |
| 常量名 | UPPER_SNAKE_CASE | `API_BASE_URL`, `MAX_PAGE_SIZE` |
| CSS类名 | kebab-case | `user-profile`, `order-list` |

#### 代码格式

- **缩进**: 2个空格
- **行长度**: 不超过100个字符
- **引号**: 单引号
- **分号**: 不使用分号

#### 组件规范

```vue
<template>
  <div class="user-profile">
    <!-- 模板内容 -->
  </div>
</template>

<script setup>
// 组件逻辑
import { ref, onMounted } from 'vue'

const userName = ref('')

onMounted(() => {
  // 初始化逻辑
})
</script>

<style scoped>
.user-profile {
  /* 样式 */
}
</style>
```

### 1.3 SQL 编码规范

#### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 表名 | 小写，下划线分隔 | `user_info`, `order_detail` |
| 字段名 | 小写，下划线分隔 | `user_id`, `create_time` |
| 索引名 | 前缀+字段名 | `idx_user_id`, `uk_username` |
| 主键名 | `id` | `id` |

#### 代码格式

```sql
-- 关键字大写
SELECT 
    id,
    username,
    nickname
FROM 
    user_info
WHERE 
    status = 1
    AND is_deleted = 0
ORDER BY 
    create_time DESC
LIMIT 10;
```

#### 索引规范

- 主键使用自增ID
- 唯一字段创建唯一索引
- 查询频繁的字段创建索引
- 避免在索引列上使用函数

## 2. Git 规范

### 2.1 分支管理

| 分支类型 | 命名格式 | 说明 |
|---------|---------|------|
| 主分支 | `main` | 生产环境代码 |
| 开发分支 | `develop` | 开发环境代码 |
| 功能分支 | `feature/<scope>-<summary>` | 新功能开发 |
| 修复分支 | `fix/<scope>-<summary>` | 缺陷修复 |
| 热修复分支 | `hotfix/vX.Y.Z-<summary>` | 线上紧急修复 |
| 发布分支 | `release/vX.Y.Z` | 版本发布准备 |

### 2.2 提交规范

#### 提交格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### 提交类型

| 类型 | 说明 | 示例 |
|------|------|------|
| feat | 新功能 | `feat(user): 添加用户注册功能` |
| fix | 修复bug | `fix(login): 修复登录失败问题` |
| docs | 文档更新 | `docs(readme): 更新README文档` |
| style | 代码格式 | `style: 格式化代码` |
| refactor | 代码重构 | `refactor(service): 重构用户服务` |
| test | 测试相关 | `test: 添加单元测试` |
| chore | 构建/工具 | `chore: 更新依赖版本` |

#### 提交示例

```
feat(user): 添加用户注册功能

- 添加用户注册接口
- 添加注册验证逻辑
- 添加注册成功邮件通知

Closes #123
```

### 2.3 代码审查

#### 审查要点

- 代码是否符合编码规范
- 逻辑是否正确
- 性能是否优化
- 安全是否考虑

#### 审查流程

1. 提交代码审查请求
2. 审查人员审查代码
3. 提出修改意见
4. 修改代码并重新提交

## 3. 代码审查规范

### 3.1 审查清单

#### 代码质量

- [ ] 代码是否符合编码规范
- [ ] 命名是否清晰易懂
- [ ] 注释是否充分
- [ ] 代码是否可读

#### 功能实现

- [ ] 功能是否按需求实现
- [ ] 边界条件是否处理
- [ ] 异常情况是否处理
- [ ] 返回值是否正确

#### 性能优化

- [ ] 是否有性能问题
- [ ] 是否有资源泄漏
- [ ] 是否有重复计算
- [ ] 是否有不必要的IO

#### 安全性

- [ ] 是否有SQL注入
- [ ] 是否有XSS攻击
- [ ] 是否有CSRF攻击
- [ ] 是否有敏感信息泄露

### 3.2 审查流程

1. **提交审查**: 开发人员提交代码审查请求
2. **分配审查**: 指定审查人员
3. **代码审查**: 审查人员审查代码
4. **反馈意见**: 审查人员提出修改意见
5. **修改代码**: 开发人员修改代码
6. **再次审查**: 审查人员再次审查
7. **合并代码**: 审查通过后合并代码

## 4. 环境搭建规范

### 4.1 开发环境

#### 必需软件

| 软件 | 版本 | 用途 |
|------|------|------|
| JDK | 17+ | Java开发 |
| Maven | 3.6+ | 构建工具 |
| Node.js | 16+ | 前端开发 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.0+ | 缓存 |
| Git | 2.30+ | 版本控制 |
| IDE | IntelliJ IDEA | 开发工具 |

#### 环境配置

```bash
# 配置 JDK
export JAVA_HOME=/usr/lib/jvm/java-17
export PATH=$JAVA_HOME/bin:$PATH

# 配置 Maven
export MAVEN_HOME=/opt/maven
export PATH=$MAVEN_HOME/bin:$PATH

# 配置 Node.js
export NODE_HOME=/opt/node
export PATH=$NODE_HOME/bin:$PATH
```

### 4.2 项目配置

#### 后端配置

```bash
# 克隆项目
git clone https://github.com/xiaou/Code-Nest.git

# 进入项目目录
cd Code-Nest

# 安装依赖
mvn clean install

# 启动项目
mvn spring-boot:run -pl xiaou-application
```

#### 前端配置

```bash
# 进入前端目录
cd vue3-user-front

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

## 5. 文档规范

### 5.1 文档类型

| 文档类型 | 说明 | 模板 |
|---------|------|------|
| PRD | 产品需求文档 | [PRD模板](../../assets/templates/PRD-template.md) |
| 技术文档 | 技术设计文档 | [技术文档模板](../../assets/templates/Technical-template.md) |
| 测试文档 | 测试计划文档 | [测试文档模板](../../assets/templates/Testing-template.md) |
| 部署文档 | 部署指南文档 | [部署文档模板](../../assets/templates/Deployment-template.md) |
| 用户文档 | 用户手册文档 | [用户文档模板](../../assets/templates/User-template.md) |
| 开发文档 | 开发规范文档 | [开发文档模板](../../assets/templates/Development-template.md) |

### 5.2 文档规范

- 使用Markdown格式编写
- 使用中文编写
- 使用清晰的标题结构
- 使用表格展示数据
- 使用代码块展示代码
- 使用图片展示界面

## 6. 版本管理规范

### 6.1 版本号规范

- 使用语义化版本号: `vX.Y.Z`
- **X**: 主版本号，重大功能变更
- **Y**: 次版本号，新增功能
- **Z**: 修订号，bug修复

### 6.2 版本发布流程

1. **创建发布分支**: `release/vX.Y.Z`
2. **版本号更新**: 更新所有版本号
3. **测试验证**: 完成测试验证
4. **合并主分支**: 合并到main分支
5. **打标签**: 创建版本标签
6. **发布上线**: 部署到生产环境

## 7. 协作规范

### 7.1 沟通规范

- 使用中文沟通
- 使用专业术语
- 保持礼貌和尊重
- 及时回复消息

### 7.2 任务管理

- 使用任务管理工具
- 明确任务负责人
- 明确任务截止时间
- 及时更新任务状态

### 7.3 知识分享

- 定期技术分享
- 记录技术文档
- 总结经验教训
- 持续学习改进

## 版本历史

| 版本 | 日期 | 更新内容 | 作者 |
|------|------|---------|------|
| v1.0.0 | 2026-05-29 | 初始版本 | AI Assistant |
