package com.streamora.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora gateway-service runtime entry point.
 */
@SpringBootApplication
public class GatewayServiceApplication {

    /**
     * Starts the gateway-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
