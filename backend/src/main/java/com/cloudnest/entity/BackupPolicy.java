package com.cloudnest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "backup_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private Industry industry;

    /** Cron expression controlling how often scheduled jobs under this policy run */
    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Column(name = "encryption_enabled")
    @Builder.Default
    private boolean encryptionEnabled = true;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;
}
