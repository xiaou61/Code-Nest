package com.xiaou.filestorage.service.impl;

import com.xiaou.filestorage.domain.FileInfo;
import com.xiaou.filestorage.domain.FileStorage;
import com.xiaou.filestorage.domain.StorageConfig;
import com.xiaou.filestorage.dto.FileUploadResult;
import com.xiaou.filestorage.event.FileOperationEventPublisher;
import com.xiaou.filestorage.factory.StorageStrategyFactory;
import com.xiaou.filestorage.mapper.FileInfoMapper;
import com.xiaou.filestorage.mapper.FileStorageMapper;
import com.xiaou.filestorage.mapper.StorageConfigMapper;
import com.xiaou.filestorage.service.FileBackupService;
import com.xiaou.filestorage.service.FileSystemSettingService;
import com.xiaou.filestorage.service.StorageHealthService;
import com.xiaou.filestorage.strategy.FileStorageStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceImplTest {

    private static final byte[] FILE_BYTES = "hello backup".getBytes(StandardCharsets.UTF_8);

    @Mock
    private FileInfoMapper fileInfoMapper;

    @Mock
    private FileStorageMapper fileStorageMapper;

    @Mock
    private StorageConfigMapper storageConfigMapper;

    @Mock
    private StorageStrategyFactory strategyFactory;

    @Mock
    private FileOperationEventPublisher eventPublisher;

    @Mock
    private FileSystemSettingService fileSystemSettingService;

    @Mock
    private FileBackupService fileBackupService;

    @Mock
    private StorageHealthService storageHealthService;

    @Mock
    private MultipartFile file;

    @Mock
    private FileStorageStrategy storageStrategy;

    @InjectMocks
    private FileStorageServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("avatar.txt");
        when(file.getSize()).thenReturn((long) FILE_BYTES.length);
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(FILE_BYTES));

        when(fileSystemSettingService.isFileTypeAllowed("txt")).thenReturn(true);
        when(fileSystemSettingService.isFileSizeExceeded((long) FILE_BYTES.length)).thenReturn(false);

        StorageConfig config = new StorageConfig();
        config.setId(1L);
        config.setStorageType("LOCAL");
        config.setConfigName("本地默认存储");
        config.setConfigParams("{}");
        config.setIsEnabled(1);
        config.setIsDefault(1);
        when(storageConfigMapper.selectDefault()).thenReturn(config);

        when(strategyFactory.createAndInitialize(eq(1L), eq("LOCAL"), anyMap())).thenReturn(storageStrategy);
        when(storageStrategy.uploadFile(eq(file), isNull()))
                .thenReturn(FileUploadResult.success("stored/avatar.txt", "/files/stored/avatar.txt", (long) FILE_BYTES.length));

        when(fileInfoMapper.selectByMd5(any())).thenReturn(null);
        when(fileInfoMapper.insert(any(FileInfo.class))).thenAnswer(invocation -> {
            FileInfo fileInfo = invocation.getArgument(0);
            fileInfo.setId(42L);
            return 1;
        });
        when(fileStorageMapper.insert(any(FileStorage.class))).thenReturn(1);
    }

    @Test
    void uploadSingleShouldSkipLocalBackupWhenAutoBackupDisabled() {
        lenient().when(fileSystemSettingService.isAutoBackupEnabled()).thenReturn(false);

        FileUploadResult result = service.uploadSingle(file, "profile", "avatar");

        assertTrue(result.isSuccess());
        verify(fileBackupService, never()).createLocalBackupAsync(any(FileInfo.class));
    }

    @Test
    void uploadSingleShouldCreateLocalBackupWhenAutoBackupEnabled() {
        lenient().when(fileSystemSettingService.isAutoBackupEnabled()).thenReturn(true);

        FileUploadResult result = service.uploadSingle(file, "profile", "avatar");

        assertTrue(result.isSuccess());
        verify(fileBackupService).createLocalBackupAsync(any(FileInfo.class));
    }

    @Test
    void uploadSingleShouldUsePublicDefaultAccessLevelWhenConfigured() {
        lenient().when(fileSystemSettingService.getDefaultAccessLevel()).thenReturn("public");

        FileUploadResult result = service.uploadSingle(file, "profile", "avatar");

        ArgumentCaptor<FileInfo> fileInfoCaptor = ArgumentCaptor.forClass(FileInfo.class);
        assertTrue(result.isSuccess());
        verify(fileInfoMapper).insert(fileInfoCaptor.capture());
        assertEquals(1, fileInfoCaptor.getValue().getIsPublic());
    }

    @Test
    void uploadSingleShouldUsePrivateDefaultAccessLevelWhenConfigured() {
        lenient().when(fileSystemSettingService.getDefaultAccessLevel()).thenReturn("private");

        FileUploadResult result = service.uploadSingle(file, "profile", "avatar");

        ArgumentCaptor<FileInfo> fileInfoCaptor = ArgumentCaptor.forClass(FileInfo.class);
        assertTrue(result.isSuccess());
        verify(fileInfoMapper).insert(fileInfoCaptor.capture());
        assertEquals(0, fileInfoCaptor.getValue().getIsPublic());
    }
}
