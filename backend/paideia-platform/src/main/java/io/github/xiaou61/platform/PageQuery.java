package io.github.xiaou61.platform;

/**
 * 分页查询参数。
 *
 * <p>分页状态通过方法参数显式传递，不依赖线程本地变量：线程本地分页状态一旦未被消费，
 * 会污染同线程上后续无关的查询。页码从 1 开始；页大小超过 {@link #MAX_SIZE} 时收敛到上限
 * 而不是抛错，避免一个非法入参把整个列表接口打回。
 */
public record PageQuery(int page, int size, String sort) {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    public static PageQuery of(Integer page, Integer size, String sort) {
        int normalizedPage = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int normalizedSize = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String normalizedSort = (sort == null || sort.isBlank()) ? null : sort.trim();
        return new PageQuery(normalizedPage, normalizedSize, normalizedSort);
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
