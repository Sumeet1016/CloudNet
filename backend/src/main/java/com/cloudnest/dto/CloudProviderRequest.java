package com.cloudnest.dto;

import com.cloudnest.entity.ProviderType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CloudProviderRequest {
    @NotNull
    private ProviderType type;

    private String displayName;

    /** Only needed for GOOGLE_DRIVE - the OAuth authorization code from the frontend consent flow */
    private String authorizationCode;
}
