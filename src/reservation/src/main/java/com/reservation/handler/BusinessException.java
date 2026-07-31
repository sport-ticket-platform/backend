package com.reservation.handler;

import com.reservation.common.ApiMessage;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ApiMessage apiMessage;
    private final Object data;

    public BusinessException(ApiMessage apiMessage) {
        super(apiMessage.getMessage());
        this.apiMessage = apiMessage;
        this.data = null;
    }

    public BusinessException(ApiMessage apiMessage, Object data) {
        super(apiMessage.getMessage());
        this.apiMessage = apiMessage;
        this.data = data;
    }
}