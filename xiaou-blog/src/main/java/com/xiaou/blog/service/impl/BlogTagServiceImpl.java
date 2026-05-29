package com.xiaou.blog.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.blog.domain.BlogArticle;
import com.xiaou.blog.domain.BlogTag;
import com.xiaou.blog.mapper.BlogArticleMapper;
import com.xiaou.blog.mapper.BlogTagMapper;
import com.xiaou.blog.service.BlogTagService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 博客标签Service实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogTagServiceImpl implements BlogTagService {
    
    private final BlogArticleMapper blogArticleMapper;
    private final BlogTagMapper blogTagMapper;
    
    @Override
    public List<BlogTag> getAllTags() {
        return blogTagMapper.selectAll();
    }
    
    @Override
    public PageResult<BlogTag> getTagList(Integer pageNum, Integer pageSize) {
        return PageHelper.doPage(pageNum, pageSize, () -> 
            blogTagMapper.selectAll()
        );
    }
    
    @Override
    public List<BlogTag> getHotTags(Integer limit) {
        return blogTagMapper.selectHotTags(limit);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeTags(Long sourceTagId, Long targetTagId) {
        if (sourceTagId.equals(targetTagId)) {
            throw new BusinessException("源标签和目标标签不能相同");
        }

        BlogTag sourceTag = blogTagMapper.selectById(sourceTagId);
        BlogTag targetTag = blogTagMapper.selectById(targetTagId);
        
        if (sourceTag == null || targetTag == null) {
            throw new BusinessException("标签不存在");
        }

        String sourceTagName = StrUtil.trim(sourceTag.getTagName());
        String targetTagName = StrUtil.trim(targetTag.getTagName());
        if (!StrUtil.isAllNotBlank(sourceTagName, targetTagName)) {
            throw new BusinessException("标签名称不能为空");
        }

        List<BlogArticle> affectedArticles = blogArticleMapper.selectByTagName(sourceTagName);
        int targetUseCountDelta = 0;
        for (BlogArticle article : affectedArticles) {
            List<String> tags = JSONUtil.toList(article.getTags(), String.class);
            if (tags == null || tags.isEmpty()) {
                continue;
            }

            int oldTargetOccurrences = 0;
            Set<String> mergedTags = new LinkedHashSet<>();
            for (String tag : tags) {
                String currentTag = StrUtil.trim(tag);
                if (!StrUtil.isNotBlank(currentTag)) {
                    continue;
                }

                if (targetTagName.equals(currentTag)) {
                    oldTargetOccurrences++;
                }
                if (sourceTagName.equals(currentTag)) {
                    currentTag = targetTagName;
                }
                mergedTags.add(currentTag);
            }

            int newTargetOccurrences = mergedTags.contains(targetTagName) ? 1 : 0;
            targetUseCountDelta += newTargetOccurrences - oldTargetOccurrences;

            BlogArticle updateArticle = new BlogArticle();
            updateArticle.setId(article.getId());
            updateArticle.setTags(JSONUtil.toJsonStr(new ArrayList<>(mergedTags)));
            blogArticleMapper.updateById(updateArticle);
        }

        int targetUseCount = targetTag.getUseCount() == null ? 0 : targetTag.getUseCount();
        BlogTag updateTag = new BlogTag();
        updateTag.setId(targetTagId);
        updateTag.setUseCount(targetUseCount + targetUseCountDelta);
        blogTagMapper.updateById(updateTag);
        
        // 删除源标签
        blogTagMapper.deleteById(sourceTagId);
        
        log.info("合并标签成功，源标签：{}，目标标签：{}", sourceTagId, targetTagId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        BlogTag tag = blogTagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        
        // 可以选择检查标签是否还在使用
        if (tag.getUseCount() != null && tag.getUseCount() > 0) {
            throw new BusinessException("该标签还在使用中，无法删除");
        }
        
        blogTagMapper.deleteById(id);
        
        log.info("删除标签成功：{}", id);
    }
}

