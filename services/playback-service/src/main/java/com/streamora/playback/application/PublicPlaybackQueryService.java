package com.streamora.playback.application;

import com.streamora.playback.domain.VideoPlayback;
import com.streamora.playback.infrastructure.PlaybackManifestRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Returns a development playback response from playback's public-manifest projection. */
@Service
public class PublicPlaybackQueryService {
    private final PlaybackManifestRepository repository;
    private final Clock clock;

    @Autowired
    public PublicPlaybackQueryService(PlaybackManifestRepository repository) {
        this(repository, Clock.systemUTC());
    }

    PublicPlaybackQueryService(PlaybackManifestRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public VideoPlayback getPlayback(String videoId) {
        var manifest = repository.findPublic(videoId).orElseThrow(() -> new PlaybackNotFoundException(videoId));
        return new VideoPlayback(manifest.videoId(), manifest.manifestUrl(), manifest.posterUrl(),
                repository.findSubtitles(videoId), clock.instant().plus(Duration.ofMinutes(15)));
    }
}
