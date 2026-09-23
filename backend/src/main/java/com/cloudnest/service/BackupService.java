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
    private final QuotaService quotaService;

    public List<BackupJob> getUserJobs(User user) {
        return backupJobRepository.findByUserOrderByStartedAtDesc(user);
    }

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

                String checksum = encryptionService.computeChecksum(tempInput);

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

                int nextVersion = backupFileRepository
                        .findMaxVersionForUserAndFileName(user, mf.getOriginalFilename()) + 1;

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

            // NEW: post-backup quota check (never fails the backup)
            quotaService.checkAfterBackup(provider);

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

    public File restoreFile(User user, Long backupFileId) {
        BackupFile backupFile = backupFileRepository.findById(backupFileId)
                .orElseThrow(() -> new IllegalArgumentException("Backup file not found"));

        BackupJob job = backupFile.getBackupJob();
        if (!job.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to restore this file");
        }

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