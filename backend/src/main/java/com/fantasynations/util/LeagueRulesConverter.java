package com.fantasynations.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fantasynations.domain.LeagueRules;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LeagueRulesConverter implements AttributeConverter<LeagueRules, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(LeagueRules rules) {
        if (rules == null) return "{}";
        try {
            return MAPPER.writeValueAsString(rules);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public LeagueRules convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return new LeagueRules();
        try {
            return MAPPER.readValue(json, LeagueRules.class);
        } catch (Exception e) {
            return new LeagueRules();
        }
    }
}
