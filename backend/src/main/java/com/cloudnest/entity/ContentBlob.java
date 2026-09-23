package com.cloudnest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A physically-stored, deduplicated payload.
 *
 * Uniqueness is (user, cloudProvider, checksum). If two BackupFile rows
 * share the same content on the same provider, they point at the same
 * ContentBlob — the bytes are only uploaded once.
 *
 * refCount tracks how many BackupFile rows currently reference this blob.
 * When it hits zero, the physical file is deleted from the provider.
 */
@Entity
@Table(
    name = "content_blobs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_blob_user_provider_checksum",
        columnNames = {"user_id", "cloud_provider_id", "checksum"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentBlob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloud_provider_id", nullable = false)
    private CloudProvider cloudProvider;

    /** SHA-256 hex digest of the *plaintext* content. */
    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    /** Provider-specific path / object id where the encrypted bytes live. */
    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    /** Size of the plaintext (so we can report original file size, not ciphertext). */
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Size of what was actually uploaded (ciphertext, includes IV + tag). */
    @Column(name = "stored_size_bytes", nullable = false)
    private long storedSizeBytes;

    @Column(name = "encrypted", nullable = false)
    @Builder.Default
    private boolean encrypted = true;

    /** How many BackupFile rows point here. Deleted at zero. */
    @Column(name = "ref_count", nullable = false)
    @Builder.Default
    private long refCount = 1;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}