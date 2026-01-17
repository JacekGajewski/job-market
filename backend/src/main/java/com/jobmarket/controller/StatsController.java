package com.jobmarket.controller;

import com.jobmarket.dto.JobCountStatsDto;
import com.jobmarket.dto.LatestCountDto;
import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.MetricType;
import com.jobmarket.entity.SalaryRange;
import com.jobmarket.scraper.JustJoinItScraperService;
import com.jobmarket.scraper.dto.JobCountResult;
import com.jobmarket.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Job count statistics endpoints")
public class StatsController {

    private final StatsService statsService;
    private final JustJoinItScraperService scraperService;

    @GetMapping("/{category}")
    @Operation(summary = "Get historical job count data for a category")
    public ResponseEntity<List<JobCountStatsDto>> getStats(
            @PathVariable String category,
            @Parameter(description = "Metric type: TOTAL, WITH_SALARY, REMOTE, REMOTE_WITH_SALARY")
            @RequestParam(required = false, defaultValue = "TOTAL") MetricType metricType,
            @Parameter(description = "City slug (e.g., 'wroclaw', 'slask'). Null for all-locations")
            @RequestParam(required = false) String city,
            @Parameter(description = "Experience level: JUNIOR, MID, SENIOR. Null for all levels")
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @Parameter(description = "Salary range: UNDER_25K, RANGE_25_30K, OVER_30K. Null for all")
            @RequestParam(required = false) SalaryRange salaryRange,
            @Parameter(description = "Start date (inclusive), format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive), format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(statsService.getHistoricalData(
                category, metricType, city, experienceLevel, salaryRange, startDate, endDate));
    }

    @GetMapping("/{category}/latest")
    @Operation(summary = "Get the most recent job count for a category")
    public ResponseEntity<LatestCountDto> getLatestCount(
            @PathVariable String category,
            @Parameter(description = "Metric type: TOTAL, WITH_SALARY, REMOTE, REMOTE_WITH_SALARY")
            @RequestParam(required = false, defaultValue = "TOTAL") MetricType metricType,
            @Parameter(description = "City slug (e.g., 'wroclaw', 'slask'). Null for all-locations")
            @RequestParam(required = false) String city,
            @Parameter(description = "Experience level: JUNIOR, MID, SENIOR. Null for all levels")
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @Parameter(description = "Salary range: UNDER_25K, RANGE_25_30K, OVER_30K. Null for all")
            @RequestParam(required = false) SalaryRange salaryRange) {
        return ResponseEntity.ok(statsService.getLatestCount(
                category, metricType, city, experienceLevel, salaryRange));
    }

    @PostMapping("/scrape")
    @Operation(summary = "Manually trigger a job count scrape for all active categories and cities")
    public ResponseEntity<Map<String, Object>> triggerScrape() {
        List<JobCountResult> results = scraperService.fetchAndSaveAllJobCounts();
        long successCount = results.stream().filter(JobCountResult::isSuccess).count();
        return ResponseEntity.ok(Map.of(
                "total", results.size(),
                "success", successCount,
                "failed", results.size() - successCount,
                "results", results.stream()
                        .map(r -> Map.of(
                                "category", r.getCategory().getName(),
                                "metricType", r.getMetricType().name(),
                                "city", r.getCity() != null ? r.getCity() : "all-locations",
                                "count", r.isSuccess() ? r.getCount() : 0,
                                "success", r.isSuccess(),
                                "source", r.isSuccess() ? r.getSource() : "N/A"
                        ))
                        .toList()
        ));
    }
}
