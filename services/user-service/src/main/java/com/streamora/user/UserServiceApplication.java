package com.streamora.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora user-service runtime entry point.
 */
@SpringBootApplication
public class UserServiceApplication {

    /**
     * Starts the user-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
