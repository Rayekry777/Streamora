package com.streamora.video.application;

import com.streamora.video.domain.HomeFeed;
import com.streamora.video.domain.VideoCard;
import com.streamora.video.domain.VideoCategory;
import com.streamora.video.domain.VideoDetail;
import com.streamora.video.infrastructure.VideoCatalogRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;

/** Builds public video read views without exposing the video persistence model. */
@Service
public class PublicVideoQueryService {
    private static final int PAGE_SIZE = 12;
    private static final List<VideoCategory> CATEGORIES = List.of(
            new VideoCategory("all", "推荐", "Sparkles"),
            new VideoCategory("life", "生活", "Coffee"),
            new VideoCategory("pets", "萌宠", "Heart"),
            new VideoCategory("games", "游戏", "Gamepad2"),
            new VideoCategory("knowledge", "知识", "Lightbulb"),
            new VideoCategory("technology", "科技", "Cpu"));
    private final VideoCatalogRepository repository;

    public PublicVideoQueryService(VideoCatalogRepository repository) {
        this.repository = repository;
    }

    public HomeFeed homeFeed(String category, String cursor) {
        String categoryId = category == null || category.isBlank() || "all".equals(category) ? null : category;
        Instant cursorTime = decodeCursor(cursor);
        List<VideoCatalogRepository.StoredVideo> rows = repository.findPublic(categoryId, cursorTime, PAGE_SIZE + 1);
        boolean hasMore = rows.size() > PAGE_SIZE;
        List<VideoCatalogRepository.StoredVideo> page = hasMore ? rows.subList(0, PAGE_SIZE) : rows;
        VideoCard featured = repository.findPublic(null, null, 1).stream().findFirst()
                .map(video -> video.toCard("今日焦点"))
                .orElseThrow(() -> new VideoNotFoundException("featured"));
        String nextCursor = hasMore ? encodeCursor(page.getLast().publishedAt()) : null;
        return new HomeFeed(featured, CATEGORIES, page.stream().map(video -> video.toCard("新鲜发布")).toList(), nextCursor, hasMore);
    }

    public VideoDetail videoDetail(String videoId) {
        VideoCatalogRepository.StoredVideo current = repository.findPublicById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));
        List<VideoCard> recommendations = repository.findPublic(null, null, 6).stream()
                .filter(video -> !video.id().equals(videoId))
                .map(video -> video.toCard("热门内容"))
                .toList();
        return current.toDetail(repository.findTags(videoId), recommendations);
    }

    private String encodeCursor(Instant publishedAt) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(publishedAt.toString().getBytes(StandardCharsets.UTF_8));
    }

    private Instant decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            return Instant.parse(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("cursor 无效");
        }
    }
}
