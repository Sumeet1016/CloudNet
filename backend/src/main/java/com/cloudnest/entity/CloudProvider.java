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

    /** OAuth access token (for Google Drive). Null/unused for local & simulated providers. */
    @Column(name = "access_token", length = 2048)
    private String accessToken;

    @Column(name = "refresh_token", length = 2048)
    private String refreshToken;

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
