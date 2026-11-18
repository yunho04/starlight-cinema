package com.cinema.cinema_backend.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgeRatingTest {

    @Test
    void DB의_18등급은_API에서_19로_내려가고_만19세_이상만_관람가능() {
        AgeRating rating = AgeRating.fromDbValue("18");
        assertThat(rating).isEqualTo(AgeRating.RESTRICTED);
        assertThat(rating.getApiValue()).isEqualTo("19");
        assertThat(rating.getMinAge()).isEqualTo(19);
    }

    @Test
    void 나머지_등급은_값이_그대로다() {
        assertThat(AgeRating.fromDbValue("ALL").getApiValue()).isEqualTo("ALL");
        assertThat(AgeRating.fromDbValue("ALL").getMinAge()).isZero();
        assertThat(AgeRating.fromDbValue("12").getMinAge()).isEqualTo(12);
        assertThat(AgeRating.fromDbValue("15").getApiValue()).isEqualTo("15");
    }

    @Test
    void 컨버터는_DB값과_enum을_왕복한다() {
        AgeRatingConverter converter = new AgeRatingConverter();
        for (AgeRating rating : AgeRating.values()) {
            assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(rating))).isEqualTo(rating);
        }
        assertThatThrownBy(() -> AgeRating.fromDbValue("19")).isInstanceOf(IllegalArgumentException.class);
    }
}
