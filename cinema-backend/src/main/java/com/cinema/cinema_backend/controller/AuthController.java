
package com.cinema.cinema_backend.controller;

import com.cinema.cinema_backend.dto.LoginRequest;
import com.cinema.cinema_backend.dto.LoginResponse;
import com.cinema.cinema_backend.dto.MemberResponse;
import com.cinema.cinema_backend.dto.SignupRequest;
import com.cinema.cinema_backend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(
            @Valid @RequestBody SignupRequest request
    ) {
        return authService.signup(request);
    }
}
