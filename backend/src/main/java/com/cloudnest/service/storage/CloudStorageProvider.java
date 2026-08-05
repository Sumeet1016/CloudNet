package com.cloudnest.service.storage;

import com.cloudnest.entity.CloudProvider;

import java.io.File;
import java.io.InputStream;

/**
 * Strategy interface implemented by every supported storage backend.
 * Adding a new real provider (e.g. AWS S3, Azure Blob) later just means
 * writing one new class here and registering it in StorageProviderFactory -
 * nothing else in the app needs to change.
 */
public interface CloudStorageProvider {

    /**
     * Uploads a local file to the remote/simulated store and returns a
     * storage path/object-id that can later be used to fetch it back.
     */
    String upload(CloudProvider provider, File localFile, String targetFileName);

    /**
     * Downloads the object at storagePath into a local file and returns it.
     */
    File download(CloudProvider provider, String storagePath);

    /**
     * Deletes the object at storagePath. Used for retention-policy cleanup.
     */
    void delete(CloudProvider provider, String storagePath);

    /** Used by the dashboard to report how much space a provider is using */
    long getUsedStorageBytes(CloudProvider provider);
}
