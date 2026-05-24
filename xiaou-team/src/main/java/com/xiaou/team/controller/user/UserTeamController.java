package com.xiaou.team.controller.user;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.team.dto.*;
import com.xiaou.team.service.StudyTeamService;
import com.xiaou.team.service.TeamCheckinService;
import com.xiaou.team.service.TeamDiscussionService;
import com.xiaou.team.service.TeamMemberService;
import com.xiaou.team.service.TeamRankService;
import com.xiaou.team.service.TeamStatsService;
import com.xiaou.team.service.TeamTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户端学习小组控制器
 * 
 * @author xiaou
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user/team")
@RequiredArgsConstructor
public class UserTeamController {
    
    private final StudyTeamService teamService;
    private final TeamMemberService memberService;
    private final TeamTaskService taskService;
    private final TeamCheckinService checkinService;
    private final TeamRankService rankService;
    private final TeamDiscussionService discussionService;
    private final TeamStatsService statsService;
    
    /**
     * 创建小组
     */
    @PostMapping("/create")
    public Result<TeamResponse> createTeam(@Valid @RequestBody TeamCreateRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        TeamResponse response = teamService.createTeam(userId, request);
        return Result.success("创建成功", response);
    }
    
    /**
     * 更新小组
     */
    @PutMapping("/{teamId}")
    public Result<TeamResponse> updateTeam(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                           @Valid @RequestBody TeamCreateRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        TeamResponse response = teamService.updateTeam(userId, teamId, request);
        return Result.success("更新成功", response);
    }
    
    /**
     * 解散小组
     */
    @DeleteMapping("/{teamId}")
    public Result<Boolean> dissolveTeam(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = teamService.dissolveTeam(userId, teamId);
        return success ? Result.success("解散成功", true) : Result.error("解散失败");
    }
    
