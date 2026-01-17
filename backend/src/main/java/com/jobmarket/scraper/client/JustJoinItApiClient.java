package com.jobmarket.scraper.client;

import com.jobmarket.config.ScraperConfig;
import com.jobmarket.scraper.dto.JobOffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class JustJoinItApiClient {

    private final WebClient webClient;
    private final ScraperConfig config;

    public JustJoinItApiClient(ScraperConfig config) {
        this.config = config;
        this.webClient = WebClient.builder()
                .baseUrl(config.getApiBaseUrl())
                .defaultHeader("User-Agent", config.getUserAgent())
                .build();
    }

    public Optional<List<JobOffer>> fetchAllOffers() {
        try {
            log.info("Fetching all offers from JustJoinIt API: {}", config.getApiBaseUrl());
            List<JobOffer> offers = webClient.get()
                    .uri("/offers")
                    .retrieve()
                    .bodyToFlux(JobOffer.class)
                    .collectList()
                    .timeout(Duration.ofMillis(config.getReadTimeoutMs()))
                    .block();

            if (offers != null && !offers.isEmpty()) {
                log.info("Successfully fetched {} offers from API", offers.size());
                return Optional.of(offers);
            }
            log.warn("API returned empty or null offers list");
            return Optional.empty();
        } catch (Exception e) {
            log.warn("API call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public long countOffersForCategory(List<JobOffer> offers, String categorySlug) {
        return offers.stream()
                .filter(offer -> matchesCategory(offer, categorySlug))
                .count();
    }

    private boolean matchesCategory(JobOffer offer, String categorySlug) {
        if (offer.getMarkerIcon() == null) {
            return false;
        }
        String normalizedMarkerIcon = offer.getMarkerIcon().toLowerCase().replace("-", "").replace("_", "");
        String normalizedSlug = categorySlug.toLowerCase().replace("-", "").replace("_", "");
        return normalizedMarkerIcon.contains(normalizedSlug) || normalizedSlug.contains(normalizedMarkerIcon);
    }
}
