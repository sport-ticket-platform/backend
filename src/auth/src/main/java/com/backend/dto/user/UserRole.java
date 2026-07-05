package com.backend.dto.user;

import lombok.Getter;

@Getter
public enum UserRole {
    USER(1),
    SUPPORT(2),
    ADMIN(3);

    private final int level;

    UserRole(int level) {
        this.level = level;
    }
}