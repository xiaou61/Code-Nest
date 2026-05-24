package com.xiaou.user.controller;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.user.dto.AdminCreateUserRequest;
import com.xiaou.user.dto.UserInfoResponse;
import com.xiaou.user.dto.UserQueryRequest;
import com.xiaou.user.dto.UserUpdateRequest;
import com.xiaou.user.service.UserInfoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台用户管理控制器
 *
 * @author xiaou
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserInfoService userInfoService;

    /**
     * 分页查询用户列表
     */
    @RequireAdmin(message = "查询用户列表需要管理员权限")
    @GetMapping("/list")
    public Result<PageResult<UserInfoResponse>> getUserList(UserQueryRequest request) {
        log.info("管理员查询用户列表，查询条件: {}", request);
        PageResult<UserInfoResponse> result = userInfoService.getUserList(request);
        log.info("管理员查询用户列表成功，总数: {}", result.getTotal());
        return Result.success("查询用户列表成功", result);
    }

    /**
     * 获取所有用户（不分页，谨慎使用）
     */
    @RequireAdmin(message = "查询所有用户需要管理员权限")
    @GetMapping("/all")
    public Result<List<UserInfoResponse>> getAllUsers() {
        log.info("管理员查询所有用户");

        UserQueryRequest request = new UserQueryRequest();
        request.setPageNum(1);
        request.setPageSize(Integer.MAX_VALUE);

        PageResult<UserInfoResponse> result = userInfoService.getUserList(request);
        log.info("管理员查询所有用户成功，总数: {}", result.getTotal());
        return Result.success("查询所有用户成功", result.getRecords());
    }

    /**
     * 根据ID获取用户详情
     */
    @RequireAdmin(message = "查询用户详情需要管理员权限")
    @GetMapping("/{userId}")
    public Result<UserInfoResponse> getUserInfo(@Positive(message = "用户ID必须为正数") @PathVariable Long userId) {
        log.info("管理员获取用户详情，用户ID: {}", userId);
        UserInfoResponse userInfo = userInfoService.getUserInfoById(userId);
        log.info("管理员获取用户详情成功，用户ID: {}", userId);
        return Result.success("获取用户详情成功", userInfo);
    }

    /**
     * 创建新用户
     */
    @RequireAdmin(message = "创建用户需要管理员权限")
    @PostMapping("/create")
    public Result<UserInfoResponse> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        log.info("管理员创建用户，用户名: {}, 管理员ID: {}", request.getUsername(), adminId);
        UserInfoResponse user = userInfoService.createUser(request, adminId);
        log.info("管理员创建用户成功，用户ID: {}", user.getId());
        return Result.success("创建用户成功", user);
    }

    /**
     * 更新用户信息
     */
    @RequireAdmin(message = "更新用户信息需要管理员权限")
    @PutMapping("/{userId}")
    public Result<UserInfoResponse> updateUser(
            @Positive(message = "用户ID必须为正数") @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        log.info("管理员更新用户，用户ID: {}, 管理员ID: {}", userId, adminId);
        UserInfoResponse user = userInfoService.updateUserInfo(userId, request);
        log.info("管理员更新用户成功，用户ID: {}", userId);
        return Result.success("更新用户成功", user);
    }

    /**
     * 删除用户（逻辑删除）
     */
    @RequireAdmin(message = "删除用户需要管理员权限")
    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@Positive(message = "用户ID必须为正数") @PathVariable Long userId) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        log.info("管理员删除用户，用户ID: {}, 管理员ID: {}", userId, adminId);
        userInfoService.deleteUser(userId, adminId);
        log.info("管理员删除用户成功，用户ID: {}", userId);
        return Result.success("删除用户成功", null);
    }

    /**
     * 批量删除用户（逻辑删除）
     */
    @RequireAdmin(message = "批量删除用户需要管理员权限")
    @DeleteMapping("/batch")
    public Result<Void> deleteUsers(@NotEmpty(message = "用户ID列表不能为空") @RequestBody List<@NotNull @Positive Long> userIds) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        log.info("管理员批量删除用户，用户数量: {}, 管理员ID: {}", userIds.size(), adminId);
        userInfoService.deleteUsers(userIds, adminId);
        log.info("管理员批量删除用户成功，用户数量: {}", userIds.size());
        return Result.success("批量删除用户成功", null);
    }

    /**
     * 启用/禁用用户
     */
    @RequireAdmin(message = "修改用户状态需要管理员权限")
    @PutMapping("/{userId}/status")
    public Result<Void> updateUserStatus(
            @Positive(message = "用户ID必须为正数") @PathVariable Long userId,
            @NotNull(message = "状态不能为空") @Min(value = 0, message = "状态参数错误") @Max(value = 1, message = "状态参数错误")
            @RequestParam Integer status) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        log.info("管理员修改用户状态，用户ID: {}, 状态: {}, 管理员ID: {}", userId, status, adminId);
        userInfoService.updateUserStatus(userId, status, adminId);
        String statusText = status == 0 ? "启用" : "禁用";
        log.info("管理员修改用户状态成功，用户ID: {}, 状态: {}", userId, statusText);
        return Result.success(statusText + "用户成功", null);
    }

    /**
     * 重置用户密码
     */
    @RequireAdmin(message = "重置用户密码需要管理员权限")
    @PutMapping("/{userId}/reset-password")
    public Result<Void> resetPassword(
            @Positive(message = "用户ID必须为正数") @PathVariable Long userId,
            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 20, message = "新密码长度必须在6-20个字符之间")
            @RequestParam(defaultValue = "123456") String newPassword) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        log.info("管理员重置用户密码，用户ID: {}, 管理员ID: {}", userId, adminId);
        userInfoService.resetUserPassword(userId, newPassword, adminId);
        log.info("管理员重置用户密码成功，用户ID: {}", userId);
        return Result.success("重置密码成功", null);
    }

    /**
     * 获取用户统计信息
     */
    @RequireAdmin(message = "查看用户统计需要管理员权限")
    @GetMapping("/statistics")
    public Result<Object> getUserStatistics() {
        log.info("管理员获取用户统计信息");

        UserQueryRequest totalRequest = new UserQueryRequest();
        totalRequest.setPageSize(1);
        PageResult<UserInfoResponse> totalResult = userInfoService.getUserList(totalRequest);

        UserQueryRequest activeRequest = new UserQueryRequest();
        activeRequest.setStatus(0);
        activeRequest.setPageSize(1);
        PageResult<UserInfoResponse> activeResult = userInfoService.getUserList(activeRequest);

        UserQueryRequest disabledRequest = new UserQueryRequest();
        disabledRequest.setStatus(1);
        disabledRequest.setPageSize(1);
        PageResult<UserInfoResponse> disabledResult = userInfoService.getUserList(disabledRequest);

        UserQueryRequest deletedRequest = new UserQueryRequest();
        deletedRequest.setStatus(2);
        deletedRequest.setPageSize(1);
        PageResult<UserInfoResponse> deletedResult = userInfoService.getUserList(deletedRequest);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalUsers", totalResult.getTotal());
        statistics.put("activeUsers", activeResult.getTotal());
        statistics.put("disabledUsers", disabledResult.getTotal());
        statistics.put("deletedUsers", deletedResult.getTotal());

        log.info("管理员获取用户统计信息成功");
        return Result.success("获取用户统计信息成功", statistics);
    }
}
