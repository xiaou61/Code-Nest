package com.xiaou.filestorage.dto;

import lombok.Data;

/**
 * 文件上传结果DTO
 *
 * @author xiaou
 */
@Data
public class FileUploadResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 存储路径
     */
    private String storagePath;

    /**
     * 访问URL
     */
    private String accessUrl;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 实际使用的存储配置ID
     */
    private Long storageConfigId;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建成功结果
     *
     * @param storagePath 存储路径
     * @param accessUrl   访问URL
     * @param fileSize    文件大小
     * @return 上传结果
     */
    public static FileUploadResult success(String storagePath, String accessUrl, Long fileSize) {
        FileUploadResult result = new FileUploadResult();
        result.setSuccess(true);
        result.setStoragePath(storagePath);
        result.setAccessUrl(accessUrl);
        result.setFileSize(fileSize);
        return result;
    }

    /**
     * 创建失败结果
     *
     * @param errorMessage 错误信息
     * @return 上传结果
     */
    public static FileUploadResult failure(String errorMessage) {
        FileUploadResult result = new FileUploadResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
} 
