package com.xiaou.codepen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新收藏夹请求
 *
 * @author xiaou
 */
@Data
public class FolderUpdateRequest {

    /**
     * 收藏夹ID
     */
    @NotNull(message = "收藏夹ID不能为空")
    @Positive(message = "收藏夹ID必须为正数")
    private Long id;

    /**
     * 收藏夹名称
     */
    @NotBlank(message = "收藏夹名称不能为空")
    @Size(max = 100, message = "收藏夹名称不能超过100个字符")
    private String folderName;

    /**
     * 收藏夹描述
     */
    @Size(max = 500, message = "收藏夹描述不能超过500个字符")
    private String folderDescription;
}
