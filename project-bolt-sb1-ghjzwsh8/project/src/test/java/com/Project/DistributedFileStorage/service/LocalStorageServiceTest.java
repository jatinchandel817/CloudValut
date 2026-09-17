package com.Project.DistributedFileStorage.service;

import com.Project.DistributedFileStorage.config.StorageConfig;
import com.Project.DistributedFileStorage.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LocalStorageService.
 */
class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService localStorageService;
    private StorageConfig storageConfig;

    @BeforeEach
    void setUp() throws IOException {
        storageConfig = mock(StorageConfig.class);
        when(storageConfig.getRootPath()).thenReturn(tempDir);
        localStorageService = new LocalStorageService(storageConfig);
    }

    @Test
    void uploadFile_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes());

        String storageKey = UUID.randomUUID() + "_test.txt";
        String result = localStorageService.uploadFile(file, storageKey);

        assertNotNull(result);
        assertTrue(Files.exists(Paths.get(result)));
    }

    @Test
    void uploadFile_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        String storageKey = UUID.randomUUID() + "_empty.txt";
        assertThrows(StorageException.class, () -> localStorageService.uploadFile(file, storageKey));
    }

    @Test
    void downloadFile_success() throws IOException {
        // First upload
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Download me".getBytes());
        String storageKey = UUID.randomUUID() + "_test.txt";
        String path = localStorageService.uploadFile(file, storageKey);

        // Then download
        var inputStream = localStorageService.downloadFile(path);
        assertNotNull(inputStream);
        String content = new String(inputStream.readAllBytes());
        assertEquals("Download me", content);
    }

    @Test
    void downloadFile_notFound_throwsException() {
        assertThrows(StorageException.class,
                () -> localStorageService.downloadFile("/nonexistent/path/file.txt"));
    }

    @Test
    void deleteFile_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "delete.txt", "text/plain", "Delete me".getBytes());
        String storageKey = UUID.randomUUID() + "_delete.txt";
        String path = localStorageService.uploadFile(file, storageKey);

        assertTrue(localStorageService.fileExists(path));
        localStorageService.deleteFile(path);
        assertFalse(localStorageService.fileExists(path));
    }

    @Test
    void fileExists_returnsFalseForNonExistent() {
        assertFalse(localStorageService.fileExists("/nonexistent/file.txt"));
    }

    @Test
    void getStorageType_returnsLocal() {
        assertEquals("LOCAL", localStorageService.getStorageType());
    }

    @Test
    void uploadFile_pathTraversal_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Malicious".getBytes());

        String storageKey = "../../../etc/passwd_test.txt";
        assertThrows(StorageException.class, () -> localStorageService.uploadFile(file, storageKey));
    }
}
