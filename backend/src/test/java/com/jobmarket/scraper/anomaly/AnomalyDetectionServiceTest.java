package com.jobmarket.scraper.anomaly;

import com.jobmarket.config.AnomalyDetectionConfig;
import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.JobCountRecord;
import com.jobmarket.entity.MetricType;
import com.jobmarket.repository.JobCountRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnomalyDetectionService")
class AnomalyDetectionServiceTest {

    @Mock
    private JobCountRecordRepository repository;

    private AnomalyDetectionConfig config;
    private AnomalyDetectionService service;

    private static final String JAVA = "java";
    private static final String ALL_LOCATIONS = "all-locations";

    @BeforeEach
    void setUp() {
        config = new AnomalyDetectionConfig();
        config.setDropThreshold(0.10); // 10% threshold
        config.setRetryDelayMs(3000);
        config.setMinimumCountThreshold(10);
        service = new AnomalyDetectionService(config, repository);
    }

    @Nested
    @DisplayName("checkForAnomaly")
    class CheckForAnomaly {

        @Test
        @DisplayName("should detect anomaly when drop exceeds threshold (15% drop)")
        void shouldDetectAnomalyWhenDropExceedsThreshold() {
            // given
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(ALL_LOCATIONS),
                    any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when - 15% drop: 100 -> 85
            AnomalyCheckResult result = service.checkForAnomaly(
                    85, JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isTrue();
            assertThat(result.getPreviousCount()).isEqualTo(100);
            assertThat(result.getDropPercentage()).isEqualTo(0.15);
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.DROP_DETECTED);
        }

        @Test
        @DisplayName("should detect anomaly when drop is just over threshold (11% drop)")
        void shouldDetectAnomalyWhenDropJustOverThreshold() {
            // given
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(ALL_LOCATIONS),
                    any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when - 11% drop: 100 -> 89
            AnomalyCheckResult result = service.checkForAnomaly(
                    89, JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isTrue();
            assertThat(result.getPreviousCount()).isEqualTo(100);
            assertThat(result.getDropPercentage()).isEqualTo(0.11);
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.DROP_DETECTED);
        }

        @Test
        @DisplayName("should NOT detect anomaly when drop is exactly at threshold (10% drop)")
        void shouldNotDetectAnomalyWhenDropExactlyAtThreshold() {
            // given
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(ALL_LOCATIONS),
                    any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when - 10% drop: 100 -> 90 (exactly at threshold, should NOT trigger)
            AnomalyCheckResult result = service.checkForAnomaly(
                    90, JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isFalse();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.NORMAL);
        }

        @Test
        @DisplayName("should NOT detect anomaly when drop is below threshold (9% drop)")
        void shouldNotDetectAnomalyWhenDropBelowThreshold() {
            // given
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(ALL_LOCATIONS),
                    any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when - 9% drop: 100 -> 91
            AnomalyCheckResult result = service.checkForAnomaly(
                    91, JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isFalse();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.NORMAL);
        }

        @Test
        @DisplayName("should NOT detect anomaly when count increases")
        void shouldNotDetectAnomalyWhenCountIncreases() {
            // given
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(ALL_LOCATIONS),
                    any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when - count increased: 100 -> 120
            AnomalyCheckResult result = service.checkForAnomaly(
                    120, JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isFalse();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.NORMAL);
        }

        @Test
        @DisplayName("should NOT detect anomaly when no previous record exists")
        void shouldNotDetectAnomalyWhenNoPreviousRecord() {
            // given
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(ALL_LOCATIONS),
                    any(), any(), any(), any()))
                    .thenReturn(Optional.empty());

