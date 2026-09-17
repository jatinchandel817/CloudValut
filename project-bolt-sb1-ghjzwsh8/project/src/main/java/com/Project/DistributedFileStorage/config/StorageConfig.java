package com.Project.DistributedFileStorage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for local file storage.
 * Reads the local storage path from application.properties and creates
 * the directory on startup if it does not exist.
 */
@Configuration
public class StorageConfig {

    @Value("${storage.local.path:./uploads}")
    private String localStoragePath;

    private Path rootPath;

    @PostConstruct
    public void init() throws IOException {
        this.rootPath = Paths.get(localStoragePath).toAbsolutePath().normalize();
        Files.createDirectories(this.rootPath);
    }

    public Path getRootPath() {
        return rootPath;
    }

    public String getLocalStoragePath() {
        return localStoragePath;
    }
}
