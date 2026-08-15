package com.streamora.playback.api;

import com.streamora.playback.application.PublicPlaybackQueryService;
import com.streamora.playback.domain.VideoPlayback;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/videos/{videoId}/playback")
public class PublicPlaybackController {
    private final PublicPlaybackQueryService queryService;

    public PublicPlaybackController(PublicPlaybackQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ApiEnvelope<VideoPlayback> getPlayback(
            @PathVariable @Size(min = 1, max = 64) String videoId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return new ApiEnvelope<>(queryService.getPlayback(videoId), RequestIds.resolve(requestId));
    }
}
