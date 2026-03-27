package com.backend.security.userdetails;

import com.backend.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
@Builder
public class CustomUserDetails implements UserDetails {

    private final User user;

    private final Long id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    @Builder.Default
    private final boolean accountNonExpired = true;

    @Builder.Default
    private final boolean accountNonLocked = true;

    /**
     * <p>Just this field check after auth in {@code postAuthenticationChecks}</p>
     * <p>
     *     Others: {@code accountNonExpired}, {@code accountNonLocked}, {@code enabled}
     *     will check before auth in {@code preAuthenticationChecks}
     * </p>
     */
    @Builder.Default
    private final boolean credentialsNonExpired = true;

    @Builder.Default
    private final boolean enabled = true;
}

