package com.cloudnest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persisted record of a quota threshold crossing.
 * Written only on state transitions (NORMAL->WARNING, NORMAL->CRITICAL,
 * WARNING->CRITICAL) so we don't spam a row on every /quota call.
 */
@Entity
@Table(name = "quota_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloud_provider_id", nullable = false)
    private CloudProvider cloudProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderQuotaSeverity severity;

    @Column(name = "percent_used", nullable = false)
    private double percentUsed;

    @Column(name = "used_bytes", nullable = false)
    private long usedBytes;

    @Column(name = "quota_bytes", nullable = false)
    private long quotaBytes;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "acknowledged", nullable = false)
    @Builder.Default
    private boolean acknowledged = false;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @PrePersist
    protected void onCreate() {
        if (this.triggeredAt == null) {
            this.triggeredAt = LocalDateTime.now();
        }
    }

    /** Severity of a quota alert. Kept separate from the DTO enum so
     *  entity and DTO can evolve independently. */
    public enum ProviderQuotaSeverity {
        WARNING, CRITICAL
    }
}