package com.xiaou.mockinterview.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建面试请求DTO
 *
 * @author xiaou
 */
@Data
public class CreateInterviewRequest {

    /**
     * 面试方向（java/frontend/python等）
     */
    private String direction;

    /**
     * 难度级别：1-初级 2-中级 3-高级
     */
    private Integer level;

    /**
     * 面试类型：1-技术 2-综合 3-专项
     */
    private Integer interviewType;

    /**
     * AI风格：1-温和 2-标准 3-压力
     */
    private Integer style;

    /**
     * 题目数量
     */
    private Integer questionCount;

    /**
     * 专项知识点（专项面试时使用）
     */
    @Size(max = 120, message = "专项关注点不能超过120个字符")
    private String specializedTopic;

    /**
     * 出题模式：1-本地题库 2-AI出题
     */
    private Integer questionMode;

    /**
     * 选择的题库ID列表（本地出题模式时使用）
     */
    private List<Long> questionSetIds;
}
