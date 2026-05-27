package com.xiaou.user.config;

import com.xiaou.common.satoken.SaTokenUserUtil;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 将用户模块的资料查询能力注册给 common，供评论、消息等通用场景复用。
 */
@Component
@RequiredArgsConstructor
public class CommonUserProfileResolverConfig {

    private final UserInfoApiService userInfoApiService;

    private SaTokenUserUtil.UserProfileResolver userProfileResolver;

    @PostConstruct
    public void registerUserProfileResolver() {
        userProfileResolver = this::resolveUserProfile;
        SaTokenUserUtil.registerUserProfileResolver(userProfileResolver);
    }

    @PreDestroy
    public void clearUserProfileResolver() {
        SaTokenUserUtil.clearUserProfileResolver(userProfileResolver);
    }

    private SaTokenUserUtil.UserProfile resolveUserProfile(Long userId) {
        SimpleUserInfo userInfo = userInfoApiService.getSimpleUserInfo(userId);
        if (userInfo == null) {
            return null;
        }
        return new SaTokenUserUtil.UserProfile(
                userInfo.getDisplayName(),
                userInfo.getAvatar(),
                userInfo.getBio()
        );
    }
}
