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

    /** Kept as-is: live sum of every connected provider's reported usage. */
    private long totalStorageUsedBytes;

    /** Physical bytes stored after dedup (from content_blobs table). */
    private long physicalStorageBytes;

    /** Logical bytes the user actually backed up (sum of plaintext sizes). */
    private long logicalStorageBytes;

    /** logicalStorageBytes − physicalStorageBytes (>= 0). */
    private long dedupSavingsBytes;

    /** logicalStorageBytes / physicalStorageBytes, rounded to 2 decimals. */
    private double dedupRatio;

    /** Number of unique content blobs for this user. */
    private long uniqueBlobs;

    private int connectedProviders;
    private int activeSchedules;
    private Map<String, Long> backupsByProvider;
    private List<ActivityLogResponse> recentActivity;
}