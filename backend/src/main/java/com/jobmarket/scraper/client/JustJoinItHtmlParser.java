package com.jobmarket.scraper.client;

import com.jobmarket.config.ScraperConfig;
import com.jobmarket.entity.ExperienceLevel;
import com.jobmarket.entity.MetricType;
import com.jobmarket.entity.SalaryRange;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class JustJoinItHtmlParser {

    // Pattern for "N offers" or "N ofert" format
    private static final Pattern COUNT_PATTERN = Pattern.compile("(\\d[\\d\\s,]*)\\s*(?:current\\s+)?(?:job\\s+)?(offers?|ofert)", Pattern.CASE_INSENSITIVE);
    // Pattern for "Remote work - 368 job offers" or similar header formats with dash separator
    private static final Pattern HEADER_COUNT_PATTERN = Pattern.compile("[-–]\\s*(\\d[\\d\\s,]*)\\s*(?:job\\s+)?(offers?|ofert)", Pattern.CASE_INSENSITIVE);

    private final ScraperConfig config;

    public JustJoinItHtmlParser(ScraperConfig config) {
        this.config = config;
    }

    public Optional<Integer> fetchCountForCategory(String categorySlug) {
        return fetchCountForCategory(categorySlug, MetricType.TOTAL);
    }

    public Optional<Integer> fetchCountForCategory(String categorySlug, MetricType metricType) {
        String url = config.getWebBaseUrl() + metricType.buildFullPath(categorySlug);
        log.info("Fetching job count from HTML for category: {}, metric: {} ({})", categorySlug, metricType, url);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(config.getUserAgent())
                    .timeout(config.getConnectionTimeoutMs())
                    .followRedirects(true)
                    .get();

            return extractJobCount(doc, categorySlug);
        } catch (IOException e) {
            log.error("Failed to fetch HTML for category {}: {}", categorySlug, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Integer> fetchCountForCategory(String categorySlug, MetricType metricType,
                                                    String city, ExperienceLevel experienceLevel,
                                                    SalaryRange salaryRange) {
        String url = config.getWebBaseUrl() + metricType.buildFullPath(categorySlug, city, experienceLevel, salaryRange);
        log.info("Fetching job count from HTML for category: {}, metric: {}, city: {}, exp: {}, salary: {} ({})",
                categorySlug, metricType, city, experienceLevel, salaryRange, url);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(config.getUserAgent())
                    .timeout(config.getConnectionTimeoutMs())
                    .followRedirects(true)
                    .get();

            return extractJobCount(doc, categorySlug);
        } catch (IOException e) {
            log.error("Failed to fetch HTML for category {} with filters: {}", categorySlug, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Integer> fetchCountWithSalaryParams(String categorySlug, MetricType metricType,
                                                         String city, ExperienceLevel experienceLevel,
                                                         String salaryParams) {
        String basePath = metricType.buildFullPath(categorySlug, city, experienceLevel, null);
        String url = config.getWebBaseUrl() + basePath;
        if (salaryParams != null && !salaryParams.isEmpty()) {
            url += (url.contains("?") ? "&" : "?") + salaryParams;
        }
        log.info("Fetching job count with custom salary params: {} ({})", salaryParams, url);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(config.getUserAgent())
                    .timeout(config.getConnectionTimeoutMs())
                    .followRedirects(true)
                    .get();

            return extractJobCount(doc, categorySlug);
        } catch (IOException e) {
            log.error("Failed to fetch HTML for category {} with salary params {}: {}",
                    categorySlug, salaryParams, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Integer> extractJobCount(Document doc, String categorySlug) {
        // Try page title first
        Optional<Integer> fromTitle = extractFromTitle(doc);
        if (fromTitle.isPresent()) {
            log.debug("Found count in title: {}", fromTitle.get());
            return fromTitle;
        }

        // Try meta description
        Optional<Integer> fromMeta = extractFromMetaDescription(doc);
        if (fromMeta.isPresent()) {
            log.debug("Found count in meta description: {}", fromMeta.get());
            return fromMeta;
        }

        // Try specific elements
        Optional<Integer> fromElements = extractFromElements(doc);
        if (fromElements.isPresent()) {
            log.debug("Found count in page elements: {}", fromElements.get());
            return fromElements;
        }

        log.warn("Could not extract job count from HTML for category: {}", categorySlug);
        return Optional.empty();
    }

    private Optional<Integer> extractFromTitle(Document doc) {
        String title = doc.title();
        return extractNumber(title);
    }

    private Optional<Integer> extractFromMetaDescription(Document doc) {
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) {
            String content = metaDesc.attr("content");
            return extractNumber(content);
        }
        return Optional.empty();
    }

    private Optional<Integer> extractFromElements(Document doc) {
        // Try specific data-test selectors first (most reliable)
        String[] specificSelectors = {
                "[data-test='offers-count']",
                ".offers-count",
                "[class*='offers-count']",
                "[class*='job-count']"
        };

        for (String selector : specificSelectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                Optional<Integer> count = extractCountFromText(element.text());
                if (count.isPresent()) {
                    log.debug("Found count {} in element: {}", count.get(), selector);
                    return count;
                }
            }
        }

        // Try to find text containing "N job offers" pattern in the page body
        // This handles "Remote work - 368 job offers" format
        String bodyText = doc.body() != null ? doc.body().text() : "";
        Optional<Integer> fromBody = extractCountFromText(bodyText);
        if (fromBody.isPresent()) {
            log.debug("Found count {} in page body text", fromBody.get());
            return fromBody;
        }

        return Optional.empty();
    }

    private Optional<Integer> extractCountFromText(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }

        String normalized = text.replaceAll("[\\s\\u00A0\\u202F\\u2007\\u2009]+", " ");

        // Try main count pattern: "N offers" or "N ofert"
        Matcher matcher = COUNT_PATTERN.matcher(normalized);
        if (matcher.find()) {
            String numberStr = matcher.group(1).replaceAll("[\\s,]", "");
            try {
                return Optional.of(Integer.parseInt(numberStr));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse number: {}", numberStr);
            }
        }

        // Try header pattern: "- N job offers"
        matcher = HEADER_COUNT_PATTERN.matcher(normalized);
        if (matcher.find()) {
            String numberStr = matcher.group(1).replaceAll("[\\s,]", "");
            try {
                return Optional.of(Integer.parseInt(numberStr));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse header number: {}", numberStr);
            }
        }

        return Optional.empty();
    }

    private Optional<Integer> extractNumber(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }

        // Normalize text: replace all whitespace-like characters with regular space
        String normalized = text.replaceAll("[\\s\\u00A0\\u202F\\u2007\\u2009]+", " ");
        log.debug("Normalized text: {}", normalized);

        Matcher matcher = COUNT_PATTERN.matcher(normalized);
        if (matcher.find()) {
            String numberStr = matcher.group(1).replaceAll("[\\s,]", "");
            log.debug("Extracted number string: {}", numberStr);
            try {
                return Optional.of(Integer.parseInt(numberStr));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse number: {}", numberStr);
            }
        }

        // No fallback - only return count if it matches the expected "N offers" pattern
        // This prevents picking up random numbers from the page (like category counts, dates, etc.)
        return Optional.empty();
    }
}
