# 简历系统

简历系统提供模板中心、在线编辑、版本管理、分析报告和导出能力，是求职闭环的重要入口。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/resume`、`/resume/templates`、`/resume/editor` |
| 管理端 | `/resume/templates`、`/resume/analytics`、`/resume/reports` |
| 后端 | `xiaou-resume` |

## 推荐学习顺序

第一次读这个模块时，不建议先看导出代码。更顺的路径是：

1. 先从模板中心理解“简历长什么样”：模板决定样式、分类、标签和经验级别。
2. 再看用户创建简历：主表保存基本信息，模块表保存经历、项目、技能等块。
3. 接着看更新逻辑：每次保存都会重建模块并写入版本快照。
4. 最后看预览、导出、分享和统计：这些是把简历交付给外部用户或招聘场景的能力。

这样读下来会发现，简历系统不是单纯的表单，而是一个“结构化内容 + 版本记录 + 文件交付”的小型内容平台。

## 源码地图

| 关注点 | 源码位置 |
| --- | --- |
| 用户端简历接口 | `xiaou-resume/src/main/java/com/xiaou/resume/controller/user/ResumeUserController.java` |
| 用户端模板接口 | `xiaou-resume/src/main/java/com/xiaou/resume/controller/user/ResumeTemplateController.java` |
| 管理端模板维护 | `xiaou-resume/src/main/java/com/xiaou/resume/controller/admin/ResumeTemplateAdminController.java` |
| 管理端分析和健康报告 | `xiaou-resume/src/main/java/com/xiaou/resume/controller/admin/ResumeAnalyticsAdminController.java` |
| 简历主流程 | `xiaou-resume/src/main/java/com/xiaou/resume/service/impl/ResumeServiceImpl.java` |
| 模板主流程 | `xiaou-resume/src/main/java/com/xiaou/resume/service/impl/ResumeTemplateServiceImpl.java` |
| PDF/Word/HTML 生成 | `xiaou-resume/src/main/java/com/xiaou/resume/service/support/ResumeExportBuilder.java` |
| 初始化 SQL | `sql/MySql/code_nest.sql`、`sql/v1.6.0/resume_module.sql` |

## 用户侧能力

- 查看我的简历。
- 从模板中心选择模板。
- 在线编辑简历模块。
- 保存简历版本。
- 查看或生成分析结果。
- 按需导出或分享。

## 管理侧能力

- 简历模板维护。
- 简历使用情况分析。
- 简历报告查看。

## 与 AI 的关系

简历系统和求职作战台存在天然连接：

- JD 解析需要读取岗位目标。
- 简历匹配需要读取简历内容。
- 面试复盘可以反向生成优化建议。
- 学习驾驶舱可以把简历短板转成任务。

## 简历数据结构

核心服务是 `ResumeServiceImpl`，主要数据拆成五类：

| 表/实体 | 说明 |
| --- | --- |
| `resume_info` | 简历主表，包含名称、模板、摘要、状态、可见性和版本号 |
| `resume_section` | 简历模块，按 `sectionType`、标题、内容和排序保存 |
| `resume_version` | 每次创建或更新后的快照 |
| `resume_share` | 分享链接、分享码、有效期和访问次数 |
| `resume_analytics` | 浏览、导出、分享和最近访问时间 |

创建简历时会写主表和模块表，保存一次“首次创建”版本快照，并初始化统计记录。更新简历时会删除旧模块、插入新模块、版本号加 1，再保存“内容更新”快照。

## 从创建到版本快照

用户保存简历时，后端按下面顺序执行：

1. 校验模板存在。模板不存在直接返回“模板不存在”。
2. 写入 `resume_info`，默认版本号为 1。
3. 把前端传入的 `sections` 转成 `resume_section` 记录；如果没有传排序，后端按 1、2、3 自动补齐。
4. 调用 `saveVersionSnapshot`，把简历主信息和模块列表序列化到 `resume_version.snapshot`。
5. 调用 `ensureAnalyticsRecord` 初始化浏览、导出、分享统计。

更新简历和创建不同：它不会逐条 diff 模块，而是删除旧模块后整体重建。这样实现简单，但也意味着前端必须在更新时提交完整模块列表；如果只提交改动过的模块，其它模块会被清空。

## 用户接口分组

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/resume` | `POST` | 创建简历 |
| `/resume/{id}` | `PUT` | 更新简历，模块整体重建 |
| `/resume/{id}` | `DELETE` | 删除简历、模块、版本、分享和统计 |
| `/resume` | `GET` | 查询个人简历列表 |
| `/resume/{id}/preview` | `GET` | 预览简历并增加浏览统计 |
| `/resume/{id}/export` | `POST` | 导出 PDF、Word 或 HTML |
| `/resume/{id}/share` | `POST` | 生成或复用分享链接 |
| `/resume/{id}/analytics` | `GET` | 查询本人简历统计 |

