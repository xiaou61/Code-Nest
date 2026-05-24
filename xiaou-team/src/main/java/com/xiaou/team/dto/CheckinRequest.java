package com.xiaou.team.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 打卡请求DTO
 * 
 * @author xiaou
 */
@Data
public class CheckinRequest {
    
    /**
     * 任务ID
     */
    @NotNull(message = "任务ID不能为空")
    @Positive(message = "任务ID必须大于0")
    private Long taskId;
    
    /**
     * 完成数量
     */
    @Min(value = 1, message = "完成数量必须大于0")
    private Integer completeValue;
    
    /**
     * 打卡内容
     */
    @Size(max = 1000, message = "打卡内容不能超过1000个字符")
    private String content;
    
    /**
     * 图片列表（最多9张）
     */
    private List<String> images;
    
    /**
     * 学习时长（分钟）
     */
    @Min(value = 0, message = "学习时长不能为负数")
    private Integer duration;
    
    /**
     * 相关代码/笔记链接
     */
    @Size(max = 255, message = "相关链接不能超过255个字符")
    private String relatedLink;
}
