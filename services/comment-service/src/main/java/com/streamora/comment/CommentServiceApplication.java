package com.streamora.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora comment-service runtime entry point.
 */
@SpringBootApplication
public class CommentServiceApplication {

    /**
     * Starts the comment-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApplication.class, args);
    }
}