这些接口都显式调用 `StpUserUtil.checkLogin()`，并且通过 `selectByIdAndUserId` 限制只能访问自己的简历。

## 导出和文件存储

导出接口是 `/resume/{id}/export`，生成逻辑在 `ResumeExportBuilder`：

| 格式 | 内容类型 | 生成方式 |
| --- | --- | --- |
| `PDF` | `application/pdf` | OpenPDF，A4，使用 `STSong-Light` 支持中文 |
| `WORD`、`DOC`、`DOCX` | Word MIME | Apache POI `XWPFDocument` |
| `HTML` | `text/html;charset=UTF-8` | HTML 字符串，输出前做 HTML escape |

导出文件生成后会封装成内存 MultipartFile，通过文件存储模块上传到 `resume/export-{format}` 目录，返回下载地址，并增加导出统计。

导出请求还支持两个重要参数：

| 参数 | 行为 |
| --- | --- |
| `theme` | 写入 PDF/Word/HTML 的主题提示，目前主要用于展示元信息 |
| `watermark` | 为导出内容追加“仅供某简历使用”的水印文字 |

文件名规则是：

```text
{简历名称}-{版本号}-{yyyyMMddHHmmss}.{扩展名}
```

名称会过滤不适合文件名的字符，只保留英文、数字、中文、下划线和短横线。

## 分享和统计

分享链接有效期为 7 天。创建分享时如果已有未过期链接，会直接复用；否则生成 8 位大写随机码，分享地址为 `/resume/share/{code}`。

预览会增加浏览数并更新最后访问时间。导出会增加导出数。分享会增加分享数。

## 模板管理

模板服务是 `ResumeTemplateServiceImpl`。模板包含名称、分类、描述、封面、预览图、标签、技术栈、经验级别和状态。创建模板时记录创建人和更新时间，初始评分、评分数和下载数为 0。

管理端模板接口都带 `@RequireAdmin`，默认只有管理员可以创建、更新和删除模板。用户端只读取启用模板，用于选择和预览。

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 更新后模块丢失 | 后端更新时先删旧模块再插入新模块 | 前端保存时提交完整 `sections` |
| 导出中文乱码 | PDF 需要中文字体支持 | 当前使用 `STSong-Light`，部署环境要确认字体可用 |
| 分享次数看起来没增加 | 有效期内重复生成分享会复用旧链接 | 只有新建分享链接时增加分享计数 |
| 简历统计不存在 | 老数据可能没有统计行 | 访问预览、导出、分享、统计时会自动补建 |
| 非本人访问返回“无权访问” | 服务统一用用户 ID 限定 | 调试时确认登录态和简历归属 |

## 健康检查

后台健康报告会扫描：

- 没有任何模块内容的简历，通常是保存时漏传 `sections`。
- 已过期但待清理的分享链接，后续可以接定时清理任务。

## 学习和验证清单

| 场景 | 预期 |
| --- | --- |
| 创建简历时模板不存在 | 返回模板不存在 |
| 非本人访问简历 | 返回无权访问该简历 |
| 更新简历 | 模块被重建，版本号加 1，写入版本快照 |
| 导出不支持格式 | 返回暂不支持的导出格式 |
| 重复创建分享链接 | 有效期内复用原链接 |
| 删除简历 | 主表、模块、版本、分享、统计一起清理 |
| 预览简历 | 返回模板、模块、版本，并增加浏览数 |
| 导出简历 | 文件上传到文件存储，并增加导出数 |
| 健康报告 | 能发现无模块简历和过期分享 |
