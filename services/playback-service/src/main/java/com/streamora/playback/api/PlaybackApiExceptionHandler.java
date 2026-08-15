package com.streamora.playback.api;

import com.streamora.playback.application.PlaybackNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class PlaybackApiExceptionHandler {
    @ExceptionHandler(PlaybackNotFoundException.class)
    ResponseEntity<ErrorEnvelope> notFound(PlaybackNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorEnvelope(
                new ErrorBody("PLAYBACK_NOT_FOUND", exception.getMessage(), List.of()),
                RequestIds.resolve(request.getHeader("X-Request-Id"))));
    }

    record ErrorEnvelope(ErrorBody error, String requestId) {
    }

    record ErrorBody(String code, String message, List<?> details) {
    }
}
