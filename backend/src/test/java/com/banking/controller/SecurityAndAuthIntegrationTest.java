package com.banking.controller;

import com.banking.dto.request.LoginRequest;
import com.banking.dto.request.RegisterRequest;
import com.banking.dto.response.AuthResponse;
import com.banking.entity.User;
import com.banking.entity.enums.Role;
import com.banking.repository.UserRepository;
import com.banking.security.JwtTokenProvider;
import com.banking.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityAndAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Security: Protected endpoints reject requests with HTTP 401 when no JWT is provided")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Authentication: Full registration and login cycle issues valid JWT")
    void testRegistrationAndLogin() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        RegisterRequest regReq = new RegisterRequest(
                "authuser_" + suffix,
                "authuser_" + suffix + "@demo.com",
                "Password123!",
                "Security",
                "Tester"
        );

        // Register
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username").value(regReq.getUsername()))
                .andExpect(jsonPath("$.balance").isNumber());

        // Login
        LoginRequest loginReq = new LoginRequest(regReq.getUsername(), "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Authorization: User cannot inspect another user's balance (returns HTTP 403 Forbidden)")
    void testUnauthorizedAccountAccessBlocked() throws Exception {
        String suffix1 = UUID.randomUUID().toString().substring(0, 6);
        AuthResponse user1 = authService.register(new RegisterRequest(
                "user1_" + suffix1, "user1_" + suffix1 + "@demo.com", "Password123!", "User", "One"));

        String suffix2 = UUID.randomUUID().toString().substring(0, 6);
        AuthResponse user2 = authService.register(new RegisterRequest(
                "user2_" + suffix2, "user2_" + suffix2 + "@demo.com", "Password123!", "User", "Two"));

        // User 1 attempts to query User 2's balance endpoint /api/accounts/{accountId}/balance
        mockMvc.perform(get("/api/accounts/" + user2.getAccountId() + "/balance")
                        .header("Authorization", "Bearer " + user1.getToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Role RBAC: Regular user cannot call /api/admin endpoints (returns HTTP 403)")
    void testAdminEndpointRestricted() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        AuthResponse regularUser = authService.register(new RegisterRequest(
                "regular_" + suffix, "regular_" + suffix + "@demo.com", "Password123!", "Regular", "User"));

        mockMvc.perform(post("/api/admin/accounts/" + regularUser.getAccountId() + "/deposits")
                        .header("Authorization", "Bearer " + regularUser.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100.00, \"description\": \"Illegal self-credit\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Role RBAC: Admin user can access /api/admin endpoints successfully")
    void testAdminAuthorizedAccess() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        User adminUser = new User(
                "admin_" + suffix,
                "admin_" + suffix + "@demo.com",
                passwordEncoder.encode("AdminPass123!"),
                "System",
                "Administrator",
                Role.ROLE_ADMIN
        );
        adminUser = userRepository.save(adminUser);
        String adminToken = jwtTokenProvider.generateToken(adminUser);

        String userSuffix = UUID.randomUUID().toString().substring(0, 6);
        AuthResponse regularUser = authService.register(new RegisterRequest(
                "target_" + userSuffix, "target_" + userSuffix + "@demo.com", "Password123!", "Target", "User"));

        mockMvc.perform(post("/api/admin/accounts/" + regularUser.getAccountId() + "/deposits")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 500.00, \"description\": \"Authorized Admin Stimulus\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(500.00));
    }
}
