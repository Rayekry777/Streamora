package com.streamora.pet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora pet-service runtime entry point.
 */
@SpringBootApplication
public class PetServiceApplication {

    /**
     * Starts the pet-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PetServiceApplication.class, args);
    }
}
