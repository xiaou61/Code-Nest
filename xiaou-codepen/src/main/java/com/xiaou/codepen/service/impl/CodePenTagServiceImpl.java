package com.xiaou.codepen.service.impl;

import com.xiaou.codepen.domain.CodePenTag;
import com.xiaou.codepen.mapper.CodePenTagMapper;
import com.xiaou.codepen.service.CodePenTagService;
import com.xiaou.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 作品标签Service实现
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodePenTagServiceImpl implements CodePenTagService {
    
    private final CodePenTagMapper tagMapper;
    
    @Override
    public List<CodePenTag> getAllTags() {
        return tagMapper.selectAll();
    }
    
    @Override
    public List<CodePenTag> getHotTags(Integer limit) {
        return tagMapper.selectHotTags(limit);
    }
    
    @Override
    public Long createTag(String tagName, String tagDescription) {
        String normalizedTagName = normalizeTagName(tagName);
        String normalizedDescription = normalizeTagDescription(tagDescription);

        // 检查标签是否已存在
        CodePenTag existTag = tagMapper.selectByName(normalizedTagName);
        if (existTag != null) {
            throw new BusinessException("标签已存在");
        }
        
        CodePenTag tag = new CodePenTag();
        tag.setTagName(normalizedTagName);
        tag.setTagDescription(normalizedDescription);
        tagMapper.insert(tag);
        
        return tag.getId();
    }
    
    @Override
    public boolean updateTag(Long id, String tagName, String tagDescription) {
        CodePenTag existingTag = tagMapper.selectById(id);
        if (existingTag == null) {
            throw new BusinessException("标签不存在");
        }

        String normalizedTagName = normalizeTagName(tagName);
        String normalizedDescription = normalizeTagDescription(tagDescription);

        if (!normalizedTagName.equals(existingTag.getTagName())) {
            CodePenTag duplicateTag = tagMapper.selectByName(normalizedTagName);
            if (duplicateTag != null && !duplicateTag.getId().equals(id)) {
                throw new BusinessException("标签已存在");
            }
        }

        CodePenTag tag = new CodePenTag();
        tag.setId(id);
        tag.setTagName(normalizedTagName);
        tag.setTagDescription(normalizedDescription);
        return tagMapper.updateById(tag) > 0;
    }
    
    @Override
    public boolean deleteTag(Long id) {
        return tagMapper.deleteById(id) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean mergeTags(Long sourceId, Long targetId) {
        if (sourceId == null || targetId == null) {
            throw new BusinessException("源标签ID和目标标签ID不能为空");
        }
        if (sourceId.equals(targetId)) {
            throw new BusinessException("源标签和目标标签不能相同");
        }
        
        // 检查两个标签是否存在
        com.xiaou.codepen.domain.CodePenTag sourceTag = tagMapper.selectById(sourceId);
        com.xiaou.codepen.domain.CodePenTag targetTag = tagMapper.selectById(targetId);
        
        if (sourceTag == null) {
            throw new BusinessException("源标签不存在");
        }
        if (targetTag == null) {
            throw new BusinessException("目标标签不存在");
        }
        
        // 合并标签（将源标签的使用次数加到目标标签上）
        tagMapper.mergeTags(sourceId, targetId);
        
        // 删除源标签
        tagMapper.deleteById(sourceId);
        
        log.info("标签合并成功，源标签ID：{}，目标标签ID：{}", sourceId, targetId);
        
        return true;
    }

    private String normalizeTagName(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new BusinessException("标签名称不能为空");
        }
        String normalized = tagName.trim();
        if (normalized.length() > 50) {
            throw new BusinessException("标签名称不能超过50个字符");
        }
        return normalized;
    }

    private String normalizeTagDescription(String tagDescription) {
        if (tagDescription == null) {
            return null;
        }
        String normalized = tagDescription.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 200) {
            throw new BusinessException("标签描述不能超过200个字符");
        }
        return normalized;
    }
}

