package com.xiaou.common.core.domain;

import lombok.Data;

import java.util.List;

/**
 * 分页结果DTO
 *
 * @author xiaou
 */
@Data
public class PageResult<T> {

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 构造分页结果
     */
    public static <T> PageResult<T> of(Integer pageNum, Integer pageSize, Long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setRecords(records);

        // 计算总页数
        result.setTotalPages((int) Math.ceil((double) total / pageSize));

        // 计算是否有上一页/下一页
        result.setHasPrevious(pageNum > 1);
        result.setHasNext(pageNum < result.getTotalPages());

        return result;
    }
}
