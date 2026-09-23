package com.cloudnest.dto;

/**
 * Storage usage + configured quota for a provider.
 * percentUsed is computed and clamped to [0, 100].
 *
 * severity is derived from percentUsed against configured thresholds:
 *   NORMAL   < warning
 *   WARNING  >= warning  and < critical
 *   CRITICAL >= critical
 */
public class ProviderQuota {

    public enum Severity {
        NORMAL, WARNING, CRITICAL
    }

    private long usedBytes;
    private long quotaBytes;
    private double percentUsed;
    private Severity severity;
    private String alertMessage;

    public ProviderQuota() {
    }

    public ProviderQuota(long usedBytes, long quotaBytes) {
        this.usedBytes = usedBytes;
        this.quotaBytes = quotaBytes;
        this.percentUsed = quotaBytes > 0
                ? Math.min(100.0, (usedBytes * 100.0) / quotaBytes)
                : 0.0;
        this.severity = Severity.NORMAL;
    }

    public ProviderQuota(long usedBytes, long quotaBytes, Severity severity, String alertMessage) {
        this(usedBytes, quotaBytes);
        this.severity = severity;
        this.alertMessage = alertMessage;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(long usedBytes) {
        this.usedBytes = usedBytes;
    }

    public long getQuotaBytes() {
        return quotaBytes;
    }

    public void setQuotaBytes(long quotaBytes) {
        this.quotaBytes = quotaBytes;
    }

    public double getPercentUsed() {
        return percentUsed;
    }

    public void setPercentUsed(double percentUsed) {
        this.percentUsed = percentUsed;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }
}