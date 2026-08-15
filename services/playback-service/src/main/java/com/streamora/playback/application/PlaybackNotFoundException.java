package com.streamora.playback.application;

public class PlaybackNotFoundException extends RuntimeException {
    public PlaybackNotFoundException(String videoId) {
        super("视频不存在或暂不可播放: " + videoId);
    }
}
