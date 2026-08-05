package com.cloudnest.service.storage;

import com.cloudnest.entity.ProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the right CloudStorageProvider strategy implementation for a
 * given ProviderType. This is the single place that needs updating when a
 * new real provider (Azure Blob, Dropbox, real AWS S3...) is added.
 */
@Component
@RequiredArgsConstructor
public class StorageProviderFactory {

    private final GoogleDriveStorageProvider googleDriveStorageProvider;
    private final LocalDiskStorageProvider localDiskStorageProvider;
    private final SimulatedS3StorageProvider simulatedS3StorageProvider;

    public CloudStorageProvider getProvider(ProviderType type) {
        return switch (type) {
            case GOOGLE_DRIVE -> googleDriveStorageProvider;
            case LOCAL_DISK -> localDiskStorageProvider;
            case SIMULATED_S3 -> simulatedS3StorageProvider;
        };
    }
}
