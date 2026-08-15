package com.streamora.media.identity;

import java.util.Optional;

/** Resolves only user sessions before media writes. */
public interface MediaUserIdentityClient {
    Optional<String> resolveUserId(String rawToken, String traceId);
}
