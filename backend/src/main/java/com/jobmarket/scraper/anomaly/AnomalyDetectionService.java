package com.jobmarket.scraper.anomaly;

import com.jobmarket.config.AnomalyDetectionConfig;
import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.JobCountRecord;
import com.jobmarket.entity.MetricType;
import com.jobmarket.entity.SalaryRange;
import com.jobmarket.repository.JobCountRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final JobCountRecordRepository repository;
    private final AnomalyDetectionConfig config;

    /**
     * Checks if a fetched count is anomalous compared to previous data.
     *
     * @param currentCount    the newly fetched count
     * @param category        category slug
     * @param metricType      metric type
     * @param city            city slug (null for all-locations)
     * @param experienceLevel experience level filter (null for all)
     * @param salaryRange     salary range filter (null for any)
     * @return AnomalyCheckResult containing detection result and previous count
     */
    public AnomalyCheckResult checkForAnomaly(
            int currentCount,
            String category,
            MetricType metricType,
            String city,
            ExperienceLevel experienceLevel,
            SalaryRange salaryRange) {

        // Determine location (city slug or metricType's default location)
        String location = city != null ? city : metricType.getLocation();

        // Get salary bounds
        Integer salaryMin = salaryRange != null ? salaryRange.getMin() : null;
        Integer salaryMax = salaryRange != null ? salaryRange.getMax() : null;

        // Query previous count
        Optional<JobCountRecord> previousRecord = repository.findPreviousByFilters(
                category,
                metricType,
                location,
                experienceLevel,
                salaryMin,
                salaryMax,
                LocalDateTime.now());

        if (previousRecord.isEmpty()) {
            log.debug("No previous data for {}[{}] city={}, skipping anomaly check",
                    category, metricType, city);
            return AnomalyCheckResult.noPreviousData();
        }

        int previousCount = previousRecord.get().getCount();

        // Determine minimum threshold based on city filter
        int minimumThreshold = city != null ? config.getMinimumCityCount() : config.getMinimumGlobalCount();

        // Skip anomaly detection for very low counts
        if (previousCount < minimumThreshold) {
            log.debug("Previous count {} below minimum threshold {} for {}[{}] city={}, skipping anomaly check",
                    previousCount, minimumThreshold, category, metricType, city);
            return AnomalyCheckResult.belowMinimumThreshold(previousCount);
        }

        // Calculate drop percentage
        double dropPercentage = (double) (previousCount - currentCount) / previousCount;

        // Check if drop exceeds threshold
        if (dropPercentage > config.getDropThreshold()) {
            log.debug("Anomaly detected for {}[{}] city={}: current={}, previous={}, drop={:.1f}%",
                    category, metricType, city, currentCount, previousCount, dropPercentage * 100);
            return AnomalyCheckResult.anomalyDetected(previousCount, dropPercentage);
        }

        return AnomalyCheckResult.noAnomaly(previousCount);
    }

    /**
     * Decides which count to use when retry was performed.
     * Strategy: Use the higher count (CDN stale data is typically lower).
     *
     * @param firstAttemptCount count from first attempt
     * @param retryCount        count from retry attempt
     * @return the resolved count to use
     */
    public int resolveCount(int firstAttemptCount, int retryCount) {
        return Math.max(firstAttemptCount, retryCount);
    }

    /**
     * Checks if anomaly detection is enabled.
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * Gets the configured retry delay in milliseconds.
     */
    public int getRetryDelayMs() {
        return config.getRetryDelayMs();
    }
}
