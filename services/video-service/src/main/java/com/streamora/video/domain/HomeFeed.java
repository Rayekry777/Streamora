package com.streamora.video.domain;

import java.util.List;

public record HomeFeed(VideoCard featuredVideo, List<VideoCategory> categories, List<VideoCard> items,
                       String nextCursor, boolean hasMore) {
}
