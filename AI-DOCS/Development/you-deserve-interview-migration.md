# You Deserve 面试题库迁移方案

## 结论

`F:\you-deserve` 是八股/面试复习内容站，不是 OJ 判题题库。它缺少输入输出样例、测试用例、判题约束和语言模板，所以不建议导入 `xiaou-oj`。

主迁移目标为 `xiaou-interview`：

- `interview_category` 承载源 `route`
- `interview_question_set` 承载源 `(route, category)`
- `interview_question` 承载单个 Markdown 题目

## Dry-run 结果

- 题目数：1925
- 题单数：41
- 路线分类数：21
- 源主题分类数：23
- 重复 slug：0
- 重复标题：0
- 缺失图解数据：0
- 最大生成答案：约 8289 bytes，未超过 MySQL `TEXT` 字段限制

## 字段映射

| You Deserve | Code-Nest | 说明 |
| --- | --- | --- |
| `route` | `interview_category.name` | 作为用户可筛选的学习路线 |
| `category` + `route` | `interview_question_set.title` | 生成如 `Redis（Java 后端上岸路线）` 的题单 |
| `title` | `interview_question.title` | 原题标题 |
| Markdown body | `interview_question.answer` | 保留原正文 |
| `slug/path/tags/difficulty/scene/summary` | `answer` 顶部来源块 | 现有表无专用列，先写入 Markdown |
| `content/visuals/question-visuals.json` | `answer` 的 `图解数据` 小节 | 保留节点、绘图提示和记忆收口 |
| `order` | `sort_order` 的组内排序依据 | 迁移时转成连续排序号 |

## 生成命令

全量 SQL：

```powershell
python scripts/you_deserve_to_interview_sql.py --source F:\you-deserve --output-dir output\you-deserve-migration --write-sql
```

小样本 SQL，每个题单最多 2 题：

```powershell
python scripts/you_deserve_to_interview_sql.py --source F:\you-deserve --output-dir output\you-deserve-migration-sample --write-sql --sample-per-set 2
```

直接导入本地 MySQL 小样本：

```powershell
python scripts/you_deserve_to_interview_sql.py --source F:\you-deserve --output-dir output\you-deserve-migration-sample --write-sql --sample-per-set 2 --apply-mysql --mysql-host 127.0.0.1 --mysql-port 3306 --mysql-user root --mysql-password <password> --mysql-database code_nest
```

直接导入本地 MySQL 全量：

```powershell
python scripts/you_deserve_to_interview_sql.py --source F:\you-deserve --output-dir output\you-deserve-migration --write-sql --apply-mysql --mysql-host 127.0.0.1 --mysql-port 3306 --mysql-user root --mysql-password <password> --mysql-database code_nest
```

生成文件：

- `output/you-deserve-migration/you_deserve_interview_summary.md`
- `output/you-deserve-migration/you_deserve_interview_manifest.json`
- `output/you-deserve-migration/you_deserve_interview_import.sql`

## SQL 行为

生成 SQL 或 `--apply-mysql` 直接导入都会：

1. 开启事务。
2. 删除 `description` 中带 `source:you-deserve` 的旧题单，依赖外键级联删除旧题目。
3. 插入或复用路线分类。
4. 插入 41 个题单和 1925 道题。
5. 刷新题单题目数和分类题单数。
6. 提交事务。

## 本地导入结果

已在本地 MySQL `code_nest` 执行迁移：

- 小样本验证：41 个题单、80 道题，计数和图解数据检查通过。
- 全量迁移：41 个题单、1925 道题。
- 最终本地库计数：分类 29、题单 42、题目 2005。
- You Deserve 数据计数：题单 41、题目 1925。
- 1925 道题均包含 `图解数据`，题单题目数无错配，无答案超过 MySQL `TEXT` 限制。

## 线上导入结果

已在 `36.140.150.167` 的线上 MySQL `code_nest` 执行迁移：

- 迁移前计数：分类 3、题单 0、题目 0、旧 You Deserve 数据 0。
- 迁移前备份：`/opt/code-nest/backup/interview-before-you-deserve-20260613122435.sql.gz`。
- 导入 SQL sha256：`700de80f943309ce4cb247dae9e91d61d266ad1d6053abf384c52f1e9c2adb40`。
- 迁移后计数：分类 24、题单 41、题目 1925。
- You Deserve 数据计数：题单 41、题目 1925。
- 1925 道题均包含 `图解数据`，题单题目数无错配，无答案超过 MySQL `TEXT` 限制。
- `http://36.140.150.167:81/api/interview/question-sets?page=1&size=5` 返回 41 个题单总数。
- `http://36.140.150.167:81/api/interview/question-sets/search` 搜索 `HashMap` 返回 29 条结果。
- 未登录访问题单题目列表接口返回业务码 `701`，属于现有登录校验行为；数据层和搜索接口已验证迁移内容可读。

## 线上删除结果

用户确认不保留本次 You Deserve 试导入内容后，已在线上库删除带 `source:you-deserve` 标记的题单，并依赖外键级联删除对应题目：

- 删除后总计数：分类 3、题单 0、题目 0。
- 删除后 You Deserve 数据计数：题单 0、题目 0。
- 线上备份仍保留：`/opt/code-nest/backup/interview-before-you-deserve-20260613122435.sql.gz`。
- 当前仓库只保留迁移工具和迁移记录，不提交生成的 SQL、manifest 或导出产物。

## 执行前检查

- 先确认目标库已经有 `interview_category`、`interview_question_set`、`interview_question` 三张表。
- 执行前备份目标数据库。
- 如果是线上库，建议先在本地或测试库执行 SQL，确认用户端面试题库列表、题单详情、题目详情、搜索和下一题/上一题正常。
- 当前 SQL 只导入内容数据，不创建学习记录、收藏记录或掌握度记录。
