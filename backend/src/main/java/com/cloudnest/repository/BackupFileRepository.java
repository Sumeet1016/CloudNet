package com.cloudnest.repository;

import com.cloudnest.entity.BackupFile;
import com.cloudnest.entity.BackupJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackupFileRepository extends JpaRepository<BackupFile, Long> {
    List<BackupFile> findByBackupJob(BackupJob backupJob);
    List<BackupFile> findByOriginalFileNameOrderByVersionNumberDesc(String originalFileName);
}
