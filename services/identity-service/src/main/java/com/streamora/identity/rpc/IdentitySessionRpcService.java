package com.streamora.identity.rpc;

import com.streamora.contract.identity.v1.AdminLoginCommand;
import com.streamora.contract.identity.v1.IdentitySessionService;
import com.streamora.contract.identity.v1.RevokeSessionCommand;
import com.streamora.contract.identity.v1.SessionAuthenticationResult;
import com.streamora.contract.identity.v1.SessionMutationResult;
import com.streamora.contract.identity.v1.SessionPrincipalResult;
import com.streamora.contract.identity.v1.SessionTokenQuery;
import com.streamora.identity.application.IdentityAuthService;
import com.streamora.identity.application.IdentityAuthenticationException;
import com.streamora.identity.domain.AuthenticatedSession;
import com.streamora.identity.domain.SessionAudience;
import com.streamora.identity.domain.SessionPrincipal;
import java.util.concurrent.CompletableFuture;
import org.apache.dubbo.config.annotation.DubboService;

/** Protobuf-defined Dubbo Triple provider for cross-service session operations. */
@DubboService(interfaceClass = IdentitySessionService.class, version = "1.0.0")
public class IdentitySessionRpcService implements IdentitySessionService {

    private final IdentityAuthService authService;

    public IdentitySessionRpcService(IdentityAuthService authService) {
        this.authService = authService;
    }

    @Override
    public SessionAuthenticationResult authenticateAdmin(AdminLoginCommand request) {
        try {
            return authenticationResult(authService.loginAdmin(request.getLogin(), request.getPassword()));
        } catch (IdentityAuthenticationException exception) {
            return SessionAuthenticationResult.newBuilder()
                    .setAuthenticated(false)
                    .setErrorCode(exception.code())
                    .build();
        }
    }

    @Override
    public CompletableFuture<SessionAuthenticationResult> authenticateAdminAsync(AdminLoginCommand request) {
        return CompletableFuture.completedFuture(authenticateAdmin(request));
    }

    @Override
    public SessionPrincipalResult resolveAdminSession(SessionTokenQuery request) {
        return resolve(request, SessionAudience.ADMIN);
    }

    @Override
    public CompletableFuture<SessionPrincipalResult> resolveAdminSessionAsync(SessionTokenQuery request) {
        return CompletableFuture.completedFuture(resolveAdminSession(request));
    }

    @Override
    public SessionPrincipalResult resolveUserSession(SessionTokenQuery request) {
        return resolve(request, SessionAudience.USER);
    }

    @Override
    public CompletableFuture<SessionPrincipalResult> resolveUserSessionAsync(SessionTokenQuery request) {
        return CompletableFuture.completedFuture(resolveUserSession(request));
    }

    @Override
    public SessionMutationResult revokeAdminSession(RevokeSessionCommand request) {
        if (!SessionAudience.ADMIN.name().equals(request.getExpectedAudience())) {
            return SessionMutationResult.newBuilder()
                    .setCompleted(false)
                    .setErrorCode("SESSION_AUDIENCE_MISMATCH")
                    .build();
        }
        boolean revoked = authService.revoke(request.getRawToken(), SessionAudience.ADMIN);
        return SessionMutationResult.newBuilder()
                .setCompleted(revoked)
                .setErrorCode(revoked ? "" : "ADMIN_SESSION_REQUIRED")
                .build();
    }

    @Override
    public CompletableFuture<SessionMutationResult> revokeAdminSessionAsync(RevokeSessionCommand request) {
        return CompletableFuture.completedFuture(revokeAdminSession(request));
    }

    private SessionPrincipalResult resolve(SessionTokenQuery request, SessionAudience audience) {
        if (!audience.name().equals(request.getExpectedAudience())) {
            return SessionPrincipalResult.newBuilder()
                    .setAuthenticated(false)
                    .setErrorCode("SESSION_AUDIENCE_MISMATCH")
                    .build();
        }
        return authService.resolve(request.getRawToken(), audience)
                .map(IdentitySessionRpcService::principalResult)
                .orElseGet(() -> SessionPrincipalResult.newBuilder()
                        .setAuthenticated(false)
                        .setErrorCode(audience.name() + "_SESSION_REQUIRED")
                        .build());
    }

    private static SessionAuthenticationResult authenticationResult(AuthenticatedSession session) {
        return SessionAuthenticationResult.newBuilder()
                .setAuthenticated(true)
                .setRawToken(session.rawToken())
                .setSubjectId(Long.toString(session.subjectId()))
                .setDisplayName(session.displayName())
                .setExpiresAt(session.expiresAt().toString())
                .build();
    }

    private static SessionPrincipalResult principalResult(SessionPrincipal principal) {
        return SessionPrincipalResult.newBuilder()
                .setAuthenticated(true)
                .setSubjectId(Long.toString(principal.subjectId()))
                .setDisplayName(principal.displayName())
                .setAudience(principal.audience().name())
                .setExpiresAt(principal.expiresAt().toString())
                .build();
    }
}
