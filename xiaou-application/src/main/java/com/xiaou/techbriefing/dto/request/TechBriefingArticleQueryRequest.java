package com.xiaou.techbriefing.dto.request;

import lombok.Data;

/**
 * 科技热点列表查询请求
 */
@Data
public class TechBriefingArticleQueryRequest {

    private String scope = "all";

    private String category;

    private Long sourceId;

    private String keyword;

    private String sortBy = "latest";

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
