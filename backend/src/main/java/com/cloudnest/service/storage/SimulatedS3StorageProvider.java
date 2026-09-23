package com.cloudnest.service.storage;

import com.cloudnest.dto.ProviderCapabilities;
import com.cloudnest.dto.ProviderHealth;
import com.cloudnest.dto.ProviderQuota;
import com.cloudnest.entity.CloudProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Simulates an S3-style bucket using a local folder, behind the exact same
 * CloudStorageProvider contract a real AWS SDK implementation would use.
 */
@Slf4j
@Component
public class SimulatedS3StorageProvider implements CloudStorageProvider {

    @Value("${app.storage.simulated-s3-path}")
    private String basePath;

    @Value("${app.quotas.simulated-s3-bytes}")
    private long quotaBytes;

    @Override
    public String upload(CloudProvider provider, File localFile, String targetFileName) {
        try {
            Path bucket = Paths.get(basePath, "bucket-user-" + provider.getUser().getId());
            Files.createDirectories(bucket);
            Path objectKey = bucket.resolve(targetFileName);
            Files.copy(localFile.toPath(), objectKey, StandardCopyOption.REPLACE_EXISTING);
            log.info("[SimulatedS3] Put object {}", objectKey);
            return objectKey.toString();
        } catch (IOException e) {
            throw new RuntimeException("Simulated S3 upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public File download(CloudProvider provider, String storagePath) {
        File file = new File(storagePath);
        if (!file.exists()) {
            throw new RuntimeException("Object not found in simulated S3: " + storagePath);
        }
        return file;
    }

    @Override
    public void delete(CloudProvider provider, String storagePath) {
        File file = new File(storagePath);
        if (file.exists() && !file.delete()) {
            log.warn("Failed to delete simulated S3 object: {}", storagePath);
        }
    }

    @Override
    public long getUsedStorageBytes(CloudProvider provider) {
        Path bucket = Paths.get(basePath, "bucket-user-" + provider.getUser().getId());
        if (!Files.exists(bucket)) return 0;
        try (var stream = Files.walk(bucket)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public ProviderHealth healthCheck(CloudProvider provider) {
        try {
            Path bucket = Paths.get(basePath, "bucket-user-" + provider.getUser().getId());
            Files.createDirectories(bucket);
            if (!Files.isWritable(bucket)) {
                return ProviderHealth.degraded("Simulated S3 bucket exists but is not writable: " + bucket);
            }
            return ProviderHealth.connected("Simulated S3 bucket writable at " + bucket);
        } catch (Exception e) {
            return ProviderHealth.unreachable("Simulated S3 unreachable: " + e.getMessage());
        }
    }

    @Override
    public ProviderQuota getQuota(CloudProvider provider) {
        long used = getUsedStorageBytes(provider);
        return new ProviderQuota(used, quotaBytes);
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return new ProviderCapabilities(false, true, false);
    }
}