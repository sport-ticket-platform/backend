package com.backend.features.auth;

import com.backend.common.ApiMessage;
import com.backend.dto.auth.CheckUsernameRequest;
import com.backend.entity.User;
import com.backend.entity.UserRole;
import com.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
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
    @DisplayName("2. Database Test: Should return isUnique=true for a fresh username")
    void checkUsername_WhenUsernameIsNew_ShouldReturnUniqueTrue() throws Exception {
        CheckUsernameRequest validRequest = CheckUsernameRequest.builder()
                .username("unique-user")
                .build();

        mockMvc.perform(post("/api/v1/auth/check-username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isUnique").value(true));
    }

    @Test
    @DisplayName("3. Database Test: Should return isUnique=false if username already exists in DB")
    void checkUsername_WhenUsernameExistsInDb_ShouldReturnUniqueFalse() throws Exception {
        User existingUser = new User();
        existingUser.setUsername("user");
        existingUser.setEmail("test@test.com");
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
                .andExpect(jsonPath("$.data.isUnique").value(false));
    }
}