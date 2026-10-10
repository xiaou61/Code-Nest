/**
 * 知识库模块。
 *
 * <p><b>职责</b>：层级分类（`knowledge_categories`）、知识条目（`knowledge_entries`）、
 * 条目关系（`knowledge_entry_relations`）、附件记录与本地文件存储（`knowledge_files`
 * 与 {@code FileStorage} 端口）、以及学习者自评（`knowledge_self_assessments`）。
 *
 * <p><b>对外契约</b>只在包根（见 {@link io.github.xiaou61.knowledge.KnowledgeApi}）；
 * `internal` 子包是内部实现，其它模块不得访问——由 Spring Modulith 在构建期拦截。
 *
 * <p><b>依赖方向是单向的</b>：本模块依赖 `paideia-platform` 的契约、`paideia-persistence`
 * 的 MyBatis 约定、`paideia-security` 的当前用户上下文。它**不依赖 `paideia-account`**：
 * 只需要"当前是谁"，不需要账号数据。
 *
 * <p><b>两条容易踩的纪律</b>：
 * <ul>
 *   <li>学习者只读浏览的查询**在 SQL 层就过滤 {@code status = 'published'}**，
 *       草稿对非管理员按 404 处理，不靠控制器判断。</li>
 *   <li>自评是本项目第一条用户私有业务数据：{@code user_id} 只取自令牌，
 *       读取一律带 {@code WHERE user_id = :currentUser}，请求里没有用户标识可传。</li>
 * </ul>
 */
package io.github.xiaou61.knowledge;
