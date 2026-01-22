package com.jobmarket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scraper.anomaly-detection")
@Getter
@Setter
public class AnomalyDetectionConfig {

    /**
     * Enable/disable anomaly detection
     */
    private boolean enabled = true;

    /**
     * Drop percentage threshold to trigger retry (0.0 - 1.0)
     * Default: 0.20 (20% drop triggers retry)
     */
    private double dropThreshold = 0.20;

    /**
     * Delay before retry in milliseconds
     */
    private int retryDelayMs = 3000;

    /**
     * Maximum retries on anomaly detection
     */
    private int maxRetries = 2;

    /**
     * Minimum absolute count for city-level scrapes.
     * If previous count is below this, skip anomaly detection.
     */
    private int minimumCityCount = 5;

    /**
     * Minimum absolute count for all-locations scrapes.
     * If previous count is below this, skip anomaly detection.
     */
    private int minimumGlobalCount = 50;
}
