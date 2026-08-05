package com.cloudnest.service;

import com.cloudnest.dto.DashboardStatsResponse;
import com.cloudnest.entity.BackupJob;
import com.cloudnest.entity.CloudProvider;
import com.cloudnest.entity.User;
import com.cloudnest.repository.ActivityLogRepository;
import com.cloudnest.repository.BackupJobRepository;
import com.cloudnest.repository.BackupScheduleRepository;
import com.cloudnest.repository.CloudProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BackupJobRepository backupJobRepository;
    private final CloudProviderRepository cloudProviderRepository;
    private final BackupScheduleRepository backupScheduleRepository;
    private final ActivityLogRepository activityLogRepository;
    private final CloudProviderService cloudProviderService;

    public DashboardStatsResponse getStats(User user) {
        List<BackupJob> jobs = backupJobRepository.findByUserOrderByStartedAtDesc(user);
        List<CloudProvider> providers = cloudProviderRepository.findByUser(user);

        long total = jobs.size();
        long success = backupJobRepository.countByUserAndStatus(user, BackupJob.BackupStatus.SUCCESS);
        long failed = backupJobRepository.countByUserAndStatus(user, BackupJob.BackupStatus.FAILED);

        long totalStorage = providers.stream()
                .filter(CloudProvider::isConnected)
                .mapToLong(cloudProviderService::getUsageForProvider)
                .sum();

        Map<String, Long> byProvider = new HashMap<>();
        for (BackupJob job : jobs) {
            String key = job.getCloudProvider().getType().name();
            byProvider.merge(key, 1L, Long::sum);
        }

        return DashboardStatsResponse.builder()
                .totalBackups(total)
                .successfulBackups(success)
                .failedBackups(failed)
                .totalStorageUsedBytes(totalStorage)
                .connectedProviders((int) providers.stream().filter(CloudProvider::isConnected).count())
                .activeSchedules((int) backupScheduleRepository.findByUser(user).stream()
                        .filter(s -> s.isActive()).count())
                .backupsByProvider(byProvider)
                .recentActivity(List.copyOf(activityLogRepository.findByUserOrderByCreatedAtDesc(user)
                        .stream().limit(10).toList()))
                .build();
    }
}
