package com.streamora.pet.api;

import java.util.UUID;

final class RequestIds {

    private RequestIds() {
    }

    static String resolve(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }
}
