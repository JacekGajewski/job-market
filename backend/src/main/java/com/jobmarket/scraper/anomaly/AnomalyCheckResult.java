package com.jobmarket.scraper.anomaly;

import lombok.Builder;
import lombok.Data;

/**
 * Result of an anomaly check operation.
 * Contains information about whether an anomaly was detected and the context.
 */
@Data
@Builder
public class AnomalyCheckResult {

    /**
     * Reasons for the anomaly check result.
     */
    public enum Reason {
        /** No anomaly detected, normal operation */
        NORMAL,
        /** A significant drop in count was detected */
        DROP_DETECTED,
        /** Previous day's value was used due to persistent anomaly */
        USED_PREVIOUS_VALUE
    }

    private boolean anomalyDetected;
    private int previousCount;
    private double dropPercentage;
    private Reason reason;

    /**
     * Creates a result indicating no anomaly was detected.
     */
    public static AnomalyCheckResult normal() {
        return AnomalyCheckResult.builder()
                .anomalyDetected(false)
                .reason(Reason.NORMAL)
                .build();
    }

    /**
     * Creates a result indicating an anomaly (significant drop) was detected.
     *
     * @param previousCount the count from the previous record
     * @param dropPercentage the percentage drop detected
     * @return anomaly check result with drop details
     */
    public static AnomalyCheckResult anomalyDetected(int previousCount, double dropPercentage) {
        return AnomalyCheckResult.builder()
                .anomalyDetected(true)
                .previousCount(previousCount)
                .dropPercentage(dropPercentage)
                .reason(Reason.DROP_DETECTED)
                .build();
    }

    /**
     * Creates a result indicating the previous day's value was used
     * because the anomaly persisted after retry.
     *
     * @param previousCount the count from the previous record that will be used
     * @return anomaly check result indicating fallback to previous value
     */
    public static AnomalyCheckResult usedPreviousValue(int previousCount) {
        return AnomalyCheckResult.builder()
                .anomalyDetected(true)
                .previousCount(previousCount)
                .reason(Reason.USED_PREVIOUS_VALUE)
                .build();
    }
}
