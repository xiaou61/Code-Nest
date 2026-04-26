package com.xiaou.filestorage.service.impl;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.filestorage.domain.StorageConfig;
import com.xiaou.filestorage.factory.StorageStrategyFactory;
import com.xiaou.filestorage.mapper.StorageConfigMapper;
import com.xiaou.filestorage.service.StorageHealthService;
import com.xiaou.filestorage.strategy.FileStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


/**
 * 存储健康检查服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
public class StorageHealthServiceImpl implements StorageHealthService {

    @Autowired
    private StorageConfigMapper storageConfigMapper;

    @Autowired
    private StorageStrategyFactory strategyFactory;

    // 简化版本，不再使用复杂缓存

    @Override
    public boolean isStorageHealthy(StorageConfig storageConfig) {
        if (storageConfig == null) {
            return false;
        }

        // 直接执行健康检查，不使用缓存
        return performHealthCheck(storageConfig);
    }

    @Override
    public StorageConfig getNextAvailableStorage(Long excludeConfigId) {
        try {
            // 简化逻辑：故障转移时直接返回本地存储配置
            List<StorageConfig> enabledConfigs = storageConfigMapper.selectEnabled();
            
            // 寻找本地存储配置
            return enabledConfigs.stream()
                .filter(config -> !config.getId().equals(excludeConfigId))
                .filter(config -> "LOCAL".equals(config.getStorageType()))
                .findFirst()
                .orElse(null);

        } catch (Exception e) {
            log.error("获取本地存储配置失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void markStorageUnhealthy(Long configId) {
        log.warn("存储标记为不健康: configId={}", configId);
        // 简化逻辑：直接更新数据库状态
        try {
            storageConfigMapper.updateTestStatus(configId, 0);
        } catch (Exception e) {
            log.error("更新存储健康状态失败: configId={}, error={}", configId, e.getMessage());
        }
    }

    @Override
    public void markStorageHealthy(Long configId) {
        log.info("存储标记为健康: configId={}", configId);
        try {
            storageConfigMapper.updateTestStatus(configId, 1);
        } catch (Exception e) {
            log.error("更新存储健康状态失败: configId={}, error={}", configId, e.getMessage());
        }
    }

    @Override
    public void checkAllStorageHealth() {
        // 简化版本：不再进行定时批量检查，按需检查即可
        log.debug("简化版本已移除定时批量健康检查，改为按需检查");
    }

    /**
     * 执行具体的健康检查
     *
     * @param storageConfig 存储配置
     * @return 是否健康
     */
    private boolean performHealthCheck(StorageConfig storageConfig) {
        try {
            // 解析配置参数
            Map<String, Object> configParams = JsonUtils.parseMap(storageConfig.getConfigParams());
            
            // 创建并初始化存储策略
            FileStorageStrategy strategy = strategyFactory.createAndInitialize(
                storageConfig.getId(), storageConfig.getStorageType(), configParams);

            if (strategy == null) {
                log.warn("存储策略初始化失败: configId={}, type={}", 
                    storageConfig.getId(), storageConfig.getStorageType());
                return false;
            }

            // 执行连接测试
            boolean testResult = strategy.testConnection();
            
            if (!testResult) {
                log.warn("存储连接测试失败: configId={}, type={}", 
                    storageConfig.getId(), storageConfig.getStorageType());
            }

            return testResult;

        } catch (Exception e) {
            log.error("执行存储健康检查失败: configId={}, error={}", storageConfig.getId(), e.getMessage(), e);
            return false;
        }
    }
}