            // when
            AnomalyCheckResult result = service.checkForAnomaly(
                    50, JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isFalse();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.NORMAL);
        }

        @Test
        @DisplayName("should NOT detect anomaly when previous count is below minimum threshold")
        void shouldNotDetectAnomalyWhenPreviousCountBelowMinimum() {
            // given - previous count of 5 is below minimum threshold of 10
            JobCountRecord previousRecord = createRecord(5);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(ALL_LOCATIONS),
                    any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when - 80% drop from 5 to 1, but should skip check due to low count
            AnomalyCheckResult result = service.checkForAnomaly(
                    1, JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isFalse();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.NORMAL);
        }

        @Test
        @DisplayName("should detect anomaly with experience level filter")
        void shouldDetectAnomalyWithExperienceLevelFilter() {
            // given
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(ALL_LOCATIONS),
                    eq(ExperienceLevel.SENIOR), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when - 20% drop with senior filter
            AnomalyCheckResult result = service.checkForAnomaly(
                    80, JAVA, MetricType.TOTAL, ALL_LOCATIONS, ExperienceLevel.SENIOR, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isTrue();
            assertThat(result.getPreviousCount()).isEqualTo(100);
        }

        @Test
        @DisplayName("should detect anomaly with salary range filter")
        void shouldDetectAnomalyWithSalaryRangeFilter() {
            // given
            JobCountRecord previousRecord = createRecord(200);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(ALL_LOCATIONS),
                    any(), eq(25000), eq(30000), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when - 25% drop with salary filter
            AnomalyCheckResult result = service.checkForAnomaly(
                    150, JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, 25000, 30000);

            // then
            assertThat(result.isAnomalyDetected()).isTrue();
            assertThat(result.getPreviousCount()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("resolveCount")
    class ResolveCount {

        @Test
        @DisplayName("should return the higher count when first is higher")
        void shouldReturnFirstCountWhenHigher() {
            // when
            int resolved = service.resolveCount(100, 80);

            // then
            assertThat(resolved).isEqualTo(100);
        }

        @Test
        @DisplayName("should return the higher count when retry is higher")
        void shouldReturnRetryCountWhenHigher() {
            // when
            int resolved = service.resolveCount(80, 100);

            // then
            assertThat(resolved).isEqualTo(100);
        }

        @Test
        @DisplayName("should return either count when both are equal")
        void shouldReturnEitherCountWhenEqual() {
            // when
            int resolved = service.resolveCount(100, 100);

            // then
            assertThat(resolved).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("getRetryDelayMs")
    class GetRetryDelayMs {

        @Test
        @DisplayName("should return configured retry delay")
        void shouldReturnConfiguredRetryDelay() {
            // when
            long delay = service.getRetryDelayMs();

            // then
            assertThat(delay).isEqualTo(3000);
        }
    }

    @Nested
    @DisplayName("validateRetryResult")
    class ValidateRetryResult {

        @Test
        @DisplayName("should return true when retry still exceeds threshold (15% drop)")
        void shouldReturnTrueWhenRetryStillExceedsThreshold() {
            // when - 15% drop from 100 to 85, threshold is 10%
            boolean usePreviousValue = service.validateRetryResult(85, 100);

            // then
            assertThat(usePreviousValue).isTrue();
        }

        @Test
        @DisplayName("should return true when retry just exceeds threshold (11% drop)")
        void shouldReturnTrueWhenRetryJustExceedsThreshold() {
            // when - 11% drop from 100 to 89, threshold is 10%
            boolean usePreviousValue = service.validateRetryResult(89, 100);

            // then
            assertThat(usePreviousValue).isTrue();
        }

        @Test
        @DisplayName("should return false when retry is within threshold (9% drop)")
        void shouldReturnFalseWhenRetryIsWithinThreshold() {
            // when - 9% drop from 100 to 91, threshold is 10%
            boolean usePreviousValue = service.validateRetryResult(91, 100);

            // then
            assertThat(usePreviousValue).isFalse();
        }

        @Test
        @DisplayName("should return false when retry exactly at threshold (10% drop)")
        void shouldReturnFalseWhenRetryExactlyAtThreshold() {
            // when - 10% drop from 100 to 90, exactly at threshold (not exceeding)
            boolean usePreviousValue = service.validateRetryResult(90, 100);

            // then
            assertThat(usePreviousValue).isFalse();
        }

        @Test
        @DisplayName("should return false when retry count increases")
        void shouldReturnFalseWhenRetryCountIncreases() {
            // when - retry count is higher than previous
            boolean usePreviousValue = service.validateRetryResult(120, 100);

            // then
            assertThat(usePreviousValue).isFalse();
        }

        @Test
        @DisplayName("should return false when previous count is zero")
        void shouldReturnFalseWhenPreviousCountIsZero() {
            // when - cannot calculate percentage with zero
            boolean usePreviousValue = service.validateRetryResult(50, 0);

            // then
            assertThat(usePreviousValue).isFalse();
        }

        @Test
        @DisplayName("should return false when previous count is negative")
        void shouldReturnFalseWhenPreviousCountIsNegative() {
            // when - cannot calculate percentage with negative
            boolean usePreviousValue = service.validateRetryResult(50, -10);

            // then
            assertThat(usePreviousValue).isFalse();
        }

        @Test
        @DisplayName("should return false when previous count is below minimum threshold")
        void shouldReturnFalseWhenPreviousCountBelowMinimum() {
            // given - minimum threshold is 10, previous is 5
            // when - even with 80% drop (5 -> 1), should skip due to low count
            boolean usePreviousValue = service.validateRetryResult(1, 5);

            // then
            assertThat(usePreviousValue).isFalse();
        }
    }

    @Nested
    @DisplayName("AnomalyCheckResult.usedPreviousValue")
    class UsedPreviousValueResult {

        @Test
        @DisplayName("should create result with USED_PREVIOUS_VALUE reason")
        void shouldCreateResultWithUsedPreviousValueReason() {
            // when
            AnomalyCheckResult result = AnomalyCheckResult.usedPreviousValue(100);

            // then
            assertThat(result.isAnomalyDetected()).isTrue();
            assertThat(result.getPreviousCount()).isEqualTo(100);
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.USED_PREVIOUS_VALUE);
        }

        @Test
        @DisplayName("should mark anomaly detected when using previous value")
        void shouldMarkAnomalyDetectedWhenUsingPreviousValue() {
            // when
            AnomalyCheckResult result = AnomalyCheckResult.usedPreviousValue(200);

            // then
            assertThat(result.isAnomalyDetected()).isTrue();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.USED_PREVIOUS_VALUE);
        }

        @Test
        @DisplayName("should store the previous count value")
        void shouldStorePreviousCountValue() {
            // when
            AnomalyCheckResult result = AnomalyCheckResult.usedPreviousValue(500);

            // then
            assertThat(result.getPreviousCount()).isEqualTo(500);
        }

        @Test
        @DisplayName("should have zero drop percentage by default")
        void shouldHaveZeroDropPercentageByDefault() {
            // when
            AnomalyCheckResult result = AnomalyCheckResult.usedPreviousValue(100);

            // then - dropPercentage is not set, defaults to 0.0
            assertThat(result.getDropPercentage()).isEqualTo(0.0);
        }
    }

    private JobCountRecord createRecord(int count) {
        return JobCountRecord.builder()
                .category(JAVA)
                .metricType(MetricType.TOTAL)
                .location(ALL_LOCATIONS)
                .count(count)
                .fetchedAt(LocalDateTime.now().minusDays(1))
                .recordDate(LocalDate.now().minusDays(1))
                .build();
    }
}
