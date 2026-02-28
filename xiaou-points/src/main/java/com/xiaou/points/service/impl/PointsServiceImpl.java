package com.xiaou.points.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.points.domain.UserCheckinBitmap;
import com.xiaou.points.domain.UserPointsBalance;
import com.xiaou.points.domain.UserPointsDetail;
import com.xiaou.points.dto.*;
import com.xiaou.points.enums.PointsType;
import com.xiaou.points.mapper.UserCheckinBitmapMapper;
import com.xiaou.points.mapper.UserPointsBalanceMapper;
import com.xiaou.points.mapper.UserPointsDetailMapper;
import com.xiaou.points.service.PointsService;
import com.xiaou.points.utils.CheckinBitmapUtil;
import com.xiaou.points.utils.CheckinPointsCalculator;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 积分服务实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {
    
    private final UserPointsBalanceMapper pointsBalanceMapper;
    private final UserPointsDetailMapper pointsDetailMapper;
    private final UserCheckinBitmapMapper checkinBitmapMapper;
    private final UserInfoApiService userInfoApiService;
    
    /**
     * 积分汇率：1000积分 = 1元
     */
    private static final int POINTS_TO_YUAN_RATE = 1000;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinResponse checkin(Long userId) {
        LocalDate today = LocalDate.now();
        String yearMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        int dayOfMonth = today.getDayOfMonth();
        
        log.info("用户{}开始打卡，日期：{}", userId, today);
        
        // 1. 获取或创建当月位图记录
        UserCheckinBitmap bitmapRecord = checkinBitmapMapper.selectByUserIdAndYearMonth(userId, yearMonth);
        if (bitmapRecord == null) {
            bitmapRecord = createNewBitmapRecord(userId, yearMonth);
        }
        
        // 2. 检查今天是否已打卡
        if (CheckinBitmapUtil.isCheckedIn(bitmapRecord.getCheckinBitmap(), dayOfMonth)) {
            throw new BusinessException("今日已打卡，请勿重复操作");
        }
        
        // 3. 计算连续打卡天数
        int continuousDays = calculateContinuousDays(userId, today, bitmapRecord);
        
        // 4. 计算积分奖励
        int pointsEarned = CheckinPointsCalculator.calculatePoints(continuousDays);
        
        // 5. 更新位图记录
        long newBitmap = CheckinBitmapUtil.setBit(bitmapRecord.getCheckinBitmap(), dayOfMonth);
        bitmapRecord.setCheckinBitmap(newBitmap);
        bitmapRecord.setContinuousDays(continuousDays);
        bitmapRecord.setLastCheckinDate(today);
        bitmapRecord.setTotalCheckinDays(CheckinBitmapUtil.countCheckinDays(newBitmap));
        bitmapRecord.setUpdateTime(LocalDateTime.now());
        
        checkinBitmapMapper.updateBitmap(bitmapRecord);
        
        // 6. 更新积分余额
        ensureUserPointsBalance(userId);
        pointsBalanceMapper.addPoints(userId, pointsEarned);
        
        // 7. 创建积分明细记录
        UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);
        UserPointsDetail detail = new UserPointsDetail();
        detail.setUserId(userId);
        detail.setPointsChange(pointsEarned);
        detail.setPointsType(PointsType.CHECK_IN.getCode());
        detail.setDescription("连续第" + continuousDays + "天打卡");
        detail.setBalanceAfter(balance.getTotalPoints());
        detail.setContinuousDays(continuousDays);
        detail.setCreateTime(LocalDateTime.now());
        
        pointsDetailMapper.insert(detail);
        
        log.info("用户{}打卡成功，获得{}积分，连续{}天", userId, pointsEarned, continuousDays);
        
        // 8. 构建响应
        return CheckinResponse.builder()
                .pointsEarned(pointsEarned)
                .continuousDays(continuousDays)
                .nextDayPoints(CheckinPointsCalculator.calculateNextDayPoints(continuousDays))
                .totalBalance(balance.getTotalPoints())
                .balanceYuan(formatPointsToYuan(balance.getTotalPoints()))
                .description("连续第" + continuousDays + "天打卡")
                .isWeekBonusDay(CheckinPointsCalculator.isWeekBonusDay(continuousDays))
                .build();
    }
    
    @Override
    public PointsBalanceResponse getPointsBalance(Long userId) {
        // 确保用户有积分账户
        ensureUserPointsBalance(userId);
        
        UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);
        LocalDate today = LocalDate.now();
        
        // 检查今日打卡状态
        boolean todayCheckedIn = isTodayCheckedIn(userId, today);
        
        // 获取连续打卡天数
        int continuousDays = getCurrentContinuousDays(userId, today);
        
        return PointsBalanceResponse.builder()
                .userId(userId)
                .totalPoints(balance.getTotalPoints())
                .balanceYuan(formatPointsToYuan(balance.getTotalPoints()))
                .todayCheckedIn(todayCheckedIn)
                .continuousDays(continuousDays)
                .todayPoints(todayCheckedIn ? 0 : CheckinPointsCalculator.calculatePoints(continuousDays + 1))
                .nextDayPoints(CheckinPointsCalculator.calculateNextDayPoints(continuousDays + (todayCheckedIn ? 0 : 1)))
                .build();
    }
    
    @Override
    public PageResult<PointsDetailResponse> getPointsDetailList(PointsDetailQueryRequest request) {
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            List<UserPointsDetail> details = pointsDetailMapper.selectDetailList(request);
            return details.stream().map(this::convertToDetailResponse).collect(Collectors.toList());
        });
    }
    
    @Override
    public CheckinCalendarResponse getCheckinCalendar(Long userId, String yearMonth) {
        if (StrUtil.isBlank(yearMonth)) {
            yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        
        UserCheckinBitmap bitmap = checkinBitmapMapper.selectByUserIdAndYearMonth(userId, yearMonth);
        if (bitmap == null) {
            // 返回空日历
            return CheckinCalendarResponse.builder()
                    .yearMonth(yearMonth)
                    .checkinBitmap(0L)
                    .checkinDays(new ArrayList<>())
                    .totalCheckinDays(0)
                    .continuousDays(0)
                    .monthlyStats(CheckinCalendarResponse.MonthlyStats.builder()
                            .totalDays(LocalDate.parse(yearMonth + "-01").lengthOfMonth())
                            .checkinRate("0.00%")
                            .pointsEarned(0)
                            .build())
                    .calendarGrid(new HashMap<>())
                    .build();
        }
        
        List<Integer> checkinDays = CheckinBitmapUtil.getCheckinDays(bitmap.getCheckinBitmap());
        int totalDaysInMonth = LocalDate.parse(yearMonth + "-01").lengthOfMonth();
        String checkinRate = String.format("%.2f%%", (double) checkinDays.size() / totalDaysInMonth * 100);
        
        // 计算当月获得积分（需要查询积分明细）
        int pointsEarned = calculateMonthlyPointsEarned(userId, yearMonth);
        
        // 构建日历网格
        Map<String, CheckinCalendarResponse.DayInfo> calendarGrid = new HashMap<>();
        for (int day = 1; day <= totalDaysInMonth; day++) {
            boolean checked = CheckinBitmapUtil.isCheckedIn(bitmap.getCheckinBitmap(), day);
            calendarGrid.put(String.valueOf(day), CheckinCalendarResponse.DayInfo.builder()
                    .checked(checked)
                    .points(checked ? 50 : 0) // 简化处理，具体积分需要根据历史记录计算
                    .build());
        }
        
        return CheckinCalendarResponse.builder()
                .yearMonth(yearMonth)
                .checkinBitmap(bitmap.getCheckinBitmap())
                .checkinDays(checkinDays)
                .totalCheckinDays(bitmap.getTotalCheckinDays())
                .continuousDays(bitmap.getContinuousDays())
                .lastCheckinDate(bitmap.getLastCheckinDate())
                .monthlyStats(CheckinCalendarResponse.MonthlyStats.builder()
                        .totalDays(totalDaysInMonth)
                        .checkinRate(checkinRate)
                        .pointsEarned(pointsEarned)
                        .build())
                .calendarGrid(calendarGrid)
                .build();
    }
    
    @Override
    public CheckinStatisticsResponse getCheckinStatistics(Long userId, Integer months) {
        if (months == null || months <= 0) {
            months = 3;
        }
        
        // 获取用户所有打卡记录
        List<UserCheckinBitmap> bitmaps = checkinBitmapMapper.selectByUserId(userId);
        
        int totalCheckinDays = bitmaps.stream().mapToInt(UserCheckinBitmap::getTotalCheckinDays).sum();
        
        // 获取当前连续打卡天数
        int currentContinuousDays = getCurrentContinuousDays(userId, LocalDate.now());
        
        // 计算历史最大连续打卡天数（简化处理）
        int maxContinuousDays = bitmaps.stream().mapToInt(UserCheckinBitmap::getContinuousDays).max().orElse(0);
        
        // 计算打卡获得的总积分
        Integer totalPointsFromCheckin = calculateTotalCheckinPoints(userId);
        
        double averagePointsPerDay = totalCheckinDays > 0 ? (double) totalPointsFromCheckin / totalCheckinDays : 0;
        
        // 构建月度统计（取最近几个月）
        List<CheckinStatisticsResponse.MonthlyCheckinStats> monthlyStats = bitmaps.stream()
                .limit(months)
                .map(bitmap -> {
                    int monthlyPoints = calculateMonthlyPointsEarned(userId, bitmap.getYearMonth());
                    int totalDaysInMonth = LocalDate.parse(bitmap.getYearMonth() + "-01").lengthOfMonth();
                    String rate = String.format("%.2f%%", (double) bitmap.getTotalCheckinDays() / totalDaysInMonth * 100);
                    
                    return CheckinStatisticsResponse.MonthlyCheckinStats.builder()
                            .yearMonth(bitmap.getYearMonth())
                            .checkinDays(bitmap.getTotalCheckinDays())
                            .pointsEarned(monthlyPoints)
                            .checkinRate(rate)
                            .build();
                })
                .collect(Collectors.toList());
        
        return CheckinStatisticsResponse.builder()
                .totalCheckinDays(totalCheckinDays)
                .currentContinuousDays(currentContinuousDays)
                .maxContinuousDays(maxContinuousDays)
                .totalPointsFromCheckin(totalPointsFromCheckin)
                .averagePointsPerDay(averagePointsPerDay)
                .monthlyStats(monthlyStats)
                .build();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminGrantPointsResponse grantPoints(AdminGrantPointsRequest request, Long adminId) {
        log.info("管理员{}为用户{}发放{}积分，原因：{}", adminId, request.getUserId(), request.getPoints(), request.getReason());
        
        // 确保用户有积分账户
        ensureUserPointsBalance(request.getUserId());
        
        // 更新积分余额
        pointsBalanceMapper.addPoints(request.getUserId(), request.getPoints());
        
        // 获取更新后的余额
        UserPointsBalance balance = pointsBalanceMapper.selectByUserId(request.getUserId());
        
        // 创建积分明细记录
        UserPointsDetail detail = new UserPointsDetail();
        detail.setUserId(request.getUserId());
        detail.setPointsChange(request.getPoints());
        detail.setPointsType(PointsType.ADMIN_GRANT.getCode());
        detail.setDescription(request.getReason());
        detail.setBalanceAfter(balance.getTotalPoints());
        detail.setAdminId(adminId);
        detail.setCreateTime(LocalDateTime.now());
        
        pointsDetailMapper.insert(detail);
        
        log.info("积分发放成功，用户{}当前余额：{}", request.getUserId(), balance.getTotalPoints());
        
        return AdminGrantPointsResponse.builder()
                .detailId(detail.getId())
                .userBalance(balance.getTotalPoints())
                .balanceYuan(formatPointsToYuan(balance.getTotalPoints()))
                .userName("用户" + request.getUserId()) // 简化处理，实际应该查询用户表
                .build();
    }
    
    @Override
    public PageResult<PointsDetailResponse> getAllPointsDetailList(PointsDetailQueryRequest request) {
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            List<UserPointsDetail> details = pointsDetailMapper.selectAllDetailList(request);
            return details.stream().map(this::convertToDetailResponse).collect(Collectors.toList());
        });
    }
    
    @Override
    public AdminPointsStatisticsResponse getAdminStatistics() {
        // 获取统计数据
        Long totalPointsIssued = pointsBalanceMapper.selectTotalPointsSum();
        Long checkinPointsSum = pointsDetailMapper.selectPointsSumByType(PointsType.CHECK_IN.getCode());
        Long adminGrantPointsSum = pointsDetailMapper.selectPointsSumByType(PointsType.ADMIN_GRANT.getCode());
        Integer activeUserCount = pointsBalanceMapper.selectActiveUserCount();
        Integer checkinActiveUserCount = checkinBitmapMapper.selectActiveCheckinUserCount();
        
        // 获取积分排行榜
        List<UserPointsBalance> topUsers = pointsBalanceMapper.selectTopUsers(20);
        List<AdminPointsStatisticsResponse.UserPointsRanking> rankings = new ArrayList<>();
        for (int i = 0; i < topUsers.size(); i++) {
            UserPointsBalance user = topUsers.get(i);
            int continuousDays = getCurrentContinuousDays(user.getUserId(), LocalDate.now());
            rankings.add(AdminPointsStatisticsResponse.UserPointsRanking.builder()
                    .userId(user.getUserId())
                    .userName("用户" + user.getUserId()) // 简化处理
                    .totalPoints(user.getTotalPoints())
                    .balanceYuan(formatPointsToYuan(user.getTotalPoints()))
                    .continuousDays(continuousDays)
                    .ranking(i + 1)
                    .build());
        }
        
        // 构建趋势数据（简化处理）
        List<AdminPointsStatisticsResponse.DailyPointsTrend> trends = new ArrayList<>();
        
        return AdminPointsStatisticsResponse.builder()
                .totalPointsIssued(totalPointsIssued != null ? totalPointsIssued : 0)
                .checkinPointsSum(checkinPointsSum != null ? checkinPointsSum : 0)
                .adminGrantPointsSum(adminGrantPointsSum != null ? adminGrantPointsSum : 0)
                .activeUserCount(activeUserCount != null ? activeUserCount : 0)
                .checkinActiveUserCount(checkinActiveUserCount != null ? checkinActiveUserCount : 0)
                .userRankings(rankings)
                .dailyTrends(trends)
                .build();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPointsAccountForNewUser(Long userId) {
        // 检查是否已存在
        UserPointsBalance existing = pointsBalanceMapper.selectByUserId(userId);
        if (existing != null) {
            return;
        }
        
        UserPointsBalance balance = new UserPointsBalance();
        balance.setUserId(userId);
        balance.setTotalPoints(0);
        balance.setCreateTime(LocalDateTime.now());
        balance.setUpdateTime(LocalDateTime.now());
        
        pointsBalanceMapper.insert(balance);
        log.info("为新用户{}创建积分账户成功", userId);
    }
    
    /**
     * 确保用户有积分余额记录
     */
    private void ensureUserPointsBalance(Long userId) {
        UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);
        if (balance == null) {
            createPointsAccountForNewUser(userId);
        }
    }
    
    /**
     * 创建新的位图记录
     */
    private UserCheckinBitmap createNewBitmapRecord(Long userId, String yearMonth) {
        UserCheckinBitmap bitmap = new UserCheckinBitmap();
        bitmap.setUserId(userId);
        bitmap.setYearMonth(yearMonth);
        bitmap.setCheckinBitmap(0L);
        bitmap.setContinuousDays(0);
        bitmap.setTotalCheckinDays(0);
        bitmap.setCreateTime(LocalDateTime.now());
        bitmap.setUpdateTime(LocalDateTime.now());
        
        checkinBitmapMapper.insert(bitmap);
        return bitmap;
    }
    
    /**
     * 计算连续打卡天数
     */
    private int calculateContinuousDays(Long userId, LocalDate today, UserCheckinBitmap currentRecord) {
        LocalDate yesterday = today.minusDays(1);
        
        if (currentRecord.getLastCheckinDate() == null) {
            return 1; // 首次打卡
        }
        
        if (currentRecord.getLastCheckinDate().equals(yesterday)) {
            return currentRecord.getContinuousDays() + 1; // 连续打卡
        } else {
            return 1; // 中断后重新开始
        }
    }
    
    /**
     * 检查今日是否已打卡
     */
    private boolean isTodayCheckedIn(Long userId, LocalDate today) {
        String yearMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        UserCheckinBitmap bitmap = checkinBitmapMapper.selectByUserIdAndYearMonth(userId, yearMonth);
        
        if (bitmap == null) {
            return false;
        }
        
        return CheckinBitmapUtil.isCheckedIn(bitmap.getCheckinBitmap(), today.getDayOfMonth());
    }
    
    /**
     * 获取当前连续打卡天数
     */
    private int getCurrentContinuousDays(Long userId, LocalDate today) {
        UserCheckinBitmap latest = checkinBitmapMapper.selectLatestByUserId(userId);
        if (latest == null || latest.getLastCheckinDate() == null) {
            return 0;
        }
        
        // 如果最后打卡日期是今天或昨天，返回连续天数
        if (latest.getLastCheckinDate().equals(today) || 
            latest.getLastCheckinDate().equals(today.minusDays(1))) {
            return latest.getContinuousDays();
        }
        
        return 0; // 连续记录已中断
    }
    
    /**
     * 计算指定月份获得的积分
     */
    private int calculateMonthlyPointsEarned(Long userId, String yearMonth) {
        String startTime = yearMonth + "-01 00:00:00";
        String endTime = yearMonth + "-31 23:59:59";
        
        List<UserPointsDetail> details = pointsDetailMapper.selectDetailsByTimeRange(userId, startTime, endTime);
        return details.stream()
                .filter(detail -> detail.getPointsType().equals(PointsType.CHECK_IN.getCode()))
                .mapToInt(UserPointsDetail::getPointsChange)
                .sum();
    }
    
    /**
     * 计算用户打卡获得的总积分
     */
    private Integer calculateTotalCheckinPoints(Long userId) {
        // 查询用户所有积分明细，筛选打卡类型并求和
        try {
            PointsDetailQueryRequest request = new PointsDetailQueryRequest();
            request.setUserId(userId);
            request.setPointsType(PointsType.CHECK_IN.getCode());
            // 查询大量数据用于统计，设置较大的分页
            request.setPageNum(1);
            request.setPageSize(10000);
            
            List<UserPointsDetail> details = pointsDetailMapper.selectDetailList(request);
            return details.stream()
                    .mapToInt(UserPointsDetail::getPointsChange)
                    .sum();
        } catch (Exception e) {
            log.warn("查询用户打卡总积分失败，userId: {}", userId, e);
            return 0;
        }
    }
    
    /**
     * 转换积分明细为响应DTO
     */
    private PointsDetailResponse convertToDetailResponse(UserPointsDetail detail) {
        PointsDetailResponse response = BeanUtil.copyProperties(detail, PointsDetailResponse.class);
        response.setPointsTypeDesc(PointsType.getDescByCode(detail.getPointsType()));
        return response;
    }
    
    @Override
    public PageResult<UserPointsRankingResponse> getUserPointsList(UserPointsListRequest request) {
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            List<UserPointsBalance> balances = pointsBalanceMapper.selectBalanceList();
            
            // 根据请求条件过滤和排序
            List<UserPointsBalance> filteredBalances = balances.stream()
                    .filter(balance -> {
                        // 用户名筛选
                        if (StrUtil.isNotBlank(request.getUserName())) {
                            String userName = getUserName(balance.getUserId());
                            if (!userName.contains(request.getUserName())) {
                                return false;
                            }
                        }
                        
                        // 积分范围筛选
                        if (request.getMinPoints() != null && balance.getTotalPoints() < request.getMinPoints()) {
                            return false;
                        }
                        if (request.getMaxPoints() != null && balance.getTotalPoints() > request.getMaxPoints()) {
                            return false;
                        }
                        
                        return true;
                    })
                    .sorted((a, b) -> {
                        // 排序处理
                        if ("points".equals(request.getOrderBy())) {
                            int result = Integer.compare(a.getTotalPoints(), b.getTotalPoints());
                            return "desc".equals(request.getOrderDirection()) ? -result : result;
                        } else if ("create_time".equals(request.getOrderBy())) {
                            int result = a.getCreateTime().compareTo(b.getCreateTime());
                            return "desc".equals(request.getOrderDirection()) ? -result : result;
                        }
                        // 默认按积分降序
                        return Integer.compare(b.getTotalPoints(), a.getTotalPoints());
                    })
                    .collect(Collectors.toList());
            
            // 转换为响应DTO并设置排名
            List<UserPointsRankingResponse> responses = new ArrayList<>();
            for (int i = 0; i < filteredBalances.size(); i++) {
                UserPointsBalance balance = filteredBalances.get(i);
                int continuousDays = getCurrentContinuousDays(balance.getUserId(), LocalDate.now());
                
                // 获取完整用户信息
                SimpleUserInfo userInfo = getUserInfo(balance.getUserId());
                String userName = userInfo != null ? userInfo.getDisplayName() : "用户" + balance.getUserId();
                
                UserPointsRankingResponse response = UserPointsRankingResponse.builder()
                        .userId(balance.getUserId())
                        .userName(userName)
                        .nickName(userInfo != null ? userInfo.getNickname() : userName)
                        .avatar(userInfo != null ? userInfo.getAvatar() : null)
                        .totalPoints(balance.getTotalPoints())
                        .balanceYuan(formatPointsToYuan(balance.getTotalPoints()))
                        .continuousDays(continuousDays)
                        .ranking(i + 1) // 设置排名（当前页面内的排名）
                        .createTime(balance.getCreateTime())
                        .updateTime(balance.getUpdateTime())
                        .build();
                
                responses.add(response);
            }
            
            return responses;
        });
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchGrantPointsResponse batchGrantPoints(BatchGrantPointsRequest request, Long adminId) {
        log.info("管理员{}开始批量发放积分，用户数量：{}，积分：{}，原因：{}", 
                adminId, request.getUserIds().size(), request.getPoints(), request.getReason());
        
        List<BatchGrantPointsResponse.GrantResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        int totalPointsGranted = 0;
        
        for (Long userId : request.getUserIds()) {
            try {
                // 确保用户有积分账户
                ensureUserPointsBalance(userId);
                
                // 更新积分余额
                pointsBalanceMapper.addPoints(userId, request.getPoints());
                
                // 获取更新后的余额
                UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);
                
                // 创建积分明细记录
                UserPointsDetail detail = new UserPointsDetail();
                detail.setUserId(userId);
                detail.setPointsChange(request.getPoints());
                detail.setPointsType(PointsType.ADMIN_GRANT.getCode());
                detail.setDescription(request.getReason());
                detail.setBalanceAfter(balance.getTotalPoints());
                detail.setAdminId(adminId);
                detail.setCreateTime(LocalDateTime.now());
                
                pointsDetailMapper.insert(detail);
                
                String userName = getUserName(userId);
                results.add(BatchGrantPointsResponse.GrantResult.builder()
                        .userId(userId)
                        .userName(userName)
                        .success(true)
                        .message("发放成功")
                        .currentBalance(balance.getTotalPoints())
                        .build());
                
                successCount++;
                totalPointsGranted += request.getPoints();
                
                log.info("为用户{}发放{}积分成功，当前余额：{}", userId, request.getPoints(), balance.getTotalPoints());
                
            } catch (Exception e) {
                log.error("为用户{}发放积分失败", userId, e);
                
                String userName = getUserName(userId);
                results.add(BatchGrantPointsResponse.GrantResult.builder()
                        .userId(userId)
                        .userName(userName)
                        .success(false)
                        .message(e.getMessage())
                        .currentBalance(0)
                        .build());
                
                failCount++;
            }
        }
        
        log.info("批量发放积分完成，成功：{}，失败：{}，总计发放：{}", successCount, failCount, totalPointsGranted);
        
        return BatchGrantPointsResponse.builder()
                .successCount(successCount)
                .failCount(failCount)
                .totalPointsGranted(totalPointsGranted)
                .grantResults(results)
                .build();
    }
    
    @Override
    public UserPointsInfoResponse getUserPointsInfo(Long userId) {
        // 参数校验
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        
        // 获取用户积分余额
        UserPointsBalance pointsBalance = pointsBalanceMapper.selectByUserId(userId);
        if (pointsBalance == null) {
            throw new BusinessException("用户积分账户不存在");
        }
        
        // 获取用户基本信息
        SimpleUserInfo userInfo = getUserInfo(userId);
        if (userInfo == null) {
            throw new BusinessException("用户不存在或已删除");
        }
        
        // 获取打卡信息
        String currentYearMonth = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        UserCheckinBitmap currentMonthBitmap = checkinBitmapMapper.selectByUserIdAndYearMonth(userId, currentYearMonth);
        UserCheckinBitmap lastCheckinRecord = checkinBitmapMapper.selectLatestByUserId(userId);
        
        // 计算打卡相关数据
        int continuousDays = 0;
        java.time.LocalDate lastCheckinDate = null;
        int monthCheckinDays = 0;
        boolean hasCheckedToday = false;
        
        if (lastCheckinRecord != null) {
            continuousDays = lastCheckinRecord.getContinuousDays();
            lastCheckinDate = lastCheckinRecord.getLastCheckinDate();
            
            // 检查今日是否已打卡
            java.time.LocalDate today = java.time.LocalDate.now();
            hasCheckedToday = lastCheckinDate != null && lastCheckinDate.equals(today);
        }
        
        if (currentMonthBitmap != null) {
            monthCheckinDays = currentMonthBitmap.getTotalCheckinDays();
        }
        
        return UserPointsInfoResponse.builder()
                .userId(userId)
                .userName(userInfo.getDisplayName())
                .nickName(userInfo.getNickname())
                .realName(userInfo.getRealName())
                .avatar(userInfo.getAvatar())
                .totalPoints(pointsBalance.getTotalPoints())
                .continuousDays(continuousDays)
                .lastCheckinDate(lastCheckinDate)
                .monthCheckinDays(monthCheckinDays)
                .createTime(pointsBalance.getCreateTime())
                .updateTime(pointsBalance.getUpdateTime())
                .hasCheckedToday(hasCheckedToday)
                .build();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantSystemPoints(Long userId, int points, int type, String description) {
        log.info("系统发放积分: userId={}, points={}, type={}, desc={}", userId, points, type, description);
        ensureUserPointsBalance(userId);
        pointsBalanceMapper.addPoints(userId, points);
        UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);

        UserPointsDetail detail = new UserPointsDetail();
        detail.setUserId(userId);
        detail.setPointsChange(points);
        detail.setPointsType(type);
        detail.setDescription(description);
        detail.setBalanceAfter(balance.getTotalPoints());
        detail.setCreateTime(LocalDateTime.now());
        pointsDetailMapper.insert(detail);
    }

    /**
     * 获取用户名称
     */
    private String getUserName(Long userId) {
        return userInfoApiService.getUserDisplayName(userId);
    }
    
    /**
     * 获取用户完整信息（用于前端显示）
     */
    private SimpleUserInfo getUserInfo(Long userId) {
        return userInfoApiService.getSimpleUserInfo(userId);
    }
    
    /**
     * 格式化积分为人民币
     */
    private String formatPointsToYuan(Integer points) {
        if (points == null || points == 0) {
            return "0.00";
        }
        BigDecimal yuan = BigDecimal.valueOf(points).divide(BigDecimal.valueOf(POINTS_TO_YUAN_RATE), 2, RoundingMode.HALF_UP);
        return yuan.toString();
    }
}



