

## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | SQL 优化模块依赖公共底座的统一响应、分页和异常处理 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | SQL 分析和优化需要用户登录态 |
| [AI Runtime](/modules/ai-runtime) | 强依赖 | SQL 分析和优化建议依赖 AI Runtime |
| [用户账户与个人中心](/modules/user-account) | 被依赖 | 用户优化记录依赖用户信息 |
| [积分与抽奖](/modules/points) | 间接关联 | SQL 优化可能触发积分奖励 |
| [系统运营后台](/modules/system-ops) | 被依赖 | SQL 优化管理界面在管理端 |
