package com.streamora.identity.api;

import java.util.UUID;

/** Request correlation helper used until the gateway correlation filter is introduced. */
final class RequestIds {

    private RequestIds() {
    }

    static String resolve(String candidate) {
        return candidate == null || candidate.isBlank() ? UUID.randomUUID().toString() : candidate;
    }
}
