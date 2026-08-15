package com.streamora.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora agent-service runtime entry point.
 */
@SpringBootApplication
public class AgentServiceApplication {

    /**
     * Starts the agent-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AgentServiceApplication.class, args);
    }
}
