package com.streamora.admin.domain;

import java.time.Instant;

/** Administrator identity combined with admin-service authorization. */
public record AdminSession(
        String rawToken,
        String subjectId,
        String displayName,
        Instant expiresAt,
        AdminAuthorization authorization) {
}
