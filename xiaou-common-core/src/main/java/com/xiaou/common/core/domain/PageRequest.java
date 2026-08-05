package com.xiaou.common.core.domain;

/**
 * 分页请求基础接口
 * 支持链式调用的分页参数设置
 *
 * @author xiaou
 */
public interface PageRequest {

    /**
     * 获取页码（从1开始）
     */
    Integer getPageNum();

    /**
     * 设置页码
     * @param pageNum 页码
     * @return 支持链式调用
     */
    PageRequest setPageNum(Integer pageNum);

    /**
     * 获取每页数量
     */
    Integer getPageSize();

    /**
     * 设置每页数量
     * @param pageSize 每页数量
     * @return 支持链式调用
     */
    PageRequest setPageSize(Integer pageSize);

    /**
     * 获取默认页码
     */
    default Integer getDefaultPageNum() {
        return 1;
    }

    /**
     * 获取默认每页数量
     */
    default Integer getDefaultPageSize() {
        return 10;
    }

    /**
     * 获取最大每页数量
     */
    default Integer getMaxPageSize() {
        return 100;
    }
}
