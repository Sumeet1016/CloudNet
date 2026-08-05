package com.cloudnest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "backup_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloud_provider_id", nullable = false)
    private CloudProvider cloudProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private BackupPolicy policy;

    @Column(name = "job_name")
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BackupStatus status = BackupStatus.PENDING;

    /** true if triggered by the scheduler rather than a manual click */
    @Column(name = "is_scheduled")
    @Builder.Default
    private boolean scheduled = false;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @OneToMany(mappedBy = "backupJob", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BackupFile> files = new ArrayList<>();

    public enum BackupStatus {
        PENDING, IN_PROGRESS, SUCCESS, FAILED, RESTORED
    }
}
