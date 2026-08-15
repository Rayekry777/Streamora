package com.streamora.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora media-service runtime entry point.
 */
@SpringBootApplication
public class MediaServiceApplication {

    /**
     * Starts the media-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }
}
