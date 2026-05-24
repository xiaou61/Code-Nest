package com.xiaou.team.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 日期维度数值统计
 */
@Data
public class DateValueStat {

    private LocalDate statDate;

    private Integer value;
}
