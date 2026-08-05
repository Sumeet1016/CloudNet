package com.cloudnest.repository;

import com.cloudnest.entity.BackupSchedule;
import com.cloudnest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BackupScheduleRepository extends JpaRepository<BackupSchedule, Long> {
    List<BackupSchedule> findByUser(User user);
    List<BackupSchedule> findByActiveTrue();
    Optional<BackupSchedule> findByIdAndUser(Long id, User user);
}
