# Pull Request

## Summary

<!-- 用 2-5 条说明本 PR 解决了什么问题、为什么要改、用户或系统会看到什么变化。 -->

- N/A

## Related Issues

<!-- 示例：Closes #123 / Related #456。没有可写 N/A。 -->

- N/A

## Change Type

- [ ] Feature
- [ ] Bug fix
- [ ] Refactor
- [ ] Performance
- [ ] Security
- [ ] Documentation
- [ ] Build / CI / dependency
- [ ] Database / migration
- [ ] Release

## Scope

- Affected modules:
- Affected pages/routes:
- Affected APIs/prefixes:
- Affected tables/fields:
- Affected permissions/roles:
- Affected configs/env vars:

## Behavior Changes

<!-- 说明兼容性变化、默认值变化、接口字段变化、数据库语义变化。没有可写 N/A。 -->

- N/A

## Verification

- [ ] Backend compile/test executed
- [ ] User frontend build/smoke test executed
- [ ] Admin frontend build/smoke test executed
- [ ] Database SQL reviewed or executed in test environment
- [ ] Permission/security path checked
- [ ] Error/empty/loading states checked
- [ ] Documentation updated
- [ ] Not applicable or explained below

Verification notes:

```text
Executed:
Passed:
Not verified:
Reason:
Dependencies:
```

## Docs Sync Checklist

- [ ] New user page -> updated `docs-site/reference/frontend-routes.md` and related module docs
- [ ] New admin page -> updated route, permission, and admin docs
- [ ] New Controller or route prefix -> updated `docs-site/reference/api-routes.md`
- [ ] New table, field, index, or migration -> updated `docs-site/reference/database-tables.md`
- [ ] New WebSocket event -> updated `docs-site/reference/websocket.md`
- [ ] New error code or exception behavior -> updated `docs-site/reference/response-errors.md`
- [ ] New AI Prompt, RAG, Schema, or regression case -> updated AI docs
- [ ] New release or high-risk fix -> updated `CHANGELOG.md` and release notes
- [ ] Documentation not required; reason:

## Security Checklist

- [ ] No secrets, tokens, passwords, cookies, or private user data are committed
- [ ] Authorization and role boundaries are unchanged or documented
- [ ] User input is validated or sanitized
- [ ] File upload/download behavior is safe
- [ ] HTML/Markdown rendering is sanitized when applicable
- [ ] SQL changes avoid injection-prone dynamic concatenation
- [ ] Security impact not applicable; reason:

## Risk and Rollback

- Main risks:
- Rollback plan:
- Data migration rollback:
- Feature flag/config fallback:

## Screenshots or Evidence

<!-- UI 改动建议附截图；接口/后端改动可附请求、日志或测试输出摘要。 -->

- N/A

## Notes for Reviewers

<!-- 指出最希望 reviewer 关注的文件、逻辑或风险点。 -->

- 
