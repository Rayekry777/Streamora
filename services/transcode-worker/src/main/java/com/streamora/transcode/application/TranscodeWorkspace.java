package com.streamora.transcode.application;

import com.streamora.transcode.domain.PublishedTranscodeArtifacts;
import com.streamora.transcode.domain.TranscodeArtifacts;
import com.streamora.transcode.domain.TranscodeJob;
import java.nio.file.Path;

/** Isolates local staging and object-store publication from FFmpeg execution. */
public interface TranscodeWorkspace {
    Path materializeSource(TranscodeJob job);

    PublishedTranscodeArtifacts publish(TranscodeJob job, TranscodeArtifacts artifacts);
}
