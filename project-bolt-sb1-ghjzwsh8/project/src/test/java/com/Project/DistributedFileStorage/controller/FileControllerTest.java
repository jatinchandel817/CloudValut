package com.Project.DistributedFileStorage.controller;

import com.Project.DistributedFileStorage.dto.FileResponse;
import com.Project.DistributedFileStorage.dto.FileUploadResponse;
import com.Project.DistributedFileStorage.dto.StorageStatusResponse;
import com.Project.DistributedFileStorage.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FileController using MockMvc.
 */
@WebMvcTest(FileController.class)
@ActiveProfiles("test")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @Test
    void uploadFile_validFile_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello".getBytes());

        FileUploadResponse response = FileUploadResponse.builder()
                .success(true)
                .message("File uploaded successfully")
                .fileId("test-id-123")
                .filename("uuid_test.txt")
                .originalName("test.txt")
                .fileSize(5L)
                .fileType("text/plain")
                .storageType("LOCAL")
                .storageNode("Local-Node-1")
                .status("ACTIVE")
                .replicationStatus("NONE")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(fileService.uploadFile(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.originalName").value("test.txt"));
    }

    @Test
    void getAllFiles_returnsList() throws Exception {
        FileResponse file = FileResponse.builder()
                .id("123")
                .originalName("test.txt")
                .fileSize(100L)
                .storageType("LOCAL")
                .status("ACTIVE")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(fileService.getAllFiles()).thenReturn(List.of(file));

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalName").value("test.txt"));
    }

    @Test
    void getFileById_existingFile_returns200() throws Exception {
        FileResponse file = FileResponse.builder()
                .id("123")
                .originalName("test.txt")
                .fileSize(100L)
                .storageType("LOCAL")
                .status("ACTIVE")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(fileService.getFileById("123")).thenReturn(file);

        mockMvc.perform(get("/api/files/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"));
    }

    @Test
    void deleteFile_existingFile_returns200() throws Exception {
        doNothing().when(fileService).deleteFile("123");

        mockMvc.perform(delete("/api/files/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void searchFiles_returnsResults() throws Exception {
        FileResponse file = FileResponse.builder()
                .id("123")
                .originalName("resume.pdf")
                .fileSize(5000L)
                .storageType("LOCAL")
                .status("ACTIVE")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(fileService.searchFiles("resume")).thenReturn(List.of(file));

        mockMvc.perform(get("/api/files/search").param("name", "resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalName").value("resume.pdf"));
    }

    @Test
    void getStorageStatus_returnsStatus() throws Exception {
        StorageStatusResponse status = StorageStatusResponse.builder()
                .totalFiles(5)
                .totalStorageUsed(50000L)
                .localFiles(3)
                .localStorageUsed(30000L)
                .s3Files(2)
                .s3StorageUsed(20000L)
                .gcsFiles(0)
                .gcsStorageUsed(0L)
                .nodes(List.of())
                .build();

        when(fileService.getStorageStatus()).thenReturn(status);

        mockMvc.perform(get("/api/files/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles").value(5));
    }
}
