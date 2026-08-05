package com.cloudnest.service.storage;

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
 * To go live: swap the body of these methods for AmazonS3 client calls
 * (putObject/getObject/deleteObject) - callers never need to change.
 */
@Slf4j
@Component
public class SimulatedS3StorageProvider implements CloudStorageProvider {

    @Value("${app.storage.simulated-s3-path}")
    private String basePath;

    @Override
    public String upload(CloudProvider provider, File localFile, String targetFileName) {
        try {
            // "bucket" per user, mirroring how S3 keys are usually namespaced
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
}
