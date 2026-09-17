package com.Project.DistributedFileStorage.exception;

/**
 * Thrown when a storage operation (upload, download, delete) fails
 * on any storage provider.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
