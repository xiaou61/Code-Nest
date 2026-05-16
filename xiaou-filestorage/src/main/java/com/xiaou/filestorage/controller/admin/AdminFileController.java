package com.xiaou.filestorage.controller.admin;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.utils.JsonUtils;
import com.xiaou.filestorage.domain.FileInfo;
import com.xiaou.filestorage.domain.StorageConfig;
import com.xiaou.filestorage.factory.StorageStrategyFactory;
import com.xiaou.filestorage.mapper.FileInfoMapper;
import com.xiaou.filestorage.mapper.StorageConfigMapper;
import com.xiaou.filestorage.service.FileStorageService;
import com.xiaou.filestorage.service.FileStatisticsService;
import com.xiaou.filestorage.strategy.FileStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员文件管理控制器
 *
 * @author xiaou
 */
@Slf4j
@RestController
@RequestMapping("/admin/file")
public class AdminFileController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileStatisticsService fileStatisticsService;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private StorageConfigMapper storageConfigMapper;

    @Autowired
    private StorageStrategyFactory strategyFactory;

    /**
     * 管理员查询文件列表(支持全量查看)
     *
     * @param moduleName   模块名称
     * @param businessType 业务类型
     * @param pageNum      页码
     * @param pageSize     页大小
     * @return 文件列表
     */
    @GetMapping("/list")
    @RequireAdmin
    public Result<Map<String, Object>> listFiles(@RequestParam(value = "moduleName", required = false) String moduleName,
                                                 @RequestParam(value = "businessType", required = false) String businessType,
                                                 @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        try {
            Map<String, Object> result = fileStorageService.listFiles(moduleName, businessType, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询文件列表失败: {}", e.getMessage(), e);
            return Result.error("查询文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 物理删除文件
     *
     * @param id 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}/force")
    @RequireAdmin
    public Result<Boolean> forceDeleteFile(@PathVariable("id") Long id) {
        try {
            FileInfo fileInfo = fileInfoMapper.selectById(id);
            if (fileInfo == null) {
                return Result.success(true); // 文件不存在，视为删除成功
            }

            // 获取默认存储配置
            StorageConfig defaultConfig = storageConfigMapper.selectDefault();
            if (defaultConfig != null) {
                // 从存储中删除文件
                Map<String, Object> configParams = JsonUtils.parseMap(defaultConfig.getConfigParams());
                FileStorageStrategy strategy = strategyFactory.createAndInitialize(
                    defaultConfig.getId(), defaultConfig.getStorageType(), configParams);
                
                if (strategy != null) {
                    strategy.deleteFile(fileInfo.getStoredName());
                }
            }

            // 从数据库中物理删除记录
            int deleted = fileInfoMapper.deleteById(id);
            
            if (deleted > 0) {
                log.info("物理删除文件成功: fileId={}, fileName={}", id, fileInfo.getOriginalName());
                return Result.success(true);
            } else {
                return Result.error("删除数据库记录失败");
            }
            
        } catch (Exception e) {
            log.error("物理删除文件失败: fileId={}, error={}", id, e.getMessage(), e);
            return Result.error("物理删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 移动文件到其他存储
     *
     * @param id              文件ID
     * @param targetStorageId 目标存储配置ID
     * @return 移动结果
     */
    @PutMapping("/{id}/move")
    @RequireAdmin
    public Result<Boolean> moveFile(@PathVariable("id") Long id, @RequestParam("targetStorageId") Long targetStorageId) {
        try {
            FileInfo fileInfo = fileInfoMapper.selectById(id);
            if (fileInfo == null) {
                return Result.error("文件不存在");
            }

            // 获取源存储配置
            StorageConfig sourceConfig = storageConfigMapper.selectDefault();
            if (sourceConfig == null) {
                return Result.error("未找到源存储配置");
            }

            // 获取目标存储配置
            StorageConfig targetConfig = storageConfigMapper.selectById(targetStorageId);
            if (targetConfig == null || targetConfig.getIsEnabled() != 1) {
                return Result.error("目标存储配置不可用");
            }

            // 初始化源存储策略
            Map<String, Object> sourceConfigParams = JsonUtils.parseMap(sourceConfig.getConfigParams());
            FileStorageStrategy sourceStrategy = strategyFactory.createAndInitialize(
                sourceConfig.getId(), sourceConfig.getStorageType(), sourceConfigParams);

            // 初始化目标存储策略
            Map<String, Object> targetConfigParams = JsonUtils.parseMap(targetConfig.getConfigParams());
            FileStorageStrategy targetStrategy = strategyFactory.createAndInitialize(
                targetConfig.getId(), targetConfig.getStorageType(), targetConfigParams);

            if (sourceStrategy == null || targetStrategy == null) {
                return Result.error("存储策略初始化失败");
            }

            // 从源存储下载文件
            String originalStoredName = fileInfo.getStoredName();
            java.io.InputStream fileStream = sourceStrategy.downloadFile(originalStoredName);
            if (fileStream == null) {
                return Result.error("从源存储下载文件失败");
            }

            try {
                // 上传到目标存储
                com.xiaou.filestorage.dto.FileUploadResult uploadResult = targetStrategy.uploadFile(
                    fileStream, fileInfo.getOriginalName(), fileInfo.getStoredName(), fileInfo.getContentType());
                
                if (!uploadResult.isSuccess()) {
                    return Result.error("上传到目标存储失败: " + uploadResult.getErrorMessage());
                }

                // 更新数据库记录
                fileInfo.setStoredName(uploadResult.getStoragePath());
                fileInfo.setAccessUrl(uploadResult.getAccessUrl());
                fileInfo.setUpdateTime(new java.util.Date());
                
                int updated = fileInfoMapper.updateById(fileInfo);
                if (updated > 0) {
                    // 删除源存储文件
                    sourceStrategy.deleteFile(originalStoredName);
                    
                    log.info("文件移动成功: fileId={}, from={} to={}", id, sourceConfig.getStorageType(), targetConfig.getStorageType());
                    return Result.success(true);
                } else {
                    // 更新数据库失败，删除目标存储中的文件
                    targetStrategy.deleteFile(uploadResult.getStoragePath());
                    return Result.error("更新数据库记录失败");
                }
                
            } finally {
                try {
                    fileStream.close();
                } catch (Exception e) {
                    log.warn("关闭文件流失败: {}", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("移动文件失败: fileId={}, targetStorageId={}, error={}", id, targetStorageId, e.getMessage(), e);
            return Result.error("移动文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    @RequireAdmin
    public Result<Map<String, Object>> getFileStatistics() {
        try {
            Map<String, Object> statistics = fileStatisticsService.getFileStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取文件统计失败: {}", e.getMessage(), e);
            return Result.error("获取文件统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取存储使用情况
     *
     * @return 使用情况
     */
    @GetMapping("/storage-usage")
    @RequireAdmin
    public Result<Map<String, Object>> getStorageUsage() {
        try {
            Map<String, Object> usage = fileStatisticsService.getStorageUsage();
            return Result.success(usage);
        } catch (Exception e) {
            log.error("获取存储使用情况失败: {}", e.getMessage(), e);
            return Result.error("获取存储使用情况失败: " + e.getMessage());
        }
    }
}
