package com.backend.security.userdetails;

import com.backend.dto.user.UserDto;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
@Builder
public class CustomUserDetails implements UserDetails {

    private final UserDto user;

    private final Long id;

    // email or phone-number
    private final String username;

    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    @Builder.Default
    private final boolean accountNonExpired = true;

    @Builder.Default
    private final boolean accountNonLocked = true;

    @Builder.Default
    private final boolean credentialsNonExpired = true;

    @Builder.Default
    private final boolean enabled = true;
}