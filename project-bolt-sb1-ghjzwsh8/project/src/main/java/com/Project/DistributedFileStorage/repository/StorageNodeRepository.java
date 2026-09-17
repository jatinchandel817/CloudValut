package com.Project.DistributedFileStorage.repository;

import com.Project.DistributedFileStorage.entity.StorageNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageNodeRepository extends JpaRepository<StorageNode, Long> {

    Optional<StorageNode> findByNodeName(String nodeName);

    List<StorageNode> findByStatus(String status);

    List<StorageNode> findByStorageType(String storageType);
}
