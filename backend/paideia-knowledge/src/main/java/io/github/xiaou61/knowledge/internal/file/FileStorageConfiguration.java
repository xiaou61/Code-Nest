package io.github.xiaou61.knowledge.internal.file;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 附件存储装配：把端口绑到本地目录实现，并把生效目录打出来。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KnowledgeStorageProperties.class)
public class FileStorageConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FileStorageConfiguration.class);

    @Bean
    FileStorage fileStorage(KnowledgeStorageProperties properties) {
        Path root = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
        if (System.getProperty("java.io.tmpdir") != null
                && root.startsWith(Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize())) {
            log.warn("""
                    附件目录未显式配置 paideia.knowledge.upload-dir，正在使用系统临时目录：{}
                    临时目录会被系统清理、也不适合备份；部署到任何真实环境前必须显式配置。
                    """, root);
        } else {
            log.info("附件目录：{}", root);
        }
        return new LocalFileStorage(root);
    }
}
