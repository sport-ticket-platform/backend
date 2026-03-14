package com.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor

@Entity
@Table(
        name = "refresh_token",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_token", columnList = "token")
        }
)
public class RefreshToken {
    @Id /**/ @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @JoinColumn(nullable = false) /**/ @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column
    private LocalDateTime revokedAt;

    @Column
    private String revokedReason;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private String userAgent;

    @Column(nullable = false)
    private String deviceId;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
