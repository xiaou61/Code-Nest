package com.xiaou.mockinterview.dto.response.admin;

import com.xiaou.mockinterview.domain.MockInterviewQA;
import com.xiaou.mockinterview.domain.MockInterviewSession;
import lombok.Data;

import java.util.List;

/**
 * 管理端面试会话详情
 *
 * @author xiaou
 */
@Data
public class AdminInterviewSessionDetailResponse {

    /**
     * 会话信息
     */
    private MockInterviewSession session;

    /**
     * 主问题列表（包含追问）
     */
    private List<MockInterviewQA> qaList;

    /**
     * 已回答题数
     */
    private Integer answeredCount;

    /**
     * 已跳过题数
     */
    private Integer skippedCount;

    /**
     * 待回答题数
     */
    private Integer pendingCount;

    /**
     * 追问数
     */
    private Integer followUpCount;
}

