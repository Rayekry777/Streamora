package com.streamora.admin.identity;

import java.time.Instant;
import java.util.Optional;

/** Port used by admin-service to authenticate and resolve administrator-only sessions. */
public interface AdminIdentityClient {

    Optional<AuthenticatedAdmin> authenticate(
            String login, String password, String userAgent, String ipAddress, String traceId);

    Optional<AdminPrincipal> resolve(String rawToken, String traceId);

    boolean revoke(String rawToken, String traceId);

    record AuthenticatedAdmin(
            String rawToken,
            String subjectId,
            String displayName,
            Instant expiresAt) {
    }

    record AdminPrincipal(
            String subjectId,
            String displayName,
            Instant expiresAt) {
    }
}
