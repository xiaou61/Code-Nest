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

## 验证清单

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



---

## 深度拆解

### 一、简历创建深度分析

`ResumeServiceImpl.createResume` 完整流程：

```text
1. resumeTemplateMapper.selectById(templateId) → null → 抛"模板不存在"
2. 构建 ResumeInfo:
   ├─ version = 1
   ├─ status = request.status ?? 0 (默认草稿)
   ├─ visibility = request.visibility ?? 0
   ├─ createTime = updateTime = now
3. resumeInfoMapper.insert(resume) → useGeneratedKeys 回填 id
4. buildSections(resume.id, request.sections, now):
   ├─ items == null || empty → return 空列表 (允许无模块简历)
   ├─ sortOrder == null → 自动从 1 递增
   └─ status == null → 默认 1
5. sections 非空 → resumeSectionMapper.insertBatch(sections)
6. saveVersionSnapshot(resume, sections, "首次创建"):
   ├─ buildSnapshot → JSONUtil.toJsonStr({resume, sections})
   └─ resumeVersionMapper.insert(version)
7. ensureAnalyticsRecord(resumeId):
   ├─ selectByResumeId → null → insert(0/0/0/0 初始统计)
   └─ return analytics
8. return resume.id
```

**模板校验只查存在**：创建时 `selectById(templateId)` 不校验模板 `status`——即使用禁用模板也能创建简历。用户端模板列表可以过滤 `status=1`，但创建接口没有做同样的校验。

**允许无模块简历**：`request.sections` 为 null 或空时，`buildSections` 返回空列表，不执行 `insertBatch`。简历主表仍然写入，版本快照的 sections 为空。这类简历会被健康报告标记为"缺少任何模块内容"。

### 二、简历更新与模块重建深度分析

`ResumeServiceImpl.updateResume` 完整流程：

```text
1. getOwnedResume(resumeId, userId) → 校验存在和归属
2. templateId 变化时 → selectById(newTemplateId) → null → 抛"模板不存在"
3. 构建 ResumeInfo 更新对象:
   ├─ version = dbResume.version + 1
   ├─ updateTime = now
4. resumeInfoMapper.updateById(update) → 动态 SET（只更新非 null 字段）
5. resumeSectionMapper.deleteByResumeId(resumeId) → 物理删除所有旧模块
6. buildSections(resumeId, request.sections, now) → 新模块列表
7. sections 非空 → insertBatch
8. dbResume.set*() → 手动同步更新后的字段到 dbResume 对象
9. saveVersionSnapshot(dbResume, sections, "内容更新")
```

**模块整体重建**：更新时先 `deleteByResumeId` 再 `insertBatch`，不做 diff。这意味着：

1. 前端必须提交完整模块列表，遗漏的模块会被清空。
2. 旧模块的 `id` 全部丢失，新建的模块获得新 `id`——如果前端引用了模块 `id`（如编辑时的锚点），更新后会失效。
3. `createTime` 全部被重置为 `now`——模块的创建时间信息丢失。

**版本号递增无上限**：每次更新 `version = oldVersion + 1`，没有上限检查。频繁保存会导致版本号持续增长。

**dbResume 手动同步**：`updateResume` 在数据库更新后，手动把 request 的字段 set 到 `dbResume` 对象上（第 104-110 行），再传给 `saveVersionSnapshot`。如果漏了某个字段，版本快照会使用旧值而不是新值——**这是一个容易出错的维护点**。

**模板变更校验不对称**：创建时校验模板存在；更新时只在 `templateId` 变化时校验。如果 `request.templateId` 等于当前值但不校验 `status`，和创建时一样——禁用模板不会被拦截。

### 三、删除级联深度分析

`ResumeServiceImpl.deleteResume`：

