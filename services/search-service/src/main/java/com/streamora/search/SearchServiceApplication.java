package com.streamora.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streamora search-service runtime entry point.
 */
@SpringBootApplication
public class SearchServiceApplication {

    /**
     * Starts the search-service process.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
