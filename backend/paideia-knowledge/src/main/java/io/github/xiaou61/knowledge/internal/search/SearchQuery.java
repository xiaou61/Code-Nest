package io.github.xiaou61.knowledge.internal.search;

/**
 * 检索串的归一化与"走不走全文索引"的判定。
 *
 * <p>纯函数，不碰数据库——这是本项目里少数几处**必须能单测**的逻辑，因为它同时决定
 * 召回率（中文能不能搜到）与安全性（LIKE 通配符要不要转义）。
 *
 * <p><b>为什么需要它</b>：知识库的全文索引带 `WITH PARSER ngram`，而 ngram 按固定长度
 * 切词（默认 2）。长度小于该值的查询（例如单个汉字）在全文索引上**召回为零**，不是"结果少"
 * 而是"永远搜不到"。所以短查询必须退回 `LIKE`，否则用户搜一个字会得到空结果并以为功能坏了。
 */
public final class SearchQuery {

    /** MySQL ngram 解析器的默认分词长度。短于此长度的查询走 LIKE。 */
    public static final int NGRAM_TOKEN_SIZE = 2;

    private SearchQuery() {
    }

    /** 去空白；空串与 null 都归一为 null（表示"没有检索条件"）。 */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 是否长到能走全文索引。null 视为否。 */
    public static boolean usesFulltext(String normalized) {
        return normalized != null
                && normalized.codePointCount(0, normalized.length()) >= NGRAM_TOKEN_SIZE;
    }

    /**
     * 构造 LIKE 用的模式串，并**转义 LIKE 的通配符**。
     *
     * <p>不转义的话，用户搜 `%` 会匹配全部条目、搜 `_` 会匹配任意单字——不是 SQL 注入
     * （参数仍是绑定的），而是"用户的字面输入被当成通配符"，属于信任边界上的输入处理，
     * 不能省。
     *
     * <p>依赖 MySQL 的默认 LIKE 转义字符是反斜杠（未设 `NO_BACKSLASH_ESCAPES`）。
     * 若将来改了这个 sql_mode，这里要显式写成 `ESCAPE` 子句。
     */
    public static String likePattern(String normalized) {
        StringBuilder out = new StringBuilder(normalized.length() + 2).append('%');
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current == '\\' || current == '%' || current == '_') {
                out.append('\\');
            }
            out.append(current);
        }
        return out.append('%').toString();
    }
}