```text
1. getOwnedResume → 校验存在和归属 (但不使用返回值)
2. resumeSectionMapper.deleteByResumeId → DELETE FROM resume_sections
3. resumeVersionMapper.deleteByResumeId → DELETE FROM resume_versions
4. resumeShareMapper.deleteByResumeId → DELETE FROM resume_shares
5. resumeAnalyticsMapper.deleteByResumeId → DELETE FROM resume_analytics
6. resumeInfoMapper.deleteById → DELETE FROM resume_info
```

**物理删除**：所有删除操作都是 `DELETE FROM`，不是软删除。删除后数据不可恢复。

**级联顺序**：先删子表（sections → versions → shares → analytics），最后删主表。顺序正确，不会留下孤儿数据。

**getOwnedResume 只校验不使用**：`getOwnedResume` 的返回值没有被使用——只用来校验简历存在且属于当前用户。这意味着删除操作不读取简历的任何属性，直接按 ID 删除。

**删除不清理文件存储**：简历导出生成的 PDF/Word/HTML 文件存储在文件存储模块中，删除简历不会清理这些导出文件。如果文件存储没有引用简历 ID，这些文件会成为孤儿文件。

### 四、分享链接深度分析

`ResumeServiceImpl.createShareLink`：

```text
1. getOwnedResume(resumeId, userId) → 校验归属
2. resumeShareMapper.selectActiveShare(resumeId, now):
   ├─ WHERE resume_id = #{resumeId} AND status = 1 AND expire_time > #{now}
   ├─ ORDER BY update_time DESC LIMIT 1
   └─ 已存在 → 直接返回 (复用)
3. 生成 8 位大写随机码 → RandomUtil.randomString(8).toUpperCase()
4. 构建 ResumeShare:
   ├─ shareUrl = "/resume/share/" + code
   ├─ expireTime = now.plusDays(7)
   ├─ status = 1, accessCount = 0
5. resumeShareMapper.insert(share)
6. ensureAnalyticsRecord → resumeAnalyticsMapper.increaseShareCount
7. return toShareResponse(share)
```

**分享码碰撞风险**：`RandomUtil.randomString(8)` 生成 8 位随机字符串（大小写字母 + 数字，约 62 个字符），8 位码有 62^8 ≈ 2.18×10^14 种组合，碰撞概率极低。但 `selectActiveShare` 只查当前简历的活跃分享——**不检查全局唯一性**。如果两张不同简历碰巧生成相同 `shareCode`，分享 URL 会指向后插入的那张简历。

**分享链接无访问计数更新**：`ResumeShare` 有 `accessCount` 字段，但 Mapper 只有 `updateById` 方法，没有 `increaseAccessCount`。也就是说，分享链接被访问时 `accessCount` 不会自动增加——**这个字段从未被使用**。

**过期分享无清理任务**：`selectExpiredShares` 查询所有已过期且 `status=1` 的分享，只在健康报告中标记。没有定时任务自动清理过期分享或将 `status` 更新为 0。

### 五、统计机制深度分析

`ResumeServiceImpl` 的统计通过 `resume_analytics` 表维护：

| 操作 | 触发方法 | SQL |
| --- | --- | --- |
| 浏览 | `previewResume` | `SET view_count = view_count + 1` |
| 导出 | `exportResume` | `SET export_count = export_count + 1` |
| 分享 | `createShareLink`（新建时） | `SET share_count = share_count + 1` |
| 最后访问 | `previewResume` | `SET last_access_time = #{time}` |

**ensureAnalyticsRecord 惰性初始化**：统计记录不在简历创建时强制写入——`createResume` 调用 `ensureAnalyticsRecord`，但 `previewResume`、`exportResume`、`createShareLink` 和 `getResumeAnalytics` 都也会调用。如果创建时的 insert 失败（如事务部分回滚），后续访问会重新创建。

**uniqueVisitors 未实现**：`resume_analytics` 有 `unique_visitors` 字段，但服务层没有任何逻辑更新这个值——始终为初始值 0。这意味着 `getResumeAnalytics` 返回的 `uniqueVisitors` 永远为 0。

**平台统计全量聚合**：`getPlatformAnalytics` 调用 `aggregateAll` 做 `SUM(view_count)` 等全表聚合。当简历数量增长到数千以上时，全表聚合可能变慢——没有分页或缓存。

