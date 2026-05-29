package com.xiaou.user.service.impl;

import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import com.xiaou.user.domain.UserInfo;
import com.xiaou.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户信息API服务实现类
 * 供其他模块调用，避免循环依赖
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoApiServiceImpl implements UserInfoApiService {
    
    private final UserInfoMapper userInfoMapper;
    
    @Override
    public SimpleUserInfo getSimpleUserInfo(Long userId) {
        if (userId == null) {
            return null;
        }
        
        try {
            UserInfo user = userInfoMapper.selectById(userId);
            if (user == null || user.getStatus() == 2) {
                return null;
            }
            
            return SimpleUserInfo.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .realName(user.getRealName())
                    .avatar(user.getAvatar())
                    .bio(user.getRemark())
                    .build();
                    
        } catch (Exception e) {
            log.warn("获取用户信息失败，userId: {}, 错误: {}", userId, e.getMessage());
            return null;
        }
    }
    
    @Override
    public String getUserDisplayName(Long userId) {
        SimpleUserInfo userInfo = getSimpleUserInfo(userId);
        return userInfo != null ? userInfo.getDisplayName() : "用户" + userId;
    }
    
    @Override
    public java.util.Map<Long, SimpleUserInfo> getSimpleUserInfoBatch(java.util.List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        try {
            // 去重并过滤空null
            java.util.List<Long> distinctIds = userIds.stream()
                    .filter(id -> id != null)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            
            if (distinctIds.isEmpty()) {
                return new java.util.HashMap<>();
            }
            
            // 批量查询用户
            java.util.List<UserInfo> users = userInfoMapper.selectBatchIds(distinctIds);
            
            // 转换为Map
            return users.stream()
                    .filter(user -> user != null && user.getStatus() != 2)
                    .collect(java.util.stream.Collectors.toMap(
                            UserInfo::getId,
                            user -> SimpleUserInfo.builder()
                                    .id(user.getId())
                                    .username(user.getUsername())
                                    .nickname(user.getNickname())
                                    .realName(user.getRealName())
                                    .avatar(user.getAvatar())
                                    .bio(user.getRemark())
                                    .build(),
                            (existing, replacement) -> existing // 如果有重复，保留第一个
                    ));
                    
        } catch (Exception e) {
            log.warn("批量获取用户信息失败，错误: {}", e.getMessage());
            return new java.util.HashMap<>();
        }
    }
}
