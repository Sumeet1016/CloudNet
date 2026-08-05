package com.cloudnest.repository;

import com.cloudnest.entity.BackupPolicy;
import com.cloudnest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BackupPolicyRepository extends JpaRepository<BackupPolicy, Long> {
    List<BackupPolicy> findByUser(User user);
    Optional<BackupPolicy> findByIdAndUser(Long id, User user);
}
