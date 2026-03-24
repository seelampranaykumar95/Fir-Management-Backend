package com.fir;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import com.fir.model.Fir;
import com.fir.model.FirCategory;
import com.fir.model.FirStatus;
import com.fir.model.User;
import com.fir.model.UserRole;
import com.fir.repository.FirRepository;
import com.fir.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class BackendContractIntegrationTest {

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

    private User citizen;
    private User admin;

    @BeforeEach
    void setUp() {
        firRepository.deleteAll();
        userRepository.deleteAll();

        citizen = saveUser("Citizen Contract", "citizen.contract@example.com", "800000000201", UserRole.CITIZEN);
        admin = saveUser("Admin Contract", "admin.contract@example.com", "800000000202", UserRole.ADMIN);

        Fir existingFir = new Fir();
        existingFir.setTitle("Existing FIR");
        existingFir.setDescription("Existing FIR for contract checks");
        existingFir.setLocation("Hyderabad");
        existingFir.setCategory(FirCategory.THEFT);
        existingFir.setStatus(FirStatus.SUBMITTED);
        existingFir.setFiledBy(citizen);
        firRepository.save(existingFir);
    }

    @Test
    void citizenCanCreateFirWithoutFiledByUserId() throws Exception {
        String token = loginAndGetToken("citizen.contract@example.com", "Password@123");

        mockMvc.perform(post("/firs")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Frontend-style FIR",
                                  "description": "Created without filedByUserId",
                                  "location": "Secunderabad",
                                  "category": "THEFT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Frontend-style FIR"))
                .andExpect(jsonPath("$.status").value(FirStatus.SUBMITTED.name()))
                .andExpect(jsonPath("$.filedByUserId").value(citizen.getId()));
    }

    @Test
    void adminCanGetAllFirsAsPageResponse() throws Exception {
        String token = loginAndGetToken("admin.contract@example.com", "Password@123");

        mockMvc.perform(get("/firs?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].filedByUserId").value(citizen.getId()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void dashboardMatchesFrontendContract() throws Exception {
        String token = loginAndGetToken("admin.contract@example.com", "Password@123");

        mockMvc.perform(get("/reports/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalFirs").value(1))
                .andExpect(jsonPath("$.summary.activeFirs").value(1))
                .andExpect(jsonPath("$.summary.underReviewFirs").value(1))
                .andExpect(jsonPath("$.summary.investigationFirs").value(0))
                .andExpect(jsonPath("$.summary.closedFirs").value(0))
                .andExpect(jsonPath("$.summary.rejectedFirs").value(0))
                .andExpect(jsonPath("$.statusDistribution").isArray())
                .andExpect(jsonPath("$.categoryDistribution").isArray())
                .andExpect(jsonPath("$.monthlyTrend").isArray());
    }

    @Test
    void citizenCannotAccessAdminFirListing() throws Exception {
        String token = loginAndGetToken("citizen.contract@example.com", "Password@123");

        mockMvc.perform(get("/firs?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void corsPreflightAllowsLocalhostViteOrigin() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private String loginAndGetToken(String identifier, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(identifier, password))))
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

    private record LoginRequest(String identifier, String password) {
    }
}
