package com.advancedswiftly.pawfit.business.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import tools.jackson.databind.json.JsonMapper;

@Converter
public class EntryConditionsConverter implements AttributeConverter<EntryConditions, String> {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(EntryConditions value) {
        return value == null ? null : JSON.writeValueAsString(value);
    }

    @Override
    public EntryConditions convertToEntityAttribute(String value) {
        return value == null ? null : JSON.readValue(value, EntryConditions.class);
    }
}
