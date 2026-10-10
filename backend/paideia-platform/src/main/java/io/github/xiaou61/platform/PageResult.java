package io.github.xiaou61.platform;

import java.util.List;

/**
 * 分页结果。{@code items} 以不可变列表保存，避免调用方拿到之后被下游改写。
 */
public record PageResult<T>(long total, int page, int size, List<T> items) {

    public PageResult {
        // 空结果是最常见的返回路径之一，不能因为 items 为 null 就把整条查询打成 500
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> PageResult<T> of(long total, PageQuery query, List<T> items) {
        return new PageResult<>(total, query.page(), query.size(), items);
    }
}
