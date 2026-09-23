package com.cloudnest.repository;

import com.cloudnest.entity.BackupFile;
import com.cloudnest.entity.BackupJob;
import com.cloudnest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BackupFileRepository extends JpaRepository<BackupFile, Long> {

    List<BackupFile> findByBackupJob(BackupJob backupJob);

    /** Legacy — not user-scoped. Kept for backward compat. */
    List<BackupFile> findByOriginalFileNameOrderByVersionNumberDesc(String originalFileName);

    /**
     * User-scoped version lookup: highest version number for this user + filename.
     * Used to compute the next version number on new backups.
     */
    @Query("SELECT COALESCE(MAX(f.versionNumber), 0) FROM BackupFile f " +
           "WHERE f.backupJob.user = :user AND f.originalFileName = :fileName")
    int findMaxVersionForUserAndFileName(@Param("user") User user,
                                         @Param("fileName") String fileName);

    /** For the migration: rows that predate dedup and have no blob. */
    @Query("SELECT f FROM BackupFile f WHERE f.contentBlob IS NULL")
    List<BackupFile> findFilesWithoutBlob();

    /** Count physical bytes the user is logically storing (sum of plaintext sizes). */
    @Query("SELECT COALESCE(SUM(f.fileSizeBytes), 0) FROM BackupFile f WHERE f.backupJob.user = :user")
    long sumLogicalBytesByUser(@Param("user") User user);
}