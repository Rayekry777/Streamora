package com.streamora.transcode.infrastructure;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.streamora.transcode.application.HlsTranscodeExecutor;
import com.streamora.transcode.domain.TranscodeArtifacts;
import com.streamora.transcode.domain.TranscodeJob;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** FFmpeg adapter using Jaffree; generated playlists are VOD-safe and seek-friendly. */
@Component
public class JaffreeHlsTranscodeExecutor implements HlsTranscodeExecutor {
    private final Path ffmpegPath;

    public JaffreeHlsTranscodeExecutor(@Value("${streamora.transcode.ffmpeg-path}") Path ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    @Override
    public TranscodeArtifacts transcode(TranscodeJob job, Path source) {
        try {
            Path artifactDirectory = source.getParent().resolve("artifacts");
            Files.createDirectories(artifactDirectory);
            Path manifest = artifactDirectory.resolve("master.m3u8");
            Path poster = artifactDirectory.resolve("poster.jpg");
            createHls(source, artifactDirectory, manifest);
            createPoster(source, poster);
            return new TranscodeArtifacts(artifactDirectory, manifest, poster);
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建转码输出目录", exception);
        }
    }

    private void createHls(Path source, Path artifactDirectory, Path manifest) {
        var output = UrlOutput.toPath(manifest).setFormat("hls");
        output.addArguments("-map", "0:v:0");
        output.addArguments("-map", "0:a?");
        output.addArguments("-c:v", "libx264");
        output.addArguments("-preset", "veryfast");
        output.addArguments("-crf", "23");
        output.addArguments("-g", "180");
        output.addArguments("-keyint_min", "180");
        output.addArguments("-sc_threshold", "0");
        output.addArguments("-c:a", "aac");
        output.addArguments("-b:a", "128k");
        output.addArguments("-hls_time", "6");
        output.addArguments("-hls_playlist_type", "vod");
        output.addArguments("-hls_segment_filename", artifactDirectory.resolve("segment_%05d.ts").toString());
        FFmpeg.atPath(ffmpegPath)
                .addInput(UrlInput.fromPath(source))
                .setOverwriteOutput(true)
                .addOutput(output)
                .execute();
    }

    private void createPoster(Path source, Path poster) {
        var output = UrlOutput.toPath(poster).setFormat("image2");
        output.addArguments("-frames:v", "1");
        output.addArguments("-q:v", "2");
        FFmpeg.atPath(ffmpegPath)
                .addInput(UrlInput.fromPath(source))
                .setOverwriteOutput(true)
                .addOutput(output)
                .execute();
    }
}
