package com.streamora.video.infrastructure;

import com.streamora.video.domain.VideoCard;
import com.streamora.video.domain.VideoDetail;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC repository for metadata solely owned by video-service. */
@Repository
public class VideoCatalogRepository {
    private static final String PUBLIC = "visibility = 'PUBLIC' AND lifecycle_status = 'PUBLISHED'";
    private final JdbcTemplate jdbcTemplate;

    public VideoCatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<StoredVideo> findPublic(String categoryId, Instant cursor, int limit) {
        String sql = """
                SELECT id, owner_subject_id, title, description, cover_url, duration_seconds, creator_display_name,
                       creator_avatar_url, category_id, category_label, view_count, published_at
                FROM video.video WHERE %s%s
                ORDER BY published_at DESC, id DESC LIMIT ?
                """.formatted(PUBLIC, categoryId == null ? "" : " AND category_id = ?",
                cursor == null ? "" : " AND published_at < ?");
        Object[] parameters;
        if (categoryId != null && cursor != null) {
            parameters = new Object[]{categoryId, Timestamp.from(cursor), limit};
        } else if (categoryId != null) {
            parameters = new Object[]{categoryId, limit};
        } else if (cursor != null) {
            parameters = new Object[]{Timestamp.from(cursor), limit};
        } else {
            parameters = new Object[]{limit};
        }
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> map(resultSet), parameters);
    }

    public Optional<StoredVideo> findPublicById(String videoId) {
        return jdbcTemplate.query("""
                        SELECT id, owner_subject_id, title, description, cover_url, duration_seconds, creator_display_name,
                               creator_avatar_url, category_id, category_label, view_count, published_at
                        FROM video.video WHERE id = ? AND visibility = 'PUBLIC' AND lifecycle_status = 'PUBLISHED'
                        """, (resultSet, rowNumber) -> map(resultSet), videoId).stream().findFirst();
    }

    public List<String> findTags(String videoId) {
        return jdbcTemplate.queryForList("SELECT tag FROM video.video_tag WHERE video_id = ? ORDER BY tag", String.class, videoId);
    }

    private StoredVideo map(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new StoredVideo(resultSet.getString("id"), resultSet.getString("owner_subject_id"), resultSet.getString("title"),
                resultSet.getString("description"), resultSet.getString("cover_url"), resultSet.getInt("duration_seconds"),
                resultSet.getString("creator_display_name"), resultSet.getString("creator_avatar_url"),
                resultSet.getString("category_id"), resultSet.getString("category_label"), resultSet.getLong("view_count"),
                resultSet.getTimestamp("published_at").toInstant());
    }

    public record StoredVideo(String id, String creatorId, String title, String description, String coverUrl,
                              int durationSeconds, String creatorDisplayName, String creatorAvatarUrl,
                              String categoryId, String categoryLabel, long viewCount, Instant publishedAt) {
        public VideoCard toCard(String reason) {
            return new VideoCard(id, title, coverUrl, durationSeconds,
                    new VideoCard.Creator(creatorId, creatorDisplayName, creatorAvatarUrl), String.valueOf(viewCount),
                    publishedAt, categoryLabel, reason);
        }

        public VideoDetail toDetail(List<String> tags, List<VideoCard> recommendations) {
            return new VideoDetail(id, title, description, coverUrl, durationSeconds,
                    new VideoCard.Creator(creatorId, creatorDisplayName, creatorAvatarUrl), String.valueOf(viewCount),
                    publishedAt, categoryLabel, tags,
                    List.of(new VideoDetail.VideoEpisode(id, "第 1 集 · 正片", durationSeconds, true)), recommendations);
        }
    }
}
