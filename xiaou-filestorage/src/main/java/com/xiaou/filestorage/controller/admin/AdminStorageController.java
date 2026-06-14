package com.xiaou.filestorage.controller.admin;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.filestorage.domain.StorageConfig;
import com.xiaou.filestorage.service.StorageConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员存储配置控制器
 *
 * @author xiaou
 */
@Slf4j
@RestController
@RequestMapping("/admin/storage")
public class AdminStorageController {

    @Autowired
    private StorageConfigService storageConfigService;

    /**
     * 获取存储配置列表
     *
     * @param storageType 存储类型
     * @param isEnabled   是否启用
     * @return 存储配置列表
     */
    @GetMapping("/configs")
    @RequireAdmin
    public Result<List<StorageConfig>> listConfigs(@RequestParam(value = "storageType", required = false) String storageType,
                                                  @RequestParam(value = "isEnabled", required = false) Integer isEnabled) {
        try {
            List<StorageConfig> configs = storageConfigService.listConfigs(storageType, isEnabled);
            return Result.success(configs);
        } catch (Exception e) {
            log.error("查询存储配置列表失败: {}", e.getMessage(), e);
            return Result.error("查询存储配置列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取存储配置
     *
     * @param id 配置ID
     * @return 存储配置
     */
    @GetMapping("/config/{id}")
    @RequireAdmin
    public Result<StorageConfig> getConfig(@PathVariable("id") Long id) {
        try {
            StorageConfig config = storageConfigService.getConfigById(id);
            if (config != null) {
                return Result.success(config);
            } else {
                return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "存储配置不存在");
            }
        } catch (Exception e) {
            log.error("查询存储配置失败: {}", e.getMessage(), e);
            return Result.error("查询存储配置失败: " + e.getMessage());
        }
    }

    /**
     * 创建存储配置
     *
     * @param storageConfig 存储配置
     * @return 创建结果
     */
    @PostMapping("/config")
    @RequireAdmin
    public Result<Boolean> createConfig(@RequestBody StorageConfig storageConfig) {
        try {
            boolean success = storageConfigService.createConfig(storageConfig);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("创建存储配置失败");
            }
        } catch (Exception e) {
            log.error("创建存储配置失败: {}", e.getMessage(), e);
            return Result.error("创建存储配置失败: " + e.getMessage());
        }
    }

    /**
     * 更新存储配置
     *
     * @param id            配置ID
     * @param storageConfig 存储配置
     * @return 更新结果
     */
    @PutMapping("/config/{id}")
    @RequireAdmin
    public Result<Boolean> updateConfig(@PathVariable("id") Long id, @RequestBody StorageConfig storageConfig) {
        try {
            storageConfig.setId(id);
            boolean success = storageConfigService.updateConfig(storageConfig);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("更新存储配置失败");
            }
        } catch (Exception e) {
            log.error("更新存储配置失败: {}", e.getMessage(), e);
            return Result.error("更新存储配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除存储配置
     *
     * @param id 配置ID
     * @return 删除结果
     */
    @DeleteMapping("/config/{id}")
    @RequireAdmin
    public Result<Boolean> deleteConfig(@PathVariable("id") Long id) {
        try {
            boolean success = storageConfigService.deleteConfig(id);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("删除存储配置失败");
            }
        } catch (Exception e) {
            log.error("删除存储配置失败: {}", e.getMessage(), e);
            return Result.error("删除存储配置失败: " + e.getMessage());
        }
    }

    /**
     * 测试存储配置
     *
     * @param id 配置ID
     * @return 测试结果
     */
    @PostMapping("/config/{id}/test")
    @RequireAdmin
    public Result<Map<String, Object>> testConfig(@PathVariable("id") Long id) {
        try {
            Map<String, Object> testResult = storageConfigService.testConfig(id);
            return Result.success(testResult);
        } catch (Exception e) {
            log.error("测试存储配置失败: {}", e.getMessage(), e);
            return Result.error("测试存储配置失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用存储配置
     *
     * @param id        配置ID
     * @param isEnabled 是否启用
     * @return 操作结果
     */
    @PutMapping("/config/{id}/enable")
    @RequireAdmin
    public Result<Boolean> toggleConfig(@PathVariable("id") Long id, @RequestParam("isEnabled") Integer isEnabled) {
        try {
            boolean success = storageConfigService.toggleConfig(id, isEnabled);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("操作失败");
            }
        } catch (Exception e) {
            log.error("切换存储配置状态失败: {}", e.getMessage(), e);
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 设置默认存储配置
     *
     * @param id 配置ID
     * @return 操作结果
     */
    @PutMapping("/config/{id}/default")
    @RequireAdmin
    public Result<Boolean> setDefaultConfig(@PathVariable("id") Long id) {
        try {
            boolean success = storageConfigService.setDefaultConfig(id);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("设置默认配置失败");
            }
        } catch (Exception e) {
            log.error("设置默认存储配置失败: {}", e.getMessage(), e);
            return Result.error("设置默认配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的存储类型
     *
     * @return 存储类型列表
     */
    @GetMapping("/types")
    @RequireAdmin
    public Result<List<String>> getSupportedStorageTypes() {
        try {
            List<String> types = storageConfigService.getSupportedStorageTypes();
            return Result.success(types);
        } catch (Exception e) {
            log.error("获取存储类型失败: {}", e.getMessage(), e);
            return Result.error("获取存储类型失败: " + e.getMessage());
        }
    }
} 
