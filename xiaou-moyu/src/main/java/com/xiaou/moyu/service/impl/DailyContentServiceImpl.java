package com.xiaou.moyu.service.impl;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.moyu.domain.DailyContent;
import com.xiaou.moyu.domain.UserCalendarCollection;
import com.xiaou.moyu.domain.UserCalendarPreference;
import com.xiaou.moyu.mapper.DailyContentMapper;
import com.xiaou.moyu.mapper.UserCalendarCollectionMapper;
import com.xiaou.moyu.mapper.UserCalendarPreferenceMapper;
import com.xiaou.moyu.service.DailyContentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 每日内容服务实现类
 *
 * @author xiaou
 */
@Slf4j
@Service
public class DailyContentServiceImpl implements DailyContentService {

    @Resource
    private DailyContentMapper dailyContentMapper;

    @Resource
    private UserCalendarPreferenceMapper userCalendarPreferenceMapper;

    @Resource
    private UserCalendarCollectionMapper userCalendarCollectionMapper;

    @Override
    public List<DailyContent> getContentByType(Integer contentType, Integer limit) {
        List<DailyContent> contents = dailyContentMapper.selectByContentType(contentType, 1, limit);
        return contents != null ? contents : new ArrayList<>();
    }

    @Override
    public List<DailyContent> getRandomContentByType(Integer contentType, Integer limit) {
        List<DailyContent> contents = dailyContentMapper.selectRandomByContentType(contentType, 1, limit);
        return contents != null ? contents : new ArrayList<>();
    }

    @Override
    public List<DailyContent> getRecommendedContent(Long userId, Integer limit) {
        UserCalendarPreference preference = userCalendarPreferenceMapper.selectByUserId(userId);
        
        if (preference != null) {
            // 根据用户偏好推荐内容
            List<DailyContent> contents = dailyContentMapper.selectByUserPreference(
                JsonUtils.jsonToIntegerList(preference.getPreferredContentTypes()),
                JsonUtils.jsonToStringList(preference.getPreferredLanguages()),
                preference.getDifficultyPreference(),
                1,
                limit
            );
            return contents != null ? contents : new ArrayList<>();
        } else {
            // 用户没有偏好设置，返回随机内容
            return getRandomContentByType(null, limit);
        }
    }

