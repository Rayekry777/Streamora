package com.streamora.identity.api;

import com.streamora.identity.application.IdentityAuthService;
import com.streamora.identity.application.IdentityAuthenticationException;
import com.streamora.identity.config.IdentitySessionProperties;
import com.streamora.identity.domain.AuthenticatedSession;
import com.streamora.identity.domain.SessionAudience;
import com.streamora.identity.domain.SessionPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public user authentication API. Administrator authentication is only exposed by admin-service. */
@RestController
@RequestMapping("/api/v1/auth")
public class UserAuthController {

    static final String SESSION_COOKIE = "STREAMORA_USER_SESSION";
    static final String CSRF_COOKIE = "STREAMORA_USER_CSRF";
    private static final String COOKIE_PATH = "/api/v1";

    private final IdentityAuthService authService;
    private final IdentitySessionProperties properties;
    private final CsrfTokenService csrfTokenService;

    public UserAuthController(
            IdentityAuthService authService,
            IdentitySessionProperties properties,
            CsrfTokenService csrfTokenService) {
        this.authService = authService;
        this.properties = properties;
        this.csrfTokenService = csrfTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiEnvelope<AuthSessionView>> register(
            @Valid @RequestBody UserRegistrationRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletResponse response) {
        AuthenticatedSession session = authService.registerUser(
                request.login(), request.displayName(), request.password());
        String csrfToken = establishBrowserSession(session, response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiEnvelope<>(AuthSessionView.from(session, csrfToken), RequestIds.resolve(requestId)));
    }

    @PostMapping("/login")
    public ApiEnvelope<AuthSessionView> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletResponse response) {
        AuthenticatedSession session = authService.loginUser(request.login(), request.password());
        String csrfToken = establishBrowserSession(session, response);
        return new ApiEnvelope<>(AuthSessionView.from(session, csrfToken), RequestIds.resolve(requestId));
    }

    @GetMapping("/session")
    public ApiEnvelope<AuthSessionView> session(
            @CookieValue(value = SESSION_COOKIE, required = false) String rawToken,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletResponse response) {
        SessionPrincipal principal = authService.resolve(rawToken, SessionAudience.USER)
                .orElseThrow(() -> new IdentityAuthenticationException("USER_SESSION_REQUIRED", "用户会话不存在或已过期"));
        String csrfToken = issueCsrfCookie(response);
        return new ApiEnvelope<>(AuthSessionView.from(principal, csrfToken), RequestIds.resolve(requestId));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = SESSION_COOKIE, required = false) String rawToken,
            @CookieValue(value = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(value = "X-CSRF-Token", required = false) String csrfHeader,
            HttpServletResponse response) {
        if (!csrfTokenService.isValid(csrfCookie, csrfHeader)) {
            throw new IdentityForbiddenException("CSRF_VALIDATION_FAILED", "CSRF 校验失败");
        }
        if (!authService.revoke(rawToken, SessionAudience.USER)) {
            throw new IdentityAuthenticationException("USER_SESSION_REQUIRED", "用户会话不存在或已过期");
        }
        expireCookie(response, SESSION_COOKIE, true);
        expireCookie(response, CSRF_COOKIE, false);
        return ResponseEntity.noContent().build();
    }

    private String establishBrowserSession(AuthenticatedSession session, HttpServletResponse response) {
        ResponseCookie sessionCookie = ResponseCookie.from(SESSION_COOKIE, session.rawToken())
                .httpOnly(true)
                .secure(properties.isSecureCookie())
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.between(Instant.now(), session.expiresAt()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());
        return issueCsrfCookie(response);
    }

    private String issueCsrfCookie(HttpServletResponse response) {
        String csrfToken = csrfTokenService.issue();
        ResponseCookie csrfCookie = ResponseCookie.from(CSRF_COOKIE, csrfToken)
                .httpOnly(false)
                .secure(properties.isSecureCookie())
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());
        return csrfToken;
    }

    private void expireCookie(HttpServletResponse response, String name, boolean httpOnly) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, "")
                .httpOnly(httpOnly)
                .secure(properties.isSecureCookie())
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build()
                .toString());
    }

    public record LoginRequest(
            @NotBlank @Size(min = 3, max = 64) String login,
            @NotBlank @Size(min = 12, max = 72) String password) {
    }

    public record UserRegistrationRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,64}$") String login,
            @NotBlank @Size(max = 40) String displayName,
            @NotBlank @Size(min = 12, max = 72) String password) {
    }

    public record AuthSessionView(
            String subjectId,
            String displayName,
            String audience,
            Instant expiresAt,
            String csrfToken) {

        static AuthSessionView from(AuthenticatedSession session, String csrfToken) {
            return new AuthSessionView(
                    Long.toString(session.subjectId()),
                    session.displayName(),
                    session.audience().name(),
                    session.expiresAt(),
                    csrfToken);
        }

        static AuthSessionView from(SessionPrincipal principal, String csrfToken) {
            return new AuthSessionView(
                    Long.toString(principal.subjectId()),
                    principal.displayName(),
                    principal.audience().name(),
                    principal.expiresAt(),
                    csrfToken);
        }
    }
}
