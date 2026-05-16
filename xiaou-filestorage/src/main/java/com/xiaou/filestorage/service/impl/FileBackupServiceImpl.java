package com.xiaou.filestorage.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.xiaou.filestorage.domain.FileInfo;
import com.xiaou.filestorage.domain.FileStorage;
import com.xiaou.filestorage.domain.StorageConfig;
import com.xiaou.filestorage.factory.StorageStrategyFactory;
import com.xiaou.filestorage.mapper.FileStorageMapper;
import com.xiaou.filestorage.mapper.StorageConfigMapper;
import com.xiaou.filestorage.service.FileBackupService;
import com.xiaou.filestorage.service.FileSystemSettingService;
import com.xiaou.filestorage.strategy.FileStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

/**
 * 文件本地备份服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
public class FileBackupServiceImpl implements FileBackupService {

    @Autowired
    private FileStorageMapper fileStorageMapper;

    @Autowired
    private StorageConfigMapper storageConfigMapper;

    @Autowired
    private StorageStrategyFactory strategyFactory;

    @Autowired
    private FileSystemSettingService fileSystemSettingService;

    private static final String BACKUP_ROOT_PATH = "/data/file-backup";

    @Override
    @Async("fileBackupExecutor")
    public void createLocalBackupAsync(FileInfo fileInfo) {
        try {
            // 简化逻辑：检查是否已有本地备份
            if (hasLocalBackup(fileInfo.getId())) {
                log.debug("文件已存在本地备份: fileId={}", fileInfo.getId());
                return;
            }

            // 直接创建本地备份（简化逻辑：从原始文件路径复制）
            log.info("开始创建本地备份: fileId={}", fileInfo.getId());

            // 创建本地备份文件路径
            String backupDir = BACKUP_ROOT_PATH + File.separator + fileInfo.getModuleName();
            FileUtil.mkdir(backupDir);
            
            String backupFileName = fileInfo.getId() + "_" + fileInfo.getStoredName();
            String backupPath = backupDir + File.separator + backupFileName;

            // 简化逻辑：假设原始文件已经在本地上传目录，直接复制
            String originalPath = System.getProperty("user.dir") + "/uploads/" + fileInfo.getStoredName();
            File originalFile = new File(originalPath);
            
            if (originalFile.exists()) {
                // 直接复制文件到备份目录
                FileUtil.copy(originalFile, new File(backupPath), true);
                log.info("从本地文件创建备份: {} -> {}", originalPath, backupPath);
            } else {
                log.warn("原始文件不存在，跳过备份: {}", originalPath);
                return;
            }

            // 创建备份存储记录（本地存储配置ID假设为1）
            FileStorage backupStorage = new FileStorage();
            backupStorage.setFileId(fileInfo.getId());
            backupStorage.setStorageConfigId(1L); // 本地存储配置ID
            backupStorage.setStoragePath(backupPath);
            backupStorage.setIsPrimary(0); // 备份存储
            backupStorage.setSyncStatus(1); // 已同步
            backupStorage.setCreateTime(new Date());
            backupStorage.setUpdateTime(new Date());

            int inserted = fileStorageMapper.insert(backupStorage);
            if (inserted > 0) {
                log.info("本地备份创建成功: fileId={}, backupPath={}", fileInfo.getId(), backupPath);
                return;
            }

        } catch (Exception e) {
            log.error("创建本地备份失败: fileId={}, error={}", fileInfo.getId(), e.getMessage(), e);
        }
    }

    @Override
    public boolean hasLocalBackup(Long fileId) {
        try {
            // 查询是否有本地存储的备份记录（假设本地存储配置ID为1）
            return fileStorageMapper.selectByFileId(fileId).stream()
                .anyMatch(storage -> storage.getStorageConfigId().equals(1L) && storage.getIsPrimary() == 0);
        } catch (Exception e) {
            log.error("检查本地备份失败: fileId={}, error={}", fileId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteLocalBackup(Long fileId) {
        try {
            // 查找本地备份记录
            FileStorage backupStorage = fileStorageMapper.selectByFileId(fileId).stream()
                .filter(storage -> storage.getStorageConfigId().equals(1L) && storage.getIsPrimary() == 0)
                .findFirst()
                .orElse(null);

            if (backupStorage != null) {
                // 删除本地文件
                if (StrUtil.isNotBlank(backupStorage.getStoragePath())) {
                    File backupFile = new File(backupStorage.getStoragePath());
                    if (backupFile.exists()) {
                        FileUtil.del(backupFile);
                    }
                }

                // 删除存储记录
                fileStorageMapper.deleteById(backupStorage.getId());
                
                log.info("本地备份删除成功: fileId={}", fileId);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("删除本地备份失败: fileId={}, error={}", fileId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int cleanExpiredBackups() {
        try {
            log.info("开始清理过期备份");
            
            // 获取备份保留天数配置（默认30天）
            int retentionDays = 30; // 默认30天，可以从配置文件读取
            Date cutoffTime = new Date(System.currentTimeMillis() - retentionDays * 24 * 60 * 60 * 1000L);
            
            // 1. 查询过期的备份记录
            int cleanedCount = 0;
            
            // 遍历备份目录，清理过期文件
            File backupRoot = new File(BACKUP_ROOT_PATH);
            if (backupRoot.exists() && backupRoot.isDirectory()) {
                cleanedCount = cleanDirectoryRecursively(backupRoot, cutoffTime);
            }
            
            log.info("过期备份清理完成，清理了 {} 个文件", cleanedCount);
            return cleanedCount;
            
        } catch (Exception e) {
            log.error("清理过期备份失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 递归清理目录中的过期文件
     *
     * @param directory 目录
     * @param cutoffTime 截止时间
     * @return 清理的文件数量
     */
    private int cleanDirectoryRecursively(File directory, Date cutoffTime) {
        int cleanedCount = 0;
        
        File[] files = directory.listFiles();
        if (files == null) {
            return cleanedCount;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                // 递归处理子目录
                cleanedCount += cleanDirectoryRecursively(file, cutoffTime);
                
                // 如果子目录为空，删除它
                if (file.listFiles() == null || file.listFiles().length == 0) {
                    if (file.delete()) {
                        log.debug("删除空目录: {}", file.getPath());
                    }
                }
            } else {
                // 检查文件最后修改时间
                long lastModified = file.lastModified();
                if (lastModified < cutoffTime.getTime()) {
                    if (file.delete()) {
                        log.debug("删除过期备份文件: {}", file.getPath());
                        cleanedCount++;
                    } else {
                        log.warn("无法删除过期备份文件: {}", file.getPath());
                    }
                }
            }
        }
        
        return cleanedCount;
    }

    @Override
    public long getLocalBackupUsage() {
        try {
            File backupRoot = new File(BACKUP_ROOT_PATH);
            if (backupRoot.exists() && backupRoot.isDirectory()) {
                return FileUtil.size(backupRoot);
            }
            return 0;
        } catch (Exception e) {
            log.error("获取本地备份使用情况失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean hasEnoughSpace(long fileSize) {
        try {
            File backupRoot = new File(BACKUP_ROOT_PATH);
            long freeSpace = backupRoot.getFreeSpace();
            long currentUsage = getLocalBackupUsage();
            long maxUsage = fileSystemSettingService.getModuleStorageQuota() * 2; // 本地备份配额为模块配额的2倍
            
            return (currentUsage + fileSize) <= maxUsage && freeSpace > fileSize;
        } catch (Exception e) {
            log.error("检查备份空间失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
