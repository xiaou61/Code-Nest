package com.xiaou.common.utils;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.xiaou.common.core.domain.PageResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.pagehelper.page.PageMethod.startPage;

/**
 * PageHelper 分页插件工具类
 * 基于 PageHelper 分页插件实现的统一分页工具
 *
 * @author xiaou
 */
@Slf4j
public class PageHelper {

    private PageHelper() {
        // 工具类，禁止实例化
    }

    /**
     * 执行分页查询
     *
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页大小
     * @param queryFunction 查询函数，该函数内应该包含实际的查询逻辑
     * @param <T> 返回数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> doPage(Integer pageNum, Integer pageSize, Supplier<List<T>> queryFunction) {
        try {
            // 参数验证和默认值设置
            pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
            pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
            pageSize = Math.min(pageSize, 100); // 限制最大页面大小

            // 开启分页
            startPage(pageNum, pageSize);

            // 执行查询
            List<T> list = queryFunction.get();

            // 获取分页信息
            PageInfo<T> pageInfo = new PageInfo<>(list);

            // 转换为统一的分页结果
            return PageResult.of(
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                pageInfo.getList()
            );

        } catch (Exception e) {
            log.error("分页查询失败", e);
            return PageResult.of(pageNum, pageSize, 0L, Collections.emptyList());
        } finally {
            // 清理分页参数，避免影响后续查询
            com.github.pagehelper.PageHelper.clearPage();
        }
    }

    /**
     * 执行分页查询（支持排序）
     *
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页大小
     * @param orderBy 排序字段，格式：field1 asc,field2 desc
     * @param queryFunction 查询函数
     * @param <T> 返回数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> doPage(Integer pageNum, Integer pageSize, String orderBy, Supplier<List<T>> queryFunction) {
        try {
            // 参数验证和默认值设置
            pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
            pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
            pageSize = Math.min(pageSize, 100); // 限制最大页面大小

            // 开启分页和排序
            if (orderBy != null && !orderBy.trim().isEmpty()) {
                startPage(pageNum, pageSize, orderBy);
            } else {
                startPage(pageNum, pageSize);
            }

            // 执行查询
            List<T> list = queryFunction.get();

            // 获取分页信息
            PageInfo<T> pageInfo = new PageInfo<>(list);

            // 转换为统一的分页结果
            return PageResult.of(
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                pageInfo.getList()
            );

        } catch (Exception e) {
            log.error("分页查询失败", e);
            return PageResult.of(pageNum, pageSize, 0L, Collections.emptyList());
        } finally {
            // 清理分页参数，避免影响后续查询
            com.github.pagehelper.PageHelper.clearPage();
        }
    }

    /**
     * 分页查询并在分页信息确定后再转换记录，避免转换后丢失 total。
     *
     * @param pageNum      页码（从1开始）
     * @param pageSize     每页大小
     * @param queryFunction 实际查询函数，应直接返回 MyBatis 分页结果
     * @param converter     分页记录转换器，转换阶段已清理分页上下文，可安全执行其他查询
     * @param <T>           查询结果元素类型
     * @param <R>           返回结果元素类型
     * @return 分页结果
     */
    public static <T, R> PageResult<R> doPageAndConvert(
            Integer pageNum,
            Integer pageSize,
            Supplier<List<T>> queryFunction,
            Function<List<T>, List<R>> converter
    ) {
        try {
            pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
            pageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
            pageSize = Math.min(pageSize, 100);

            startPage(pageNum, pageSize);
            List<T> rows = queryFunction.get();
            com.github.pagehelper.PageHelper.clearPage();
            PageInfo<T> pageInfo = new PageInfo<>(rows);
            List<R> records = converter.apply(pageInfo.getList());

            return PageResult.of(
                    pageInfo.getPageNum(),
                    pageInfo.getPageSize(),
                    pageInfo.getTotal(),
                    records
            );
        } catch (Exception e) {
            log.error("分页转换查询失败", e);
            return PageResult.of(pageNum, pageSize, 0L, Collections.emptyList());
        } finally {
            com.github.pagehelper.PageHelper.clearPage();
        }
    }

    /**
     * 直接将 PageHelper 查询结果转换为 PageResult
     * 适用于已经使用 PageHelper.startPage() 的场景
     *
     * @param list PageHelper 查询结果
     * @param <T> 数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> toPageResult(List<T> list) {
        if (list instanceof Page) {
            Page<T> page = (Page<T>) list;
            return PageResult.of(
                page.getPageNum(),
                page.getPageSize(),
                page.getTotal(),
                page.getResult()
            );
        }

        // 如果不是 Page 对象，说明没有使用分页
        PageInfo<T> pageInfo = new PageInfo<>(list);
        return PageResult.of(
            pageInfo.getPageNum(),
            pageInfo.getPageSize(),
            pageInfo.getTotal(),
            pageInfo.getList()
        );
    }
}
