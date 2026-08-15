package com.streamora.playback.domain;

import java.time.Instant;
import java.util.List;

public record VideoPlayback(String videoId, String manifestUrl, String posterUrl, List<SubtitleTrack> subtitles,
                            Instant expiresAt) {
    public record SubtitleTrack(String language, String label, String url) {
    }
}
