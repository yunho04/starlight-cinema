
package com.cinema.cinema_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SignupRequest(

        @JsonProperty("login_id")
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "아이디는 영문, 숫자, _ 조합 4~20자로 입력해 주세요.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해 주세요.")
        String password,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 80, message = "이름은 80자 이하로 입력해 주세요.")
        String name,

        @NotBlank(message = "전화번호를 입력해 주세요.")
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. 예) 010-1234-5678")
        String phone,

        @JsonProperty("birth_date")
        @NotNull(message = "생년월일을 입력해 주세요.")
        @Past(message = "생년월일이 올바르지 않습니다.")
        LocalDate birthDate

) {}
