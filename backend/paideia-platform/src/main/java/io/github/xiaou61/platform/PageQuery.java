package io.github.xiaou61.platform;

import java.util.regex.Pattern;

/**
 * 分页查询参数。
 *
 * <p>分页状态通过方法参数显式传递，不依赖线程本地变量：线程本地分页状态一旦未被消费，
 * 会污染同线程上后续无关的查询。页码从 1 开始；页大小超过 {@link #MAX_SIZE} 时收敛到上限
 * 而不是抛错，避免一个非法入参把整个列表接口打回。
 *
 * <p><b>归一化在紧凑构造器里</b>：记录的标准构造器是 public 的，只守着 {@link #of} 等于
 * 留了一扇绕过归一化的门——`new PageQuery(-5, 0, null)` 能直接进来。
 */
public record PageQuery(int page, int size, String sort) {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    /** 允许的排序写法：`列名` 或 `列名 asc|desc`，多列用逗号分隔。 */
    private static final Pattern SORT_PATTERN = Pattern.compile(
            "[A-Za-z_][A-Za-z0-9_]*(?:\\s+(?:asc|desc))?"
                    + "(?:\\s*,\\s*[A-Za-z_][A-Za-z0-9_]*(?:\\s+(?:asc|desc))?)*",
            Pattern.CASE_INSENSITIVE);

    public PageQuery {
        page = page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
        size = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        sort = (sort == null || sort.isBlank()) ? null : sort.trim();
        if (sort != null && !SORT_PATTERN.matcher(sort).matches()) {
            // 注意：这条只保证"看起来像列名"，**不是注入防线**。真正的防线在下游——
            // 必须把这个字符串映射成白名单列名（或只允许具名参数），不得直接拼进 SQL。
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "排序字段非法：" + sort);
        }
    }

    public static PageQuery of(Integer page, Integer size, String sort) {
        return new PageQuery(
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size,
                sort);
    }

    /**
     * 返回 long：页码接近上限时 {@code (page - 1) * size} 会溢出 int。
     */
    public long offset() {
        return (long) (page - 1) * size;
    }

    /** 与 {@link #size()} 同义，供 SQL 里直接使用 {@code #{limit}} 之类的具名参数。 */
    public int limit() {
        return size;
    }
}