### 六、导出机制深度分析

`ResumeServiceImpl.exportResume`：

```text
1. buildPreview(resumeId, userId) → 校验归属 + 查简历、模板、模块、版本
2. renderPlainText(preview) → 生成纯文本预览内容
3. format = StrUtil.blankToDefault(request.format, "PDF")
4. resumeExportBuilder.buildFile(preview, request) → 生成导出文件
5. ByteArrayMultipartFile → 内存 MultipartFile
6. fileStorageService.uploadSingle → 上传到文件存储
7. uploadResult 失败 → 抛"导出文件上传失败"
8. 构建 ResumeExportResponse
9. ensureAnalyticsRecord → increaseExportCount
```

**PDF 生成** `buildPdf`：

```text
1. Document(A4, 36/36/48/36 margin)
2. BaseFont = STSong-Light + IDENTITY_H (中文支持)
3. titleFont(18/BOLD), sectionFont(14/BOLD), contentFont(12), metaFont(10/ITALIC)
4. 添加标题 → 添加摘要 → 添加主题 → 逐模块添加标题+内容
5. watermark=true → 添加"仅供 {name} 使用"段落
6. document.close → return byte[]
```

**PDF 水印不是真水印**：`watermark` 参数只是在文档末尾追加一个普通段落文本"仅供 xxx 使用"，不是 PDF 层级的水印。这个文本可以被删除，不影响文档主体内容。真正的 PDF 水印应该使用 `PdfContentByte` 在每页叠加半透明文字。

**Word 生成** `buildWord`：使用 `SimSun`（宋体）字体家族。与 PDF 的 `STSong-Light` 不同，部署环境需要安装 `SimSun` 字体。跨平台部署时字体不一致可能导致样式差异。

**HTML 生成** `buildHtml`：所有文本都经过 `HtmlUtils.htmlEscape` 转义，防止 XSS。CSS 是内联的硬编码样式（Microsoft YaHei, 卡片式布局），不可配置模板样式。

**导出内容只含纯文本**：所有三种格式只输出 `sectionContent`（纯文本），不支持模块内容中的富文本、HTML 或 Markdown 格式。如果前端编辑器支持富文本，导出后会丢失所有格式。

### 七、模板管理深度分析

`ResumeTemplateServiceImpl`：

**创建模板** `createTemplate`：

```text
1. mapToEntity(request) → tags/techStack 用 String.join(",", list) 逗号拼接
2. rating=0.0, ratingCount=0, downloadCount=0
3. createdBy = StpAdminUtil.getLoginIdAsLong()
4. insert → return id
```

**标签存储格式**：`tags` 和 `techStack` 用逗号拼接的字符串存储（如 "Java,Spring,MySQL"），不是 JSON 或关联表。查询时用 `FIND_IN_SET(#{tag}, tags)`，这无法利用索引，大数据量下性能差。

**模板评分系统预留**：`rating`、`ratingCount` 和 `downloadCount` 创建时初始化为 0，但没有任何接口更新这些值——评分和下载计数功能未实现。

**模板删除无级联检查**：`deleteTemplate` 直接物理删除模板，不检查是否有简历正在使用该模板。如果简历的 `templateId` 指向被删除的模板，简历预览中的 `template` 字段会返回 null。

**用户端模板查询无 status 过滤**：`ResumeTemplateController.listTemplates` 调用 `listTemplates(request)`，`selectList` SQL 只有 `<if test="request.status != null">` 才加 `AND status = #{request.status}`。如果前端不传 `status`，禁用模板也会出现在用户端列表中。

### 八、健康报告深度分析

`ResumeServiceImpl.getHealthReports`：

