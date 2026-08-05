package com.cloudnest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backup_job_id", nullable = false)
    private BackupJob backupJob;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    /** Path or remote object id where the (encrypted) file actually lives */
    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    @Column(name = "file_size_bytes")
    private long fileSizeBytes;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "version_number")
    @Builder.Default
    private int versionNumber = 1;

    @Column(name = "encrypted")
    @Builder.Default
    private boolean encrypted = true;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
