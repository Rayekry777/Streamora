package com.streamora.transcode.domain;

import java.nio.file.Path;

/** Locally generated HLS artifacts before they are published to the object-store adapter. */
public record TranscodeArtifacts(Path artifactDirectory, Path manifest, Path poster) {
}
