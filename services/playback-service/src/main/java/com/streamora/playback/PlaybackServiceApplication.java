package com.streamora.playback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora playback-service runtime entry point.
 */
@SpringBootApplication
public class PlaybackServiceApplication {

    /**
     * Starts the playback-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PlaybackServiceApplication.class, args);
    }
}
