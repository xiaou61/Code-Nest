package com.xiaou.web.learning.controller;

import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.web.learning.dto.LearningCockpitOverviewResponse;
import com.xiaou.web.learning.service.LearningCockpitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习成长驾驶舱聚合接口
 */
@RestController
@RequestMapping("/user/learning-cockpit")
@RequiredArgsConstructor
public class LearningCockpitController {

    private final LearningCockpitService learningCockpitService;

    @GetMapping("/overview")
    public Result<LearningCockpitOverviewResponse> getOverview(
            @RequestParam(required = false) String targetRole,
            @RequestParam(required = false) Integer weeklyHours
    ) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(learningCockpitService.getOverview(userId, targetRole, weeklyHours));
    }
}
