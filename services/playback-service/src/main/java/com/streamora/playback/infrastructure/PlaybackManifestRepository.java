package com.streamora.playback.infrastructure;

import com.streamora.playback.domain.VideoPlayback;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Stores only playback-owned manifest projections and subtitle references. */
@Repository
public class PlaybackManifestRepository {
    private final JdbcTemplate jdbcTemplate;

    public PlaybackManifestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ManifestRecord> findPublic(String videoId) {
        return jdbcTemplate.query("""
                        SELECT video_id, manifest_url, poster_url FROM playback.playback_manifest
                        WHERE video_id = ? AND publication_state = 'PUBLIC'
                        """, (resultSet, rowNumber) -> new ManifestRecord(resultSet.getString("video_id"),
                        resultSet.getString("manifest_url"), resultSet.getString("poster_url")), videoId).stream().findFirst();
    }

    public List<VideoPlayback.SubtitleTrack> findSubtitles(String videoId) {
        return jdbcTemplate.query("SELECT language, label, url FROM playback.subtitle_track WHERE video_id = ? ORDER BY language",
                (resultSet, rowNumber) -> new VideoPlayback.SubtitleTrack(resultSet.getString("language"),
                        resultSet.getString("label"), resultSet.getString("url")), videoId);
    }

    public record ManifestRecord(String videoId, String manifestUrl, String posterUrl) {
    }
}
