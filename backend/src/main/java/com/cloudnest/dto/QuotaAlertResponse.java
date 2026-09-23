package com.cloudnest.dto;

import com.cloudnest.entity.QuotaAlert;

import java.time.LocalDateTime;

/**
 * Safe view of a QuotaAlert for the frontend — no JPA relations.
 */
public class QuotaAlertResponse {

    private Long id;
    private Long providerId;
    private String providerName;
    private String providerType;
    private String severity;
    private double percentUsed;
    private long usedBytes;
    private long quotaBytes;
    private String message;
    private boolean acknowledged;
    private LocalDateTime triggeredAt;

    public static QuotaAlertResponse from(QuotaAlert a) {
        QuotaAlertResponse r = new QuotaAlertResponse();
        r.id = a.getId();
        r.providerId = a.getCloudProvider().getId();
        r.providerName = a.getCloudProvider().getDisplayName();
        r.providerType = a.getCloudProvider().getType().name();
        r.severity = a.getSeverity().name();
        r.percentUsed = a.getPercentUsed();
        r.usedBytes = a.getUsedBytes();
        r.quotaBytes = a.getQuotaBytes();
        r.message = a.getMessage();
        r.acknowledged = a.isAcknowledged();
        r.triggeredAt = a.getTriggeredAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getProviderId() { return providerId; }
    public String getProviderName() { return providerName; }
    public String getProviderType() { return providerType; }
    public String getSeverity() { return severity; }
    public double getPercentUsed() { return percentUsed; }
    public long getUsedBytes() { return usedBytes; }
    public long getQuotaBytes() { return quotaBytes; }
    public String getMessage() { return message; }
    public boolean isAcknowledged() { return acknowledged; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; }
}