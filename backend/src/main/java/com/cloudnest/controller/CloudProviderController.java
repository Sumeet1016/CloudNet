package com.cloudnest.controller;

import com.cloudnest.dto.CloudProviderRequest;
import com.cloudnest.dto.CloudProviderResponse;
import com.cloudnest.dto.ProviderHealth;
import com.cloudnest.dto.ProviderQuota;
import com.cloudnest.dto.QuotaAlertResponse;
import com.cloudnest.entity.CloudProvider;
import com.cloudnest.security.CurrentUserProvider;
import com.cloudnest.service.CloudProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cloud-providers")
@RequiredArgsConstructor
public class CloudProviderController {

    private final CloudProviderService cloudProviderService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<CloudProviderResponse>> listProviders() {
        return ResponseEntity.ok(cloudProviderService.getUserProviders(currentUserProvider.getCurrentUser())
                .stream().map(CloudProviderResponse::from).toList());
    }

    @PostMapping("/connect")
    public ResponseEntity<CloudProviderResponse> connect(@Valid @RequestBody CloudProviderRequest request) {
        return ResponseEntity.ok(CloudProviderResponse.from(
                cloudProviderService.connectProvider(currentUserProvider.getCurrentUser(), request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disconnect(@PathVariable Long id) {
        cloudProviderService.disconnectProvider(currentUserProvider.getCurrentUser(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/usage")
    public ResponseEntity<Long> getUsage(@PathVariable Long id) {
        CloudProvider provider = cloudProviderService.getUserProviders(currentUserProvider.getCurrentUser())
                .stream().filter(p -> p.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        return ResponseEntity.ok(cloudProviderService.getUsageForProvider(provider));
    }

    @GetMapping("/{id}/quota")
    public ResponseEntity<ProviderQuota> getQuota(@PathVariable Long id) {
        CloudProvider provider = cloudProviderService.getOwnedProvider(currentUserProvider.getCurrentUser(), id);
        return ResponseEntity.ok(cloudProviderService.getQuotaForProvider(provider));
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<ProviderHealth> healthCheck(@PathVariable Long id) {
        CloudProvider provider = cloudProviderService.getOwnedProvider(currentUserProvider.getCurrentUser(), id);
        return ResponseEntity.ok(cloudProviderService.healthCheckProvider(provider));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<QuotaAlertResponse>> getAlerts() {
        return ResponseEntity.ok(cloudProviderService
                .getUnacknowledgedAlerts(currentUserProvider.getCurrentUser())
                .stream().map(QuotaAlertResponse::from).toList());
    }
}