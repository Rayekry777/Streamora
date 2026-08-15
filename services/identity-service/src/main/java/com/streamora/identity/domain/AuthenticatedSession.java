package com.streamora.identity.domain;

import java.time.Instant;

/** Newly issued session including the raw token returned exactly once. */
public record AuthenticatedSession(
        String rawToken,
        long subjectId,
        String displayName,
        SessionAudience audience,
        Instant expiresAt) {
}
