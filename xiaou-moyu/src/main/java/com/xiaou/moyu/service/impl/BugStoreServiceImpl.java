package com.xiaou.moyu.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.moyu.domain.BugItem;
import com.xiaou.moyu.domain.UserBugHistory;
import com.xiaou.moyu.dto.AdminBugItemRequest;
import com.xiaou.moyu.dto.BugItemDto;
import com.xiaou.moyu.dto.BugItemQueryRequest;
import com.xiaou.moyu.mapper.BugItemMapper;
import com.xiaou.moyu.mapper.UserBugHistoryMapper;
import com.xiaou.moyu.service.BugStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bug商店服务实现类
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BugStoreServiceImpl implements BugStoreService {
    
    private final BugItemMapper bugItemMapper;
    private final UserBugHistoryMapper userBugHistoryMapper;
    
    /**
     * 防重复时间窗口（小时）
     */
    private static final int DUPLICATE_WINDOW_HOURS = 2;
    
    /**
     * 最大排除条数
     */
    private static final int MAX_EXCLUDE_COUNT = 50;

    @Override
    public BugItemDto getRandomBug(Long userId) {
        try {
            // 获取用户最近浏览的Bug ID列表，避免重复
            List<Long> excludeIds = userBugHistoryMapper.selectRecentBugIds(
                userId, DUPLICATE_WINDOW_HOURS, MAX_EXCLUDE_COUNT);
            
            // 随机获取一个Bug
            BugItem bugItem = bugItemMapper.selectRandomBug(userId, excludeIds, 1);
            
            if (bugItem == null) {
                // 如果所有Bug都被排除了，清除排除列表重新获取
                if (excludeIds != null && !excludeIds.isEmpty()) {
                    bugItem = bugItemMapper.selectRandomBug(userId, null, 1);
                }
            }
            
            if (bugItem == null) {
                return null;
            }
            
            // 转换为DTO
            BugItemDto dto = convertToDto(bugItem);
            
            // 异步记录浏览历史
            recordUserView(userId, bugItem.getId());
            
            return dto;
        } catch (Exception e) {
            log.error("获取随机Bug失败, userId: {}", userId, e);
            return null;
        }
    }

    @Override
    public PageResult<BugItem> getBugList(BugItemQueryRequest query) {
        return PageHelper.doPage(query.getCurrent(), query.getSize(), () ->
            bugItemMapper.selectByPage(query)
        );
    }

    @Override
    public BugItem getBugById(Long id) {
        return bugItemMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addBug(AdminBugItemRequest request, Long createdBy) {
        try {
            BugItem bugItem = new BugItem();
            BeanUtil.copyProperties(request, bugItem);
            
            // 设置默认值
            bugItem.setStatus(request.getStatus() != null ? request.getStatus() : 1);
            bugItem.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
            bugItem.setCreatedBy(createdBy);
            bugItem.setCreateTime(LocalDateTime.now());
            bugItem.setUpdateTime(LocalDateTime.now());
            
            // 插入数据库
            int result = bugItemMapper.insert(bugItem);
            
            return result > 0 ? bugItem.getId() : null;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("添加Bug失败", e);
            throw new BusinessException("添加Bug失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBug(AdminBugItemRequest request) {
        try {
            if (request.getId() == null) {
                throw new IllegalArgumentException("Bug ID不能为空");
            }
            
            BugItem bugItem = new BugItem();
            BeanUtil.copyProperties(request, bugItem);
            bugItem.setUpdateTime(LocalDateTime.now());
            
            int result = bugItemMapper.updateById(bugItem);
            
            return result > 0;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新Bug失败, id: {}", request.getId(), e);
            throw new BusinessException("更新Bug失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBug(Long id) {
        try {
            int result = bugItemMapper.deleteById(id);
            return result > 0;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除Bug失败, id: {}", id, e);
            throw new BusinessException("删除Bug失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchImportBugs(List<AdminBugItemRequest> requests, Long createdBy) {
        try {
            if (requests == null || requests.isEmpty()) {
                throw new IllegalArgumentException("导入数据不能为空");
            }
            
            List<BugItem> bugItems = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            
            for (AdminBugItemRequest request : requests) {
                BugItem bugItem = new BugItem();
                BeanUtil.copyProperties(request, bugItem);
                
                // 设置默认值
                bugItem.setStatus(request.getStatus() != null ? request.getStatus() : 1);
                bugItem.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
                bugItem.setCreatedBy(createdBy);
                bugItem.setCreateTime(now);
                bugItem.setUpdateTime(now);
                
                bugItems.add(bugItem);
            }
            
            // 批量插入
            int result = bugItemMapper.insertBatch(bugItems);
            
            return result > 0;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量导入Bug失败", e);
            throw new BusinessException("批量导入Bug失败");
        }
    }

    @Override
    public void recordUserView(Long userId, Long bugId) {
        try {
            UserBugHistory history = new UserBugHistory();
            history.setUserId(userId);
            history.setBugId(bugId);
            history.setViewTime(LocalDateTime.now());
            history.setCreateTime(LocalDateTime.now());
            
            userBugHistoryMapper.insert(history);
        } catch (Exception e) {
            // 记录浏览历史失败不影响主流程
            log.warn("记录用户浏览历史失败, userId: {}, bugId: {}", userId, bugId, e);
        }
    }
    
    /**
     * 转换为DTO
     */
    private BugItemDto convertToDto(BugItem bugItem) {
        BugItemDto dto = new BugItemDto();
        BeanUtil.copyProperties(bugItem, dto);
        
        // 处理技术标签
        if (StrUtil.isNotBlank(bugItem.getTechTags())) {
            dto.setTechTags(bugItem.getTechTags().split(","));
        } else {
            dto.setTechTags(new String[0]);
        }
        
        // 设置难度名称
        dto.setDifficultyName(bugItem.getDifficultyName());
        
        return dto;
    }
}
