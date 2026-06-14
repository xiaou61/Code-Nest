package com.xiaou.moyu.service.impl;

import com.xiaou.moyu.domain.DeveloperCalendarEvent;
import com.xiaou.moyu.domain.UserCalendarCollection;
import com.xiaou.moyu.domain.UserCalendarPreference;
import com.xiaou.moyu.mapper.DeveloperCalendarEventMapper;
import com.xiaou.moyu.mapper.UserCalendarCollectionMapper;
import com.xiaou.moyu.mapper.UserCalendarPreferenceMapper;
import com.xiaou.moyu.service.DeveloperCalendarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 程序员日历服务实现类
 *
 * @author xiaou
 */
@Slf4j
@Service
public class DeveloperCalendarServiceImpl implements DeveloperCalendarService {

    @Resource
    private DeveloperCalendarEventMapper developerCalendarEventMapper;

    @Resource
    private UserCalendarPreferenceMapper userCalendarPreferenceMapper;

    @Resource
    private UserCalendarCollectionMapper userCalendarCollectionMapper;

    @Override
    public List<DeveloperCalendarEvent> getEventsByDate(String eventDate) {
        List<DeveloperCalendarEvent> events = developerCalendarEventMapper.selectByEventDate(eventDate);
        return events != null ? events : new ArrayList<>();
    }

    @Override
    public List<DeveloperCalendarEvent> getEventsByMonth(String month) {
        List<DeveloperCalendarEvent> events = developerCalendarEventMapper.selectByMonth(month);
        return events != null ? events : new ArrayList<>();
    }

    @Override
    public List<DeveloperCalendarEvent> getMajorEvents() {
        List<DeveloperCalendarEvent> events = developerCalendarEventMapper.selectMajorEvents();
        return events != null ? events : new ArrayList<>();
    }

    @Override
    public List<DeveloperCalendarEvent> getEventsByType(Integer eventType) {
        List<DeveloperCalendarEvent> events = developerCalendarEventMapper.selectByEventType(eventType, 1);
        return events != null ? events : new ArrayList<>();
    }

    @Override
    public List<DeveloperCalendarEvent> getTodayEvents() {
        // 获取今天的日期，格式为MM-dd
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        return getEventsByDate(today);
    }

