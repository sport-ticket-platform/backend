package com.backend.security.userdetails;

import com.backend.dto.user.UserDto;
import com.backend.dto.user.UserRole;
import com.backend.grpc.GetUserLoginInfoByEmailRequest;
import com.backend.grpc.GetUserLoginInfoByIdRequest; // باید در proto اضافه شده باشد
import com.backend.grpc.GetUserLoginInfoByPhoneRequest;
import com.backend.grpc.UserLoginInfoResponse;
import com.backend.grpc.UserServiceGrpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * <h2>Service to adapt {@link UserDto} to {@link CustomUserDetails} via gRPC</h2>
 *
 * <p>Loads user by ID, email, or phone number</p>
 *
 * @since 1.0.0
 * @version 2.0.0
 * @author logTAHA
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @Override
    public UserDetails loadUserByUsername(@NotNull String identifier) throws UsernameNotFoundException {
        UserLoginInfoResponse grpcResponse;

        try {
            // check that entry is email or phone-number
            if (identifier.contains("@")) {
                grpcResponse = userServiceStub.getUserByEmail(
                        GetUserLoginInfoByEmailRequest.newBuilder().setEmail(identifier).build()
                );
            } else {
                grpcResponse = userServiceStub.getUserByPhone(
                        GetUserLoginInfoByPhoneRequest.newBuilder().setPhone(identifier).build()
                );
            }
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                log.warn("User not found with identifier: {}", identifier);
                throw new UsernameNotFoundException("User not found");
            }
            log.error("gRPC error while fetching user by identifier: {}", identifier, e);
            throw new AuthenticationServiceException("Error connecting to user service", e);
        }

        return mapToUserDetails(grpcResponse, identifier);
    }

    public UserDetails loadUserById(@NotNull Long id) throws UsernameNotFoundException {
        UserLoginInfoResponse grpcResponse;

        try {
            grpcResponse = userServiceStub.getUserById(
                    GetUserLoginInfoByIdRequest.newBuilder().setId(id).build()
            );
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                log.warn("User not found with id: {}", id);
                throw new UsernameNotFoundException("User not found");
            }
            log.error("gRPC error while fetching user by id: {}", id, e);
            throw new AuthenticationServiceException("Error connecting to user service", e);
        }

        String username = grpcResponse.getEmail();

        return mapToUserDetails(grpcResponse, username);
    }

    private CustomUserDetails mapToUserDetails(UserLoginInfoResponse grpcResponse, String username) {
        UserDto userDto = UserDto.builder()
                .id(grpcResponse.getId())
                .email(grpcResponse.getEmail())
                .phone(grpcResponse.getPhone())
                .password(grpcResponse.getPassword())
                .role(UserRole.valueOf(grpcResponse.getRole()))
                .isActive(grpcResponse.getStatus())
                .isTwoFactorEnabled(grpcResponse.getIsTwoFactorEnabled())
                .build();

        return CustomUserDetails.builder()
                .user(userDto)
                .id(userDto.getId())
                .username(username)
                .password(userDto.getPassword())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + userDto.getRole().name())
                ))
                .enabled(userDto.isActive())
                .build();
    }
}