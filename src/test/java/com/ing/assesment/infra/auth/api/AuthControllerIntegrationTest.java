package com.ing.assesment.infra.auth.api;

import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.infra.auth.api.request.LoginRequest;
import com.ing.assesment.infra.auth.api.request.RefreshTokenRequest;
import com.ing.assesment.infra.auth.api.request.RegisterRequest;
import com.ing.assesment.infra.auth.persistence.entity.UserEntity;
import com.ing.assesment.infra.auth.persistence.repository.UserJpaRepository;
import com.ing.assesment.infra.common.AbstractIntegrationTest;
import com.ing.assesment.infra.security.ratelimit.RateLimitPolicy;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Set;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void cleanup() {
        rateLimitingFilter.clear();
        userJpaRepository.deleteAll();
    }

    @Test
    void registerSuccess() throws Exception {

        RegisterRequest request = new RegisterRequest("a@b.com", "password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated());

        assertTrue(userJpaRepository.findByEmail("a@b.com").isPresent());
    }

    @Test
    void registerDuplicateEmail() throws Exception {
        saveUser("dup@b.com",  "pass", Set.of(UserRole.CUSTOMER));

        RegisterRequest request = new RegisterRequest("dup@b.com", "password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginSuccess() throws Exception {
        saveUser("login@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        LoginRequest request = new LoginRequest("login@test.com", "Password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        UserEntity savedUser = userJpaRepository.findByEmail("login@test.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotNull(savedUser.getLastLoginAt());
    }

    @Test
    void loginShouldReturnUnauthorizedWhenUserNotFound() throws Exception {
        LoginRequest request = new LoginRequest("missing@test.com", "Password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldReturnUnauthorizedWhenPasswordIsWrong() throws Exception {
        saveUser("login@test.com", "CorrectPassword", Set.of(UserRole.CUSTOMER));

        LoginRequest request = new LoginRequest("login@test.com", "WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshSuccess() throws Exception {
        saveUser("refresh@test.com", "Password123",  Set.of(UserRole.CUSTOMER));

        LoginRequest loginRequest = new LoginRequest("refresh@test.com", "Password123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = JsonPath.read(loginResponse, "$.refreshToken");

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken").value(refreshToken))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void refreshShouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn429WhenRateLimitExceededOnLogin() throws Exception {

        saveUser("ratelimit@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        LoginRequest request = new LoginRequest("ratelimit@test.com", "Password123");

        String json = objectMapper.writeValueAsString(request);

        for (int i = 0; i < RateLimitPolicy.LOGIN.maxRequests(); i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests());
    }

    private void saveUser(String email, String password, Set<UserRole> roles) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setRoles(roles);
        user.setCreatedAt(Instant.now());
        userJpaRepository.saveAndFlush(user);
    }
}
