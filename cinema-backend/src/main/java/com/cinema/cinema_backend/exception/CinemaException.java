package com.cinema.cinema_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 프론트에 { code, message } 형태로 전달되는 비즈니스 예외 */
@Getter
public class CinemaException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public CinemaException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static CinemaException unauthorized(String message) {
        return new CinemaException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, message);
    }

    public static CinemaException notFound(String message) {
        return new CinemaException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, message);
    }

    public static CinemaException badRequest(String code, String message) {
        return new CinemaException(HttpStatus.BAD_REQUEST, code, message);
    }
}
