package com.cloudnest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalBackups;
    private long successfulBackups;
    private long failedBackups;
    private long totalStorageUsedBytes;
    private int connectedProviders;
    private int activeSchedules;
    private Map<String, Long> backupsByProvider;
    private List<Object> recentActivity;
}
