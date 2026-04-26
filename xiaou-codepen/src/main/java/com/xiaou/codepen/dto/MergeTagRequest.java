package com.xiaou.codepen.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 合并标签请求
 * 
 * @author xiaou
 */
@Data
public class MergeTagRequest {
    
    /**
     * 源标签ID（将被合并）
     */
    @NotNull(message = "源标签ID不能为空")
    @Positive(message = "源标签ID必须为正数")
    private Long sourceId;
    
    /**
     * 目标标签ID（保留）
     */
    @NotNull(message = "目标标签ID不能为空")
    @Positive(message = "目标标签ID必须为正数")
    private Long targetId;
}

