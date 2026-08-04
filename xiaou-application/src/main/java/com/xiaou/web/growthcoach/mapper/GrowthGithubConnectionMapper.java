package com.xiaou.web.growthcoach.mapper;

import com.xiaou.web.growthcoach.domain.GrowthGithubConnection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * GitHub OAuth 账号绑定的最小持久化访问。
 */
@Mapper
public interface GrowthGithubConnectionMapper {

    GrowthGithubConnection selectByUserId(@Param("userId") Long userId);

    GrowthGithubConnection selectByGithubUserId(@Param("githubUserId") Long githubUserId);

    int upsert(GrowthGithubConnection connection);

    int deleteByUserId(@Param("userId") Long userId);
}
