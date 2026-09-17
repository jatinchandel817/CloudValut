package com.Project.DistributedFileStorage.repository;

import com.Project.DistributedFileStorage.entity.FileStorageMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileStorageMappingRepository extends JpaRepository<FileStorageMapping, Long> {

    List<FileStorageMapping> findByFileId(String fileId);

    List<FileStorageMapping> findByFileIdAndReplicationStatus(String fileId, String replicationStatus);
}
