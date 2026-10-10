package io.github.xiaou61.knowledge.internal.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 本地存储实现的读写与边界。 */
class LocalFileStorageTest {

    private static byte[] payload() {
        return "hello".getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void storesAndReadsBack(@TempDir Path root) throws IOException {
        LocalFileStorage storage = new LocalFileStorage(root);

        String id = storage.put(payload(), "image/png");

        assertThat(id).hasSize(36);
        assertThat(storage.read(id)).contains(payload());
        // 文件名就是 id，且目录里不留 .part 临时文件
        assertThat(root.resolve(id)).exists();
        assertThat(Files.list(root).filter(path -> path.toString().endsWith(".part")).count()).isZero();
    }

    @Test
    void eachPutGetsADistinctId(@TempDir Path root) {
        LocalFileStorage storage = new LocalFileStorage(root);

        assertThat(storage.put(payload(), "image/png")).isNotEqualTo(storage.put(payload(), "image/png"));
    }

    @Test
    void readReturnsEmptyForUnknownOrMalformedId(@TempDir Path root) {
        LocalFileStorage storage = new LocalFileStorage(root);
        storage.put(payload(), "image/png");

        assertThat(storage.read("not-a-uuid")).isEmpty();
        assertThat(storage.read("../../../etc/passwd")).isEmpty();
        assertThat(storage.read(null)).isEmpty();
        assertThat(storage.read("00000000-0000-0000-0000-000000000000")).isEmpty();
    }

    @Test
    void deleteReportsWhetherSomethingWasRemoved(@TempDir Path root) {
        LocalFileStorage storage = new LocalFileStorage(root);
        String id = storage.put(payload(), "image/png");

        assertThat(storage.delete(id)).isTrue();
        assertThat(storage.read(id)).isEmpty();
        assertThat(storage.delete(id)).isFalse();
        assertThat(storage.delete("not-a-uuid")).isFalse();
    }

    @Test
    void unwritableRootFailsAtConstructionRatherThanAtFirstUpload(@TempDir Path root) throws IOException {
        // 根路径是一个**文件**（不是目录）：createDirectories 会失败。
        // 构思期就失败好过第一次上传时才发现——那是用户遇到的第一件事。
        Path file = root.resolve("not-a-directory");
        Files.writeString(file, "x");

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() -> new LocalFileStorage(file.resolve("sub")));
        assertThat(failure).isInstanceOf(IllegalStateException.class);
        assertThat(failure).hasMessageContaining("附件目录");
    }

    @Test
    void readSurvivesAFileRemovedBehindItsBack(@TempDir Path root) throws IOException {
        LocalFileStorage storage = new LocalFileStorage(root);
        String id = storage.put(payload(), "image/png");
        Files.delete(root.resolve(id));

        // 运维清理、或换了实例（本地目录不共享）都会走到这里；按"不存在"返回而不是抛异常
        assertThat(storage.read(id)).isEmpty();
    }
}
