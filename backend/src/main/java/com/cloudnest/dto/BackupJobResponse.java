package com.cloudnest.dto;

import com.cloudnest.entity.BackupJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupJobResponse {
    private Long id;
    private String jobName;
    private BackupJob.BackupStatus status;
    private String providerName;
    private boolean scheduled;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private List<String> fileNames;
}
