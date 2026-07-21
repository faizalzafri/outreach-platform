package com.outreach.platform.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables async method execution for AI service operations.
 * AI jobs run asynchronously and update their status upon completion.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
