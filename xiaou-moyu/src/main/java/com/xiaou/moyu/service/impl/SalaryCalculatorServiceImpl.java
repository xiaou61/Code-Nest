package com.xiaou.moyu.service.impl;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.moyu.dto.SalaryCalculatorDto;
import com.xiaou.moyu.dto.SalaryConfigRequest;
import com.xiaou.moyu.dto.WorkTimeRequest;
import com.xiaou.moyu.domain.UserSalaryConfig;
import com.xiaou.moyu.domain.WorkRecord;
import com.xiaou.moyu.mapper.UserSalaryConfigMapper;
import com.xiaou.moyu.mapper.WorkRecordMapper;
import com.xiaou.moyu.service.SalaryCalculatorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

/**
 * 时薪计算器服务实现类
 *
 * @author xiaou
 */
@Service
public class SalaryCalculatorServiceImpl implements SalaryCalculatorService {

    @Resource
    private UserSalaryConfigMapper userSalaryConfigMapper;
    
    @Resource
    private WorkRecordMapper workRecordMapper;

    @Override
    public SalaryCalculatorDto getSalaryCalculator(Long userId) {
        SalaryCalculatorDto dto = new SalaryCalculatorDto();
        
        // 获取用户薪资配置
        UserSalaryConfig config = userSalaryConfigMapper.selectByUserId(userId);
        if (config == null) {
            return dto; // 返回空数据，表示用户未配置薪资信息
        }
        
        // 设置基础薪资信息
        dto.setMonthlySalary(config.getMonthlySalary());
        dto.setWorkDaysPerMonth(config.getWorkDaysPerMonth());
        dto.setWorkHoursPerDay(config.getWorkHoursPerDay());
        dto.setHourlyRate(config.getHourlyRate());
        
        // 计算今日工作数据
        calculateTodayData(userId, dto);
        
        // 计算本周工作数据
        calculateWeekData(userId, dto);
        
        // 计算本月工作数据
        calculateMonthData(userId, dto);
        
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateSalaryConfig(Long userId, SalaryConfigRequest request) {
        UserSalaryConfig existingConfig = userSalaryConfigMapper.selectByUserId(userId);
        
        LocalDateTime now = LocalDateTime.now();
        
        if (existingConfig == null) {
            // 新增配置
            UserSalaryConfig config = new UserSalaryConfig();
            config.setUserId(userId);
            config.setMonthlySalary(request.getMonthlySalary());
            config.setWorkDaysPerMonth(request.getWorkDaysPerMonth());
            config.setWorkHoursPerDay(request.getWorkHoursPerDay());
            config.setStatus(1);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            
            return userSalaryConfigMapper.insert(config) > 0;
        } else {
            // 更新配置
            existingConfig.setMonthlySalary(request.getMonthlySalary());
            existingConfig.setWorkDaysPerMonth(request.getWorkDaysPerMonth());
            existingConfig.setWorkHoursPerDay(request.getWorkHoursPerDay());
            existingConfig.setUpdateTime(now);
            
            return userSalaryConfigMapper.update(existingConfig) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalaryCalculatorDto handleWorkTimeAction(Long userId, WorkTimeRequest request) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        
        // 获取用户薪资配置
        UserSalaryConfig config = userSalaryConfigMapper.selectByUserId(userId);
        if (config == null) {
            throw new BusinessException("请先配置薪资信息");
        }
        
        // 获取或创建今日工作记录
        WorkRecord todayRecord = workRecordMapper.selectByUserIdAndDate(userId, today);
        if (todayRecord == null) {
            todayRecord = new WorkRecord();
            todayRecord.setUserId(userId);
            todayRecord.setWorkDate(today);
            todayRecord.setWorkHours(BigDecimal.ZERO);
            todayRecord.setDailyEarnings(BigDecimal.ZERO);
            todayRecord.setStatus(1);
            todayRecord.setCreateTime(now);
            todayRecord.setUpdateTime(now);
        }
        
        // 处理不同的工作时间操作
        switch (request.getAction().toUpperCase()) {
            case "START":
                todayRecord.setStartTime(now);
                todayRecord.setWorkStatus(1); // 进行中
                todayRecord.setTotalPauseMinutes(0);
                todayRecord.setRemark(request.getRemark());
                break;
                
            case "PAUSE":
                if (todayRecord.getWorkStatus() != 1) {
                    throw new BusinessException("只有工作中状态才能暂停");
                }
                todayRecord.setPauseStartTime(now);
                todayRecord.setWorkStatus(2); // 暂停中
                break;
                
            case "RESUME":
                if (todayRecord.getWorkStatus() != 2) {
                    throw new BusinessException("只有暂停状态才能恢复");
                }
                if (todayRecord.getPauseStartTime() != null) {
                    // 累加暂停时长
                    long pauseMinutes = java.time.Duration.between(todayRecord.getPauseStartTime(), now).toMinutes();
                    int totalPause = (todayRecord.getTotalPauseMinutes() == null ? 0 : todayRecord.getTotalPauseMinutes());
                    todayRecord.setTotalPauseMinutes(totalPause + (int)pauseMinutes);
                    todayRecord.setPauseStartTime(null);
                }
                todayRecord.setWorkStatus(1); // 恢复进行中
                break;
                
            case "END":
                if (todayRecord.getStartTime() == null) {
                    throw new BusinessException("请先开始工作");
                }
                // 如果正在暂停，先处理暂停时长
                if (todayRecord.getWorkStatus() == 2 && todayRecord.getPauseStartTime() != null) {
                    long pauseMinutes = java.time.Duration.between(todayRecord.getPauseStartTime(), now).toMinutes();
                    int totalPause = (todayRecord.getTotalPauseMinutes() == null ? 0 : todayRecord.getTotalPauseMinutes());
                    todayRecord.setTotalPauseMinutes(totalPause + (int)pauseMinutes);
                    todayRecord.setPauseStartTime(null);
                }
                todayRecord.setEndTime(now);
                todayRecord.setWorkStatus(3); // 已完成
                // 计算工作时长和收入
                calculateWorkHoursAndEarnings(todayRecord, config);
                break;
                
            default:
                throw new BusinessException("不支持的操作类型：" + request.getAction());
        }
        
        todayRecord.setUpdateTime(now);
        
        // 保存或更新工作记录
        if (todayRecord.getId() == null) {
            workRecordMapper.insert(todayRecord);
        } else {
            workRecordMapper.update(todayRecord);
        }
        
        // 返回最新的计算器数据
        return getSalaryCalculator(userId);
    }

    @Override
    public SalaryConfigRequest getSalaryConfig(Long userId) {
        UserSalaryConfig config = userSalaryConfigMapper.selectByUserId(userId);
        if (config == null) {
            return null;
        }
        
        SalaryConfigRequest request = new SalaryConfigRequest();
        request.setMonthlySalary(config.getMonthlySalary());
        request.setWorkDaysPerMonth(config.getWorkDaysPerMonth());
        request.setWorkHoursPerDay(config.getWorkHoursPerDay());
        
        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSalaryConfig(Long userId) {
        return userSalaryConfigMapper.deleteByUserId(userId) > 0;
    }

    /**
     * 计算今日工作数据
     */
    private void calculateTodayData(Long userId, SalaryCalculatorDto dto) {
        LocalDate today = LocalDate.now();
        WorkRecord todayRecord = workRecordMapper.selectByUserIdAndDate(userId, today);
        
        if (todayRecord != null) {
            dto.setTodayWorkHours(todayRecord.getWorkHours() != null ? todayRecord.getWorkHours() : BigDecimal.ZERO);
            dto.setTodayEarnings(todayRecord.getDailyEarnings() != null ? todayRecord.getDailyEarnings() : BigDecimal.ZERO);
            dto.setTodayStartTime(todayRecord.getStartTime());
            dto.setTotalPauseMinutes(todayRecord.getTotalPauseMinutes());
            
            // 使用workStatus字段
            Integer workStatus = todayRecord.getWorkStatus() != null ? todayRecord.getWorkStatus() : 0;
            dto.setWorkStatus(workStatus);
        } else {
            dto.setTodayWorkHours(BigDecimal.ZERO);
            dto.setTodayEarnings(BigDecimal.ZERO);
            dto.setTotalPauseMinutes(0);
            dto.setWorkStatus(0); // 未开始
        }
    }

    /**
     * 计算本周工作数据
     */
    private void calculateWeekData(Long userId, SalaryCalculatorDto dto) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1);
        LocalDate weekEnd = today.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 7);
        
        List<WorkRecord> weekRecords = workRecordMapper.selectByUserIdAndDateRange(userId, weekStart, weekEnd);
        
        BigDecimal weekWorkHours = BigDecimal.ZERO;
        BigDecimal weekEarnings = BigDecimal.ZERO;
        
        for (WorkRecord record : weekRecords) {
            if (record.getWorkHours() != null) {
                weekWorkHours = weekWorkHours.add(record.getWorkHours());
            }
            if (record.getDailyEarnings() != null) {
                weekEarnings = weekEarnings.add(record.getDailyEarnings());
            }
        }
        
        dto.setWeekWorkHours(weekWorkHours);
        dto.setWeekEarnings(weekEarnings);
    }

