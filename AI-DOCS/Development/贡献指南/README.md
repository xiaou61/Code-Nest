# 贡献指南

## 目录说明

本目录存放项目贡献指南文档。

## 如何贡献

### 1. Fork 项目

```bash
# 1. 点击项目右上角的 Fork 按钮
# 2. 克隆 Fork 的项目
git clone https://github.com/your-username/Code-Nest.git
cd Code-Nest

# 3. 添加上游仓库
git remote add upstream https://github.com/xiaou/Code-Nest.git
```

### 2. 创建分支

```bash
# 同步上游代码
git fetch upstream
git checkout main
git merge upstream/main

# 创建功能分支
git checkout -b feature/your-feature-name
```

### 3. 开发代码

```bash
# 修改代码
# ...

# 提交代码
git add .
git commit -m "feat: 添加新功能"

# 推送到远程
git push origin feature/your-feature-name
```

### 4. 创建 Pull Request

```bash
# 1. 访问你的 Fork 仓库
# 2. 点击 "New Pull Request" 按钮
# 3. 填写 PR 描述
# 4. 等待审核
```

## 开发规范

### 1. 代码规范

- 遵循项目编码规范
- 使用有意义的变量名
- 添加必要的注释
- 保持代码简洁

### 2. 提交规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**类型说明**:

| 类型 | 说明 |
|------|------|
| feat | 新功能 |
| fix | 修复bug |
| docs | 文档更新 |
| style | 代码格式 |
| refactor | 代码重构 |
| test | 测试相关 |
| chore | 构建/工具 |

**示例**:

```
feat(user): 添加用户注册功能

- 添加用户注册接口
- 添加注册验证逻辑
- 添加注册成功邮件通知

Closes #123
```

### 3. 分支规范

| 分支类型 | 命名格式 | 说明 |
|---------|---------|------|
| 功能分支 | `feature/<scope>-<summary>` | 新功能开发 |
| 修复分支 | `fix/<scope>-<summary>` | 缺陷修复 |
| 文档分支 | `docs/<scope>-<summary>` | 文档更新 |

## Pull Request 规范

### 1. PR 标题

```
<type>(<scope>): <description>
```

**示例**:
- `feat(user): 添加用户注册功能`
- `fix(login): 修复登录失败问题`
- `docs(readme): 更新README文档`

### 2. PR 描述

```markdown
## 描述

简要描述本次修改的内容。

## 修改内容

- 修改1
- 修改2
- 修改3

## 测试

描述如何测试本次修改。

## 相关Issue

Closes #123
```

### 3. PR 检查清单

- [ ] 代码符合编码规范
- [ ] 添加了必要的测试
- [ ] 更新了相关文档
- [ ] 提交信息符合规范
- [ ] 没有引入新的警告

## Issue 规范

### 1. Bug 报告

```markdown
## Bug 描述

简要描述 bug 的表现。

## 复现步骤

1. 步骤1
2. 步骤2
3. 步骤3

## 预期行为

描述预期的行为。

## 实际行为

描述实际的行为。

## 环境信息

- 操作系统: [例如 Windows 11]
- 浏览器: [例如 Chrome 120]
- Java版本: [例如 JDK 17]
```

### 2. 功能请求

```markdown
## 功能描述

简要描述需要的功能。

## 使用场景

描述功能的使用场景。

## 实现建议

描述可能的实现方式。
```

## 代码审查

### 1. 审查要点

- 代码是否符合规范
- 逻辑是否正确
- 性能是否优化
- 安全是否考虑

### 2. 审查流程

1. 提交代码审查请求
2. 审查人员审查代码
3. 提出修改意见
4. 修改代码并重新提交

## 社区规范

### 1. 行为准则

- 尊重他人
- 保持礼貌
- 积极讨论
- 接受建议

### 2. 沟通方式

- 使用中文沟通
- 使用专业术语
- 保持简洁明了
- 及时回复消息

## 版本历史

| 版本 | 日期 | 更新内容 | 作者 |
|------|------|---------|------|
| v1.0.0 | 2026-05-29 | 初始版本 | AI Assistant |
