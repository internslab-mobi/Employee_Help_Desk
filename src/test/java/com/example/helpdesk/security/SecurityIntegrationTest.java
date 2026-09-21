package com.example.helpdesk.security;

import com.example.helpdesk.dto.request.LoginRequest;
import com.example.helpdesk.dto.response.LoginResponse;
import com.example.helpdesk.enums.Role;
import com.example.helpdesk.repository.EmployeeRepository;
import com.example.helpdesk.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private EmployeeRepository employeeRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("A. Login endpoint is public and authenticates valid user credentials")
    void testLoginEndpointIsPublicAndAuthenticates() throws Exception {
        LoginRequest loginRequest = new LoginRequest("vikram.singh@example.com", "Admin@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("vikram.singh@example.com"));
    }

    @Test
    @DisplayName("B. Swagger UI & OpenAPI endpoints are public")
    void testSwaggerEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("C. Protected endpoint without JWT returns 401 Unauthorized")
    void testProtectedEndpointWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("D. Invalid JWT returns 401 Unauthorized")
    void testInvalidJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .header("Authorization", "Bearer invalid_token_xyz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("E. Valid EMPLOYEE JWT can access allowed employee endpoint")
    void testEmployeeRoleAccessAllowedEndpoint() throws Exception {
        String token = jwtService.generateToken(1L, "ravi.kumar@example.com", Role.EMPLOYEE.name());

        mockMvc.perform(get("/api/tickets/1/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("F. EMPLOYEE cannot access ADMIN-only endpoint -> 403 Forbidden")
    void testEmployeeAccessAdminEndpointReturns403() throws Exception {
        String token = jwtService.generateToken(1L, "ravi.kumar@example.com", Role.EMPLOYEE.name());

        mockMvc.perform(post("/api/sla/check-breaches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("G. AGENT can access allowed agent endpoint")
    void testAgentRoleAccessAllowedEndpoint() throws Exception {
        String token = jwtService.generateToken(3L, "arun.raj@example.com", Role.AGENT.name());

        mockMvc.perform(get("/api/tickets/1/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("H. MANAGER can access allowed manager endpoint")
    void testManagerRoleAccessAllowedEndpoint() throws Exception {
        String token = jwtService.generateToken(4L, "meena.joseph@example.com", Role.MANAGER.name());

        mockMvc.perform(get("/api/reports/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("I. ADMIN can access admin endpoint")
    void testAdminRoleAccessAdminEndpoint() throws Exception {
        String token = jwtService.generateToken(5L, "vikram.singh@example.com", Role.ADMIN.name());

        mockMvc.perform(post("/api/sla/check-breaches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("J, K, L, M. JwtService generates valid token containing subject, email, role, and JwtDecoder decodes it")
    void testJwtClaimsAndDecoder() {
        Long empId = 5L;
        String email = "vikram.singh@example.com";
        String role = "ADMIN";

        String token = jwtService.generateToken(empId, email, role);
        assertNotNull(token);

        Jwt decodedJwt = jwtDecoder.decode(token);
        assertEquals(empId.toString(), decodedJwt.getSubject());
        assertEquals(email, decodedJwt.getClaimAsString("email"));
        assertEquals(role, decodedJwt.getClaimAsString("role"));
        assertNotNull(decodedJwt.getIssuedAt());
        assertNotNull(decodedJwt.getExpiresAt());
    }

    @Test
    @DisplayName("N. Password authentication uses BCrypt encoder")
    void testPasswordBCryptAuthentication() {
        var employeeOpt = employeeRepository.findByEmail("vikram.singh@example.com");
        assertTrue(employeeOpt.isPresent());

        var employee = employeeOpt.get();
        assertTrue(passwordEncoder.matches("Admin@123", employee.getPasswordHash()));
        assertEquals(60, employee.getPasswordHash().length());
        assertTrue(employee.getPasswordHash().startsWith("$2a$") || employee.getPasswordHash().startsWith("$2b$"));
    }

    @Test
    @DisplayName("CRITICAL TEST: End-to-end flow: login -> real JWT -> Authorization: Bearer <JWT> -> JwtDecoder -> role conversion -> controller authorization")
    void testEndToEndRealJwtFlow() throws Exception {
        // 1. Perform login to get real JWT
        LoginRequest loginRequest = new LoginRequest("vikram.singh@example.com", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(responseJson, LoginResponse.class);
        String realToken = loginResponse.getToken();

        assertNotNull(realToken, "Real JWT token should not be null");

        // 2. Access protected ADMIN endpoint using real JWT token header
        mockMvc.perform(post("/api/sla/check-breaches")
                        .header("Authorization", "Bearer " + realToken))
                .andExpect(status().isOk());

        // 3. Perform login for EMPLOYEE to get real JWT token
        LoginRequest empLogin = new LoginRequest("ravi.kumar@example.com", "Employee@123");
        MvcResult empLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empLogin)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse empLoginResponse = objectMapper.readValue(empLoginResult.getResponse().getContentAsString(), LoginResponse.class);
        String realEmpToken = empLoginResponse.getToken();

        // 4. Access protected ADMIN endpoint with real EMPLOYEE token -> expect 403 Forbidden
        mockMvc.perform(post("/api/sla/check-breaches")
                        .header("Authorization", "Bearer " + realEmpToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
