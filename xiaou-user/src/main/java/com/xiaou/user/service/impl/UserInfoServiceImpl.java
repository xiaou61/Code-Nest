package com.xiaou.user.service.impl;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.common.utils.PasswordUtil;
import com.xiaou.notification.api.NotificationCommand;
import com.xiaou.notification.api.NotificationPublisher;
import com.xiaou.user.domain.UserInfo;
import com.xiaou.user.dto.*;
import com.xiaou.user.mapper.UserInfoMapper;
import com.xiaou.user.service.CaptchaService;
import com.xiaou.points.service.PointsService;
import com.xiaou.user.service.UserInfoService;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xiaou.common.utils.AvatarUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息服务实现类
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoServiceImpl implements UserInfoService {

    private final UserInfoMapper userInfoMapper;
    private final CaptchaService captchaService;
    private final PointsService pointsService;
    private final NotificationPublisher notificationPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfo register(UserRegisterRequest request) {
        log.info("用户注册，用户名: {}, 邮箱: {}", request.getUsername(), request.getEmail());

        // 验证验证码
        if (!captchaService.verifyCaptcha(request.getCaptchaKey(), request.getCaptcha())) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 验证两次密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        // 检查用户名是否已存在
        if (isUsernameExists(request.getUsername(), null)) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (isEmailExists(request.getEmail(), null)) {
            throw new BusinessException("邮箱已被注册");
        }

        // 检查手机号是否已存在（如果提供了手机号）
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            if (isPhoneExists(request.getPhone(), null)) {
                throw new BusinessException("手机号已被注册");
            }
        }

        try {
            // 创建用户对象
            UserInfo user = new UserInfo();
            user.setUsername(request.getUsername());
            user.setPassword(PasswordUtil.encode(request.getPassword())); // 加密密码
            user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setStatus(0); // 正常状态
            user.setRegisterTime(LocalDateTime.now());
            user.setCreateTime(LocalDateTime.now());

            // 保存用户
            int result = userInfoMapper.insert(user);
            if (result <= 0) {
                throw new BusinessException("用户注册失败");
            }

            log.info("用户注册成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());

            // 为新用户创建积分账户
            try {
                pointsService.createPointsAccountForNewUser(user.getId());
                log.info("为新用户创建积分账户成功，用户ID: {}", user.getId());
            } catch (Exception e) {
                log.warn("为新用户创建积分账户失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            }

            // 发送欢迎消息
            try {
                notificationPublisher.publish(NotificationCommand.system(
                    user.getId(),
                    "欢迎加入 Code-Nest",
                    "亲爱的 " + user.getNickname() + "，欢迎您加入 Code-Nest 学习社区！这里有丰富的技术内容和活跃的学习氛围，祝您学习愉快！"
                ));
            } catch (Exception e) {
                log.warn("发送用户注册欢迎消息失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            }

            // 返回用户信息（不包含密码）
            user.setPassword(null);
            return user;

        } catch (Exception e) {
            log.error("用户注册异常，用户名: {}", request.getUsername(), e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("用户注册失败，请稍后重试");
        }
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request, String clientIp) {
        // 验证验证码
        if (!captchaService.verifyCaptcha(request.getCaptchaKey(), request.getCaptcha())) {
            throw new BusinessException("验证码错误或已过期");
        }

        try {
            // 根据用户名或邮箱查询用户
            UserInfo user = userInfoMapper.selectByUsernameOrEmail(request.getUsername());
            
            // 统一返回“用户名或密码错误”防止用户枚举攻击
            if (user == null) {
                throw new BusinessException("用户名或密码错误");
            }

            // 检查用户状态
            if (user.getStatus() == 1) {
                throw new BusinessException("账户已被禁用，请联系管理员");
            }
            if (user.getStatus() == 2) {
                throw new BusinessException("用户名或密码错误"); // 已删除用户也返回相同提示
            }

            // 验证密码
            if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessException("用户名或密码错误");
            }

            // 更新最后登录信息
            updateLastLoginInfo(user.getId(), clientIp);

            // 使用 Sa-Token 进行登录（自动生成 Token 并存储到 Redis）
            StpUserUtil.login(user.getId());
            
            // 存储用户信息到 Sa-Token Session
            StpUserUtil.set("userInfo", user);
            // 单独存储用户名，方便快速获取
            StpUserUtil.set("username", user.getUsername());
            
            // 获取 Token 值
            String accessToken = StpUserUtil.getTokenValue();

            // 构建响应
            UserLoginResponse response = new UserLoginResponse();
            response.setAccessToken(accessToken);
            response.setTokenType("Bearer");
            response.setExpiresIn(604800L); // 7天

            // 设置用户信息
            UserLoginResponse.UserInfo userInfo = new UserLoginResponse.UserInfo();
            userInfo.setId(user.getId());
            userInfo.setUsername(user.getUsername());
            userInfo.setNickname(user.getNickname());
            userInfo.setRealName(user.getRealName());
            userInfo.setEmail(user.getEmail());
            userInfo.setPhone(user.getPhone());
            // 处理头像，如果为空则使用默认头像
            userInfo.setAvatar(AvatarUtils.getUserAvatar(user.getAvatar(), user.getId()));
            userInfo.setGender(user.getGender());
            userInfo.setLastLoginTime(user.getLastLoginTime());
            response.setUserInfo(userInfo);

            log.info("用户登录成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
            return response;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户登录异常，用户名: {}", request.getUsername(), e);
            throw new BusinessException("登录失败，请稍后重试");
        }
    }

    @Override
    public UserInfo getUserById(Long id) {
        if (id == null) {
            return null;
        }
        UserInfo user = userInfoMapper.selectById(id);
        if (user != null) {
            // 不返回密码信息
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public UserInfo getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return userInfoMapper.selectByUsername(username);
    }

    @Override
    public UserInfo getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return userInfoMapper.selectByEmail(email);
    }

    @Override
    public boolean isUsernameExists(String username, Long excludeId) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        int count = userInfoMapper.checkUsernameExists(username, excludeId);
        return count > 0;
    }

    @Override
    public boolean isEmailExists(String email, Long excludeId) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        int count = userInfoMapper.checkEmailExists(email, excludeId);
        return count > 0;
    }

    @Override
    public boolean isPhoneExists(String phone, Long excludeId) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        int count = userInfoMapper.checkPhoneExists(phone, excludeId);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLoginInfo(Long userId, String clientIp) {
        if (userId != null) {
            LocalDateTime now = LocalDateTime.now();
            userInfoMapper.updateLastLoginInfo(userId, now, clientIp);
            log.debug("更新用户最后登录信息，用户ID: {}, IP: {}", userId, clientIp);
        }
    }

    @Override
    public UserInfoResponse getUserInfoById(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        try {
            UserInfo user = userInfoMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }

            if (user.getStatus() == 2) {
                throw new BusinessException("用户已被删除");
            }

            return convertToUserInfoResponse(user);

        } catch (Exception e) {
            log.error("获取用户信息失败，用户ID: {}", userId, e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("获取用户信息失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse updateUserInfo(Long userId, UserUpdateRequest request) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        log.info("更新用户信息，用户ID: {}", userId);

        try {
            // 检查用户是否存在
            UserInfo existingUser = userInfoMapper.selectById(userId);
            if (existingUser == null) {
                throw new BusinessException("用户不存在");
            }

            if (existingUser.getStatus() == 2) {
                throw new BusinessException("用户已被删除");
            }

            // 检查邮箱是否被其他用户使用
            if (request.getEmail() != null && !request.getEmail().equals(existingUser.getEmail())) {
                if (isEmailExists(request.getEmail(), userId)) {
                    throw new BusinessException("邮箱已被其他用户使用");
                }
            }

            // 检查手机号是否被其他用户使用
            if (request.getPhone() != null && !request.getPhone().equals(existingUser.getPhone())) {
                if (isPhoneExists(request.getPhone(), userId)) {
                    throw new BusinessException("手机号已被其他用户使用");
                }
            }

            // 更新用户信息
            UserInfo updateUser = new UserInfo();
            updateUser.setId(userId);
            updateUser.setNickname(request.getNickname());
            updateUser.setRealName(request.getRealName());
            updateUser.setEmail(request.getEmail());
            updateUser.setPhone(request.getPhone());
            updateUser.setAvatar(request.getAvatar());
            updateUser.setGender(request.getGender());
            updateUser.setBirthday(request.getBirthday());
            updateUser.setUpdateTime(LocalDateTime.now());

            int result = userInfoMapper.update(updateUser);
            if (result <= 0) {
                throw new BusinessException("用户信息更新失败");
            }

            log.info("用户信息更新成功，用户ID: {}", userId);

            // 发送信息变更通知
            try {
                // 检查是否有重要信息变更（邮箱或手机号）
                boolean hasImportantChange = false;
                StringBuilder changeDetails = new StringBuilder("您的账户信息已更新：");
                
                if (request.getEmail() != null && !request.getEmail().equals(existingUser.getEmail())) {
                    hasImportantChange = true;
                    changeDetails.append("邮箱已更新、");
                }
                if (request.getPhone() != null && !request.getPhone().equals(existingUser.getPhone())) {
                    hasImportantChange = true;
                    changeDetails.append("手机号已更新、");
                }
                
                if (hasImportantChange) {
                    String details = changeDetails.toString();
                    if (details.endsWith("、")) {
                        details = details.substring(0, details.length() - 1);
                    }
                    details += "。如非本人操作，请立即联系客服。";
                    
                    notificationPublisher.publish(NotificationCommand.system(
                        userId,
                        "账户信息变更通知",
                        details
                    ));
                }
            } catch (Exception e) {
                log.warn("发送用户信息变更通知失败，用户ID: {}, 错误: {}", userId, e.getMessage());
            }

            // 返回更新后的用户信息
            return getUserInfoById(userId);

        } catch (Exception e) {
            log.error("更新用户信息失败，用户ID: {}", userId, e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("更新用户信息失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, UserChangePasswordRequest request) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        log.info("修改用户密码，用户ID: {}", userId);

        // 验证验证码
        if (!captchaService.verifyCaptcha(request.getCaptchaKey(), request.getCaptcha())) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 验证两次新密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }

        try {
            // 检查用户是否存在
            UserInfo user = userInfoMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }

            if (user.getStatus() == 2) {
                throw new BusinessException("用户已被删除");
            }

            // 验证原密码
            if (!PasswordUtil.matches(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException("原密码错误");
            }

            // 检查新密码是否与原密码相同
            if (PasswordUtil.matches(request.getNewPassword(), user.getPassword())) {
                throw new BusinessException("新密码不能与原密码相同");
            }

            // 更新密码
            String encodedNewPassword = PasswordUtil.encode(request.getNewPassword());
            int result = userInfoMapper.updatePassword(userId, encodedNewPassword);
            if (result <= 0) {
                throw new BusinessException("密码修改失败");
            }

            log.info("用户密码修改成功，用户ID: {}", userId);

            // 发送密码修改成功通知
            try {
                notificationPublisher.publish(NotificationCommand.system(
                    userId,
                    "密码修改成功",
                    "您的账户密码已成功修改，修改时间：" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "。如非本人操作，请立即联系客服。"
                ));
            } catch (Exception e) {
                log.warn("发送密码修改通知失败，用户ID: {}, 错误: {}", userId, e.getMessage());
            }

        } catch (Exception e) {
            log.error("修改用户密码失败，用户ID: {}", userId, e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("修改密码失败，请稍后重试");
        }
    }

    @Override
    public PageResult<UserInfoResponse> getUserList(UserQueryRequest request) {
        log.info("分页查询用户列表，查询条件: {}", request);

        try {
            return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
                // 构建查询条件
                UserInfo queryUser = new UserInfo();
                queryUser.setUsername(request.getUsername());
                queryUser.setNickname(request.getNickname());
                queryUser.setRealName(request.getRealName());
                queryUser.setEmail(request.getEmail());
                queryUser.setPhone(request.getPhone());
                queryUser.setGender(request.getGender());
                queryUser.setStatus(request.getStatus());

                // 查询用户列表
                List<UserInfo> userList = userInfoMapper.selectList(queryUser);
                
                // 转换为响应DTO
                return userList.stream()
                        .map(this::convertToUserInfoResponse)
                        .collect(Collectors.toList());
            });

        } catch (Exception e) {
            log.error("分页查询用户列表失败", e);
            throw new BusinessException("查询用户列表失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse createUser(AdminCreateUserRequest request, Long adminId) {
        log.info("管理员创建用户，用户名: {}, 管理员ID: {}", request.getUsername(), adminId);

        // 检查用户名是否已存在
        if (isUsernameExists(request.getUsername(), null)) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (isEmailExists(request.getEmail(), null)) {
            throw new BusinessException("邮箱已被注册");
        }

        // 检查手机号是否已存在（如果提供了手机号）
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            if (isPhoneExists(request.getPhone(), null)) {
                throw new BusinessException("手机号已被注册");
            }
        }

        try {
            // 创建用户对象
            UserInfo user = new UserInfo();
            user.setUsername(request.getUsername());
            user.setPassword(PasswordUtil.encode(request.getPassword())); // 加密密码
            user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
            user.setRealName(request.getRealName());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setAvatar(request.getAvatar());
            user.setGender(request.getGender());
            user.setBirthday(request.getBirthday());
            user.setStatus(request.getStatus() != null ? request.getStatus() : 0);
            user.setRemark(request.getRemark());
            user.setRegisterTime(LocalDateTime.now());
            user.setCreateTime(LocalDateTime.now());
            user.setCreateBy(adminId);

            // 保存用户
            int result = userInfoMapper.insert(user);
            if (result <= 0) {
                throw new BusinessException("创建用户失败");
            }

            log.info("管理员创建用户成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());

            // 为新用户创建积分账户
            try {
                pointsService.createPointsAccountForNewUser(user.getId());
                log.info("为新用户创建积分账户成功，用户ID: {}", user.getId());
            } catch (Exception e) {
                log.warn("为新用户创建积分账户失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            }

            return convertToUserInfoResponse(user);

        } catch (Exception e) {
            log.error("管理员创建用户异常，用户名: {}", request.getUsername(), e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("创建用户失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId, Long adminId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        log.info("管理员删除用户，用户ID: {}, 管理员ID: {}", userId, adminId);

        try {
            // 检查用户是否存在
            UserInfo user = userInfoMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }

            if (user.getStatus() == 2) {
                throw new BusinessException("用户已被删除");
            }

            // 逻辑删除
            int result = userInfoMapper.deleteById(userId);
            if (result <= 0) {
                throw new BusinessException("删除用户失败");
            }

            log.info("管理员删除用户成功，用户ID: {}", userId);

        } catch (Exception e) {
            log.error("管理员删除用户失败，用户ID: {}", userId, e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("删除用户失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(List<Long> userIds, Long adminId) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("用户ID列表不能为空");
        }

        log.info("管理员批量删除用户，用户数量: {}, 管理员ID: {}", userIds.size(), adminId);

        try {
            // 批量逻辑删除
            int result = userInfoMapper.deleteByIds(userIds);
            if (result <= 0) {
                throw new BusinessException("批量删除用户失败");
            }

            log.info("管理员批量删除用户成功，删除数量: {}", result);

        } catch (Exception e) {
            log.error("管理员批量删除用户失败", e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("批量删除用户失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status, Long adminId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("用户状态参数错误");
        }

        log.info("管理员修改用户状态，用户ID: {}, 状态: {}, 管理员ID: {}", userId, status, adminId);

        try {
            // 检查用户是否存在
            UserInfo user = userInfoMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }

            if (user.getStatus() == 2) {
                throw new BusinessException("用户已被删除");
            }

            // 更新状态
            int result = userInfoMapper.updateStatus(userId, status);
            if (result <= 0) {
                throw new BusinessException("修改用户状态失败");
            }

            log.info("管理员修改用户状态成功，用户ID: {}, 状态: {}", userId, status);

        } catch (Exception e) {
            log.error("管理员修改用户状态失败，用户ID: {}", userId, e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("修改用户状态失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(Long userId, String newPassword, Long adminId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BusinessException("新密码不能为空");
        }

        log.info("管理员重置用户密码，用户ID: {}, 管理员ID: {}", userId, adminId);

        try {
            // 检查用户是否存在
            UserInfo user = userInfoMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }

            if (user.getStatus() == 2) {
                throw new BusinessException("用户已被删除");
            }

            // 加密新密码
            String encodedPassword = PasswordUtil.encode(newPassword);

            // 更新密码
            int result = userInfoMapper.updatePassword(userId, encodedPassword);
            if (result <= 0) {
                throw new BusinessException("重置密码失败");
            }

            log.info("管理员重置用户密码成功，用户ID: {}", userId);

        } catch (Exception e) {
            log.error("管理员重置用户密码失败，用户ID: {}", userId, e);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("重置密码失败，请稍后重试");
        }
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        return !isUsernameExists(username, null);
    }

    @Override
    public boolean isEmailAvailable(String email) {
        return !isEmailExists(email, null);
    }

    @Override
    public boolean isPhoneAvailable(String phone) {
        return !isPhoneExists(phone, null);
    }

    /**
     * 转换UserInfo为UserInfoResponse
     */
    private UserInfoResponse convertToUserInfoResponse(UserInfo user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRealName(user.getRealName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        // 处理头像，如果为空则使用默认头像
        response.setAvatar(AvatarUtils.getUserAvatar(user.getAvatar(), user.getId()));
        response.setGender(user.getGender());
        response.setBirthday(user.getBirthday());
        response.setStatus(user.getStatus());
        response.setLastLoginTime(user.getLastLoginTime());
        response.setRegisterTime(user.getRegisterTime());
        response.setCreateTime(user.getCreateTime());
        return response;
    }

}
