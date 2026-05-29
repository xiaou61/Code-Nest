

## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | 模拟面试模块依赖公共底座的统一响应、并发工具和异常处理 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 面试会话和求职作战台需要用户登录态 |
| [AI Runtime](/modules/ai-runtime) | 强依赖 | AI 出题、评价、总结和求职分析依赖 AI Runtime |
| [用户账户与个人中心](/modules/user-account) | 强依赖 | 用户面试记录和成长数据依赖用户信息 |
| [积分与抽奖](/modules/points) | 间接关联 | 完成面试可能触发积分奖励 |
| [题库与成长闭环](/modules/interview-and-growth) | 强依赖 | 模拟面试与成长闭环紧密关联 |
| [系统运营后台](/modules/system-ops) | 被依赖 | 面试管理界面在管理端 |
