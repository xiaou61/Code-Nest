package com.xiaou.resume.service.impl;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.resume.domain.ResumeTemplate;
import com.xiaou.resume.dto.ResumeTemplateRequest;
import com.xiaou.resume.dto.TemplateQueryRequest;
import com.xiaou.resume.mapper.ResumeTemplateMapper;
import com.xiaou.resume.service.ResumeTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模板服务实现
 */
@Service
@RequiredArgsConstructor
public class ResumeTemplateServiceImpl implements ResumeTemplateService {

    private final ResumeTemplateMapper resumeTemplateMapper;

    @Override
    public PageResult<ResumeTemplate> listTemplates(TemplateQueryRequest request) {
        TemplateQueryRequest query = request == null ? new TemplateQueryRequest() : request;
        return PageHelper.doPage(query.getPage(), query.getSize(),
                () -> resumeTemplateMapper.selectList(query));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(ResumeTemplateRequest request) {
        return createTemplate(StpUserUtil.getLoginIdAsLong(), request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(Long operatorId, ResumeTemplateRequest request) {
        ResumeTemplate template = mapToEntity(request);
        LocalDateTime now = LocalDateTime.now();
        template.setStatus(request.getStatus());
        template.setRating(0.0);
        template.setRatingCount(0);
        template.setDownloadCount(0);
        template.setCreatedBy(operatorId);
        template.setUpdatedBy(template.getCreatedBy());
        template.setCreateTime(now);
        template.setUpdateTime(now);
        resumeTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long id, ResumeTemplateRequest request) {
        updateTemplate(StpUserUtil.getLoginIdAsLong(), id, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long operatorId, Long id, ResumeTemplateRequest request) {
        ResumeTemplate exists = resumeTemplateMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("模板不存在");
        }
        ResumeTemplate template = mapToEntity(request);
        template.setId(id);
        template.setUpdateTime(LocalDateTime.now());
        template.setUpdatedBy(operatorId);
        resumeTemplateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        resumeTemplateMapper.deleteById(id);
    }

    @Override
    public ResumeTemplate getTemplate(Long id) {
        return resumeTemplateMapper.selectById(id);
    }

    private ResumeTemplate mapToEntity(ResumeTemplateRequest request) {
        return new ResumeTemplate()
                .setName(request.getName())
                .setCategory(request.getCategory())
                .setDescription(request.getDescription())
                .setCoverUrl(request.getCoverUrl())
                .setPreviewUrl(request.getPreviewUrl())
                .setTags(join(request.getTags()))
                .setTechStack(join(request.getTechStack()))
                .setExperienceLevel(request.getExperienceLevel())
                .setStatus(request.getStatus());
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }
}
