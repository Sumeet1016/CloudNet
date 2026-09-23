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

    /**
     * Denormalized from ContentBlob for fast lookups and backward compat.
     * Still populated on every save — the source of truth is the blob.
     */
    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "file_size_bytes")
    private long fileSizeBytes;

    /** SHA-256 hex of the plaintext. Denormalized from ContentBlob. */
    @Column(name = "checksum")
    private String checksum;

    @Column(name = "version_number")
    @Builder.Default
    private int versionNumber = 1;

    @Column(name = "encrypted")
    @Builder.Default
    private boolean encrypted = true;

    /**
     * The deduplicated payload this file points to.
     * Nullable for pre-dedup rows that haven't been migrated yet.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_blob_id")
    private ContentBlob contentBlob;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}