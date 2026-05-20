## Summary

- 

## Change Type

- [ ] Docs only
- [ ] Backend
- [ ] User frontend
- [ ] Admin frontend
- [ ] AI / RAG
- [ ] OJ / go-judge
- [ ] WebSocket / chat
- [ ] File upload / storage
- [ ] Deploy / infra / monitoring

## Scope

- Affected modules:
- Affected pages/routes:
- Affected APIs/prefixes:
- Affected tables/fields:

## Verification

- [ ] `docs-site`: `npm run build`
- [ ] Backend build / tests
- [ ] User frontend build / smoke test
- [ ] Admin frontend build / smoke test
- [ ] AI / RAG regression
- [ ] OJ / go-judge smoke test
- [ ] WebSocket / ACK / failure path
- [ ] File upload and URL access

Verification notes:

```text
已执行：
通过项：
未验证：
未验证原因：
依赖状态：
```

## Docs Sync Checklist

- [ ] New user page -> updated `docs-site/reference/frontend-routes.md`, module page, and user manual when needed
- [ ] New admin page -> updated `docs-site/reference/frontend-routes.md`, module page, and admin manual when needed
- [ ] New Controller or route prefix -> updated `docs-site/reference/api-routes.md`
- [ ] New table or field -> updated `docs-site/reference/database-tables.md` and related module page
- [ ] New WebSocket event -> updated `docs-site/reference/websocket.md`
- [ ] New error code -> updated `docs-site/reference/response-errors.md`
- [ ] New AI Prompt / RAG / Schema / regression -> updated `docs-site/reference/ai-schemas.md` and `docs-site/modules/ai-runtime.md`
- [ ] New deploy flow / risk / high-risk fix -> updated `docs-site/guide/release-verification.md` and `docs-site/manuals/verified-scenarios.md`
- [ ] New screenshot or verification result -> updated manuals or verified scenarios when needed
- [ ] New docs batch -> updated `docs-site/roadmap/v2.2.0-docs-plan.md` when needed

## Risk

- Main risk:
- Rollback / downgrade plan:

## Notes for Reviewers

- 
