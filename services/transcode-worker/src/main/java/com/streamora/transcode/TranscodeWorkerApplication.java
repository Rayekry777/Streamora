package com.streamora.transcode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora transcode-worker runtime entry point.
 */
@SpringBootApplication
public class TranscodeWorkerApplication {

    /**
     * Starts the transcode-worker process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(TranscodeWorkerApplication.class, args);
    }
}
