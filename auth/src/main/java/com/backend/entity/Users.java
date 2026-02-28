package com.backend.entity;

import jakarta.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

@Entity
@Table(name = "users")
public class Users {
    @Id /**/ @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false) /* */ @Enumerated(EnumType.STRING)
    private UserRoles role;
    @Column(name = "is_active")
    private boolean isActive = true;
    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 1;

    public Users(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = UserRoles.valueOf(role);
    }
}