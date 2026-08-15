package com.streamora.video;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora video-service runtime entry point.
 */
@SpringBootApplication
public class VideoServiceApplication {

    /**
     * Starts the video-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(VideoServiceApplication.class, args);
    }
}
