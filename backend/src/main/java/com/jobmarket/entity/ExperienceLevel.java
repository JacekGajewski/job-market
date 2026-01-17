package com.jobmarket.entity;

import lombok.Getter;

@Getter
public enum ExperienceLevel {
    JUNIOR("junior", "Junior"),
    MID("mid", "Mid"),
    SENIOR("senior", "Senior");

    private final String slug;
    private final String displayName;

    ExperienceLevel(String slug, String displayName) {
        this.slug = slug;
        this.displayName = displayName;
    }

    public String buildQueryParam() {
        return "experience-level=" + slug;
    }

    public static ExperienceLevel fromSlug(String slug) {
        if (slug == null) return null;
        for (ExperienceLevel level : values()) {
            if (level.slug.equalsIgnoreCase(slug)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown experience level: " + slug);
    }
}
