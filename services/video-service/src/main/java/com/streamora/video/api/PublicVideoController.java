package com.streamora.video.api;

import com.streamora.video.application.PublicVideoQueryService;
import com.streamora.video.domain.HomeFeed;
import com.streamora.video.domain.VideoDetail;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PublicVideoController {
    private final PublicVideoQueryService queryService;

    public PublicVideoController(PublicVideoQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/home/feed")
    public ApiEnvelope<HomeFeed> getHomeFeed(
            @RequestParam(required = false) @Size(max = 32) String category,
            @RequestParam(required = false) String cursor,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return new ApiEnvelope<>(queryService.homeFeed(category, cursor), RequestIds.resolve(requestId));
    }

    @GetMapping("/videos/{videoId}")
    public ApiEnvelope<VideoDetail> getVideoDetail(
            @PathVariable @Size(min = 1, max = 64) String videoId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return new ApiEnvelope<>(queryService.videoDetail(videoId), RequestIds.resolve(requestId));
    }
}
