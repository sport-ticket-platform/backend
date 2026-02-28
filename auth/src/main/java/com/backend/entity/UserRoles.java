package com.backend.entity;

import lombok.Getter;

@Getter
public enum UserRoles {
    USER(1),
    ADMIN(2),
    MODERATOR(3);

    final int level;

    UserRoles(int level) {
        this.level = level;
    }
}
