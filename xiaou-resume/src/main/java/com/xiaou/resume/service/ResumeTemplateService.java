package com.xiaou.resume.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.resume.domain.ResumeTemplate;
import com.xiaou.resume.dto.ResumeTemplateRequest;
import com.xiaou.resume.dto.TemplateQueryRequest;

/**
 * 模板服务
 */
public interface ResumeTemplateService {

    PageResult<ResumeTemplate> listTemplates(TemplateQueryRequest request);

    Long createTemplate(ResumeTemplateRequest request);

    Long createTemplate(Long operatorId, ResumeTemplateRequest request);

    void updateTemplate(Long id, ResumeTemplateRequest request);

    void updateTemplate(Long operatorId, Long id, ResumeTemplateRequest request);

    void deleteTemplate(Long id);

    ResumeTemplate getTemplate(Long id);
}
