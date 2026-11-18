
package com.cinema.cinema_backend.dto;

import com.cinema.cinema_backend.entity.Member;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record MemberResponse(

        @JsonProperty("member_id")
        Long memberId,

        // DB: members.id
        @JsonProperty("login_id")
        String loginId,

        String name,

        // DB: phone_number
        String phone,

        // DB: birth_day
        @JsonProperty("birth_date")
        LocalDate birthDate

) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getMemberId(),
                member.getLoginId(),
                member.getName(),
                member.getPhoneNumber(),
                member.getBirthDate()
        );
    }
}
