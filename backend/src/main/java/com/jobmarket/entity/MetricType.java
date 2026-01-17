package com.jobmarket.entity;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum MetricType {
    TOTAL("all-locations", null, false, "Total Offers"),
    WITH_SALARY("all-locations", "yes", false, "With Salary"),
    REMOTE("remote", null, true, "Remote"),
    REMOTE_WITH_SALARY("remote", "yes", true, "Remote + Salary");

    private final String location;
    private final String withSalary;
    private final boolean remote;
    private final String displayName;

    MetricType(String location, String withSalary, boolean remote, String displayName) {
        this.location = location;
        this.withSalary = withSalary;
        this.remote = remote;
        this.displayName = displayName;
    }

    public String buildUrlPath(String categorySlug) {
        return "/" + location + "/" + categorySlug;
    }

    public String buildUrlPath(String categorySlug, String city) {
        String loc = city != null ? city : location;
        return "/" + loc + "/" + categorySlug;
    }

    public String buildQueryString() {
        return withSalary != null ? "?with-salary=" + withSalary : "";
    }

    public String buildFullPath(String categorySlug) {
        return buildUrlPath(categorySlug) + buildQueryString();
    }

    public String buildFullPath(String categorySlug, String city,
                                 ExperienceLevel experienceLevel, SalaryRange salaryRange) {
        String path = buildUrlPath(categorySlug, city);
        List<String> params = new ArrayList<>();

        if (withSalary != null) {
            params.add("with-salary=" + withSalary);
        }

        if (city != null && remote) {
            params.add("workplace=remote");
        }

        if (experienceLevel != null) {
            params.add(experienceLevel.buildQueryParam());
        }

        if (salaryRange != null) {
            String salaryParams = salaryRange.buildQueryParams();
            if (!salaryParams.isEmpty()) {
                params.add(salaryParams);
            }
        }

        if (params.isEmpty()) {
            return path;
        }
        return path + "?" + String.join("&", params);
    }
}
