package com.xiaou.filestorage.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.filestorage.domain.FileSystemSetting;
import com.xiaou.filestorage.mapper.FileSystemSettingMapper;
import com.xiaou.filestorage.service.FileSystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件系统设置服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
public class FileSystemSettingServiceImpl implements FileSystemSettingService {

    @Autowired
    private FileSystemSettingMapper fileSystemSettingMapper;

    // 缓存设置值，避免频繁查询数据库
    private Map<String, String> settingCache = new HashMap<>();
    private long lastCacheTime = 0;
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000; // 5分钟缓存

    @Override
    public Map<String, String> getSystemSettings() {
        try {
            refreshCacheIfNeeded();
            return new HashMap<>(settingCache);
        } catch (Exception e) {
            log.error("获取系统设置失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public String getSettingValue(String settingKey) {
        return getSettingValue(settingKey, null);
    }

    @Override
    public String getSettingValue(String settingKey, String defaultValue) {
        try {
            refreshCacheIfNeeded();
            String value = settingCache.get(settingKey);
            return StrUtil.isNotBlank(value) ? value : defaultValue;
        } catch (Exception e) {
            log.error("获取设置值失败: settingKey={}, error={}", settingKey, e.getMessage(), e);
            return defaultValue;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSystemSettings(Map<String, String> settings) {
        try {
            boolean allSuccess = true;
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                boolean success = updateSetting(entry.getKey(), entry.getValue());
                if (!success) {
                    allSuccess = false;
                }
            }
            
            if (allSuccess) {
                clearCache();
            }
            
            return allSuccess;
        } catch (Exception e) {
            log.error("批量更新系统设置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSetting(String settingKey, String settingValue) {
        try {
            FileSystemSetting existing = fileSystemSettingMapper.selectByKey(settingKey);
            
            if (existing != null) {
                existing.setSettingValue(settingValue);
                existing.setUpdateTime(new Date());
                int updated = fileSystemSettingMapper.updateById(existing);
                
                if (updated > 0) {
                    clearCache();
                    return true;
                }
            } else {
                // 创建新设置
                FileSystemSetting setting = new FileSystemSetting();
                setting.setSettingKey(settingKey);
                setting.setSettingValue(settingValue);
                setting.setSettingDesc("系统自动创建");
                setting.setCreateTime(new Date());
                setting.setUpdateTime(new Date());
                
                int inserted = fileSystemSettingMapper.insert(setting);
                
                if (inserted > 0) {
                    clearCache();
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            log.error("更新系统设置失败: settingKey={}, error={}", settingKey, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<String> getAllowedFileTypes() {
        try {
            String fileTypesJson = getSettingValue("ALLOWED_FILE_TYPES", "[]");
            return JSONUtil.toList(fileTypesJson, String.class);
        } catch (Exception e) {
            log.error("获取允许文件类型失败: {}", e.getMessage(), e);
            // 返回默认允许的文件类型
            return List.of("jpg", "jpeg", "png", "gif", "pdf", "txt", "doc", "docx");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFileTypes(List<String> fileTypes) {
        try {
            String fileTypesJson = JSONUtil.toJsonStr(fileTypes);
            return updateSetting("ALLOWED_FILE_TYPES", fileTypesJson);
        } catch (Exception e) {
            log.error("更新文件类型白名单失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public long getMaxFileSize() {
        try {
            String maxSizeStr = getSettingValue("MAX_FILE_SIZE", "104857600"); // 默认100MB
            return Long.parseLong(maxSizeStr);
        } catch (Exception e) {
            log.error("获取最大文件大小失败: {}", e.getMessage(), e);
            return 104857600L; // 100MB
        }
    }

    @Override
    public boolean isFileTypeAllowed(String fileExtension) {
        try {
            if (StrUtil.isBlank(fileExtension)) {
                return false;
            }
            
            List<String> allowedTypes = getAllowedFileTypes();
            String ext = fileExtension.toLowerCase();
            if (ext.startsWith(".")) {
                ext = ext.substring(1);
            }
            
            return allowedTypes.contains(ext);
        } catch (Exception e) {
            log.error("检查文件类型失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isFileSizeExceeded(long fileSize) {
        try {
            long maxSize = getMaxFileSize();
            return fileSize > maxSize;
        } catch (Exception e) {
            log.error("检查文件大小失败: {}", e.getMessage(), e);
            return true; // 出错时默认超限，更安全
        }
    }

    @Override
    public long getModuleStorageQuota() {
        try {
            String quotaStr = getSettingValue("STORAGE_QUOTA_PER_MODULE", "10737418240"); // 默认10GB
            return Long.parseLong(quotaStr);
        } catch (Exception e) {
            log.error("获取模块存储配额失败: {}", e.getMessage(), e);
            return 10737418240L; // 10GB
        }
    }

    @Override
    public int getTempLinkExpireHours() {
        try {
            String hoursStr = getSettingValue("TEMP_LINK_EXPIRE_HOURS", "24"); // 默认24小时
            return Integer.parseInt(hoursStr);
        } catch (Exception e) {
            log.error("获取临时链接有效期失败: {}", e.getMessage(), e);
            return 24; // 24小时
        }
    }

    @Override
    public boolean isAutoBackupEnabled() {
        try {
            String enabledStr = getSettingValue("AUTO_BACKUP_ENABLED", "true");
            return "true".equalsIgnoreCase(enabledStr);
        } catch (Exception e) {
            log.error("获取自动备份配置失败: {}", e.getMessage(), e);
            return true; // 默认启用
        }
    }

    @Override
    public String getDefaultAccessLevel() {
        try {
            String accessLevel = getSettingValue("DEFAULT_ACCESS_LEVEL",
                    getSettingValue("defaultAccessLevel", "private"));
            return "public".equalsIgnoreCase(accessLevel) ? "public" : "private";
        } catch (Exception e) {
            log.error("获取默认访问权限失败: {}", e.getMessage(), e);
            return "private";
        }
    }

    /**
     * 刷新缓存（如果需要）
     */
    private void refreshCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheTime > CACHE_EXPIRE_TIME) {
            refreshCache();
        }
    }

    /**
     * 刷新缓存
     */
    private void refreshCache() {
        try {
            List<FileSystemSetting> settings = fileSystemSettingMapper.selectAll();
            Map<String, String> newCache = new HashMap<>();
            
            for (FileSystemSetting setting : settings) {
                newCache.put(setting.getSettingKey(), setting.getSettingValue());
            }
            
            this.settingCache = newCache;
            this.lastCacheTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            log.error("刷新设置缓存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 清除缓存
     */
    private void clearCache() {
        this.lastCacheTime = 0;
    }
}
