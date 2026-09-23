package com.cloudnest.service;

import com.cloudnest.dto.ProviderQuota;
import com.cloudnest.entity.ActivityLog;
import com.cloudnest.entity.CloudProvider;
import com.cloudnest.entity.QuotaAlert;
import com.cloudnest.entity.User;
import com.cloudnest.repository.QuotaAlertRepository;
import com.cloudnest.service.storage.CloudStorageProvider;
import com.cloudnest.service.storage.StorageProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Evaluates storage quota for a provider and writes persisted alerts on
 * threshold transitions.
 *
 * Design:
 *  - evaluate() returns the *current* severity every time (cheap, always fresh)
 *  - persistTransitionIfNeeded() inserts a QuotaAlert row ONLY when the
 *    severity has changed since the last unacknowledged alert — this is what
 *    prevents log spam on every dashboard refresh
 *  - onTransition write is wrapped in try/catch by callers so a quota failure
 *    never breaks the primary operation (backup, page load, etc.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final StorageProviderFactory storageProviderFactory;
    private final QuotaAlertRepository quotaAlertRepository;
    private final ActivityLogService activityLogService;

    @Value("${app.quotas.warning-threshold:0.80}")
    private double warningThreshold;

    @Value("${app.quotas.critical-threshold:0.95}")
    private double criticalThreshold;

    /**
     * Computes current quota + severity for a provider.
     * Does NOT persist anything — pure read.
     */
    public ProviderQuota evaluate(CloudProvider provider) {
        CloudStorageProvider storage = storageProviderFactory.getProvider(provider.getType());
        ProviderQuota base = storage.getQuota(provider);

        ProviderQuota.Severity severity = severityFor(base.getPercentUsed());
        String message = messageFor(severity, base);

        return new ProviderQuota(base.getUsedBytes(), base.getQuotaBytes(), severity, message);
    }

    /**
     * Evaluate + persist an alert row if the severity has transitioned since
     * the last unacknowledged alert for this provider.
     *
     * Safe to call from any flow — swallows internal errors into a log line.
     */
    @Transactional
    public ProviderQuota evaluateAndPersist(CloudProvider provider) {
        ProviderQuota quota = evaluate(provider);
        try {
            persistTransitionIfNeeded(provider, quota);
        } catch (Exception e) {
            log.warn("[Quota] Could not persist alert transition for provider {}: {}",
                    provider.getId(), e.getMessage());
        }
        return quota;
    }

    /**
     * Called after a successful backup job. Wrapped in try/catch by the caller
     * so quota checking can never fail a backup.
     */
    public void checkAfterBackup(CloudProvider provider) {
        try {
            evaluateAndPersist(provider);
        } catch (Exception e) {
            log.warn("[Quota] Post-backup quota check failed for provider {}: {}",
                    provider.getId(), e.getMessage());
        }
    }

    private ProviderQuota.Severity severityFor(double percentUsed) {
        double ratio = percentUsed / 100.0;
        if (ratio >= criticalThreshold) return ProviderQuota.Severity.CRITICAL;
        if (ratio >= warningThreshold)  return ProviderQuota.Severity.WARNING;
        return ProviderQuota.Severity.NORMAL;
    }

    private String messageFor(ProviderQuota.Severity severity, ProviderQuota quota) {
        return switch (severity) {
            case CRITICAL -> String.format("Storage at %.1f%% of quota — nearly full", quota.getPercentUsed());
            case WARNING  -> String.format("Storage at %.1f%% of quota — approaching limit", quota.getPercentUsed());
            case NORMAL   -> null;
        };
    }

    private void persistTransitionIfNeeded(CloudProvider provider, ProviderQuota quota) {
        // Only track WARNING / CRITICAL — NORMAL means "no active alert"
        if (quota.getSeverity() == ProviderQuota.Severity.NORMAL) {
            return;
        }

        Optional<QuotaAlert> existing = quotaAlertRepository
                .findFirstByCloudProviderAndAcknowledgedFalseOrderByTriggeredAtDesc(provider);

        QuotaAlert.ProviderQuotaSeverity newSeverity = mapSeverity(quota.getSeverity());

        if (existing.isPresent()) {
            QuotaAlert last = existing.get();
            if (last.getSeverity() == newSeverity) {
                // Same severity already recorded — do not spam
                return;
            }
            // Escalation (WARNING -> CRITICAL): acknowledge old, insert new
            last.setAcknowledged(true);
            quotaAlertRepository.save(last);
        }

        QuotaAlert alert = QuotaAlert.builder()
                .user(provider.getUser())
                .cloudProvider(provider)
                .severity(newSeverity)
                .percentUsed(quota.getPercentUsed())
                .usedBytes(quota.getUsedBytes())
                .quotaBytes(quota.getQuotaBytes())
                .message(quota.getAlertMessage())
                .acknowledged(false)
                .build();
        quotaAlertRepository.save(alert);

        activityLogService.log(
                provider.getUser(),
                "QUOTA_" + newSeverity.name(),
                quota.getAlertMessage() + " (" + provider.getDisplayName() + ")",
                newSeverity == QuotaAlert.ProviderQuotaSeverity.CRITICAL
                        ? ActivityLog.LogLevel.ERROR
                        : ActivityLog.LogLevel.WARNING
        );

       log.warn("[Quota] {} for provider {} at {}%",
        newSeverity, provider.getId(), String.format("%.1f", quota.getPercentUsed()));
    }

    private QuotaAlert.ProviderQuotaSeverity mapSeverity(ProviderQuota.Severity s) {
        return switch (s) {
            case CRITICAL -> QuotaAlert.ProviderQuotaSeverity.CRITICAL;
            case WARNING  -> QuotaAlert.ProviderQuotaSeverity.WARNING;
            case NORMAL   -> null; // unreachable; caller guards against it
        };
    }

    public List<QuotaAlert> getUnacknowledgedAlerts(User user) {
        return quotaAlertRepository.findByUserAndAcknowledgedFalseOrderByTriggeredAtDesc(user);
    }
}