package com.cloudnest.service;

import com.cloudnest.entity.*;
import com.cloudnest.repository.BackupFileRepository;
import com.cloudnest.repository.BackupJobRepository;
import com.cloudnest.service.storage.CloudStorageProvider;
import com.cloudnest.service.storage.StorageProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Shared logic used by ScheduleService to actually perform a backup of a
 * directory's contents. Pulled out of ScheduleService to keep the polling
 * loop readable and to make the execution path unit-testable on its own.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupExecutionHelper {

    private final BackupJobRepository backupJobRepository;
    private final BackupFileRepository backupFileRepository;
    private final StorageProviderFactory storageProviderFactory;
    private final EncryptionService encryptionService;
    private final ActivityLogService activityLogService;

    public void executeScheduledBackup(BackupSchedule schedule) throws IOException {
        User user = schedule.getUser();
        CloudProvider provider = schedule.getCloudProvider();
        BackupPolicy policy = schedule.getPolicy();

        BackupJob job = BackupJob.builder()
                .user(user)
                .cloudProvider(provider)
                .policy(policy)
                .jobName("Scheduled Backup - " + schedule.getSourcePath())
                .status(BackupJob.BackupStatus.IN_PROGRESS)
                .scheduled(true)
                .startedAt(LocalDateTime.now())
                .build();
        job = backupJobRepository.save(job);

        Path sourceDir = Path.of(schedule.getSourcePath());
        if (!Files.exists(sourceDir)) {
            job.setStatus(BackupJob.BackupStatus.FAILED);
            job.setErrorMessage("Source path does not exist: " + schedule.getSourcePath());
            job.setCompletedAt(LocalDateTime.now());
            backupJobRepository.save(job);
            return;
        }

        CloudStorageProvider storage = storageProviderFactory.getProvider(provider.getType());
        boolean encrypt = policy == null || policy.isEncryptionEnabled();

        try (var stream = Files.walk(sourceDir)) {
            List<Path> filesToBackup = stream.filter(Files::isRegularFile).toList();

            for (Path path : filesToBackup) {
                File original = path.toFile();
                File toUpload = encrypt ? encryptionService.encryptFile(original) : original;
                String checksum = encryptionService.computeChecksum(original);
                String targetName = original.getName() + (encrypt ? ".enc" : "");

                String storagePath = storage.upload(provider, toUpload, targetName);

                int nextVersion = backupFileRepository
                        .findByOriginalFileNameOrderByVersionNumberDesc(original.getName())
                        .stream().findFirst().map(f -> f.getVersionNumber() + 1).orElse(1);

                BackupFile backupFile = BackupFile.builder()
                        .backupJob(job)
                        .originalFileName(original.getName())
                        .storagePath(storagePath)
                        .fileSizeBytes(original.length())
                        .checksum(checksum)
                        .versionNumber(nextVersion)
                        .encrypted(encrypt)
                        .build();
                backupFileRepository.save(backupFile);

                if (encrypt) Files.deleteIfExists(toUpload.toPath());
            }
        }

        job.setStatus(BackupJob.BackupStatus.SUCCESS);
        job.setCompletedAt(LocalDateTime.now());
        backupJobRepository.save(job);

        activityLogService.log(user, "SCHEDULED_BACKUP_SUCCESS",
                "Scheduled backup of " + schedule.getSourcePath() + " completed on " + provider.getType());
    }
}
