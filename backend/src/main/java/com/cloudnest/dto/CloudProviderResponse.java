package com.cloudnest.dto;

import com.cloudnest.entity.CloudProvider;
import com.cloudnest.entity.ProviderType;

import java.time.LocalDateTime;

/** Public provider representation. Credentials and the owning user must never be serialized. */
public record CloudProviderResponse(
        Long id, ProviderType type, String displayName, String bucketName,
        boolean connected, LocalDateTime connectedAt) {

    public static CloudProviderResponse from(CloudProvider provider) {
        return new CloudProviderResponse(provider.getId(), provider.getType(), provider.getDisplayName(),
                provider.getBucketName(), provider.isConnected(), provider.getConnectedAt());
    }
}
