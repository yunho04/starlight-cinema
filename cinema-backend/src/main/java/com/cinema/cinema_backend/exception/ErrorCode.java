package com.cinema.cinema_backend.exception;

/** 프론트(src/api/errors.js)와 맞춘 에러 코드 */
public final class ErrorCode {

    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String DUPLICATE_LOGIN_ID = "DUPLICATE_LOGIN_ID";
    public static final String DUPLICATE_PHONE = "DUPLICATE_PHONE";
    public static final String SEAT_ALREADY_RESERVED = "SEAT_ALREADY_RESERVED";
    public static final String INVALID_SEAT = "INVALID_SEAT";
    public static final String AGE_RESTRICTED = "AGE_RESTRICTED";
    public static final String SCHEDULE_STARTED = "SCHEDULE_STARTED";
    public static final String ALREADY_CANCELLED = "ALREADY_CANCELLED";
    public static final String CONFLICT = "CONFLICT";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ErrorCode() {
    }
}
