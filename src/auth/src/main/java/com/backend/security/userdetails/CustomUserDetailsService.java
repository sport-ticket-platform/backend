package com.backend.security.userdetails;

import com.backend.dto.user.UserDto;
import com.backend.dto.user.UserRole;
import com.backend.grpc.GetUserLoginInfoByEmailRequest;
import com.backend.grpc.GetUserLoginInfoByIdRequest;
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

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @Override
    public UserDetails loadUserByUsername(@NotNull String identifier) throws UsernameNotFoundException {
        if (identifier.contains("@")) {
            return loadUserByEmailViaGrpc(identifier);
        } else {
            return loadUserByPhoneViaGrpc(identifier);
        }
    }

    public UserDetails loadUserById(@NotNull Long id) throws UsernameNotFoundException {
        UserLoginInfoResponse grpcResponse;

        try {
            grpcResponse = userServiceStub.getUserById(
                    GetUserLoginInfoByIdRequest.newBuilder()
                            .setId(id)
                            .build()
            );
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                log.warn("User not found with id: {}", id);
                throw new UsernameNotFoundException("User not found");
            }

            log.error("gRPC error while fetching user by id: {}", id, e);
            throw new AuthenticationServiceException(
                    "Error connecting to user service", e
            );
        }

        return mapToUserDetails(grpcResponse, grpcResponse.getEmail());
    }

    private CustomUserDetails loadUserByEmailViaGrpc(String email) {
        UserLoginInfoResponse grpcResponse;

        try {
            grpcResponse = userServiceStub.getUserByEmail(
                    GetUserLoginInfoByEmailRequest.newBuilder()
                            .setEmail(email)
                            .build()
            );
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                log.warn("User not found via gRPC with email: {}", email);
                throw new UsernameNotFoundException("User not found");
            }

            log.error("gRPC error while fetching user by email: {}", email, e);
            throw new AuthenticationServiceException(
                    "Error connecting to user service", e
            );
        }

        return mapToUserDetails(grpcResponse, email);
    }

    private CustomUserDetails loadUserByPhoneViaGrpc(String phone) {
        UserLoginInfoResponse grpcResponse;

        try {
            grpcResponse = userServiceStub.getUserByPhone(
                    GetUserLoginInfoByPhoneRequest.newBuilder()
                            .setPhone(phone)
                            .build()
            );
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                log.warn("User not found via gRPC with phone: {}", phone);
                throw new UsernameNotFoundException("User not found");
            }

            log.error("gRPC error while fetching user by phone: {}", phone, e);
            throw new AuthenticationServiceException(
                    "Error connecting to user service", e
            );
        }

        return mapToUserDetails(grpcResponse, phone);
    }

    private CustomUserDetails buildCustomUserDetails(UserDto userDto, String username) {
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

    private CustomUserDetails mapToUserDetails(
            UserLoginInfoResponse grpcResponse,
            String username
    ) {
        UserDto userDto = UserDto.builder()
                .id(grpcResponse.getId())
                .email(grpcResponse.getEmail())
                .phone(grpcResponse.getPhone())
                .password(grpcResponse.getPassword())
                .role(UserRole.valueOf(grpcResponse.getRole()))
                .isActive(grpcResponse.getStatus())
                .isTwoFactorEnabled(grpcResponse.getIsTwoFactorEnabled())
                .build();

        return buildCustomUserDetails(userDto, username);
    }
}