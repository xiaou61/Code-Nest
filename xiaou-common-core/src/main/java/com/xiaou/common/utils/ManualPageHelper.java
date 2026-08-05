package com.xiaou.common.utils;

import com.xiaou.common.core.domain.PageRequest;
import com.xiaou.common.core.domain.PageResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 手动分页工具类（备份实现）
 * 封装分页查询的通用逻辑，消除重复代码
 *
 * @author xiaou
 * @deprecated 建议使用新的 PageHelper 分页插件实现
 */
@Slf4j
@Deprecated
public class ManualPageHelper {

    private ManualPageHelper() {
        // 工具类，禁止实例化
    }

    /**
     * 执行分页查询
     *
     * @param pageRequest 分页请求参数
     * @param countFunction 总数查询函数
     * @param selectFunction 分页数据查询函数（参数：查询条件，offset，pageSize）
     * @param queryParam 查询条件参数
     * @param <T> 查询条件类型
     * @param <R> 返回数据类型
     * @return 分页结果
     */
    public static <T, R> PageResult<R> doPage(
            PageRequest pageRequest,
            Function<T, Long> countFunction,
            TriFunction<T, Integer, Integer, List<R>> selectFunction,
            T queryParam) {

        try {
            // 参数验证和默认值设置
            validateAndSetDefaults(pageRequest);

            // 查询总数
            Long total = countFunction.apply(queryParam);
            if (total == null || total <= 0) {
                return PageResult.of(pageRequest.getPageNum(), pageRequest.getPageSize(), 0L, Collections.emptyList());
            }

            // 计算分页参数
            int offset = (pageRequest.getPageNum() - 1) * pageRequest.getPageSize();

            // 查询分页数据
            List<R> records = selectFunction.apply(queryParam, offset, pageRequest.getPageSize());
            if (records == null) {
                records = Collections.emptyList();
            }

            return PageResult.of(pageRequest.getPageNum(), pageRequest.getPageSize(), total, records);

        } catch (Exception e) {
            log.error("分页查询失败", e);
            return PageResult.of(
                pageRequest.getPageNum() != null ? pageRequest.getPageNum() : 1,
                pageRequest.getPageSize() != null ? pageRequest.getPageSize() : 10,
                0L,
                Collections.emptyList()
            );
        }
    }

    /**
     * 执行分页查询（简化版本，当查询条件和查询参数是同一个对象时使用）
     *
     * @param pageRequest 分页请求参数（同时也是查询条件）
     * @param countFunction 总数查询函数
     * @param selectFunction 分页数据查询函数
     * @param <T> 查询条件类型（实现了PageRequest接口）
     * @param <R> 返回数据类型
     * @return 分页结果
     */
    public static <T extends PageRequest, R> PageResult<R> doPage(
            T pageRequest,
            Function<T, Long> countFunction,
            TriFunction<T, Integer, Integer, List<R>> selectFunction) {

        return doPage(pageRequest, countFunction, selectFunction, pageRequest);
    }

    /**
     * 执行分页查询（使用BiFunction的简化版本）
     *
     * @param pageRequest 分页请求参数
     * @param countFunction 总数查询函数
     * @param selectFunction 分页数据查询函数（参数：查询条件，分页信息）
     * @param queryParam 查询条件参数
     * @param <T> 查询条件类型
     * @param <R> 返回数据类型
     * @return 分页结果
     */
    public static <T, R> PageResult<R> doPageWithBiFunction(
            PageRequest pageRequest,
            Function<T, Long> countFunction,
            BiFunction<T, PageInfo, List<R>> selectFunction,
            T queryParam) {

        return doPage(pageRequest, countFunction,
            (param, offset, pageSize) -> selectFunction.apply(param, new PageInfo(offset, pageSize)),
            queryParam);
    }

    /**
     * 参数验证和默认值设置
     */
    private static void validateAndSetDefaults(PageRequest pageRequest) {
        if (pageRequest.getPageNum() == null || pageRequest.getPageNum() < 1) {
            pageRequest.setPageNum(pageRequest.getDefaultPageNum());
        }

        if (pageRequest.getPageSize() == null || pageRequest.getPageSize() < 1) {
            pageRequest.setPageSize(pageRequest.getDefaultPageSize());
        }

        // 限制最大页面大小
        if (pageRequest.getPageSize() > pageRequest.getMaxPageSize()) {
            pageRequest.setPageSize(pageRequest.getMaxPageSize());
        }
    }

    /**
     * 三参数函数式接口
     */
    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    /**
     * 分页信息类
     */
    public static class PageInfo {
        private final int offset;
        private final int pageSize;

        public PageInfo(int offset, int pageSize) {
            this.offset = offset;
            this.pageSize = pageSize;
        }

        public int getOffset() {
            return offset;
        }

        public int getPageSize() {
            return pageSize;
        }
    }
}
