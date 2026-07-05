package com.backend.service.auth;

import com.backend.common.ApiMessage;
import com.backend.dto.auth.*;
import com.backend.dto.user.UserDto;
import com.backend.handler.AuthException;
import com.backend.handler.UserSuspendException;
import com.backend.security.userdetails.CustomUserDetails;
import com.backend.service.RefreshTokenService;
import com.backend.grpc.*;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public LoginResponse login(LoginRequest loginRequest, String ip, String userAgent, String deviceId) {
        log.info("Attempting to authenticate user: {}", loginRequest.identifier());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.identifier(),
                            loginRequest.password()
                    )
            );
        } catch (BadCredentialsException | CredentialsExpiredException e) {
            log.warn("Authentication failed for identifier {}: {}", loginRequest.identifier(), e.getMessage());
            throw e;
        } catch (org.springframework.security.authentication.AuthenticationServiceException e) {
            log.error("Infrastructure error during auth for {}: {}", loginRequest.identifier(), e.getMessage());
            throw e;
        } catch (AuthenticationException e) {
            log.error("Authentication error for identifier {}: {}", loginRequest.identifier(), e.getMessage());
            throw new IllegalStateException("Authentication error occurred", e);
        } catch (Exception e) {
            log.error("Unexpected error during authentication for identifier {}: {}", loginRequest.identifier(), e.getMessage());
            throw new IllegalStateException("Internal server error", e);
        }

        UserDto user = ((CustomUserDetails) authentication.getPrincipal()).getUser();

        checkUserLocked();
        checkUserSuspend(user);

        log.info("User with id: {} authenticated successfully. Generating refresh-token...", user.getId());

        String token = refreshTokenService.createRefreshToken(user.getId(), ip, userAgent, deviceId);

        return LoginResponse.builder()
                .token(token)
                .build();
    }

    private void checkUserSuspend(UserDto user) {
        if (!user.isActive()) {
            log.warn("User with id {} is suspend", user.getId());
            throw new UserSuspendException("User suspended", "Account is deactivated.");
        }
    }

    private void checkUserLocked() {
        // TODO
    }

    private void checkEmailUnique(String email) {
        CheckExistsResponse response = userServiceStub.checkEmailExists(
                CheckEmailExistsRequest.newBuilder().setEmail(email).build()
        );
        if (response.getExists()) {
            log.warn("Signup failed. Email [{}] is already registered.", email);
            throw new AuthException(ApiMessage.SIGNUP_EMAIL_TAKEN);
        }
    }

    private void checkPhoneUnique(String phone) {
        CheckExistsResponse response = userServiceStub.checkPhoneExists(
                CheckPhoneExistsRequest.newBuilder().setPhone(phone).build()
        );
        if (response.getExists()) {
            log.warn("Signup failed. Phone [{}] is already registered.", phone);
            throw new AuthException(ApiMessage.SIGNUP_PHONE_TAKEN, "phone");
        }
    }

    public SignupResponse signup(SignupRequest request) {
        String email = request.email() != null ? request.email().toLowerCase().trim() : "";
        String phone = request.phone() != null ? request.phone().trim() : "";

        checkEmailUnique(email);
        checkPhoneUnique(phone);

        try {
            CreateUserRequest grpcRequest = CreateUserRequest.newBuilder()
                    .setEmail(email)
                    .setPhone(phone)
                    .setFirstName(request.first_name().trim())
                    .setLastName(request.last_name().trim())
                    .setPassword(passwordEncoder.encode(request.password().trim()))
                    .build();

            CreateUserResponse response = userServiceStub.createUser(grpcRequest);
            log.info("User successfully registered via gRPC. User ID: {}", response.getUserId());

            return SignupResponse.builder()
                    .user_id(response.getUserId())
                    .build();

        } catch (StatusRuntimeException e) {
            log.warn("gRPC error during signup. Status: {}", e.getStatus().getCode(), e);

            throw new AuthException(ApiMessage.SIGNUP_INTERNAL_ERROR, "Registration failed on server.");
        }
    }
}