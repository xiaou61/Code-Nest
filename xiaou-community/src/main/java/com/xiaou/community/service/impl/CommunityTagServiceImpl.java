package com.xiaou.community.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.community.domain.CommunityTag;
import com.xiaou.community.dto.CommunityTagCreateRequest;
import com.xiaou.community.dto.CommunityTagQueryRequest;
import com.xiaou.community.dto.CommunityTagUpdateRequest;
import com.xiaou.community.mapper.CommunityPostTagMapper;
import com.xiaou.community.mapper.CommunityTagMapper;
import com.xiaou.community.service.CommunityCacheService;
import com.xiaou.community.service.CommunityTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 社区标签Service实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityTagServiceImpl implements CommunityTagService {
    
    private final CommunityTagMapper communityTagMapper;
    private final CommunityPostTagMapper communityPostTagMapper;
    private final CommunityCacheService communityCacheService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTag(CommunityTagCreateRequest request) {
        // 检查标签名称是否已存在
        CommunityTag existingTag = communityTagMapper.selectByName(request.getName());
        if (existingTag != null) {
            throw new BusinessException("标签名称已存在");
        }
        
        CommunityTag tag = new CommunityTag();
        BeanUtil.copyProperties(request, tag);
        tag.setPostCount(0);
        tag.setFollowCount(0);
        tag.setStatus(1); // 默认启用
        
        // 如果没有设置颜色，使用默认颜色
        if (tag.getColor() == null) {
            tag.setColor("#409EFF");
        }
        
        // 如果没有设置排序，使用0
        if (tag.getSortOrder() == null) {
            tag.setSortOrder(0);
        }
        
        int result = communityTagMapper.insert(tag);
        if (result <= 0) {
            throw new BusinessException("创建标签失败");
        }
        
        // 清除标签缓存
        communityCacheService.evictTags();
        
        log.info("创建标签成功，标签ID: {}, 名称: {}", tag.getId(), tag.getName());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTag(CommunityTagUpdateRequest request) {
        // 检查标签是否存在
        CommunityTag existingTag = getById(request.getId());
        
        // 如果修改了名称，检查新名称是否已被占用
        if (request.getName() != null && !request.getName().equals(existingTag.getName())) {
            CommunityTag tagWithSameName = communityTagMapper.selectByName(request.getName());
            if (tagWithSameName != null) {
                throw new BusinessException("标签名称已存在");
            }
        }
        
        CommunityTag tag = new CommunityTag();
        BeanUtil.copyProperties(request, tag);
        
        int result = communityTagMapper.update(tag);
        if (result <= 0) {
            throw new BusinessException("更新标签失败");
        }
        
        // 清除标签缓存
        communityCacheService.evictTags();
        
        log.info("更新标签成功，标签ID: {}", tag.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        // 检查标签是否存在
        getById(id);
        
        // 检查是否有帖子在使用该标签
        int postCount = communityPostTagMapper.countPostsByTagId(id);
        if (postCount > 0) {
            throw new BusinessException("该标签下还有帖子，无法删除");
        }
        
        int result = communityTagMapper.deleteById(id);
        if (result <= 0) {
            throw new BusinessException("删除标签失败");
        }
        
        log.info("删除标签成功，标签ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleTagStatus(Long id) {
        CommunityTag tag = getById(id);

        CommunityTag updateTag = new CommunityTag();
        updateTag.setId(id);
        updateTag.setStatus(tag.getStatus() == 1 ? 0 : 1);

        int result = communityTagMapper.update(updateTag);
        if (result <= 0) {
            throw new BusinessException("更新标签状态失败");
        }

        communityCacheService.evictTags();

        log.info("更新标签状态成功，标签ID: {}, 新状态: {}", id, updateTag.getStatus());
    }
    
    @Override
    public CommunityTag getById(Long id) {
        CommunityTag tag = communityTagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        return tag;
    }
    
    @Override
    public List<CommunityTag> getEnabledList() {
        // 先尝试从缓存获取
        List<CommunityTag> cachedTags = communityCacheService.getCachedTags();
        if (cachedTags != null) {
            log.debug("从缓存获取标签列表");
            return cachedTags;
        }
        
        // 缓存未命中，从数据库查询
        List<CommunityTag> tags = communityTagMapper.selectEnabledList();
        
        // 写入缓存
        if (tags != null && !tags.isEmpty()) {
            communityCacheService.cacheTags(tags);
        }
        
        return tags;
    }
    
    @Override
    public List<CommunityTag> getHotList(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        return communityTagMapper.selectHotList(limit);
    }
    
    @Override
    public PageResult<CommunityTag> getAdminList(CommunityTagQueryRequest request) {
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> 
            communityTagMapper.selectAdminList(request)
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePostCount(Long tagId, Integer increment) {
        communityTagMapper.updatePostCount(tagId, increment);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdatePostCount(List<Long> tagIds, Integer increment) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            communityTagMapper.updatePostCount(tagId, increment);
        }
    }
}

