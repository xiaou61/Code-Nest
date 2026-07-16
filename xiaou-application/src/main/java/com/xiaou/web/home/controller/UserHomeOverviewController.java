package com.xiaou.web.home.controller;

import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.web.home.dto.UserHomeOverviewResponse;
import com.xiaou.web.home.service.UserHomeOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户首页聚合接口。
 */
@RestController
@RequestMapping("/user/home")
@RequiredArgsConstructor
public class UserHomeOverviewController {

    private final UserHomeOverviewService userHomeOverviewService;

    @GetMapping("/overview")
    public Result<UserHomeOverviewResponse> getOverview() {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(userHomeOverviewService.getOverview(userId));
    }
}
