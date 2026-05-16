# 敏感词风控

敏感词风控为社区、动态、博客、聊天、评论等内容型模块提供安全基础能力。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 管理端 | `/sensitive/words`、`/sensitive/whitelist`、`/sensitive/strategy`、`/sensitive/statistics`、`/sensitive/source`、`/sensitive/version`、`/sensitive/config` |
| 后端模块 | `xiaou-sensitive`、`xiaou-sensitive-api` |

## 对外检测接口

| 接口 | 能力 |
| --- | --- |
| `/sensitive/check` | 单条文本检测 |
| `/sensitive/check/batch` | 批量检测 |

## 管理能力

| 接口域 | 能力 |
| --- | --- |
| `/admin/sensitive/words` | 词库列表、详情、新增、更新、删除、批量删除、导入、导出、刷新 |
| `/sensitive/whitelist` | 白名单列表、详情、新增、更新、删除、批量删除、导入、刷新 |
| `/sensitive/strategy` | 策略配置 |
| `/sensitive/statistics` | 统计分析 |
| `/sensitive/source` | 词库来源、连接测试、同步 |
| `/sensitive/version` | 版本历史 |

## 技术亮点

- AC 自动机进行多词匹配。
- 预处理识别变形词、同音字、相似字等绕过方式。
- 词库热更新减少重启成本。
- 白名单和策略降低误判。
- 命中日志和统计形成风控闭环。

## 开发注意

- 内容发布链路要失败可控，不能因为风控服务异常拖垮主流程。
- 管理端导入需要预览和确认，避免误导入。
- 命中结果展示要避免泄露过多绕过策略。