    @Override
    public List<DailyContent> getTodayContent(Long userId) {
        List<DailyContent> todayContent = new ArrayList<>();
        
        try {
            // 获取用户偏好
            UserCalendarPreference preference = userCalendarPreferenceMapper.selectByUserId(userId);
            
            if (preference != null && preference.getDailyContentPush() == 1) {
                // 用户开启了每日内容推送，根据偏好推荐
                List<Integer> contentTypes = JsonUtils.jsonToIntegerList(preference.getPreferredContentTypes());
                List<String> languages = JsonUtils.jsonToStringList(preference.getPreferredLanguages());
                Integer difficultyLevel = preference.getDifficultyPreference();
                
                if (!CollectionUtils.isEmpty(contentTypes)) {
                    // 为每种内容类型随机获取一条内容
                    for (Integer type : contentTypes) {
                        List<DailyContent> typeContent;
                        if (!CollectionUtils.isEmpty(languages)) {
                            // 如果用户指定了编程语言偏好，优先获取相关语言的内容
                            typeContent = dailyContentMapper.selectByUserPreference(
                                List.of(type), languages, difficultyLevel, 1, 1
                            );
                        } else {
                            typeContent = dailyContentMapper.selectRandomByContentType(type, 1, 1);
                        }
                        
                        if (!CollectionUtils.isEmpty(typeContent)) {
                            todayContent.addAll(typeContent);
                        }
                    }
                } else {
                    // 用户没有指定内容类型偏好，每种类型随机获取一条
                    for (int i = 1; i <= 4; i++) { // 1-编程格言，2-技术小贴士，3-代码片段，4-历史上的今天
                        List<DailyContent> typeContent = dailyContentMapper.selectRandomByContentType(i, 1, 1);
                        if (!CollectionUtils.isEmpty(typeContent)) {
                            todayContent.addAll(typeContent);
                        }
                    }
                }
            } else {
                // 用户关闭了推送或没有偏好设置，返回默认推荐
                for (int i = 1; i <= 4; i++) {
                    List<DailyContent> typeContent = dailyContentMapper.selectRandomByContentType(i, 1, 1);
                    if (!CollectionUtils.isEmpty(typeContent)) {
                        todayContent.addAll(typeContent);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取今日内容失败", e);
        }
        
        return todayContent;
    }

    @Override
    public List<DailyContent> getPopularContent(Integer limit) {
        List<DailyContent> contents = dailyContentMapper.selectPopularContent(1, limit);
        return contents != null ? contents : new ArrayList<>();
    }

    @Override
    public List<DailyContent> getContentByLanguage(String programmingLanguage, Integer limit) {
        List<DailyContent> contents = dailyContentMapper.selectByProgrammingLanguage(programmingLanguage, 1, limit);
        return contents != null ? contents : new ArrayList<>();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementViewCount(Long contentId) {
        try {
            return dailyContentMapper.incrementViewCount(contentId) > 0;
        } catch (Exception e) {
            log.error("增加查看次数失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likeContent(Long contentId) {
        try {
            return dailyContentMapper.incrementLikeCount(contentId) > 0;
        } catch (Exception e) {
            log.error("点赞内容失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleContentCollection(Long userId, Long contentId) {
        try {
            UserCalendarCollection existing = userCalendarCollectionMapper.selectByUserIdAndTarget(userId, 2, contentId);
            
            if (existing != null) {
                // 切换收藏状态
                int newStatus = existing.getStatus() == 1 ? 0 : 1;
                return userCalendarCollectionMapper.updateStatus(userId, 2, contentId, newStatus) > 0;
            } else {
                // 新增收藏记录
                UserCalendarCollection collection = new UserCalendarCollection();
                collection.setUserId(userId);
                collection.setCollectionType(2); // 2-内容
                collection.setTargetId(contentId);
                collection.setCollectionTime(LocalDateTime.now());
                collection.setStatus(1);
                collection.setCreateTime(LocalDateTime.now());
                collection.setUpdateTime(LocalDateTime.now());
                return userCalendarCollectionMapper.insert(collection) > 0;
            }
        } catch (Exception e) {
            log.error("切换内容收藏状态失败", e);
            return false;
        }
    }

    @Override
    public List<DailyContent> getUserCollectedContent(Long userId) {
        List<UserCalendarCollection> collections = userCalendarCollectionMapper.selectByUserIdAndType(userId, 2, 1);
        if (CollectionUtils.isEmpty(collections)) {
            return new ArrayList<>();
        }

        // 批量查询所有收藏的内容，避免N+1问题
        List<Long> contentIds = collections.stream()
                .map(UserCalendarCollection::getTargetId)
                .collect(java.util.stream.Collectors.toList());
        
        List<DailyContent> allContents = dailyContentMapper.selectBatchIds(contentIds);
        
        // 过滤出状态为启用的内容
        return allContents.stream()
                .filter(content -> content != null && content.getStatus() == 1)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public boolean isContentCollected(Long userId, Long contentId) {
        UserCalendarCollection collection = userCalendarCollectionMapper.selectByUserIdAndTarget(userId, 2, contentId);
        return collection != null && collection.getStatus() == 1;
    }

    // ========== 后台管理方法实现 ==========

    @Override
    public List<DailyContent> getAllContent(Integer contentType, Integer limit) {
        if (contentType != null) {
            List<DailyContent> contents = dailyContentMapper.selectByContentType(contentType, 1, limit);
            return contents != null ? contents : new ArrayList<>();
        } else {
            List<DailyContent> allContents = new ArrayList<>();
            for (int i = 1; i <= 4; i++) { // 1-4种内容类型
                List<DailyContent> typeContents = dailyContentMapper.selectByContentType(i, 1, limit);
                if (!CollectionUtils.isEmpty(typeContents)) {
                    allContents.addAll(typeContents);
                }
            }
            return allContents;
        }
    }

    @Override
    public List<DailyContent> getAdminContentByType(Integer contentType, Integer limit) {
        // 后台管理获取所有状态的内容，包括已禁用的
        List<DailyContent> contents = dailyContentMapper.selectByContentType(contentType, null, limit);
        return contents != null ? contents : new ArrayList<>();
    }

    @Override
    public DailyContent getContentById(Long id) {
        return dailyContentMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createContent(DailyContent content) {
        try {
            content.setStatus(1); // 默认启用
            content.setViewCount(0);
            content.setLikeCount(0);
            content.setCreateTime(LocalDateTime.now());
            content.setUpdateTime(LocalDateTime.now());
            return dailyContentMapper.insert(content) > 0;
        } catch (Exception e) {
            log.error("创建内容失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContent(DailyContent content) {
        try {
            content.setUpdateTime(LocalDateTime.now());
            return dailyContentMapper.updateById(content) > 0;
        } catch (Exception e) {
            log.error("更新内容失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteContent(Long id) {
        try {
            return dailyContentMapper.deleteById(id) > 0;
        } catch (Exception e) {
            log.error("删除内容失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteContent(List<Long> ids) {
        try {
            if (CollectionUtils.isEmpty(ids)) {
                return true;
            }
            // 使用批量删除，避免N+1问题
            dailyContentMapper.deleteBatchIds(ids);
            return true;
        } catch (Exception e) {
            log.error("批量删除内容失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContentStatus(Long id, Integer status) {
        try {
            return dailyContentMapper.updateStatusById(id, status) > 0;
        } catch (Exception e) {
            log.error("更新内容状态失败", e);
            return false;
        }
    }

    @Override
    public java.util.Map<String, Object> getContentStatistics() {
        try {
            java.util.Map<String, Object> statistics = new java.util.HashMap<>();
            
            // 统计各种类型的内容数量
            Long totalContents = dailyContentMapper.countByStatus(1);
            Long quotes = 0L;        // 编程格言
            Long tips = 0L;          // 技术小贴士
            Long codeSnippets = 0L;  // 代码片段
            Long histories = 0L;     // 历史上的今天
            
            for (int i = 1; i <= 4; i++) {
                List<DailyContent> typeContents = dailyContentMapper.selectByContentType(i, 1, null);
                switch (i) {
                    case 1: quotes = (long) typeContents.size(); break;
                    case 2: tips = (long) typeContents.size(); break;
                    case 3: codeSnippets = (long) typeContents.size(); break;
                    case 4: histories = (long) typeContents.size(); break;
                }
            }
            
            // 统计总查看次数和点赞次数
            List<DailyContent> popularContents = dailyContentMapper.selectPopularContent(1, 100);
            Long totalViews = popularContents.stream()
                    .map(content -> content.getViewCount() != null ? content.getViewCount().longValue() : 0L)
                    .reduce(0L, Long::sum);
            Long totalLikes = popularContents.stream()
                    .map(content -> content.getLikeCount() != null ? content.getLikeCount().longValue() : 0L)
                    .reduce(0L, Long::sum);
            
            statistics.put("totalContents", totalContents);
            statistics.put("quotes", quotes);
            statistics.put("tips", tips);
            statistics.put("codeSnippets", codeSnippets);
            statistics.put("histories", histories);
            statistics.put("totalViews", totalViews);
            statistics.put("totalLikes", totalLikes);
            
            return statistics;
        } catch (Exception e) {
            log.error("获取内容统计失败", e);
            return new java.util.HashMap<>();
        }
    }

    @Override
    public java.util.Map<String, Object> getCollectionStatistics() {
        try {
            java.util.Map<String, Object> statistics = new java.util.HashMap<>();
            
            // 这里需要添加更多的收藏统计逻辑
            // 由于没有专门的统计查询方法，先返回基础统计
            statistics.put("totalCollections", 0L);
            statistics.put("eventCollections", 0L);
            statistics.put("contentCollections", 0L);
            
            return statistics;
        } catch (Exception e) {
            log.error("获取收藏统计失败", e);
            return new java.util.HashMap<>();
        }
    }
}
