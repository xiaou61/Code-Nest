# 发布流程

## v2.3.1

- 新增统一 CI 工作流，覆盖后端、双前端、文档站和脚本语法检查。
- 新增生产部署 GitHub Actions，支持版本分支/tag 推送或手动触发后构建 release bundle 并同步服务器。
- 新增服务器端发布脚本，内置备份、替换、健康检查和失败回滚。
- 新增生产 CI/CD 文档，说明 Secrets、目录、触发方式和回滚命令。

本文档定义 Code Nest 的版本发布流程，适用于正式版本、补丁版本和紧急修复版本。

## 版本号

版本号格式：

```text
vMAJOR.MINOR.PATCH
```

- `MAJOR`：架构级重构、不兼容变更或版本主题升级。
- `MINOR`：新增功能、模块增强或较大范围体验优化。
- `PATCH`：缺陷修复、安全修复、工程治理或兼容性优化。

示例：

```text
v2.2.1
v2.3.0
v3.0.0
```

## 发布分支

推荐分支：

- `release/vX.Y.Z`：常规发布准备分支。
- `hotfix/vX.Y.Z-summary`：紧急修复分支。
- `vX.Y.Z`：如需要保留版本分支，可在发布完成后推送到远端。

## 发布前检查

### 代码状态

- 工作区不包含无关改动。
- PR 已完成 review。
- 数据库脚本、接口文档、前端路由文档已同步。
- 版本号已在 Maven、前端 package、README 或部署脚本中保持一致。

### 后端验证

至少执行：

```bash
mvn -pl xiaou-application -am -DskipTests compile
```

如发布只涉及单模块，可额外执行单模块编译：

```bash
mvn -pl xiaou-team -am -DskipTests compile
```

涉及高风险逻辑时，应补充单元测试、集成测试或接口冒烟验证。

### 前端验证

用户端：

```bash
cd vue3-user-front
npm run build
```

管理端：

```bash
cd vue3-admin-front
npm run build
```

如改动涉及页面交互，应补充浏览器冒烟验证或截图说明。

### 数据库验证

- 新增表、字段、索引、约束时，确认 SQL 可重复评审。
- 避免破坏旧数据。
- 明确是否需要迁移脚本、默认值或灰度步骤。
- 更新 `sql/` 和 `docs-site/reference/database-tables.md`。

### 安全验证

涉及登录、权限、文件、WebSocket、富文本、AI 外部调用、积分和后台高权限操作时，必须说明：

- 权限边界。
- 输入校验。
- 失败态。
- 回滚方案。
- 是否涉及敏感信息。

## 发布步骤

1. 从目标基线创建发布分支。
2. 完成代码、文档、SQL 和版本号调整。
3. 执行必要的构建、测试和冒烟验证。
4. 更新 `CHANGELOG.md`。
5. 提交 Pull Request，并使用 PR 模板填写验证结果。
6. 合并后创建 Git tag。
7. 推送 tag 和发布分支。
8. 如使用 GitHub Release，复制 `CHANGELOG.md` 中对应版本内容作为 Release Notes。

## Git 命令示例

```bash
git checkout -b release/v2.2.1
git status
git add <changed-files>
git commit -m "chore(release): prepare v2.2.1"
git push -u origin release/v2.2.1
```

创建标签：

```bash
git tag -a v2.2.1 -m "Code Nest v2.2.1"
git push origin v2.2.1
```

## 回滚策略

发布前必须明确：

- 是否可以直接回滚代码。
- 数据库变更是否向后兼容。
- 是否需要保留旧配置。
- 是否需要关闭某个功能开关。
- 回滚后前端资源、缓存、定时任务和消息队列如何处理。

## 发布说明模板

```markdown
## Summary

- N/A

## Highlights

- N/A

## Fixes

- N/A

## Migration

- Database:
- Config:
- Data:

## Verification

- Backend:
- User frontend:
- Admin frontend:
- Smoke test:

## Risks

- N/A

## Rollback

- N/A
```
