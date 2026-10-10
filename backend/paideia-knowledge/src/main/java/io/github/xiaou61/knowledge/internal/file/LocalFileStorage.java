package io.github.xiaou61.knowledge.internal.file;

import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地目录实现：文件写在配置指定的目录下，文件名就是 UUID。
 *
 * <p><b>为什么文件名必须是 UUID 而不是自增序号</b>：读取端点免鉴权（用户裁决），
 * 自增就等于把全部附件按 {@code /files/1}、{@code /files/2} 列出来。
 *
 * <p><b>先写临时文件再原子改名</b>：直接写目标文件的话，进程在写到一半时挂掉会留下一个
 * 内容残缺但"看起来存在"的文件。临时文件 + {@code ATOMIC_MOVE} 让"存在"就意味着"完整"。
 *
 * <p><b>天花板</b>：单实例、本地磁盘。多实例部署时这个目录在各实例上是不同的，
 * 必须换共享存储——这就是端口存在的理由。
 */
public class LocalFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    private final Path root;

    public LocalFileStorage(Path root) {
        this.root = root;
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            // 启动即失败好过第一次上传时才发现目录不可写
            throw new IllegalStateException("附件目录不可用：" + root + "（" + exception.getMessage() + "）", exception);
        }
        if (!Files.isWritable(root)) {
            throw new IllegalStateException("附件目录不可写：" + root);
        }
    }

    @Override
    public String put(byte[] content, String contentType) {
        String id = UUID.randomUUID().toString();
        Path target = root.resolve(id);
        Path temporary = root.resolve(id + ".part");
        try {
            Files.write(temporary, content);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupFailure) {
                log.warn("清理临时附件失败：{}", temporary, cleanupFailure);
            }
            throw new BizException(ErrorCode.INTERNAL, "附件写入失败：" + root);
        }
        return id;
    }

    @Override
    public Optional<byte[]> read(String id) {
        if (!isUuid(id)) {
            // id 来自数据库，理论上是自己写进去的；仍然校一次形状，避免任何"拼接路径"的可能
            return Optional.empty();
        }
        Path target = root.resolve(id);
        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(target));
        } catch (IOException exception) {
            log.warn("读取附件失败：{}", target, exception);
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String id) {
        if (!isUuid(id)) {
            return false;
        }
        try {
            return Files.deleteIfExists(root.resolve(id));
        } catch (IOException exception) {
            log.warn("删除附件失败：{}", id, exception);
            return false;
        }
    }

    private static boolean isUuid(String value) {
        if (value == null || value.length() != 36) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
