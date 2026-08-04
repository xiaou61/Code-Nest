package com.xiaou.web.growthcoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户确认附加公开代码来源的请求。
 */
@Data
public class GrowthCodeArtifactAttachRequest {

    @NotBlank(message = "公开 GitHub 链接不能为空")
    @Size(max = 512, message = "公开 GitHub 链接不能超过512个字符")
    private String url;
}
