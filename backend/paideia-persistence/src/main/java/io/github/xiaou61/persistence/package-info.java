/**
 * 数据访问基础设施（MySQL 单库）。
 *
 * <p>本模块的约定，改动前请先读完：
 *
 * <ul>
 *   <li><b>只面向 MySQL</b>。多数据库适配已由用户决定不做，因此这里不引入方言抽象；
 *       SQL 可以直接使用 MySQL 的语法与特性。</li>
 *   <li><b>分页不靠拦截器改写 SQL</b>。{@code PageQuery} 提供 {@code limit()} 与 {@code offset()}，
 *       由 mapper 显式写成 {@code LIMIT #{limit} OFFSET #{offset}}。自动改写 SQL 的拦截器
 *       （以及依赖线程本地状态传递分页参数的做法）在复杂语句下会产生错误结果、且难以排查，
 *       收益不抵风险。</li>
 *   <li><b>总数由显式 count 语句提供</b>，不由框架改写生成。含 {@code GROUP BY} / {@code DISTINCT}
 *       的语句尤其不能靠自动改写得到正确总数。</li>
 *   <li>SQL 只写在 mapper 中，不在 Java 代码里拼接。</li>
 * </ul>
 */
package io.github.xiaou61.persistence;
