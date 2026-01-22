package com.jobmarket.scraper.anomaly;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnomalyCheckResult {

    public enum Reason {
        NO_PREVIOUS_DATA,
        BELOW_THRESHOLD,
        DROP_DETECTED,
        NORMAL
    }

    private final boolean anomalyDetected;
    private final Integer previousCount;
    private final Double dropPercentage;
    private final Reason reason;

    public static AnomalyCheckResult noPreviousData() {
        return AnomalyCheckResult.builder()
                .anomalyDetected(false)
                .reason(Reason.NO_PREVIOUS_DATA)
                .build();
    }

    public static AnomalyCheckResult noAnomaly(int previousCount) {
        return AnomalyCheckResult.builder()
                .anomalyDetected(false)
                .previousCount(previousCount)
                .reason(Reason.NORMAL)
                .build();
    }

    public static AnomalyCheckResult anomalyDetected(int previousCount, double dropPercentage) {
        return AnomalyCheckResult.builder()
                .anomalyDetected(true)
                .previousCount(previousCount)
                .dropPercentage(dropPercentage)
                .reason(Reason.DROP_DETECTED)
                .build();
    }

    public static AnomalyCheckResult belowMinimumThreshold(int previousCount) {
        return AnomalyCheckResult.builder()
                .anomalyDetected(false)
                .previousCount(previousCount)
                .reason(Reason.BELOW_THRESHOLD)
                .build();
    }
}
