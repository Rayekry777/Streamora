package com.streamora.admin.api;

import com.streamora.admin.application.AdminAccessService;
import com.streamora.admin.application.AdminForbiddenException;
import com.streamora.admin.domain.AdminSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administrator-only login and session API with an isolated Strict cookie scope. */
@RestController
@RequestMapping("/admin-api/v1/auth")
public class AdminAuthController {

    static final String SESSION_COOKIE = "STREAMORA_ADMIN_SESSION";
    static final String CSRF_COOKIE = "STREAMORA_ADMIN_CSRF";
    private static final String COOKIE_PATH = "/admin-api/v1";

    private final AdminAccessService accessService;
    private final AdminCsrfTokenService csrfTokenService;
    private final boolean secureCookie;

    public AdminAuthController(
            AdminAccessService accessService,
            AdminCsrfTokenService csrfTokenService,
            @Value("${streamora.session.secure-cookie:false}") boolean secureCookie) {
        this.accessService = accessService;
        this.csrfTokenService = csrfTokenService;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    public ApiEnvelope<AdminSessionView> login(
            @Valid @RequestBody LoginRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request,
            HttpServletResponse response) {
        String traceId = RequestIds.resolve(requestId);
        AdminSession session = accessService.login(
                body.login(),
                body.password(),
                valueOrEmpty(request.getHeader("User-Agent")),
                request.getRemoteAddr(),
                traceId);
        String csrfToken = establishBrowserSession(session, response);
        return new ApiEnvelope<>(AdminSessionView.from(session, csrfToken), traceId);
    }

    @GetMapping("/session")
    public ApiEnvelope<AdminSessionView> session(
            @CookieValue(value = SESSION_COOKIE, required = false) String rawToken,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletResponse response) {
        String traceId = RequestIds.resolve(requestId);
        AdminSession session = accessService.resolve(rawToken, traceId);
        String csrfToken = issueCsrfCookie(response);
        return new ApiEnvelope<>(AdminSessionView.from(session, csrfToken), traceId);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = SESSION_COOKIE, required = false) String rawToken,
            @CookieValue(value = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(value = "X-CSRF-Token", required = false) String csrfHeader,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletResponse response) {
        if (!csrfTokenService.isValid(csrfCookie, csrfHeader)) {
            throw new AdminForbiddenException("CSRF_VALIDATION_FAILED", "CSRF 校验失败");
        }
        accessService.logout(rawToken, RequestIds.resolve(requestId));
        expireCookie(response, SESSION_COOKIE, true);
        expireCookie(response, CSRF_COOKIE, false);
        return ResponseEntity.noContent().build();
    }

    private String establishBrowserSession(AdminSession session, HttpServletResponse response) {
        Duration remaining = Duration.between(Instant.now(), session.expiresAt());
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(SESSION_COOKIE, session.rawToken())
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(remaining.isNegative() ? Duration.ZERO : remaining)
                .build()
                .toString());
        return issueCsrfCookie(response);
    }

    private String issueCsrfCookie(HttpServletResponse response) {
        String csrfToken = csrfTokenService.issue();
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(CSRF_COOKIE, csrfToken)
                .httpOnly(false)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .build()
                .toString());
        return csrfToken;
    }

    private void expireCookie(HttpServletResponse response, String name, boolean httpOnly) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, "")
                .httpOnly(httpOnly)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build()
                .toString());
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    public record LoginRequest(
            @NotBlank @Size(min = 3, max = 64) String login,
            @NotBlank @Size(min = 6, max = 72) String password) {
    }

    public record AdminSessionView(
            String subjectId,
            String displayName,
            String audience,
            Instant expiresAt,
            String csrfToken,
            Set<String> roles,
            Set<String> permissions) {

        static AdminSessionView from(AdminSession session, String csrfToken) {
            return new AdminSessionView(
                    session.subjectId(),
                    session.displayName(),
                    "ADMIN",
                    session.expiresAt(),
                    csrfToken,
                    session.authorization().roles(),
                    session.authorization().permissions());
        }
    }
}
