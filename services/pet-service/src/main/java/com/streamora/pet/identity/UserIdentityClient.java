package com.streamora.pet.identity;

import java.util.Optional;

/** Port for resolving a user-only session without accepting administrator credentials. */
public interface UserIdentityClient {

    Optional<UserPrincipal> resolveUser(String rawToken, String traceId);

    record UserPrincipal(String subjectId, String displayName) {
    }
}
