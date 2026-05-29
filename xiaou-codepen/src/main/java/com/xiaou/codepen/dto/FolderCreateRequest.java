package com.xiaou.codepen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建收藏夹请求
 *
 * @author xiaou
 */
@Data
public class FolderCreateRequest {

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
