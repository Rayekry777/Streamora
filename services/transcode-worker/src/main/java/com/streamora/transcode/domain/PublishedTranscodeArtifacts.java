package com.streamora.transcode.domain;

/** Object keys returned after a successful artifact publication. */
public record PublishedTranscodeArtifacts(String manifestObjectKey, String posterObjectKey) {
}
