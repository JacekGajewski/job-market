package com.jobmarket.scraper.anomaly;

import com.jobmarket.config.AnomalyDetectionConfig;
import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.JobCountRecord;
import com.jobmarket.entity.MetricType;
import com.jobmarket.repository.JobCountRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service responsible for detecting anomalies in job count data.
 * An anomaly is detected when the current count drops more than the configured
 * threshold percentage compared to the previous day's count.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final AnomalyDetectionConfig config;
    private final JobCountRecordRepository jobCountRecordRepository;

    /**
     * Checks if the current count represents an anomaly compared to the previous record.
     *
     * @param currentCount the current job count
     * @param category the category slug
     * @param metricType the metric type
     * @param location the location
     * @param experienceLevel the experience level (can be null)
     * @param salaryMin minimum salary filter (can be null)
     * @param salaryMax maximum salary filter (can be null)
     * @return AnomalyCheckResult indicating whether an anomaly was detected
     */
    public AnomalyCheckResult checkForAnomaly(int currentCount, String category, MetricType metricType,
                                               String location, ExperienceLevel experienceLevel,
                                               Integer salaryMin, Integer salaryMax) {
        Optional<JobCountRecord> previousRecord = jobCountRecordRepository.findPreviousByFilters(
                category, metricType, location, experienceLevel, salaryMin, salaryMax, LocalDateTime.now());

        if (previousRecord.isEmpty()) {
            log.debug("No previous record found for category='{}' metric={} location={} - skipping anomaly check",
                    category, metricType, location);
            return AnomalyCheckResult.normal();
        }

        int previousCount = previousRecord.get().getCount();

        if (previousCount < config.getMinimumCountThreshold()) {
            log.debug("Previous count {} is below minimum threshold {} - skipping anomaly check",
                    previousCount, config.getMinimumCountThreshold());
            return AnomalyCheckResult.normal();
        }

        double dropPercentage = calculateDropPercentage(currentCount, previousCount);

        if (dropPercentage > config.getDropThreshold()) {
            log.warn("Anomaly detected: count dropped from {} to {} ({:.1f}% drop, threshold: {:.1f}%)",
                    previousCount, currentCount, dropPercentage * 100, config.getDropThreshold() * 100);
            return AnomalyCheckResult.anomalyDetected(previousCount, dropPercentage);
        }

        return AnomalyCheckResult.normal();
    }

    /**
     * Validates whether the retry result still exceeds the anomaly threshold.
     * Returns true if the retry count still shows an anomalous drop from the previous count,
     * meaning the previous day's value should be used instead.
     *
     * @param retryCount the count obtained from the retry fetch
     * @param previousCount the count from the previous day's record
     * @return true if the retry still exceeds threshold (use previous value), false if retry is acceptable
     */
    public boolean validateRetryResult(int retryCount, int previousCount) {
        if (previousCount <= 0) {
            log.debug("Previous count is {} - cannot validate retry, accepting retry result", previousCount);
            return false;
        }

        if (previousCount < config.getMinimumCountThreshold()) {
            log.debug("Previous count {} is below minimum threshold {} - accepting retry result",
                    previousCount, config.getMinimumCountThreshold());
            return false;
        }

        double dropPercentage = calculateDropPercentage(retryCount, previousCount);

        if (dropPercentage > config.getDropThreshold()) {
            log.info("Retry result still exceeds threshold: {} -> {} ({:.1f}% drop > {:.1f}% threshold)",
                    previousCount, retryCount, dropPercentage * 100, config.getDropThreshold() * 100);
            return true;
        }

        log.debug("Retry result within threshold: {} -> {} ({:.1f}% drop <= {:.1f}% threshold)",
                previousCount, retryCount, dropPercentage * 100, config.getDropThreshold() * 100);
        return false;
    }

    /**
     * Resolves the final count to use when an anomaly was detected.
     * Returns the higher of the two counts (legacy behavior).
     *
     * @param firstCount the count from the first fetch
     * @param retryCount the count from the retry fetch
     * @return the higher count
     */
    public int resolveCount(int firstCount, int retryCount) {
        int resolved = Math.max(firstCount, retryCount);
        log.debug("Resolved count: max({}, {}) = {}", firstCount, retryCount, resolved);
        return resolved;
    }

    /**
     * Gets the configured retry delay in milliseconds.
     *
     * @return retry delay in ms
     */
    public long getRetryDelayMs() {
        return config.getRetryDelayMs();
    }

    /**
     * Calculates the drop percentage from previousCount to currentCount.
     * A positive result indicates a drop, negative indicates an increase.
     *
     * @param currentCount the current count
     * @param previousCount the previous count (must be > 0)
     * @return the drop percentage as a decimal (e.g., 0.15 for 15% drop)
     */
    private double calculateDropPercentage(int currentCount, int previousCount) {
        return (double) (previousCount - currentCount) / previousCount;
    }
}
