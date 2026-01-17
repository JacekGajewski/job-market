package com.jobmarket.scheduler;

import com.jobmarket.scraper.JustJoinItScraperService;
import com.jobmarket.scraper.dto.JobCountResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
@ConditionalOnProperty(name = "scheduler.job-count.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class JobCountScheduler {

    private final JustJoinItScraperService scraperService;
    private final Random random = new Random();

    @Value("${scheduler.job-count.jitter-minutes:30}")
    private int jitterMinutes;

    @Scheduled(cron = "${scheduler.job-count.java.cron:0 0 6 * * *}")
    public void scrapeJavaCategory() {
        scrapeCategory("java");
    }

    @Scheduled(cron = "${scheduler.job-count.data.cron:0 0 14 * * *}")
    public void scrapeDataCategory() {
        scrapeCategory("data");
    }

    private void scrapeCategory(String categorySlug) {
        try {
            applyRandomJitter();

            log.info("=== Starting scheduled job count fetch for '{}' at {} ===",
                    categorySlug, LocalDateTime.now());

            List<JobCountResult> results = scraperService.fetchAndSaveJobCountsForCategory(categorySlug);

            long successCount = results.stream().filter(JobCountResult::isSuccess).count();
            long failCount = results.size() - successCount;

            log.info("=== Scheduled job count fetch for '{}' completed: {} success, {} failed ===",
                    categorySlug, successCount, failCount);
        } catch (Exception e) {
            log.error("=== Scheduled job count fetch for '{}' failed: {} ===",
                    categorySlug, e.getMessage(), e);
        }
    }

    private void applyRandomJitter() {
        if (jitterMinutes <= 0) {
            return;
        }
        int jitterMs = random.nextInt(jitterMinutes * 60 * 1000);
        log.info("Applying {} minute jitter before scrape", jitterMs / 60000);
        try {
            Thread.sleep(jitterMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Jitter sleep interrupted");
        }
    }
}
