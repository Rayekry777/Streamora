package com.streamora.video.application;

/** Stable client-safe error for an unavailable public video. */
public class VideoNotFoundException extends RuntimeException {
    public VideoNotFoundException(String videoId) {
        super("视频不存在或暂不可访问: " + videoId);
    }
}
