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
 * Simulates an "on-premise / local disk" backup target. Useful for demoing
 * the platform without any external account, and as a fallback provider.
 */
@Slf4j
@Component
public class LocalDiskStorageProvider implements CloudStorageProvider {

    @Value("${app.storage.local-disk-path}")
    private String basePath;

    @Override
    public String upload(CloudProvider provider, File localFile, String targetFileName) {
        try {
            Path dir = Paths.get(basePath, "user_" + provider.getUser().getId());
            Files.createDirectories(dir);
            Path target = dir.resolve(targetFileName);
            Files.copy(localFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("[LocalDisk] Stored file at {}", target);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("Local disk upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public File download(CloudProvider provider, String storagePath) {
        File file = new File(storagePath);
        if (!file.exists()) {
            throw new RuntimeException("File not found on local disk: " + storagePath);
        }
        return file;
    }

    @Override
    public void delete(CloudProvider provider, String storagePath) {
        File file = new File(storagePath);
        if (file.exists() && !file.delete()) {
            log.warn("Failed to delete local file: {}", storagePath);
        }
    }

    @Override
    public long getUsedStorageBytes(CloudProvider provider) {
        Path dir = Paths.get(basePath, "user_" + provider.getUser().getId());
        if (!Files.exists(dir)) return 0;
        try (var stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }
}
