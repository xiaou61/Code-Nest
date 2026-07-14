package com.xiaou.filestorage.strategy.impl;

import com.xiaou.filestorage.dto.FileUploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageStrategyTest {

    private static final byte[] FILE_BYTES = "file-content".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path tempDir;

    private LocalStorageStrategy strategy;
    private Path baseDir;

    @BeforeEach
    void setUp() {
        baseDir = tempDir.resolve("base");
        strategy = new LocalStorageStrategy();

        assertTrue(strategy.initialize(Map.of(
                "basePath", baseDir.toString(),
                "urlPrefix", "http://localhost/files"
        )));
    }

    @Test
    void shouldKeepNormalFilesInsideBaseDirectory() throws Exception {
        FileUploadResult uploadResult = strategy.uploadFile(
                new ByteArrayInputStream(FILE_BYTES),
                "note.txt",
                "documents/note.txt",
                "text/plain"
        );

        assertTrue(uploadResult.isSuccess());
        assertTrue(Files.exists(baseDir.resolve("documents/note.txt")));
        assertTrue(strategy.existsFile("documents/note.txt"));
        assertEquals("http://localhost/files/documents/note.txt",
                strategy.getFileUrl("documents/note.txt", null));
        assertTrue(strategy.copyFile("documents/note.txt", "archive/note.txt"));
        assertTrue(strategy.existsFile("archive/note.txt"));
        assertEquals((long) FILE_BYTES.length, strategy.getFileSize("archive/note.txt"));

        try (InputStream inputStream = strategy.downloadFile("archive/note.txt")) {
            assertNotNull(inputStream);
            assertArrayEquals(FILE_BYTES, inputStream.readAllBytes());
        }

        assertTrue(strategy.deleteFile("archive/note.txt"));
        assertFalse(strategy.existsFile("archive/note.txt"));
    }

    @Test
    void shouldRejectTraversalForUploadAndLeaveOutsideFileUntouched() throws Exception {
        Path outsideFile = tempDir.resolve("outside.txt");

        FileUploadResult uploadResult = strategy.uploadFile(
                new MockMultipartFile("file", "note.txt", "text/plain", FILE_BYTES),
                "../outside.txt"
        );

        assertFalse(uploadResult.isSuccess());
        assertFalse(Files.exists(outsideFile));
    }

    @Test
    void shouldRejectAbsolutePathForStreamUpload() {
        Path outsideFile = tempDir.resolve("absolute.txt");

        FileUploadResult uploadResult = strategy.uploadFile(
                new ByteArrayInputStream(FILE_BYTES),
                "note.txt",
                outsideFile.toString(),
                "text/plain"
        );

        assertFalse(uploadResult.isSuccess());
        assertFalse(Files.exists(outsideFile));
    }

    @Test
    void shouldRejectSymbolicLinkPaths() throws Exception {
        Path outsideDirectory = tempDir.resolve("outside-directory");
        Files.createDirectories(outsideDirectory);
        Path linkedDirectory = baseDir.resolve("linked-directory");

        try {
            Files.createSymbolicLink(linkedDirectory, outsideDirectory);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            Assumptions.abort("当前文件系统不支持创建符号链接");
        }

        FileUploadResult uploadResult = strategy.uploadFile(
                new ByteArrayInputStream(FILE_BYTES),
                "note.txt",
                "linked-directory/note.txt",
                "text/plain"
        );

        assertFalse(uploadResult.isSuccess());
        assertFalse(Files.exists(outsideDirectory.resolve("note.txt")));
        assertFalse(strategy.deleteFile("linked-directory/note.txt"));
        assertFalse(strategy.existsFile("linked-directory/note.txt"));
    }

    @Test
    void shouldRejectUnsafePathsAcrossReadDeleteCopySizeAndUrlOperations() throws Exception {
        Path outsideFile = tempDir.resolve("outside.txt");
        Files.write(outsideFile, FILE_BYTES);
        Files.write(baseDir.resolve("inside.txt"), FILE_BYTES);

        try (InputStream inputStream = strategy.downloadFile("../outside.txt")) {
            assertNull(inputStream);
        }
        assertFalse(strategy.deleteFile("../outside.txt"));
        assertFalse(strategy.existsFile("../outside.txt"));
        assertNull(strategy.getFileSize("../outside.txt"));
        assertNull(strategy.getFileUrl("../outside.txt", null));
        assertFalse(strategy.deleteFile("."));
        assertTrue(Files.exists(baseDir));
        assertFalse(strategy.copyFile("../outside.txt", "copied.txt"));
        assertFalse(strategy.copyFile("inside.txt", "../copied.txt"));
        assertTrue(Files.exists(outsideFile));
        assertFalse(Files.exists(tempDir.resolve("copied.txt")));
    }
}
