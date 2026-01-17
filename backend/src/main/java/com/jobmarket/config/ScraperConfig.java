package com.jobmarket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scraper.justjoinit")
@Getter
@Setter
public class ScraperConfig {

    private String apiBaseUrl = "https://justjoin.it/api";
    private String webBaseUrl = "https://justjoin.it/job-offers";
    private int connectionTimeoutMs = 10000;
    private int readTimeoutMs = 30000;
    private int maxRetries = 3;

    // Randomized delay settings for human-like scraping patterns
    private int minDelayMs = 3000;
    private int maxDelayMs = 8000;
    private int pauseEveryNRequests = 15;
    private int pauseDurationMs = 45000;

    // Realistic browser user agent
    private String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
}
