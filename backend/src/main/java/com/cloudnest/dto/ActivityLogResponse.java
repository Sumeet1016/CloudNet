package com.cloudnest.dto;

import com.cloudnest.entity.ActivityLog;

import java.time.LocalDateTime;

/** Public activity entry, deliberately excluding its lazy-loaded User relation. */
public record ActivityLogResponse(
        Long id, String action, String details, ActivityLog.LogLevel level, LocalDateTime createdAt) {

    public static ActivityLogResponse from(ActivityLog log) {
        return new ActivityLogResponse(log.getId(), log.getAction(), log.getDetails(), log.getLevel(), log.getCreatedAt());
    }
}
