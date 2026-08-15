package com.streamora.video.domain;

import java.time.Instant;

public record VideoCard(String videoId, String title, String coverUrl, int durationSeconds, Creator creator,
                        String viewCount, Instant publishedAt, String category, String recommendationReason) {
    public record Creator(String creatorId, String displayName, String avatarUrl) {
    }
}
