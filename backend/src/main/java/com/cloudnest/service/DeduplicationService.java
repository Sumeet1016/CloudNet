package com.cloudnest.service;

import com.cloudnest.entity.BackupFile;
import com.cloudnest.entity.CloudProvider;
import com.cloudnest.entity.ContentBlob;
import com.cloudnest.entity.User;
import com.cloudnest.repository.BackupFileRepository;
import com.cloudnest.repository.ContentBlobRepository;
import com.cloudnest.service.storage.CloudStorageProvider;
import com.cloudnest.service.storage.StorageProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Optional;

/**
 * Handles content deduplication.
 *
 * getOrCreateBlob() is called *per file* during a backup:
 *   1. Compute SHA-256 of the plaintext (already done by caller — passed in)
 *   2. Look for an existing ContentBlob with the same (user, provider, checksum)
 *   3. If found: increment refCount, return it (NO upload happens)
 *   4. If not found: encrypt (if needed), upload, create the blob, return it
 *
 * The caller then links a BackupFile to the returned blob.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeduplicationService {

    private final ContentBlobRepository contentBlobRepository;
    private final BackupFileRepository backupFileRepository;
    private final StorageProviderFactory storageProviderFactory;
    private final EncryptionService encryptionService;

    /**
     * Result of a dedup check. Tells the caller whether we reused or stored.
     */
    public static class BlobResult {
        public final ContentBlob blob;
        public final boolean deduplicated;  // true = reused, false = newly stored

        public BlobResult(ContentBlob blob, boolean deduplicated) {
            this.blob = blob;
            this.deduplicated = deduplicated;
        }
    }

    /**
     * @param user          owner
     * @param provider      provider this backup targets (dedup is scoped per provider)
     * @param plaintextFile the *original* file on disk (before encryption)
     * @param checksum      SHA-256 hex of the plaintext (caller already computed it)
     * @param encrypt       whether to encrypt before upload
     * @param targetName    filename to use at the provider
     */
    @Transactional
    public BlobResult getOrCreateBlob(User user,
                                      CloudProvider provider,
                                      File plaintextFile,
                                      String checksum,
                                      boolean encrypt,
                                      String targetName) {

        // 1) Look for an existing blob
        Optional<ContentBlob> existing = contentBlobRepository
                .findByUserAndCloudProviderAndChecksum(user, provider, checksum);

        if (existing.isPresent()) {
            ContentBlob blob = existing.get();
            blob.setRefCount(blob.getRefCount() + 1);
            contentBlobRepository.save(blob);
            log.info("[Dedup] HIT — user={} provider={} checksum={} refCount={}",
                    user.getId(), provider.getType(), checksum.substring(0, 12), blob.getRefCount());
            return new BlobResult(blob, true);
        }

        // 2) Miss: encrypt (if needed), upload, create blob
        File toUpload = encrypt ? encryptionService.encryptFile(plaintextFile) : plaintextFile;
        long storedSize = toUpload.length();
        long plaintextSize = plaintextFile.length();

        try {
            CloudStorageProvider storage = storageProviderFactory.getProvider(provider.getType());
            String storagePath = storage.upload(provider, toUpload, targetName);

            ContentBlob blob = ContentBlob.builder()
                    .user(user)
                    .cloudProvider(provider)
                    .checksum(checksum)
                    .storagePath(storagePath)
                    .sizeBytes(plaintextSize)
                    .storedSizeBytes(storedSize)
                    .encrypted(encrypt)
                    .refCount(1)
                    .build();

            ContentBlob saved = contentBlobRepository.save(blob);
            log.info("[Dedup] MISS — user={} provider={} checksum={} uploaded {} bytes",
                    user.getId(), provider.getType(), checksum.substring(0, 12), storedSize);
            return new BlobResult(saved, false);
        } finally {
            // Cleanup: only delete the encrypted temp file (never the caller's plaintext)
            if (encrypt && toUpload != plaintextFile) {
                //noinspection ResultOfMethodCallIgnored
                toUpload.delete();
            }
        }
    }

    /**
     * Called when a BackupFile is deleted. Decrements refCount and physically
     * removes the blob if no other file references it.
     *
     * Currently unused (no delete endpoint exists yet) but wired up for Feature 5.
     */
    @Transactional
    public void releaseBlob(BackupFile backupFile) {
        ContentBlob blob = backupFile.getContentBlob();
        if (blob == null) return;

        blob.setRefCount(blob.getRefCount() - 1);
        if (blob.getRefCount() <= 0) {
            try {
                CloudStorageProvider storage = storageProviderFactory
                        .getProvider(blob.getCloudProvider().getType());
                storage.delete(blob.getCloudProvider(), blob.getStoragePath());
            } catch (Exception e) {
                log.warn("[Dedup] Failed to delete physical blob {}: {}",
                        blob.getStoragePath(), e.getMessage());
            }
            contentBlobRepository.delete(blob);
            log.info("[Dedup] Deleted orphaned blob id={}", blob.getId());
        } else {
            contentBlobRepository.save(blob);
            log.info("[Dedup] Released ref, blob id={} refCount now {}",
                    blob.getId(), blob.getRefCount());
        }
    }
}