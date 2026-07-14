package com.xiaou.moyu.controller.admin;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.JsonUtils;
import com.xiaou.moyu.domain.DeveloperCalendarEvent;
import com.xiaou.moyu.dto.AdminCalendarEventRequest;
import com.xiaou.moyu.service.DeveloperCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 后台程序员日历管理控制器
 *
 * @author xiaou
 */
@Slf4j
@RestController
@RequestMapping("/admin/moyu/developer-calendar")
@RequiredArgsConstructor
public class AdminDeveloperCalendarController {
    
    private final DeveloperCalendarService developerCalendarService;
    
    /**
     * 获取所有日历事件列表
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "获取日历事件列表")
    @GetMapping("/events")
    @RequireAdmin
    public Result<List<DeveloperCalendarEvent>> getAllEvents() {
        List<DeveloperCalendarEvent> events = developerCalendarService.getAllEvents();
        return Result.success(events);
    }
    
    /**
     * 根据事件类型获取事件列表
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "根据类型获取事件")
    @GetMapping("/events/type/{eventType}")
    @RequireAdmin
    public Result<List<DeveloperCalendarEvent>> getEventsByType(@PathVariable Integer eventType) {
        List<DeveloperCalendarEvent> events = developerCalendarService.getEventsByType(eventType);
        return Result.success(events);
    }
    
    /**
     * 根据ID获取事件详情
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "获取事件详情")
    @GetMapping("/events/{id}")
    @RequireAdmin
    public Result<DeveloperCalendarEvent> getEventById(@PathVariable Long id) {
        DeveloperCalendarEvent event = developerCalendarService.getEventById(id);
        if (event == null) {
            throw new BusinessException("事件不存在");
        }
        return Result.success(event);
    }
    
    /**
     * 创建日历事件
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.INSERT, description = "创建日历事件")
    @PostMapping("/events")
    @RequireAdmin
    public Result<Void> createEvent(@Valid @RequestBody AdminCalendarEventRequest request) {
        DeveloperCalendarEvent event = convertToEntity(request);
        boolean success = developerCalendarService.createEvent(event);
        if (!success) {
            throw new BusinessException("创建事件失败");
        }
        return Result.success();
    }
    
    /**
     * 更新日历事件
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.UPDATE, description = "更新日历事件")
    @PutMapping("/events/{id}")
    @RequireAdmin
    public Result<Void> updateEvent(@PathVariable Long id, @Valid @RequestBody AdminCalendarEventRequest request) {
        DeveloperCalendarEvent event = convertToEntity(request);
        event.setId(id);
        boolean success = developerCalendarService.updateEvent(event);
        if (!success) {
            throw new BusinessException("更新事件失败");
        }
        return Result.success();
    }
    
    /**
     * 删除日历事件
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.DELETE, description = "删除日历事件")
    @DeleteMapping("/events/{id}")
    @RequireAdmin
    public Result<Void> deleteEvent(@PathVariable Long id) {
        boolean success = developerCalendarService.deleteEvent(id);
        if (!success) {
            throw new BusinessException("删除事件失败");
        }
        return Result.success();
    }
    
    /**
     * 批量删除日历事件
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.DELETE, description = "批量删除日历事件")
    @PostMapping("/events/batch-delete")
    @RequireAdmin
    public Result<Void> batchDeleteEvents(@RequestBody List<Long> ids) {
        boolean success = developerCalendarService.batchDeleteEvents(ids);
        if (!success) {
            throw new BusinessException("批量删除事件失败");
        }
        return Result.success();
    }
    
    /**
     * 更新事件状态
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.UPDATE, description = "更新事件状态")
    @PostMapping("/events/{id}/status")
    @RequireAdmin
    public Result<Void> updateEventStatus(@PathVariable Long id, @RequestParam Integer status) {
        boolean success = developerCalendarService.updateEventStatus(id, status);
        if (!success) {
            throw new BusinessException("更新状态失败");
        }
        return Result.success();
    }
    
    /**
     * 获取日历事件统计信息
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "获取事件统计")
    @GetMapping("/events/statistics")
    @RequireAdmin
    public Result<Object> getEventStatistics() {
        Object statistics = developerCalendarService.getEventStatistics();
        return Result.success(statistics);
    }
    
    /**
     * 将请求对象转换为实体对象
     */
    private DeveloperCalendarEvent convertToEntity(AdminCalendarEventRequest request) {
        DeveloperCalendarEvent event = new DeveloperCalendarEvent();
        event.setId(request.getId());
        event.setEventDate(request.getEventDate());
        event.setEventName(request.getEventName());
        event.setEventType(request.getEventType());
        event.setDescription(request.getDescription());
        event.setBlessingText(request.getBlessingText());
        event.setRelatedLinks(JsonUtils.listToJson(request.getRelatedLinks()));
        event.setIcon(request.getIcon());
        event.setColor(request.getColor());
        event.setIsMajor(request.getIsMajor());
        event.setSortOrder(request.getSortOrder());
        event.setStatus(request.getStatus());
        return event;
    }
}
