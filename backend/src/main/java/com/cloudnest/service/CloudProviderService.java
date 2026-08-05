package com.cloudnest.service;

import com.cloudnest.dto.CloudProviderRequest;
import com.cloudnest.entity.CloudProvider;
import com.cloudnest.entity.ProviderType;
import com.cloudnest.entity.User;
import com.cloudnest.repository.CloudProviderRepository;
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
     * - GOOGLE_DRIVE: expects an OAuth access token to already have been
     *   exchanged on the frontend (or via /google/callback) and passed in
     *   as request.authorizationCode for simplicity in this project scope.
     */
    public CloudProvider connectProvider(User user, CloudProviderRequest request) {
        CloudProvider provider = CloudProvider.builder()
                .user(user)
                .type(request.getType())
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : defaultName(request.getType()))
                .accessToken(request.getAuthorizationCode()) // for GOOGLE_DRIVE this holds the access token
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

    private String defaultName(ProviderType type) {
        return switch (type) {
            case GOOGLE_DRIVE -> "Google Drive";
            case LOCAL_DISK -> "Local Disk";
            case SIMULATED_S3 -> "Simulated S3 Bucket";
        };
    }
}
