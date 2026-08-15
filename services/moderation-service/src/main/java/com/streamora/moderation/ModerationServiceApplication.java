package com.streamora.moderation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora moderation-service runtime entry point.
 */
@SpringBootApplication
public class ModerationServiceApplication {

    /**
     * Starts the moderation-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ModerationServiceApplication.class, args);
    }
}
