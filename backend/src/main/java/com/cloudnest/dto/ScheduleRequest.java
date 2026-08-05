package com.cloudnest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScheduleRequest {
    @NotNull
    private Long cloudProviderId;

    @NotNull
    private Long policyId;

    @NotBlank
    private String sourcePath;

    /** Optional override - if blank, the linked policy's cron expression is used */
    private String cronExpression;
}
