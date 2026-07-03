package com.backend.security.userdetails;

import com.backend.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.backend.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * <h2>Service to adapt {@link User} to {@link CustomUserDetails}</h2>
 *
 * <p>for now, load user by username</p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NotNull String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(u -> CustomUserDetails.builder()
                        .user(u)
                        .id(u.getId())
                        .username(u.getUsername())
                        .password(u.getPassword())
                        .authorities(Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + u.getRole().name())
                        ))
                        .credentialsNonExpired(!u.isCredentialExpired())
                        .build()
                ).orElseThrow(() -> new UsernameNotFoundException("username not found"));
    }
}

