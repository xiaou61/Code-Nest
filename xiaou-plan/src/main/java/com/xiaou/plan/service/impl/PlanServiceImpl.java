package com.xiaou.plan.service.impl;

import cn.hutool.core.util.StrUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.plan.domain.PlanCheckinRecord;
import com.xiaou.plan.domain.UserPlan;
import com.xiaou.plan.dto.*;
import com.xiaou.plan.enums.PlanStatus;
import com.xiaou.plan.enums.PlanType;
import com.xiaou.plan.enums.RepeatType;
import com.xiaou.plan.mapper.PlanCheckinRecordMapper;
import com.xiaou.plan.mapper.UserPlanMapper;
import com.xiaou.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 计划服务实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {
    
    private final UserPlanMapper planMapper;
    private final PlanCheckinRecordMapper checkinRecordMapper;
    
    /**
     * 计划打卡积分
     */
    private static final int CHECKIN_POINTS = 10;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanResponse createPlan(Long userId, PlanCreateRequest request) {
        // 参数校验
        if (StrUtil.isBlank(request.getPlanName())) {
            throw new BusinessException("计划名称不能为空");
        }
        if (request.getPlanType() == null) {
            throw new BusinessException("计划类型不能为空");
        }
        if (request.getStartDate() == null) {
            throw new BusinessException("开始日期不能为空");
        }
        
        // 构建计划实体
        UserPlan plan = new UserPlan();
        plan.setUserId(userId);
        plan.setPlanName(request.getPlanName());
        plan.setPlanDesc(request.getPlanDesc());
        plan.setPlanType(normalizePlanTypeCode(request.getPlanType()));
        plan.setTargetType(request.getTargetType() != null ? request.getTargetType() : 1);
        plan.setTargetValue(request.getTargetValue() != null ? request.getTargetValue() : 1);
        plan.setTargetUnit(request.getTargetUnit() != null ? request.getTargetUnit() : "次");
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setDailyStartTime(request.getDailyStartTime());
        plan.setDailyEndTime(request.getDailyEndTime());
        plan.setRepeatType(normalizeRepeatTypeCode(request.getRepeatType(), request.getRepeatDays()));
        plan.setRepeatDays(request.getRepeatDays());
        plan.setRemindBefore(request.getRemindBefore() != null ? request.getRemindBefore() : 30);
        plan.setRemindDeadline(request.getRemindDeadline() != null ? request.getRemindDeadline() : 10);
        plan.setRemindEnabled(request.getRemindEnabled() != null ? request.getRemindEnabled() : 1);
        plan.setStatus(PlanStatus.ACTIVE.getCode());
        plan.setTotalCheckinDays(0);
        plan.setCurrentStreak(0);
        plan.setMaxStreak(0);
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        
        planMapper.insert(plan);
        log.info("用户{}创建计划成功，计划ID：{}", userId, plan.getId());
        
        return convertToPlanResponse(plan, LocalDate.now());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanResponse updatePlan(Long userId, Long planId, PlanCreateRequest request) {
        UserPlan plan = planMapper.selectByUserIdAndId(userId, planId);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        
        // 更新字段
        if (StrUtil.isNotBlank(request.getPlanName())) {
            plan.setPlanName(request.getPlanName());
        }
        plan.setPlanDesc(request.getPlanDesc());
        if (request.getPlanType() != null) {
            plan.setPlanType(normalizePlanTypeCode(request.getPlanType()));
        }
        if (request.getTargetType() != null) {
            plan.setTargetType(request.getTargetType());
        }
        if (request.getTargetValue() != null) {
            plan.setTargetValue(request.getTargetValue());
        }
        if (request.getTargetUnit() != null) {
            plan.setTargetUnit(request.getTargetUnit());
        }
        if (request.getStartDate() != null) {
            plan.setStartDate(request.getStartDate());
        }
        plan.setEndDate(request.getEndDate());
        plan.setDailyStartTime(request.getDailyStartTime());
        plan.setDailyEndTime(request.getDailyEndTime());
        if (request.getRepeatType() != null) {
            plan.setRepeatType(normalizeRepeatTypeCode(request.getRepeatType(), request.getRepeatDays()));
        }
        plan.setRepeatDays(request.getRepeatDays());
        if (request.getRemindBefore() != null) {
            plan.setRemindBefore(request.getRemindBefore());
        }
        if (request.getRemindDeadline() != null) {
            plan.setRemindDeadline(request.getRemindDeadline());
        }
        if (request.getRemindEnabled() != null) {
            plan.setRemindEnabled(request.getRemindEnabled());
        }
        
        planMapper.update(plan);
        log.info("用户{}更新计划成功，计划ID：{}", userId, planId);
        
        return convertToPlanResponse(plan, LocalDate.now());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePlan(Long userId, Long planId) {
        int result = planMapper.deleteById(planId, userId);
        if (result > 0) {
            log.info("用户{}删除计划成功，计划ID：{}", userId, planId);
            return true;
        }
        return false;
    }
    
    @Override
    public PlanResponse getPlanDetail(Long userId, Long planId) {
        UserPlan plan = planMapper.selectByUserIdAndId(userId, planId);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        return convertToPlanResponse(plan, LocalDate.now());
    }
    
    @Override
    public PageResult<PlanResponse> getPlanList(PlanListRequest request) {
        LocalDate today = LocalDate.now();
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            List<UserPlan> plans = planMapper.selectList(request);
            return plans.stream()
                    .map(plan -> convertToPlanResponse(plan, today))
                    .collect(Collectors.toList());
        });
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pausePlan(Long userId, Long planId) {
        UserPlan plan = planMapper.selectByUserIdAndId(userId, planId);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        if (plan.getStatus() != PlanStatus.ACTIVE.getCode()) {
            throw new BusinessException("只有进行中的计划可以暂停");
        }
        
        planMapper.updateStatus(planId, PlanStatus.PAUSED.getCode());
        log.info("用户{}暂停计划，计划ID：{}", userId, planId);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resumePlan(Long userId, Long planId) {
        UserPlan plan = planMapper.selectByUserIdAndId(userId, planId);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        if (plan.getStatus() != PlanStatus.PAUSED.getCode()) {
            throw new BusinessException("只有已暂停的计划可以恢复");
        }
        
        planMapper.updateStatus(planId, PlanStatus.ACTIVE.getCode());
        log.info("用户{}恢复计划，计划ID：{}", userId, planId);
        return true;
    }
    
    @Override
    public List<TodayTaskResponse> getTodayTasks(Long userId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        List<UserPlan> plans = planMapper.selectTodayPlans(userId, today);
        List<TodayTaskResponse> tasks = new ArrayList<>();
        
        for (UserPlan plan : plans) {
            // 检查今天是否需要打卡（根据重复规则）
            if (!shouldCheckinToday(plan, today)) {
                continue;
            }
            
            // 检查今天是否已打卡
            PlanCheckinRecord record = checkinRecordMapper.selectByPlanIdAndDate(plan.getId(), today);
            boolean checkedIn = record != null;
            
            // 计算任务状态
            int taskStatus = 0; // 待完成
            String taskStatusDesc = "待完成";
            if (checkedIn) {
                taskStatus = 1;
                taskStatusDesc = "已完成";
            } else if (plan.getDailyEndTime() != null && now.isAfter(plan.getDailyEndTime())) {
                taskStatus = 2;
                taskStatusDesc = "已过期";
            }
            
            // 计算剩余时间
            Long remainingMinutes = null;
            String remainingTimeDesc = "";
            if (!checkedIn && plan.getDailyEndTime() != null && now.isBefore(plan.getDailyEndTime())) {
                remainingMinutes = now.until(plan.getDailyEndTime(), ChronoUnit.MINUTES);
                if (remainingMinutes >= 60) {
                    remainingTimeDesc = "剩余 " + (remainingMinutes / 60) + "小时" + (remainingMinutes % 60) + "分";
                } else {
                    remainingTimeDesc = "剩余 " + remainingMinutes + "分钟";
                }
            }
            
            Integer normalizedPlanType = normalizePlanTypeCode(plan.getPlanType());
            PlanType planType = PlanType.fromCode(normalizedPlanType);
            
            tasks.add(TodayTaskResponse.builder()
                    .planId(plan.getId())
                    .planName(plan.getPlanName())
                    .planType(normalizedPlanType)
                    .planTypeDesc(planType.getDesc())
                    .planTypeIcon(planType.getIcon())
                    .targetValue(plan.getTargetValue())
                    .targetUnit(plan.getTargetUnit())
                    .dailyStartTime(plan.getDailyStartTime())
                    .dailyEndTime(plan.getDailyEndTime())
                    .taskStatus(taskStatus)
                    .taskStatusDesc(taskStatusDesc)
                    .checkedIn(checkedIn)
                    .remainingMinutes(remainingMinutes)
                    .remainingTimeDesc(remainingTimeDesc)
                    .currentStreak(plan.getCurrentStreak())
                    .build());
        }
        
        return tasks;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanCheckinResponse checkin(Long userId, PlanCheckinRequest request) {
        LocalDate today = LocalDate.now();
        
        // 查询计划
        UserPlan plan = planMapper.selectByUserIdAndId(userId, request.getPlanId());
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        if (plan.getStatus() != PlanStatus.ACTIVE.getCode()) {
            throw new BusinessException("计划未在进行中，无法打卡");
        }
        
        // 检查今天是否已打卡
        PlanCheckinRecord existingRecord = checkinRecordMapper.selectByPlanIdAndDate(plan.getId(), today);
        if (existingRecord != null) {
            throw new BusinessException("今日已打卡，请勿重复操作");
        }
        
        // 检查今天是否需要打卡
        if (!shouldCheckinToday(plan, today)) {
            throw new BusinessException("今日不需要打卡");
        }
        
        // 计算连续打卡天数
        int newStreak = calculateNewStreak(plan, today);
        int newMaxStreak = Math.max(plan.getMaxStreak(), newStreak);
        int newTotalDays = plan.getTotalCheckinDays() + 1;
        
        // 创建打卡记录
        PlanCheckinRecord record = new PlanCheckinRecord();
        record.setPlanId(plan.getId());
        record.setUserId(userId);
        record.setCheckinDate(today);
        record.setCheckinTime(LocalDateTime.now());
        record.setCompleteValue(request.getCompleteValue());
        record.setCompleteContent(request.getCompleteContent());
        record.setRemark(request.getRemark());
        record.setIsSupplement(0);
        record.setPointsEarned(CHECKIN_POINTS);
        record.setCreateTime(LocalDateTime.now());
        
        checkinRecordMapper.insert(record);
        
        // 更新计划统计
        planMapper.updateCheckinStats(plan.getId(), newTotalDays, newStreak, newMaxStreak);
        
        log.info("用户{}打卡成功，计划ID：{}，连续{}天", userId, plan.getId(), newStreak);
        
        return PlanCheckinResponse.builder()
                .recordId(record.getId())
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .pointsEarned(CHECKIN_POINTS)
                .currentStreak(newStreak)
                .totalCheckinDays(newTotalDays)
                .message("打卡成功！连续打卡 " + newStreak + " 天")
                .build();
    }
    
    @Override
    public List<PlanCheckinRecord> getCheckinRecords(Long userId, Long planId) {
        // 验证计划所属用户
        UserPlan plan = planMapper.selectByUserIdAndId(userId, planId);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        return checkinRecordMapper.selectByPlanId(planId);
    }
    
    @Override
    public PlanStatsResponse getStatsOverview(Long userId) {
        LocalDate today = LocalDate.now();
        
        // 统计进行中计划数
        int activePlanCount = planMapper.countByUserId(userId, PlanStatus.ACTIVE.getCode());
        
        // 统计累计打卡次数
        int totalCheckinCount = checkinRecordMapper.countByUserId(userId);
        
        // 获取所有计划计算最长连续天数
        List<UserPlan> plans = planMapper.selectActivePlans(userId);
        int maxStreak = plans.stream()
                .mapToInt(UserPlan::getMaxStreak)
                .max()
                .orElse(0);
        
        // 获取今日任务统计
        List<TodayTaskResponse> todayTasks = getTodayTasks(userId);
        int todayCompletedCount = (int) todayTasks.stream().filter(TodayTaskResponse::getCheckedIn).count();
        int todayPendingCount = todayTasks.size() - todayCompletedCount;
        
        return PlanStatsResponse.builder()
                .activePlanCount(activePlanCount)
                .totalCheckinCount(totalCheckinCount)
                .avgCompletionRate(0.0) // 简化处理
                .maxStreak(maxStreak)
                .weekCheckinCount(0) // 简化处理
                .monthCheckinCount(0) // 简化处理
                .todayCompletedCount(todayCompletedCount)
                .todayPendingCount(todayPendingCount)
                .build();
    }
    
    /**
     * 判断今天是否需要打卡
     */
    private boolean shouldCheckinToday(UserPlan plan, LocalDate today) {
        int repeatType = normalizeRepeatTypeCode(plan.getRepeatType(), plan.getRepeatDays());
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        
        switch (repeatType) {
            case 1: // 每日
                return true;
            case 2: // 工作日
                return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
            case 3: // 周末
                return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
            case 4: // 自定义
                if (StrUtil.isBlank(plan.getRepeatDays())) {
                    return true;
                }
                int todayValue = dayOfWeek.getValue(); // 1=周一, 7=周日
                return Arrays.stream(plan.getRepeatDays().split(","))
                        .map(String::trim)
                        .anyMatch(d -> d.equals(String.valueOf(todayValue)));
            default:
                return true;
        }
    }
    
    /**
     * 计算新的连续打卡天数
     */
    private int calculateNewStreak(UserPlan plan, LocalDate today) {
        PlanCheckinRecord lastRecord = checkinRecordMapper.selectLatestByPlanId(plan.getId());
        if (lastRecord == null) {
            return 1; // 首次打卡
        }
        
        LocalDate lastCheckinDate = lastRecord.getCheckinDate();
        
        // 检查是否连续（考虑重复规则）
        LocalDate checkDate = today.minusDays(1);
        while (!checkDate.isBefore(lastCheckinDate)) {
            if (shouldCheckinToday(plan, checkDate)) {
                // 这一天需要打卡
                if (checkDate.equals(lastCheckinDate)) {
                    // 上次打卡就是这天，连续
                    return plan.getCurrentStreak() + 1;
                } else {
                    // 中间漏了需要打卡的日子
                    return 1;
                }
            }
            checkDate = checkDate.minusDays(1);
        }
        
        // 如果走到这里说明两次打卡之间没有需要打卡的日子，视为连续
        return plan.getCurrentStreak() + 1;
    }
    
    /**
     * 转换为响应DTO
     */
    private PlanResponse convertToPlanResponse(UserPlan plan, LocalDate today) {
        Integer normalizedPlanType = normalizePlanTypeCode(plan.getPlanType());
        Integer normalizedRepeatType = normalizeRepeatTypeCode(plan.getRepeatType(), plan.getRepeatDays());
        PlanType planType = PlanType.fromCode(normalizedPlanType);
        PlanStatus status = PlanStatus.fromCode(plan.getStatus());
        RepeatType repeatType = RepeatType.fromCode(normalizedRepeatType);
        
        // 检查今天是否已打卡
        PlanCheckinRecord todayRecord = checkinRecordMapper.selectByPlanIdAndDate(plan.getId(), today);
        boolean todayCheckedIn = todayRecord != null;
        
        // 检查今天是否需要打卡
        boolean todayNeedCheckin = plan.getStatus() == PlanStatus.ACTIVE.getCode() 
                && shouldCheckinToday(plan, today)
                && (plan.getEndDate() == null || !today.isAfter(plan.getEndDate()));
        
        // 计算完成率
        Double completionRate = 0.0;
        if (plan.getStartDate() != null) {
            LocalDate endDate = plan.getEndDate() != null ? plan.getEndDate() : today;
            long totalDays = ChronoUnit.DAYS.between(plan.getStartDate(), endDate) + 1;
            if (totalDays > 0 && plan.getTotalCheckinDays() != null) {
                completionRate = (double) plan.getTotalCheckinDays() / totalDays * 100;
            }
        }
        
        return PlanResponse.builder()
                .id(plan.getId())
                .userId(plan.getUserId())
                .planName(plan.getPlanName())
                .planDesc(plan.getPlanDesc())
                .planType(normalizedPlanType)
                .planTypeDesc(planType.getDesc())
                .planTypeIcon(planType.getIcon())
                .targetType(plan.getTargetType())
                .targetValue(plan.getTargetValue())
                .targetUnit(plan.getTargetUnit())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .dailyStartTime(plan.getDailyStartTime())
                .dailyEndTime(plan.getDailyEndTime())
                .repeatType(normalizedRepeatType)
                .repeatTypeDesc(repeatType.getDesc())
                .repeatDays(plan.getRepeatDays())
                .remindBefore(plan.getRemindBefore())
                .remindDeadline(plan.getRemindDeadline())
                .remindEnabled(plan.getRemindEnabled())
                .status(plan.getStatus())
                .statusDesc(status.getDesc())
                .totalCheckinDays(plan.getTotalCheckinDays())
                .currentStreak(plan.getCurrentStreak())
                .maxStreak(plan.getMaxStreak())
                .completionRate(completionRate)
                .todayCheckedIn(todayCheckedIn)
                .todayNeedCheckin(todayNeedCheckin)
                .createTime(plan.getCreateTime())
                .updateTime(plan.getUpdateTime())
                .build();
    }

    private Integer normalizePlanTypeCode(Integer planType) {
        if (planType == null) {
            return null;
        }
        return planType == 99 ? PlanType.CUSTOM.getCode() : planType;
    }

    private Integer normalizeRepeatTypeCode(Integer repeatType, String repeatDays) {
        if (repeatType == null) {
            return null;
        }
        if (repeatType == 3 && StrUtil.isNotBlank(repeatDays)) {
            return RepeatType.CUSTOM.getCode();
        }
        return repeatType;
    }
}
