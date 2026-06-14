package com.backend.handler;

import com.backend.common.ApiMessage;
import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {
    private final ApiMessage apiMessage;
    private final String fieldName;

    // for special fields
    public AuthException(ApiMessage apiMessage, String fieldName) {
        super(apiMessage.getMessage());
        this.apiMessage = apiMessage;
        this.fieldName = fieldName;
    }

    // for global Fields
    public AuthException(ApiMessage apiMessage) {
        super(apiMessage.getMessage());
        this.apiMessage = apiMessage;
        this.fieldName = null;
    }
}