package com.streamora.danmaku;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora danmaku-service runtime entry point.
 */
@SpringBootApplication
public class DanmakuServiceApplication {

    /**
     * Starts the danmaku-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(DanmakuServiceApplication.class, args);
    }
}
