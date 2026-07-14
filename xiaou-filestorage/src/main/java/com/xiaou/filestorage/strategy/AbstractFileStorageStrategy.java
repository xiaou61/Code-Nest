package com.xiaou.filestorage.strategy;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

/**
 * 抽象文件存储策略 - 模板方法模式
 *
 * @author xiaou
 */
@Slf4j
public abstract class AbstractFileStorageStrategy implements FileStorageStrategy {

    /**
     * 配置参数
     */
    protected Map<String, Object> configParams;

    /**
     * 初始化状态
     */
    protected boolean initialized = false;

    @Override
    public boolean initialize(Map<String, Object> configParams) {
        this.initialized = false;
        try {
            this.configParams = configParams;
            boolean result = doInitialize(configParams);
            this.initialized = result;
            return result;
        } catch (Exception e) {
            this.initialized = false;
            log.error("初始化存储策略失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean testConnection() {
        if (!initialized) {
            return false;
        }
        try {
            return doTestConnection();
        } catch (Exception e) {
            log.error("测试存储连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 生成存储路径
     *
     * @param originalFileName 原始文件名
     * @param moduleName      模块名称
     * @param businessType    业务类型
     * @return 存储路径
     */
    protected String generateStoragePath(String originalFileName, String moduleName, String businessType) {
        String extension = FileUtil.extName(originalFileName);
        String fileName = System.currentTimeMillis() + "_" + generateRandomString();
        if (StrUtil.isNotBlank(extension)) {
            fileName = fileName + "." + extension;
        }
        
        String datePath = cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyy/MM/dd");
        return String.format("%s/%s/%s/%s", moduleName, businessType, datePath, fileName);
    }

    /**
     * 生成随机字符串
     *
     * @return 随机字符串
     */
    protected String generateRandomString() {
        return cn.hutool.core.util.IdUtil.simpleUUID().substring(0, 8);
    }

    /**
     * 计算文件MD5
     *
     * @param inputStream 文件流
     * @return MD5值
     */
    protected String calculateMd5(InputStream inputStream) {
        try {
            return DigestUtil.md5Hex(inputStream);
        } catch (Exception e) {
            log.error("计算文件MD5失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取配置参数
     *
     * @param key 参数键
     * @return 参数值
     */
    protected String getConfigParam(String key) {
        if (configParams == null) {
            return null;
        }
        Object value = configParams.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取配置参数
     *
     * @param key          参数键
     * @param defaultValue 默认值
     * @return 参数值
     */
    protected String getConfigParam(String key, String defaultValue) {
        String value = getConfigParam(key);
        return StrUtil.isNotBlank(value) ? value : defaultValue;
    }

    /**
     * 子类实现具体的初始化逻辑
     *
     * @param configParams 配置参数
     * @return 初始化是否成功
     */
    protected abstract boolean doInitialize(Map<String, Object> configParams);

    /**
     * 子类实现具体的连接测试逻辑
     *
     * @return 连接是否正常
     */
    protected abstract boolean doTestConnection();
}
