package com.xiaou.filestorage.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.xiaou.filestorage.dto.FileUploadResult;
import com.xiaou.filestorage.strategy.AbstractFileStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
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

    private String urlPrefix;
    private Path baseDirectory;

    @Override
    public String getStorageType() {
        return "LOCAL";
    }

    @Override
    protected boolean doInitialize(Map<String, Object> configParams) {
        String configuredBasePath = getConfigParam("basePath", "/uploads");
        this.urlPrefix = getConfigParam("urlPrefix", "http://localhost:9999/files");

        // 创建基础目录
        try {
            Path baseDir = Paths.get(configuredBasePath).toAbsolutePath().normalize();
            Files.createDirectories(baseDir);
            this.baseDirectory = baseDir.toRealPath();
            return true;
        } catch (Exception e) {
            log.error("创建本地存储目录失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected boolean doTestConnection() {
        try {
            return baseDirectory != null
                    && Files.exists(baseDirectory)
                    && Files.isWritable(baseDirectory);
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

            Path targetPath = resolveStoragePath(storagePath);
            
            // 创建目录
            Files.createDirectories(targetPath.getParent());
            
            // 保存文件
            file.transferTo(targetPath.toFile());
            
            // 生成访问URL
            String normalizedStoragePath = toStoragePath(targetPath);
            String accessUrl = urlPrefix + "/" + normalizedStoragePath;
            long fileSize = Files.size(targetPath);
            
            return FileUploadResult.success(normalizedStoragePath, accessUrl, fileSize);
            
        } catch (IllegalArgumentException e) {
            log.warn("本地上传文件路径被拒绝: {}", e.getMessage());
            return FileUploadResult.failure("上传文件失败: " + e.getMessage());
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

            Path targetPath = resolveStoragePath(storagePath);
            
            // 创建目录
            Files.createDirectories(targetPath.getParent());
            
            // 保存文件
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 生成访问URL
            String normalizedStoragePath = toStoragePath(targetPath);
            String accessUrl = urlPrefix + "/" + normalizedStoragePath;
            
            // 获取文件大小
            long fileSize = Files.size(targetPath);
            
            return FileUploadResult.success(normalizedStoragePath, accessUrl, fileSize);
            
        } catch (IllegalArgumentException e) {
            log.warn("本地上传文件流路径被拒绝: {}", e.getMessage());
            return FileUploadResult.failure("上传文件流失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("本地上传文件流失败: {}", e.getMessage(), e);
            return FileUploadResult.failure("上传文件流失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String storagePath) {
        try {
            Path filePath = resolveStoragePath(storagePath);
            if (!Files.exists(filePath)) {
                return null;
            }
            return new FileInputStream(filePath.toFile());
        } catch (IllegalArgumentException e) {
            log.warn("本地下载文件路径被拒绝: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("本地下载文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean deleteFile(String storagePath) {
        try {
            Path filePath = resolveStoragePath(storagePath);
            return Files.deleteIfExists(filePath);
        } catch (IllegalArgumentException e) {
            log.warn("本地删除文件路径被拒绝: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("本地删除文件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean existsFile(String storagePath) {
        try {
            Path filePath = resolveStoragePath(storagePath);
            return Files.exists(filePath);
        } catch (IllegalArgumentException e) {
            log.warn("本地检查文件路径被拒绝: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("本地检查文件存在性失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFileUrl(String storagePath, Integer expireHours) {
        try {
            // 本地存储不需要临时URL，直接返回永久URL
            return urlPrefix + "/" + toStoragePath(resolveStoragePath(storagePath));
        } catch (Exception e) {
            log.warn("本地生成文件URL失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean copyFile(String sourceStoragePath, String targetStoragePath) {
        try {
            Path sourcePath = resolveStoragePath(sourceStoragePath);
            Path targetPath = resolveStoragePath(targetStoragePath);
            
            if (!Files.exists(sourcePath)) {
                return false;
            }
            
            // 创建目标目录
            Files.createDirectories(targetPath.getParent());
            
            // 复制文件
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
            
        } catch (IllegalArgumentException e) {
            log.warn("本地复制文件路径被拒绝: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("本地复制文件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Long getFileSize(String storagePath) {
        try {
            Path filePath = resolveStoragePath(storagePath);
            if (!Files.exists(filePath)) {
                return null;
            }
            return Files.size(filePath);
        } catch (IllegalArgumentException e) {
            log.warn("本地获取文件大小路径被拒绝: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("本地获取文件大小失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private Path resolveStoragePath(String storagePath) {
        if (StrUtil.isBlank(storagePath)) {
            throw new IllegalArgumentException("存储路径不能为空");
        }

        Path requestedPath = Paths.get(storagePath);
        if (requestedPath.isAbsolute()) {
            throw new IllegalArgumentException("存储路径必须是相对路径");
        }

        Path resolvedPath = baseDirectory.resolve(requestedPath).normalize();
        if (!resolvedPath.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("存储路径超出基础目录");
        }
        if (resolvedPath.equals(baseDirectory)) {
            throw new IllegalArgumentException("存储路径不能指向基础目录");
        }

        Path currentPath = baseDirectory;
        for (Path pathPart : baseDirectory.relativize(resolvedPath)) {
            currentPath = currentPath.resolve(pathPart);
            if (Files.isSymbolicLink(currentPath)) {
                throw new IllegalArgumentException("存储路径不能包含符号链接");
            }
        }
        return resolvedPath;
    }

    private String toStoragePath(Path resolvedPath) {
        return baseDirectory.relativize(resolvedPath).toString().replace("\\", "/");
    }
}
