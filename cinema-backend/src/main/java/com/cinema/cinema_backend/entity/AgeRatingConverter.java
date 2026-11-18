package com.cinema.cinema_backend.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AgeRatingConverter implements AttributeConverter<AgeRating, String> {

    @Override
    public String convertToDatabaseColumn(AgeRating attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public AgeRating convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AgeRating.fromDbValue(dbData);
    }
}
