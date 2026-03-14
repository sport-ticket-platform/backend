package com.backend.entity;

import lombok.Getter;

@Getter
public enum UserRole {
    USER(1),
    ADMIN(2),
    MODERATOR(3);

    private final int level;

    UserRole(int level) {
        this.level = level;
    }
}
