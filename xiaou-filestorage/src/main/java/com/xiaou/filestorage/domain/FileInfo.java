package com.xiaou.filestorage.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 文件信息实体
 *
 * @author xiaou
 */
@Data
public class FileInfo {

    /**
     * 文件ID
     */
    private Long id;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储文件名
     */
    private String storedName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件MIME类型
     */
    private String contentType;

    /**
     * 文件MD5校验值
     */
    private String md5Hash;

    /**
     * 所属模块名称
     */
    private String moduleName;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 当前主存储配置ID
     */
    private Long storageConfigId;

    /**
     * 上传时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date uploadTime;

    /**
     * 访问URL
     */
    private String accessUrl;

    /**
     * 文件状态: 0=已删除, 1=正常
     */
    private Integer status;

    /**
     * 是否公开访问: 0=私有, 1=公开
     */
    private Integer isPublic;

    /**
     * 删除时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date deleteTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
} 
