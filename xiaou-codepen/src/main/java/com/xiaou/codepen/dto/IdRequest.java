package com.xiaou.codepen.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 通用ID请求
 *
 * @author xiaou
 */
@Data
public class IdRequest {

    /**
     * 主键ID
     */
    @NotNull(message = "ID不能为空")
    @Positive(message = "ID必须为正数")
    private Long id;
}
