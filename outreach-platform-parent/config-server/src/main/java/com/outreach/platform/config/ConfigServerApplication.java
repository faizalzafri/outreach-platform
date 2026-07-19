package com.outreach.platform.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Centralized configuration server for the Outreach Platform.
 * Serves externalized configuration to all microservices via Spring Cloud Config.
 * Supports native filesystem (local dev) and Git-backed (production) config repositories.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
