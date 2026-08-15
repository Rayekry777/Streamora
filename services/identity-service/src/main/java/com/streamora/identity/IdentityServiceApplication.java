package com.streamora.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora identity-service runtime entry point.
 */
@SpringBootApplication
public class IdentityServiceApplication {

    /**
     * Starts the identity-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
