package com.streamora.pet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamora.pet.identity.UserIdentityClient;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies public, personal and admin-cookie isolation behavior for the active pet API. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ActivePetFlowIntegrationTest.TestIdentityConfiguration.class)
class ActivePetFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnPublicMascotForAnonymousBrowser() throws Exception {
        mockMvc.perform(get("/api/v1/pets/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.petId").value("public-mascot"))
                .andExpect(jsonPath("$.data.source").value("PUBLIC"))
                .andExpect(jsonPath("$.data.ownerSubjectId").isEmpty());
    }

    @Test
    void shouldIgnoreAdministratorCookie() throws Exception {
        mockMvc.perform(get("/api/v1/pets/active")
                        .cookie(new Cookie("STREAMORA_ADMIN_SESSION", "valid-user-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("PUBLIC"));
    }

    @Test
    void shouldCreateAndReusePersonalPetForValidUserCookie() throws Exception {
        String firstBody = mockMvc.perform(get("/api/v1/pets/active")
                        .cookie(new Cookie("STREAMORA_USER_SESSION", "valid-user-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("PERSONAL"))
                .andExpect(jsonPath("$.data.ownerSubjectId").value("user-42"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String firstPetId = objectMapper.readTree(firstBody).path("data").path("petId").asText();

        mockMvc.perform(get("/api/v1/pets/active")
                        .cookie(new Cookie("STREAMORA_USER_SESSION", "valid-user-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.petId").value(firstPetId));
    }

    @TestConfiguration
    static class TestIdentityConfiguration {

        @Bean
        @Primary
        UserIdentityClient testUserIdentityClient() {
            return (rawToken, traceId) -> "valid-user-token".equals(rawToken)
                    ? Optional.of(new UserIdentityClient.UserPrincipal("user-42", "小河"))
                    : Optional.empty();
        }
    }
}
