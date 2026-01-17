package com.jobmarket.scraper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobOffer {

    private String id;
    private String title;
    private String slug;

    @JsonProperty("marker_icon")
    private String markerIcon;

    @JsonProperty("workplace_type")
    private String workplaceType;

    @JsonProperty("experience_level")
    private String experienceLevel;

    @JsonProperty("employment_types")
    private List<EmploymentType> employmentTypes;

    private String city;
    private String street;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("company_url")
    private String companyUrl;

    @JsonProperty("company_size")
    private String companySize;

    @JsonProperty("company_logo_url")
    private String companyLogoUrl;

    @JsonProperty("published_at")
    private String publishedAt;

    private List<String> skills;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmploymentType {
        private String type;
        private Salary salary;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Salary {
        private Integer from;
        private Integer to;
        private String currency;
    }
}
