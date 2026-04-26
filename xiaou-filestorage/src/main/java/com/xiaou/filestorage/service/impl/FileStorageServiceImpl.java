package com.xiaou.filestorage.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.xiaou.common.utils.JsonUtils;
import com.xiaou.filestorage.domain.FileInfo;
import com.xiaou.filestorage.domain.StorageConfig;
import com.xiaou.filestorage.dto.FileUploadResult;
import com.xiaou.filestorage.factory.StorageStrategyFactory;
import com.xiaou.filestorage.event.FileOperationEventPublisher;
import com.xiaou.filestorage.mapper.FileInfoMapper;
import com.xiaou.filestorage.mapper.StorageConfigMapper;
import com.xiaou.filestorage.service.FileBackupService;
import com.xiaou.filestorage.service.FileStorageService;
import com.xiaou.filestorage.service.FileSystemSettingService;
import com.xiaou.filestorage.service.StorageHealthService;
import com.xiaou.filestorage.strategy.FileStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件存储服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private StorageConfigMapper storageConfigMapper;

    @Autowired
    private StorageStrategyFactory strategyFactory;

    @Autowired
    private FileOperationEventPublisher eventPublisher;

    @Autowired
    private FileSystemSettingService fileSystemSettingService;

    @Autowired
    private FileBackupService fileBackupService;

    @Autowired
    private StorageHealthService storageHealthService;

    private final Tika tika = new Tika();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResult uploadSingle(MultipartFile file, String moduleName, String businessType) {
        try {
            // 参数校验
            if (file == null || file.isEmpty()) {
                return FileUploadResult.failure("文件不能为空");
            }
            if (StrUtil.isBlank(moduleName)) {
                return FileUploadResult.failure("模块名称不能为空");
            }

            // 文件基本信息
            String originalName = file.getOriginalFilename();
            long fileSize = file.getSize();
            String contentType = detectContentType(file);

            // 文件类型校验
            String fileExtension = cn.hutool.core.io.FileUtil.extName(originalName);
            if (!fileSystemSettingService.isFileTypeAllowed(fileExtension)) {
                return FileUploadResult.failure("不支持的文件类型: " + fileExtension);
            }

            // 文件大小校验
            if (fileSystemSettingService.isFileSizeExceeded(fileSize)) {
                long maxSize = fileSystemSettingService.getMaxFileSize();
                return FileUploadResult.failure("文件大小超限，最大允许: " + formatFileSize(maxSize));
            }

            // 计算MD5
            String md5Hash = DigestUtil.md5Hex(file.getInputStream());

            // 检查文件是否已存在
            FileInfo existingFile = fileInfoMapper.selectByMd5(md5Hash);
            if (existingFile != null && existingFile.getStatus() == 1) {
                // 文件已存在，直接返回
                return FileUploadResult.success(existingFile.getStoredName(), 
                    existingFile.getAccessUrl(), existingFile.getFileSize());
            }

            // 获取默认存储配置并尝试上传
            StorageConfig defaultConfig = storageConfigMapper.selectDefault();
            FileUploadResult uploadResult;
            
            if (defaultConfig != null && defaultConfig.getIsEnabled() == 1) {
                // 尝试默认存储
                uploadResult = tryUploadWithStorage(defaultConfig, file);
                if (uploadResult != null && uploadResult.isSuccess()) {
                    // 主存储上传成功
                } else {
                    log.warn("主存储上传失败，切换到本地存储");
                    // 主存储失败，直接使用本地存储
                    uploadResult = tryLocalStorage(file);
                }
            } else {
                // 没有默认配置，直接使用本地存储
                log.info("未找到默认存储配置，使用本地存储");
                uploadResult = tryLocalStorage(file);
            }
            
            if (uploadResult == null || !uploadResult.isSuccess()) {
                return uploadResult != null ? uploadResult : FileUploadResult.failure("文件上传失败");
            }

            // 保存文件信息到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setOriginalName(originalName);
            fileInfo.setStoredName(uploadResult.getStoragePath());
            fileInfo.setFileSize(fileSize);
            fileInfo.setContentType(contentType);
            fileInfo.setMd5Hash(md5Hash);
            fileInfo.setModuleName(moduleName);
            fileInfo.setBusinessType(businessType);
            fileInfo.setUploadTime(new Date());
            fileInfo.setAccessUrl(uploadResult.getAccessUrl());
            fileInfo.setStatus(1);
            fileInfo.setIsPublic(0);
            fileInfo.setCreateTime(new Date());
            fileInfo.setUpdateTime(new Date());

            int inserted = fileInfoMapper.insert(fileInfo);
            if (inserted > 0) {
                // 发布上传事件
                eventPublisher.publishUploadEvent(fileInfo.getId(), originalName, moduleName, businessType, fileSize);
                
                // 异步创建本地备份
                fileBackupService.createLocalBackupAsync(fileInfo);
                
                return uploadResult;
            } else {
                // 数据库保存失败，删除已上传的文件
                // 注：这里可能需要根据实际使用的存储策略来删除文件，暂时跳过
                log.warn("数据库保存失败，建议手动清理文件: {}", uploadResult.getStoragePath());
                return FileUploadResult.failure("保存文件信息失败");
            }

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return FileUploadResult.failure("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public List<FileUploadResult> uploadBatch(MultipartFile[] files, String moduleName, String businessType) {
        List<FileUploadResult> results = new ArrayList<>();
        
        if (files == null || files.length == 0) {
            return results;
        }

        for (MultipartFile file : files) {
            FileUploadResult result = uploadSingle(file, moduleName, businessType);
            results.add(result);
        }

        return results;
    }

    @Override
    public InputStream downloadFile(Long fileId) {
        try {
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null || fileInfo.getStatus() != 1) {
                return null;
            }

            // 尝试从默认存储下载，失败则尝试本地存储
            StorageConfig defaultConfig = storageConfigMapper.selectDefault();
            if (defaultConfig != null && defaultConfig.getIsEnabled() == 1) {
                InputStream result = tryDownloadWithStorage(defaultConfig, fileInfo.getStoredName());
                if (result != null) {
                    return result;
                }
                log.warn("主存储下载失败，尝试本地存储: fileId={}", fileId);
            }
            
            // 尝试本地存储下载
            return tryDownloadFromLocal(fileInfo.getStoredName());

        } catch (Exception e) {
            log.error("文件下载失败: fileId={}, error={}", fileId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getFileInfo(Long fileId) {
        try {
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null || fileInfo.getStatus() != 1) {
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", fileInfo.getId());
            result.put("originalName", fileInfo.getOriginalName());
            result.put("fileSize", fileInfo.getFileSize());
            result.put("contentType", fileInfo.getContentType());
            result.put("uploadTime", fileInfo.getUploadTime());
            result.put("accessUrl", fileInfo.getAccessUrl());

            return result;

        } catch (Exception e) {
            log.error("获取文件信息失败: fileId={}, error={}", fileId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getFileUrl(Long fileId, Integer expireHours) {
        try {
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null || fileInfo.getStatus() != 1) {
                return null;
            }

            // 尝试从默认存储获取URL，失败则尝试本地存储
            StorageConfig defaultConfig = storageConfigMapper.selectDefault();
            if (defaultConfig != null && defaultConfig.getIsEnabled() == 1) {
                String result = tryGetUrlWithStorage(defaultConfig, fileInfo.getStoredName(), expireHours);
                if (result != null) {
                    return result;
                }
                log.warn("主存储获取URL失败，尝试本地存储: fileId={}", fileId);
            }
            
            // 尝试本地存储获取URL
            return tryGetUrlFromLocal(fileInfo.getStoredName(), expireHours);

        } catch (Exception e) {
            log.error("获取文件URL失败: fileId={}, error={}", fileId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Map<Long, String> getFileUrls(List<Long> fileIds) {
        Map<Long, String> result = new HashMap<>();
        
        if (CollectionUtil.isEmpty(fileIds)) {
            return result;
        }

        for (Long fileId : fileIds) {
            String url = getFileUrl(fileId, null);
            if (url != null) {
                result.put(fileId, url);
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(Long fileId, String moduleName) {
        try {
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo == null) {
                return true; // 文件不存在，视为删除成功
            }

            // 权限检查
            if (!fileInfo.getModuleName().equals(moduleName)) {
                log.warn("无权限删除文件: fileId={}, moduleName={}, fileModule={}", 
                    fileId, moduleName, fileInfo.getModuleName());
                return false;
            }

            // 逻辑删除
            int deleted = fileInfoMapper.logicalDeleteById(fileId);
            if (deleted > 0) {
                // 发布删除事件
                eventPublisher.publishDeleteEvent(fileId, fileInfo.getOriginalName(), moduleName);
            }
            return deleted > 0;

        } catch (Exception e) {
            log.error("删除文件失败: fileId={}, error={}", fileId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> listFiles(String moduleName, String businessType, Integer pageNum, Integer pageSize) {
        try {
            List<FileInfo> files = fileInfoMapper.selectByCondition(moduleName, businessType, 1, null, null);
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", files);
            result.put("total", files.size());
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);

            return result;

        } catch (Exception e) {
            log.error("查询文件列表失败: error={}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<Long, Boolean> checkFilesExists(List<Long> fileIds) {
        Map<Long, Boolean> result = new HashMap<>();
        
        if (CollectionUtil.isEmpty(fileIds)) {
            return result;
        }

        try {
            List<FileInfo> existingFiles = fileInfoMapper.selectByIds(fileIds);
            Set<Long> existingIds = existingFiles.stream()
                .filter(f -> f.getStatus() == 1)
                .map(FileInfo::getId)
                .collect(Collectors.toSet());

            for (Long fileId : fileIds) {
                result.put(fileId, existingIds.contains(fileId));
            }

        } catch (Exception e) {
            log.error("检查文件存在性失败: error={}", e.getMessage(), e);
            // 发生异常时，默认返回不存在
            for (Long fileId : fileIds) {
                result.put(fileId, false);
            }
        }

        return result;
    }

    /**
     * 检测文件内容类型
     *
     * @param file 文件
     * @return 内容类型
     */
    private String detectContentType(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (StrUtil.isNotBlank(contentType)) {
                return contentType;
            }
            
            // 使用Tika检测文件类型
            return tika.detect(file.getInputStream(), file.getOriginalFilename());
        } catch (Exception e) {
            log.warn("检测文件类型失败: {}", e.getMessage());
            return "application/octet-stream";
        }
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 尝试使用指定存储配置上传文件
     *
     * @param storageConfig 存储配置
     * @param file         文件
     * @return 上传结果
     */
    private FileUploadResult tryUploadWithStorage(StorageConfig storageConfig, MultipartFile file) {
        try {
            // 解析配置参数
            Map<String, Object> configParams = JsonUtils.parseMap(storageConfig.getConfigParams());
            
            // 获取存储策略
            FileStorageStrategy strategy = strategyFactory.createAndInitialize(
                storageConfig.getId(), storageConfig.getStorageType(), configParams);
                
            if (strategy == null) {
                log.error("初始化存储策略失败: configId={}", storageConfig.getId());
                return null;
            }

            // 执行上传
            return strategy.uploadFile(file, null);
            
        } catch (Exception e) {
            log.error("存储上传异常: configId={}, error={}", storageConfig.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 使用本地存储上传文件
     *
     * @param file 文件
     * @return 上传结果
     */
    private FileUploadResult tryLocalStorage(MultipartFile file) {
        try {
            // 获取或创建本地存储配置
            FileStorageStrategy localStrategy = strategyFactory.getStrategy("LOCAL");
            if (localStrategy == null) {
                log.error("本地存储策略不可用");
                return FileUploadResult.failure("本地存储策略不可用");
            }
            
            // 初始化本地存储配置
            Map<String, Object> localParams = new HashMap<>();
            localParams.put("basePath", System.getProperty("user.dir") + "/uploads");
            localParams.put("urlPrefix", "http://localhost:8080/files");
            
            boolean initialized = localStrategy.initialize(localParams);
            if (!initialized) {
                log.error("本地存储初始化失败");
                return FileUploadResult.failure("本地存储初始化失败");
            }
            
            // 执行本地上传
            FileUploadResult result = localStrategy.uploadFile(file, null);
            if (result.isSuccess()) {
                log.info("本地存储上传成功: {}", result.getStoragePath());
            } else {
                log.error("本地存储上传失败: {}", result.getErrorMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("本地存储上传异常: {}", e.getMessage(), e);
            return FileUploadResult.failure("本地存储上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 尝试使用指定存储配置下载文件
     *
     * @param storageConfig 存储配置
     * @param storagePath  存储路径
     * @return 文件流
     */
    private InputStream tryDownloadWithStorage(StorageConfig storageConfig, String storagePath) {
        try {
            Map<String, Object> configParams = JsonUtils.parseMap(storageConfig.getConfigParams());
            FileStorageStrategy strategy = strategyFactory.createAndInitialize(
                storageConfig.getId(), storageConfig.getStorageType(), configParams);
                
            if (strategy == null) {
                return null;
            }

            return strategy.downloadFile(storagePath);
            
        } catch (Exception e) {
            log.error("存储下载异常: configId={}, error={}", storageConfig.getId(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从本地存储下载文件
     *
     * @param storagePath 存储路径
     * @return 文件流
     */
    private InputStream tryDownloadFromLocal(String storagePath) {
        try {
            FileStorageStrategy localStrategy = strategyFactory.getStrategy("LOCAL");
            if (localStrategy == null) {
                return null;
            }
            
            Map<String, Object> localParams = new HashMap<>();
            localParams.put("basePath", System.getProperty("user.dir") + "/uploads");
            localParams.put("urlPrefix", "http://localhost:8080/files");
            
            boolean initialized = localStrategy.initialize(localParams);
            if (!initialized) {
                return null;
            }
            
            return localStrategy.downloadFile(storagePath);
            
        } catch (Exception e) {
            log.error("本地存储下载异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 尝试使用指定存储配置获取文件URL
     *
     * @param storageConfig 存储配置
     * @param storagePath  存储路径
     * @param expireHours  过期小时数
     * @return 文件URL
     */
    private String tryGetUrlWithStorage(StorageConfig storageConfig, String storagePath, Integer expireHours) {
        try {
            Map<String, Object> configParams = JsonUtils.parseMap(storageConfig.getConfigParams());
            FileStorageStrategy strategy = strategyFactory.createAndInitialize(
                storageConfig.getId(), storageConfig.getStorageType(), configParams);
                
            if (strategy == null) {
                return null;
            }

            return strategy.getFileUrl(storagePath, expireHours);
            
        } catch (Exception e) {
            log.error("存储获取URL异常: configId={}, error={}", storageConfig.getId(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从本地存储获取文件URL
     *
     * @param storagePath 存储路径
     * @param expireHours 过期小时数
     * @return 文件URL
     */
    private String tryGetUrlFromLocal(String storagePath, Integer expireHours) {
        try {
            FileStorageStrategy localStrategy = strategyFactory.getStrategy("LOCAL");
            if (localStrategy == null) {
                return null;
            }
            
            Map<String, Object> localParams = new HashMap<>();
            localParams.put("basePath", System.getProperty("user.dir") + "/uploads");
            localParams.put("urlPrefix", "http://localhost:8080/files");
            
            boolean initialized = localStrategy.initialize(localParams);
            if (!initialized) {
                return null;
            }
            
            return localStrategy.getFileUrl(storagePath, expireHours);
            
        } catch (Exception e) {
            log.error("本地存储获取URL异常: {}", e.getMessage(), e);
            return null;
        }
    }
}