    @Override
    public UserCalendarPreference getUserPreference(Long userId) {
        UserCalendarPreference preference = userCalendarPreferenceMapper.selectByUserId(userId);
        if (preference == null) {
            // 返回默认偏好设置
            preference = createDefaultPreference(userId);
        }
        return preference;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateUserPreference(Long userId, UserCalendarPreference preference) {
        try {
            preference.setUserId(userId);
            UserCalendarPreference existing = userCalendarPreferenceMapper.selectByUserId(userId);
            
            if (existing != null) {
                // 更新现有配置
                preference.setId(existing.getId());
                return userCalendarPreferenceMapper.updateByUserId(preference) > 0;
            } else {
                // 插入新配置
                preference.setStatus(1);
                preference.setCreateTime(LocalDateTime.now());
                preference.setUpdateTime(LocalDateTime.now());
                return userCalendarPreferenceMapper.insert(preference) > 0;
            }
        } catch (Exception e) {
            log.error("保存用户偏好设置失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleEventCollection(Long userId, Long eventId) {
        try {
            UserCalendarCollection existing = userCalendarCollectionMapper.selectByUserIdAndTarget(userId, 1, eventId);
            
            if (existing != null) {
                // 切换收藏状态
                int newStatus = existing.getStatus() == 1 ? 0 : 1;
                return userCalendarCollectionMapper.updateStatus(userId, 1, eventId, newStatus) > 0;
            } else {
                // 新增收藏记录
                UserCalendarCollection collection = new UserCalendarCollection();
                collection.setUserId(userId);
                collection.setCollectionType(1); // 1-事件
                collection.setTargetId(eventId);
                collection.setCollectionTime(LocalDateTime.now());
                collection.setStatus(1);
                collection.setCreateTime(LocalDateTime.now());
                collection.setUpdateTime(LocalDateTime.now());
                return userCalendarCollectionMapper.insert(collection) > 0;
            }
        } catch (Exception e) {
            log.error("切换事件收藏状态失败", e);
            return false;
        }
    }

    @Override
    public List<DeveloperCalendarEvent> getUserCollectedEvents(Long userId) {
        List<UserCalendarCollection> collections = userCalendarCollectionMapper.selectByUserIdAndType(userId, 1, 1);
        if (CollectionUtils.isEmpty(collections)) {
            return new ArrayList<>();
        }

        // 批量查询所有收藏的事件，避免N+1问题
        List<Long> eventIds = collections.stream()
                .map(UserCalendarCollection::getTargetId)
                .collect(java.util.stream.Collectors.toList());
        
        List<DeveloperCalendarEvent> allEvents = developerCalendarEventMapper.selectBatchIds(eventIds);
        
        // 过滤出状态为启用的事件
        return allEvents.stream()
                .filter(event -> event != null && event.getStatus() == 1)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public boolean isEventCollected(Long userId, Long eventId) {
        UserCalendarCollection collection = userCalendarCollectionMapper.selectByUserIdAndTarget(userId, 1, eventId);
        return collection != null && collection.getStatus() == 1;
    }

    /**
     * 创建默认偏好设置
     */
    private UserCalendarPreference createDefaultPreference(Long userId) {
        UserCalendarPreference preference = new UserCalendarPreference();
        preference.setUserId(userId);
        preference.setEventReminder(1);
        preference.setDailyContentPush(1);
        preference.setDifficultyPreference(2); // 中级
        preference.setStatus(1);
        return preference;
    }

    // ========== 后台管理方法实现 ==========

    @Override
    public List<DeveloperCalendarEvent> getAllEvents() {
        List<DeveloperCalendarEvent> events = developerCalendarEventMapper.selectAllEnabled();
        return events != null ? events : new ArrayList<>();
    }

    @Override
    public DeveloperCalendarEvent getEventById(Long id) {
        return developerCalendarEventMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createEvent(DeveloperCalendarEvent event) {
        try {
            event.setStatus(1); // 默认启用
            event.setCreateTime(LocalDateTime.now());
            event.setUpdateTime(LocalDateTime.now());
            return developerCalendarEventMapper.insert(event) > 0;
        } catch (Exception e) {
            log.error("创建事件失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEvent(DeveloperCalendarEvent event) {
        try {
            event.setUpdateTime(LocalDateTime.now());
            return developerCalendarEventMapper.updateById(event) > 0;
        } catch (Exception e) {
            log.error("更新事件失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEvent(Long id) {
        try {
            return developerCalendarEventMapper.deleteById(id) > 0;
        } catch (Exception e) {
            log.error("删除事件失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteEvents(List<Long> ids) {
        try {
            if (CollectionUtils.isEmpty(ids)) {
                return true;
            }
            // 使用批量删除，避免N+1问题
            developerCalendarEventMapper.deleteBatchIds(ids);
            return true;
        } catch (Exception e) {
            log.error("批量删除事件失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEventStatus(Long id, Integer status) {
        try {
            return developerCalendarEventMapper.updateStatusById(id, status) > 0;
        } catch (Exception e) {
            log.error("更新事件状态失败", e);
            return false;
        }
    }

    @Override
    public Object getEventStatistics() {
        try {
            java.util.Map<String, Object> statistics = new java.util.HashMap<>();
            
            // 统计各种类型的事件数量
            Long totalEvents = developerCalendarEventMapper.countByStatus(1);
            Long programmerHolidays = 0L; // 程序员节日
            Long techMemorials = 0L;      // 技术纪念日
            Long openSourceHolidays = 0L; // 开源节日
            
            List<DeveloperCalendarEvent> allEvents = developerCalendarEventMapper.selectAllEnabled();
            for (DeveloperCalendarEvent event : allEvents) {
                switch (event.getEventType()) {
                    case 1: programmerHolidays++; break;
                    case 2: techMemorials++; break;
                    case 3: openSourceHolidays++; break;
                }
            }
            
            Long majorEvents = allEvents.stream()
                    .map(event -> event.getIsMajor() == 1 ? 1L : 0L)
                    .reduce(0L, Long::sum);
            
            statistics.put("totalEvents", totalEvents);
            statistics.put("programmerHolidays", programmerHolidays);
            statistics.put("techMemorials", techMemorials);
            statistics.put("openSourceHolidays", openSourceHolidays);
            statistics.put("majorEvents", majorEvents);
            
            return statistics;
        } catch (Exception e) {
            log.error("获取事件统计失败", e);
            return new java.util.HashMap<>();
        }
    }
}
