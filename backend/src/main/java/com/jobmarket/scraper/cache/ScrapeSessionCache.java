package com.jobmarket.scraper.cache;

import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session-scoped cache for storing intermediate HTTP response values during a single scrape batch.
 *
 * <p>This cache enables reuse of fetched values within a batch to avoid redundant HTTP requests.
 * For example, the "total" count (any salary) fetched for UNDER_25K calculation can be reused
 * as the result for the "Any Salary" filter option.
 *
 * <p>The cache should be cleared at the start of each batch to ensure fresh data.
 *
 * <p>Cache key format: {@code categorySlug|metricType|city|expLevel|salaryParams}
 */
@Component
@Slf4j
public class ScrapeSessionCache {

    private final ConcurrentHashMap<String, Integer> cache = new ConcurrentHashMap<>();

    /**
     * Stores a value in the cache.
     *
     * @param key   the cache key
     * @param value the count value to store
     */
    public void put(String key, Integer value) {
        cache.put(key, value);
        log.debug("Cache PUT: {} = {}", key, value);
    }

    /**
     * Retrieves a value from the cache.
     *
     * @param key the cache key
     * @return an Optional containing the cached value, or empty if not found
     */
    public Optional<Integer> get(String key) {
        Integer value = cache.get(key);
        if (value != null) {
            log.debug("Cache HIT: {} = {}", key, value);
            return Optional.of(value);
        } else {
            log.debug("Cache MISS: {}", key);
            return Optional.empty();
        }
    }

    /**
     * Clears all cached values.
     * Should be called at the start of each scrape batch.
     */
    public void clear() {
        int previousSize = cache.size();
        cache.clear();
        log.info("Cache cleared ({} entries removed)", previousSize);
    }

    /**
     * Returns the number of entries currently in the cache.
     *
     * @return the cache size
     */
    public int size() {
        return cache.size();
    }

    /**
     * Generates a cache key for a given set of filter parameters.
     *
     * @param categorySlug    the category slug (e.g., "java", "python")
     * @param metricType      the metric type (TOTAL, WITH_SALARY, etc.)
     * @param city            the city slug, or null for "all-locations"
     * @param experienceLevel the experience level, or null for "all levels"
     * @param salaryParams    the salary query parameters, or null for "any salary"
     * @return the generated cache key
     */
    public static String generateKey(String categorySlug, MetricType metricType,
                                       String city, ExperienceLevel experienceLevel,
                                       String salaryParams) {
        return String.format("%s|%s|%s|%s|%s",
                categorySlug,
                metricType.name(),
                city != null ? city : "ALL",
                experienceLevel != null ? experienceLevel.name() : "ALL",
                salaryParams != null ? salaryParams : "ANY");
    }
}
