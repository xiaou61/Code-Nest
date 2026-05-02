package com.xiaou.filestorage.service;

import com.xiaou.filestorage.domain.FileInfo;

/**
 * 文件本地备份服务接口
 *
 * @author xiaou
 */
public interface FileBackupService {

    /**
     * 异步创建本地备份
     *
     * @param fileInfo 文件信息
     */
    void createLocalBackupAsync(FileInfo fileInfo);

    /**
     * 检查文件是否有本地备份
     *
     * @param fileId 文件ID
     * @return 是否存在本地备份
     */
    boolean hasLocalBackup(Long fileId);

    /**
     * 删除本地备份
     *
     * @param fileId 文件ID
     * @return 删除是否成功
     */
    boolean deleteLocalBackup(Long fileId);

    /**
     * 清理过期的本地备份
     *
     * @return 清理的文件数量
     */
    int cleanExpiredBackups();

    /**
     * 获取本地备份存储使用情况
     *
     * @return 使用情况（字节）
     */
    long getLocalBackupUsage();

    /**
     * 检查本地备份存储空间
     *
     * @return 是否有足够空间
     */
    boolean hasEnoughSpace(long fileSize);
}
