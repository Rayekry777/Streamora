package com.streamora.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora admin-service runtime entry point.
 */
@SpringBootApplication
public class AdminServiceApplication {

    /**
     * Starts the admin-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
