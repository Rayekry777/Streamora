package com.streamora.admin;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora admin-service runtime entry point.
 */
@SpringBootApplication
@EnableDubbo
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
