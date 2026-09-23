package com.cloudnest.service;

import com.cloudnest.dto.CloudProviderRequest;
import com.cloudnest.dto.ProviderHealth;
import com.cloudnest.dto.ProviderQuota;
import com.cloudnest.entity.CloudProvider;
import com.cloudnest.entity.ProviderType;
import com.cloudnest.entity.User;
import com.cloudnest.repository.CloudProviderRepository;
import com.cloudnest.service.storage.CloudStorageProvider;
import com.cloudnest.service.storage.StorageProviderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CloudProviderService {

    private final CloudProviderRepository cloudProviderRepository;
    private final StorageProviderFactory storageProviderFactory;
    private final ActivityLogService activityLogService;

    public List<CloudProvider> getUserProviders(User user) {
        return cloudProviderRepository.findByUser(user);
    }

    /**
     * Connects a new storage provider for the user.
     * - LOCAL_DISK / SIMULATED_S3: no external auth needed, connects instantly.
     * - GOOGLE_DRIVE: request.authorizationCode holds an OAuth access token.
     * - UPSTASH_BLOB: request.authorizationCode holds the secret key,
     *                 request.accessKeyId holds the access key id,
     *                 request.bucketName optionally overrides the bucket.
     */
    public CloudProvider connectProvider(User user, CloudProviderRequest request) {
        CloudProvider provider = CloudProvider.builder()
                .user(user)
                .type(request.getType())
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : defaultName(request.getType()))
                .accessToken(request.getAuthorizationCode())
                .accessKeyId(request.getAccessKeyId())
                .bucketName(request.getBucketName())
                .connected(true)
                .build();

        CloudProvider saved = cloudProviderRepository.save(provider);
        activityLogService.log(user, "PROVIDER_CONNECTED", "Connected " + request.getType());
        return saved;
    }

    public void disconnectProvider(User user, Long providerId) {
        CloudProvider provider = cloudProviderRepository.findByIdAndUser(providerId, user)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        provider.setConnected(false);
        cloudProviderRepository.save(provider);
        activityLogService.log(user, "PROVIDER_DISCONNECTED", "Disconnected " + provider.getType());
    }

    public long getUsageForProvider(CloudProvider provider) {
        return storageProviderFactory.getProvider(provider.getType()).getUsedStorageBytes(provider);
    }

    public ProviderQuota getQuotaForProvider(CloudProvider provider) {
        return storageProviderFactory.getProvider(provider.getType()).getQuota(provider);
    }

    public ProviderHealth healthCheckProvider(CloudProvider provider) {
        return storageProviderFactory.getProvider(provider.getType()).healthCheck(provider);
    }

    /** Resolve a provider that belongs to the given user, or throw. */
    public CloudProvider getOwnedProvider(User user, Long providerId) {
        return cloudProviderRepository.findByIdAndUser(providerId, user)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
    }

    private String defaultName(ProviderType type) {
        return switch (type) {
            case GOOGLE_DRIVE -> "Google Drive";
            case LOCAL_DISK -> "Local Disk";
            case SIMULATED_S3 -> "Simulated S3 Bucket";
            case BACKBLAZE_B2 -> "Backblaze B2 Bucket";
        };
    }
}
