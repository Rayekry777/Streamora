package com.streamora.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamora.admin.identity.AdminIdentityClient;
import com.streamora.admin.infrastructure.AdminRbacRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Verifies isolated admin cookies, RBAC enforcement and CSRF-protected logout. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AdminAuthFlowIntegrationTest.TestIdentityConfiguration.class)
class AdminAuthFlowIntegrationTest {

    private static final String SUBJECT_ID = "test-admin-1";
    private static final String RAW_TOKEN = "admin-only-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminRbacRepository repository;

    @BeforeEach
    void assignRole() {
        repository.assignRoleIfMissing(
                SUBJECT_ID, "SUPER_ADMIN", "TEST_SETUP", Instant.now());
    }

    @Test
    void shouldLoginUseStrictCookieAndReachProtectedOverview() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"admin","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audience").value("ADMIN"))
                .andExpect(jsonPath("$.data.roles[0]").value("SUPER_ADMIN"))
                .andReturn();

        String sessionHeader = responseCookieHeader(result, "STREAMORA_ADMIN_SESSION");
        assertThat(sessionHeader).contains("HttpOnly", "SameSite=Strict", "Path=/admin-api/v1");
        Cookie sessionCookie = responseCookie(result, "STREAMORA_ADMIN_SESSION");

        mockMvc.perform(get("/admin-api/v1/operations/overview").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phase").value("2"));
    }

    @Test
    void shouldIgnoreUserCookieOnAdminRoutes() throws Exception {
        mockMvc.perform(get("/admin-api/v1/auth/session")
                        .cookie(new Cookie("STREAMORA_USER_SESSION", RAW_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ADMIN_SESSION_REQUIRED"));
    }

    @Test
    void shouldRequireMatchingCsrfTokenForLogout() throws Exception {
        MvcResult login = mockMvc.perform(post("/admin-api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"admin","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        Cookie sessionCookie = responseCookie(login, "STREAMORA_ADMIN_SESSION");
        Cookie csrfCookie = responseCookie(login, "STREAMORA_ADMIN_CSRF");
        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        String csrfToken = body.path("data").path("csrfToken").asText();

        mockMvc.perform(post("/admin-api/v1/auth/logout")
                        .cookie(sessionCookie, csrfCookie)
                        .header("X-CSRF-Token", "wrong-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CSRF_VALIDATION_FAILED"));

        mockMvc.perform(post("/admin-api/v1/auth/logout")
                        .cookie(sessionCookie, csrfCookie)
                        .header("X-CSRF-Token", csrfToken))
                .andExpect(status().isNoContent());
    }

    private static Cookie responseCookie(MvcResult result, String name) {
        String header = responseCookieHeader(result, name);
        String value = header.substring(name.length() + 1, header.indexOf(';'));
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/admin-api/v1");
        cookie.setHttpOnly(header.contains("HttpOnly"));
        return cookie;
    }

    private static String responseCookieHeader(MvcResult result, String name) {
        List<String> headers = result.getResponse().getHeaders("Set-Cookie");
        return headers.stream()
                .filter(value -> value.startsWith(name + "="))
                .findFirst()
                .orElseThrow();
    }

    @TestConfiguration
    static class TestIdentityConfiguration {

        @Bean
        @Primary
        AdminIdentityClient testAdminIdentityClient() {
            return new AdminIdentityClient() {
                @Override
                public Optional<AuthenticatedAdmin> authenticate(
                        String login, String password, String userAgent, String ipAddress, String traceId) {
                    if (!"admin".equals(login) || !"123456".equals(password)) {
                        return Optional.empty();
                    }
                    return Optional.of(new AuthenticatedAdmin(
                            RAW_TOKEN,
                            SUBJECT_ID,
                            "阶段二管理员",
                            Instant.now().plus(2, ChronoUnit.HOURS)));
                }

                @Override
                public Optional<AdminPrincipal> resolve(String rawToken, String traceId) {
                    if (!RAW_TOKEN.equals(rawToken)) {
                        return Optional.empty();
                    }
                    return Optional.of(new AdminPrincipal(
                            SUBJECT_ID,
                            "阶段二管理员",
                            Instant.now().plus(2, ChronoUnit.HOURS)));
                }

                @Override
                public boolean revoke(String rawToken, String traceId) {
                    return RAW_TOKEN.equals(rawToken);
                }
            };
        }
    }
}
