package com.cloudnest.dto;

/**
 * What a given provider can actually do. The UI uses this to decide which
 * controls to show (e.g. hide quota bars for providers that can't report it).
 */
public class ProviderCapabilities {

    private boolean supportsVersioning;
    private boolean supportsQuotaQuery;
    private boolean supportsServerSideEncryption;

    public ProviderCapabilities() {
    }

    public ProviderCapabilities(boolean supportsVersioning,
                                boolean supportsQuotaQuery,
                                boolean supportsServerSideEncryption) {
        this.supportsVersioning = supportsVersioning;
        this.supportsQuotaQuery = supportsQuotaQuery;
        this.supportsServerSideEncryption = supportsServerSideEncryption;
    }

    public boolean isSupportsVersioning() {
        return supportsVersioning;
    }

    public void setSupportsVersioning(boolean supportsVersioning) {
        this.supportsVersioning = supportsVersioning;
    }

    public boolean isSupportsQuotaQuery() {
        return supportsQuotaQuery;
    }

    public void setSupportsQuotaQuery(boolean supportsQuotaQuery) {
        this.supportsQuotaQuery = supportsQuotaQuery;
    }

    public boolean isSupportsServerSideEncryption() {
        return supportsServerSideEncryption;
    }

    public void setSupportsServerSideEncryption(boolean supportsServerSideEncryption) {
        this.supportsServerSideEncryption = supportsServerSideEncryption;
    }
}