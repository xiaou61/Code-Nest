package com.xiaou.filestorage.strategy.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.xiaou.filestorage.dto.FileUploadResult;
import com.xiaou.filestorage.strategy.AbstractFileStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * 本地存储策略实现
 *
 * @author xiaou
 */
@Slf4j
@Component
public class LocalStorageStrategy extends AbstractFileStorageStrategy {

    private String basePath;
    private String urlPrefix;

    @Override
    public String getStorageType() {
        return "LOCAL";
    }

    @Override
    protected boolean doInitialize(Map<String, Object> configParams) {
        this.basePath = getConfigParam("basePath", "/uploads");
        this.urlPrefix = getConfigParam("urlPrefix", "http://localhost:9999/api/files");

        // 创建基础目录
        try {
            Path baseDir = Paths.get(basePath);
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }
            return true;
        } catch (Exception e) {
            log.error("创建本地存储目录失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected boolean doTestConnection() {
        try {
            Path baseDir = Paths.get(basePath);
            return Files.exists(baseDir) && Files.isWritable(baseDir);
        } catch (Exception e) {
            log.error("测试本地存储连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public FileUploadResult uploadFile(MultipartFile file, String storagePath) {
        try {
            if (StrUtil.isBlank(storagePath)) {
                storagePath = generateStoragePath(file.getOriginalFilename(), "default", "upload");
            }

            Path targetPath = Paths.get(basePath, storagePath);
            
            // 创建目录
            Files.createDirectories(targetPath.getParent());
            
            // 保存文件
            file.transferTo(targetPath.toFile());
            
            // 生成访问URL
            String accessUrl = urlPrefix + "/" + storagePath.replace("\\", "/");
            long fileSize = Files.size(targetPath);
            
            return FileUploadResult.success(storagePath, accessUrl, fileSize);
            
        } catch (Exception e) {
            log.error("本地上传文件失败: {}", e.getMessage(), e);
            return FileUploadResult.failure("上传文件失败: " + e.getMessage());
        }
    }

    @Override
    public FileUploadResult uploadFile(InputStream inputStream, String fileName, String storagePath, String contentType) {
        try {
            if (StrUtil.isBlank(storagePath)) {
                storagePath = generateStoragePath(fileName, "default", "upload");
            }

            Path targetPath = Paths.get(basePath, storagePath);
            
            // 创建目录
            Files.createDirectories(targetPath.getParent());
            
            // 保存文件
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 生成访问URL
            String accessUrl = urlPrefix + "/" + storagePath.replace("\\", "/");
            
            // 获取文件大小
            long fileSize = Files.size(targetPath);
            
            return FileUploadResult.success(storagePath, accessUrl, fileSize);
            
        } catch (Exception e) {
            log.error("本地上传文件流失败: {}", e.getMessage(), e);
            return FileUploadResult.failure("上传文件流失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String storagePath) {
        try {
            Path filePath = Paths.get(basePath, storagePath);
            if (!Files.exists(filePath)) {
                return null;
            }
            return new FileInputStream(filePath.toFile());
        } catch (Exception e) {
            log.error("本地下载文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean deleteFile(String storagePath) {
        try {
            Path filePath = Paths.get(basePath, storagePath);
            return Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.error("本地删除文件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean existsFile(String storagePath) {
        try {
            Path filePath = Paths.get(basePath, storagePath);
            return Files.exists(filePath);
        } catch (Exception e) {
            log.error("本地检查文件存在性失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFileUrl(String storagePath, Integer expireHours) {
        // 本地存储不需要临时URL，直接返回永久URL
        return urlPrefix + "/" + storagePath.replace("\\", "/");
    }

    @Override
    public boolean copyFile(String sourceStoragePath, String targetStoragePath) {
        try {
            Path sourcePath = Paths.get(basePath, sourceStoragePath);
            Path targetPath = Paths.get(basePath, targetStoragePath);
            
            if (!Files.exists(sourcePath)) {
                return false;
            }
            
            // 创建目标目录
            Files.createDirectories(targetPath.getParent());
            
            // 复制文件
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
            
        } catch (Exception e) {
            log.error("本地复制文件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Long getFileSize(String storagePath) {
        try {
            Path filePath = Paths.get(basePath, storagePath);
            if (!Files.exists(filePath)) {
                return null;
            }
            return Files.size(filePath);
        } catch (Exception e) {
            log.error("本地获取文件大小失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
