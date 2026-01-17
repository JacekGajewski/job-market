package com.jobmarket.service;

import com.jobmarket.dto.JobCountStatsDto;
import com.jobmarket.dto.LatestCountDto;
import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.JobCountRecord;
import com.jobmarket.entity.MetricType;
import com.jobmarket.entity.SalaryRange;
import com.jobmarket.exception.CategoryNotFoundException;
import com.jobmarket.exception.NoDataFoundException;
import com.jobmarket.mapper.JobCountMapper;
import com.jobmarket.repository.JobCountRecordRepository;
import com.jobmarket.repository.TrackedCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatsService {

    private static final int DEFAULT_DAYS_BACK = 30;

    private final JobCountRecordRepository jobCountRepository;
    private final TrackedCategoryRepository categoryRepository;
    private final JobCountMapper jobCountMapper;

    public List<JobCountStatsDto> getHistoricalData(String category, LocalDate startDate, LocalDate endDate) {
        return getHistoricalData(category, MetricType.TOTAL, null, null, null, startDate, endDate);
    }

    public List<JobCountStatsDto> getHistoricalData(String category, MetricType metricType, LocalDate startDate, LocalDate endDate) {
        return getHistoricalData(category, metricType, null, null, null, startDate, endDate);
    }

    public List<JobCountStatsDto> getHistoricalData(String category, MetricType metricType,
                                                     String city, ExperienceLevel experienceLevel,
                                                     SalaryRange salaryRange,
                                                     LocalDate startDate, LocalDate endDate) {
        validateCategoryExists(category);

        LocalDateTime start = resolveStartDate(startDate);
        LocalDateTime end = resolveEndDate(endDate);

        Integer salaryMin = salaryRange != null ? salaryRange.getMin() : null;
        Integer salaryMax = salaryRange != null ? salaryRange.getMax() : null;
        String location = city != null ? city : metricType.getLocation();

        List<JobCountRecord> records = jobCountRepository.findByFilters(
                category, metricType, location, experienceLevel, salaryMin, salaryMax, start, end);

        return records.stream()
                .map(jobCountMapper::toDto)
                .toList();
    }

    public LatestCountDto getLatestCount(String category) {
        return getLatestCount(category, MetricType.TOTAL, null, null, null);
    }

    public LatestCountDto getLatestCount(String category, MetricType metricType) {
        return getLatestCount(category, metricType, null, null, null);
    }

    public LatestCountDto getLatestCount(String category, MetricType metricType,
                                          String city, ExperienceLevel experienceLevel,
                                          SalaryRange salaryRange) {
        validateCategoryExists(category);

        Integer salaryMin = salaryRange != null ? salaryRange.getMin() : null;
        Integer salaryMax = salaryRange != null ? salaryRange.getMax() : null;
        String location = city != null ? city : metricType.getLocation();

        JobCountRecord latest = jobCountRepository.findLatestByFilters(
                category, metricType, location, experienceLevel, salaryMin, salaryMax)
                .orElseThrow(() -> new NoDataFoundException(category));

        Optional<JobCountRecord> previous = jobCountRepository.findPreviousByFilters(
                category, metricType, location, experienceLevel, salaryMin, salaryMax, latest.getFetchedAt());

        return buildLatestCountDto(latest, previous.orElse(null));
    }

    private void validateCategoryExists(String category) {
        if (!categoryRepository.existsBySlug(category)) {
            throw new CategoryNotFoundException(category);
        }
    }

    private LocalDateTime resolveStartDate(LocalDate startDate) {
        return startDate != null
                ? startDate.atStartOfDay()
                : LocalDate.now().minusDays(DEFAULT_DAYS_BACK).atStartOfDay();
    }

    private LocalDateTime resolveEndDate(LocalDate endDate) {
        return endDate != null
                ? endDate.atTime(23, 59, 59)
                : LocalDateTime.now();
    }

    private LatestCountDto buildLatestCountDto(JobCountRecord latest, JobCountRecord previous) {
        Integer change = null;
        Double percentageChange = null;

        if (previous != null && previous.getCount() != null && previous.getCount() > 0) {
            change = latest.getCount() - previous.getCount();
            percentageChange = (change * 100.0) / previous.getCount();
        }

        return LatestCountDto.builder()
                .category(latest.getCategory())
                .metricType(latest.getMetricType().name())
                .count(latest.getCount())
                .fetchedAt(latest.getFetchedAt())
                .changeFromPrevious(change)
                .percentageChange(percentageChange)
                .build();
    }
}
