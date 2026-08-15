package com.streamora.playback.api;

import java.util.UUID;

final class RequestIds {
    private RequestIds() {
    }

    static String resolve(String candidate) {
        return candidate == null || candidate.isBlank() ? UUID.randomUUID().toString() : candidate;
    }
}
