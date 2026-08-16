package com.backend.security.userdetails;

import com.backend.dto.user.UserDto;
import com.backend.dto.user.UserRole;
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
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(@NotNull String identifier) throws UsernameNotFoundException {
        if (identifier.contains("@")) {
            return loadUserByEmailDirectly(identifier);
        } else {
            return loadUserByPhoneViaGrpc(identifier);
        }
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

        return mapToUserDetails(grpcResponse, grpcResponse.getEmail());
    }

    private CustomUserDetails loadUserByEmailDirectly(String email) {
        String sql = "SELECT user_id, email, phone_number, password, role, is_active, two_factor_enabled FROM users WHERE email = ?";

        try {
            UserDto userDto = jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                            UserDto.builder()
                                    .id(rs.getLong("user_id"))
                                    .email(rs.getString("email"))
                                    .phone(rs.getString("phone_number"))
                                    .password(rs.getString("password"))
                                    .role(UserRole.valueOf(rs.getString("role")))
                                    .isActive(rs.getBoolean("is_active"))
                                    .isTwoFactorEnabled(rs.getBoolean("two_factor_enabled"))
                                    .build()
                    , email);

            return buildCustomUserDetails(userDto, email);

        } catch (EmptyResultDataAccessException e) {
            log.warn("User not found in Database with email: {}", email);
            throw new UsernameNotFoundException("User not found");
        } catch (Exception e) {
            log.error("Database error while fetching user directly by email: {}", email, e);
            throw new AuthenticationServiceException("Error querying database directly", e);
        }
    }

    private CustomUserDetails loadUserByPhoneViaGrpc(String phone) {
        UserLoginInfoResponse grpcResponse;
        try {
            grpcResponse = userServiceStub.getUserByPhone(
                    GetUserLoginInfoByPhoneRequest.newBuilder().setPhone(phone).build()
            );
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                log.warn("User not found via gRPC with phone: {}", phone);
                throw new UsernameNotFoundException("User not found");
            }
            log.error("gRPC error while fetching user by phone: {}", phone, e);
            throw new AuthenticationServiceException("Error connecting to user service", e);
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

        return buildCustomUserDetails(userDto, username);
    }
}