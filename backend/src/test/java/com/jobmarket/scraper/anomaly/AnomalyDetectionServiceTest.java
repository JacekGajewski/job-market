package com.jobmarket.scraper.anomaly;

import com.jobmarket.config.AnomalyDetectionConfig;
import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.JobCountRecord;
import com.jobmarket.entity.MetricType;
import com.jobmarket.entity.SalaryRange;
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
    private static final String SLASK = "slask";

    @BeforeEach
    void setUp() {
        config = new AnomalyDetectionConfig();
        config.setEnabled(true);
        config.setDropThreshold(0.20); // 20% drop threshold
        config.setRetryDelayMs(3000);
        config.setMaxRetries(2);
        config.setMinimumCityCount(5);
        config.setMinimumGlobalCount(50);

        service = new AnomalyDetectionService(repository, config);
    }

    @Nested
    @DisplayName("checkForAnomaly")
    class CheckForAnomaly {

        @Test
        @DisplayName("should return no anomaly when no previous data exists")
        void shouldReturnNoAnomalyWhenNoPreviousData() {
            // given
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(SLASK), any(), any(), any(), any()))
                    .thenReturn(Optional.empty());

            // when
            AnomalyCheckResult result = service.checkForAnomaly(
                    100, JAVA, MetricType.TOTAL, SLASK, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isFalse();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.NO_PREVIOUS_DATA);
            assertThat(result.getPreviousCount()).isNull();
        }

        @Test
        @DisplayName("should return no anomaly when count is similar to previous (15% drop)")
        void shouldReturnNoAnomalyWhenCountSimilar() {
            // given - previous count 100, current count 85 (15% drop, below 20% threshold)
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(SLASK), any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when
            AnomalyCheckResult result = service.checkForAnomaly(
                    85, JAVA, MetricType.TOTAL, SLASK, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isFalse();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.NORMAL);
            assertThat(result.getPreviousCount()).isEqualTo(100);
        }

        @Test
        @DisplayName("should detect anomaly when drop exceeds threshold (25% drop)")
        void shouldDetectAnomalyWhenDropExceedsThreshold() {
            // given - previous count 100, current count 75 (25% drop, above 20% threshold)
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(SLASK), any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when
            AnomalyCheckResult result = service.checkForAnomaly(
                    75, JAVA, MetricType.TOTAL, SLASK, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isTrue();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.DROP_DETECTED);
            assertThat(result.getPreviousCount()).isEqualTo(100);
            assertThat(result.getDropPercentage()).isEqualTo(0.25);
        }

        @Test
        @DisplayName("should detect anomaly at exact threshold (20% drop)")
        void shouldDetectAnomalyAtExactThreshold() {
            // given - previous count 100, current count 79 (21% drop, just above 20% threshold)
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(SLASK), any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when
            AnomalyCheckResult result = service.checkForAnomaly(
                    79, JAVA, MetricType.TOTAL, SLASK, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isTrue();
            assertThat(result.getDropPercentage()).isGreaterThan(0.20);
        }

        @Test
        @DisplayName("should not detect anomaly just below threshold (19% drop)")
        void shouldNotDetectAnomalyJustBelowThreshold() {
            // given - previous count 100, current count 81 (19% drop, just below 20% threshold)
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(SLASK), any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when
            AnomalyCheckResult result = service.checkForAnomaly(
                    81, JAVA, MetricType.TOTAL, SLASK, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isFalse();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.NORMAL);
        }

        @Test
        @DisplayName("should detect massive drop (99% drop like the CDN bug)")
        void shouldDetectMassiveDrop() {
            // given - previous count 230, current count 2 (99% drop)
            JobCountRecord previousRecord = createRecord(230);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(SLASK), any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when
            AnomalyCheckResult result = service.checkForAnomaly(
                    2, JAVA, MetricType.TOTAL, SLASK, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isTrue();
            assertThat(result.getDropPercentage()).isGreaterThan(0.99);
        }

        @Test
        @DisplayName("should skip anomaly check for city when previous count below minimum (5)")
        void shouldSkipCheckWhenCityCountBelowMinimum() {
            // given - previous count 4, below minimumCityCount of 5
            JobCountRecord previousRecord = createRecord(4);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(SLASK), any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when - current count 2 (50% drop, but should be skipped)
            AnomalyCheckResult result = service.checkForAnomaly(
                    2, JAVA, MetricType.TOTAL, SLASK, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isFalse();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.BELOW_THRESHOLD);
        }

        @Test
        @DisplayName("should skip anomaly check for all-locations when previous count below minimum (50)")
        void shouldSkipCheckWhenGlobalCountBelowMinimum() {
            // given - previous count 40, below minimumGlobalCount of 50
            JobCountRecord previousRecord = createRecord(40);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq("all-locations"), any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when - city=null means all-locations
            AnomalyCheckResult result = service.checkForAnomaly(
                    20, JAVA, MetricType.TOTAL, null, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isFalse();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.BELOW_THRESHOLD);
        }

        @Test
        @DisplayName("should not detect anomaly when count increases")
        void shouldNotDetectAnomalyWhenCountIncreases() {
            // given - previous count 100, current count 150 (50% increase)
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(SLASK), any(), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when
            AnomalyCheckResult result = service.checkForAnomaly(
                    150, JAVA, MetricType.TOTAL, SLASK, null, null);

            // then
            assertThat(result.isAnomalyDetected()).isFalse();
            assertThat(result.getReason()).isEqualTo(AnomalyCheckResult.Reason.NORMAL);
        }

        @Test
        @DisplayName("should handle experience level filter correctly")
        void shouldHandleExperienceLevelFilter() {
            // given
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(SLASK), eq(ExperienceLevel.SENIOR), any(), any(), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when
            AnomalyCheckResult result = service.checkForAnomaly(
                    50, JAVA, MetricType.TOTAL, SLASK, ExperienceLevel.SENIOR, null);

            // then
            assertThat(result.isAnomalyDetected()).isTrue();
        }

        @Test
        @DisplayName("should handle salary range filter correctly")
        void shouldHandleSalaryRangeFilter() {
            // given
            JobCountRecord previousRecord = createRecord(100);
            when(repository.findPreviousByFilters(
                    eq(JAVA), eq(MetricType.TOTAL), eq(SLASK), any(),
                    eq(SalaryRange.OVER_30K.getMin()), eq(SalaryRange.OVER_30K.getMax()), any()))
                    .thenReturn(Optional.of(previousRecord));

            // when
            AnomalyCheckResult result = service.checkForAnomaly(
                    50, JAVA, MetricType.TOTAL, SLASK, null, SalaryRange.OVER_30K);

            // then
            assertThat(result.isAnomalyDetected()).isTrue();
        }
    }

    @Nested
    @DisplayName("resolveCount")
    class ResolveCount {

        @Test
        @DisplayName("should return higher count when retry is higher")
        void shouldReturnHigherCountWhenRetryIsHigher() {
            // when
            int resolved = service.resolveCount(2, 230);

            // then
            assertThat(resolved).isEqualTo(230);
        }

        @Test
        @DisplayName("should return first count when first is higher")
        void shouldReturnFirstCountWhenFirstIsHigher() {
            // when
            int resolved = service.resolveCount(230, 2);

            // then
            assertThat(resolved).isEqualTo(230);
        }

        @Test
        @DisplayName("should return either when counts are equal")
        void shouldReturnEitherWhenCountsAreEqual() {
            // when
            int resolved = service.resolveCount(100, 100);

            // then
            assertThat(resolved).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("isEnabled")
    class IsEnabled {

        @Test
        @DisplayName("should return true when enabled")
        void shouldReturnTrueWhenEnabled() {
            // given
            config.setEnabled(true);

            // when/then
            assertThat(service.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should return false when disabled")
        void shouldReturnFalseWhenDisabled() {
            // given
            config.setEnabled(false);

            // when/then
            assertThat(service.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("getRetryDelayMs")
    class GetRetryDelayMs {

        @Test
        @DisplayName("should return configured delay")
        void shouldReturnConfiguredDelay() {
            // given
            config.setRetryDelayMs(5000);

            // when/then
            assertThat(service.getRetryDelayMs()).isEqualTo(5000);
        }
    }

    private JobCountRecord createRecord(int count) {
        return JobCountRecord.builder()
                .category(JAVA)
                .count(count)
                .fetchedAt(LocalDateTime.now().minusDays(1))
                .location(SLASK)
                .metricType(MetricType.TOTAL)
                .city(SLASK)
                .recordDate(LocalDate.now().minusDays(1))
                .build();
    }
}
