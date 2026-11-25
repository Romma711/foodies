package com.example.Foodies.Config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on the scheduled jobs (expired reservations and waitlist entries). Tests set
 * {@code foodies.scheduling.enabled=false} and call the jobs directly with a test clock.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "foodies.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
