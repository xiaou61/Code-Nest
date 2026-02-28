package com.xiaou.oj.controller.pub;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.oj.domain.OjContest;
import com.xiaou.oj.dto.contest.ContestQueryRequest;
import com.xiaou.oj.dto.contest.ContestRankingItem;
import com.xiaou.oj.service.OjContestRankingService;
import com.xiaou.oj.service.OjContestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OJ 赛事公共接口（用户端）
 *
 * @author xiaou
 */
@Tag(name = "OJ赛事", description = "赛事浏览、报名和榜单")
@RestController
@RequestMapping("/oj/contests")
@RequiredArgsConstructor
public class OjContestPublicController {

    private final OjContestService contestService;
    private final OjContestRankingService contestRankingService;

    @Operation(summary = "分页查询赛事")
    @PostMapping("/list")
    public Result<PageResult<OjContest>> list(@RequestBody ContestQueryRequest request) {
        return Result.success(contestService.getPublicContests(request));
    }

    @Operation(summary = "赛事详情")
    @GetMapping("/{id}")
    public Result<OjContest> detail(@PathVariable Long id) {
        return Result.success(contestService.getContestById(id));
    }

    @Operation(summary = "报名赛事")
    @PostMapping("/{id}/join")
    public Result<Void> join(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        contestService.joinContest(id, userId);
        return Result.success();
    }

    @Operation(summary = "赛事榜单")
    @GetMapping("/{id}/ranking")
    public Result<List<ContestRankingItem>> ranking(@PathVariable Long id) {
        return Result.success(contestRankingService.getContestRanking(id));
    }
}
