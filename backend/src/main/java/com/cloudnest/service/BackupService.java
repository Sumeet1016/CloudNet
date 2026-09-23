package com.cloudnest.service;

import com.cloudnest.entity.*;
import com.cloudnest.repository.BackupFileRepository;
import com.cloudnest.repository.BackupJobRepository;
import com.cloudnest.repository.CloudProviderRepository;
import com.cloudnest.service.storage.CloudStorageProvider;
import com.cloudnest.service.storage.StorageProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final BackupJobRepository backupJobRepository;
    private final BackupFileRepository backupFileRepository;
    private final CloudProviderRepository cloudProviderRepository;
    private final StorageProviderFactory storageProviderFactory;
    private final EncryptionService encryptionService;
    private final DeduplicationService deduplicationService;
    private final ActivityLogService activityLogService;

    public List<BackupJob> getUserJobs(User user) {
        return backupJobRepository.findByUserOrderByStartedAtDesc(user);
    }

    /**
     * Manually triggered backup.
     *
     * For each file:
     *   1. Write to a temp file
     *   2. Compute SHA-256 of the plaintext
     *   3. Ask DeduplicationService for a blob (reuses if identical content
     *      already exists for this user+provider; uploads only on miss)
     *   4. Record a BackupFile pointing at that blob
     */
    public BackupJob runManualBackup(User user, Long cloudProviderId, BackupPolicy policy,
                                     String jobName, MultipartFile[] uploadedFiles) {

        CloudProvider provider = cloudProviderRepository.findByIdAndUser(cloudProviderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Cloud provider not found"));

        BackupJob job = BackupJob.builder()
                .user(user)
                .cloudProvider(provider)
                .policy(policy)
                .jobName(jobName != null ? jobName : "Manual Backup " + LocalDateTime.now())
                .status(BackupJob.BackupStatus.IN_PROGRESS)
                .scheduled(false)
                .startedAt(LocalDateTime.now())
                .build();
        job = backupJobRepository.save(job);

        int dedupHits = 0;
        int stored = 0;

        try {
            boolean encrypt = policy == null || policy.isEncryptionEnabled();

            for (MultipartFile mf : uploadedFiles) {
                File tempInput = File.createTempFile("cloudnest-upload-", "-" + mf.getOriginalFilename());
                mf.transferTo(tempInput);

                // 1) Hash the PLAINTEXT — this is the dedup key
                String checksum = encryptionService.computeChecksum(tempInput);

                // 2) Ask dedup service to get-or-create the blob (upload only on miss)
                String targetName = encrypt
                        ? mf.getOriginalFilename() + ".enc"
                        : mf.getOriginalFilename();

                DeduplicationService.BlobResult result = deduplicationService.getOrCreateBlob(
                        user, provider, tempInput, checksum, encrypt, targetName);

                if (result.deduplicated) {
                    dedupHits++;
                } else {
                    stored++;
                }

                // 3) Compute next version (user-scoped — fixes a latent cross-user bug)
                int nextVersion = backupFileRepository
                        .findMaxVersionForUserAndFileName(user, mf.getOriginalFilename()) + 1;

                // 4) Link BackupFile -> ContentBlob
                BackupFile backupFile = BackupFile.builder()
                        .backupJob(job)
                        .originalFileName(mf.getOriginalFilename())
                        .storagePath(result.blob.getStoragePath())
                        .fileSizeBytes(result.blob.getSizeBytes())
                        .checksum(checksum)
                        .versionNumber(nextVersion)
                        .encrypted(encrypt)
                        .contentBlob(result.blob)
                        .build();
                backupFileRepository.save(backupFile);

                Files.deleteIfExists(tempInput.toPath());
            }

            job.setStatus(BackupJob.BackupStatus.SUCCESS);
            job.setCompletedAt(LocalDateTime.now());
            activityLogService.log(user, "BACKUP_SUCCESS",
                    String.format("Backup '%s' on %s — %d stored, %d deduplicated",
                            job.getJobName(), provider.getType(), stored, dedupHits));

        } catch (IOException | RuntimeException e) {
            log.error("Backup job {} failed", job.getId(), e);
            job.setStatus(BackupJob.BackupStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            activityLogService.log(user, "BACKUP_FAILED",
                    "Backup '" + job.getJobName() + "' failed: " + e.getMessage(),
                    ActivityLog.LogLevel.ERROR);
        }

        return backupJobRepository.save(job);
    }

    /** Restores a file: downloads the blob's bytes + decrypts if needed. */
    public File restoreFile(User user, Long backupFileId) {
        BackupFile backupFile = backupFileRepository.findById(backupFileId)
                .orElseThrow(() -> new IllegalArgumentException("Backup file not found"));

        BackupJob job = backupFile.getBackupJob();
        if (!job.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to restore this file");
        }

        // Prefer the blob's provider/storagePath. Fall back to BackupFile fields
        // so pre-dedup rows still restore.
        CloudProvider provider;
        String storagePath;

        if (backupFile.getContentBlob() != null) {
            provider = backupFile.getContentBlob().getCloudProvider();
            storagePath = backupFile.getContentBlob().getStoragePath();
        } else {
            provider = job.getCloudProvider();
            storagePath = backupFile.getStoragePath();
        }

        CloudStorageProvider storage = storageProviderFactory.getProvider(provider.getType());
        File downloaded = storage.download(provider, storagePath);
        File result = backupFile.isEncrypted()
                ? encryptionService.decryptFile(downloaded)
                : downloaded;

        job.setStatus(BackupJob.BackupStatus.RESTORED);
        backupJobRepository.save(job);
        activityLogService.log(user, "RESTORE", "Restored file " + backupFile.getOriginalFileName()
                + " (v" + backupFile.getVersionNumber() + ")");

        return result;
    }

    public List<BackupFile> getFileVersions(Long jobId, User user) {
        BackupJob job = backupJobRepository.findByIdAndUser(jobId, user)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        return backupFileRepository.findByBackupJob(job);
    }
}