package com.cloudnest.service;

import com.cloudnest.dto.BackupPolicyRequest;
import com.cloudnest.entity.BackupPolicy;
import com.cloudnest.entity.Industry;
import com.cloudnest.entity.User;
import com.cloudnest.repository.BackupPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BackupPolicyService {

    private final BackupPolicyRepository backupPolicyRepository;

    public List<BackupPolicy> getUserPolicies(User user) {
        return backupPolicyRepository.findByUser(user);
    }

    /** Creates a policy, auto-filling cron/retention from the Industry preset unless overridden */
    public BackupPolicy createPolicy(User user, BackupPolicyRequest request) {
        Industry industry = request.getIndustry() != null ? request.getIndustry() : user.getIndustry();

        BackupPolicy policy = BackupPolicy.builder()
                .user(user)
                .name(request.getName())
                .industry(industry)
                .cronExpression(request.getCronExpression() != null ? request.getCronExpression() : industry.getDefaultCron())
                .retentionDays(request.getRetentionDays() != null ? request.getRetentionDays() : industry.getDefaultRetentionDays())
                .encryptionEnabled(request.getEncryptionEnabled() == null || request.getEncryptionEnabled())
                .active(true)
                .build();

        return backupPolicyRepository.save(policy);
    }

    public BackupPolicy getPolicy(User user, Long id) {
        return backupPolicyRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));
    }

    public void deactivatePolicy(User user, Long id) {
        BackupPolicy policy = getPolicy(user, id);
        policy.setActive(false);
        backupPolicyRepository.save(policy);
    }
}
