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
    private final ActivityLogService activityLogService;

    public List<BackupJob> getUserJobs(User user) {
        return backupJobRepository.findByUserOrderByStartedAtDesc(user);
    }

    /**
     * Manually triggered backup: encrypts each uploaded file (if the policy
     * requires it), pushes it to the chosen provider, and records everything.
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

        try {
            CloudStorageProvider storage = storageProviderFactory.getProvider(provider.getType());
            boolean encrypt = policy == null || policy.isEncryptionEnabled();

            for (MultipartFile mf : uploadedFiles) {
                File tempInput = File.createTempFile("cloudnest-upload-", "-" + mf.getOriginalFilename());
                mf.transferTo(tempInput);

                File toUpload = encrypt ? encryptionService.encryptFile(tempInput) : tempInput;
                String checksum = encryptionService.computeChecksum(tempInput);

                String targetName = (encrypt ? mf.getOriginalFilename() + ".enc" : mf.getOriginalFilename());
                String storagePath = storage.upload(provider, toUpload, targetName);

                int nextVersion = backupFileRepository
                        .findByOriginalFileNameOrderByVersionNumberDesc(mf.getOriginalFilename())
                        .stream().findFirst().map(f -> f.getVersionNumber() + 1).orElse(1);

                BackupFile backupFile = BackupFile.builder()
                        .backupJob(job)
                        .originalFileName(mf.getOriginalFilename())
                        .storagePath(storagePath)
                        .fileSizeBytes(mf.getSize())
                        .checksum(checksum)
                        .versionNumber(nextVersion)
                        .encrypted(encrypt)
                        .build();
                backupFileRepository.save(backupFile);

                Files.deleteIfExists(tempInput.toPath());
                if (encrypt) Files.deleteIfExists(toUpload.toPath());
            }

            job.setStatus(BackupJob.BackupStatus.SUCCESS);
            job.setCompletedAt(LocalDateTime.now());
            activityLogService.log(user, "BACKUP_SUCCESS", "Backup '" + job.getJobName() + "' completed on " + provider.getType());

        } catch (IOException | RuntimeException e) {
            log.error("Backup job {} failed", job.getId(), e);
            job.setStatus(BackupJob.BackupStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            activityLogService.log(user, "BACKUP_FAILED", "Backup '" + job.getJobName() + "' failed: " + e.getMessage(),
                    ActivityLog.LogLevel.ERROR);
        }

        return backupJobRepository.save(job);
    }

    /** Restores a given backup file: downloads + decrypts (if needed) and returns the local file to send back */
    public File restoreFile(User user, Long backupFileId) {
        BackupFile backupFile = backupFileRepository.findById(backupFileId)
                .orElseThrow(() -> new IllegalArgumentException("Backup file not found"));

        BackupJob job = backupFile.getBackupJob();
        if (!job.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to restore this file");
        }

        CloudProvider provider = job.getCloudProvider();
        CloudStorageProvider storage = storageProviderFactory.getProvider(provider.getType());

        File downloaded = storage.download(provider, backupFile.getStoragePath());
        File result = backupFile.isEncrypted() ? encryptionService.decryptFile(downloaded) : downloaded;

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
