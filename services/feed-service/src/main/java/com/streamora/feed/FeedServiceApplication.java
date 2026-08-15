package com.streamora.feed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora feed-service runtime entry point.
 */
@SpringBootApplication
public class FeedServiceApplication {

    /**
     * Starts the feed-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(FeedServiceApplication.class, args);
    }
}
