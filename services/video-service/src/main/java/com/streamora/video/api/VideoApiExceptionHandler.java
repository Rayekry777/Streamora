package com.streamora.video.api;

import com.streamora.video.application.VideoNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class VideoApiExceptionHandler {
    @ExceptionHandler(VideoNotFoundException.class)
    ResponseEntity<ErrorEnvelope> notFound(VideoNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("VIDEO_NOT_FOUND", exception.getMessage(), request));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorEnvelope> validation(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(error("VALIDATION_FAILED", "请求参数不符合要求", request));
    }

    private ErrorEnvelope error(String code, String message, HttpServletRequest request) {
        return new ErrorEnvelope(new ErrorBody(code, message, List.of()), RequestIds.resolve(request.getHeader("X-Request-Id")));
    }

    record ErrorEnvelope(ErrorBody error, String requestId) {
    }

    record ErrorBody(String code, String message, List<?> details) {
    }
}
