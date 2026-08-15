package com.streamora.identity.domain;

import java.time.Instant;

/** Validated principal that never exposes the stored or raw session token. */
public record SessionPrincipal(
        long subjectId,
        String displayName,
        SessionAudience audience,
        Instant expiresAt) {
}
