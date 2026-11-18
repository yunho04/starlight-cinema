package com.cinema.cinema_backend.entity;

import java.util.Arrays;

/**
 * 영화 관람등급.
 * DB(movies.age_rating ENUM)에는 청소년 관람불가가 '18'로 저장되어 있지만
 * 프론트는 '19'를 사용하므로 API 응답에서는 apiValue로 변환한다.
 */
public enum AgeRating {

    ALL("ALL", "ALL", 0),
    AGE_12("12", "12", 12),
    AGE_15("15", "15", 15),
    // 프론트 기준(상영일 기준 만 19세 이상)과 동일하게 검사한다.
    RESTRICTED("18", "19", 19);

    private final String dbValue;
    private final String apiValue;
    private final int minAge;

    AgeRating(String dbValue, String apiValue, int minAge) {
        this.dbValue = dbValue;
        this.apiValue = apiValue;
        this.minAge = minAge;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getApiValue() {
        return apiValue;
    }

    public int getMinAge() {
        return minAge;
    }

    public static AgeRating fromDbValue(String value) {
        return Arrays.stream(values())
                .filter(r -> r.dbValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 관람등급: " + value));
    }
}
