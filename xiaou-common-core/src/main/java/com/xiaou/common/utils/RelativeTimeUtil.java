package com.xiaou.common.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 相对时间显示工具类
 * 将时间转换为"刚刚"、"3分钟前"、"2小时前"等相对时间格式
 * 
 * @author xiaou
 */
public class RelativeTimeUtil {
    
    /**
     * 时间格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 将时间转换为相对时间
     * 
     * @param dateTime 时间
     * @return 相对时间字符串
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now);
        
        long seconds = duration.getSeconds();
        
        // 1分钟内：刚刚
        if (seconds < 60) {
            return "刚刚";
        }
        
        // 60分钟内：X分钟前
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "分钟前";
        }
        
        // 24小时内：X小时前
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "小时前";
        }
        
        // 7天内：X天前
        long days = hours / 24;
        if (days < 7) {
            return days + "天前";
        }
        
        // 超过7天但在同一年：显示月-日
        if (dateTime.getYear() == now.getYear()) {
            return dateTime.format(DATE_FORMATTER);
        }
        
        // 超过1年：显示年-月-日
        return dateTime.format(DATETIME_FORMATTER);
    }
    
    /**
     * 将时间字符串转换为相对时间
     * 
     * @param dateTimeStr 时间字符串（格式：yyyy-MM-dd HH:mm:ss）
     * @return 相对时间字符串
     */
    public static String format(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return "";
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);
            return format(dateTime);
        } catch (Exception e) {
            return dateTimeStr;
        }
    }
}

