package com.streamora.video.domain;

import java.time.Instant;
import java.util.List;

public record VideoDetail(String videoId, String title, String description, String coverUrl, int durationSeconds,
                          VideoCard.Creator creator, String viewCount, Instant publishedAt, String category,
                          List<String> tags, List<VideoEpisode> episodes, List<VideoCard> recommendedVideos) {
    public record VideoEpisode(String episodeId, String title, int durationSeconds, boolean isCurrent) {
    }
}
