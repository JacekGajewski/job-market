# Scraper Service Implementation Plan

## Overview
Service to fetch job counts from justjoin.it with dual-strategy: API first, HTML fallback.

## 1. Architecture

```
JobCountScheduler (triggers daily)
       │
       ▼
JustJoinItScraperService
       │
       ├── JustJoinItApiClient (tries API first)
       │
       └── JustJoinItHtmlParser (fallback)
       │
       ▼
JobCountRecordRepository (saves to DB)
```

## 2. Package Structure

```
com.jobmarket/
├── config/
│   └── ScraperConfig.java
├── scraper/
│   ├── JustJoinItScraperService.java
│   ├── client/
│   │   ├── JustJoinItApiClient.java
│   │   └── JustJoinItHtmlParser.java
│   ├── dto/
│   │   ├── JobOffer.java
│   │   └── JobCountResult.java
│   └── exception/
│       └── ScraperException.java
└── scheduler/
    └── JobCountScheduler.java
```

## 3. Configuration

### ScraperConfig.java
```java
@Configuration
@ConfigurationProperties(prefix = "scraper.justjoinit")
public class ScraperConfig {
    private String apiBaseUrl = "https://justjoin.it/api";
    private String webBaseUrl = "https://justjoin.it/job-offers/all-locations";
    private int connectionTimeoutMs = 10000;
    private int readTimeoutMs = 30000;
    private int delayBetweenRequestsMs = 2000;
    private int maxRetries = 3;
    private String userAgent = "Mozilla/5.0 (compatible; JobMarketBot/1.0)";
    // getters/setters
}
```

### application.yml
```yaml
scraper:
  justjoinit:
    api-base-url: https://justjoin.it/api
    web-base-url: https://justjoin.it/job-offers/all-locations
    delay-between-requests-ms: 2000
    max-retries: 3

scheduler:
  job-count:
    enabled: true
    cron: "0 0 6 * * *"  # Daily at 6 AM
```

## 4. API Client

```java
@Component
public class JustJoinItApiClient {
    private final WebClient webClient;
    private final ScraperConfig config;

    public Optional<List<JobOffer>> fetchAllOffers() {
        try {
            List<JobOffer> offers = webClient.get()
                .uri("/offers")
                .retrieve()
                .bodyToFlux(JobOffer.class)
                .collectList()
                .timeout(Duration.ofMillis(config.getReadTimeoutMs()))
                .block();
            return Optional.ofNullable(offers);
        } catch (Exception e) {
            log.warn("API call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public long countOffersForCategory(List<JobOffer> offers, String category) {
        return offers.stream()
            .filter(offer -> matchesCategory(offer, category))
            .count();
    }
}
```

## 5. HTML Parser (Fallback)

```java
@Component
public class JustJoinItHtmlParser {
    private static final Pattern COUNT_PATTERN =
        Pattern.compile("([\\d,\\s]+)\\s*(offer|ofert)", Pattern.CASE_INSENSITIVE);

    public Optional<Integer> fetchCountForCategory(String category) {
        String url = config.getWebBaseUrl() + "/" + category;

        try {
            Document doc = Jsoup.connect(url)
                .userAgent(config.getUserAgent())
                .timeout(config.getConnectionTimeoutMs())
                .get();
            return extractJobCount(doc);
        } catch (IOException e) {
            log.error("Failed to fetch HTML: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Integer> extractJobCount(Document doc) {
        // Try title first
        Matcher matcher = COUNT_PATTERN.matcher(doc.title());
        if (matcher.find()) {
            return parseCount(matcher.group(1));
        }
        // Try meta description
        // Try specific HTML elements
        return Optional.empty();
    }
}
```

## 6. Main Scraper Service

```java
@Service
public class JustJoinItScraperService {
    private final JustJoinItApiClient apiClient;
    private final JustJoinItHtmlParser htmlParser;
    private final TrackedCategoryRepository categoryRepository;

    public List<JobCountResult> fetchAllJobCounts() {
        List<TrackedCategory> categories = categoryRepository.findByActiveTrue();
        Optional<List<JobOffer>> apiOffers = apiClient.fetchAllOffers();

        List<JobCountResult> results = new ArrayList<>();
        for (TrackedCategory category : categories) {
            try {
                JobCountResult result = fetchCountForCategory(category, apiOffers);
                results.add(result);

                if (apiOffers.isEmpty()) {
                    Thread.sleep(config.getDelayBetweenRequestsMs());
                }
            } catch (Exception e) {
                results.add(JobCountResult.failed(category, e.getMessage()));
            }
        }
        return results;
    }

    private JobCountResult fetchCountForCategory(TrackedCategory cat, Optional<List<JobOffer>> apiOffers) {
        Integer count = null;

        // Try API first
        if (apiOffers.isPresent()) {
            count = (int) apiClient.countOffersForCategory(apiOffers.get(), cat.getSlug());
        }

        // Fallback to HTML
        if (count == null || count == 0) {
            count = htmlParser.fetchCountForCategory(cat.getSlug()).orElse(null);
        }

        if (count == null) {
            throw new ScraperException("Could not fetch count for: " + cat.getName());
        }

        return JobCountResult.success(cat, count, LocalDateTime.now());
    }
}
```

## 7. Scheduler

```java
@Component
@ConditionalOnProperty(name = "scheduler.job-count.enabled", havingValue = "true")
public class JobCountScheduler {
    private final JustJoinItScraperService scraperService;
    private final JobCountPersistenceService persistenceService;

    @Scheduled(cron = "${scheduler.job-count.cron:0 0 6 * * *}")
    public void fetchDailyJobCounts() {
        log.info("Starting daily job count fetch");
        List<JobCountResult> results = scraperService.fetchAllJobCounts();
        persistenceService.saveResults(results);
        log.info("Daily fetch completed");
    }
}
```

## 8. Dependencies (build.gradle)

```groovy
implementation 'org.springframework.boot:spring-boot-starter-webflux'
implementation 'org.jsoup:jsoup:1.18.3'
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
implementation 'com.google.guava:guava:33.0.0-jre'
```

## 9. Error Handling

- Retry 3 times with exponential backoff
- Circuit breaker for API failures
- Per-category error isolation
- Comprehensive logging

## 10. Implementation Checklist

- [ ] Create ScraperConfig
- [ ] Create JobOffer and JobCountResult DTOs
- [ ] Implement JustJoinItApiClient
- [ ] Implement JustJoinItHtmlParser
- [ ] Implement JustJoinItScraperService
- [ ] Create JobCountScheduler
- [ ] Add retry/circuit breaker config
- [ ] Write unit tests
- [ ] Write integration tests with WireMock
