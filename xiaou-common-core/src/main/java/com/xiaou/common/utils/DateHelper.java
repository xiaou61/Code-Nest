package com.xiaou.common.utils;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期处理工具类
 * 统一处理系统中的日期转换
 *
 * @author xiaou
 */
@Slf4j
public class DateHelper {

    /**
     * 标准日期时间格式
     */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 标准日期格式
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 标准时间格式
     */
    public static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 获取当前时间
     */
    public static Date now() {
        return new Date();
    }

    /**
     * 格式化日期时间为字符串
     * @param date 日期
     * @return 格式化后的字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return DateUtil.formatDateTime(date);
    }

    /**
     * 格式化当前日期时间为字符串
     * @return 格式化后的字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String formatCurrentDateTime() {
        return formatDateTime(new Date());
    }

    /**
     * 格式化日期为字符串
     * @param date 日期
     * @return 格式化后的字符串 (yyyy-MM-dd)
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return DateUtil.formatDate(date);
    }

    /**
     * 字符串转换为日期时间
     * @param dateTimeStr 日期时间字符串 (yyyy-MM-dd HH:mm:ss)
     * @return 日期对象
     */
    public static Date parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_PATTERN);
            return sdf.parse(dateTimeStr);
        } catch (ParseException e) {
            log.error("日期时间解析失败: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * 字符串转换为日期
     * @param dateStr 日期字符串 (yyyy-MM-dd)
     * @return 日期对象
     */
    public static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            log.error("日期解析失败: {}", dateStr, e);
            return null;
        }
    }

    /**
     * 日期加小时
     * @param date 原日期
     * @param hours 要增加的小时数
     * @return 新的日期
     */
    public static Date addHours(Date date, int hours) {
        if (date == null) {
            return null;
        }
        return DateUtil.offsetHour(date, hours);
    }

    /**
     * 从当前时间加小时
     * @param hours 要增加的小时数
     * @return 新的日期
     */
    public static Date addHoursFromNow(int hours) {
        return addHours(new Date(), hours);
    }

    /**
     * 日期加天数
     * @param date 原日期
     * @param days 要增加的天数
     * @return 新的日期
     */
    public static Date addDays(Date date, int days) {
        if (date == null) {
            return null;
        }
        return DateUtil.offsetDay(date, days);
    }

    /**
     * 从当前时间加天数
     * @param days 要增加的天数
     * @return 新的日期
     */
    public static Date addDaysFromNow(int days) {
        return addDays(new Date(), days);
    }

    /**
     * 判断日期是否已过期
     * @param expireDate 过期日期
     * @return true-已过期, false-未过期
     */
    public static boolean isExpired(Date expireDate) {
        if (expireDate == null) {
            return false;
        }
        return expireDate.before(new Date());
    }

    /**
     * 判断字符串日期是否已过期
     * @param expireDateStr 过期日期字符串
     * @return true-已过期, false-未过期
     */
    public static boolean isExpired(String expireDateStr) {
        Date expireDate = parseDateTime(expireDateStr);
        return isExpired(expireDate);
    }
}
