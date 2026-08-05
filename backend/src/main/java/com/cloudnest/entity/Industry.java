package com.cloudnest.entity;

/**
 * Predefined industry types. Each carries a recommended backup frequency
 * (cron expression) and retention period, used to auto-fill a BackupPolicy
 * when a user selects their industry.
 */
public enum Industry {
    HEALTHCARE("0 0 */6 * * *", 365, "Frequent backups, long retention for compliance (HIPAA-style)"),
    FINANCE("0 0 */4 * * *", 730, "High frequency, very long retention for audits"),
    EDUCATION("0 0 0 * * *", 180, "Daily backups, moderate retention"),
    MANUFACTURING("0 0 0 * * *", 90, "Daily backups, shorter retention for operational data"),
    GENERAL("0 0 0 * * *", 30, "Default policy for uncategorized use");

    private final String defaultCron;
    private final int defaultRetentionDays;
    private final String description;

    Industry(String defaultCron, int defaultRetentionDays, String description) {
        this.defaultCron = defaultCron;
        this.defaultRetentionDays = defaultRetentionDays;
        this.description = description;
    }

    public String getDefaultCron() {
        return defaultCron;
    }

    public int getDefaultRetentionDays() {
        return defaultRetentionDays;
    }

    public String getDescription() {
        return description;
    }
}
