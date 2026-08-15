package com.streamora.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamora.identity.application.IdentityAuthService;
import com.streamora.identity.domain.SessionAudience;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Covers the browser user flow and the hard boundary between user and administrator sessions. */
@SpringBootTest
@AutoConfigureMockMvc
class IdentityAuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IdentityAuthService authService;

    @Test
    void shouldRegisterResolveAndLogoutUserWithCsrfProtection() throws Exception {
        String login = "user-" + UUID.randomUUID();
        MvcResult registered = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","displayName":"阶段二用户","password":"Correct-Horse-42"}
                                """.formatted(login)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.audience").value("USER"))
                .andReturn();

        Cookie sessionCookie = responseCookie(registered, "STREAMORA_USER_SESSION");
        Cookie csrfCookie = responseCookie(registered, "STREAMORA_USER_CSRF");
        JsonNode body = objectMapper.readTree(registered.getResponse().getContentAsString());
        String csrfToken = body.path("data").path("csrfToken").asText();

        assertThat(sessionCookie.isHttpOnly()).isTrue();
        assertThat(csrfCookie.isHttpOnly()).isFalse();
        assertThat(csrfCookie.getValue()).isEqualTo(csrfToken);

        mockMvc.perform(get("/api/v1/auth/session").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("阶段二用户"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(sessionCookie, csrfCookie)
                        .header("X-CSRF-Token", csrfToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/session").cookie(sessionCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("USER_SESSION_REQUIRED"));
    }

    @Test
    void shouldNeverResolveATokenForTheOtherAudience() {
        String suffix = UUID.randomUUID().toString();
        var user = authService.registerUser("u-" + suffix, "隔离用户", "Correct-Horse-42");
        long adminId = authService.createBootstrapAdmin(
                        "a-" + suffix, "隔离管理员", "Correct-Horse-42")
                .orElseThrow();
        var admin = authService.loginAdmin("a-" + suffix, "Correct-Horse-42");

        assertThat(admin.subjectId()).isEqualTo(adminId);
        assertThat(authService.resolve(user.rawToken(), SessionAudience.USER)).isPresent();
        assertThat(authService.resolve(user.rawToken(), SessionAudience.ADMIN)).isEmpty();
        assertThat(authService.resolve(admin.rawToken(), SessionAudience.ADMIN)).isPresent();
        assertThat(authService.resolve(admin.rawToken(), SessionAudience.USER)).isEmpty();
    }

    @Test
    void shouldRejectLogoutWhenCsrfHeaderDoesNotMatchCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("STREAMORA_USER_CSRF", "not-a-valid-token"))
                        .header("X-CSRF-Token", "different-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CSRF_VALIDATION_FAILED"));
    }

    private static Cookie responseCookie(MvcResult result, String name) {
        List<String> headers = result.getResponse().getHeaders("Set-Cookie");
        String header = headers.stream()
                .filter(value -> value.startsWith(name + "="))
                .findFirst()
                .orElseThrow();
        String value = header.substring(name.length() + 1, header.indexOf(';'));
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/api/v1");
        cookie.setHttpOnly(header.contains("HttpOnly"));
        return cookie;
    }
}
