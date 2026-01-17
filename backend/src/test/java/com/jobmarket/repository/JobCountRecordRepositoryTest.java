package com.jobmarket.repository;

import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.JobCountRecord;
import com.jobmarket.entity.MetricType;
import com.jobmarket.entity.SalaryRange;
import com.jobmarket.repository.testdata.JobCountRecordTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.jobmarket.repository.testdata.JobCountRecordTestBuilder.aRecord;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
@DisplayName("JobCountRecordRepository")
class JobCountRecordRepositoryTest {

    @Autowired
    private JobCountRecordRepository repository;

    private static final String JAVA = "java";
    private static final String DATA = "data";
    private static final String WROCLAW = "wroclaw";
    private static final String SLASK = "slask";
    private static final String ALL_LOCATIONS = "all-locations";

    // Use a base time for consistent test data - tests will use relative times from this
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2024, 6, 15, 12, 0, 0);
    private static final LocalDateTime ONE_DAY_AGO = BASE_TIME.minusDays(1);
    private static final LocalDateTime TWO_DAYS_AGO = BASE_TIME.minusDays(2);
    private static final LocalDateTime THREE_DAYS_AGO = BASE_TIME.minusDays(3);
    private static final LocalDateTime FIVE_DAYS_AGO = BASE_TIME.minusDays(5);
    private static final LocalDateTime TEN_DAYS_AGO = BASE_TIME.minusDays(10);
    private static final LocalDateTime THIRTY_DAYS_AGO = BASE_TIME.minusDays(30);
    // Query range that covers all test data
    private static final LocalDateTime QUERY_START = BASE_TIME.minusDays(60);
    private static final LocalDateTime QUERY_END = BASE_TIME.plusDays(1);

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("findByFilters")
    class FindByFilters {

        @Nested
        @DisplayName("Category Filtering")
        class CategoryFiltering {

            @Test
            @DisplayName("should return only records matching the specified category")
            void shouldReturnRecordsMatchingCategory() {
                // given
                repository.save(aRecord().withCategory(JAVA).withCount(100).build());
                repository.save(aRecord().withCategory(JAVA).withCount(110).build());
                repository.save(aRecord().withCategory(DATA).withCount(200).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null,
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(2);
                assertThat(results).allMatch(r -> r.getCategory().equals(JAVA));
            }

            @Test
            @DisplayName("should return empty list when category has no records")
            void shouldReturnEmptyWhenNoRecordsForCategory() {
                // given
                repository.save(aRecord().withCategory(JAVA).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        "python", MetricType.TOTAL, ALL_LOCATIONS, null, null, null,
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).isEmpty();
            }
        }

        @Nested
        @DisplayName("MetricType Filtering")
        class MetricTypeFiltering {

            @Test
            @DisplayName("should return only records matching the specified metric type")
            void shouldReturnRecordsMatchingMetricType() {
                // given
                repository.save(aRecord().withMetricType(MetricType.TOTAL).withCount(100).build());
                repository.save(aRecord().withMetricType(MetricType.REMOTE).withLocation("remote").withCount(50).build());
                repository.save(aRecord().withMetricType(MetricType.WITH_SALARY).withCount(80).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null,
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getMetricType()).isEqualTo(MetricType.TOTAL);
            }

            @ParameterizedTest
            @EnumSource(MetricType.class)
            @DisplayName("should filter by each metric type correctly")
            void shouldFilterByEachMetricType(MetricType metricType) {
                // given
                String location = metricType.getLocation();
                repository.save(aRecord().withMetricType(metricType).withLocation(location).withCount(100).build());

                for (MetricType other : MetricType.values()) {
                    if (other != metricType) {
                        repository.save(aRecord().withMetricType(other).withLocation(other.getLocation()).withCount(50).build());
                    }
                }

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, metricType, location, null, null, null,
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getMetricType()).isEqualTo(metricType);
            }
        }

        @Nested
        @DisplayName("Location Filtering")
        class LocationFiltering {

            @Test
            @DisplayName("should return only records matching the specified location")
            void shouldReturnRecordsMatchingLocation() {
                // given
                repository.save(aRecord().withLocation(ALL_LOCATIONS).withCount(500).build());
                repository.save(aRecord().withLocation(WROCLAW).withCount(100).build());
                repository.save(aRecord().withLocation(SLASK).withCount(80).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, WROCLAW, null, null, null,
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getLocation()).isEqualTo(WROCLAW);
                assertThat(results.get(0).getCount()).isEqualTo(100);
            }
        }

        @Nested
        @DisplayName("Experience Level Filtering - Critical Null Handling")
        class ExperienceLevelFiltering {

            @Test
            @DisplayName("should return ONLY records with NULL experience level when filter is null")
            void shouldReturnOnlyNullExperienceLevelWhenFilterIsNull() {
                // given - records with various experience levels
                repository.save(aRecord().withExperienceLevel(null).withCount(500).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.JUNIOR).withCount(100).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.MID).withCount(200).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.SENIOR).withCount(150).build());

                // when - filter with null (meaning "all levels" aggregate)
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null,
                        QUERY_START, QUERY_END);

                // then - should ONLY return the null experience level record
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getExperienceLevel()).isNull();
                assertThat(results.get(0).getCount()).isEqualTo(500);
            }

            @Test
            @DisplayName("should NOT mix null and non-null experience levels when filter is null")
            void shouldNotMixNullAndNonNullExperienceLevels() {
                // given
                repository.save(aRecord().withExperienceLevel(null).withCount(500).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.JUNIOR).withCount(100).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.MID).withCount(200).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.SENIOR).withCount(150).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null,
                        QUERY_START, QUERY_END);

                // then - verify no Junior/Mid/Senior records leak through
                assertThat(results)
                        .extracting(JobCountRecord::getExperienceLevel)
                        .containsOnly((ExperienceLevel) null);
                assertThat(results)
                        .extracting(JobCountRecord::getExperienceLevel)
                        .doesNotContain(ExperienceLevel.JUNIOR, ExperienceLevel.MID, ExperienceLevel.SENIOR);
            }

            @Test
            @DisplayName("should return only JUNIOR records when JUNIOR filter is selected")
            void shouldReturnOnlyJuniorWhenJuniorFilterSelected() {
                // given
                repository.save(aRecord().withExperienceLevel(null).withCount(500).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.JUNIOR).withCount(100).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.MID).withCount(200).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.SENIOR).withCount(150).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, ExperienceLevel.JUNIOR, null, null,
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getExperienceLevel()).isEqualTo(ExperienceLevel.JUNIOR);
                assertThat(results.get(0).getCount()).isEqualTo(100);
            }

            @Test
            @DisplayName("should return only MID records when MID filter is selected")
            void shouldReturnOnlyMidWhenMidFilterSelected() {
                // given
                repository.save(aRecord().withExperienceLevel(null).withCount(500).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.JUNIOR).withCount(100).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.MID).withCount(200).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.SENIOR).withCount(150).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, ExperienceLevel.MID, null, null,
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getExperienceLevel()).isEqualTo(ExperienceLevel.MID);
                assertThat(results.get(0).getCount()).isEqualTo(200);
            }

            @Test
            @DisplayName("should return only SENIOR records when SENIOR filter is selected")
            void shouldReturnOnlySeniorWhenSeniorFilterSelected() {
                // given
                repository.save(aRecord().withExperienceLevel(null).withCount(500).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.JUNIOR).withCount(100).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.MID).withCount(200).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.SENIOR).withCount(150).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, ExperienceLevel.SENIOR, null, null,
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getExperienceLevel()).isEqualTo(ExperienceLevel.SENIOR);
                assertThat(results.get(0).getCount()).isEqualTo(150);
            }

            @ParameterizedTest
            @EnumSource(ExperienceLevel.class)
            @DisplayName("should filter by each experience level correctly")
            void shouldFilterByEachExperienceLevel(ExperienceLevel level) {
                // given
                repository.save(aRecord().withExperienceLevel(null).withCount(500).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.JUNIOR).withCount(100).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.MID).withCount(200).build());
                repository.save(aRecord().withExperienceLevel(ExperienceLevel.SENIOR).withCount(150).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, level, null, null,
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getExperienceLevel()).isEqualTo(level);
            }
        }

        @Nested
        @DisplayName("Salary Range Filtering - Critical Null Handling")
        class SalaryRangeFiltering {

            @Test
            @DisplayName("should return ONLY records with NULL salary when filter is null")
            void shouldReturnOnlyNullSalaryWhenFilterIsNull() {
                // given - records with various salary ranges
                repository.save(aRecord().withSalaryRange(null).withCount(500).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.UNDER_25K).withCount(100).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.RANGE_25_30K).withCount(150).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.OVER_30K).withCount(200).build());

                // when - filter with null salary (meaning "any salary" aggregate)
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null,
                        QUERY_START, QUERY_END);

                // then - should ONLY return the null salary record
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getSalaryMin()).isNull();
                assertThat(results.get(0).getSalaryMax()).isNull();
                assertThat(results.get(0).getCount()).isEqualTo(500);
            }

            @Test
            @DisplayName("should NOT mix null and non-null salary ranges when filter is null")
            void shouldNotMixNullAndNonNullSalaryRanges() {
                // given
                repository.save(aRecord().withSalaryRange(null).withCount(500).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.UNDER_25K).withCount(100).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.RANGE_25_30K).withCount(150).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.OVER_30K).withCount(200).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null,
                        QUERY_START, QUERY_END);

                // then - verify no specific salary records leak through
                assertThat(results)
                        .allSatisfy(record -> {
                            assertThat(record.getSalaryMin()).isNull();
                            assertThat(record.getSalaryMax()).isNull();
                        });
            }

            @Test
            @DisplayName("should return only UNDER_25K records when UNDER_25K filter is selected")
            void shouldReturnOnlyUnder25kWhenUnder25kFilterSelected() {
                // given
                repository.save(aRecord().withSalaryRange(null).withCount(500).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.UNDER_25K).withCount(100).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.RANGE_25_30K).withCount(150).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.OVER_30K).withCount(200).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null,
                        SalaryRange.UNDER_25K.getMin(), SalaryRange.UNDER_25K.getMax(),
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getSalaryMin()).isEqualTo(SalaryRange.UNDER_25K.getMin());
                assertThat(results.get(0).getSalaryMax()).isEqualTo(SalaryRange.UNDER_25K.getMax());
                assertThat(results.get(0).getCount()).isEqualTo(100);
            }

            @Test
            @DisplayName("should return only RANGE_25_30K records when RANGE_25_30K filter is selected")
            void shouldReturnOnly25to30kWhen25to30kFilterSelected() {
                // given
                repository.save(aRecord().withSalaryRange(null).withCount(500).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.UNDER_25K).withCount(100).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.RANGE_25_30K).withCount(150).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.OVER_30K).withCount(200).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null,
                        SalaryRange.RANGE_25_30K.getMin(), SalaryRange.RANGE_25_30K.getMax(),
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getSalaryMin()).isEqualTo(SalaryRange.RANGE_25_30K.getMin());
                assertThat(results.get(0).getSalaryMax()).isEqualTo(SalaryRange.RANGE_25_30K.getMax());
                assertThat(results.get(0).getCount()).isEqualTo(150);
            }

            @Test
            @DisplayName("should return only OVER_30K records when OVER_30K filter is selected")
            void shouldReturnOnlyOver30kWhenOver30kFilterSelected() {
                // given
                repository.save(aRecord().withSalaryRange(null).withCount(500).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.UNDER_25K).withCount(100).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.RANGE_25_30K).withCount(150).build());
                repository.save(aRecord().withSalaryRange(SalaryRange.OVER_30K).withCount(200).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null,
                        SalaryRange.OVER_30K.getMin(), SalaryRange.OVER_30K.getMax(),
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getSalaryMin()).isEqualTo(SalaryRange.OVER_30K.getMin());
                assertThat(results.get(0).getSalaryMax()).isEqualTo(SalaryRange.OVER_30K.getMax());
                assertThat(results.get(0).getCount()).isEqualTo(200);
            }
        }

        @Nested
        @DisplayName("Combined Filter Tests")
        class CombinedFilterTests {

            @Test
            @DisplayName("should return records matching all specified filters")
            void shouldReturnRecordsMatchingAllFilters() {
                // given - create records with various combinations
                repository.save(aRecord()
                        .withCategory(JAVA)
                        .withMetricType(MetricType.TOTAL)
                        .withLocation(WROCLAW)
                        .withExperienceLevel(ExperienceLevel.SENIOR)
                        .withSalaryRange(SalaryRange.OVER_30K)
                        .withCount(50)
                        .build());

                // other combinations that should NOT match
                repository.save(aRecord()
                        .withCategory(JAVA)
                        .withMetricType(MetricType.TOTAL)
                        .withLocation(WROCLAW)
                        .withExperienceLevel(ExperienceLevel.JUNIOR)
                        .withSalaryRange(SalaryRange.OVER_30K)
                        .withCount(30)
                        .build());

                repository.save(aRecord()
                        .withCategory(JAVA)
                        .withMetricType(MetricType.TOTAL)
                        .withLocation(WROCLAW)
                        .withExperienceLevel(ExperienceLevel.SENIOR)
                        .withSalaryRange(SalaryRange.UNDER_25K)
                        .withCount(40)
                        .build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, WROCLAW, ExperienceLevel.SENIOR,
                        SalaryRange.OVER_30K.getMin(), SalaryRange.OVER_30K.getMax(),
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                JobCountRecord result = results.get(0);
                assertThat(result.getCategory()).isEqualTo(JAVA);
                assertThat(result.getMetricType()).isEqualTo(MetricType.TOTAL);
                assertThat(result.getLocation()).isEqualTo(WROCLAW);
                assertThat(result.getExperienceLevel()).isEqualTo(ExperienceLevel.SENIOR);
                assertThat(result.getSalaryMin()).isEqualTo(SalaryRange.OVER_30K.getMin());
                assertThat(result.getSalaryMax()).isEqualTo(SalaryRange.OVER_30K.getMax());
                assertThat(result.getCount()).isEqualTo(50);
            }

            @Test
            @DisplayName("should return empty list when no matching combination exists")
            void shouldReturnEmptyWhenNoMatchingCombination() {
                // given - records exist but not the requested combination
                repository.save(aRecord()
                        .withExperienceLevel(ExperienceLevel.JUNIOR)
                        .withSalaryRange(SalaryRange.UNDER_25K)
                        .build());

                // when - search for a different combination
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, ExperienceLevel.SENIOR,
                        SalaryRange.OVER_30K.getMin(), SalaryRange.OVER_30K.getMax(),
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).isEmpty();
            }
        }

        @Nested
        @DisplayName("Date Range Filtering")
        class DateRangeFiltering {

            @Test
            @DisplayName("should return only records within date range")
            void shouldFilterByDateRange() {
                // given
                repository.save(aRecord().fetchedAt(FIVE_DAYS_AGO).withCount(100).build());
                repository.save(aRecord().fetchedAt(THREE_DAYS_AGO).withCount(110).build());
                repository.save(aRecord().fetchedAt(ONE_DAY_AGO).withCount(120).build());

                // when - query for last 4 days from BASE_TIME
                LocalDateTime startDate = BASE_TIME.minusDays(4);
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null,
                        startDate, BASE_TIME);

                // then
                assertThat(results).hasSize(2);
                assertThat(results).extracting(JobCountRecord::getCount).containsExactly(110, 120);
            }

            @Test
            @DisplayName("should include records at boundary dates")
            void shouldIncludeRecordsAtBoundaryDates() {
                // given
                LocalDateTime exactStart = BASE_TIME.minusDays(3).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime exactEnd = BASE_TIME.minusDays(1).withHour(23).withMinute(59).withSecond(59);

                repository.save(aRecord().fetchedAt(exactStart).withCount(100).build());
                repository.save(aRecord().fetchedAt(exactEnd).withCount(120).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null,
                        exactStart, exactEnd);

                // then
                assertThat(results).hasSize(2);
            }

            @Test
            @DisplayName("should return empty when date range has no records")
            void shouldReturnEmptyWhenDateRangeHasNoRecords() {
                // given
                repository.save(aRecord().fetchedAt(TEN_DAYS_AGO).build());

                // when - query for last 3 days only
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null,
                        THREE_DAYS_AGO, BASE_TIME);

                // then
                assertThat(results).isEmpty();
            }
        }

        @Nested
        @DisplayName("Ordering")
        class Ordering {

            @Test
            @DisplayName("should return records ordered by fetchedAt ascending")
            void shouldReturnRecordsOrderedByFetchedAtAsc() {
                // given - save in random order
                repository.save(aRecord().fetchedAt(ONE_DAY_AGO).withCount(300).build());
                repository.save(aRecord().fetchedAt(FIVE_DAYS_AGO).withCount(100).build());
                repository.save(aRecord().fetchedAt(THREE_DAYS_AGO).withCount(200).build());

                // when
                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null,
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(3);
                assertThat(results).extracting(JobCountRecord::getCount)
                        .containsExactly(100, 200, 300);
            }
        }

        @Nested
        @DisplayName("Parameterized Filter Combinations")
        class ParameterizedFilterCombinations {

            @ParameterizedTest(name = "{2}")
            @MethodSource("com.jobmarket.repository.JobCountRecordRepositoryTest#filterCombinations")
            @DisplayName("should return only matching records for filter combination")
            void shouldReturnOnlyMatchingRecordsForFilterCombination(
                    ExperienceLevel expLevel,
                    SalaryRange salaryRange,
                    String description) {

                // given - create all 16 combinations
                seedAllFilterCombinations();

                // when
                Integer salaryMin = salaryRange != null ? salaryRange.getMin() : null;
                Integer salaryMax = salaryRange != null ? salaryRange.getMax() : null;

                List<JobCountRecord> results = repository.findByFilters(
                        JAVA, MetricType.TOTAL, ALL_LOCATIONS, expLevel, salaryMin, salaryMax,
                        QUERY_START, QUERY_END);

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getExperienceLevel()).isEqualTo(expLevel);

                if (salaryRange != null) {
                    assertThat(results.get(0).getSalaryMin()).isEqualTo(salaryRange.getMin());
                    assertThat(results.get(0).getSalaryMax()).isEqualTo(salaryRange.getMax());
                } else {
                    assertThat(results.get(0).getSalaryMin()).isNull();
                    assertThat(results.get(0).getSalaryMax()).isNull();
                }
            }

            private void seedAllFilterCombinations() {
                ExperienceLevel[] expLevels = {null, ExperienceLevel.JUNIOR, ExperienceLevel.MID, ExperienceLevel.SENIOR};
                SalaryRange[] salaryRanges = {null, SalaryRange.UNDER_25K, SalaryRange.RANGE_25_30K, SalaryRange.OVER_30K};

                int count = 100;
                for (ExperienceLevel exp : expLevels) {
                    for (SalaryRange salary : salaryRanges) {
                        repository.save(aRecord()
                                .withExperienceLevel(exp)
                                .withSalaryRange(salary)
                                .withCount(count++)
                                .build());
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("findLatestByFilters")
    class FindLatestByFilters {

        @Test
        @DisplayName("should return the most recent record only")
        void shouldReturnMostRecentRecordOnly() {
            // given
            repository.save(aRecord().fetchedAt(FIVE_DAYS_AGO).withCount(100).build());
            repository.save(aRecord().fetchedAt(THREE_DAYS_AGO).withCount(110).build());
            repository.save(aRecord().fetchedAt(ONE_DAY_AGO).withCount(120).build());

            // when
            Optional<JobCountRecord> result = repository.findLatestByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getCount()).isEqualTo(120);
        }

        @Test
        @DisplayName("should return empty when no records exist")
        void shouldReturnEmptyWhenNoRecordsExist() {
            // given - no records

            // when
            Optional<JobCountRecord> result = repository.findLatestByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should respect all filters when determining latest")
        void shouldRespectAllFiltersDeterminingLatest() {
            // given
            repository.save(aRecord()
                    .withExperienceLevel(ExperienceLevel.SENIOR)
                    .fetchedAt(ONE_DAY_AGO)
                    .withCount(50)
                    .build());
            repository.save(aRecord()
                    .withExperienceLevel(ExperienceLevel.JUNIOR)
                    .fetchedAt(BASE_TIME) // More recent but different experience level
                    .withCount(30)
                    .build());

            // when
            Optional<JobCountRecord> result = repository.findLatestByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, ExperienceLevel.SENIOR, null, null);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getExperienceLevel()).isEqualTo(ExperienceLevel.SENIOR);
            assertThat(result.get().getCount()).isEqualTo(50);
        }

        @Test
        @DisplayName("should return NULL experience level record when filter is null - critical null handling")
        void shouldReturnNullExperienceLevelRecordWhenFilterIsNull() {
            // given
            repository.save(aRecord().withExperienceLevel(null).fetchedAt(ONE_DAY_AGO).withCount(500).build());
            repository.save(aRecord().withExperienceLevel(ExperienceLevel.SENIOR).fetchedAt(BASE_TIME).withCount(50).build());

            // when
            Optional<JobCountRecord> result = repository.findLatestByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getExperienceLevel()).isNull();
            assertThat(result.get().getCount()).isEqualTo(500);
        }

        @Test
        @DisplayName("should return NULL salary record when filter is null - critical null handling")
        void shouldReturnNullSalaryRecordWhenFilterIsNull() {
            // given
            repository.save(aRecord().withSalaryRange(null).fetchedAt(ONE_DAY_AGO).withCount(500).build());
            repository.save(aRecord().withSalaryRange(SalaryRange.OVER_30K).fetchedAt(BASE_TIME).withCount(50).build());

            // when
            Optional<JobCountRecord> result = repository.findLatestByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getSalaryMin()).isNull();
            assertThat(result.get().getSalaryMax()).isNull();
            assertThat(result.get().getCount()).isEqualTo(500);
        }

        @Test
        @DisplayName("should return empty when filters match no records")
        void shouldReturnEmptyWhenFiltersMatchNoRecords() {
            // given
            repository.save(aRecord().withExperienceLevel(ExperienceLevel.JUNIOR).build());

            // when
            Optional<JobCountRecord> result = repository.findLatestByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, ExperienceLevel.SENIOR, null, null);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findPreviousByFilters")
    class FindPreviousByFilters {

        @Test
        @DisplayName("should return the record immediately before given timestamp")
        void shouldReturnRecordImmediatelyBeforeGivenTimestamp() {
            // given
            repository.save(aRecord().fetchedAt(FIVE_DAYS_AGO).withCount(100).build());
            repository.save(aRecord().fetchedAt(THREE_DAYS_AGO).withCount(110).build());
            repository.save(aRecord().fetchedAt(ONE_DAY_AGO).withCount(120).build());

            // when
            Optional<JobCountRecord> result = repository.findPreviousByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null, ONE_DAY_AGO);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getCount()).isEqualTo(110);
        }

        @Test
        @DisplayName("should return empty when no previous record exists")
        void shouldReturnEmptyWhenNoPreviousExists() {
            // given
            JobCountRecord onlyRecord = repository.save(aRecord().fetchedAt(ONE_DAY_AGO).build());

            // when
            Optional<JobCountRecord> result = repository.findPreviousByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null, onlyRecord.getFetchedAt());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should respect all filters when finding previous")
        void shouldRespectAllFiltersWhenFindingPrevious() {
            // given
            repository.save(aRecord()
                    .withExperienceLevel(ExperienceLevel.SENIOR)
                    .fetchedAt(THREE_DAYS_AGO)
                    .withCount(100)
                    .build());
            repository.save(aRecord()
                    .withExperienceLevel(ExperienceLevel.JUNIOR)
                    .fetchedAt(TWO_DAYS_AGO) // More recent but wrong filter
                    .withCount(50)
                    .build());
            LocalDateTime currentTime = ONE_DAY_AGO;

            // when
            Optional<JobCountRecord> result = repository.findPreviousByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, ExperienceLevel.SENIOR, null, null, currentTime);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getExperienceLevel()).isEqualTo(ExperienceLevel.SENIOR);
            assertThat(result.get().getCount()).isEqualTo(100);
        }

        @Test
        @DisplayName("should NOT return records at or after the given timestamp")
        void shouldNotReturnRecordsAtOrAfterTimestamp() {
            // given
            LocalDateTime timestamp = TWO_DAYS_AGO;
            repository.save(aRecord().fetchedAt(timestamp).withCount(100).build()); // Exact same time
            repository.save(aRecord().fetchedAt(ONE_DAY_AGO).withCount(110).build()); // After

            // when
            Optional<JobCountRecord> result = repository.findPreviousByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null, timestamp);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return NULL experience level record when filter is null - critical null handling")
        void shouldReturnNullExperienceLevelRecordWhenFilterIsNull() {
            // given
            repository.save(aRecord().withExperienceLevel(null).fetchedAt(THREE_DAYS_AGO).withCount(500).build());
            repository.save(aRecord().withExperienceLevel(ExperienceLevel.SENIOR).fetchedAt(TWO_DAYS_AGO).withCount(50).build());

            // when
            Optional<JobCountRecord> result = repository.findPreviousByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null, ONE_DAY_AGO);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getExperienceLevel()).isNull();
            assertThat(result.get().getCount()).isEqualTo(500);
        }

        @Test
        @DisplayName("should return NULL salary record when filter is null - critical null handling")
        void shouldReturnNullSalaryRecordWhenFilterIsNull() {
            // given
            repository.save(aRecord().withSalaryRange(null).fetchedAt(THREE_DAYS_AGO).withCount(500).build());
            repository.save(aRecord().withSalaryRange(SalaryRange.OVER_30K).fetchedAt(TWO_DAYS_AGO).withCount(50).build());

            // when
            Optional<JobCountRecord> result = repository.findPreviousByFilters(
                    JAVA, MetricType.TOTAL, ALL_LOCATIONS, null, null, null, ONE_DAY_AGO);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getSalaryMin()).isNull();
            assertThat(result.get().getSalaryMax()).isNull();
            assertThat(result.get().getCount()).isEqualTo(500);
        }
    }

    // Method source for parameterized tests - all 16 filter combinations
    static Stream<Arguments> filterCombinations() {
        return Stream.of(
                // null experience + various salaries
                Arguments.of(null, null, "All Levels + Any Salary"),
                Arguments.of(null, SalaryRange.UNDER_25K, "All Levels + Under 25k"),
                Arguments.of(null, SalaryRange.RANGE_25_30K, "All Levels + 25-30k"),
                Arguments.of(null, SalaryRange.OVER_30K, "All Levels + Over 30k"),
                // JUNIOR + various salaries
                Arguments.of(ExperienceLevel.JUNIOR, null, "Junior + Any Salary"),
                Arguments.of(ExperienceLevel.JUNIOR, SalaryRange.UNDER_25K, "Junior + Under 25k"),
                Arguments.of(ExperienceLevel.JUNIOR, SalaryRange.RANGE_25_30K, "Junior + 25-30k"),
                Arguments.of(ExperienceLevel.JUNIOR, SalaryRange.OVER_30K, "Junior + Over 30k"),
                // MID + various salaries
                Arguments.of(ExperienceLevel.MID, null, "Mid + Any Salary"),
                Arguments.of(ExperienceLevel.MID, SalaryRange.UNDER_25K, "Mid + Under 25k"),
                Arguments.of(ExperienceLevel.MID, SalaryRange.RANGE_25_30K, "Mid + 25-30k"),
                Arguments.of(ExperienceLevel.MID, SalaryRange.OVER_30K, "Mid + Over 30k"),
                // SENIOR + various salaries
                Arguments.of(ExperienceLevel.SENIOR, null, "Senior + Any Salary"),
                Arguments.of(ExperienceLevel.SENIOR, SalaryRange.UNDER_25K, "Senior + Under 25k"),
                Arguments.of(ExperienceLevel.SENIOR, SalaryRange.RANGE_25_30K, "Senior + 25-30k"),
                Arguments.of(ExperienceLevel.SENIOR, SalaryRange.OVER_30K, "Senior + Over 30k")
        );
    }
}
