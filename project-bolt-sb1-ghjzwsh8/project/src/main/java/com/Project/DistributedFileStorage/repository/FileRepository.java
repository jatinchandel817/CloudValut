package com.Project.DistributedFileStorage.repository;

import com.Project.DistributedFileStorage.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, String> {

    /** Find a file by its UUID-prefixed physical filename */
    Optional<File> findByFilename(String filename);

    /** Search files whose original name contains the query string (case-insensitive) */
    @Query("SELECT f FROM File f WHERE LOWER(f.originalName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<File> searchByOriginalName(@Param("name") String name);

    /** Count files stored on a specific storage type */
    long countByStorageType(String storageType);

    /** Sum of all file sizes */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM File f WHERE f.status <> 'DELETED'")
    Long sumTotalFileSize();

    /** Sum of file sizes for a specific storage type */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM File f WHERE f.storageType = :storageType AND f.status <> 'DELETED'")
    Long sumFileSizeByStorageType(@Param("storageType") String storageType);
}
