package com.xiaou.notification.dto;

import com.xiaou.notification.domain.NotificationTemplate;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationTemplateRequest {

    @NotBlank(message = "模板编码不能为空")
    private String code;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    @NotBlank(message = "标题模板不能为空")
    private String titleTemplate;

    @NotBlank(message = "内容模板不能为空")
    private String contentTemplate;

    private Boolean isEnabled = true;

    public NotificationTemplate toDomain(Long id) {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(id);
        template.setCode(code);
        template.setName(name);
        template.setTitleTemplate(titleTemplate);
        template.setContentTemplate(contentTemplate);
        template.setIsEnabled(isEnabled);
        return template;
    }
}
