package com.cloudnest.service;

import com.cloudnest.dto.ScheduleRequest;
import com.cloudnest.entity.*;
import com.cloudnest.repository.BackupPolicyRepository;
import com.cloudnest.repository.BackupScheduleRepository;
import com.cloudnest.repository.CloudProviderRepository;
import com.cloudnest.service.storage.CloudStorageProvider;
import com.cloudnest.service.storage.StorageProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final BackupScheduleRepository backupScheduleRepository;
    private final CloudProviderRepository cloudProviderRepository;
    private final BackupPolicyRepository backupPolicyRepository;
    private final StorageProviderFactory storageProviderFactory;
    private final EncryptionService encryptionService;
    private final ActivityLogService activityLogService;
    private final BackupExecutionHelper backupExecutionHelper;

    public List<BackupSchedule> getUserSchedules(User user) {
        return backupScheduleRepository.findByUser(user);
    }

    public BackupSchedule createSchedule(User user, ScheduleRequest request) {
        CloudProvider provider = cloudProviderRepository.findByIdAndUser(request.getCloudProviderId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Cloud provider not found"));
        BackupPolicy policy = backupPolicyRepository.findByIdAndUser(request.getPolicyId(), user)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));

        String cron = request.getCronExpression() != null && !request.getCronExpression().isBlank()
                ? request.getCronExpression() : policy.getCronExpression();

        BackupSchedule schedule = BackupSchedule.builder()
                .user(user)
                .cloudProvider(provider)
                .policy(policy)
                .sourcePath(request.getSourcePath())
                .cronExpression(cron)
                .active(true)
                .nextRunAt(computeNextRun(cron))
                .build();

        BackupSchedule saved = backupScheduleRepository.save(schedule);
        activityLogService.log(user, "SCHEDULE_CREATED", "Scheduled backups for " + request.getSourcePath()
                + " (cron: " + cron + ")");
        return saved;
    }

    public void deactivateSchedule(User user, Long id) {
        BackupSchedule schedule = backupScheduleRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        schedule.setActive(false);
        backupScheduleRepository.save(schedule);
    }

    private LocalDateTime computeNextRun(String cron) {
        try {
            CronExpression cronExpression = CronExpression.parse(cron);
            return cronExpression.next(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Invalid cron expression '{}', defaulting to +1 day", cron);
            return LocalDateTime.now().plusDays(1);
        }
    }

    /**
     * Runs every minute and checks which active schedules are due. This is a
     * simple polling scheduler - fine for a college-project scale. For each
     * due schedule it backs up every file currently in sourcePath.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    public void runDueSchedules() {
        List<BackupSchedule> dueSchedules = backupScheduleRepository.findByActiveTrue().stream()
                .filter(s -> s.getNextRunAt() != null && !s.getNextRunAt().isAfter(LocalDateTime.now()))
                .toList();

        for (BackupSchedule schedule : dueSchedules) {
            try {
                backupExecutionHelper.executeScheduledBackup(schedule);
            } catch (Exception e) {
                log.error("Scheduled backup failed for schedule {}", schedule.getId(), e);
                activityLogService.log(schedule.getUser(), "SCHEDULED_BACKUP_FAILED",
                        "Schedule " + schedule.getId() + " failed: " + e.getMessage(), ActivityLog.LogLevel.ERROR);
            } finally {
                schedule.setLastRunAt(LocalDateTime.now());
                schedule.setNextRunAt(computeNextRun(schedule.getCronExpression()));
                backupScheduleRepository.save(schedule);
            }
        }
    }
}
