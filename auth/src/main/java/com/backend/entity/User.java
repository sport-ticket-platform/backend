package com.backend.entity;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "users",
        indexes = {
            @Index(name = "idx_user_role", columnList = "role"),
            @Index(name = "idx_user_is_active", columnList = "isActive")
        }
)
public class User {
    @Id /**/ @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String number;

    @Column
    private Boolean numberVerified;

    @Column
    private Boolean emailVerified;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false) /* */ @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(length = 256)
    private String suspendReason;

    @Column(nullable = false)
    private boolean isCredentialExpired = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastUpdate;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void preUpdate() {
        lastUpdate = LocalDateTime.now();
    }
}