package com.xiaou.filestorage.service;

import com.xiaou.filestorage.domain.FileSystemSetting;

import java.util.List;
import java.util.Map;

/**
 * 文件系统设置服务接口
 *
 * @author xiaou
 */
public interface FileSystemSettingService {

    /**
     * 获取系统设置
     *
     * @return 设置映射
     */
    Map<String, String> getSystemSettings();

    /**
     * 获取单个设置值
     *
     * @param settingKey 设置键
     * @return 设置值
     */
    String getSettingValue(String settingKey);

    /**
     * 获取单个设置值（带默认值）
     *
     * @param settingKey   设置键
     * @param defaultValue 默认值
     * @return 设置值
     */
    String getSettingValue(String settingKey, String defaultValue);

    /**
     * 更新系统设置
     *
     * @param settings 设置映射
     * @return 更新是否成功
     */
    boolean updateSystemSettings(Map<String, String> settings);

    /**
     * 更新单个设置
     *
     * @param settingKey   设置键
     * @param settingValue 设置值
     * @return 更新是否成功
     */
    boolean updateSetting(String settingKey, String settingValue);

    /**
     * 获取允许的文件类型
     *
     * @return 文件类型列表
     */
    List<String> getAllowedFileTypes();

    /**
     * 更新文件类型白名单
     *
     * @param fileTypes 文件类型列表
     * @return 更新是否成功
     */
    boolean updateFileTypes(List<String> fileTypes);

    /**
     * 获取最大文件大小限制（字节）
     *
     * @return 最大文件大小
     */
    long getMaxFileSize();

    /**
     * 检查文件类型是否允许
     *
     * @param fileExtension 文件扩展名
     * @return 是否允许
     */
    boolean isFileTypeAllowed(String fileExtension);

    /**
     * 检查文件大小是否超限
     *
     * @param fileSize 文件大小
     * @return 是否超限
     */
    boolean isFileSizeExceeded(long fileSize);

    /**
     * 获取模块存储配额（字节）
     *
     * @return 配额大小
     */
    long getModuleStorageQuota();

    /**
     * 获取临时链接有效期（小时）
     *
     * @return 有效期小时数
     */
    int getTempLinkExpireHours();

    /**
     * 是否自动创建本地备份
     *
     * @return 是否自动备份
     */
    boolean isAutoBackupEnabled();

    /**
     * 获取默认访问权限
     *
     * @return 默认访问权限，public 或 private
     */
    String getDefaultAccessLevel();
}
