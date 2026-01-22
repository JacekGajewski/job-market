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
import com.jobmarket.scraper.cache.ScrapeSessionCache;
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
    private final ScrapeSessionCache cache;

    private final Random random = new Random();
    private final AtomicInteger requestCount = new AtomicInteger(0);

    public List<JobCountResult> fetchAllJobCounts() {
        // Clear cache at the start of each batch to ensure fresh data
        cache.clear();

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

        log.info("Batch complete. Cache size at end: {} entries", cache.size());
        return results;
    }

    public List<JobCountResult> fetchJobCountsForCategory(String categorySlug) {
        // Clear cache at the start of each batch to ensure fresh data
        cache.clear();

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
        log.info("Batch complete for category '{}'. Cache size at end: {} entries", categorySlug, cache.size());
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
        // Optimized approach: for each (metric, city, expLevel) combo, fetch 3 base values
        // then derive all 4 salary range results from the cached values
        for (MetricType metricType : MetricType.values()) {
            for (String city : cityOptions) {
                for (ExperienceLevel experienceLevel : experienceLevels) {
                    // Step 1: Fetch and cache the 3 base values for this combo
                    fetchAndCacheBaseValues(category, metricType, city, experienceLevel);

                    // Step 2: Derive all 4 salary range results from cached values
                    for (SalaryRange salaryRange : salaryRanges) {
                        deriveAndAddResult(results, category, metricType, city, experienceLevel, salaryRange);
                    }
                }
            }
        }
    }

    /**
     * Fetches and caches the 3 base values needed for salary range derivation:
     * - 'ANY' (no salary filter) = total count
     * - '25k+' (>=25k) = salary=25000,500000
     * - '30k+' (>=30k) = salary=30000,500000
     */
    private void fetchAndCacheBaseValues(TrackedCategory category, MetricType metricType,
                                          String city, ExperienceLevel experienceLevel) {
        // Constants for salary params
        final String SALARY_25K_PLUS = "salary=25000,500000";
        final String SALARY_30K_PLUS = "salary=30000,500000";

        // Fetch and cache 'ANY' (no salary filter)
        String keyAny = ScrapeSessionCache.generateKey(
                category.getSlug(), metricType, city, experienceLevel, null);
        if (cache.get(keyAny).isEmpty()) {
            try {
                Optional<Integer> count = htmlParser.fetchCountForCategory(
                        category.getSlug(), metricType, city, experienceLevel, null);
                if (count.isPresent()) {
                    cache.put(keyAny, count.get());
                    log.debug("Fetched and cached 'ANY' value for {}/{}/{}/{}: {}",
                            category.getSlug(), metricType, city, experienceLevel, count.get());
                }
                applyRandomDelay();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while fetching base value 'ANY'");
            } catch (Exception e) {
                log.error("Failed to fetch 'ANY' base value: {}", e.getMessage());
            }
        }

        // Fetch and cache '25k+' (>=25k)
        String key25k = ScrapeSessionCache.generateKey(
                category.getSlug(), metricType, city, experienceLevel, SALARY_25K_PLUS);
        if (cache.get(key25k).isEmpty()) {
            try {
                Optional<Integer> count = htmlParser.fetchCountWithSalaryParams(
                        category.getSlug(), metricType, city, experienceLevel, SALARY_25K_PLUS);
                if (count.isPresent()) {
                    cache.put(key25k, count.get());
                    log.debug("Fetched and cached '25k+' value for {}/{}/{}/{}: {}",
                            category.getSlug(), metricType, city, experienceLevel, count.get());
                }
                applyRandomDelay();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while fetching base value '25k+'");
            } catch (Exception e) {
                log.error("Failed to fetch '25k+' base value: {}", e.getMessage());
            }
        }

        // Fetch and cache '30k+' (>=30k)
        String key30k = ScrapeSessionCache.generateKey(
                category.getSlug(), metricType, city, experienceLevel, SALARY_30K_PLUS);
        if (cache.get(key30k).isEmpty()) {
            try {
                Optional<Integer> count = htmlParser.fetchCountWithSalaryParams(
                        category.getSlug(), metricType, city, experienceLevel, SALARY_30K_PLUS);
                if (count.isPresent()) {
                    cache.put(key30k, count.get());
                    log.debug("Fetched and cached '30k+' value for {}/{}/{}/{}: {}",
                            category.getSlug(), metricType, city, experienceLevel, count.get());
                }
                applyRandomDelay();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while fetching base value '30k+'");
            } catch (Exception e) {
                log.error("Failed to fetch '30k+' base value: {}", e.getMessage());
            }
        }
    }

    /**
     * Derives salary range result from cached base values and adds to results list.
     * Derivation rules:
     * - null (Any Salary): direct from 'ANY' cache
     * - OVER_30K: direct from '30k+' cache (salary=30000,500000)
     * - UNDER_25K: 'ANY' - '25k+' (total - jobs paying >=25k)
     * - RANGE_25_30K: '25k+' - '30k+' (jobs paying >=25k minus jobs paying >=30k)
     */
    private void deriveAndAddResult(List<JobCountResult> results, TrackedCategory category,
                                     MetricType metricType, String city,
                                     ExperienceLevel experienceLevel, SalaryRange salaryRange) {
        // Constants for salary params (must match fetchAndCacheBaseValues)
        final String SALARY_25K_PLUS = "salary=25000,500000";
        final String SALARY_30K_PLUS = "salary=30000,500000";

        try {
            Integer count = null;
            String source = "DERIVED";

            if (salaryRange == null) {
                // Any Salary: direct from 'ANY' cache
                String keyAny = ScrapeSessionCache.generateKey(
                        category.getSlug(), metricType, city, experienceLevel, null);
                Optional<Integer> cached = cache.get(keyAny);
                if (cached.isPresent()) {
                    count = cached.get();
                    source = "CACHE";
                }
            } else if (salaryRange == SalaryRange.OVER_30K) {
                // OVER_30K: direct from '30k+' cache
                String key30k = ScrapeSessionCache.generateKey(
                        category.getSlug(), metricType, city, experienceLevel, SALARY_30K_PLUS);
                Optional<Integer> cached = cache.get(key30k);
                if (cached.isPresent()) {
                    count = cached.get();
                    source = "CACHE";
                }
            } else if (salaryRange == SalaryRange.UNDER_25K) {
                // UNDER_25K: 'ANY' - '25k+' (total minus jobs paying >=25k)
                String keyAny = ScrapeSessionCache.generateKey(
                        category.getSlug(), metricType, city, experienceLevel, null);
                String key25k = ScrapeSessionCache.generateKey(
                        category.getSlug(), metricType, city, experienceLevel, SALARY_25K_PLUS);

                Optional<Integer> cachedAny = cache.get(keyAny);
                Optional<Integer> cached25k = cache.get(key25k);

                if (cachedAny.isPresent() && cached25k.isPresent()) {
                    count = Math.max(0, cachedAny.get() - cached25k.get());
                    log.debug("Derived UNDER_25K: {} - {} = {}",
                            cachedAny.get(), cached25k.get(), count);
                }
            } else if (salaryRange == SalaryRange.RANGE_25_30K) {
                // RANGE_25_30K: '25k+' - '30k+' (jobs >=25k minus jobs >=30k)
                String key25k = ScrapeSessionCache.generateKey(
                        category.getSlug(), metricType, city, experienceLevel, SALARY_25K_PLUS);
                String key30k = ScrapeSessionCache.generateKey(
                        category.getSlug(), metricType, city, experienceLevel, SALARY_30K_PLUS);

                Optional<Integer> cached25k = cache.get(key25k);
                Optional<Integer> cached30k = cache.get(key30k);

                if (cached25k.isPresent() && cached30k.isPresent()) {
                    count = Math.max(0, cached25k.get() - cached30k.get());
                    log.debug("Derived RANGE_25_30K: {} - {} = {}",
                            cached25k.get(), cached30k.get(), count);
                }
            }

            if (count == null) {
                // Cache miss - fall back to direct HTTP fetch
                log.warn("Cache miss for {}/{}/{}/{}/{}, falling back to direct fetch",
                        category.getSlug(), metricType, city, experienceLevel, salaryRange);
                JobCountResult result = fetchCountForCategory(category, metricType, city, experienceLevel, salaryRange);
                results.add(result);
                log.info("Category '{}' [{}] city={} exp={} salary={}: {} jobs (source: FALLBACK)",
                        category.getName(), metricType, city, experienceLevel, salaryRange, result.getCount());
                return;
            }

            // Create result from derived/cached value
            JobCountResult result = JobCountResult.success(
                    category, metricType, city, experienceLevel, salaryRange,
                    count, LocalDateTime.now(), source);
            results.add(result);

            log.info("Category '{}' [{}] city={} exp={} salary={}: {} jobs (source: {})",
                    category.getName(), metricType, city, experienceLevel, salaryRange, count, source);

        } catch (Exception e) {
            log.error("Failed to derive count for category '{}' [{}] city={} exp={} salary={}: {}",
                    category.getName(), metricType, city, experienceLevel, salaryRange, e.getMessage());
            results.add(JobCountResult.failed(category, metricType, city, experienceLevel, salaryRange, e.getMessage()));
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
