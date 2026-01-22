package com.jobmarket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "anomaly-detection")
@Getter
@Setter
public class AnomalyDetectionConfig {

    /**
     * Threshold for detecting anomalous drops in job counts.
     * If the current count drops more than this percentage from the previous count,
     * it's flagged as an anomaly. Default is 10% (0.10).
     */
    private double dropThreshold = 0.10;

    /**
     * Delay in milliseconds before retrying a fetch when an anomaly is detected.
     */
    private long retryDelayMs = 3000;

    /**
     * Minimum count threshold below which anomaly detection is skipped.
     * Small counts can have high percentage swings that are not anomalous.
     */
    private int minimumCountThreshold = 10;
}
