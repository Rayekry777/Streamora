package com.streamora.identity.application;

import com.streamora.identity.config.IdentitySessionProperties;
import com.streamora.identity.domain.AuthenticatedSession;
import com.streamora.identity.domain.SessionAudience;
import com.streamora.identity.domain.SessionPrincipal;
import com.streamora.identity.infrastructure.IdentityJdbcRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Identity use cases for registration, login and audience-bound sessions. */
@Service
public class IdentityAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final IdentityJdbcRepository repository;
    private final IdentitySessionProperties properties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
    private final Clock clock = Clock.systemUTC();

    public IdentityAuthService(IdentityJdbcRepository repository, IdentitySessionProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public AuthenticatedSession registerUser(String login, String displayName, String password) {
        String normalizedLogin = normalizeLogin(login);
        Instant now = clock.instant();
        try {
            long accountId = repository.createAccount(
                    normalizedLogin,
                    displayName.trim(),
                    SessionAudience.USER,
                    passwordEncoder.encode(password),
                    now);
            return issueSession(accountId, displayName.trim(), SessionAudience.USER, now);
        } catch (DuplicateKeyException exception) {
            throw new IdentityConflictException("LOGIN_ALREADY_EXISTS", "登录名已存在", exception);
        }
    }

    @Transactional
    public AuthenticatedSession loginUser(String login, String password) {
        return authenticate(login, password, SessionAudience.USER);
    }

    @Transactional
    public AuthenticatedSession loginAdmin(String login, String password) {
        return authenticate(login, password, SessionAudience.ADMIN);
    }

    @Transactional(readOnly = true)
    public Optional<SessionPrincipal> resolve(String rawToken, SessionAudience audience) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return repository.findActiveSession(hashToken(rawToken), audience, clock.instant());
    }

    @Transactional
    public boolean revoke(String rawToken, SessionAudience audience) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        return repository.revokeSession(hashToken(rawToken), audience, clock.instant()) > 0;
    }

    @Transactional
    public Optional<Long> createBootstrapAdmin(String login, String displayName, String password) {
        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        String normalizedLogin = normalizeLogin(login);
        if (repository.accountExists(normalizedLogin, SessionAudience.ADMIN)) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        long id = repository.createAccount(
                normalizedLogin,
                displayName.trim(),
                SessionAudience.ADMIN,
                passwordEncoder.encode(password),
                now);
        return Optional.of(id);
    }

    private AuthenticatedSession authenticate(String login, String password, SessionAudience audience) {
        var credential = repository.findCredential(normalizeLogin(login), audience)
                .filter(account -> "ACTIVE".equals(account.status()))
                .filter(account -> passwordEncoder.matches(password, account.passwordHash()))
                .orElseThrow(() -> new IdentityAuthenticationException(
                        "INVALID_CREDENTIALS", "登录名或密码不正确"));
        return issueSession(credential.id(), credential.displayName(), audience, clock.instant());
    }

    private AuthenticatedSession issueSession(
            long accountId,
            String displayName,
            SessionAudience audience,
            Instant now) {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        Instant expiresAt = now.plus(audience == SessionAudience.USER
                ? properties.getUserDuration()
                : properties.getAdminDuration());
        repository.createSession(
                UUID.randomUUID().toString(),
                accountId,
                hashToken(rawToken),
                audience,
                now,
                expiresAt);
        return new AuthenticatedSession(rawToken, accountId, displayName, audience, expiresAt);
    }

    private static String normalizeLogin(String login) {
        return login.trim().toLowerCase(Locale.ROOT);
    }

    private static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
