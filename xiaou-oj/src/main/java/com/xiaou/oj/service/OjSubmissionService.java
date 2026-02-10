package com.xiaou.oj.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.oj.domain.OjSubmission;
import com.xiaou.oj.dto.OjStatisticsVO;
import com.xiaou.oj.dto.SubmissionQueryRequest;
import com.xiaou.oj.dto.SubmitCodeRequest;

/**
 * OJ提交记录服务接口
 *
 * @author xiaou
 */
public interface OjSubmissionService {

    /**
     * 提交代码
     */
    Long submitCode(Long userId, SubmitCodeRequest request);

    /**
     * 获取提交详情
     */
    OjSubmission getSubmissionById(Long id);

    /**
     * 分页查询提交记录
     */
    PageResult<OjSubmission> getSubmissions(SubmissionQueryRequest request);

    /**
     * 获取个人做题统计
     */
    OjStatisticsVO getStatistics(Long userId);
}
