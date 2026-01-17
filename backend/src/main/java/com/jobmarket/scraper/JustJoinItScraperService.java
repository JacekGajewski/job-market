package com.jobmarket.scraper;

import com.jobmarket.config.ScraperConfig;
import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.JobCountRecord;
import com.jobmarket.entity.MetricType;
import com.jobmarket.entity.SalaryRange;
import com.jobmarket.entity.TrackedCategory;
import com.jobmarket.entity.TrackedCity;
import com.jobmarket.repository.JobCountRecordRepository;
import com.jobmarket.repository.TrackedCategoryRepository;
import com.jobmarket.repository.TrackedCityRepository;
import com.jobmarket.scraper.client.JustJoinItApiClient;
import com.jobmarket.scraper.client.JustJoinItHtmlParser;
import com.jobmarket.scraper.dto.JobCountResult;
import com.jobmarket.scraper.dto.JobOffer;
import com.jobmarket.scraper.exception.ScraperException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class JustJoinItScraperService {

    private final JustJoinItApiClient apiClient;
    private final JustJoinItHtmlParser htmlParser;
    private final TrackedCategoryRepository categoryRepository;
    private final TrackedCityRepository cityRepository;
    private final JobCountRecordRepository jobCountRecordRepository;
    private final ScraperConfig config;

    private final Random random = new Random();
    private final AtomicInteger requestCount = new AtomicInteger(0);

    public List<JobCountResult> fetchAllJobCounts() {
        List<TrackedCategory> categories = categoryRepository.findByActiveTrue();
        List<TrackedCity> cities = cityRepository.findByActiveTrue();

        // Experience levels: null (All Levels) + JUNIOR, MID, SENIOR
        List<ExperienceLevel> experienceLevels = new ArrayList<>();
        experienceLevels.add(null); // All Levels
        experienceLevels.addAll(List.of(ExperienceLevel.values()));

        // Salary ranges: null (Any Salary) + UNDER_25K, RANGE_25_30K, OVER_30K
        List<SalaryRange> salaryRanges = new ArrayList<>();
        salaryRanges.add(null); // Any Salary
        salaryRanges.addAll(List.of(SalaryRange.values()));

        // Cities: null (all-locations) + active cities
        List<String> cityOptions = new ArrayList<>();
        cityOptions.add(null); // all-locations
        cities.forEach(c -> cityOptions.add(c.getSlug()));

        int totalRequests = categories.size() * MetricType.values().length * cityOptions.size()
                * experienceLevels.size() * salaryRanges.size();
        log.info("Starting job count fetch: {} categories × {} metrics × {} cities × {} exp levels × {} salary ranges = {} total requests",
                categories.size(), MetricType.values().length, cityOptions.size(),
                experienceLevels.size(), salaryRanges.size(), totalRequests);

        // Reset request counter for this batch
        requestCount.set(0);

        List<JobCountResult> results = new ArrayList<>();

        for (TrackedCategory category : categories) {
            scrapeCategory(category, cityOptions, experienceLevels, salaryRanges, results);
        }

        return results;
    }

    public List<JobCountResult> fetchJobCountsForCategory(String categorySlug) {
        TrackedCategory category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categorySlug));

        if (!Boolean.TRUE.equals(category.getActive())) {
            log.warn("Category '{}' is not active, skipping", categorySlug);
            return List.of();
        }

        List<TrackedCity> cities = cityRepository.findByActiveTrue();

        List<ExperienceLevel> experienceLevels = new ArrayList<>();
        experienceLevels.add(null);
        experienceLevels.addAll(List.of(ExperienceLevel.values()));

        List<SalaryRange> salaryRanges = new ArrayList<>();
        salaryRanges.add(null);
        salaryRanges.addAll(List.of(SalaryRange.values()));

        List<String> cityOptions = new ArrayList<>();
        cityOptions.add(null);
        cities.forEach(c -> cityOptions.add(c.getSlug()));

        int totalRequests = MetricType.values().length * cityOptions.size()
                * experienceLevels.size() * salaryRanges.size();
        log.info("Starting job count fetch for category '{}': {} metrics × {} cities × {} exp levels × {} salary ranges = {} requests",
                categorySlug, MetricType.values().length, cityOptions.size(),
                experienceLevels.size(), salaryRanges.size(), totalRequests);

        // Reset request counter for this batch
        requestCount.set(0);

        List<JobCountResult> results = new ArrayList<>();
        scrapeCategory(category, cityOptions, experienceLevels, salaryRanges, results);
        return results;
    }

    @Transactional
    public List<JobCountResult> fetchAndSaveJobCountsForCategory(String categorySlug) {
        List<JobCountResult> results = fetchJobCountsForCategory(categorySlug);
        saveResults(results);
        return results;
    }

    private void scrapeCategory(TrackedCategory category, List<String> cityOptions,
                                 List<ExperienceLevel> experienceLevels, List<SalaryRange> salaryRanges,
                                 List<JobCountResult> results) {
        for (MetricType metricType : MetricType.values()) {
            for (String city : cityOptions) {
                for (ExperienceLevel experienceLevel : experienceLevels) {
                    for (SalaryRange salaryRange : salaryRanges) {
                        fetchAndAddResult(results, category, metricType, city, experienceLevel, salaryRange);
                    }
                }
            }
        }
    }

    private void fetchAndAddResult(List<JobCountResult> results, TrackedCategory category,
                                    MetricType metricType, String city,
                                    ExperienceLevel experienceLevel, SalaryRange salaryRange) {
        try {
            JobCountResult result = fetchCountForCategory(category, metricType, city, experienceLevel, salaryRange);
            results.add(result);
            log.info("Category '{}' [{}] city={} exp={} salary={}: {} jobs (source: {})",
                    category.getName(), metricType, city, experienceLevel, salaryRange,
                    result.getCount(), result.getSource());

            applyRandomDelay();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Scraping interrupted");
        } catch (Exception e) {
            log.error("Failed to fetch count for category '{}' [{}] city={}: {}",
                    category.getName(), metricType, city, e.getMessage());
            results.add(JobCountResult.failed(category, metricType, city, experienceLevel, salaryRange, e.getMessage()));
        }
    }

    private void applyRandomDelay() throws InterruptedException {
        int count = requestCount.incrementAndGet();

        // Random delay between min and max
        int delay = random.nextInt(config.getMaxDelayMs() - config.getMinDelayMs()) + config.getMinDelayMs();
        Thread.sleep(delay);

        // Every N requests, add a longer pause (simulates human taking a break)
        if (count % config.getPauseEveryNRequests() == 0) {
            int pause = random.nextInt(30000) + config.getPauseDurationMs();
            log.info("Taking a longer pause after {} requests ({} ms)", count, pause);
            Thread.sleep(pause);
        }
    }

    private JobCountResult fetchCountForCategory(TrackedCategory category, MetricType metricType) {
        return fetchCountForCategory(category, metricType, null, null, null);
    }

    private JobCountResult fetchCountForCategory(TrackedCategory category, MetricType metricType,
                                                  String city, ExperienceLevel experienceLevel,
                                                  SalaryRange salaryRange) {
        int count;

        if (salaryRange != null && salaryRange.isRequiresSubtraction()) {
            count = fetchCountWithSubtraction(category, metricType, city, experienceLevel, salaryRange);
        } else {
            Optional<Integer> htmlCount = htmlParser.fetchCountForCategory(
                    category.getSlug(), metricType, city, experienceLevel, salaryRange);

            if (htmlCount.isEmpty()) {
                throw new ScraperException("Could not fetch count for category: " + category.getName() +
                        " [" + metricType + "] city=" + city);
            }
            count = htmlCount.get();
        }

        return JobCountResult.success(category, metricType, city, experienceLevel, salaryRange,
                count, LocalDateTime.now(), "HTML");
    }

    private int fetchCountWithSubtraction(TrackedCategory category, MetricType metricType,
                                           String city, ExperienceLevel experienceLevel,
                                           SalaryRange salaryRange) {
        log.info("Using subtraction approach for {} (city={}, exp={}, salary={})",
                category.getSlug(), city, experienceLevel, salaryRange);

        Optional<Integer> totalCount = htmlParser.fetchCountForCategory(
                category.getSlug(), metricType, city, experienceLevel, null);

        if (totalCount.isEmpty()) {
            throw new ScraperException("Could not fetch total count for subtraction: " + category.getName());
        }

        try {
            applyRandomDelay();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Optional<Integer> aboveThresholdCount = htmlParser.fetchCountWithSalaryParams(
                category.getSlug(), metricType, city, experienceLevel, salaryRange.buildSubtractionQueryParams());

        if (aboveThresholdCount.isEmpty()) {
            throw new ScraperException("Could not fetch above-threshold count for subtraction: " + category.getName());
        }

        int result = totalCount.get() - aboveThresholdCount.get();
        log.info("Subtraction result: {} - {} = {}", totalCount.get(), aboveThresholdCount.get(), result);

        return Math.max(0, result);
    }

    @Transactional
    public void saveResults(List<JobCountResult> results) {
        log.info("Saving {} job count results", results.size());

        int successCount = 0;
        int skippedCount = 0;
        int failCount = 0;

        for (JobCountResult result : results) {
            if (result.isSuccess()) {
                LocalDate recordDate = result.getFetchedAt().toLocalDate();

                Optional<JobCountRecord> existingRecord = jobCountRecordRepository.findExistingRecord(
                        result.getCategory().getSlug(),
                        result.getMetricType(),
                        result.getCity(),
                        result.getExperienceLevel(),
                        result.getSalaryMin(),
                        result.getSalaryMax(),
                        recordDate);

                if (existingRecord.isPresent()) {
                    log.debug("Skipping duplicate record for category='{}' metric={} city={} exp={} salary={}/{} date={}",
                            result.getCategory().getSlug(), result.getMetricType(), result.getCity(),
                            result.getExperienceLevel(), result.getSalaryMin(), result.getSalaryMax(), recordDate);
                    skippedCount++;
                    continue;
                }

                String location = result.getCity() != null ? result.getCity() : result.getMetricType().getLocation();
                JobCountRecord record = JobCountRecord.builder()
                        .category(result.getCategory().getSlug())
                        .count(result.getCount())
                        .fetchedAt(result.getFetchedAt())
                        .location(location)
                        .metricType(result.getMetricType())
                        .city(result.getCity())
                        .experienceLevel(result.getExperienceLevel())
                        .salaryMin(result.getSalaryMin())
                        .salaryMax(result.getSalaryMax())
                        .recordDate(recordDate)
                        .build();
                jobCountRecordRepository.save(record);
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("Saved {} records, skipped {} duplicates, {} failures", successCount, skippedCount, failCount);
    }

    @Transactional
    public List<JobCountResult> fetchAndSaveAllJobCounts() {
        List<JobCountResult> results = fetchAllJobCounts();
        saveResults(results);
        return results;
    }
}
