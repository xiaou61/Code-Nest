package com.xiaou.notification.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.domain.Notification;
import com.xiaou.common.domain.NotificationTemplate;
import com.xiaou.common.utils.NotificationUtil;
import com.xiaou.notification.dto.AnnouncementRequest;
import com.xiaou.notification.dto.BatchSendRequest;
import com.xiaou.notification.dto.NotificationQueryRequest;
import com.xiaou.notification.dto.NotificationStatistics;
import com.xiaou.notification.dto.StatisticsRequest;
import com.xiaou.notification.service.NotificationAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端消息通知控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/notification")
@RequiredArgsConstructor
public class AdminNotificationController {
    
    private final NotificationAdminService notificationAdminService;
    
    /**
     * 发布系统公告
     */
    @Log(module = "消息通知", type = Log.OperationType.INSERT, description = "发布系统公告")
    @PostMapping("/announcement")
    @RequireAdmin
    public Result<Void> publishAnnouncement(@RequestBody @Validated AnnouncementRequest request) {
        log.info("管理员发布系统公告: {}", request);
        NotificationUtil.sendAnnouncement(request.getTitle(), request.getContent(), request.getPriority());
        return Result.success();
    }
    
    /**
     * 获取消息统计信息
     */
    @Log(module = "消息通知", type = Log.OperationType.SELECT, description = "查询消息统计")
    @PostMapping("/statistics")
    @RequireAdmin
    public Result<NotificationStatistics> getStatistics(@RequestBody StatisticsRequest request) {
        NotificationStatistics statistics = notificationAdminService.getStatistics(request);
        return Result.success(statistics);
    }
    
    /**
     * 管理端查询所有消息列表
     */
    @Log(module = "消息通知", type = Log.OperationType.SELECT, description = "查询所有消息列表")
    @PostMapping("/list")
    @RequireAdmin
    public Result<PageResult<Notification>> getAllMessages(@RequestBody NotificationQueryRequest request) {
        log.info("管理端查询消息列表: {}", request);
        PageResult<Notification> result = notificationAdminService.getAllMessageList(request);
        return Result.success(result);
    }
    
    /**
     * 批量发送个人消息
     */
    @Log(module = "消息通知", type = Log.OperationType.INSERT, description = "批量发送个人消息")
    @PostMapping("/batch-send")
    @RequireAdmin
    public Result<Void> batchSendMessage(@RequestBody @Validated BatchSendRequest request) {
        log.info("管理员批量发送消息: {}", request);
        boolean success = notificationAdminService.batchSendMessage(request);
        if (success) {
            return Result.success();
        } else {
            return Result.error("批量发送失败");
        }
    }
    
    /**
     * 删除消息（管理员操作）
     */
    @Log(module = "消息通知", type = Log.OperationType.DELETE, description = "管理员删除消息")
    @PostMapping("/delete/{id}")
    @RequireAdmin
    public Result<Void> deleteMessage(@PathVariable Long id) {
        boolean success = notificationAdminService.deleteMessage(id);
        if (success) {
            return Result.success();
        } else {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "删除消息失败");
        }
    }
    
    /**
     * 获取所有模板列表
     */
    @Log(module = "消息通知", type = Log.OperationType.SELECT, description = "查询消息模板列表")
    @GetMapping("/templates")
    @RequireAdmin
    public Result<List<NotificationTemplate>> getTemplates() {
        List<NotificationTemplate> templates = notificationAdminService.getAllTemplates();
        return Result.success(templates);
    }
    
    /**
     * 创建消息模板
     */
    @Log(module = "消息通知", type = Log.OperationType.INSERT, description = "创建消息模板")
    @PostMapping("/templates")
    @RequireAdmin
    public Result<Void> createTemplate(@RequestBody @Validated NotificationTemplate template) {
        boolean success = notificationAdminService.createTemplate(template);
        if (success) {
            return Result.success();
        } else {
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建模板失败");
        }
    }
    
    /**
     * 更新消息模板
     */
    @Log(module = "消息通知", type = Log.OperationType.UPDATE, description = "更新消息模板")
    @PutMapping("/templates/{id}")
    @RequireAdmin
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody @Validated NotificationTemplate template) {
        template.setId(id);
        boolean success = notificationAdminService.updateTemplate(template);
        if (success) {
            return Result.success();
        } else {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "更新模板失败");
        }
    }
    
    /**
     * 删除消息模板
     */
    @Log(module = "消息通知", type = Log.OperationType.DELETE, description = "删除消息模板")
    @DeleteMapping("/templates/{id}")
    @RequireAdmin
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        boolean success = notificationAdminService.deleteTemplate(id);
        if (success) {
            return Result.success();
        } else {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "删除模板失败");
        }
    }
} 
