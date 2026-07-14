package com.xiaou.filestorage.strategy;

import com.xiaou.filestorage.dto.FileUploadResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractFileStorageStrategyTest {

    @Test
    void shouldInvalidateInitializedStateWhenReinitializationThrows() {
        ProbeStorageStrategy strategy = new ProbeStorageStrategy();

        assertTrue(strategy.initialize(Map.of("endpoint", "first")));
        assertTrue(strategy.testConnection());
        assertEquals(1, strategy.connectionChecks);

        strategy.throwOnInitialize = true;

        assertFalse(strategy.initialize(Map.of("endpoint", "broken")));
        assertFalse(strategy.testConnection());
        assertEquals(1, strategy.connectionChecks);
    }

    private static final class ProbeStorageStrategy extends AbstractFileStorageStrategy {

        private boolean throwOnInitialize;
        private int connectionChecks;

        @Override
        public String getStorageType() {
            return "PROBE";
        }

        @Override
        protected boolean doInitialize(Map<String, Object> configParams) {
            if (throwOnInitialize) {
                throw new IllegalStateException("invalid configuration");
            }
            return true;
        }

        @Override
        protected boolean doTestConnection() {
            connectionChecks++;
            return true;
        }

        @Override
        public FileUploadResult uploadFile(MultipartFile file, String storagePath) {
            return null;
        }

        @Override
        public FileUploadResult uploadFile(InputStream inputStream, String fileName,
                                          String storagePath, String contentType) {
            return null;
        }

        @Override
        public InputStream downloadFile(String storagePath) {
            return null;
        }

        @Override
        public boolean deleteFile(String storagePath) {
            return false;
        }

        @Override
        public boolean existsFile(String storagePath) {
            return false;
        }

        @Override
        public String getFileUrl(String storagePath, Integer expireHours) {
            return null;
        }

        @Override
        public boolean copyFile(String sourceStoragePath, String targetStoragePath) {
            return false;
        }

        @Override
        public Long getFileSize(String storagePath) {
            return null;
        }
    }
}
