package com.fir;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fir.config.security.JwtTokenProvider;
import com.fir.model.Fir;
import com.fir.model.FirCategory;
import com.fir.model.FirStatus;
import com.fir.model.User;
import com.fir.model.UserRole;
import com.fir.repository.FirRepository;
import com.fir.repository.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class CitizenFirAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirRepository firRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User citizenOne;
    private User citizenTwo;
    private User admin;

    @BeforeEach
    void setUp() {
        firRepository.deleteAll();
        userRepository.deleteAll();

        citizenOne = saveUser("Citizen One", "citizen.one@example.com", "800000000101", UserRole.CITIZEN);
        citizenTwo = saveUser("Citizen Two", "citizen.two@example.com", "800000000102", UserRole.CITIZEN);
        admin = saveUser("Admin One", "admin.one@example.com", "800000000103", UserRole.ADMIN);

        Fir fir = new Fir();
        fir.setTitle("Citizen one FIR");
        fir.setDescription("Citizen-owned FIR");
        fir.setLocation("Hyderabad");
        fir.setCategory(FirCategory.THEFT);
        fir.setStatus(FirStatus.SUBMITTED);
        fir.setFiledBy(citizenOne);
        firRepository.save(fir);
    }

    @Test
    void loginResponseContainsMatchingUserIdClaim() throws Exception {
        String loginResponse = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "citizen.one@example.com",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(citizenOne.getId()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(loginResponse);
        String token = response.get("token").asText();

        org.junit.jupiter.api.Assertions.assertEquals(citizenOne.getId(), jwtTokenProvider.getUserIdFromJWT(token));
        org.junit.jupiter.api.Assertions.assertEquals(UserRole.CITIZEN.name(), jwtTokenProvider.getRoleFromJWT(token));
    }

    @Test
    void authMeReturnsCanonicalIdentity() throws Exception {
        String token = loginAndGetToken("admin.one@example.com", "Password@123");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(admin.getId()))
                .andExpect(jsonPath("$.email").value("admin.one@example.com"))
                .andExpect(jsonPath("$.role").value(UserRole.ADMIN.name()));
    }

    @Test
    void citizenCanAccessOwnFirs() throws Exception {
        String token = loginAndGetToken("citizen.one@example.com", "Password@123");

        mockMvc.perform(get("/firs/user/{userId}", citizenOne.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].filedByUserId").value(citizenOne.getId()));
    }

    @Test
    void citizenCanAccessOwnFirsViaMeEndpoint() throws Exception {
        String token = loginAndGetToken("citizen.one@example.com", "Password@123");

        mockMvc.perform(get("/firs/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].filedByUserId").value(citizenOne.getId()));
    }

    @Test
    void citizenCannotAccessAnotherUsersFirs() throws Exception {
        String token = loginAndGetToken("citizen.one@example.com", "Password@123");

        mockMvc.perform(get("/firs/user/{userId}", citizenTwo.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only access your own FIRs"));
    }

    @Test
    void invalidTokenGetsUnauthorized() throws Exception {
        mockMvc.perform(get("/firs/user/{userId}", citizenOne.getId())
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenGetsUnauthorizedOnMeEndpoint() throws Exception {
        mockMvc.perform(get("/firs/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenGetsUnauthorizedOnAuthMeEndpoint() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanAccessCitizenFirs() throws Exception {
        String token = loginAndGetToken("admin.one@example.com", "Password@123");

        mockMvc.perform(get("/firs/user/{userId}", citizenOne.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].filedByUserId").value(citizenOne.getId()));
    }

    @Test
    void adminCanLoadDashboardAfterLogin() throws Exception {
        String token = loginAndGetToken("admin.one@example.com", "Password@123");

        mockMvc.perform(get("/reports/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalFirs").value(1));
    }

    @Test
    void inactiveUserCannotLogin() throws Exception {
        User inactiveUser = saveUser("Inactive User", "inactive@example.com", "800000000104", UserRole.CITIZEN);
        inactiveUser.setActive(false);
        userRepository.save(inactiveUser);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "inactive@example.com",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndGetToken(String identifier, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "%s",
                                  "password": "%s"
                                }
                                """.formatted(identifier, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private User saveUser(String name, String email, String aadhaarNumber, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setAadhaarNumber(aadhaarNumber);
        user.setPassword(passwordEncoder.encode("Password@123"));
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }
}
