package com.cloudnest.dto;

import java.time.LocalDateTime;

/**
 * Result of a health check against a storage provider.
 * status is one of: CONNECTED, DEGRADED, UNREACHABLE.
 */
public class ProviderHealth {

    public enum Status {
        CONNECTED,
        DEGRADED,
        UNREACHABLE
    }

    private Status status;
    private String message;
    private LocalDateTime checkedAt;

    public ProviderHealth() {
    }

    public ProviderHealth(Status status, String message, LocalDateTime checkedAt) {
        this.status = status;
        this.message = message;
        this.checkedAt = checkedAt;
    }

    public static ProviderHealth connected(String message) {
        return new ProviderHealth(Status.CONNECTED, message, LocalDateTime.now());
    }

    public static ProviderHealth degraded(String message) {
        return new ProviderHealth(Status.DEGRADED, message, LocalDateTime.now());
    }

    public static ProviderHealth unreachable(String message) {
        return new ProviderHealth(Status.UNREACHABLE, message, LocalDateTime.now());
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}