
package com.cinema.cinema_backend.dto;

public record LoginResponse(
        String token,
        MemberResponse member
) {}

