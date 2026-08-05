package com.cloudnest.dto;

import com.cloudnest.entity.Industry;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BackupPolicyRequest {
    @NotBlank
    private String name;

    private Industry industry;

    /** Optional overrides - if null, defaults from the Industry enum are used */
    private String cronExpression;
    private Integer retentionDays;
    private Boolean encryptionEnabled;
}
