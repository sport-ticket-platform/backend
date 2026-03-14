package com.backend.entity;

import jakarta.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor

@Entity
@Table(name = "users")
public class User {
    @Id /**/ @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false) /* */ @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false) // for force logout (check with refresh token)
    private int tokenVersion = 1;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
    }
}