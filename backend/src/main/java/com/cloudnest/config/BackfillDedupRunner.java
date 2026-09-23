package com.cloudnest.config;

import com.cloudnest.entity.BackupFile;
import com.cloudnest.entity.CloudProvider;
import com.cloudnest.entity.ContentBlob;
import com.cloudnest.repository.BackupFileRepository;
import com.cloudnest.repository.ContentBlobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One-time migration: wraps pre-dedup BackupFile rows in ContentBlob rows.
 *
 * Safe to run every startup — it only touches rows whose content_blob_id is null,
 * and it never re-uploads or deletes anything.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class BackfillDedupRunner implements CommandLineRunner {

    private final BackupFileRepository backupFileRepository;
    private final ContentBlobRepository contentBlobRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<BackupFile> orphans = backupFileRepository.findFilesWithoutBlob();
        if (orphans.isEmpty()) {
            log.info("[Backfill] No pre-dedup BackupFile rows to migrate.");
            return;
        }

        log.info("[Backfill] Found {} BackupFile rows without a ContentBlob. Migrating...", orphans.size());

        // Cache: (userId, providerId, checksum) -> blob we've already built in this run
        Map<String, ContentBlob> seen = new HashMap<>();
        int created = 0;
        int linked = 0;

        for (BackupFile file : orphans) {
            if (file.getChecksum() == null || file.getStoragePath() == null) {
                log.warn("[Backfill] Skipping file id={} (missing checksum or storagePath)", file.getId());
                continue;
            }

            CloudProvider provider = file.getBackupJob().getCloudProvider();
            Long userId = file.getBackupJob().getUser().getId();
            String key = userId + "|" + provider.getId() + "|" + file.getChecksum();

            ContentBlob blob = seen.get(key);
            if (blob == null) {
                // Check DB too (in case a previous startup created it)
                Optional<ContentBlob> existing = contentBlobRepository
                        .findByUserAndCloudProviderAndChecksum(
                                file.getBackupJob().getUser(), provider, file.getChecksum());
                if (existing.isPresent()) {
                    blob = existing.get();
                } else {
                    blob = contentBlobRepository.save(ContentBlob.builder()
                            .user(file.getBackupJob().getUser())
                            .cloudProvider(provider)
                            .checksum(file.getChecksum())
                            .storagePath(file.getStoragePath())
                            .sizeBytes(file.getFileSizeBytes())
                            .storedSizeBytes(file.getFileSizeBytes()) // best guess — we don't know ciphertext size
                            .encrypted(file.isEncrypted())
                            .refCount(0)   // incremented below per linked file
                            .build());
                    created++;
                }
                seen.put(key, blob);
            }

            // Link the file and bump refCount
            file.setContentBlob(blob);
            blob.setRefCount(blob.getRefCount() + 1);
            backupFileRepository.save(file);
            contentBlobRepository.save(blob);
            linked++;
        }

        log.info("[Backfill] Done. Created {} blobs, linked {} files.", created, linked);
    }
}