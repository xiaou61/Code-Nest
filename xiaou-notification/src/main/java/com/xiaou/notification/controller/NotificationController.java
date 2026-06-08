package com.xiaou.notification.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.domain.Notification;
import com.xiaou.notification.dto.DeleteMessageRequest;
import com.xiaou.notification.dto.MarkReadRequest;
import com.xiaou.notification.dto.NotificationQueryRequest;
import com.xiaou.notification.service.NotificationUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端消息通知控制器
 */
@Slf4j
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationUserService notificationUserService;
    
    /**
     * 获取消息列表
     */
    @Log(module = "消息通知", type = Log.OperationType.SELECT, description = "查询消息列表")
    @PostMapping("/list")
    public Result<PageResult<Notification>> getMessages(@RequestBody NotificationQueryRequest request) {
        log.info("Controller收到消息列表查询请求: {}", request);
        PageResult<Notification> result = notificationUserService.getMessageList(request);
        log.info("Controller返回结果，total: {}", result.getTotal());
        return Result.success(result);
    }
    
    /**
     * 获取未读消息数量
     */
    @Log(module = "消息通知", type = Log.OperationType.SELECT, description = "获取未读消息数量")
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        int count = notificationUserService.getUnreadCount();
        log.info("用户未读消息数量: {}", count);
        return Result.success(count);
    }
    
    /**
     * 获取消息详情
     */
    @Log(module = "消息通知", type = Log.OperationType.SELECT, description = "查询消息详情")
    @GetMapping("/{id}")
    public Result<Notification> getMessageDetail(@PathVariable Long id) {
        Notification notification = notificationUserService.getMessageDetail(id);
        return Result.success(notification);
    }
    
    /**
     * 标记消息已读
     */
    @Log(module = "消息通知", type = Log.OperationType.UPDATE, description = "标记消息已读")
    @PostMapping("/mark-read")
    public Result<Void> markAsRead(@RequestBody @Validated MarkReadRequest request) {
        boolean success = notificationUserService.markAsRead(request);
        if (success) {
            return Result.success();
        } else {
            return Result.error("标记已读失败");
        }
    }
    
    /**
     * 删除消息
     */
    @Log(module = "消息通知", type = Log.OperationType.DELETE, description = "删除消息")
    @PostMapping("/delete")
    public Result<Void> deleteMessage(@RequestBody @Validated DeleteMessageRequest request) {
        boolean success = notificationUserService.deleteMessage(request);
        if (success) {
            return Result.success();
        } else {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "删除消息失败");
        }
    }
    
    /**
     * 全部标记已读
     */
    @Log(module = "消息通知", type = Log.OperationType.UPDATE, description = "全部标记已读")
    @PostMapping("/mark-all-read")
    public Result<Void> markAllAsRead() {
        boolean success = notificationUserService.markAllAsRead();
        if (success) {
            return Result.success();
        } else {
            return Result.error("全部标记已读失败");
        }
    }
    

} 
