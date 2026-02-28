package com.xiaou.oj.dto;

import lombok.Data;

/**
 * OJ评论查询请求
 *
 * @author xiaou
 */
@Data
public class OjCommentQueryRequest {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页大小 */
    private Integer pageSize = 10;

    /** 排序方式：time/hot */
    private String sort = "time";
}
