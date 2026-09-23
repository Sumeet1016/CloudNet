package com.cloudnest.dto;

import com.cloudnest.entity.ProviderType;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for POST /api/cloud-providers/connect.
 *
 * Field usage depends on type:
 *  - GOOGLE_DRIVE: authorizationCode = OAuth access token
 *  - UPSTASH_BLOB: authorizationCode = secret key,
 *                  accessKeyId       = access key id,
 *                  bucketName        = optional bucket override
 *  - LOCAL_DISK / SIMULATED_S3: none required
 */
public class CloudProviderRequest {

    @NotNull
    private ProviderType type;

    private String displayName;

    private String authorizationCode;

    private String accessKeyId;

    private String bucketName;

    public ProviderType getType() {
        return type;
    }

    public void setType(ProviderType type) {
        this.type = type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}