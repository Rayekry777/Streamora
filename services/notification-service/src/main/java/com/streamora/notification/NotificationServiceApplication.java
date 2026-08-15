package com.streamora.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora notification-service runtime entry point.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    /**
     * Starts the notification-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