```text
1. selectResumesWithoutSections → 查无模块简历
   ├─ NOT EXISTS (SELECT 1 FROM resume_sections WHERE resume_id = ri.id)
   ├─ severity = 2 (较严重)
   └─ issue = "简历缺少任何模块内容"
2. selectExpiredShares(now) → 查过期分享
   ├─ WHERE status = 1 AND expire_time <= #{now}
   ├─ severity = 1 (较轻)
   └─ issue = "存在过期分享链接待清理"
   └─ resumeName = "RESUME-" + resumeId (硬编码前缀)
```

**无模块简历检测**：`selectResumesWithoutSections` 使用 `NOT EXISTS` 子查询，这在简历数量大时可能变慢。可以考虑改为 `LEFT JOIN + IS NULL` 或在主表加 `section_count` 字段。

**过期分享简历名硬编码**：过期分享的健康报告中，`resumeName` 被设为 `"RESUME-" + share.getResumeId()`，没有通过 JOIN 或子查询获取实际简历名。如果简历已被删除，这个硬编码名称对管理员没有参考意义。

### 九、版本快照深度分析

`ResumeServiceImpl.saveVersionSnapshot`：

```text
1. 构建 ResumeVersion:
   ├─ versionNumber = resume.getVersion()
   ├─ snapshot = JSONUtil.toJsonStr({resume: ResumeInfo, sections: List<ResumeSection>})
   ├─ changeLog = "首次创建" 或 "内容更新"
   ├─ createdBy = resume.getUserId()
2. resumeVersionMapper.insert(version)
```

**快照内容包含完整简历主表**：`buildSnapshot` 把整个 `ResumeInfo` 和 `List<ResumeSection>` 序列化为 JSON。这意味着每次保存都会重复存储简历名称、模板 ID、summary 等不变或少量变化的字段。版本多时，`resume_versions` 表的数据量会快速增长。

**没有版本回滚接口**：虽然版本快照保存了完整数据，但没有提供从某个版本回滚的接口。快照数据只能查看，不能恢复。

**版本号与快照不一致风险**：`updateResume` 中先 `updateById` 写入 `version = oldVersion + 1`，再 `saveVersionSnapshot`。如果快照写入失败（如 JSON 过大超限），数据库中的 version 已经递增但快照缺失这个版本——**事务覆盖整个方法**（`@Transactional(rollbackFor = Exception.class)`），所以理论上不会发生，但如果 JSON 序列化本身不抛异常而是返回截断结果，事务不会回滚。

### 十、深度发现与坑点

#### 10.1 确认 BUG

| 编号 | BUG | 位置 | 说明 |
| --- | --- | --- | --- |
| BUG-1 | 模板校验不检查 status | `createResume/updateResume` | 禁用模板也能创建/更新简历 |
| BUG-2 | 模块重建丢失 id 和 createTime | `updateResume:98-102` | deleteByResumeId 后 insertBatch，旧 id 和时间全部丢失 |
| BUG-3 | 分享码全局不唯一 | `createShareLink:182` | 8 位随机码不查全局唯一性 |
| BUG-4 | 分享 accessCount 未使用 | `ResumeShare.accessCount` | Mapper 没有 increaseAccessCount 方法 |
| BUG-5 | uniqueVisitors 永远为 0 | `ResumeAnalytics.uniqueVisitors` | 无任何逻辑更新此字段 |
| BUG-6 | PDF 水印是普通段落 | `buildPdf:83-85` | 不是真正的 PDF 水印，可以被删除 |
| BUG-7 | 模板删除不检查关联简历 | `deleteTemplate` | 删除模板后使用该模板的简历 template 引用为 null |
| BUG-8 | 用户端模板查询不强制过滤禁用 | `ResumeTemplateController` | 不传 status 时禁用模板也可见 |
| BUG-9 | 删除简历不清理导出文件 | `deleteResume` | 文件存储中的导出文件成为孤儿 |
| BUG-10 | dbResume 手动同步字段 | `updateResume:104-110` | 容易遗漏字段导致版本快照使用旧值 |

