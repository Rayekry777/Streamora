package com.streamora.media.api;

import com.streamora.media.application.MediaUploadException;
import com.streamora.media.application.MediaUploadService;
import com.streamora.media.domain.CompletedPart;
import com.streamora.media.domain.MediaUploadCompletion;
import com.streamora.media.domain.MediaUploadSession;
import com.streamora.media.identity.MediaUserIdentityClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media/uploads")
public class MediaUploadController {
    private final MediaUploadService uploadService;
    private final MediaUserIdentityClient identityClient;

    public MediaUploadController(MediaUploadService uploadService, MediaUserIdentityClient identityClient) {
        this.uploadService = uploadService;
        this.identityClient = identityClient;
    }

    @PostMapping
    public ResponseEntity<ApiEnvelope<MediaUploadSession>> create(
            @CookieValue(value = "STREAMORA_USER_SESSION", required = false) String rawToken,
            @RequestHeader("Idempotency-Key") @Size(min = 16, max = 128) String idempotencyKey,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody CreateMediaUploadRequest request) {
        String traceId = RequestIds.resolve(requestId);
        String owner = identityClient.resolveUserId(rawToken, traceId)
                .orElseThrow(() -> new MediaUploadException("USER_SESSION_REQUIRED", "需要有效的用户会话"));
        MediaUploadSession session = uploadService.create(owner, request.fileName(), request.contentType(), request.totalBytes(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiEnvelope<>(session, traceId));
    }

    @PostMapping("/{uploadId}/complete")
    public ApiEnvelope<MediaUploadCompletion> complete(
            @CookieValue(value = "STREAMORA_USER_SESSION", required = false) String rawToken,
            @org.springframework.web.bind.annotation.PathVariable String uploadId,
            @RequestHeader("Idempotency-Key") @Size(min = 16, max = 128) String idempotencyKey,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody CompleteMediaUploadRequest request) {
        String traceId = RequestIds.resolve(requestId);
        String owner = identityClient.resolveUserId(rawToken, traceId)
                .orElseThrow(() -> new MediaUploadException("USER_SESSION_REQUIRED", "需要有效的用户会话"));
        return new ApiEnvelope<>(uploadService.complete(owner, uploadId, idempotencyKey,
                request.parts().stream().map(part -> new CompletedPart(part.partNumber(), part.etag())).toList()), traceId);
    }

    public record CreateMediaUploadRequest(
            @NotBlank @Size(max = 255) String fileName,
            @NotBlank @Pattern(regexp = "^video/.+$") @Size(max = 128) String contentType,
            @Min(1) long totalBytes) {
    }

    public record CompleteMediaUploadRequest(@Valid @Size(min = 1) List<CompletedPartRequest> parts) {
    }

    public record CompletedPartRequest(@Min(1) int partNumber, @NotBlank @Size(max = 512) String etag) {
    }
}
