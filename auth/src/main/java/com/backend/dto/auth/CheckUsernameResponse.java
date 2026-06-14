package com.backend.dto.auth;

import lombok.Builder;

@Builder
public record CheckUsernameResponse (
        Boolean is_unique
){

}
