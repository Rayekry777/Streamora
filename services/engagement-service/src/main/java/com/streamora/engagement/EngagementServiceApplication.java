package com.streamora.engagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora engagement-service runtime entry point.
 */
@SpringBootApplication
public class EngagementServiceApplication {

    /**
     * Starts the engagement-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(EngagementServiceApplication.class, args);
    }
}
