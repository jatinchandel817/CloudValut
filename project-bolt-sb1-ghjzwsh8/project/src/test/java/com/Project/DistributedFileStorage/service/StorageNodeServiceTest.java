package com.Project.DistributedFileStorage.service;

import com.Project.DistributedFileStorage.entity.StorageNode;
import com.Project.DistributedFileStorage.repository.StorageNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StorageNodeService, focusing on the node selection algorithm.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class StorageNodeServiceTest {

    @Mock
    private StorageNodeRepository storageNodeRepository;
    @Mock
    private LocalStorageService localStorageService;
    @Mock
    private S3StorageService s3StorageService;
    @Mock
    private GCSStorageService gcsStorageService;

    @InjectMocks
    private StorageNodeService storageNodeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageNodeService, "enabledProviders", "LOCAL");
    }

    @Test
    void selectStorageNode_picksHighestAvailableSpace() {
        StorageNode smallNode = StorageNode.builder()
                .id(1L).nodeName("Small").storageType("LOCAL")
                .availableSpace(1_000_000L).usedSpace(0L).status("ACTIVE").build();
        StorageNode bigNode = StorageNode.builder()
                .id(2L).nodeName("Big").storageType("LOCAL")
                .availableSpace(5_000_000L).usedSpace(0L).status("ACTIVE").build();

        when(storageNodeRepository.findByStatus("ACTIVE"))
                .thenReturn(List.of(smallNode, bigNode));

        StorageNode selected = storageNodeService.selectStorageNode();

        assertEquals("Big", selected.getNodeName());
    }

    @Test
    void selectStorageNode_cloudNodeTreatedAsUnlimited() {
        StorageNode localNode = StorageNode.builder()
                .id(1L).nodeName("Local").storageType("LOCAL")
                .availableSpace(10_000_000L).usedSpace(0L).status("ACTIVE").build();
        StorageNode s3Node = StorageNode.builder()
                .id(2L).nodeName("S3").storageType("S3")
                .availableSpace(null).usedSpace(0L).status("ACTIVE").build();

        when(storageNodeRepository.findByStatus("ACTIVE"))
                .thenReturn(List.of(localNode, s3Node));

        StorageNode selected = storageNodeService.selectStorageNode();

        // S3 has null available space = unlimited = Long.MAX_VALUE, so it should be selected
        assertEquals("S3", selected.getNodeName());
    }

    @Test
    void selectStorageNode_noActiveNodes_throwsException() {
        when(storageNodeRepository.findByStatus("ACTIVE")).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> storageNodeService.selectStorageNode());
    }

    @Test
    void selectReplicaNode_returnsDifferentType() {
        StorageNode localNode = StorageNode.builder()
                .id(1L).nodeName("Local").storageType("LOCAL")
                .availableSpace(10_000_000L).usedSpace(0L).status("ACTIVE").build();
        StorageNode s3Node = StorageNode.builder()
                .id(2L).nodeName("S3").storageType("S3")
                .availableSpace(null).usedSpace(0L).status("ACTIVE").build();

        when(storageNodeRepository.findByStatus("ACTIVE"))
                .thenReturn(List.of(localNode, s3Node));

        Optional<StorageNode> replica = storageNodeService.selectReplicaNode("LOCAL");

        assertTrue(replica.isPresent());
        assertEquals("S3", replica.get().getStorageType());
    }

    @Test
    void selectReplicaNode_noDifferentType_returnsEmpty() {
        StorageNode localNode = StorageNode.builder()
                .id(1L).nodeName("Local").storageType("LOCAL")
                .availableSpace(10_000_000L).usedSpace(0L).status("ACTIVE").build();

        when(storageNodeRepository.findByStatus("ACTIVE"))
                .thenReturn(List.of(localNode));

        Optional<StorageNode> replica = storageNodeService.selectReplicaNode("LOCAL");

        assertTrue(replica.isEmpty());
    }

    @Test
    void getStorageService_localReturnsLocalStorageService() {
        assertEquals(localStorageService, storageNodeService.getStorageService("LOCAL"));
    }

    @Test
    void getStorageService_s3ReturnsS3StorageService() {
        assertEquals(s3StorageService, storageNodeService.getStorageService("S3"));
    }

    @Test
    void getStorageService_gcsReturnsGCSStorageService() {
        assertEquals(gcsStorageService, storageNodeService.getStorageService("GCS"));
    }

    @Test
    void getStorageService_unknownType_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> storageNodeService.getStorageService("UNKNOWN"));
    }
}
