package com.xiaou.system.service;

import com.xiaou.system.dto.DashboardOverviewResponse;

/**
 * 仪表板服务接口
 *
 * @author xiaou
 */
public interface SysDashboardService {

    /**
     * 获取仪表板总览数据
     */
    DashboardOverviewResponse getOverview();
}
