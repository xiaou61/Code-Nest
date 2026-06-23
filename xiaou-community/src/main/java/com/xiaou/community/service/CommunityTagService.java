package com.xiaou.community.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.community.domain.CommunityTag;
import com.xiaou.community.dto.CommunityTagCreateRequest;
import com.xiaou.community.dto.CommunityTagQueryRequest;
import com.xiaou.community.dto.CommunityTagUpdateRequest;

import java.util.List;

/**
 * 社区标签Service
 * 
 * @author xiaou
 */
public interface CommunityTagService {
    
    /**
     * 创建标签
     */
    void createTag(CommunityTagCreateRequest request);
    
    /**
     * 更新标签
     */
    void updateTag(CommunityTagUpdateRequest request);
    
    /**
     * 删除标签
     */
    void deleteTag(Long id);

    /**
     * 启用/禁用标签
     */
    void toggleTagStatus(Long id);
    
    /**
     * 根据ID查询标签
     */
    CommunityTag getById(Long id);
    
    /**
     * 查询启用的标签列表（前台）
     */
    List<CommunityTag> getEnabledList();
    
    /**
     * 查询热门标签列表（前台）
     */
    List<CommunityTag> getHotList(Integer limit);
    
    /**
     * 后台分页查询标签列表
     */
    PageResult<CommunityTag> getAdminList(CommunityTagQueryRequest request);
    
    /**
     * 更新标签帖子数量
     */
    void updatePostCount(Long tagId, Integer increment);
    
    /**
     * 批量更新标签帖子数量
     */
    void batchUpdatePostCount(List<Long> tagIds, Integer increment);
}

