package com.cloudnest.repository;

import com.cloudnest.entity.BackupJob;
import com.cloudnest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BackupJobRepository extends JpaRepository<BackupJob, Long> {
    List<BackupJob> findByUserOrderByStartedAtDesc(User user);
    Optional<BackupJob> findByIdAndUser(Long id, User user);
    long countByUserAndStatus(User user, BackupJob.BackupStatus status);
}
