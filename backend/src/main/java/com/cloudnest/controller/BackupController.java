package com.cloudnest.controller;

import com.cloudnest.entity.BackupFile;
import com.cloudnest.entity.BackupJob;
import com.cloudnest.entity.BackupPolicy;
import com.cloudnest.security.CurrentUserProvider;
import com.cloudnest.service.BackupPolicyService;
import com.cloudnest.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;
    private final BackupPolicyService backupPolicyService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<BackupJob>> listJobs() {
        return ResponseEntity.ok(backupService.getUserJobs(currentUserProvider.getCurrentUser()));
    }

    @PostMapping(value = "/run", consumes = "multipart/form-data")
    public ResponseEntity<BackupJob> runManualBackup(
            @RequestParam Long cloudProviderId,
            @RequestParam(required = false) Long policyId,
            @RequestParam(required = false) String jobName,
            @RequestParam("files") MultipartFile[] files) {

        var user = currentUserProvider.getCurrentUser();
        BackupPolicy policy = policyId != null ? backupPolicyService.getPolicy(user, policyId) : null;

        BackupJob job = backupService.runManualBackup(user, cloudProviderId, policy, jobName, files);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/{jobId}/files")
    public ResponseEntity<List<BackupFile>> listFiles(@PathVariable Long jobId) {
        return ResponseEntity.ok(backupService.getFileVersions(jobId, currentUserProvider.getCurrentUser()));
    }

    @GetMapping("/restore/{backupFileId}")
    public ResponseEntity<FileSystemResource> restore(@PathVariable Long backupFileId) {
        File restored = backupService.restoreFile(currentUserProvider.getCurrentUser(), backupFileId);
        FileSystemResource resource = new FileSystemResource(restored);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + restored.getName() + "\"")
                .body(resource);
    }
}
