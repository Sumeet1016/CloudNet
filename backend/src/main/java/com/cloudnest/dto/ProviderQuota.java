package com.cloudnest.dto;

/**
 * Storage usage + configured quota for a provider.
 * percentUsed is computed and clamped to [0, 100].
 */
public class ProviderQuota {

    private long usedBytes;
    private long quotaBytes;
    private double percentUsed;

    public ProviderQuota() {
    }

    public ProviderQuota(long usedBytes, long quotaBytes) {
        this.usedBytes = usedBytes;
        this.quotaBytes = quotaBytes;
        this.percentUsed = quotaBytes > 0
                ? Math.min(100.0, (usedBytes * 100.0) / quotaBytes)
                : 0.0;
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
}