#### 10.2 设计风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 版本快照膨胀 | 每次保存完整 JSON，频繁更新简历时 snapshot 表快速增长 |
| RISK-2 | 全表聚合无缓存 | getPlatformAnalytics 对 analytics 表做全量 SUM，简历多时慢 |
| RISK-3 | 导出内容丢失格式 | 只导出纯文本，富文本/Markdown 内容格式丢失 |
| RISK-4 | 过期分享无定时清理 | 只有健康报告标记，没有自动清理任务 |
| RISK-5 | FIND_IN_SET 无法索引 | 模板标签用逗号拼接字符串存储，查询靠 FIND_IN_SET |
| RISK-6 | 物理删除不可恢复 | 所有删除操作都是 DELETE FROM，没有软删除或回收机制 |
| RISK-7 | Word 字体跨平台不一致 | PDF 用 STSong-Light，Word 用 SimSun，不同环境可能缺字体 |

#### 10.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 模块整体重建 | 删除旧模块再批量插入，实现简单、不需要 diff 逻辑 |
| H-2 | 版本快照自动保存 | 创建和更新都保存快照，历史可追溯 |
| H-3 | 分享链接复用 | 有效期内不重复生成，减少分享码冗余 |
| H-4 | 惰性初始化统计 | ensureAnalyticsRecord 在需要时才创建，避免空数据 |
| H-5 | 三格式导出统一入口 | PDF/Word/HTML 通过同一个 ExportedFile record 返回 |
| H-6 | 文件名字符过滤 | 安全化文件名，只保留英文数字中文下划线短横线 |
| H-7 | HTML 全量 XSS 转义 | HtmlUtils.htmlEscape 保护所有输出文本 |
| H-8 | 归属校验 getOwnedResume | selectByIdAndUserId 确保用户只能访问自己的简历 |
| H-9 | 健康报告自动巡检 | 无模块简历和过期分享自动检测 |
| H-10 | 导出计数原子递增 | SET export_count = export_count + 1 并发安全 |

#### 10.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 创建简历 | `ResumeServiceImpl.java` — createResume + 模板校验 + 版本快照 |
| 更新简历 | `ResumeServiceImpl.java` — updateResume + 模块重建 + dbResume 手动同步 |
| 删除简历 | `ResumeServiceImpl.java` — deleteResume + 五表级联物理删除 |
| 预览简历 | `ResumeServiceImpl.java` — previewResume + buildPreview + 浏览计数 |
| 导出简历 | `ResumeServiceImpl.java` — exportResume + 文件存储上传 |
| PDF 生成 | `ResumeExportBuilder.java` — buildPdf + STSong-Light + 水印段落 |
| Word 生成 | `ResumeExportBuilder.java` — buildWord + SimSun + 逐行写入 |
| HTML 生成 | `ResumeExportBuilder.java` — buildHtml + htmlEscape + 卡片样式 |
| 分享链接 | `ResumeServiceImpl.java` — createShareLink + 复用 + 8 位随机码 |
| 统计 | `ResumeServiceImpl.java` — ensureAnalyticsRecord + increaseView/Export/Share |
| 平台统计 | `ResumeServiceImpl.java` — getPlatformAnalytics + aggregateAll |
| 健康报告 | `ResumeServiceImpl.java` — getHealthReports + 无模块检测 + 过期分享 |
| 模板创建 | `ResumeTemplateServiceImpl.java` — createTemplate + tags 逗号拼接 |
| 模板查询 | `ResumeTemplateMapper.xml` — selectList + FIND_IN_SET |
| 版本快照 | `ResumeServiceImpl.java` — saveVersionSnapshot + buildSnapshot + JSONUtil |
| 简历主表 | `ResumeInfo.java` — userId + templateId + version + status + visibility |
| 简历模块 | `ResumeSection.java` — sectionType + title + content + sortOrder |
| 分享记录 | `ResumeShare.java` — shareCode + expireTime + accessCount(未使用) |
| 统计记录 | `ResumeAnalytics.java` — view/export/share/uniqueVisitors(未使用) |
| 模板 | `ResumeTemplate.java` — category + tags(逗号) + techStack(逗号) + rating(预留) |
