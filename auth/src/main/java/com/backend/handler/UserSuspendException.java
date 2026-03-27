package com.backend.handler;

import lombok.Getter;
import org.springframework.security.authentication.DisabledException;

@Getter
public class UserSuspendException extends DisabledException {
    private final String banReason;

    public UserSuspendException(String message, String banReason) {
        super(message);
        this.banReason = banReason;
    }
}
