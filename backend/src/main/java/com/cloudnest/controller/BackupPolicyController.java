package com.cloudnest.controller;

import com.cloudnest.dto.BackupPolicyRequest;
import com.cloudnest.entity.BackupPolicy;
import com.cloudnest.entity.Industry;
import com.cloudnest.security.CurrentUserProvider;
import com.cloudnest.service.BackupPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class BackupPolicyController {

    private final BackupPolicyService backupPolicyService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<BackupPolicy>> list() {
        return ResponseEntity.ok(backupPolicyService.getUserPolicies(currentUserProvider.getCurrentUser()));
    }

    @PostMapping
    public ResponseEntity<BackupPolicy> create(@Valid @RequestBody BackupPolicyRequest request) {
        return ResponseEntity.ok(backupPolicyService.createPolicy(currentUserProvider.getCurrentUser(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        backupPolicyService.deactivatePolicy(currentUserProvider.getCurrentUser(), id);
        return ResponseEntity.noContent().build();
    }

    /** Lets the frontend show recommended defaults per industry before the user submits a policy */
    @GetMapping("/industry-presets")
    public ResponseEntity<Industry[]> industryPresets() {
        return ResponseEntity.ok(Industry.values());
    }
}
