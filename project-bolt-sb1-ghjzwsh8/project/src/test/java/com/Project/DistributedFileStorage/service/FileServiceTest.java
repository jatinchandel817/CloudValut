package com.Project.DistributedFileStorage.service;

import com.Project.DistributedFileStorage.dto.FileResponse;
import com.Project.DistributedFileStorage.dto.FileUploadResponse;
import com.Project.DistributedFileStorage.entity.File;
import com.Project.DistributedFileStorage.entity.StorageNode;
import com.Project.DistributedFileStorage.exception.FileNotFoundException;
import com.Project.DistributedFileStorage.exception.InvalidFileException;
import com.Project.DistributedFileStorage.repository.FileRepository;
import com.Project.DistributedFileStorage.repository.FileStorageMappingRepository;
import com.Project.DistributedFileStorage.repository.StorageNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FileService.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;
    @Mock
    private FileStorageMappingRepository mappingRepository;
    @Mock
    private StorageNodeRepository storageNodeRepository;
    @Mock
    private StorageNodeService storageNodeService;
    @Mock
    private LocalStorageService localStorageService;

    @InjectMocks
    private FileService fileService;

    @Test
    void uploadFile_validFile_returnsSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes());

        StorageNode node = StorageNode.builder()
                .id(1L)
                .nodeName("Local-Node-1")
                .storageType("LOCAL")
                .status("ACTIVE")
                .build();

        when(storageNodeService.selectStorageNode()).thenReturn(node);
        when(storageNodeService.getStorageService("LOCAL")).thenReturn(localStorageService);
        when(localStorageService.uploadFile(any(), any())).thenReturn("/uploads/test.txt");
        when(storageNodeService.selectReplicaNode("LOCAL")).thenReturn(Optional.empty());

        FileUploadResponse response = fileService.uploadFile(file);

        assertTrue(response.isSuccess());
        assertEquals("test.txt", response.getOriginalName());
        assertEquals("LOCAL", response.getStorageType());
        verify(fileRepository, times(1)).save(any(File.class));
    }

    @Test
    void uploadFile_emptyFile_throwsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        assertThrows(InvalidFileException.class, () -> fileService.uploadFile(file));
    }

    @Test
    void uploadFile_nullFile_throwsInvalidFileException() {
        assertThrows(InvalidFileException.class, () -> fileService.uploadFile(null));
    }

    @Test
    void uploadFile_pathTraversalFilename_throwsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../etc/passwd", "text/plain", "Malicious".getBytes());

        assertThrows(InvalidFileException.class, () -> fileService.uploadFile(file));
    }

    @Test
    void getFileById_existingFile_returnsResponse() {
        File file = File.builder()
                .id("123")
                .filename("uuid_test.txt")
                .originalName("test.txt")
                .filePath("/uploads/test.txt")
                .fileSize(100L)
                .fileType("text/plain")
                .storageType("LOCAL")
                .status("ACTIVE")
                .build();

        when(fileRepository.findById("123")).thenReturn(Optional.of(file));

        FileResponse response = fileService.getFileById("123");

        assertEquals("123", response.getId());
        assertEquals("test.txt", response.getOriginalName());
    }

    @Test
    void getFileById_nonExistentFile_throwsFileNotFoundException() {
        when(fileRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileService.getFileById("nonexistent"));
    }

    @Test
    void deleteFile_existingFile_marksAsDeleted() {
        File file = File.builder()
                .id("123")
                .filename("uuid_test.txt")
                .originalName("test.txt")
                .filePath("/uploads/test.txt")
                .fileSize(100L)
                .storageType("LOCAL")
                .status("ACTIVE")
                .build();

        when(fileRepository.findById("123")).thenReturn(Optional.of(file));
        when(mappingRepository.findByFileId("123")).thenReturn(List.of());
        when(fileRepository.save(any(File.class))).thenReturn(file);

        fileService.deleteFile("123");

        assertEquals("DELETED", file.getStatus());
        verify(fileRepository, times(1)).save(any(File.class));
    }

    @Test
    void deleteFile_nonExistentFile_throwsFileNotFoundException() {
        when(fileRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileService.deleteFile("nonexistent"));
    }

    @Test
    void searchFiles_returnsMatchingFiles() {
        File file1 = File.builder()
                .id("1")
                .originalName("resume.pdf")
                .fileSize(5000L)
                .storageType("LOCAL")
                .status("ACTIVE")
                .build();

        when(fileRepository.searchByOriginalName("resume")).thenReturn(List.of(file1));

        List<FileResponse> results = fileService.searchFiles("resume");

        assertEquals(1, results.size());
        assertEquals("resume.pdf", results.get(0).getOriginalName());
    }

    @Test
    void getAllFiles_excludesDeletedFiles() {
        File activeFile = File.builder()
                .id("1")
                .originalName("active.txt")
                .fileSize(100L)
                .storageType("LOCAL")
                .status("ACTIVE")
                .build();

        File deletedFile = File.builder()
                .id("2")
                .originalName("deleted.txt")
                .fileSize(100L)
                .storageType("LOCAL")
                .status("DELETED")
                .build();

        when(fileRepository.findAll()).thenReturn(List.of(activeFile, deletedFile));

        List<FileResponse> results = fileService.getAllFiles();

        assertEquals(1, results.size());
        assertEquals("active.txt", results.get(0).getOriginalName());
    }
}
