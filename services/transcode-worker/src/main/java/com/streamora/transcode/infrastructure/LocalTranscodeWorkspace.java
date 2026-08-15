package com.streamora.transcode.infrastructure;

import com.streamora.transcode.application.TranscodeWorkspace;
import com.streamora.transcode.domain.PublishedTranscodeArtifacts;
import com.streamora.transcode.domain.TranscodeArtifacts;
import com.streamora.transcode.domain.TranscodeJob;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Local-development workspace; production S3/SeaweedFS publication is a separate adapter. */
@Component
public class LocalTranscodeWorkspace implements TranscodeWorkspace {
    private final Path inputRoot;
    private final Path outputRoot;
    private final Path workRoot;

    public LocalTranscodeWorkspace(
            @Value("${streamora.transcode.local-input-root}") Path inputRoot,
            @Value("${streamora.transcode.local-output-root}") Path outputRoot,
            @Value("${streamora.transcode.work-root}") Path workRoot) {
        this.inputRoot = inputRoot.toAbsolutePath().normalize();
        this.outputRoot = outputRoot.toAbsolutePath().normalize();
        this.workRoot = workRoot.toAbsolutePath().normalize();
    }

    @Override
    public Path materializeSource(TranscodeJob job) {
        Path source = resolveUnder(inputRoot, job.sourceObjectKey());
        if (!Files.isRegularFile(source)) {
            throw new IllegalStateException("媒体源文件不可用");
        }
        try {
            Path jobDirectory = workRoot.resolve(job.jobId()).normalize();
            Files.createDirectories(jobDirectory);
            Path localSource = jobDirectory.resolve("source");
            Files.copy(source, localSource, StandardCopyOption.REPLACE_EXISTING);
            return localSource;
        } catch (IOException exception) {
            throw new IllegalStateException("无法准备媒体转码工作目录", exception);
        }
    }

    @Override
    public PublishedTranscodeArtifacts publish(TranscodeJob job, TranscodeArtifacts artifacts) {
        Path targetRoot = resolveUnder(outputRoot, job.outputPrefix());
        try {
            Files.createDirectories(targetRoot);
            try (var paths = Files.walk(artifacts.artifactDirectory())) {
                paths.filter(Files::isRegularFile).forEach(path -> copyArtifact(artifacts.artifactDirectory(), targetRoot, path));
            }
            return new PublishedTranscodeArtifacts(job.outputPrefix() + artifacts.manifest().getFileName(),
                    job.outputPrefix() + artifacts.poster().getFileName());
        } catch (IOException exception) {
            throw new IllegalStateException("无法发布转码产物", exception);
        }
    }

    private void copyArtifact(Path artifactDirectory, Path targetRoot, Path source) {
        try {
            Path target = targetRoot.resolve(artifactDirectory.relativize(source)).normalize();
            if (!target.startsWith(targetRoot)) {
                throw new IllegalStateException("转码产物路径不合法");
            }
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("无法复制转码产物", exception);
        }
    }

    private static Path resolveUnder(Path root, String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("对象路径不合法");
        }
        return resolved;
    }
}
