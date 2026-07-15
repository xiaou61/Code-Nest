<script setup>
import baseline from '../.vitepress/cache/docs-sync-baseline.json'

const generatedAt = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'medium'
}).format(new Date(baseline.generatedAt))
const upstream = baseline.upstream || '未配置'
</script>

# 文档同步基线

本页数据在启动或构建 VitePress 前自动生成，用于判断当前文档对应哪个源码提交，以及关键规模数据是否已经漂移。

## 当前基线

| 项 | 当前值 |
| --- | --- |
| Maven 项目版本 | `{{ baseline.projectVersion }}` |
| 文档工作线 | `{{ baseline.branch }}` |
| 当前提交 | `{{ baseline.shortSha }}` |
| 提交说明 | {{ baseline.subject }} |
| 上游分支 | `{{ upstream }}` |
| 工作区状态 | {{ baseline.workingTree }} |
| Markdown 页面 | {{ baseline.documentPages }} 页 |
| Maven 子模块 | {{ baseline.mavenModules }} 个 |
| 主库基线表 | {{ baseline.databaseTables }} 张 |
| 生成时间 | {{ generatedAt }} |

> {{ baseline.note }}

## 数据来源

| 指标 | 权威来源 |
| --- | --- |
| 项目版本、模块数 | 根 `pom.xml` |
| 文档页数 | `docs-site` 下的 Markdown 文件，排除 `.vitepress` 和 `node_modules` |
| 数据库表数 | `sql/MySql/code_nest.sql` 中的 `CREATE TABLE` |
| 分支、提交、工作区 | 当前 Git 仓库 |

## 自动校验

```bash
cd docs-site
npm run audit
npm run build
```

`npm run audit` 检查：

1. 每个 Markdown 页面是否进入 VitePress 导航。
2. 各一级分类是否有总览页。
3. 首页、架构页是否仍保留已知过时统计。
4. 是否继续把原生 WebSocket 错写成 STOMP。
5. 是否出现超过 1200 行、需要拆分的超长页面。

## 发现漂移时怎么处理

| 漂移类型 | 处理方式 |
| --- | --- |
| 模块数变化 | 更新架构总览、后端模块页和模块导航 |
| 数据表变化 | 更新数据库架构页、数据表索引和相关模块页 |
| 新增 Controller 或路由 | 更新 API 页面、路由索引和权限边界 |
| 新增前端页面 | 更新前端应用页和前端路由索引 |
| 配置项变化 | 更新环境变量总表和部署文档 |
| 状态机或计数口径变化 | 更新对应参考索引和最小回归矩阵 |

详细维护规则见 [文档维护规范](/guide/documentation-maintenance)。
