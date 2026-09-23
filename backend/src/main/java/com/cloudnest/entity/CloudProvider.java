package com.cloudnest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cloud_providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType type;

    @Column(name = "display_name")
    private String displayName;

    /** OAuth access token (Google Drive) or secret key (Upstash Blob). */
    @Column(name = "access_token", length = 2048)
    private String accessToken;

    @Column(name = "refresh_token", length = 2048)
    private String refreshToken;

    /** Access key id — used by S3-compatible providers like Upstash Blob. */
    @Column(name = "access_key_id", length = 512)
    private String accessKeyId;

    /** Optional per-provider bucket name override (Upstash Blob). */
    @Column(name = "bucket_name", length = 255)
    private String bucketName;

    @Column(name = "connected")
    @Builder.Default
    private boolean connected = true;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @PrePersist
    protected void onCreate() {
        this.connectedAt = LocalDateTime.now();
    }
}