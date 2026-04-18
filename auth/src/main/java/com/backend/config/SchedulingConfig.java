package com.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class responsible for enabling Spring's scheduled task execution capabilities.
 * <p>
 * This acts as the heartbeat for any <code>@Scheduled</code> tasks within the application context,
 * allowing background jobs (such as cache synchronization) to run asynchronously at defined intervals.
 * </p>
 *
 * @since 1.0.0
 * @version 1.0.0
 * @author logTAHA
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

}
