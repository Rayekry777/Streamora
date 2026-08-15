package com.streamora.identity;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora identity-service runtime entry point.
 */
@SpringBootApplication
@EnableDubbo
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
