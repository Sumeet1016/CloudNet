package com.cloudnest.service.storage;

import com.cloudnest.dto.ProviderCapabilities;
import com.cloudnest.dto.ProviderHealth;
import com.cloudnest.dto.ProviderQuota;
import com.cloudnest.entity.CloudProvider;

import java.io.File;

/**
 * The single contract every storage backend in CloudNest must implement.
 * The rest of the application never references a concrete provider class —
 * it only ever talks to this interface (Strategy pattern).
 *
 * Adding a new real cloud = one new class implementing this interface +
 * one line in StorageProviderFactory.
 */
public interface CloudStorageProvider {

    /**
     * Upload a local file into the provider.
     *
     * @return a provider-specific storage path/identifier that can later be
     *         passed to download() and delete().
     */
    String upload(CloudProvider provider, File localFile, String targetFileName);

    /**
     * Download the file identified by storagePath into a local temp file.
     */
    File download(CloudProvider provider, String storagePath);

    /**
     * Permanently delete the file identified by storagePath.
     */
    void delete(CloudProvider provider, String storagePath);

    /**
     * Bytes currently used by this user on this provider.
     * May return 0 if the provider cannot report it.
     */
    long getUsedStorageBytes(CloudProvider provider);

    /**
     * Perform a lightweight connectivity/health check for this provider.
     * Must never throw — failures are reported via ProviderHealth.UNREACHABLE.
     */
    ProviderHealth healthCheck(CloudProvider provider);

    /**
     * Compute used vs configured quota for this provider.
     * The quota value comes from application.yml (app.quotas.*).
     */
    ProviderQuota getQuota(CloudProvider provider);

    /**
     * What this provider supports. Defaults are conservative; providers
     * override to advertise features they actually have.
     */
    default ProviderCapabilities getCapabilities() {
        return new ProviderCapabilities(false, false, false);
    }
}