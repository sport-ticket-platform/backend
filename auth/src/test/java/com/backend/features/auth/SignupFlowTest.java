package com.backend.features.auth;

import com.backend.common.ApiMessage;
import com.backend.config.ApplicationProperties;
import com.backend.dto.auth.CheckUsernameRequest;
import com.backend.dto.auth.SignupRequest;
import com.backend.entity.User;
import com.backend.entity.UserRole;
import com.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

@SpringBootTest
@Transactional
class SignupFlowTest {

    @Autowired
    private ApplicationProperties appPrp;
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        }
    }

    @Test
    @DisplayName("1. Validation Test: Should return 400 when username is empty")
    void checkUsername_WhenUsernameIsEmpty_ShouldTriggerGlobalExceptionHandler() throws Exception {

        CheckUsernameRequest blankUsername = CheckUsernameRequest.builder()
                .username("")
                .build();
        mockMvc.perform(post("/api/v1/auth/check-username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankUsername)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.data.username[*].message", hasItem(ApiMessage.SIGNUP_USERNAME_REQUIRED.getMessage())))
                .andExpect(jsonPath("$.data.username[*].message", hasItem(ApiMessage.SIGNUP_USERNAME_SIZE.getMessage())));


        CheckUsernameRequest nullUsername = CheckUsernameRequest.builder()
                .username(null)
                .build();
        mockMvc.perform(post("/api/v1/auth/check-username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullUsername))
                ).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.data.username[0].message").value(ApiMessage.SIGNUP_USERNAME_REQUIRED.getMessage()));
    }

    @Test
    @DisplayName("2. Database Test: Should return is_unique=true for a fresh username")
    void checkUsername_WhenUsernameIsNew_ShouldReturnUniqueTrue() throws Exception {
        CheckUsernameRequest validRequest = CheckUsernameRequest.builder()
                .username("unique_user")
                .build();

        mockMvc.perform(post("/api/v1/auth/check-username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_unique").value(true));
    }

    @Test
    @DisplayName("3. Database Test: Should return is_unique=false if username already exists in DB")
    void checkUsername_WhenUsernameExistsInDb_ShouldReturnUniqueFalse() throws Exception {
        User existingUser = new User();
        existingUser.setUsername("user");
        existingUser.setPassword("hashed_pass");
        existingUser.setRole(UserRole.USER);
        existingUser.setFirstName("taha");
        existingUser.setLastName("taha");
        userRepository.save(existingUser);

        assertTrue(userRepository.existsByUsername("user"));

        CheckUsernameRequest request = CheckUsernameRequest.builder()
                .username("user")
                .build();

        mockMvc.perform(post("/api/v1/auth/check-username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_unique").value(false));
    }

    @Test
    @DisplayName("4. Rate limit test: should return error 429 (too many request)")
    void checkUsername_WhenRateLimitExceeded_ShouldTriggerGlobalExceptionHandler() throws Exception {

        int maxRequests = appPrp.getEndpointLimitsPerMin().getCheckUsername();

        CheckUsernameRequest validRequest = CheckUsernameRequest.builder()
                .username("rate_limit_tester")
                .build();

        // fake ip, because in previous tests we use a few time of default ip
        String testIp = "192.41.1.2";

        for (int i = 0; i < maxRequests; i++) {
            mockMvc.perform(post("/api/v1/auth/check-username")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk());
        }

        // This request should be blocked
        mockMvc.perform(post("/api/v1/auth/check-username")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value(ApiMessage.TOO_MANY_REQUESTS.getTitle()))
                .andExpect(jsonPath("$.message").value(ApiMessage.TOO_MANY_REQUESTS.getMessage()));
    }

    @Test
    @DisplayName("5. Validation test: Should return 400 for invalid data or not unique username")
    void signup_WhenEntriesAreInvalid_ShouldTriggerGlobalExceptionHandler() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("")
                .first_name("")
                .last_name("")
                .password("").build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value(ApiMessage.SIGNUP_VALIDATION_FAILED.getTitle()))
                .andExpect(jsonPath("$.message").value(ApiMessage.SIGNUP_VALIDATION_FAILED.getMessage()))

                // Password Errors
                .andExpect(jsonPath("$.data.password[0].message").value(ApiMessage.SIGNUP_PASSWORD_REQUIRED.getMessage()))
                .andExpect(jsonPath("$.data.password[1].message").value(ApiMessage.SIGNUP_PASSWORD_SIZE.getMessage()))
                .andExpect(jsonPath("$.data.password[2].message").value(ApiMessage.SIGNUP_PASSWORD_WEAK.getMessage()))

                // names
                .andExpect(jsonPath("$.data.last_name[0].message").value(ApiMessage.SIGNUP_LASTNAME_REQUIRED.getMessage()))
                .andExpect(jsonPath("$.data.last_name[1].message").value(ApiMessage.SIGNUP_LASTNAME_SIZE.getMessage()))
                .andExpect(jsonPath("$.data.last_name[2].message").value(ApiMessage.SIGNUP_NAME_FORMAT.getMessage()))
                .andExpect(jsonPath("$.data.first_name[0].message").value(ApiMessage.SIGNUP_FIRSTNAME_REQUIRED.getMessage()))
                .andExpect(jsonPath("$.data.first_name[1].message").value(ApiMessage.SIGNUP_FIRSTNAME_SIZE.getMessage()))
                .andExpect(jsonPath("$.data.first_name[2].message").value(ApiMessage.SIGNUP_NAME_FORMAT.getMessage()))

                // username
                .andExpect(jsonPath("$.data.username[0].message").value(ApiMessage.SIGNUP_USERNAME_REQUIRED.getMessage()))
                .andExpect(jsonPath("$.data.username[1].message").value(ApiMessage.SIGNUP_USERNAME_SIZE.getMessage()))
                .andExpect(jsonPath("$.data.username[2].message").value(ApiMessage.SIGNUP_USERNAME_FORMAT.getMessage()));

        // Username check
        User existingUser = new User();
        existingUser.setUsername("taha127119839");
        existingUser.setPassword("Taha1111");
        existingUser.setRole(UserRole.USER);
        existingUser.setFirstName("taha");
        existingUser.setLastName("taha");
        userRepository.save(existingUser);

        SignupRequest request2 = SignupRequest.builder()
                .username("taha127119839")
                .first_name("ali")
                .last_name("ali")
                .password("Ta12ag2jah").build();

        mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.username[0].message").value(ApiMessage.SIGNUP_USERNAME_TAKEN.getMessage()));
    }

    @Test
    @DisplayName("6. Database Test: should return registered userID")
    void signup_WhenEntriesAreOk_ShouldReturnRegisteredUserID() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .username("ali1299819")
                .first_name("ali")
                .last_name("ali")
                .password("Ali11111").build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.title").value(ApiMessage.SIGNUP_SUCCESS.getTitle()))
                .andExpect(jsonPath("$.message").value(ApiMessage.SIGNUP_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.user_id").exists());
    }

    @Test
    @DisplayName("7. Rate limit test: should return error 429 (too many request)")
    void signup_WhenRateLimitExceeded_ShouldTriggerGlobalExceptionHandler() throws Exception {

        int maxRequests = appPrp.getEndpointLimitsPerMin().getSignup();

        // fake ip, because in previous tests we use a few time of default ip
        String testIp = "192.12.14.2";

        for (int i = 0; i < maxRequests; i++) {
            SignupRequest validRequest = SignupRequest.builder()
                    .username("rate_limit_tester_4209_" + i)
                    .first_name("taha")
                    .last_name("taha")
                    .password("Taha1111")
                    .build();
            mockMvc.perform(post("/api/v1/auth/signup")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        // This request should be blocked
        SignupRequest validRequest = SignupRequest.builder()
                .username("rate_limit_tester_4208")
                .first_name("taha")
                .last_name("taha")
                .password("Taha1111")
                .build();
        mockMvc.perform(post("/api/v1/auth/signup")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value(ApiMessage.TOO_MANY_REQUESTS.getTitle()))
                .andExpect(jsonPath("$.message").value(ApiMessage.TOO_MANY_REQUESTS.getMessage()));
    }
}