    /**
     * 获取小组详情
     */
    @GetMapping("/{teamId}")
    public Result<TeamDetailResponse> getTeamDetail(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        TeamDetailResponse response = teamService.getTeamDetail(userId, teamId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取小组列表（广场）
     */
    @PostMapping("/list")
    public Result<PageResult<TeamResponse>> getTeamList(@Valid @RequestBody TeamListRequest request) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        PageResult<TeamResponse> response = teamService.getTeamList(userId, request);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取我的小组列表
     */
    @GetMapping("/my")
    public Result<List<TeamResponse>> getMyTeams() {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        List<TeamResponse> response = teamService.getMyTeams(userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取我创建的小组列表
     */
    @GetMapping("/created")
    public Result<List<TeamResponse>> getCreatedTeams() {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        List<TeamResponse> response = teamService.getCreatedTeams(userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取推荐小组
     */
    @GetMapping("/recommend")
    public Result<List<TeamResponse>> getRecommendTeams() {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        List<TeamResponse> response = teamService.getRecommendTeams(userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取邀请码
     */
    @GetMapping("/{teamId}/invite-code")
    public Result<String> getInviteCode(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        String code = teamService.getInviteCode(userId, teamId);
        return Result.success("获取成功", code);
    }
    
    /**
     * 刷新邀请码
     */
    @PostMapping("/{teamId}/invite-code/refresh")
    public Result<String> refreshInviteCode(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        String code = teamService.refreshInviteCode(userId, teamId);
        return Result.success("刷新成功", code);
    }
    
    /**
     * 根据邀请码获取小组信息
     */
    @GetMapping("/by-code/{inviteCode}")
    public Result<TeamResponse> getTeamByInviteCode(@PathVariable @NotBlank(message = "邀请码不能为空") String inviteCode) {
        TeamResponse response = teamService.getTeamByInviteCode(inviteCode);
        return Result.success("获取成功", response);
    }
    
    // ==================== 成员管理接口 ====================
    
    /**
     * 申请加入小组
     */
    @PostMapping("/{teamId}/join")
    public Result<Boolean> applyJoin(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                     @RequestBody(required = false) JoinRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        if (request == null) {
            request = new JoinRequest();
        }
        request.setTeamId(teamId);
        boolean success = memberService.applyJoin(userId, request);
        return success ? Result.success("申请成功", true) : Result.error("申请失败");
    }
    
    /**
     * 通过邀请码加入小组
     */
    @PostMapping("/join-by-code")
    public Result<Boolean> joinByInviteCode(@Valid @RequestBody JoinRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = memberService.joinByInviteCode(userId, request.getInviteCode());
        return success ? Result.success("加入成功", true) : Result.error("加入失败");
    }
    
    /**
     * 退出小组
     */
    @PostMapping("/{teamId}/quit")
    public Result<Boolean> quitTeam(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = memberService.quitTeam(userId, teamId);
        return success ? Result.success("退出成功", true) : Result.error("退出失败");
    }
    
    /**
     * 获取小组成员列表
     */
    @GetMapping("/{teamId}/members")
    public Result<List<MemberResponse>> getMemberList(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        List<MemberResponse> response = memberService.getMemberList(teamId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取小组申请列表
     */
    @GetMapping("/{teamId}/applications")
    public Result<List<ApplicationResponse>> getApplicationList(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        List<ApplicationResponse> response = memberService.getApplicationList(userId, teamId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取我的申请记录
     */
    @GetMapping("/applications/my")
    public Result<List<ApplicationResponse>> getMyApplications() {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        List<ApplicationResponse> response = memberService.getMyApplications(userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 审批申请 - 通过
     */
    @PostMapping("/{teamId}/application/{applicationId}/approve")
    public Result<Boolean> approveApplication(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                              @PathVariable @Positive(message = "申请ID必须大于0") Long applicationId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = memberService.approveApplication(userId, applicationId);
        return success ? Result.success("审批通过", true) : Result.error("审批失败");
    }
    
    /**
     * 审批申请 - 拒绝
     */
    @PostMapping("/{teamId}/application/{applicationId}/reject")
    public Result<Boolean> rejectApplication(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                             @PathVariable @Positive(message = "申请ID必须大于0") Long applicationId,
                                             @RequestParam(required = false) @Pattern(regexp = "^.{0,200}$", message = "拒绝理由不能超过200个字符") String rejectReason) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = memberService.rejectApplication(userId, applicationId, rejectReason);
        return success ? Result.success("已拒绝", true) : Result.error("操作失败");
    }
    
    /**
     * 取消申请
     */
    @PostMapping("/application/{applicationId}/cancel")
    public Result<Boolean> cancelApplication(@PathVariable @Positive(message = "申请ID必须大于0") Long applicationId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = memberService.cancelApplication(userId, applicationId);
        return success ? Result.success("取消成功", true) : Result.error("取消失败");
    }
    
    /**
     * 移除成员
     */
    @DeleteMapping("/{teamId}/member/{targetUserId}")
    public Result<Boolean> removeMember(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                        @PathVariable @Positive(message = "目标用户ID必须大于0") Long targetUserId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = memberService.removeMember(userId, teamId, targetUserId);
        return success ? Result.success("移除成功", true) : Result.error("移除失败");
    }
    
    /**
     * 设置成员角色
     */
    @PutMapping("/{teamId}/member/{targetUserId}/role")
    public Result<Boolean> setMemberRole(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                         @PathVariable @Positive(message = "目标用户ID必须大于0") Long targetUserId,
                                         @RequestParam @Min(value = 2, message = "角色值不合法") @Max(value = 3, message = "角色值不合法") Integer role) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = memberService.setMemberRole(userId, teamId, targetUserId, role);
        return success ? Result.success("设置成功", true) : Result.error("设置失败");
    }
    
    /**
     * 转让组长
     */
    @PutMapping("/{teamId}/transfer")
    public Result<Boolean> transferLeader(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                          @RequestParam @Positive(message = "新组长ID必须大于0") Long newLeaderId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = memberService.transferLeader(userId, teamId, newLeaderId);
        return success ? Result.success("转让成功", true) : Result.error("转让失败");
    }
    
    /**
     * 禁言成员
     */
    @PostMapping("/{teamId}/member/{targetUserId}/mute")
    public Result<Boolean> muteMember(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                      @PathVariable @Positive(message = "目标用户ID必须大于0") Long targetUserId,
                                      @RequestParam @Min(value = 1, message = "禁言时长必须大于0")
                                      @Max(value = 10080, message = "禁言时长最长7天") Integer minutes) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = memberService.muteMember(userId, teamId, targetUserId, minutes);
        return success ? Result.success("禁言成功", true) : Result.error("禁言失败");
    }
    
    /**
     * 解除禁言
     */
    @DeleteMapping("/{teamId}/member/{targetUserId}/mute")
    public Result<Boolean> unmuteMember(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                        @PathVariable @Positive(message = "目标用户ID必须大于0") Long targetUserId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = memberService.unmuteMember(userId, teamId, targetUserId);
        return success ? Result.success("解除禁言成功", true) : Result.error("解除禁言失败");
    }
    
    // ==================== 任务管理接口 ====================
    
    /**
     * 创建打卡任务
     */
    @PostMapping("/{teamId}/task")
    public Result<Long> createTask(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                   @Valid @RequestBody TaskCreateRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        Long taskId = taskService.createTask(teamId, request, userId);
        return Result.success("创建成功", taskId);
    }
    
    /**
     * 更新打卡任务
     */
    @PutMapping("/task/{taskId}")
    public Result<Boolean> updateTask(@PathVariable @Positive(message = "任务ID必须大于0") Long taskId,
                                      @Valid @RequestBody TaskCreateRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        taskService.updateTask(taskId, request, userId);
        return Result.success("更新成功", true);
    }
    
    /**
     * 删除打卡任务
     */
    @DeleteMapping("/task/{taskId}")
    public Result<Boolean> deleteTask(@PathVariable @Positive(message = "任务ID必须大于0") Long taskId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        taskService.deleteTask(taskId, userId);
        return Result.success("删除成功", true);
    }
    
    /**
     * 启用/禁用任务
     */
    @PutMapping("/task/{taskId}/status")
    public Result<Boolean> setTaskStatus(@PathVariable @Positive(message = "任务ID必须大于0") Long taskId,
                                         @RequestParam @Min(value = 0, message = "任务状态不合法")
                                         @Max(value = 1, message = "任务状态不合法") Integer status) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        taskService.setTaskStatus(taskId, status, userId);
        return Result.success("操作成功", true);
    }
    
    /**
     * 获取任务详情
     */
    @GetMapping("/task/{taskId}")
    public Result<TaskResponse> getTaskDetail(@PathVariable @Positive(message = "任务ID必须大于0") Long taskId) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        TaskResponse response = taskService.getTaskDetail(taskId, userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取小组任务列表
     */
    @GetMapping("/{teamId}/tasks")
    public Result<List<TaskResponse>> getTaskList(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                                  @RequestParam(required = false) @Min(value = 0, message = "任务状态不合法")
                                                  @Max(value = 1, message = "任务状态不合法") Integer status) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        List<TaskResponse> response = taskService.getTaskList(teamId, status, userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取今日需要打卡的任务
     */
    @GetMapping("/{teamId}/tasks/today")
    public Result<List<TaskResponse>> getTodayTasks(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        List<TaskResponse> response = taskService.getTodayTasks(teamId, userId);
        return Result.success("获取成功", response);
    }
    
    // ==================== 打卡接口 ====================
    
    /**
     * 打卡
     */
    @PostMapping("/{teamId}/checkin")
    public Result<Long> checkin(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                @Valid @RequestBody CheckinRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        Long checkinId = checkinService.checkin(teamId, request, userId);
        return Result.success("打卡成功", checkinId);
    }
    
    /**
     * 补卡
     */
    @PostMapping("/{teamId}/checkin/supplement")
    public Result<Long> supplementCheckin(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                          @Valid @RequestBody CheckinRequest request,
                                          @RequestParam @NotBlank(message = "补卡日期不能为空")
                                          @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "补卡日期格式必须为yyyy-MM-dd") String date) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        LocalDate checkinDate = LocalDate.parse(date);
        Long checkinId = checkinService.supplementCheckin(teamId, request, checkinDate, userId);
        return Result.success("补卡成功", checkinId);
    }
    
    /**
     * 删除打卡记录
     */
    @DeleteMapping("/checkin/{checkinId}")
    public Result<Boolean> deleteCheckin(@PathVariable @Positive(message = "打卡ID必须大于0") Long checkinId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        checkinService.deleteCheckin(checkinId, userId);
        return Result.success("删除成功", true);
    }
    
    /**
     * 获取打卡详情
     */
    @GetMapping("/checkin/{checkinId}")
    public Result<CheckinResponse> getCheckinDetail(@PathVariable @Positive(message = "打卡ID必须大于0") Long checkinId) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        CheckinResponse response = checkinService.getCheckinDetail(checkinId, userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取小组打卡动态列表
     */
    @GetMapping("/{teamId}/checkins")
    public Result<List<CheckinResponse>> getCheckinList(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                                        @RequestParam(required = false) @Positive(message = "任务ID必须大于0") Long taskId,
                                                        @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
                                                        @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1")
                                                        @Max(value = 100, message = "每页条数最大为100") Integer pageSize) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        List<CheckinResponse> response = checkinService.getCheckinList(teamId, taskId, page, pageSize, userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取我的打卡记录
     */
    @GetMapping("/{teamId}/checkins/my")
    public Result<List<CheckinResponse>> getMyCheckins(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                                       @RequestParam(required = false) @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "开始日期格式必须为yyyy-MM-dd") String startDate,
                                                       @RequestParam(required = false) @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "结束日期格式必须为yyyy-MM-dd") String endDate) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        List<CheckinResponse> response = checkinService.getUserCheckins(userId, teamId, start, end);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取打卡日历数据
     */
    @GetMapping("/{teamId}/checkin/calendar")
    public Result<List<LocalDate>> getCheckinCalendar(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                                      @RequestParam @Min(value = 2000, message = "年份不合法")
                                                      @Max(value = 2100, message = "年份不合法") Integer year,
                                                      @RequestParam @Min(value = 1, message = "月份不合法")
                                                      @Max(value = 12, message = "月份不合法") Integer month) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        List<LocalDate> response = checkinService.getCheckinCalendar(userId, teamId, year, month);
        return Result.success("获取成功", response);
    }
    
    /**
     * 点赞打卡
     */
    @PostMapping("/checkin/{checkinId}/like")
    public Result<Boolean> likeCheckin(@PathVariable @Positive(message = "打卡ID必须大于0") Long checkinId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        checkinService.likeCheckin(checkinId, userId);
        return Result.success("点赞成功", true);
    }
    
    /**
     * 取消点赞
     */
    @DeleteMapping("/checkin/{checkinId}/like")
    public Result<Boolean> unlikeCheckin(@PathVariable @Positive(message = "打卡ID必须大于0") Long checkinId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        checkinService.unlikeCheckin(checkinId, userId);
        return Result.success("取消点赞成功", true);
    }
    
    /**
     * 获取连续打卡天数
     */
    @GetMapping("/{teamId}/checkin/streak")
    public Result<Integer> getStreakDays(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                         @RequestParam(required = false) @Positive(message = "任务ID必须大于0") Long taskId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        Integer streak = checkinService.getStreakDays(userId, teamId, taskId);
        return Result.success("获取成功", streak);
    }
    
    /**
     * 获取总打卡天数
     */
    @GetMapping("/{teamId}/checkin/total")
    public Result<Integer> getTotalCheckinDays(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        Integer total = checkinService.getTotalCheckinDays(userId, teamId);
        return Result.success("获取成功", total);
    }
    
    // ==================== 排行榜接口 ====================
    
    /**
     * 获取打卡次数排行榜
     */
    @GetMapping("/{teamId}/rank/checkin")
    public Result<List<RankResponse>> getCheckinRank(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                                     @RequestParam(defaultValue = "total") @Pattern(regexp = "total|weekly|monthly", message = "排行类型不合法") String type,
                                                     @RequestParam(defaultValue = "20") @Min(value = 1, message = "榜单条数最小为1")
                                                     @Max(value = 100, message = "榜单条数最大为100") Integer limit) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        List<RankResponse> response = rankService.getCheckinRank(teamId, type, limit, userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取连续打卡排行榜
     */
    @GetMapping("/{teamId}/rank/streak")
    public Result<List<RankResponse>> getStreakRank(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                                    @RequestParam(defaultValue = "20") @Min(value = 1, message = "榜单条数最小为1")
                                                    @Max(value = 100, message = "榜单条数最大为100") Integer limit) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        List<RankResponse> response = rankService.getStreakRank(teamId, limit, userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取学习时长排行榜
     */
    @GetMapping("/{teamId}/rank/duration")
    public Result<List<RankResponse>> getDurationRank(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                                      @RequestParam(defaultValue = "total") @Pattern(regexp = "total|weekly|monthly", message = "排行类型不合法") String type,
                                                      @RequestParam(defaultValue = "20") @Min(value = 1, message = "榜单条数最小为1")
                                                      @Max(value = 100, message = "榜单条数最大为100") Integer limit) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        List<RankResponse> response = rankService.getDurationRank(teamId, type, limit, userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取贡献值排行榜
     */
    @GetMapping("/{teamId}/rank/contribution")
    public Result<List<RankResponse>> getContributionRank(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                                          @RequestParam(defaultValue = "20") @Min(value = 1, message = "榜单条数最小为1")
                                                          @Max(value = 100, message = "榜单条数最大为100") Integer limit) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        List<RankResponse> response = rankService.getContributionRank(teamId, limit, userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取我的排名
     */
    @GetMapping("/{teamId}/rank/my")
    public Result<RankResponse> getMyRank(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                          @RequestParam(defaultValue = "checkin") @Pattern(regexp = "checkin|streak|duration|contribution", message = "排名类型不合法") String rankType) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        RankResponse response = rankService.getUserRank(teamId, userId, rankType);
        return Result.success("获取成功", response);
    }
    
    // ==================== 讨论接口 ====================
    
    /**
     * 创建讨论
     */
    @PostMapping("/{teamId}/discussion")
    public Result<Long> createDiscussion(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                         @Valid @RequestBody DiscussionCreateRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        Long discussionId = discussionService.createDiscussion(teamId, request, userId);
        return Result.success("发布成功", discussionId);
    }
    
    /**
     * 更新讨论
     */
    @PutMapping("/discussion/{discussionId}")
    public Result<Boolean> updateDiscussion(@PathVariable @Positive(message = "讨论ID必须大于0") Long discussionId,
                                            @Valid @RequestBody DiscussionCreateRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        discussionService.updateDiscussion(discussionId, request, userId);
        return Result.success("更新成功", true);
    }
    
    /**
     * 删除讨论
     */
    @DeleteMapping("/discussion/{discussionId}")
    public Result<Boolean> deleteDiscussion(@PathVariable @Positive(message = "讨论ID必须大于0") Long discussionId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        discussionService.deleteDiscussion(discussionId, userId);
        return Result.success("删除成功", true);
    }
    
    /**
     * 获取讨论详情
     */
    @GetMapping("/discussion/{discussionId}")
    public Result<DiscussionResponse> getDiscussionDetail(@PathVariable @Positive(message = "讨论ID必须大于0") Long discussionId) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        DiscussionResponse response = discussionService.getDiscussionDetail(discussionId, userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取讨论列表
     */
    @GetMapping("/{teamId}/discussions")
    public Result<List<DiscussionResponse>> getDiscussionList(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                                              @RequestParam(required = false) @Min(value = 1, message = "讨论分类不合法")
                                                              @Max(value = 5, message = "讨论分类不合法") Integer category,
                                                              @RequestParam(required = false) @Pattern(regexp = "^.{0,100}$", message = "关键字不能超过100个字符") String keyword,
                                                              @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
                                                              @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1")
                                                              @Max(value = 100, message = "每页条数最大为100") Integer pageSize) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        List<DiscussionResponse> response = discussionService.getDiscussionList(teamId, category, keyword, page, pageSize, userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 置顶/取消置顶
     */
    @PutMapping("/discussion/{discussionId}/top")
    public Result<Boolean> setDiscussionTop(@PathVariable @Positive(message = "讨论ID必须大于0") Long discussionId,
                                            @RequestParam @Min(value = 0, message = "置顶状态不合法")
                                            @Max(value = 1, message = "置顶状态不合法") Integer isTop) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        discussionService.setTop(discussionId, isTop, userId);
        return Result.success("操作成功", true);
    }
    
    /**
     * 设为精华/取消精华
     */
    @PutMapping("/discussion/{discussionId}/essence")
    public Result<Boolean> setDiscussionEssence(@PathVariable @Positive(message = "讨论ID必须大于0") Long discussionId,
                                                @RequestParam @Min(value = 0, message = "精华状态不合法")
                                                @Max(value = 1, message = "精华状态不合法") Integer isEssence) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        discussionService.setEssence(discussionId, isEssence, userId);
        return Result.success("操作成功", true);
    }
    
    /**
     * 点赞讨论
     */
    @PostMapping("/discussion/{discussionId}/like")
    public Result<Boolean> likeDiscussion(@PathVariable @Positive(message = "讨论ID必须大于0") Long discussionId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        discussionService.likeDiscussion(discussionId, userId);
        return Result.success("点赞成功", true);
    }
    
    /**
     * 取消点赞讨论
     */
    @DeleteMapping("/discussion/{discussionId}/like")
    public Result<Boolean> unlikeDiscussion(@PathVariable @Positive(message = "讨论ID必须大于0") Long discussionId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        discussionService.unlikeDiscussion(discussionId, userId);
        return Result.success("取消点赞成功", true);
    }
    
    // ==================== 统计接口 ====================
    
    /**
     * 获取小组统计概览
     */
    @GetMapping("/{teamId}/stats")
    public Result<TeamStatsResponse> getTeamStats(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        Long userId = null;
        if (StpUserUtil.isLogin()) {
            userId = StpUserUtil.getLoginIdAsLong();
        }
        TeamStatsResponse response = statsService.getTeamStats(teamId, userId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取小组每周统计
     */
    @GetMapping("/{teamId}/stats/weekly")
    public Result<TeamStatsResponse> getWeeklyStats(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        TeamStatsResponse response = statsService.getWeeklyStats(teamId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取小组每月统计
     */
    @GetMapping("/{teamId}/stats/monthly")
    public Result<TeamStatsResponse> getMonthlyStats(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId,
                                                     @RequestParam @Min(value = 2000, message = "年份不合法")
                                                     @Max(value = 2100, message = "年份不合法") Integer year,
                                                     @RequestParam @Min(value = 1, message = "月份不合法")
                                                     @Max(value = 12, message = "月份不合法") Integer month) {
        TeamStatsResponse response = statsService.getMonthlyStats(teamId, year, month);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取用户个人统计
     */
    @GetMapping("/{teamId}/stats/my")
    public Result<TeamStatsResponse.UserStats> getMyStats(@PathVariable @Positive(message = "小组ID必须大于0") Long teamId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        TeamStatsResponse.UserStats response = statsService.getUserStats(teamId, userId);
        return Result.success("获取成功", response);
    }
}