    /**
     * 计算本月工作数据
     */
    private void calculateMonthData(Long userId, SalaryCalculatorDto dto) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        
        List<WorkRecord> monthRecords = workRecordMapper.selectByUserIdAndDateRange(userId, monthStart, monthEnd);
        
        BigDecimal monthWorkHours = BigDecimal.ZERO;
        BigDecimal monthEarnings = BigDecimal.ZERO;
        
        for (WorkRecord record : monthRecords) {
            if (record.getWorkHours() != null) {
                monthWorkHours = monthWorkHours.add(record.getWorkHours());
            }
            if (record.getDailyEarnings() != null) {
                monthEarnings = monthEarnings.add(record.getDailyEarnings());
            }
        }
        
        dto.setMonthWorkHours(monthWorkHours);
        dto.setMonthEarnings(monthEarnings);
    }

    /**
     * 计算工作时长和收入
     */
    private void calculateWorkHoursAndEarnings(WorkRecord record, UserSalaryConfig config) {
        if (record.getStartTime() != null && record.getEndTime() != null) {
            // 计算总时长（小时）
            long totalMinutes = java.time.Duration.between(record.getStartTime(), record.getEndTime()).toMinutes();
            
            // 减去暂停时长
            int pauseMinutes = record.getTotalPauseMinutes() != null ? record.getTotalPauseMinutes() : 0;
            long actualWorkMinutes = totalMinutes - pauseMinutes;
            
            // 确保工作时长不为负数
            if (actualWorkMinutes < 0) {
                actualWorkMinutes = 0;
            }
            
            BigDecimal workHours = BigDecimal.valueOf(actualWorkMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            record.setWorkHours(workHours);
            
            // 计算当日收入
            BigDecimal dailyEarnings = workHours.multiply(config.getHourlyRate()).setScale(2, RoundingMode.HALF_UP);
            record.setDailyEarnings(dailyEarnings);
        }
    }
}
