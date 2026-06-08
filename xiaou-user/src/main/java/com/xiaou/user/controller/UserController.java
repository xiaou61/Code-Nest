package com.xiaou.user.controller;

import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.user.dto.UserChangePasswordRequest;
import com.xiaou.user.dto.UserInfoResponse;
import com.xiaou.user.dto.UserUpdateRequest;
import com.xiaou.user.service.UserInfoService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.xiaou.filestorage.service.FileStorageService;
import com.xiaou.filestorage.dto.FileUploadResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户信息管理控制器
 *
 * @author xiaou
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserInfoService userInfoService;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    public Result<UserInfoResponse> getUserInfo(@Positive(message = "用户ID必须为正数") @PathVariable Long userId) {
        checkCurrentUser(userId);
        log.info("获取用户信息，用户ID: {}", userId);
        UserInfoResponse userInfo = userInfoService.getUserInfoById(userId);
        log.info("获取用户信息成功，用户ID: {}", userId);
        return Result.success("获取用户信息成功", userInfo);
    }

    /**
     * 获取当前用户信息（通过token获取用户ID）
     */
    @GetMapping("/profile")
    public Result<UserInfoResponse> getCurrentUserInfo() {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        UserInfoResponse userInfo = userInfoService.getUserInfoById(userId);
        return Result.success("获取用户信息成功", userInfo);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{userId}")
    public Result<UserInfoResponse> updateUserInfo(
            @Positive(message = "用户ID必须为正数") @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        checkCurrentUser(userId);
        log.info("更新用户信息，用户ID: {}", userId);
        UserInfoResponse userInfo = userInfoService.updateUserInfo(userId, request);
        log.info("更新用户信息成功，用户ID: {}", userId);
        return Result.success("用户信息更新成功", userInfo);
    }

    /**
     * 更新当前用户信息（通过token获取用户ID）
     */
    @PutMapping("/profile")
    public Result<UserInfoResponse> updateCurrentUserInfo(@Valid @RequestBody UserUpdateRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        UserInfoResponse userInfo = userInfoService.updateUserInfo(userId, request);
        return Result.success("用户信息更新成功", userInfo);
    }

    /**
     * 修改密码
     */
    @PutMapping("/{userId}/password")
    public Result<Void> changePassword(
            @Positive(message = "用户ID必须为正数") @PathVariable Long userId,
            @Valid @RequestBody UserChangePasswordRequest request) {
        checkCurrentUser(userId);
        log.info("修改用户密码，用户ID: {}", userId);
        userInfoService.changePassword(userId, request);
        StpUserUtil.logout();
        log.info("修改用户密码成功，用户ID: {}", userId);
        return Result.success("密码修改成功，请重新登录", null);
    }

    /**
     * 修改当前用户密码（通过token获取用户ID）
     */
    @PutMapping("/password")
    public Result<Void> changeCurrentUserPassword(@Valid @RequestBody UserChangePasswordRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        userInfoService.changePassword(userId, request);
        // 密码修改后，退出当前登录，强制重新登录
        StpUserUtil.logout();
        return Result.success("密码修改成功，请重新登录", null);
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar/upload")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();

        // 文件校验
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的头像文件");
        }

        // 检查文件类型
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            return Result.error("文件名无效");
        }
        
        String extension = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        if (!extension.matches("jpg|jpeg|png|gif")) {
            return Result.error("仅支持jpg、jpeg、png、gif格式的图片");
        }

        // 检查文件大小 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("头像文件大小不能超过5MB");
        }

        log.info("用户上传头像，用户ID: {}, 文件名: {}, 大小: {}KB", 
            userId, originalName, file.getSize() / 1024);

        // 上传文件
        FileUploadResult uploadResult = fileStorageService.uploadSingle(file, "user", "avatar");
        
        if (!uploadResult.isSuccess()) {
            return Result.error("头像上传失败: " + uploadResult.getErrorMessage());
        }

        // 更新用户头像
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setAvatar(uploadResult.getAccessUrl());
        
        userInfoService.updateUserInfo(userId, updateRequest);

        log.info("用户头像上传成功，用户ID: {}, 头像URL: {}", 
            userId, uploadResult.getAccessUrl());
        
        return Result.success("头像上传成功", uploadResult.getAccessUrl());
    }

    private void checkCurrentUser(Long userId) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        if (!currentUserId.equals(userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "权限不足，无法操作其他用户");
        }
    }
}
