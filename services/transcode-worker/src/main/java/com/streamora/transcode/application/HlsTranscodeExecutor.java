package com.streamora.transcode.application;

import com.streamora.transcode.domain.TranscodeArtifacts;
import com.streamora.transcode.domain.TranscodeJob;
import java.nio.file.Path;

/** Port that turns one materialized source into HLS artifacts. */
public interface HlsTranscodeExecutor {
    TranscodeArtifacts transcode(TranscodeJob job, Path source);
}
