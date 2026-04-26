package com.xiaou.filestorage.service.impl;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.filestorage.domain.StorageConfig;
import com.xiaou.filestorage.factory.StorageStrategyFactory;
import com.xiaou.filestorage.mapper.StorageConfigMapper;
import com.xiaou.filestorage.service.StorageConfigService;
import com.xiaou.filestorage.strategy.FileStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存储配置服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
public class StorageConfigServiceImpl implements StorageConfigService {

    @Autowired
    private StorageConfigMapper storageConfigMapper;

    @Autowired
    private StorageStrategyFactory strategyFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createConfig(StorageConfig storageConfig) {
        try {
            // 检查配置名称是否重复
            StorageConfig existing = storageConfigMapper.selectByConfigName(storageConfig.getConfigName());
            if (existing != null) {
                log.warn("存储配置名称已存在: {}", storageConfig.getConfigName());
                return false;
            }

            // 检查存储类型是否支持
            if (!strategyFactory.isSupported(storageConfig.getStorageType())) {
                log.warn("不支持的存储类型: {}", storageConfig.getStorageType());
                return false;
            }

            storageConfig.setCreateTime(new Date());
            storageConfig.setUpdateTime(new Date());

            // 如果是第一个配置或设置为默认，则设为默认配置
            if (storageConfig.getIsDefault() == 1) {
                storageConfigMapper.clearAllDefaultStatus();
            }

            int inserted = storageConfigMapper.insert(storageConfig);
            if (inserted > 0) {
                // 自动测试配置
                testConfig(storageConfig.getId());
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("创建存储配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(StorageConfig storageConfig) {
        try {
            StorageConfig existing = storageConfigMapper.selectById(storageConfig.getId());
            if (existing == null) {
                log.warn("存储配置不存在: {}", storageConfig.getId());
                return false;
            }

            // 检查配置名称是否与其他配置重复
            StorageConfig duplicateName = storageConfigMapper.selectByConfigName(storageConfig.getConfigName());
            if (duplicateName != null && !duplicateName.getId().equals(storageConfig.getId())) {
                log.warn("存储配置名称已存在: {}", storageConfig.getConfigName());
                return false;
            }

            // 如果设置为默认，清除其他默认配置
            if (storageConfig.getIsDefault() != null && storageConfig.getIsDefault() == 1) {
                storageConfigMapper.clearAllDefaultStatus();
            }

            storageConfig.setUpdateTime(new Date());

            int updated = storageConfigMapper.updateById(storageConfig);
            if (updated > 0) {
                // 移除缓存的策略实例
                strategyFactory.removeInitializedStrategy(storageConfig.getId());
                
                // 如果配置已启用，自动测试配置
                if (storageConfig.getIsEnabled() != null && storageConfig.getIsEnabled() == 1) {
                    testConfig(storageConfig.getId());
                }
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("更新存储配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(Long id) {
        try {
            StorageConfig config = storageConfigMapper.selectById(id);
            if (config == null) {
                return true; // 配置不存在，视为删除成功
            }

            // 不允许删除默认配置
            if (config.getIsDefault() == 1) {
                log.warn("不能删除默认存储配置: {}", id);
                return false;
            }

            int deleted = storageConfigMapper.deleteById(id);
            if (deleted > 0) {
                // 移除缓存的策略实例
                strategyFactory.removeInitializedStrategy(id);
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("删除存储配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public StorageConfig getConfigById(Long id) {
        try {
            return storageConfigMapper.selectById(id);
        } catch (Exception e) {
            log.error("查询存储配置失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<StorageConfig> listConfigs(String storageType, Integer isEnabled) {
        try {
            return storageConfigMapper.selectByCondition(storageType, isEnabled);
        } catch (Exception e) {
            log.error("查询存储配置列表失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public StorageConfig getDefaultConfig() {
        try {
            return storageConfigMapper.selectDefault();
        } catch (Exception e) {
            log.error("查询默认存储配置失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultConfig(Long id) {
        try {
            StorageConfig config = storageConfigMapper.selectById(id);
            if (config == null) {
                log.warn("存储配置不存在: {}", id);
                return false;
            }

            if (config.getIsEnabled() != 1) {
                log.warn("只有启用的配置才能设为默认: {}", id);
                return false;
            }

            // 清除其他默认配置
            storageConfigMapper.clearAllDefaultStatus();
            
            // 设置当前配置为默认
            int updated = storageConfigMapper.updateDefaultStatus(id, 1);
            return updated > 0;

        } catch (Exception e) {
            log.error("设置默认存储配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleConfig(Long id, Integer isEnabled) {
        try {
            StorageConfig config = storageConfigMapper.selectById(id);
            if (config == null) {
                log.warn("存储配置不存在: {}", id);
                return false;
            }

            // 如果要禁用默认配置，需要先取消默认状态
            if (isEnabled == 0 && config.getIsDefault() == 1) {
                storageConfigMapper.updateDefaultStatus(id, 0);
            }

            int updated = storageConfigMapper.updateEnabledStatus(id, isEnabled);
            if (updated > 0) {
                // 移除缓存的策略实例
                strategyFactory.removeInitializedStrategy(id);
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("切换存储配置状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> testConfig(Long id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            StorageConfig config = storageConfigMapper.selectById(id);
            if (config == null) {
                result.put("success", false);
                result.put("message", "存储配置不存在");
                return result;
            }

            // 解析配置参数
            Map<String, Object> configParams = JsonUtils.parseMap(config.getConfigParams());
            
            // 创建并测试策略
            FileStorageStrategy strategy = strategyFactory.createAndInitialize(
                config.getId(), config.getStorageType(), configParams);

            if (strategy == null) {
                result.put("success", false);
                result.put("message", "初始化存储策略失败");
                storageConfigMapper.updateTestStatus(id, 0);
                return result;
            }

            // 测试连接
            boolean testResult = strategy.testConnection();
            
            // 更新测试状态
            storageConfigMapper.updateTestStatus(id, testResult ? 1 : 0);
            
            result.put("success", testResult);
            result.put("message", testResult ? "连接测试成功" : "连接测试失败");
            
            return result;

        } catch (Exception e) {
            log.error("测试存储配置失败: {}", e.getMessage(), e);
            
            // 更新测试状态为失败
            storageConfigMapper.updateTestStatus(id, 0);
            
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public List<String> getSupportedStorageTypes() {
        return strategyFactory.getSupportedStorageTypes();
    }
}
