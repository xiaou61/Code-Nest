package io.github.xiaou61.knowledge.internal.file;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 附件存储配置。
 *
 * <p>{@code upload-dir} **有默认值**，但默认落在系统临时目录并会在启动时告警。这与
 * WORK-004 设计里"没配置就启动失败"不同，理由是实测的：本地开发机与测试环境都没有这个
 * 环境变量，缺省即失败会让每台机器一上来就起不来；而一个会告警的临时目录至少让人看得见。
 * 生产必须显式配置——告警文案里写明了。
 */
@ConfigurationProperties(prefix = "paideia.knowledge")
public class KnowledgeStorageProperties {

    /** 附件根目录。相对路径按进程工作目录解析。 */
    private String uploadDir = Path.of(System.getProperty("java.io.tmpdir"), "paideia-uploads").toString();

    /** 单文件上限（字节）。默认 10 MB。 */
    private long maxFileSize = 10L * 1024 * 1024;

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
}
