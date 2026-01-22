package com.jobmarket.entity;

import lombok.Getter;

@Getter
public enum SalaryRange {
    UNDER_25K(null, 25000, "< 25k", true),
    RANGE_25_30K(25000, 30000, "25-30k", true),
    OVER_30K(30000, null, "> 30k", false);

    private static final int MAX_SALARY = 500000;

    private final Integer min;
    private final Integer max;
    private final String displayName;
    private final boolean requiresSubtraction;

    SalaryRange(Integer min, Integer max, String displayName, boolean requiresSubtraction) {
        this.min = min;
        this.max = max;
        this.displayName = displayName;
        this.requiresSubtraction = requiresSubtraction;
    }

    public String buildQueryParams() {
        if (requiresSubtraction) {
            return "";
        }
        int effectiveMin = min != null ? min : 0;
        int effectiveMax = max != null ? max : MAX_SALARY;
        return "salary=" + effectiveMin + "," + effectiveMax;
    }

    public String buildSubtractionQueryParams() {
        if (!requiresSubtraction) {
            return buildQueryParams();
        }
        if (this == UNDER_25K) {
            // For <25k: fetch >=25k to subtract from total
            return "salary=" + max + "," + MAX_SALARY;
        }
        // For RANGE_25_30K: fetch >=25k
        return "salary=" + min + "," + MAX_SALARY;
    }

    public String buildRangeSubtractionQueryParams() {
        // For RANGE_25_30K: returns "salary=25000,500000" (>=25k)
        return "salary=" + min + "," + MAX_SALARY;
    }

    public String buildRangeUpperBoundQueryParams() {
        // For RANGE_25_30K: returns "salary=30000,500000" (>=30k)
        return "salary=" + max + "," + MAX_SALARY;
    }

    public static SalaryRange fromMinMax(Integer min, Integer max) {
        for (SalaryRange range : values()) {
            if (java.util.Objects.equals(range.min, min) && java.util.Objects.equals(range.max, max)) {
                return range;
            }
        }
        return null;
    }
}
